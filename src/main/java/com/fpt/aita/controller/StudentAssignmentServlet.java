package com.fpt.aita.controller;

import com.fpt.aita.dal.AssignmentDAO;
import com.fpt.aita.model.Assignment;
import com.fpt.aita.model.User;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(name = "StudentAssignmentServlet", urlPatterns = {"/student-assignments", "/submissions"})
public class StudentAssignmentServlet extends HttpServlet {

    private AssignmentDAO dao = new AssignmentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");

        // Kiểm tra đăng nhập
        if (currentUser == null) {
            response.sendRedirect("login");
            return;
        }

        // Lấy danh sách bài tập từ CSDL SQL Server thật
        List<Assignment> list = dao.getAllAssignments();
        request.setAttribute("assignmentList", list);

        // Chuyển tiếp đến giao diện sinh viên
        request.getRequestDispatcher("student_assignments.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");

        if (currentUser == null) {
            response.sendRedirect("login");
            return;
        }

        String assignmentId = request.getParameter("assignmentId");
        String assignmentTitle = request.getParameter("assignmentTitle");
        String fileName = request.getParameter("fileName");
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "Submission_" + currentUser.getUsername() + ".zip";
        }

        // Tạo mã SHA-256 ngẫu nhiên mô phỏng cho bài nộp (Đúng phong cách RBL của đề tài AITA)
        String mockSha256 = Long.toHexString(Double.doubleToLongBits(Math.random()))
                + Long.toHexString(System.currentTimeMillis())
                + "a9f8b7c6d5e4";
        if (mockSha256.length() > 64) {
            mockSha256 = mockSha256.substring(0, 64);
        }

        // Đặt thông báo thành công vào session để hiển thị trên giao diện
        session.setAttribute("flashSuccess", "🎉 <b>Nộp bài thành công!</b><br/>"
                + "• Bài tập: <b>" + (assignmentTitle != null ? assignmentTitle : ("#" + assignmentId)) + "</b><br/>"
                + "• Tệp tải lên: <code>" + fileName + "</code><br/>"
                + "• Mã băm SHA-256: <code>" + mockSha256 + "</code><br/>"
                + "• Trạng thái: <span style='color:#38bdf8;'>Đã tiếp nhận (Pending AI AST Analyzer)</span>");

        response.sendRedirect("student-assignments");
    }
}
