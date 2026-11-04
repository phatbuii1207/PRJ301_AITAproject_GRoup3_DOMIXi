package com.fpt.aita.dal;

import com.fpt.aita.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** One short-lived connection per request/job; never shared between queue and servlet threads. */
public final class GradingDAO implements AutoCloseable {
    private final Connection connection;
    public GradingDAO() throws SQLException { this(new DBContext().connection); }
    public GradingDAO(Connection connection) throws SQLException {
        if (connection == null || connection.isClosed()) throw new SQLException("Không kết nối được CSDL chấm bài.");
        this.connection = connection;
    }

    public Assignment findAssignment(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM \"ASSIGNMENTS\" WHERE assignment_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Assignment a = new Assignment();
                a.setAssignmentId(id); a.setLecturerId(rs.getInt("lecturer_id")); a.setCourseId(rs.getInt("course_id"));
                a.setTitle(rs.getString("title")); a.setDeadline(rs.getTimestamp("deadline"));
                return a;
            }
        }
    }

    public Submission findSubmission(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM \"SUBMISSIONS\" WHERE submission_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Submission s = new Submission();
                s.setSubmissionId(id); s.setStudentId(rs.getInt("student_id")); s.setAssignmentId(rs.getInt("assignment_id"));
                s.setFilePath(rs.getString("file_path")); s.setSha256Hash(rs.getString("sha256_hash"));
                s.setStatus(rs.getString("status")); return s;
            }
        }
    }

    public boolean isCourseMember(int userId, int courseId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM \"USER_CLASS\" WHERE user_id=? AND class_id=?")) {
            ps.setInt(1, userId); ps.setInt(2, courseId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public GradingConfig loadConfig(int assignmentId) throws SQLException {
        String sql = "SELECT c.main_class,c.config_version,t.* FROM \"GRADING_CONFIGS\" c "
                + "LEFT JOIN \"GRADING_TEST_CASES\" t ON c.assignment_id=t.assignment_id WHERE c.assignment_id=? ORDER BY t.case_order";
        List<GradingTestCase> tests = new ArrayList<>();
        String main = null; int version = 0;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, assignmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    main = rs.getString("main_class"); version = rs.getInt("config_version");
                    if (rs.getObject("case_order") != null) tests.add(testFrom(rs));
                }
            }
        }
        return tests.isEmpty() ? null : new GradingConfig(main, version, tests);
    }

    public void saveConfig(int assignmentId, GradingConfig config) throws SQLException {
        connection.setAutoCommit(false);
        try {
            // PostgreSQL row locks cannot lock a missing config row. Serialize initial saves too.
            try (PreparedStatement lock = connection.prepareStatement("SELECT pg_advisory_xact_lock(301, ?)")) {
                lock.setInt(1, assignmentId); lock.execute();
            }
            int version = 0;
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT config_version FROM \"GRADING_CONFIGS\" WHERE assignment_id=? FOR UPDATE")) {
                ps.setInt(1, assignmentId);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) version = rs.getInt(1); }
            }
            if (version != config.getVersion()) throw new IllegalArgumentException("Bộ test đã được sửa. Tải lại trang trước khi lưu.");
            String sql = version == 0 ? "INSERT INTO \"GRADING_CONFIGS\"(main_class,config_version,assignment_id) VALUES(?,?,?)"
                    : "UPDATE \"GRADING_CONFIGS\" SET main_class=?,config_version=? WHERE assignment_id=?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, config.getMainClass()); ps.setInt(2, version + 1); ps.setInt(3, assignmentId); ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM \"GRADING_TEST_CASES\" WHERE assignment_id=?")) {
                ps.setInt(1, assignmentId); ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO \"GRADING_TEST_CASES\" "
                    + "(assignment_id,case_order,case_name,stdin_text,expected_output,weight,is_hidden) VALUES(?,?,?,?,?,?,?)")) {
                int n = 0;
                for (GradingTestCase t : config.getTests()) {
                    ps.setInt(1, assignmentId); ps.setInt(2, ++n); bindTest(ps, 3, t); ps.addBatch();
                }
                ps.executeBatch();
            }
            connection.commit();
        } catch (SQLException | RuntimeException ex) { connection.rollback(); throw ex; }
        finally { connection.setAutoCommit(true); }
    }

    public void updateStatus(int submissionId, String status) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE \"SUBMISSIONS\" SET status=? WHERE submission_id=?")) {
            ps.setString(1, status); ps.setInt(2, submissionId);
            if (ps.executeUpdate() != 1) throw new SQLException("Không tìm thấy bài nộp.");
        }
    }

    public void saveReport(int submissionId, GradingReport report) throws SQLException {
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM \"GRADING_CASE_RESULTS\" WHERE submission_id=?")) {
                ps.setInt(1, submissionId); ps.executeUpdate();
            }
            int updated;
            try (PreparedStatement ps = connection.prepareStatement("UPDATE \"GRADING_REPORTS\" SET main_class=?,config_version=?,"
                    + "verdict=?,score=?,message=?,graded_at=timezone('UTC', CURRENT_TIMESTAMP) WHERE submission_id=?")) {
                bindReport(ps, submissionId, report); updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO \"GRADING_REPORTS\" "
                        + "(main_class,config_version,verdict,score,message,submission_id) VALUES(?,?,?,?,?,?)")) {
                    bindReport(ps, submissionId, report); ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO \"GRADING_CASE_RESULTS\" "
                    + "(submission_id,case_order,case_name,stdin_text,expected_output,weight,is_hidden,verdict,actual_output,diagnostics,execution_time_ms) "
                    + "VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
                for (GradingCaseResult c : report.getCases()) {
                    ps.setInt(1, submissionId); ps.setInt(2, c.getOrder());
                    bindTest(ps, 3, new GradingTestCase(c.getName(), c.getInput(), c.getExpectedOutput(), c.getWeight(), c.isHidden()));
                    ps.setString(8, c.getVerdict().name()); ps.setString(9, c.getActualOutput());
                    ps.setString(10, c.getDiagnostics()); ps.setLong(11, c.getExecutionTimeMs()); ps.addBatch();
                }
                ps.executeBatch();
            }
            updateStatus(submissionId, report.isComplete() ? "Completed" : "NOT_CONFIGURED".equals(report.getVerdict()) ? "Pending" : "Failed");
            connection.commit();
        } catch (SQLException | RuntimeException ex) { connection.rollback(); throw ex; }
        finally { connection.setAutoCommit(true); }
    }

    public GradingReport loadReport(int submissionId) throws SQLException {
        String verdict, main, message; int version; java.math.BigDecimal score;
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM \"GRADING_REPORTS\" WHERE submission_id=?")) {
            ps.setInt(1, submissionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                verdict = rs.getString("verdict"); main = rs.getString("main_class"); version = rs.getInt("config_version");
                message = rs.getString("message"); score = rs.getBigDecimal("score");
            }
        }
        List<GradingCaseResult> cases = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM \"GRADING_CASE_RESULTS\" WHERE submission_id=? ORDER BY case_order")) {
            ps.setInt(1, submissionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) cases.add(new GradingCaseResult(rs.getInt("case_order"), testFrom(rs),
                        GradingCaseResult.Verdict.valueOf(rs.getString("verdict")), rs.getString("actual_output"),
                        rs.getString("diagnostics"), rs.getLong("execution_time_ms")));
            }
        }
        return new GradingReport(verdict, main, version, score, message, cases);
    }

    private GradingTestCase testFrom(ResultSet rs) throws SQLException {
        return new GradingTestCase(rs.getString("case_name"), rs.getString("stdin_text"), rs.getString("expected_output"),
                rs.getBigDecimal("weight"), rs.getBoolean("is_hidden"));
    }
    private void bindTest(PreparedStatement ps, int start, GradingTestCase t) throws SQLException {
        ps.setString(start, t.getName()); ps.setString(start + 1, t.getInput()); ps.setString(start + 2, t.getExpectedOutput());
        ps.setBigDecimal(start + 3, t.getWeight()); ps.setBoolean(start + 4, t.isHidden());
    }
    private void bindReport(PreparedStatement ps, int id, GradingReport r) throws SQLException {
        ps.setString(1, r.getMainClass()); ps.setInt(2, r.getConfigVersion()); ps.setString(3, r.getVerdict());
        if (r.getScore() == null) ps.setNull(4, Types.DECIMAL); else ps.setBigDecimal(4, r.getScore());
        ps.setString(5, r.getMessage()); ps.setInt(6, id);
    }
    @Override public void close() throws SQLException { connection.close(); }
}
