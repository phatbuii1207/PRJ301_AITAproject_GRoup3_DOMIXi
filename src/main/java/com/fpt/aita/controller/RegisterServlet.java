package com.fpt.aita.controller;

import com.fpt.aita.dal.UserDAO;
import com.fpt.aita.model.User;
import com.fpt.aita.util.PasswordUtil;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(name = "RegisterServlet", urlPatterns = {"/register"})
public class RegisterServlet extends HttpServlet {

    private static void redirectByRole(User user, HttpServletResponse response) throws IOException {
        if ("Lecturer".equalsIgnoreCase(user.getRole())) {
            response.sendRedirect("assignments");
        } else if ("Student".equalsIgnoreCase(user.getRole())) {
            response.sendRedirect("student-assignments");
        } else {
            response.sendRedirect("home.jsp");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("currentUser") != null) {
            User user = (User) session.getAttribute("currentUser");
            redirectByRole(user, response);
            return;
        }

        // Chưa đăng nhập: chuyển tiếp tới trang đăng ký register.jsp
        request.getRequestDispatcher("register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("currentUser") != null) {
            User user = (User) session.getAttribute("currentUser");
            redirectByRole(user, response);
            return;
        }

        String username = request.getParameter("username");
        String fullName = request.getParameter("fullName");
        String password = request.getParameter("password");
        String repassword = request.getParameter("repassword");

        // Trim khoảng trắng đầu/cuối của username và fullName (chống leo thang: không đọc role từ request)
        if (username != null) {
            username = username.trim();
        }
        if (fullName != null) {
            fullName = fullName.trim();
        }

        String error = null;

        // 1. Validation theo thứ tự, lỗi đầu tiên thì dừng
        if (username == null || username.isEmpty()) {
            error = "Tên đăng nhập không được để trống!";
        } else if (!username.matches("^[A-Za-z0-9_]{5,50}$")) {
            error = "Tên đăng nhập phải từ 5 đến 50 ký tự, chỉ gồm chữ cái, chữ số và dấu gạch dưới!";
        } else if (fullName == null || fullName.isEmpty()) {
            error = "Họ và tên không được để trống!";
        } else if (!fullName.matches("^[\\p{L}\\p{M} .-]{1,100}$")) {
            error = "Họ và tên chỉ gồm chữ cái, khoảng trắng, dấu chấm, gạch nối (tối đa 100 ký tự)!";
        } else if (password == null || password.length() < 6 || password.length() > 100) {
            error = "Mật khẩu phải từ 6 đến 100 ký tự!";
        } else if (!password.equals(repassword)) {
            error = "Mật khẩu nhập lại không khớp!";
        }

        // Chỉ tạo UserDAO khi các bước validate không cần DB đều đạt
        UserDAO dao = null;
        if (error == null) {
            dao = new UserDAO();
            if (dao.isUsernameExists(username)) {
                error = "Tên đăng nhập đã tồn tại!";
            }
        }

        // 2. Nếu có lỗi validation
        if (error != null) {
            request.setAttribute("error", error);
            request.setAttribute("draftUsername", username);
            request.setAttribute("draftFullName", fullName);
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        // 3. Dữ liệu hợp lệ -> Băm mật khẩu và đăng ký với vai trò mặc định 'Student'
        String passwordHash = PasswordUtil.sha256Hex(password);
        User newUser = new User(0, username, passwordHash, fullName, "Student", null);
        boolean success = dao.register(newUser);

        if (success) {
            // Thành công: lưu thông báo vào session flash và redirect về login (PRG)
            HttpSession currentSession = request.getSession();
            currentSession.setAttribute("flashSuccess", "Đăng ký thành công! Hãy đăng nhập bằng tài khoản vừa tạo.");
            response.sendRedirect("login");
        } else {
            // Đăng ký thất bại do lỗi CSDL
            request.setAttribute("error", "Đăng ký thất bại, vui lòng thử lại!");
            request.setAttribute("draftUsername", username);
            request.setAttribute("draftFullName", fullName);
            request.getRequestDispatcher("register.jsp").forward(request, response);
        }
    }
}
