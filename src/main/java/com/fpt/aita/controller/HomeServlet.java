package com.fpt.aita.controller;

import com.fpt.aita.dal.CourseDAO;
import com.fpt.aita.model.Course;
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
 * Controller Quản lý Trang chủ EduNext Portal & Danh sách môn học
 * Dành cho Thành viên 2 (PRJ301)
 */
@WebServlet(name = "HomeServlet", urlPatterns = {"/home", "/courses"})
public class HomeServlet extends HttpServlet {

    private CourseDAO courseDAO = new CourseDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");

        // 1. Kiểm tra trạng thái phiên đăng nhập (Authentication Guard)
        if (currentUser == null) {
            response.sendRedirect("login");
            return;
        }

        // 2. Xử lý lấy danh sách môn học theo phân quyền (Giảng viên phụ trách hoặc Sinh viên tham gia)
        String search = request.getParameter("search");
        List<Course> list;
        if (search != null && !search.trim().isEmpty()) {
            list = courseDAO.searchCoursesByUserId(currentUser.getUserId(), search.trim());
        } else {
            list = courseDAO.getCoursesByUserId(currentUser.getUserId());
        }

        // 3. Đóng gói dữ liệu sang phạm vi Request (Model -> View)
        request.setAttribute("courseList", list);
        request.setAttribute("searchKeyword", search != null ? search.trim() : "");

        // 4. Chuyển tiếp nội bộ tới giao diện home.jsp
        request.getRequestDispatcher("home.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
