package com.fpt.aita.ai;

import com.fpt.aita.dal.AiOptimizationDAO;
import com.fpt.aita.dal.AnalysisReportDAO;
import java.util.Arrays;
import java.util.Locale;
import com.fpt.aita.dal.AiOptimizationDAO.IssueDTO;
import com.fpt.aita.dal.AiOptimizationDAO.MethodWithIssuesDTO;
import com.fpt.aita.model.AiOptimization;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Service duy nhất mà Người 3 (hàng đợi chấm bài) gọi vào sau khi AST Engine hoàn tất.
 * - Không phụ thuộc HttpServletRequest/Session: có thể chạy từ thread nền.
 * - Không gửi tên/mã sinh viên lên AI.
 * - AI hỏng không làm thay đổi SUBMISSIONS.status.
 * Tương thích Java 1.8.
 */
public class AiAnalysisService {

    // Danh sách mức độ phức tạp cần phân tích (từ O(N^2) trở lên)
    private static final List<String> COMPLEX_ENOUGH;
    private static final List<String> RUBRIC_METHODS;
    static {
        COMPLEX_ENOUGH = new ArrayList<String>();
        COMPLEX_ENOUGH.add("O(N^2)");
        COMPLEX_ENOUGH.add("O(N^3)");
        COMPLEX_ENOUGH.add("O(2^N)");
        COMPLEX_ENOUGH.add("O(N!)");

        RUBRIC_METHODS = new ArrayList<String>();
        RUBRIC_METHODS.add("isempty");
        RUBRIC_METHODS.add("size");
        RUBRIC_METHODS.add("addfirst");
        RUBRIC_METHODS.add("addlast");
        RUBRIC_METHODS.add("removefirst");
        RUBRIC_METHODS.add("removelast");
        RUBRIC_METHODS.add("get");
        RUBRIC_METHODS.add("getat");
        RUBRIC_METHODS.add("clear");
        RUBRIC_METHODS.add("traverse");
        RUBRIC_METHODS.add("print");
        RUBRIC_METHODS.add("addat");
        RUBRIC_METHODS.add("insertat");
        RUBRIC_METHODS.add("insertafter");
        RUBRIC_METHODS.add("search");
        RUBRIC_METHODS.add("contains");
        RUBRIC_METHODS.add("indexof");
        RUBRIC_METHODS.add("update");
        RUBRIC_METHODS.add("set");
        RUBRIC_METHODS.add("removebyvalue");
        RUBRIC_METHODS.add("removeat");
        RUBRIC_METHODS.add("count");
        RUBRIC_METHODS.add("max");
        RUBRIC_METHODS.add("min");
        RUBRIC_METHODS.add("reverse");
        RUBRIC_METHODS.add("findmiddle");
        RUBRIC_METHODS.add("removeduplicates");
        RUBRIC_METHODS.add("mergesorted");
        RUBRIC_METHODS.add("detectcycle");
        RUBRIC_METHODS.add("sort");
        RUBRIC_METHODS.add("removenthfromend");
        RUBRIC_METHODS.add("ispalindrome");
        RUBRIC_METHODS.add("findcyclestart");
        RUBRIC_METHODS.add("getintersection");
    }

    private final AiClient aiClient;
    private final AiConfig config;
    private final MethodCodeProvider codeProvider;
    private final AiOptimizationDAO dao;
    private final AnalysisReportDAO reportDAO;

    /**
     * Constructor chính — Người 3 sử dụng constructor này trong production.
     * Tự tạo AiClient từ AiConfig và FileMethodCodeProvider từ AI_EXTRACTED_ROOT.
     */
    public AiAnalysisService() {
        this.config = new AiConfig();
        this.aiClient = AiClientFactory.create(this.config);
        this.dao = new AiOptimizationDAO();
        this.reportDAO = new AnalysisReportDAO();
        String root = this.config.getExtractedRoot();
        if (root != null && !root.trim().isEmpty()) {
            this.codeProvider = new FileMethodCodeProvider(root);
        } else {
            // Fallback: không có thư mục giải nén thì trả code rỗng
            this.codeProvider = new MethodCodeProvider() {
                @Override
                public String getCode(int submissionId, String fileName, int startLine, int endLine) {
                    return "";
                }
            };
        }
    }

    /**
     * Constructor dành cho kiểm thử (inject từ ngoài client, provider, dao).
     */
    public AiAnalysisService(AiClient aiClient, AiConfig config, MethodCodeProvider codeProvider, AiOptimizationDAO dao) {
        this(aiClient, config, codeProvider, dao, new AnalysisReportDAO());
    }

    public AiAnalysisService(AiClient aiClient, AiConfig config, MethodCodeProvider codeProvider,
                             AiOptimizationDAO dao, AnalysisReportDAO reportDAO) {
        this.aiClient = aiClient;
        this.config = config;
        this.codeProvider = codeProvider;
        this.dao = dao;
        this.reportDAO = reportDAO;
    }

    /**
     * Phân tích tất cả các phương thức đủ điều kiện của một bài nộp.
     * Đây là hàm duy nhất mà Người 3 (hàng đợi chấm bài) gọi vào.
     *
     * @param submissionId ID bài nộp cần phân tích
     * @return AiRunSummary tổng kết kết quả
     */
    public AiRunSummary analyzeSubmission(int submissionId) {
        AiRunSummary summary = new AiRunSummary();
        String rubricType = PromptBuilder.normalizeRubricType(reportDAO.getRubricTypeBySubmissionId(submissionId));
        String assignmentPrompt = "NONE".equals(rubricType)
                ? null
                : reportDAO.getAssignmentPromptBySubmissionId(submissionId);

        // 1. Lấy danh sách method kèm issues của bài nộp
        List<MethodWithIssuesDTO> allMethods = dao.getMethodsWithIssues(submissionId);
        if (allMethods == null || allMethods.isEmpty()) {
            System.out.println("[AiAnalysisService] submissionId=" + submissionId + " không có method nào để phân tích.");
            if (!"NONE".equals(rubricType)) {
                dao.updateAnalysisReportQualityScore(submissionId, 0);
            }
            return summary;
        }

        // 2. Lọc: chỉ lấy method có lỗi Critical/Warning hoặc độ phức tạp O(N^2) trở lên
        List<MethodWithIssuesDTO> eligible = filterEligibleMethods(allMethods, rubricType);
        if (eligible.isEmpty()) {
            System.out.println("[AiAnalysisService] Không có method nào đủ điều kiện phân tích AI.");
            if (!"NONE".equals(rubricType)) {
                dao.updateAnalysisReportQualityScore(submissionId, 0);
            }
            return summary;
        }

        // 3. Ưu tiên method có lỗi Critical lên trước
        sortBySeverity(eligible);

        // 4. Giới hạn tối đa AI_MAX_METHODS_PER_SUBMISSION
        int maxMethods = config.getMaxMethodsPerSubmission();
        List<MethodWithIssuesDTO> toProcess = eligible.size() > maxMethods
                ? eligible.subList(0, maxMethods)
                : eligible;

        summary.setTotalMethods(toProcess.size());

        List<Double> aiScores = new ArrayList<Double>();
        double earnedPoints = 0.0;
        double maxPoints = 0.0;

        // 5. Xử lý từng method
        if ("LINKEDLIST".equals(rubricType) || "BST".equals(rubricType)) {
            for (MethodWithIssuesDTO method : eligible) {
                maxPoints += PromptBuilder.getMaxPoints(method.getMethodName(), rubricType);
            }
        }
        for (MethodWithIssuesDTO method : toProcess) {
            Double mScore = processMethod(submissionId, method, summary, rubricType, assignmentPrompt);
            if (mScore != null) {
                aiScores.add(mScore);
                double functionMax = PromptBuilder.getMaxPoints(method.getMethodName(), rubricType);
                earnedPoints += Math.max(0.0, Math.min(functionMax, mScore));
                if (!"LINKEDLIST".equals(rubricType) && !"BST".equals(rubricType)) {
                    maxPoints += functionMax;
                }
            }
        }

        // 6. Nếu có điểm đánh giá từ AI theo Rubric, cập nhật lại quality_score trong ANALYSIS_REPORTS
        if (!aiScores.isEmpty()) {
            int avgScore100 = maxPoints > 0.0
                    ? (int) Math.round(earnedPoints * 100.0 / maxPoints)
                    : 0;
            try {
                dao.updateAnalysisReportQualityScore(submissionId, avgScore100);
                System.out.println("[AiAnalysisService] Cập nhật quality_score theo Rubric AI: " + avgScore100 + "/100 cho bài nộp #" + submissionId);
            } catch (Exception ex) {
                System.err.println("[AiAnalysisService] Lỗi khi cập nhật quality_score theo AI: " + ex.getMessage());
            }
        } else if (!"NONE".equals(rubricType)) {
            dao.updateAnalysisReportQualityScore(submissionId, 0);
        }

        System.out.println("[AiAnalysisService] Hoàn tất submissionId=" + submissionId + " | " + summary);
        return summary;
    }

    /**
     * Xử lý phân tích một phương thức, bọc try/catch riêng để lỗi không lan rộng.
     * @return Điểm số do AI chấm theo rubric (nếu có), hoặc null
     */
    private Double processMethod(int submissionId, MethodWithIssuesDTO method, AiRunSummary summary, String rubricType,
                                 String assignmentPrompt) {
        int methodId = method.getMethodId();

        try {
            AiOptimization existing = dao.getByMethodId(methodId);
            if (isReusable(existing, rubricType)) {
                summary.incrementSkipped();
                return existing.getScore();
            }

            // Đọc mã nguồn của phương thức từ file đã giải nén
            String code = codeProvider.getCode(submissionId, method.getFileName(), method.getStartLine(), method.getEndLine());

            // Kiểm tra cache theo original_code nếu code không rỗng
            if (code != null && !code.trim().isEmpty()) {
                AiOptimization cached = dao.findByOriginalCode(code, rubricType);
                if (isReusable(cached, rubricType)) {
                    System.out.println("[AiAnalysisService] method_id=" + methodId + " hit cache -> dùng lại kết quả.");
                    // Lưu bản ghi mới với method_id này nhưng reuse nội dung tối ưu
                    AiOptimization reused = new AiOptimization(
                            methodId,
                            cached.getOriginalCode(),
                            cached.getOptimizedCode(),
                            cached.getExplanation()
                    );
                    reused.setScore(cached.getScore());
                    reused.setRubricType(rubricType);
                    reused.setComplexityAfter(cached.getComplexityAfter());
                    reused.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                    if (!dao.insertOptimization(reused)) {
                        summary.incrementFailed();
                        return null;
                    }
                    updateExplanationForIssues(method.getIssues(), cached.getExplanation());
                    summary.incrementFromCache();
                    return cached.getScore();
                }
            }

            // Xây dựng AiRequest (KHÔNG đưa tên/mã sinh viên vào)
            AiRequest req = buildRequest(method, code);
            req.setRubricType(rubricType);
            req.setAssignmentPrompt(assignmentPrompt);

            // Gọi AI
            AiResult result = aiClient.analyze(req);

            if (!result.isSuccess() || result.getExplanation() == null) {
                System.err.println("[AiAnalysisService] method_id=" + methodId + " AI trả kết quả không hợp lệ.");
                summary.incrementFailed();
                return null;
            }

            Double score = validScore(result.getScore());
            if (!"NONE".equals(rubricType)
                    && (method.getMethodName() == null
                    || !method.getMethodName().equals(result.getRequiredMethodName()))) {
                score = 0.0;
                System.out.println("[AiAnalysisService] method_id=" + methodId
                        + " sai tên hàm so với đề; điểm hàm được đặt về 0.");
            }

            // Lưu vào AI_OPTIMIZATIONS
            AiOptimization opt = new AiOptimization(
                    methodId,
                    code != null ? code : "",
                    result.getOptimizedCode() != null ? result.getOptimizedCode() : "",
                    result.getExplanation()
            );
            opt.setScore(score);
            opt.setRubricType(rubricType);
            opt.setComplexityAfter(result.getNewComplexity());
            opt.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            boolean saved = dao.insertOptimization(opt);
            if (!saved) {
                System.err.println("[AiAnalysisService] method_id=" + methodId + " ghi DB thất bại.");
                summary.incrementFailed();
                return null;
            }

            // Cập nhật ISSUES.ai_explanation cho từng lỗi của method này
            updateExplanationForIssues(method.getIssues(), result.getExplanation());
            summary.incrementAnalyzed();
            return opt.getScore();

        } catch (Exception e) {
            // Mỗi method bọc try/catch riêng: lỗi không làm dừng toàn bộ vòng lặp
            System.err.println("[AiAnalysisService] method_id=" + methodId + " xảy ra ngoại lệ: " + e.getMessage());
            summary.incrementFailed();
            return null;
        }
    }

    private static Double validScore(Double score) {
        return score != null && !score.isNaN() && !score.isInfinite() ? score : null;
    }

    private static boolean isReusable(AiOptimization value, String rubricType) {
        // Rubric scores are tied to the current prompt. Never reuse an older rubric
        // result after strict scoring rules have changed.
        if (!"NONE".equals(rubricType)) {
            return false;
        }
        return value != null && rubricType.equals(value.getRubricType()) && validScore(value.getScore()) != null;
    }

    /**
     * Cập nhật ai_explanation cho danh sách lỗi của phương thức.
     */
    private void updateExplanationForIssues(List<IssueDTO> issues, String explanation) {
        if (issues == null || issues.isEmpty() || explanation == null) {
            return;
        }
        for (IssueDTO issue : issues) {
            if (issue != null && issue.getIssueId() > 0) {
                dao.updateIssueAiExplanation(issue.getIssueId(), explanation);
            }
        }
    }

    /**
     * Lọc method đủ điều kiện phân tích:
     * - Có lỗi mức Critical hoặc Warning, HOẶC
     * - Độ phức tạp từ O(N^2) trở lên, HOẶC
     * - Thuộc danh sách hàm trong Rubric LinkedList cần AI chấm điểm.
     */
    private List<MethodWithIssuesDTO> filterEligibleMethods(List<MethodWithIssuesDTO> methods, String rubricType) {
        List<MethodWithIssuesDTO> eligible = new ArrayList<MethodWithIssuesDTO>();
        for (MethodWithIssuesDTO m : methods) {
            if (hasCriticalOrWarning(m) || isHighComplexity(m.getEstimatedComplexity()) || isRubricMethod(m.getMethodName(), rubricType)) {
                eligible.add(m);
            }
        }
        return eligible;
    }

    private boolean isRubricMethod(String methodName, String rubricType) {
        if (methodName == null || methodName.trim().isEmpty()) {
            return false;
        }
        String name = methodName.trim().toLowerCase(Locale.ROOT);
        if ("LINKEDLIST".equals(rubricType)) return RUBRIC_METHODS.contains(name);
        return "BST".equals(rubricType) && Arrays.asList("insert", "search", "delete", "traverse",
                "inorder", "preorder", "postorder", "height").contains(name);
    }

    /**
     * Kiểm tra method có lỗi Critical hoặc Warning không.
     */
    private boolean hasCriticalOrWarning(MethodWithIssuesDTO m) {
        if (m.getIssues() == null) {
            return false;
        }
        for (IssueDTO issue : m.getIssues()) {
            if (issue != null) {
                String sev = issue.getSeverity();
                if ("Critical".equalsIgnoreCase(sev) || "Warning".equalsIgnoreCase(sev)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Kiểm tra độ phức tạp từ O(N^2) trở lên.
     */
    private boolean isHighComplexity(String complexity) {
        if (complexity == null || complexity.trim().isEmpty()) {
            return false;
        }
        String normalized = complexity.trim().toUpperCase().replace(" ", "");
        for (String c : COMPLEX_ENOUGH) {
            if (normalized.equalsIgnoreCase(c.replace(" ", ""))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sắp xếp method theo ưu tiên: Critical trước, rồi Warning, rồi phức tạp cao.
     */
    private void sortBySeverity(List<MethodWithIssuesDTO> methods) {
        Collections.sort(methods, new Comparator<MethodWithIssuesDTO>() {
            @Override
            public int compare(MethodWithIssuesDTO a, MethodWithIssuesDTO b) {
                return getScore(b) - getScore(a); // Điểm cao hơn lên trước
            }

            private int getScore(MethodWithIssuesDTO m) {
                int score = 0;
                if (m.getIssues() != null) {
                    for (IssueDTO i : m.getIssues()) {
                        if (i == null) {
                            continue;
                        }
                        if ("Critical".equalsIgnoreCase(i.getSeverity())) {
                            score += 10;
                        } else if ("Warning".equalsIgnoreCase(i.getSeverity())) {
                            score += 5;
                        }
                    }
                }
                if (isHighComplexity(m.getEstimatedComplexity())) {
                    score += 3;
                }
                return score;
            }
        });
    }

    /**
     * Xây dựng AiRequest từ thông tin method.
     * KHÔNG đưa tên/mã sinh viên vào request.
     */
    private AiRequest buildRequest(MethodWithIssuesDTO method, String code) {
        AiRequest req = new AiRequest();
        req.setMethodName(method.getMethodName());
        req.setEstimatedComplexity(method.getEstimatedComplexity());
        req.setCode(code);
        if (method.getIssues() != null) {
            for (IssueDTO i : method.getIssues()) {
                if (i != null) {
                    req.addViolation(new AiRequest.RuleViolation(
                            i.getRuleCode(),
                            i.getSeverity(),
                            i.getMessage(),
                            i.getLineNumber()
                    ));
                }
            }
        }
        return req;
    }

    /**
     * Chạy phân tích một bài nộp theo cấu hình môi trường.
     */
    public static void main(String[] args) {
        AiConfig config = AiConfig.fromEnv();
        AiClient client = AiClientFactory.create(config);
        AiOptimizationDAO dao = new AiOptimizationDAO();
        int submissionId = 1;
        if (args != null && args.length > 0 && args[0] != null && !args[0].trim().isEmpty()) {
            try {
                submissionId = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException e) {
                System.err.println("submissionId phải là số nguyên dương.");
                dao.closeConnection();
                return;
            }
        }

        String extractedRoot = config.getExtractedRoot();
        MethodCodeProvider codeProvider;
        if (extractedRoot != null && !extractedRoot.trim().isEmpty()) {
            codeProvider = new FileMethodCodeProvider(extractedRoot);
        } else {
            codeProvider = new MethodCodeProvider() {
                @Override
                public String getCode(int id, String fileName, int startLine, int endLine) {
                    return "";
                }
            };
        }

        try {
            AiAnalysisService service = new AiAnalysisService(client, config, codeProvider, dao);
            AiRunSummary summary = service.analyzeSubmission(submissionId);
            System.out.println("AiRunSummary submissionId=" + submissionId + ": " + summary);
        } finally {
            dao.closeConnection();
        }
    }
}
