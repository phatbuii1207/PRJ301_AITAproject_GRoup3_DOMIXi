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

        // ================= EXCEPTION PATH 1: XÓA BÀI TẬP =================
        if ("delete".equals(action)) {
            String idStr = request.getParameter("id");
            try {
                if (idStr == null || idStr.trim().isEmpty()) {
                    request.setAttribute("error", "Mã bài tập không được để trống!");
                } else {
                    int id = Integer.parseInt(idStr);
                    Assignment a = dao.getAssignmentById(id);

                    if (a == null) {
                        request.setAttribute("error", "Không tìm thấy bài tập #" + id + " (có thể đã bị xóa)!");
                    } else if (a.getLecturerId() == currentUser.getUserId()) {
                        boolean ok = dao.deleteAssignment(id);
                        if (ok) {
                            session.setAttribute("flashSuccess", "Đã xóa thành công bài tập #" + id + "!");
                        } else {
                            request.setAttribute("error", "Không thể xóa bài tập #" + id + " do lỗi ràng buộc dữ liệu!");
                        }
                        response.sendRedirect("assignments");
                        return;
                    } else {
                        request.setAttribute("error", "Bạn không có quyền xóa bài tập #" + id + " của giảng viên khác!");
                    }
                }
            } catch (NumberFormatException e) {
                request.setAttribute("error", "Mã bài tập không hợp lệ (phải là số nguyên)!");
            }

        // ================= EXCEPTION PATH 2: SỬA BÀI TẬP =================
        } else if ("edit".equals(action)) {
            String idStr = request.getParameter("id");
            try {
                if (idStr == null || idStr.trim().isEmpty()) {
                    request.setAttribute("error", "Mã bài tập không được để trống!");
                } else {
                    int id = Integer.parseInt(idStr);
                    Assignment a = dao.getAssignmentById(id);

                    if (a == null) {
                        request.setAttribute("error", "Không tìm thấy bài tập #" + id + " để chỉnh sửa!");
                    } else if (a.getLecturerId() == currentUser.getUserId()) {
                        request.setAttribute("editAssignment", a);
                    } else {
                        request.setAttribute("error", "Bạn không có quyền sửa bài tập #" + id + " của giảng viên khác!");
                    }
                }
            } catch (NumberFormatException e) {
                request.setAttribute("error", "Mã bài tập không hợp lệ (phải là số nguyên)!");
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

        // ================= EXCEPTION PATH 3: VALIDATION DỮ LIỆU ĐẦU VÀO =================
        String validationError = null;
        if (title == null || title.trim().isEmpty()) {
            validationError = "Tiêu đề bài tập không được để trống!";
        } else if (title.trim().length() < 5) {
            validationError = "Tiêu đề bài tập quá ngắn (phải có ít nhất 5 ký tự)!";
        } else if (title.trim().length() > 150) {
            validationError = "Tiêu đề bài tập không được vượt quá 150 ký tự!";
        } else if (description != null && description.trim().length() > 500) {
            validationError = "Mô tả bài tập không được vượt quá 500 ký tự!";
        }

        if (validationError != null) {
            // Có lỗi dữ liệu đầu vào -> Trả về giao diện, hiển thị lỗi và giữ nguyên form người dùng vừa nhập
            request.setAttribute("error", validationError);
            if ("update".equals(action)) {
                try {
                    int id = Integer.parseInt(request.getParameter("id"));
                    request.setAttribute("editAssignment", new Assignment(id, currentUser.getUserId(), currentUser.getFullName(), title, description, null));
                } catch (NumberFormatException ignored) {}
            } else {
                request.setAttribute("draftTitle", title);
                request.setAttribute("draftDescription", description);
            }
            List<Assignment> list = dao.getAllAssignments();
            request.setAttribute("assignmentList", list);
            request.getRequestDispatcher("assignments.jsp").forward(request, response);
            return;
        }

        // ================= XỬ LÝ LƯU CSDL KHI DỮ LIỆU ĐÃ HỢP LỆ =================
        if ("update".equals(action)) {
            try {
                int id = Integer.parseInt(request.getParameter("id"));
                Assignment existing = dao.getAssignmentById(id);
                if (existing == null) {
                    request.setAttribute("error", "Bài tập #" + id + " không còn tồn tại!");
                    List<Assignment> list = dao.getAllAssignments();
                    request.setAttribute("assignmentList", list);
                    request.getRequestDispatcher("assignments.jsp").forward(request, response);
                    return;
                }
                if (existing.getLecturerId() != currentUser.getUserId()) {
                    request.setAttribute("error", "Bạn không có quyền cập nhật bài tập của giảng viên khác!");
                    List<Assignment> list = dao.getAllAssignments();
                    request.setAttribute("assignmentList", list);
                    request.getRequestDispatcher("assignments.jsp").forward(request, response);
                    return;
                }

                Assignment a = new Assignment(id, currentUser.getUserId(), null, title.trim(), description != null ? description.trim() : "", null);
                boolean ok = dao.updateAssignment(a);
                if (ok) {
                    session.setAttribute("flashSuccess", "Cập nhật bài tập #" + id + " thành công!");
                } else {
                    request.setAttribute("error", "Lỗi CSDL khi cập nhật bài tập!");
                }
            } catch (NumberFormatException e) {
                request.setAttribute("error", "Mã bài tập cập nhật không hợp lệ!");
            }
        } else {
            // CREATE: Thêm bài tập mới
            int lecturerId = currentUser.getUserId();
            Assignment a = new Assignment(0, lecturerId, null, title.trim(), description != null ? description.trim() : "", null);
            boolean ok = dao.insertAssignment(a);
            if (ok) {
                session.setAttribute("flashSuccess", "Tạo bài tập mới thành công!");
            } else {
                request.setAttribute("error", "Lỗi CSDL khi thêm bài tập mới!");
            }
        }

        response.sendRedirect("assignments");
    }
}

