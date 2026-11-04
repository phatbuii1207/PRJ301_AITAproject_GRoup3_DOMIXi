package com.fpt.aita.service.queue;

import com.fpt.aita.ai.AiAnalysisService;
import com.fpt.aita.ai.AiRunSummary;
import com.fpt.aita.dal.SubmissionDAO;
import com.fpt.aita.model.Submission;
import com.fpt.aita.service.AstAnalyzerService;
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
 * - Kết nối hoàn chỉnh Pipeline: AST Engine (TV1) -> AI Optimization (TV2) -> DB & View.
 */
public class GradingQueueManager {

    private static volatile GradingQueueManager instance;

    // Hàng đợi lưu trữ các submission_id chờ chấm theo nguyên tắc FIFO
    private final BlockingQueue<Integer> queue;

    // Bảng theo dõi tiến độ / vị trí của submission trong hàng đợi
    private final ConcurrentHashMap<Integer, String> taskStatusMap;
    // Bảng lưu đường dẫn file vật lý (nếu được truyền trực tiếp khi nộp)
    private final ConcurrentHashMap<Integer, String> filePathMap;

    private final SubmissionDAO submissionDAO;
    private final AstAnalyzerService astAnalyzer;
    private Thread workerThread;
    private volatile boolean isRunning = false;

    // Đường dẫn gốc của webapp (set bởi GradingQueueListener khi Tomcat khởi động)
    private volatile String webAppRoot = null;

    private GradingQueueManager() {
        this.queue = new LinkedBlockingQueue<Integer>();
        this.taskStatusMap = new ConcurrentHashMap<Integer, String>();
        this.filePathMap = new ConcurrentHashMap<Integer, String>();
        this.submissionDAO = new SubmissionDAO();
        this.astAnalyzer = AstAnalyzerService.getInstance();
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
        return enqueue(submissionId, null);
    }

    /**
     * Đẩy bài nộp kèm đường dẫn tệp thực tế trên server
     */
    public synchronized boolean enqueue(int submissionId, String absoluteFilePath) {
        if (submissionId <= 0) return false;

        if (absoluteFilePath != null && !absoluteFilePath.trim().isEmpty()) {
            filePathMap.put(submissionId, absoluteFilePath);
        }

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
     * Luồng chấm bài tích hợp hoàn chỉnh:
     * 1. Lấy thông tin bài nộp từ DB & định vị file trên đĩa
     * 2. [THÀNH VIÊN 1]: Bóc tách AST -> ghi SOURCE_FILES, METHODS, ISSUES, ANALYSIS_REPORTS
     * 3. [THÀNH VIÊN 2]: Giải nén zip & gọi AI phân tích tối ưu mã nguồn (AI_OPTIMIZATIONS)
     * 4. Cập nhật trạng thái Completed / Failed
     */
    private void processSubmission(int submissionId) {
        try {
            System.out.println(">>> [GradingQueue] Bắt đầu chấm bài ID: " + submissionId);

            // Bước 1: Chuyển trạng thái sang Processing
            submissionDAO.updateSubmissionStatus(submissionId, "Processing");
            taskStatusMap.put(submissionId, "Đang định vị file và chuẩn bị phân tích...");

            // Bước 2: Lấy thông tin bài nộp
            Submission sub = submissionDAO.getSubmissionById(submissionId);
            if (sub == null) {
                System.err.println(">>> [GradingQueue] Không tìm thấy submission ID=" + submissionId + " trong DB.");
                submissionDAO.updateSubmissionStatus(submissionId, "Failed");
                taskStatusMap.put(submissionId, "Lỗi: Không tìm thấy bài nộp trong DB");
                return;
            }

            File subFile = resolveSubmissionFile(sub);
            if (subFile == null || !subFile.exists()) {
                System.err.println(">>> [GradingQueue] Không tìm thấy tệp vật lý cho bài nộp #" + submissionId);
                submissionDAO.updateSubmissionStatus(submissionId, "Failed");
                taskStatusMap.put(submissionId, "Lỗi: Không tìm thấy file bài nộp trên máy chủ.");
                return;
            }

            // Bước 3: [THÀNH VIÊN 1] Phân tích cú pháp tĩnh AST & quét bộ luật RULES
            taskStatusMap.put(submissionId, "Đang bóc tách AST, trích xuất METHODS & quét RULES...");
            boolean astSuccess = astAnalyzer.analyzeAndSave(submissionId, subFile);
            if (!astSuccess) {
                System.err.println(">>> [GradingQueue] AST Analyzer cảnh báo lỗi phân tích cho ID=" + submissionId);
            }

            // Bước 4: Giải nén mã nguồn vào thư mục extracted (nếu là file zip) để phục vụ AI đọc code
            String extractedRoot = resolveExtractedRoot();
            if (subFile.getName().toLowerCase().endsWith(".zip")) {
                taskStatusMap.put(submissionId, "Đang giải nén mã nguồn...");
                List<String> javaFiles = ZipExtractorUtil.extract(subFile.getAbsolutePath(), extractedRoot, submissionId);
                System.out.println(">>> [GradingQueue] submissionId=" + submissionId + ": Giải nén " + javaFiles.size() + " file .java.");
            }

            // Bước 5: [THÀNH VIÊN 2] Gọi AI phân tích và đề xuất tối ưu hóa
            // Lúc này METHODS và ISSUES đã được AST Analyzer (Bước 3) ghi đầy đủ vào CSDL,
            // nên AiAnalysisService sẽ đọc được đúng các hàm có lỗi để hỏi AI!
            taskStatusMap.put(submissionId, "Đang gửi ngữ cảnh hàm lỗi đến AI để tối ưu hóa...");
            try {
                AiAnalysisService aiService = new AiAnalysisService();
                AiRunSummary summary = aiService.analyzeSubmission(submissionId);
                System.out.println(">>> [GradingQueue] AI Summary submissionId=" + submissionId + ": " + summary);
            } catch (Exception aiEx) {
                System.err.println(">>> [GradingQueue] Cảnh báo lỗi AI (vẫn giữ kết quả AST): " + aiEx.getMessage());
            }

            // Bước 6: Hoàn tất
            submissionDAO.updateSubmissionStatus(submissionId, "Completed");
            taskStatusMap.put(submissionId, "Đã chấm hoàn tất (Completed)");
            System.out.println(">>> [GradingQueue] Chấm bài ID: " + submissionId + " THÀNH CÔNG!");

        } catch (Exception e) {
            System.err.println(">>> [GradingQueue] Lỗi khi chấm bài ID " + submissionId + ": " + e.getMessage());
            e.printStackTrace();
            submissionDAO.updateSubmissionStatus(submissionId, "Failed");
            taskStatusMap.put(submissionId, "Lỗi phân tích: " + e.getMessage());
        } finally {
            filePathMap.remove(submissionId);
        }
    }

    /**
     * Định vị file bài nộp từ đường dẫn tạm hoặc các vị trí triển khai của Tomcat
     */
    private File resolveSubmissionFile(Submission sub) {
        if (sub == null) return null;

        String explicitPath = filePathMap.get(sub.getSubmissionId());
        if (explicitPath != null) {
            File f = new File(explicitPath);
            if (f.exists()) return f;
        }

        String dbFilePath = sub.getFilePath();
        if (dbFilePath == null || dbFilePath.trim().isEmpty()) return null;

        File direct = new File(dbFilePath);
        if (direct.exists()) return direct;

        String resolved = resolveFilePath(dbFilePath);
        File rf = new File(resolved);
        if (rf.exists()) return rf;

        String rel = dbFilePath.trim();
        File[] candidates = new File[] {
            new File(rel),
            new File("src/main/webapp" + (rel.startsWith("/") ? "" : "/") + rel),
            new File(System.getProperty("user.dir") + (rel.startsWith("/") ? "" : "/") + rel),
            new File(System.getProperty("user.dir") + "/src/main/webapp" + (rel.startsWith("/") ? "" : "/") + rel),
            new File(System.getProperty("user.dir") + "/target/AITA_StaticCodeAnalysis-1.0-SNAPSHOT" + (rel.startsWith("/") ? "" : "/") + rel)
        };

        for (File c : candidates) {
            if (c.exists()) return c;
        }

        return null;
    }

    /**
     * Chuyển đổi file_path lưu trong DB thành đường dẫn tuyệt đối trên đĩa.
     */
    private String resolveFilePath(String dbFilePath) {
        if (dbFilePath == null || dbFilePath.trim().isEmpty()) {
            return "";
        }
        File f = new File(dbFilePath);
        if (f.isAbsolute()) {
            return dbFilePath;
        }
        if (webAppRoot != null) {
            String relative = dbFilePath.startsWith("/") ? dbFilePath.substring(1) : dbFilePath;
            return new File(webAppRoot, relative).getAbsolutePath();
        }
        return new File(dbFilePath).getAbsolutePath();
    }

    /**
     * Lấy đường dẫn thư mục giải nén.
     */
    private String resolveExtractedRoot() {
        String envRoot = System.getenv("AI_EXTRACTED_ROOT");
        if (envRoot != null && !envRoot.trim().isEmpty()) {
            return envRoot.trim();
        }
        if (webAppRoot != null) {
            return new File(webAppRoot, "uploads/extracted").getAbsolutePath();
        }
        return "uploads/extracted";
    }

    public String getStatusDetail(int submissionId) {
        return taskStatusMap.getOrDefault(submissionId, "Không có trong hàng đợi");
    }

    public int getQueueSize() {
        return queue.size();
    }

    public synchronized void shutdown() {
        isRunning = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }
}
