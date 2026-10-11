package com.fpt.aita.controller;

import com.fpt.aita.dal.UserDAO;
import com.fpt.aita.model.User;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(name = "LoginServlet", urlPatterns = {"/login", "/logout"})
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getServletPath();
        if ("/logout".equals(action)) {
            request.getSession().invalidate();
            response.sendRedirect("login");
            return;
        }
        // Chuyển tới giao diện đăng nhập login.jsp (JSP sẽ đọc ${cookie.remUser.value})
        request.getRequestDispatcher("login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String u = request.getParameter("username");
        String p = request.getParameter("password");
        String remember = request.getParameter("remember");

        UserDAO dao = new UserDAO();
        User user = dao.login(u, p);

        if (user != null) {
            // 1. Quản lý trạng thái Session
            HttpSession session = request.getSession();
            session.setAttribute("currentUser", user);

            // 2. Quản lý trạng thái Cookie (Remember Me)
            Cookie cookieUser = new Cookie("remUser", u);
            if ("yes".equals(remember) || "on".equals(remember)) {
                cookieUser.setMaxAge(7 * 24 * 60 * 60); // Lưu 7 ngày
            } else {
                cookieUser.setMaxAge(0); // Xóa cookie nếu không tích chọn
            }
            cookieUser.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
            response.addCookie(cookieUser);

            // 3. Chuyển hướng vào Cổng học tập EduNext Portal (Home)
            response.sendRedirect("home");
        } else {
            // Đăng nhập thất bại -> Báo lỗi ra màn hình
            request.setAttribute("error", "Sai tên đăng nhập hoặc mật khẩu!");
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }
}

