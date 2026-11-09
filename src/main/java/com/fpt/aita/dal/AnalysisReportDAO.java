package com.fpt.aita.dal;

import java.sql.Connection;

import com.fpt.aita.model.AiOptimization;
import com.fpt.aita.model.AnalysisReport;
import com.fpt.aita.model.Issue;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object phụ trách truy xuất dữ liệu Báo Cáo Phân Tích (ANALYSIS_REPORTS),
 * Danh sách lỗi tĩnh (ISSUES) và Tối ưu hóa thuật toán AI (AI_OPTIMIZATIONS).
 */
public class AnalysisReportDAO extends DBContext {

    /**
     * Lấy báo cáo phân tích theo submission_id kèm siêu dữ liệu bài nộp
     */
    public AnalysisReport getBySubmissionId(int submissionId) {
        String sql = "SELECT ar.report_id, ar.submission_id, ar.quality_score, ar.total_issues, ar.created_at, "
                   + "       s.student_id, u.full_name AS student_name, u.username AS student_username, "
                   + "       s.assignment_id, a.title AS assignment_title, s.file_path, s.file_size, s.submitted_at, s.status AS submission_status "
                   + "FROM \"ANALYSIS_REPORTS\" ar "
                   + "JOIN \"SUBMISSIONS\" s ON ar.submission_id = s.submission_id "
                   + "JOIN \"USERS\" u ON s.student_id = u.user_id "
                   + "LEFT JOIN \"ASSIGNMENTS\" a ON s.assignment_id = a.assignment_id "
                   + "WHERE ar.submission_id = ?";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, submissionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AnalysisReport r = mapAnalysisReport(rs);
                    loadExtraStatistics(r);
                    return r;
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi getBySubmissionId: " + ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy báo cáo phân tích theo report_id
     */
    public AnalysisReport getReportById(int reportId) {
        String sql = "SELECT ar.report_id, ar.submission_id, ar.quality_score, ar.total_issues, ar.created_at, "
                   + "       s.student_id, u.full_name AS student_name, u.username AS student_username, "
                   + "       s.assignment_id, a.title AS assignment_title, s.file_path, s.file_size, s.submitted_at, s.status AS submission_status "
                   + "FROM \"ANALYSIS_REPORTS\" ar "
                   + "JOIN \"SUBMISSIONS\" s ON ar.submission_id = s.submission_id "
                   + "JOIN \"USERS\" u ON s.student_id = u.user_id "
                   + "LEFT JOIN \"ASSIGNMENTS\" a ON s.assignment_id = a.assignment_id "
                   + "WHERE ar.report_id = ?";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AnalysisReport r = mapAnalysisReport(rs);
                    loadExtraStatistics(r);
                    return r;
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi getReportById: " + ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy danh sách lỗi/cảnh báo (ISSUES) theo report_id
     * Join với RULES, METHODS, SOURCE_FILES để lấy đầy đủ tên luật, tên hàm và dòng lỗi
     */
    public List<Issue> getIssues(int reportId) {
        List<Issue> list = new ArrayList<>();
        String sql = "SELECT i.issue_id, i.file_id, sf.file_name, i.method_id, m.method_name, "
                   + "       i.rule_id, r.rule_code, r.name AS rule_name, r.category, "
                   + "       i.line_number, i.severity, i.message, i.ai_explanation "
                   + "FROM \"ISSUES\" i "
                   + "JOIN \"SOURCE_FILES\" sf ON i.file_id = sf.file_id "
                   + "JOIN \"ANALYSIS_REPORTS\" ar ON sf.submission_id = ar.submission_id "
                   + "JOIN \"RULES\" r ON i.rule_id = r.rule_id "
                   + "LEFT JOIN \"METHODS\" m ON i.method_id = m.method_id "
                   + "WHERE ar.report_id = ? "
                   + "ORDER BY "
                   + "    CASE i.severity "
                   + "        WHEN 'Critical' THEN 1 "
                   + "        WHEN 'Warning' THEN 2 "
                   + "        WHEN 'Info' THEN 3 "
                   + "        ELSE 4 "
                   + "    END, "
                   + "    i.line_number ASC";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapIssue(rs));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi getIssues: " + ex.getMessage());
            ex.printStackTrace();
        }
        return list;
    }

    /**
     * Lấy danh sách lỗi/cảnh báo (ISSUES) theo submission_id
     */
    public List<Issue> getIssuesBySubmissionId(int submissionId) {
        List<Issue> list = new ArrayList<>();
        String sql = "SELECT i.issue_id, i.file_id, sf.file_name, i.method_id, m.method_name, "
                   + "       i.rule_id, r.rule_code, r.name AS rule_name, r.category, "
                   + "       i.line_number, i.severity, i.message, i.ai_explanation "
                   + "FROM \"ISSUES\" i "
                   + "JOIN \"SOURCE_FILES\" sf ON i.file_id = sf.file_id "
                   + "JOIN \"RULES\" r ON i.rule_id = r.rule_id "
                   + "LEFT JOIN \"METHODS\" m ON i.method_id = m.method_id "
                   + "WHERE sf.submission_id = ? "
                   + "ORDER BY "
                   + "    CASE i.severity "
                   + "        WHEN 'Critical' THEN 1 "
                   + "        WHEN 'Warning' THEN 2 "
                   + "        WHEN 'Info' THEN 3 "
                   + "        ELSE 4 "
                   + "    END, "
                   + "    i.line_number ASC";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, submissionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapIssue(rs));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi getIssuesBySubmissionId: " + ex.getMessage());
            ex.printStackTrace();
        }
        return list;
    }

    /**
     * Lấy danh sách đề xuất tối ưu AI (AI_OPTIMIZATIONS) theo report_id
     */
    public List<AiOptimization> getOptimizations(int reportId) {
        List<AiOptimization> list = new ArrayList<>();
        String sql = "SELECT o.optimization_id, o.method_id, m.method_name, sf.file_name, "
                   + "       o.original_code, o.optimized_code, o.explanation, o.created_at, "
                   + "       m.estimated_complexity, o.new_complexity "
                   + "FROM \"AI_OPTIMIZATIONS\" o "
                   + "JOIN \"METHODS\" m ON o.method_id = m.method_id "
                   + "JOIN \"SOURCE_FILES\" sf ON m.file_id = sf.file_id "
                   + "JOIN \"ANALYSIS_REPORTS\" ar ON sf.submission_id = ar.submission_id "
                   + "WHERE ar.report_id = ? "
                   + "ORDER BY o.optimization_id ASC";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AiOptimization opt = new AiOptimization(
                        rs.getInt("optimization_id"),
                        rs.getInt("method_id"),
                        rs.getString("method_name"),
                        rs.getString("file_name"),
                        rs.getString("original_code"),
                        rs.getString("optimized_code"),
                        rs.getString("explanation"),
                        rs.getTimestamp("created_at")
                    );
                    opt.setComplexityBefore(rs.getString("estimated_complexity"));
                    // new_complexity do AI ghi; có thể NULL nếu AI chưa trả về -> JSP hiển thị "—"
                    opt.setComplexityAfter(rs.getString("new_complexity"));
                    list.add(opt);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi getOptimizations: " + ex.getMessage());
            ex.printStackTrace();
        }
        return list;
    }

    /**
     * Lấy bản ghi tối ưu đầu tiên của báo cáo (dùng cho view cơ bản theo đúng đặc tả Bước 1)
     */
    public AiOptimization getOptimization(int reportId) {
        List<AiOptimization> list = getOptimizations(reportId);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    /**
     * Kiểm tra xem bài nộp đã có báo cáo phân tích hay chưa
     */
    public boolean hasReport(int submissionId) {
        String sql = "SELECT 1 FROM \"ANALYSIS_REPORTS\" WHERE submission_id = ?";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, submissionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi hasReport: " + ex.getMessage());
        }
        return false;
    }

    /**
     * Lấy danh đồ trạng thái có báo cáo hay chưa theo từng submissionId trong môn học
     */
    public Map<Integer, Boolean> getReportsStatusMapByCourse(int courseId) {
        Map<Integer, Boolean> map = new HashMap<>();
        String sql = "SELECT s.submission_id, CASE WHEN ar.report_id IS NOT NULL THEN 1 ELSE 0 END AS has_report "
                   + "FROM \"SUBMISSIONS\" s "
                   + "JOIN \"ASSIGNMENTS\" a ON s.assignment_id = a.assignment_id "
                   + "LEFT JOIN \"ANALYSIS_REPORTS\" ar ON s.submission_id = ar.submission_id "
                   + "WHERE a.course_id = ?";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getInt("submission_id"), rs.getInt("has_report") == 1);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi getReportsStatusMapByCourse: " + ex.getMessage());
        }
        return map;
    }

    /**
     * Nạp các thống kê số lượng lỗi theo từng mức độ và tổng dòng code (LOC)
     */
    private void loadExtraStatistics(AnalysisReport r) {
        if (r == null) return;
        String sqlIssues = "SELECT "
                         + "    COUNT(CASE WHEN i.severity = 'Critical' THEN 1 END) AS crit_cnt, "
                         + "    COUNT(CASE WHEN i.severity = 'Warning' THEN 1 END) AS warn_cnt, "
                         + "    COUNT(CASE WHEN i.severity = 'Info' THEN 1 END) AS info_cnt "
                         + "FROM \"ISSUES\" i "
                         + "JOIN \"SOURCE_FILES\" sf ON i.file_id = sf.file_id "
                         + "WHERE sf.submission_id = ?";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(sqlIssues)) {
            ps.setInt(1, r.getSubmissionId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    r.setCriticalCount(rs.getInt("crit_cnt"));
                    r.setWarningCount(rs.getInt("warn_cnt"));
                    r.setInfoCount(rs.getInt("info_cnt"));
                }
            }
        } catch (SQLException ignored) {}

        String sqlLoc = "SELECT COALESCE(SUM(loc), 0) AS total_loc FROM \"SOURCE_FILES\" WHERE submission_id = ?";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(sqlLoc)) {
            ps.setInt(1, r.getSubmissionId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    r.setLoc(rs.getInt("total_loc"));
                }
            }
        } catch (SQLException ignored) {}
    }

    private AnalysisReport mapAnalysisReport(ResultSet rs) throws SQLException {
        AnalysisReport r = new AnalysisReport();
        r.setReportId(rs.getInt("report_id"));
        r.setSubmissionId(rs.getInt("submission_id"));
        r.setQualityScore(rs.getInt("quality_score"));
        r.setTotalIssues(rs.getInt("total_issues"));
        r.setCreatedAt(rs.getTimestamp("created_at"));

        r.setStudentId(rs.getInt("student_id"));
        r.setStudentName(rs.getString("student_name"));
        r.setStudentUsername(rs.getString("student_username"));
        r.setAssignmentId(rs.getInt("assignment_id"));
        r.setAssignmentTitle(rs.getString("assignment_title"));
        r.setFilePath(rs.getString("file_path"));
        r.setFileSize(rs.getInt("file_size"));
        r.setSubmittedAt(rs.getTimestamp("submitted_at"));
        r.setSubmissionStatus(rs.getString("submission_status"));
        return r;
    }

    private Issue mapIssue(ResultSet rs) throws SQLException {
        Issue i = new Issue();
        i.setIssueId(rs.getInt("issue_id"));
        i.setFileId(rs.getInt("file_id"));
        i.setFileName(rs.getString("file_name"));
        int mId = rs.getInt("method_id");
        if (!rs.wasNull()) {
            i.setMethodId(mId);
        }
        i.setMethodName(rs.getString("method_name"));
        i.setRuleId(rs.getInt("rule_id"));
        i.setRuleCode(rs.getString("rule_code"));
        i.setRuleName(rs.getString("rule_name"));
        i.setCategory(rs.getString("category"));
        int line = rs.getInt("line_number");
        if (!rs.wasNull()) {
            i.setLineNumber(line);
        }
        i.setSeverity(rs.getString("severity"));
        i.setMessage(rs.getString("message"));
        i.setAiExplanation(rs.getString("ai_explanation"));
        return i;
    }

    /**
     * Dữ liệu giả định mẫu (Mock Report) phục vụ kiểm thử và demo nếu người 1 và 2 chưa kịp nạp vào DB
     */
    public AnalysisReport getMockReport(int submissionId) {
        AnalysisReport report = new AnalysisReport(999, submissionId, 78, 3, new Timestamp(System.currentTimeMillis()));
        report.setStudentName("Nguyễn Văn An");
        report.setStudentUsername("HE180001");
        report.setAssignmentTitle("Lab 1: Thao Tác Mảng & Cấu Trúc Dữ Liệu");
        report.setFileName("ArrayDuplicateFinder.java");
        report.setFileSize(45200);
        report.setSubmittedAt(new Timestamp(System.currentTimeMillis() - 3600000));
        report.setSubmissionStatus("Completed");
        report.setCriticalCount(1);
        report.setWarningCount(1);
        report.setInfoCount(1);
        report.setLoc(120);
        return report;
    }

    public List<Issue> getMockIssues() {
        List<Issue> list = new ArrayList<>();
        list.add(new Issue(1, 1, "ArrayDuplicateFinder.java", 5, "hasDuplicateItems", 
            1, "RULE_PERF_NESTED_LOOP", "Vòng lặp lồng nhau gây độ phức tạp O(N^2)", "Performance", 
            25, "Warning", "Tìm trùng lặp phần tử bằng 2 vòng lặp vét cạn O(N^2)", 
            "Phương thức so sánh từng cặp phần tử bằng 2 vòng for lồng nhau. Bạn nên dùng cấu trúc dữ liệu HashSet để kiểm tra sự tồn tại trong thời gian O(1), đưa độ phức tạp toàn bộ hàm về O(N)."));

        list.add(new Issue(2, 1, "ArrayDuplicateFinder.java", 7, "checkLoginUnsafe", 
            3, "RULE_SEC_SQL_INJECTION", "Nguy cơ tấn công SQL Injection", "Security", 
            52, "Critical", "Ghép trực tiếp biến username vào chuỗi SQL gây nguy cơ SQL Injection", 
            "Sử dụng String concatenation trong câu truy vấn 'SELECT * FROM Users WHERE username = ...' cho phép kẻ tấn công chèn mã độc. Bắt buộc dùng PreparedStatement với dấu hỏi chấm (?)."));

        list.add(new Issue(3, 1, "ArrayDuplicateFinder.java", 8, "getAllUsersLeak", 
            4, "RULE_SMELL_RESOURCE_LEAK", "Không đóng tài nguyên kết nối Database", "Code Smells", 
            110, "Info", "Không đóng Connection và ResultSet trong khối try-with-resources", 
            "Kết nối JDBC không được đóng đúng cách sẽ gây rò rỉ bộ nhớ (memory leak) và cạn kiệt connection pool của PostgreSQL khi có nhiều request đồng thời."));
        return list;
    }

    public AiOptimization getMockOptimization() {
        AiOptimization opt = new AiOptimization(
            1, 5, "hasDuplicateItems", "ArrayDuplicateFinder.java",
            "public boolean hasDuplicateItems(int[] arr) {\n" +
            "    for (int i = 0; i < arr.length; i++) {\n" +
            "        for (int j = i + 1; j < arr.length; j++) {\n" +
            "            if (arr[i] == arr[j]) return true;\n" +
            "        } \n" +
            "    }\n" +
            "    return false;\n" +
            "}",
            "public boolean hasDuplicateItems(int[] arr) {\n" +
            "    Set<Integer> seen = new HashSet<>();\n" +
            "    for (int num : arr) {\n" +
            "        if (!seen.add(num)) return true;\n" +
            "    }\n" +
            "    return false;\n" +
            "}",
            "AI đã tối ưu độ phức tạp từ O(N^2) xuống O(N) nhờ sử dụng bảng băm (HashSet). Thao tác add và contains trên HashSet chỉ mất O(1), giúp tiết kiệm thời gian đáng kể khi mảng có hàng triệu phần tử.",
            new Timestamp(System.currentTimeMillis())
        );
        opt.setComplexityBefore("O(N^2)");
        opt.setComplexityAfter("O(N)");
        return opt;
    }

    // =========================================================================
    // PHAN INSERT DU LIEU BOC TACH AST VA BO LUAT RULES (THANH VIEN 1)
    // =========================================================================

    public int insertSourceFile(int submissionId, String fileName, int loc) {
        String sql = "INSERT INTO \"SOURCE_FILES\" (submission_id, file_name, file_type, loc) VALUES (?, ?, 'java', ?)";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, submissionId);
            ps.setString(2, fileName);
            ps.setInt(3, loc);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Loi insertSourceFile: " + e.getMessage());
        }
        return -1;
    }

    public int insertMethod(int fileId, String methodName, int startLine, int endLine,
                            int loopDepth, int cyclomaticComplexity, String estimatedComplexity, boolean isRecursive) {
        String sql = "INSERT INTO \"METHODS\" (file_id, method_name, start_line, end_line, loop_depth, "
                   + "cyclomatic_complexity, estimated_complexity, is_recursive) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, fileId);
            ps.setString(2, methodName);
            ps.setInt(3, startLine);
            ps.setInt(4, endLine);
            ps.setInt(5, loopDepth);
            ps.setInt(6, cyclomaticComplexity);
            ps.setString(7, estimatedComplexity);
            ps.setBoolean(8, isRecursive);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Loi insertMethod: " + e.getMessage());
        }
        return -1;
    }

    public int getRuleIdByCode(String ruleCode) {
        String sql = "SELECT rule_id FROM \"RULES\" WHERE rule_code = ?";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ruleCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("rule_id");
            }
        } catch (SQLException e) {
            System.err.println("Loi getRuleIdByCode: " + e.getMessage());
        }
        return -1;
    }

    public String getRubricTypeBySubmissionId(int submissionId) {
        String sql = "SELECT a.rubric_type "
                   + "FROM \"ASSIGNMENTS\" a "
                   + "JOIN \"SUBMISSIONS\" s ON a.assignment_id = s.assignment_id "
                   + "WHERE s.submission_id = ?";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, submissionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String rubric = rs.getString("rubric_type");
                    if (rubric != null && !rubric.trim().isEmpty()) {
                        return rubric.trim();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Loi getRubricTypeBySubmissionId: " + e.getMessage());
        }
        return "NONE";
    }

    public String getAssignmentPromptBySubmissionId(int submissionId) {
        String sql = "SELECT a.title, a.description "
                   + "FROM \"ASSIGNMENTS\" a "
                   + "JOIN \"SUBMISSIONS\" s ON a.assignment_id = s.assignment_id "
                   + "WHERE s.submission_id = ?";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, submissionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String title = rs.getString("title");
                    String description = rs.getString("description");
                    return (title == null ? "" : title) + "\n" + (description == null ? "" : description);
                }
            }
        } catch (SQLException e) {
            System.err.println("Loi getAssignmentPromptBySubmissionId: " + e.getMessage());
        }
        return "";
    }

    public boolean insertIssue(int fileId, Integer methodId, int ruleId, int lineNumber,
                               String severity, String message, String aiExplanation) {
        if (ruleId <= 0) {
            return false;
        }
        String sql = "INSERT INTO \"ISSUES\" (file_id, method_id, rule_id, line_number, severity, message, ai_explanation) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            if (methodId != null && methodId > 0) {
                ps.setInt(2, methodId);
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setInt(3, ruleId);
            ps.setInt(4, lineNumber);
            ps.setString(5, severity);
            ps.setString(6, message);
            ps.setString(7, aiExplanation);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Loi insertIssue: " + e.getMessage());
        }
        return false;
    }

    public int insertAnalysisReport(int submissionId, int qualityScore, int totalIssues) {
        String deleteSql = "DELETE FROM \"ANALYSIS_REPORTS\" WHERE submission_id = ?";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(deleteSql)) {
            ps.setInt(1, submissionId);
            ps.executeUpdate();
        } catch (SQLException ignored) {}

        String sql = "INSERT INTO \"ANALYSIS_REPORTS\" (submission_id, quality_score, total_issues, created_at) "
                   + "VALUES (?, ?, ?, LOCALTIMESTAMP)";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, submissionId);
            ps.setInt(2, qualityScore);
            ps.setInt(3, totalIssues);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Loi insertAnalysisReport: " + e.getMessage());
        }
        return -1;
    }

    public void cleanOldAnalysisData(int submissionId) {
        try (Connection connection = openConnection(); PreparedStatement ps1 = connection.prepareStatement("DELETE FROM \"SOURCE_FILES\" WHERE submission_id = ?");
             PreparedStatement ps2 = connection.prepareStatement("DELETE FROM \"ANALYSIS_REPORTS\" WHERE submission_id = ?")) {
            ps1.setInt(1, submissionId);
            ps1.executeUpdate();
            ps2.setInt(1, submissionId);
            ps2.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Loi cleanOldAnalysisData: " + e.getMessage());
        }
    }
}
