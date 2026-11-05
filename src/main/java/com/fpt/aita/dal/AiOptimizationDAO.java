package com.fpt.aita.dal;

import com.fpt.aita.model.AiOptimization;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data Access Object quản lý dữ liệu tối ưu hóa AI và thông tin method/issue liên quan.
 * Thừa kế DBContext, sử dụng PreparedStatement và try-with-resources.
 * Tương thích Java 1.8.
 */
public class AiOptimizationDAO extends DBContext {

    /**
     * DTO chứa thông tin phương thức và các lỗi/vi phạm phát hiện được.
     */
    public static class MethodWithIssuesDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        private int methodId;
        private int fileId;
        private String fileName;
        private String methodName;
        private int startLine;
        private int endLine;
        private String estimatedComplexity;
        private List<IssueDTO> issues = new ArrayList<IssueDTO>();

        public MethodWithIssuesDTO() {
        }

        public int getMethodId() {
            return methodId;
        }

        public void setMethodId(int methodId) {
            this.methodId = methodId;
        }

        public int getFileId() {
            return fileId;
        }

        public void setFileId(int fileId) {
            this.fileId = fileId;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public int getStartLine() {
            return startLine;
        }

        public void setStartLine(int startLine) {
            this.startLine = startLine;
        }

        public int getEndLine() {
            return endLine;
        }

        public void setEndLine(int endLine) {
            this.endLine = endLine;
        }

        public String getEstimatedComplexity() {
            return estimatedComplexity;
        }

        public void setEstimatedComplexity(String estimatedComplexity) {
            this.estimatedComplexity = estimatedComplexity;
        }

        public List<IssueDTO> getIssues() {
            return issues;
        }

        public void setIssues(List<IssueDTO> issues) {
            this.issues = (issues != null) ? issues : new ArrayList<IssueDTO>();
        }

        public void addIssue(IssueDTO issue) {
            if (issue != null) {
                this.issues.add(issue);
            }
        }

        @Override
        public String toString() {
            return "MethodWithIssuesDTO{" +
                    "methodId=" + methodId +
                    ", fileName='" + fileName + '\'' +
                    ", methodName='" + methodName + '\'' +
                    ", lines=" + startLine + "-" + endLine +
                    ", issuesCount=" + issues.size() +
                    '}';
        }
    }

    /**
     * DTO chi tiết một lỗi (issue) gắn với method.
     */
    public static class IssueDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        private int issueId;
        private String ruleCode;
        private String severity;
        private String message;
        private int lineNumber;

        public IssueDTO() {
        }

        public IssueDTO(int issueId, String ruleCode, String severity, String message, int lineNumber) {
            this.issueId = issueId;
            this.ruleCode = ruleCode;
            this.severity = severity;
            this.message = message;
            this.lineNumber = lineNumber;
        }

        public int getIssueId() {
            return issueId;
        }

        public void setIssueId(int issueId) {
            this.issueId = issueId;
        }

        public String getRuleCode() {
            return ruleCode;
        }

        public void setRuleCode(String ruleCode) {
            this.ruleCode = ruleCode;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        @Override
        public String toString() {
            return "IssueDTO{" +
                    "ruleCode='" + ruleCode + '\'' +
                    ", severity='" + severity + '\'' +
                    ", lineNumber=" + lineNumber +
                    '}';
        }
    }

    /**
     * Kiểm tra kết nối CSDL có sẵn sàng không.
     * Cung cấp cho các class bên ngoài package dal để tránh truy cập trực tiếp field protected.
     */
    public boolean isConnected() {
        return connection != null;
    }

    /**
     * Đóng kết nối CSDL an toàn.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Lỗi đóng kết nối: " + e.getMessage());
        }
    }

    /**
     * Cập nhật điểm chất lượng (quality_score) trong bảng ANALYSIS_REPORTS sau khi AI chấm điểm theo Rubric.
     */
    public boolean updateAnalysisReportQualityScore(int submissionId, int qualityScore) {
        if (connection == null) {
            return false;
        }
        String sql = "UPDATE \"ANALYSIS_REPORTS\" SET quality_score = ? WHERE submission_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, qualityScore);
            ps.setInt(2, submissionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi updateAnalysisReportQualityScore: " + e.getMessage());
        }
        return false;
    }

    /**
     * 1. Thêm mới bản ghi tối ưu hóa vào bảng AI_OPTIMIZATIONS.
     */
    public boolean insertOptimization(AiOptimization o) {
        if (o == null || connection == null) {
            return false;
        }
        String sql = "INSERT INTO \"AI_OPTIMIZATIONS\" (method_id, original_code, optimized_code, explanation, new_complexity, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, o.getMethodId());
            ps.setString(2, o.getOriginalCode());
            ps.setString(3, o.getOptimizedCode());
            ps.setString(4, o.getExplanation());
            ps.setString(5, o.getComplexityAfter());
            ps.setTimestamp(6, o.getCreatedAt() != null ? o.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi insertOptimization: " + e.getMessage());
        }
        return false;
    }

    /**
     * 2. Lấy bản ghi tối ưu hóa theo method_id.
     */
    public AiOptimization getByMethodId(int methodId) {
        if (connection == null) {
            return null;
        }
        String sql = "SELECT optimization_id, method_id, original_code, optimized_code, explanation, new_complexity, created_at "
                + "FROM \"AI_OPTIMIZATIONS\" WHERE method_id = ? ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, methodId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToOptimization(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi getByMethodId: " + e.getMessage());
        }
        return null;
    }

    /**
     * 2. Lấy danh sách tối ưu hóa theo submission_id (JOIN METHODS -> SOURCE_FILES).
     */
    public List<AiOptimization> getBySubmissionId(int submissionId) {
        List<AiOptimization> list = new ArrayList<AiOptimization>();
        if (connection == null) {
            return list;
        }
        String sql = "SELECT o.optimization_id, o.method_id, o.original_code, o.optimized_code, o.explanation, o.new_complexity, o.created_at "
                + "FROM \"AI_OPTIMIZATIONS\" o "
                + "INNER JOIN \"METHODS\" m ON o.method_id = m.method_id "
                + "INNER JOIN \"SOURCE_FILES\" sf ON m.file_id = sf.file_id "
                + "WHERE sf.submission_id = ? "
                + "ORDER BY o.created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, submissionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToOptimization(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi getBySubmissionId: " + e.getMessage());
        }
        return list;
    }

    /**
     * 3. Tìm bản ghi mới nhất có original_code giống hệt (dùng làm bộ nhớ đệm cache).
     */
    public AiOptimization findByOriginalCode(String code) {
        if (code == null || code.trim().isEmpty() || connection == null) {
            return null;
        }
        String sql = "SELECT optimization_id, method_id, original_code, optimized_code, explanation, new_complexity, created_at "
                + "FROM \"AI_OPTIMIZATIONS\" "
                + "WHERE CAST(original_code AS TEXT) = CAST(? AS TEXT) "
                + "ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToOptimization(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi findByOriginalCode: " + e.getMessage());
        }
        return null;
    }

    /**
     * 4. Cập nhật giải thích từ AI vào bảng ISSUES.ai_explanation.
     */
    public boolean updateIssueAiExplanation(int issueId, String text) {
        if (connection == null) {
            return false;
        }
        String sql = "UPDATE \"ISSUES\" SET ai_explanation = ? WHERE issue_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, text);
            ps.setInt(2, issueId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi updateIssueAiExplanation: " + e.getMessage());
        }
        return false;
    }

    /**
     * 5. Lấy danh sách method kèm file_name, start_line, end_line, estimated_complexity
     * và danh sách các issue (rule_code, severity, message, line_number).
     */
    public List<MethodWithIssuesDTO> getMethodsWithIssues(int submissionId) {
        List<MethodWithIssuesDTO> methods = new ArrayList<MethodWithIssuesDTO>();
        if (connection == null) {
            return methods;
        }

        // Lấy danh sách các phương thức thuộc bài nộp
        String sqlMethods = "SELECT m.method_id, m.file_id, sf.file_name, m.method_name, "
                + "m.start_line, m.end_line, m.estimated_complexity "
                + "FROM \"METHODS\" m "
                + "INNER JOIN \"SOURCE_FILES\" sf ON m.file_id = sf.file_id "
                + "WHERE sf.submission_id = ? "
                + "ORDER BY sf.file_id, m.start_line";

        try (PreparedStatement psMethod = connection.prepareStatement(sqlMethods)) {
            psMethod.setInt(1, submissionId);
            try (ResultSet rsMethod = psMethod.executeQuery()) {
                while (rsMethod.next()) {
                    MethodWithIssuesDTO dto = new MethodWithIssuesDTO();
                    dto.setMethodId(rsMethod.getInt("method_id"));
                    dto.setFileId(rsMethod.getInt("file_id"));
                    dto.setFileName(rsMethod.getString("file_name"));
                    dto.setMethodName(rsMethod.getString("method_name"));
                    dto.setStartLine(rsMethod.getInt("start_line"));
                    dto.setEndLine(rsMethod.getInt("end_line"));
                    dto.setEstimatedComplexity(rsMethod.getString("estimated_complexity"));
                    methods.add(dto);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi getMethodsWithIssues (lấy methods): " + e.getMessage());
            return methods;
        }

        // Lấy danh sách các issue gắn với từng method
        if (!methods.isEmpty()) {
            String sqlIssues = "SELECT i.issue_id, i.method_id, r.rule_code, i.severity, i.message, i.line_number "
                    + "FROM \"ISSUES\" i "
                    + "INNER JOIN \"RULES\" r ON i.rule_id = r.rule_id "
                    + "INNER JOIN \"SOURCE_FILES\" sf ON i.file_id = sf.file_id "
                    + "WHERE sf.submission_id = ? AND i.method_id IS NOT NULL";

            try (PreparedStatement psIssue = connection.prepareStatement(sqlIssues)) {
                psIssue.setInt(1, submissionId);
                try (ResultSet rsIssue = psIssue.executeQuery()) {
                    while (rsIssue.next()) {
                        int mid = rsIssue.getInt("method_id");
                        IssueDTO issue = new IssueDTO(
                                rsIssue.getInt("issue_id"),
                                rsIssue.getString("rule_code"),
                                rsIssue.getString("severity"),
                                rsIssue.getString("message"),
                                rsIssue.getInt("line_number")
                        );

                        // Gán vào method tương ứng
                        for (MethodWithIssuesDTO m : methods) {
                            if (m.getMethodId() == mid) {
                                m.addIssue(issue);
                                break;
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Lỗi getMethodsWithIssues (lấy issues): " + e.getMessage());
            }
        }

        return methods;
    }

    /**
     * 6. Kiểm tra xem đã có bản ghi tối ưu hóa cho method này chưa (tránh ghi trùng).
     */
    public boolean existsForMethod(int methodId) {
        if (connection == null) {
            return false;
        }
        String sql = "SELECT 1 FROM \"AI_OPTIMIZATIONS\" WHERE method_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, methodId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Lỗi existsForMethod: " + e.getMessage());
        }
        return false;
    }

    /**
     * Ánh xạ từ dòng ResultSet sang đối tượng AiOptimization.
     */
    private AiOptimization mapResultSetToOptimization(ResultSet rs) throws SQLException {
        AiOptimization o = new AiOptimization();
        o.setOptimizationId(rs.getInt("optimization_id"));
        o.setMethodId(rs.getInt("method_id"));
        o.setOriginalCode(rs.getString("original_code"));
        o.setOptimizedCode(rs.getString("optimized_code"));
        o.setExplanation(rs.getString("explanation"));
        try {
            o.setComplexityAfter(rs.getString("new_complexity"));
        } catch (SQLException ignored) {}
        o.setCreatedAt(rs.getTimestamp("created_at"));
        return o;
    }

    /**
     * Hàm main() kiểm tra với dữ liệu mẫu trong DB.
     */
    public static void main(String[] args) {
        System.out.println("=== KIỂM THỬ AiOptimizationDAO VỚI CƠ SỞ DỮ LIỆU ===");
        AiOptimizationDAO dao = new AiOptimizationDAO();

        if (dao.connection == null) {
            System.err.println("Không thể kết nối PostgreSQL. Kiểm tra DB_URL, DB_USER, DB_PASSWORD và dịch vụ database.");
            return;
        }

        try {
            // 1. Kiểm tra đọc methods và issues theo submission_id = 1 (file_id 1 và 2)
            int testSubmissionId = 1;
            System.out.println("\n1. Đọc methods kèm issues của submission_id = " + testSubmissionId);
            List<MethodWithIssuesDTO> methods = dao.getMethodsWithIssues(testSubmissionId);
            System.out.println("-> Tìm thấy: " + methods.size() + " method(s)");
            for (MethodWithIssuesDTO m : methods) {
                System.out.println("   + Method [" + m.getMethodId() + "]: " + m.getMethodName()
                        + " (" + m.getEstimatedComplexity() + ") trong file '" + m.getFileName()
                        + "' dòng " + m.getStartLine() + "-" + m.getEndLine()
                        + " - Số lỗi: " + m.getIssues().size());
                for (IssueDTO i : m.getIssues()) {
                    System.out.println("      - Lỗi [" + i.getRuleCode() + "][" + i.getSeverity() + "]: " + i.getMessage());
                }
            }

            // 2. Kiểm tra existsForMethod
            int testMethodId = 5; // Method hasDuplicateItems từ seed data
            boolean existsBefore = dao.existsForMethod(testMethodId);
            System.out.println("\n2. Kiểm tra tồn tại method_id = " + testMethodId + ": " + existsBefore);

            // 3. Đọc theo submission_id
            List<AiOptimization> listBySub = dao.getBySubmissionId(testSubmissionId);
            System.out.println("\n3. Số bản ghi \"AI_OPTIMIZATIONS\" của submission_id " + testSubmissionId + ": " + listBySub.size());

            // 4. Thử thêm một bản ghi mới (sử dụng method_id = 1 từ seed data nếu chưa có)
            int insertMethodId = 1;
            System.out.println("\n4. Kiểm tra thêm bản ghi mới cho method_id = " + insertMethodId);
            if (!dao.existsForMethod(insertMethodId)) {
                AiOptimization newOpt = new AiOptimization(
                        insertMethodId,
                        "void sort(int[] a) { /* bubble sort */ }",
                        "void sort(int[] a) { Arrays.sort(a); }",
                        "Tối ưu từ O(N^2) sang O(N log N) bằng thuật toán Dual-Pivot Quicksort."
                );
                boolean inserted = dao.insertOptimization(newOpt);
                System.out.println("-> Thêm mới thành công: " + inserted);
            } else {
                System.out.println("-> Method " + insertMethodId + " đã có bản ghi tối ưu, bỏ qua insert để tránh trùng.");
            }

            // 5. Đọc lại bản ghi vừa thêm hoặc đã có
            AiOptimization fetched = dao.getByMethodId(insertMethodId);
            System.out.println("\n5. Đọc lại theo method_id = " + insertMethodId + ":");
            if (fetched != null) {
                System.out.println("-> ID: " + fetched.getOptimizationId());
                System.out.println("-> Giải thích: " + fetched.getExplanation());
                System.out.println("-> Code tối ưu:\n" + fetched.getOptimizedCode());
            }

            // 6. Kiểm tra tìm kiếm theo original_code (cache)
            if (fetched != null) {
                System.out.println("\n6. Kiểm tra cache findByOriginalCode:");
                AiOptimization cached = dao.findByOriginalCode(fetched.getOriginalCode());
                System.out.println("-> Tìm thấy bản ghi cache: " + (cached != null && cached.getOptimizationId() == fetched.getOptimizationId()));
            }

        } finally {
            try {
                if (dao.connection != null && !dao.connection.isClosed()) {
                    dao.connection.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }
}
