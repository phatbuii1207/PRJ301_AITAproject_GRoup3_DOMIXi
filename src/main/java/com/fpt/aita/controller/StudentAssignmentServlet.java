package com.fpt.aita.controller;

import com.fpt.aita.dal.AnalysisReportDAO;
import com.fpt.aita.dal.AssignmentDAO;
import com.fpt.aita.dal.SubmissionDAO;
import com.fpt.aita.model.Assignment;
import com.fpt.aita.model.Submission;
import com.fpt.aita.model.User;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * StudentAssignmentServlet: Cổng quản lý và nộp bài tập của Sinh viên
 * Cung cấp danh sách bài tập, trạng thái bài nộp và liên kết Báo cáo đánh giá (/report)
 */
@WebServlet(name = "StudentAssignmentServlet", urlPatterns = {"/student-assignments", "/submissions"})
public class StudentAssignmentServlet extends HttpServlet {

    private final AssignmentDAO assignmentDAO = new AssignmentDAO();
    private final SubmissionDAO submissionDAO = new SubmissionDAO();
    private final AnalysisReportDAO reportDAO = new AnalysisReportDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (currentUser == null) {
            response.sendRedirect("login");
            return;
        }

        List<Assignment> assignmentList = assignmentDAO.getAllAssignments();
        request.setAttribute("assignmentList", assignmentList);

        // Nạp bản ghi nộp bài mới nhất của sinh viên cho từng bài tập
        Map<Integer, Submission> submissionMap = new HashMap<>();
        Map<Integer, Boolean> reportMap = new HashMap<>();

        if (assignmentList != null) {
            for (Assignment a : assignmentList) {
                Submission sub = submissionDAO.getLatestSubmission(currentUser.getUserId(), a.getAssignmentId());
                if (sub != null) {
                    submissionMap.put(a.getAssignmentId(), sub);
                    reportMap.put(sub.getSubmissionId(), reportDAO.hasReport(sub.getSubmissionId()));
                }
            }
        }

        request.setAttribute("submissionMap", submissionMap);
        request.setAttribute("reportMap", reportMap);
        request.getRequestDispatcher("student_assignments.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("student-assignments");
    }
}
