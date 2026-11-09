package com.fpt.aita.controller;

import com.fpt.aita.dal.AnalysisReportDAO;
import com.fpt.aita.dal.AssignmentDAO;
import com.fpt.aita.dal.SubmissionDAO;
import com.fpt.aita.dal.UserDAO;
import com.fpt.aita.model.AiOptimization;
import com.fpt.aita.model.AnalysisReport;
import com.fpt.aita.model.Assignment;
import com.fpt.aita.model.Issue;
import com.fpt.aita.model.Submission;
import com.fpt.aita.model.User;

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Controller xử lý xem Báo Cáo Phân Tích Mã Nguồn Tĩnh & Tối Ưu Thuật Toán AI
 * Endpoint: /report?submissionId=... hoặc /report?reportId=...
 */
@WebServlet(name = "AnalysisReportServlet", urlPatterns = {"/report"})
public class AnalysisReportServlet extends HttpServlet {

    private final AnalysisReportDAO reportDAO = new AnalysisReportDAO();
    private final SubmissionDAO submissionDAO = new SubmissionDAO();
    private final AssignmentDAO assignmentDAO = new AssignmentDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        if (!"Lecturer".equalsIgnoreCase(currentUser.getRole())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Báo cáo AST/AI chỉ dành cho giảng viên phụ trách.");
            return;
        }

        // 1. Đọc tham số submissionId hoặc reportId
        String submissionIdParam = req.getParameter("submissionId");
        String reportIdParam = req.getParameter("reportId");
        if (submissionIdParam == null && reportIdParam == null) {
            submissionIdParam = req.getParameter("id");
        }

        int submissionId = 0;
        int reportId = 0;
        try {
            if (submissionIdParam != null && !submissionIdParam.trim().isEmpty()) {
                submissionId = Integer.parseInt(submissionIdParam.trim());
            } else if (reportIdParam != null && !reportIdParam.trim().isEmpty()) {
                reportId = Integer.parseInt(reportIdParam.trim());
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã bài nộp hoặc mã báo cáo không hợp lệ!");
            return;
        }

        if (submissionId <= 0 && reportId <= 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Thiếu tham số submissionId hoặc reportId!");
            return;
        }

        // 2. Tra cứu đối tượng bài nộp (Submission)
        Submission submission = null;
        AnalysisReport report = null;

        if (reportId > 0) {
            report = reportDAO.getReportById(reportId);
            if (report != null) {
                submissionId = report.getSubmissionId();
                submission = submissionDAO.getSubmissionById(submissionId);
            }
        } else {
            submission = submissionDAO.getSubmissionById(submissionId);
            report = reportDAO.getBySubmissionId(submissionId);
        }

        // Hỗ trợ chế độ demo mock data nếu có cờ mock=true hoặc submission ID giả lập (999)
        String mockParam = req.getParameter("mock");
        boolean isMockMode = "true".equalsIgnoreCase(mockParam) || submissionId == 999;
        if (isMockMode && report == null) {
            report = reportDAO.getMockReport(submissionId > 0 ? submissionId : 999);
            req.setAttribute("isMockData", true);
        }

        // Nếu hoàn toàn không tìm thấy bài nộp lẫn báo cáo
        if (submission == null && report == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy thông tin bài nộp #" + submissionId);
            return;
        }

        // 3. KIỂM TRA PHÂN QUYỀN TRUY CẬP (AUTHORIZATION)
        int assignmentId = (submission != null) ? submission.getAssignmentId() : report.getAssignmentId();

        // Giảng viên chỉ xem bài của lớp môn học mình phụ trách.
        Assignment assignment = assignmentDAO.getAssignmentById(assignmentId);
        if (assignment == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy bài tập.");
            return;
        }
        List<Integer> lecturerClassIds = userDAO.getClassIdsByUserId(currentUser.getUserId());
        boolean isAuthor = assignment.getLecturerId() == currentUser.getUserId();
        boolean isTeachingCourse = lecturerClassIds != null && lecturerClassIds.contains(assignment.getCourseId());
        if (!isAuthor && !isTeachingCourse) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền xem báo cáo của môn học không do bạn phụ trách!");
            return;
        }

        // 4. XỬ LÝ TRƯỜNG HỢP: Bài nộp đang trong tiến trình phân tích ("Đang chấm")
        if (report == null) {
            req.setAttribute("submission", submission);
            req.setAttribute("isPending", true);
            req.getRequestDispatcher("/analysis_report.jsp").forward(req, resp);
            return;
        }

        // 5. TRUY XUẤT DỮ LIỆU ĐẦY ĐỦ CHO VIEW
        List<Issue> issues;
        List<AiOptimization> optimizations;
        AiOptimization singleOpt;

        if (isMockMode && req.getAttribute("isMockData") != null) {
            issues = reportDAO.getMockIssues();
            singleOpt = reportDAO.getMockOptimization();
            optimizations = new java.util.ArrayList<>();
            optimizations.add(singleOpt);
        } else {
            issues = reportDAO.getIssues(report.getReportId());
            optimizations = reportDAO.getOptimizations(report.getReportId());
            singleOpt = (optimizations != null && !optimizations.isEmpty()) ? optimizations.get(0) : null;
        }

        // Cung cấp các thuộc tính cho JSP theo đúng đặc tả Bước 2
        req.setAttribute("report", report);
        req.setAttribute("submission", submission);
        req.setAttribute("issues", issues);
        req.setAttribute("opt", singleOpt);
        req.setAttribute("optimizations", optimizations);

        // Chuyển tiếp tới giao diện phân tích
        req.getRequestDispatcher("/analysis_report.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }
}
