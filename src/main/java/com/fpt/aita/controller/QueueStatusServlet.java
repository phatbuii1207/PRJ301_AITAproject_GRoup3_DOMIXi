package com.fpt.aita.controller;

import com.fpt.aita.dal.SubmissionDAO;
import com.fpt.aita.model.Submission;
import com.fpt.aita.model.User;
import com.fpt.aita.service.queue.GradingQueueManager;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * THÀNH VIÊN 3: Servlet cung cấp API kiểm tra trạng thái hàng đợi chấm bài
 * Phục vụ Client thăm dò (Polling bằng JavaScript Ajax / Fetch) để cập nhật giao diện thời gian thực.
 */
@WebServlet(name = "QueueStatusServlet", urlPatterns = {"/queue-status"})
public class QueueStatusServlet extends HttpServlet {

    private SubmissionDAO submissionDAO;

    @Override
    public void init() throws ServletException {
        this.submissionDAO = new SubmissionDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"error\":\"Chưa đăng nhập\"}");
            return;
        }

        String subIdParam = request.getParameter("submissionId");
        if (subIdParam == null || subIdParam.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\":\"Thiếu tham số submissionId\"}");
            return;
        }

        try {
            int submissionId = Integer.parseInt(subIdParam.trim());
            Submission sub = submissionDAO.getSubmissionById(submissionId);

            if (sub == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\":\"Không tìm thấy bài nộp\"}");
                return;
            }

            // Bảo mật: Chỉ người nộp hoặc Giảng viên mới được check
            if (!"Lecturer".equalsIgnoreCase(currentUser.getRole()) && sub.getStudentId() != currentUser.getUserId()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write("{\"error\":\"Không có quyền truy cập bài nộp này\"}");
                return;
            }

            GradingQueueManager queueManager = GradingQueueManager.getInstance();
            String detail = queueManager.getStatusDetail(submissionId);
            int queueSize = queueManager.getQueueSize();

            // Trả về JSON thủ công (không cần phụ thuộc thư viện Jackson/Gson bên ngoài)
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"submissionId\":").append(sub.getSubmissionId()).append(",");
            json.append("\"status\":\"").append(sub.getStatus()).append("\",");
            json.append("\"detail\":\"").append(escapeJson(detail)).append("\",");
            json.append("\"queueSize\":").append(queueSize);
            json.append("}");

            out.write(json.toString());

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\":\"submissionId không hợp lệ\"}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }
}
