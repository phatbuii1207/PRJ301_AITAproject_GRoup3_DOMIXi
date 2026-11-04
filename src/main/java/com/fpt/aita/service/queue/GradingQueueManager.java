package com.fpt.aita.service.queue;

import com.fpt.aita.dal.SubmissionDAO;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * THÀNH VIÊN 3: Quản lý Hàng Đợi Chấm Bài Bất Đồng Bộ (Grading Queue Manager)
 * - Sử dụng Singleton Pattern & In-Memory BlockingQueue chuẩn Thread-safe.
 * - Tránh nghẽn server Tomcat khi nhiều sinh viên nộp bài cùng lúc.
 * - Cung cấp cơ chế cắm các engine phân tích mã nguồn AST / AI vào phía sau.
 */
public class GradingQueueManager {

    private static volatile GradingQueueManager instance;

    // Hàng đợi lưu trữ các submission_id chờ chấm theo nguyên tắc FIFO
    private final BlockingQueue<Integer> queue;

    // Bảng theo dõi tiến độ / vị trí của submission trong hàng đợi
    // Key: submissionId, Value: Mô tả tiến độ (vd: "Đang chờ ở hàng đợi", "Đang phân tích AST...")
    private final ConcurrentHashMap<Integer, String> taskStatusMap;

    private final SubmissionDAO submissionDAO;
    private Thread workerThread;
    private volatile boolean isRunning = false;

    private GradingQueueManager() {
        this.queue = new LinkedBlockingQueue<>();
        this.taskStatusMap = new ConcurrentHashMap<>();
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

        workerThread = new Thread(() -> {
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
        }, "AITA-Grading-Worker-Thread");

        workerThread.setDaemon(true); // Tự giải phóng khi Tomcat dừng
        workerThread.start();
    }

    /**
     * Luồng chấm bài (gọi Static Code Analysis AST & AI)
     */
    private void processSubmission(int submissionId) {
        try {
            System.out.println(">>> [GradingQueue] Bắt đầu chấm bài ID: " + submissionId);
            
            // 1. Chuyển trạng thái sang Processing
            submissionDAO.updateSubmissionStatus(submissionId, "Processing");
            taskStatusMap.put(submissionId, "Đang phân tích mã nguồn tĩnh (AST)...");

            // Giả lập thời gian xử lý AST + AI (từ 2 - 3 giây)
            // Khi thành viên làm AST/AI hoàn thành, chỉ cần gọi hàm engine ở đây!
            Thread.sleep(2500);

            // 2. Cập nhật tiến độ tiếp theo
            taskStatusMap.put(submissionId, "Đang quét lỗi bảo mật & đối chiếu bộ luật RULES...");
            Thread.sleep(1500);

            // 3. Hoàn tất quá trình chấm bài
            submissionDAO.updateSubmissionStatus(submissionId, "Completed");
            taskStatusMap.put(submissionId, "Đã chấm hoàn tất (Completed)");
            System.out.println(">>> [GradingQueue] Chấm bài ID: " + submissionId + " THÀNH CÔNG!");

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            submissionDAO.updateSubmissionStatus(submissionId, "Failed");
            taskStatusMap.put(submissionId, "Bị gián đoạn");
        } catch (Exception e) {
            System.err.println(">>> [GradingQueue] Lỗi khi chấm bài ID " + submissionId + ": " + e.getMessage());
            submissionDAO.updateSubmissionStatus(submissionId, "Failed");
            taskStatusMap.put(submissionId, "Lỗi phân tích: " + e.getMessage());
        }
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
     * Dừng worker khi tắt ứng dụng
     */
    public synchronized void shutdown() {
        isRunning = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }
}
