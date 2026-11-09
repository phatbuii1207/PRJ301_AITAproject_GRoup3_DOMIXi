package com.fpt.aita.service.queue;

import com.fpt.aita.dal.GradingDAO;
import com.fpt.aita.dal.AnalysisReportDAO;
import com.fpt.aita.model.Assignment;
import com.fpt.aita.model.GradingConfig;
import com.fpt.aita.model.GradingReport;
import com.fpt.aita.model.AnalysisReport;
import com.fpt.aita.model.Submission;
import com.fpt.aita.service.GradingService;
import com.fpt.aita.service.SubmissionAnalysisService;
import com.fpt.aita.util.UploadStorage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/** Bounded FIFO worker. Grading is real; AI/AST reporting remains a separate pipeline. */
public final class GradingQueueManager {
    private static volatile GradingQueueManager instance;
    private static final Logger LOG = Logger.getLogger(GradingQueueManager.class.getName());
    private final BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(100);
    private final ConcurrentHashMap<Integer, String> details = new ConcurrentHashMap<>();
    private final Set<Integer> active = ConcurrentHashMap.newKeySet();
    private final Path uploadRoot;
    private final Thread worker;
    private volatile boolean running = true;

    private GradingQueueManager(Path uploadRoot) {
        this.uploadRoot = (uploadRoot != null ? uploadRoot : UploadStorage.getInstance().getUploadDir()).toAbsolutePath().normalize();
        worker = new Thread(() -> {
            while (running) {
                try {
                    int id = queue.take();
                    try { process(id); } finally {
                        active.remove(id);
                        if (details.size() > 1000) {
                            for (Integer old : details.keySet()) {
                                if (!active.contains(old)) details.remove(old);
                                if (details.size() <= 1000) break;
                            }
                        }
                    }
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); break; }
            }
        }, "AITA-Grading-Worker-Thread");
        worker.setDaemon(true); worker.start();
    }

    public static synchronized void initialize(Path uploadRoot) {
        if (instance == null) instance = new GradingQueueManager(uploadRoot);
    }
    public static GradingQueueManager getInstance() {
        GradingQueueManager manager = instance;
        if (manager == null) throw new IllegalStateException("Grading lifecycle chưa khởi tạo.");
        return manager;
    }

    public synchronized boolean enqueue(int submissionId) {
        if (!running || submissionId <= 0 || queue.remainingCapacity() == 0 || !active.add(submissionId)) return false;
        try (GradingDAO dao = new GradingDAO()) {
            dao.updateStatus(submissionId, "Pending");
            details.put(submissionId, "Đang chờ chấm bài.");
            queue.add(submissionId);
            return true;
        } catch (Exception ex) {
            active.remove(submissionId);
            LOG.warning("Cannot enqueue submission " + submissionId + " (" + ex.getClass().getSimpleName() + ")");
            return false;
        }
    }

    /** Compatibility with the team's upload API; supplied paths must stay inside the upload root or fallback root. */
    public boolean enqueue(int submissionId, String absolutePath) {
        if (absolutePath != null) {
            Path parent = java.nio.file.Paths.get(absolutePath).toAbsolutePath().normalize().getParent();
            boolean insidePrimary = uploadRoot.equals(parent);
            Path fallback = UploadStorage.getInstance().getFallbackDir();
            boolean insideFallback = fallback != null && fallback.equals(parent);
            if (!insidePrimary && !insideFallback) return false;
        }
        return enqueue(submissionId);
    }

    private void process(int id) {
        try {
            Submission submission; GradingConfig config;
            try (GradingDAO dao = new GradingDAO()) {
                submission = dao.findSubmission(id);
                if (submission == null) return;
                dao.updateStatus(id, "Processing");
                config = dao.loadConfig(submission.getAssignmentId());
            }
            Path archive = resolveArchive(submission.getFilePath());
            if (!hash(archive).equalsIgnoreCase(submission.getSha256Hash())) throw new IllegalArgumentException("Submission hash mismatch");
            boolean zip = archive.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".zip");
            Assignment assignment;
            try (GradingDAO dao = new GradingDAO()) {
                assignment = dao.findAssignment(submission.getAssignmentId());
            }
            boolean rubricOnly = assignment != null && !"NONE".equalsIgnoreCase(assignment.getRubricType());
            GradingReport report;
            if (rubricOnly) {
                details.put(id, "Đang phân tích và chấm theo rubric; không chạy test case...");
                report = GradingReport.unavailable("RUBRIC_PENDING", "Đang chấm theo rubric AI.");
            } else {
                details.put(id, "Đang chạy test case trong Docker Sandbox...");
                try {
                    report = new GradingService().grade(archive, config);
                } catch (RuntimeException gradingFailure) {
                    report = GradingReport.unavailable("GRADING_ERROR", "Lỗi chấm test case; tiếp tục phân tích AST/AI.");
                }
            }
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            // Optional analysis failures must not replace a valid functional score with GRADING_ERROR.
            {
                details.put(id, rubricOnly ? "Đang phân tích AST và chấm điểm rubric AI..."
                        : "Đã xử lý test case; đang phân tích AST và đề xuất AI...");
                try {
                    boolean analyzed = new SubmissionAnalysisService().analyze(id, archive);
                    if (rubricOnly && analyzed) {
                        AnalysisReport rubricReport = new AnalysisReportDAO().getBySubmissionId(id);
                        int qualityScore = rubricReport == null ? 0 : Math.max(0, Math.min(100, rubricReport.getQualityScore()));
                        BigDecimal score = BigDecimal.valueOf(qualityScore).divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
                        report = new GradingReport("RUBRIC", "AI", 1, score,
                                "Điểm được tính theo rubric " + assignment.getRubricType() + "; không chạy test case.",
                                Collections.<com.fpt.aita.model.GradingCaseResult>emptyList());
                    } else if (!rubricOnly && !zip && analyzed && "NOT_CONFIGURED".equals(report.getVerdict())) {
                        try (GradingDAO dao = new GradingDAO()) { dao.updateStatus(id, "Completed"); }
                    }
                } catch (Exception analysisFailure) {
                    LOG.warning("Optional AST/AI analysis failed for submission " + id + " (" + analysisFailure.getClass().getSimpleName() + ")");
                }
            }
            try (GradingDAO dao = new GradingDAO()) { dao.saveReport(id, report); }
            details.put(id, report.isComplete()
                    ? (rubricOnly ? "Đã chấm theo rubric; điểm " + report.getScore() + "/10."
                            : "Đã chấm " + report.getPassed() + "/" + report.getTotal() + " test; điểm " + report.getScore() + "/10.")
                    : report.getMessage());
        } catch (InterruptedException ex) {
            details.put(id, "Tác vụ bị gián đoạn; cần chấm lại.");
            saveFailure(id);
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            details.put(id, "Không hoàn tất chấm bài. Kiểm tra cấu hình, file và hạ tầng rồi chấm lại.");
            LOG.warning("Cannot grade submission " + id + " (" + ex.getClass().getSimpleName() + ")");
            saveFailure(id);
        }
    }

    private void saveFailure(int id) {
        try (GradingDAO dao = new GradingDAO()) {
            dao.saveReport(id, GradingReport.unavailable("GRADING_ERROR", "Chưa có điểm hợp lệ; kiểm tra hạ tầng/file rồi chấm lại."));
        } catch (Exception ex) { LOG.warning("Cannot persist grading failure for submission " + id); }
    }

    private Path resolveArchive(String stored) throws Exception {
        return UploadStorage.getInstance().resolveArchive(stored);
    }
    private String hash(Path archive) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(archive)) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) digest.update(buffer, 0, n);
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) hex.append(String.format("%02x", b));
        return hex.toString();
    }
    public String getStatusDetail(int id) { return details.getOrDefault(id, "Xem kết quả đã lưu tại trang chấm test case."); }
    public int getQueueSize() { return queue.size(); }
    public boolean isActive(int id) { return active.contains(id); }
    public synchronized void shutdown() { running = false; worker.interrupt(); }
}
