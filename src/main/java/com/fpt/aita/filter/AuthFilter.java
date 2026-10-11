package com.fpt.aita.filter;

import com.fpt.aita.model.User;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * THÀNH VIÊN 3: BỘ LỌC XÁC THỰC VÀ PHÂN QUYỀN TẬP TRUNG (AuthFilter)
 * - Chặn truy cập trái phép trực tiếp vào các file .jsp từ trình duyệt
 * - Kiểm tra phiên đăng nhập (Session) trên toàn bộ tài nguyên được bảo vệ
 * - Phân quyền vai trò Giảng viên (Lecturer) và Sinh viên (Student)
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = {"/*"})
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        // 1. TÀI NGUYÊN TĨNH VÀ CÔNG KHAI (STATIC & PUBLIC ASSETS)
        if (path.startsWith("/assets/") || path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.equals("/favicon.ico")
                || path.equals("/index.html")) {
            chain.doFilter(request, response);
            return;
        }

        // 2. CHẶN TRUY CẬP TRỰC TIẾP FILE .JSP (CHỐNG LỖ HỔNG XEM TRỰC TIẾP VIEW BỎ QUA CONTROLLER)
        if (path.endsWith(".jsp")) {
            // Cho phép xem giao diện login & register nếu chưa đăng nhập
            if (path.equals("/login.jsp") || path.equals("/register.jsp")) {
                if (currentUser != null) {
                    response.sendRedirect(contextPath + "/home");
                    return;
                }
                chain.doFilter(request, response);
                return;
            }

            // Mọi file .jsp nghiệp vụ khác (home.jsp, course_detail.jsp, assignments.jsp, student_assignments.jsp, ...)
            // Bị chặn triệt để khi gõ thẳng URL từ trình duyệt
            if (currentUser == null) {
                response.sendRedirect(contextPath + "/login");
            } else {
                response.sendRedirect(contextPath + "/home");
            }
            return;
        }

        // 3. CÁC ĐƯỜNG DẪN XÁC THỰC CÔNG KHAI (PUBLIC SERVLETS)
        if (path.equals("/login") || path.equals("/register") || path.equals("/logout") || path.equals("") || path.equals("/")) {
            if (currentUser != null && (path.equals("/login") || path.equals("/register"))) {
                response.sendRedirect(contextPath + "/home");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // 4. KIỂM TRA PHIÊN ĐĂNG NHẬP (AUTHENTICATION CHECK CHO CÁC TÀI NGUYÊN BẢO VỆ)
        if (currentUser == null) {
            HttpSession newSession = request.getSession(true);
            newSession.setAttribute("error", "Vui lòng đăng nhập để tiếp tục truy cập!");
            response.sendRedirect(contextPath + "/login");
            return;
        }

        // 5. KIỂM TRA PHÂN QUYỀN VAI TRÒ (ROLE-BASED AUTHORIZATION)
        String action = request.getParameter("action");
        if ("Student".equalsIgnoreCase(currentUser.getRole())) {
            // Sinh viên cố tình kích hoạt thao tác quản trị của Giảng viên
            if ("create_assignment".equals(action) || "delete_assignment".equals(action) || "update_deadline".equals(action)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thực hiện chức năng quản trị của Giảng viên!");
                return;
            }
        }

        // Hợp lệ -> Tiếp tục chuỗi Filter
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
