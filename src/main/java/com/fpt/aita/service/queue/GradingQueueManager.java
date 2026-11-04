package com.fpt.aita.service.queue;

import com.fpt.aita.ai.AiAnalysisService;
import com.fpt.aita.ai.AiRunSummary;
import com.fpt.aita.dal.SubmissionDAO;
import com.fpt.aita.model.Submission;
import com.fpt.aita.util.ZipExtractorUtil;
import java.io.File;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * THÀNH VIÊN 3: Quản lý Hàng Đợi Chấm Bài Bất Đồng Bộ (Grading Queue Manager)
 * - Sử dụng Singleton Pattern & In-Memory BlockingQueue chuẩn Thread-safe.
 * - Tránh nghẽn server Tomcat khi nhiều sinh viên nộp bài cùng lúc.
 * - Tích hợp ZipExtractorUtil và AiAnalysisService để chấm bài thực tế.
 */
public class GradingQueueManager {

    private static volatile GradingQueueManager instance;

    // Hàng đợi lưu trữ các submission_id chờ chấm theo nguyên tắc FIFO
    private final BlockingQueue<Integer> queue;

    // Bảng theo dõi tiến độ / vị trí của submission trong hàng đợi
    // Key: submissionId, Value: Mô tả tiến độ (vd: "Đang chờ ở hàng đợi", "Đang phân tích AI...")
    private final ConcurrentHashMap<Integer, String> taskStatusMap;

    private final SubmissionDAO submissionDAO;
    private Thread workerThread;
    private volatile boolean isRunning = false;

    // Đường dẫn gốc của webapp (set bởi GradingQueueListener khi Tomcat khởi động)
    private volatile String webAppRoot = null;

    private GradingQueueManager() {
        this.queue = new LinkedBlockingQueue<Integer>();
        this.taskStatusMap = new ConcurrentHashMap<Integer, String>();
        this.submissionDAO = new SubmissionDAO();
        startWorker();
    }

    public static GradingQueueManager getInstance() {
        if (instance == null) {
            synchronized (GradingQueueManager.class) {
                if (instance == null) {
                    instance = new GradingQueueManager();
                }
            }
        }
        return instance;
    }

    /**
     * Được gọi bởi GradingQueueListener khi ứng dụng khởi động.
     * Truyền đường dẫn thực (realPath) của thư mục gốc webapp trên đĩa.
     */
    public void init(String webAppRoot) {
        this.webAppRoot = webAppRoot;
        System.out.println("[GradingQueue] webAppRoot đã được cấu hình: " + webAppRoot);
    }

    /**
     * Đẩy một bài nộp vào hàng đợi chấm bài
     */
    public synchronized boolean enqueue(int submissionId) {
        if (submissionId <= 0) return false;

        // Đặt trạng thái ban đầu trong DB là Pending
        submissionDAO.updateSubmissionStatus(submissionId, "Pending");
        taskStatusMap.put(submissionId, "Đang chờ chấm (Vị trí trong hàng đợi: #" + (queue.size() + 1) + ")");

        return queue.offer(submissionId);
    }

    /**
     * Khởi động luồng Worker xử lý ngầm (Background Worker)
     */
    private void startWorker() {
        if (isRunning) return;
        isRunning = true;

        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println(">>> [GradingQueue] Worker đã khởi động thành công, sẵn sàng nhận bài!");
                while (isRunning) {
                    try {
                        // Chờ và lấy bài nộp tiếp theo trong hàng đợi (Block nếu hàng đợi rỗng)
                        Integer submissionId = queue.take();
                        processSubmission(submissionId);
                    } catch (InterruptedException e) {
                        System.out.println(">>> [GradingQueue] Worker nhận tín hiệu dừng.");
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }, "AITA-Grading-Worker-Thread");

        workerThread.setDaemon(true); // Tự giải phóng khi Tomcat dừng
        workerThread.start();
    }

    /**
     * Luồng chấm bài thực tế:
     * 1. Lấy thông tin bài nộp từ DB
     * 2. Giải nén file .zip vào thư mục extracted
     * 3. Gọi AiAnalysisService phân tích mã nguồn (nếu có METHODS trong DB)
     * 4. Cập nhật trạng thái Completed / Failed
     */
    private void processSubmission(int submissionId) {
        try {
            System.out.println(">>> [GradingQueue] Bắt đầu chấm bài ID: " + submissionId);

            // Bước 1: Chuyển trạng thái sang Processing
            submissionDAO.updateSubmissionStatus(submissionId, "Processing");
            taskStatusMap.put(submissionId, "Đang lấy thông tin bài nộp từ DB...");

            // Bước 2: Lấy thông tin bài nộp
            Submission sub = submissionDAO.getSubmissionById(submissionId);
            if (sub == null) {
                System.err.println(">>> [GradingQueue] Không tìm thấy submission ID=" + submissionId + " trong DB.");
                submissionDAO.updateSubmissionStatus(submissionId, "Failed");
                taskStatusMap.put(submissionId, "Lỗi: Không tìm thấy bài nộp trong DB");
                return;
            }

            // Bước 3: Giải nén file .zip
            taskStatusMap.put(submissionId, "Đang giải nén file bài nộp...");
            String zipAbsPath = resolveFilePath(sub.getFilePath());
            String extractedRoot = resolveExtractedRoot();

            List<String> javaFiles = ZipExtractorUtil.extract(zipAbsPath, extractedRoot, submissionId);
            if (javaFiles.isEmpty()) {
                System.out.println(">>> [GradingQueue] submissionId=" + submissionId
                        + ": Không có file .java nào được giải nén (zip hỏng hoặc không có Java source)."
                        + " Tiếp tục phân tích DB (nếu AST Engine đã chạy)...");
            } else {
                System.out.println(">>> [GradingQueue] submissionId=" + submissionId
                        + ": Giải nén thành công " + javaFiles.size() + " file .java.");
            }

            // Bước 4: Gọi AI phân tích
            // Nếu METHODS chưa có dữ liệu (AST Engine chưa chạy), AiAnalysisService
            // sẽ tự bỏ qua và trả về summary rỗng — không làm bài bị Failed.
            taskStatusMap.put(submissionId, "Đang phân tích AI và đối chiếu bộ luật...");
            AiAnalysisService aiService = new AiAnalysisService();
            AiRunSummary summary = aiService.analyzeSubmission(submissionId);
            System.out.println(">>> [GradingQueue] AI Summary submissionId=" + submissionId + ": " + summary);

            // Bước 5: Hoàn tất
            submissionDAO.updateSubmissionStatus(submissionId, "Completed");
            taskStatusMap.put(submissionId, "Đã chấm hoàn tất (Completed)");
            System.out.println(">>> [GradingQueue] Chấm bài ID: " + submissionId + " THÀNH CÔNG!");

        } catch (Exception e) {
            System.err.println(">>> [GradingQueue] Lỗi khi chấm bài ID " + submissionId + ": " + e.getMessage());
            e.printStackTrace();
            submissionDAO.updateSubmissionStatus(submissionId, "Failed");
            taskStatusMap.put(submissionId, "Lỗi phân tích: " + e.getMessage());
        }
    }

    /**
     * Chuyển đổi file_path lưu trong DB thành đường dẫn tuyệt đối trên đĩa.
     * DB lưu dạng: "/uploads/submissions/abc.zip" hoặc "uploads/submissions/abc.zip"
     */
    private String resolveFilePath(String dbFilePath) {
        if (dbFilePath == null || dbFilePath.trim().isEmpty()) {
            return "";
        }
        // Nếu đã là đường dẫn tuyệt đối thì trả về thẳng
        File f = new File(dbFilePath);
        if (f.isAbsolute()) {
            return dbFilePath;
        }
        // Nếu có webAppRoot, ghép vào
        if (webAppRoot != null) {
            // Loại bỏ dấu / đầu nếu có (tránh double-slash trên Linux)
            String relative = dbFilePath.startsWith("/") ? dbFilePath.substring(1) : dbFilePath;
            return new File(webAppRoot, relative).getAbsolutePath();
        }
        // Fallback: dùng working directory
        return new File(dbFilePath).getAbsolutePath();
    }

    /**
     * Lấy đường dẫn thư mục giải nén.
     * Ưu tiên: env var AI_EXTRACTED_ROOT -> {webAppRoot}/uploads/extracted -> fallback tương đối
     */
    private String resolveExtractedRoot() {
        // Ưu tiên env var (có thể set trong Tomcat setenv.bat)
        String envRoot = System.getenv("AI_EXTRACTED_ROOT");
        if (envRoot != null && !envRoot.trim().isEmpty()) {
            return envRoot.trim();
        }
        // Dùng webAppRoot nếu có
        if (webAppRoot != null) {
            return new File(webAppRoot, "uploads/extracted").getAbsolutePath();
        }
        // Fallback tương đối từ JVM working dir
        return "uploads/extracted";
    }

    /**
     * Lấy mô tả trạng thái hiện tại của bài nộp
     */
    public String getStatusDetail(int submissionId) {
        return taskStatusMap.getOrDefault(submissionId, "Không có trong hàng đợi");
    }

    /**
     * Số lượng bài nộp còn đang chờ trong hàng đợi
     */
    public int getQueueSize() {
        return queue.size();
    }

    /**
     * Dừng worker khi tắt ứng dụng (gọi bởi GradingQueueListener.contextDestroyed)
     */
    public synchronized void shutdown() {
        isRunning = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }
}
