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

@WebServlet(name = "AssignmentServlet", urlPatterns = {"/assignments"})
public class AssignmentServlet extends HttpServlet {

    private AssignmentDAO dao = new AssignmentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");

        if (currentUser == null) {
            response.sendRedirect("login");
            return;
        }
        if ("Student".equalsIgnoreCase(currentUser.getRole())) {
            response.sendRedirect("student-assignments");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) {
            action = "list";
        }

        if ("delete".equals(action)) {
            int id = Integer.parseInt(request.getParameter("id"));
            Assignment a = dao.getAssignmentById(id);

            if (a.getLecturerId() == currentUser.getUserId()) {
                dao.deleteAssignment(id);
                response.sendRedirect("assignments");
                return;
            } else {
                request.setAttribute("error", "Bạn không có quyền xóa bài tập này!");
            }

        } else if ("edit".equals(action)) {
            // Lấy thông tin bài tập cần SỬA
            int id = Integer.parseInt(request.getParameter("id"));
            Assignment a = dao.getAssignmentById(id);

            if (a.getLecturerId() == currentUser.getUserId()) {
                request.setAttribute("editAssignment", a);
            } else {
                request.setAttribute("error", "Bạn không có quyền sửa bài tập này!");
            }

        }

        // Mặc định: Hiển thị danh sách (READ)
        List<Assignment> list = dao.getAllAssignments();
        request.setAttribute("assignmentList", list);
        request.getRequestDispatcher("assignments.jsp").forward(request, response);
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
        if ("Student".equalsIgnoreCase(currentUser.getRole())) {
            response.sendRedirect("student-assignments");
            return;
        }

        String action = request.getParameter("action");
        String title = request.getParameter("title");
        String description = request.getParameter("description");

        if ("update".equals(action)) {
            // Xử lý LƯU THAY ĐỔI (UPDATE)
            int id = Integer.parseInt(request.getParameter("id"));
            Assignment a = new Assignment(id, 0, null, title, description, null);
            dao.updateAssignment(a);
        } else {
            // Xử lý THÊM MỚI (CREATE)
            int lecturerId = currentUser.getUserId();

            Assignment a = new Assignment(0, lecturerId, null, title, description, null);
            dao.insertAssignment(a);
        }

        response.sendRedirect("assignments");
    }
}
