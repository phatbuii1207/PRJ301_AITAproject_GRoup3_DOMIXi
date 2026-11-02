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

/**
 * AssignmentServlet: Cổng quản lý Bài tập dành cho Giảng viên
 */
@WebServlet(name = "AssignmentServlet", urlPatterns = {"/assignments"})
public class AssignmentServlet extends HttpServlet {

    private final AssignmentDAO assignmentDAO = new AssignmentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (currentUser == null) {
            response.sendRedirect("login");
            return;
        }

        String action = request.getParameter("action");
        if ("delete".equalsIgnoreCase(action)) {
            try {
                int id = Integer.parseInt(request.getParameter("id"));
                assignmentDAO.deleteAssignment(id);
            } catch (Exception ignored) {}
            response.sendRedirect("assignments");
            return;
        }

        if ("edit".equalsIgnoreCase(action)) {
            try {
                int id = Integer.parseInt(request.getParameter("id"));
                Assignment a = assignmentDAO.getAssignmentById(id);
                request.setAttribute("editAssignment", a);
            } catch (Exception ignored) {}
        }

        List<Assignment> assignmentList = assignmentDAO.getAllAssignments();
        request.setAttribute("assignmentList", assignmentList);
        request.getRequestDispatcher("assignments.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (currentUser == null) {
            response.sendRedirect("login");
            return;
        }

        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String idStr = request.getParameter("id");

        if (title != null && !title.trim().isEmpty()) {
            if (idStr != null && !idStr.trim().isEmpty()) {
                try {
                    int id = Integer.parseInt(idStr);
                    Assignment a = new Assignment(id, currentUser.getUserId(), currentUser.getFullName(), 1, title.trim(), description, null, null);
                    assignmentDAO.updateAssignment(a);
                } catch (Exception ignored) {}
            } else {
                Assignment a = new Assignment(0, currentUser.getUserId(), currentUser.getFullName(), 1, title.trim(), description, null, null);
                assignmentDAO.insertAssignment(a);
            }
        }
        response.sendRedirect("assignments");
    }
}
