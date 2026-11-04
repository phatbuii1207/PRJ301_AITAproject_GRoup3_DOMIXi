package com.fpt.aita.controller;

import com.fpt.aita.dal.GradingDAO;
import com.fpt.aita.model.*;
import com.fpt.aita.service.queue.GradingQueueManager;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet(name = "GradingServlet", urlPatterns = {"/grading-tests", "/grading-result"})
public final class GradingServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException { handle(req, resp, false); }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException { handle(req, resp, true); }

    private void handle(HttpServletRequest req, HttpServletResponse resp, boolean post) throws IOException, ServletException {
        resp.setHeader("Cache-Control", "no-store");
        User user = req.getSession(false) == null ? null : (User) req.getSession(false).getAttribute("currentUser");
        if (user == null) { resp.sendRedirect(req.getContextPath() + "/login"); return; }
        String csrf = (String) req.getSession().getAttribute("gradingCsrf");
        if (csrf == null) { csrf = UUID.randomUUID().toString(); req.getSession().setAttribute("gradingCsrf", csrf); }
        if (post && (req.getParameter("csrf") == null || !MessageDigest.isEqual(csrf.getBytes(StandardCharsets.UTF_8),
                req.getParameter("csrf").getBytes(StandardCharsets.UTF_8)))) {
            resp.sendError(403, "Phiên biểu mẫu không hợp lệ. Tải lại trang."); return;
        }
        try (GradingDAO dao = new GradingDAO()) {
            boolean resultPage = "/grading-result".equals(req.getServletPath());
            int id = Integer.parseInt(req.getParameter(resultPage ? "submissionId" : "assignmentId"));
            Submission submission = resultPage ? dao.findSubmission(id) : null;
            if (resultPage && submission == null) { resp.sendError(404); return; }
            Assignment assignment = dao.findAssignment(resultPage ? submission.getAssignmentId() : id);
            if (assignment == null) { resp.sendError(404); return; }
            boolean manager = "Lecturer".equalsIgnoreCase(user.getRole()) && assignment.getLecturerId() == user.getUserId();
            boolean student = "Student".equalsIgnoreCase(user.getRole());
            boolean lecturer = "Lecturer".equalsIgnoreCase(user.getRole());
            boolean member = dao.isCourseMember(user.getUserId(), assignment.getCourseId());
            boolean canRead = resultPage ? (student && submission.getStudentId() == user.getUserId()) || manager || lecturer && member
                    : manager || (student || lecturer) && member;
            if (!canRead || post && !manager) { resp.sendError(403, "Bạn không có quyền truy cập dữ liệu chấm bài này."); return; }
            if (post) {
                if (resultPage) {
                    if (!GradingQueueManager.getInstance().enqueue(id)) { resp.sendError(409, "Bài đang chấm, hàng đợi đầy hoặc CSDL chưa sẵn sàng."); return; }
                } else dao.saveConfig(id, parseConfig(req));
                resp.sendRedirect(req.getContextPath() + req.getServletPath() + (resultPage ? "?submissionId=" : "?assignmentId=") + id);
                return;
            }
            req.setAttribute("assignment", assignment); req.setAttribute("canManage", manager);
            if (resultPage) {
                boolean processing = GradingQueueManager.getInstance().isActive(id);
                GradingReport report = processing ? null : dao.loadReport(id);
                if (report != null && report.isComplete() && !"Completed".equals(dao.findSubmission(id).getStatus())) {
                    report = GradingReport.unavailable("GRADING_ERROR", "Lượt chấm mới chưa hoàn tất. Giảng viên cần chấm lại; điểm cũ chưa được xác nhận cho lượt này.");
                }
                req.setAttribute("grade", report == null ? null : student ? report.forStudent() : report);
                req.setAttribute("submission", submission); req.setAttribute("processing", processing);
                req.getRequestDispatcher("/WEB-INF/views/grading-result.jsp").forward(req, resp);
            } else {
                GradingConfig config = dao.loadConfig(id);
                List<GradingTestCase> visible = new ArrayList<>();
                if (config != null) for (GradingTestCase t : config.getTests()) if (manager || !t.isHidden()) visible.add(t);
                req.setAttribute("tests", visible);
                req.setAttribute("mainClass", config == null ? "Main" : config.getMainClass());
                req.setAttribute("configVersion", config == null ? 0 : config.getVersion());
                req.getRequestDispatcher("/WEB-INF/views/grading-tests.jsp").forward(req, resp);
            }
        } catch (NumberFormatException ex) { resp.sendError(400, "Mã bài hoặc dữ liệu số không hợp lệ."); }
        catch (IllegalArgumentException ex) { resp.sendError(400, ex.getMessage()); }
        catch (SQLException ex) { log("Grading database unavailable: " + ex.getClass().getSimpleName()); resp.sendError(503, "CSDL chấm bài chưa sẵn sàng. Kiểm tra migration 001_test_case_grading.sql."); }
    }

    private GradingConfig parseConfig(HttpServletRequest req) {
        String[] names = req.getParameterValues("caseName"), inputs = req.getParameterValues("stdin"),
                expected = req.getParameterValues("expected"), weights = req.getParameterValues("weight"), hidden = req.getParameterValues("hidden");
        if (names == null || names.length > 20 || inputs == null || expected == null || weights == null || hidden == null
                || inputs.length != names.length || expected.length != names.length || weights.length != names.length || hidden.length != names.length) {
            throw new IllegalArgumentException("Biểu mẫu test case không hợp lệ (tối đa 20 test).");
        }
        List<GradingTestCase> tests = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            if (names[i].trim().isEmpty() && inputs[i].isEmpty() && expected[i].isEmpty()) continue;
            tests.add(new GradingTestCase(names[i], inputs[i], expected[i], new BigDecimal(weights[i]), "true".equals(hidden[i])));
        }
        return new GradingConfig(req.getParameter("mainClass"), Integer.parseInt(req.getParameter("version")), tests);
    }
}
