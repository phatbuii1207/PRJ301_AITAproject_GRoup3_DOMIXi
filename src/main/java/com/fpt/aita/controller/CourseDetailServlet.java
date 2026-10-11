package com.fpt.aita.controller;

import com.fpt.aita.dal.AssignmentDAO;
import com.fpt.aita.dal.SubmissionDAO;
import com.fpt.aita.model.Assignment;
import com.fpt.aita.model.Submission;
import com.fpt.aita.model.User;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

/**
 * THÀNH VIÊN 3: CHI TIẾT MÔN HỌC & PHÂN QUYỀN BÀI TẬP
 * Servlet: /course
 * Hỗ trợ Upload File Kéo Thả (Drag & Drop), Kiểm tra Deadline, Tải File Bài Nộp
 */
@WebServlet(name = "CourseDetailServlet", urlPatterns = {"/course"})
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,  // 2MB bộ nhớ đệm
    maxFileSize = 1024 * 1024 * 50,       // 50MB tối đa mỗi tệp
    maxRequestSize = 1024 * 1024 * 100    // 100MB tối đa toàn request
)
public class CourseDetailServlet extends HttpServlet {

    private AssignmentDAO assignmentDAO = new AssignmentDAO();
    private SubmissionDAO submissionDAO = new SubmissionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");

        // 1. Kiểm tra xác thực (Bảo đảm an toàn theo yêu cầu bảo mật)
        if (currentUser == null) {
            response.sendRedirect("login");
            return;
        }

        // 2. Tiếp nhận action đặc biệt (Tải file bài nộp)
        String action = request.getParameter("action");
        if ("download_submission".equals(action)) {
            handleDownloadSubmission(request, response, currentUser);
            return;
        }

        // 3. Tiếp nhận mã môn học (hỗ trợ cả id, courseId dạng số và chuỗi mã môn như PRJ301)
        String courseIdStr = request.getParameter("id");
        if (courseIdStr == null || courseIdStr.trim().isEmpty()) {
            courseIdStr = request.getParameter("courseId");
        }
        int courseId = 1;
        try {
            if (courseIdStr != null && !courseIdStr.trim().isEmpty()) {
                courseId = Integer.parseInt(courseIdStr.trim());
            }
        } catch (NumberFormatException e) {
            if (courseIdStr != null) {
                String upper = courseIdStr.trim().toUpperCase();
                if (upper.contains("PRJ301")) courseId = 1;
                else if (upper.contains("CSD201")) courseId = 2;
                else if (upper.contains("PRO192") || upper.contains("LAB211")) courseId = 3;
                else if (upper.contains("DBI202") || upper.contains("PRF192")) courseId = 4;
                else if (upper.contains("WED201")) courseId = 5;
                else courseId = 1;
            }
        }

        // 4. Lấy thông tin cơ bản môn học (Hỗ trợ cấu trúc 4 môn học EduNext)
        Map<String, Object> courseInfo = getCourseInfo(courseId);
        request.setAttribute("course", courseInfo);
        request.setAttribute("courseId", courseId);

        // 5. Lấy danh sách bài tập của môn học này từ AssignmentDAO (TV3)
        List<Assignment> assignmentList = assignmentDAO.getAssignmentsByCourse(courseId);
        request.setAttribute("assignmentList", assignmentList);

        // 6. Phân quyền hiển thị dữ liệu bài nộp
        if ("Lecturer".equalsIgnoreCase(currentUser.getRole())) {
            // Giảng viên: Nạp toàn bộ bài nộp của các sinh viên thuộc môn học này
            Map<Integer, List<Submission>> courseSubmissionsMap = submissionDAO.getSubmissionsMapByCourse(courseId);
            request.setAttribute("courseSubmissionsMap", courseSubmissionsMap);
        } else if ("Student".equalsIgnoreCase(currentUser.getRole())) {
            // Sinh viên: Nạp danh sách bài đã nộp của cá nhân
            Map<Integer, Submission> submissionMap = submissionDAO.getStudentSubmissionsForCourse(currentUser.getUserId(), courseId);
            request.setAttribute("submissionMap", submissionMap);
        }

        // 7. Chuyển tiếp sang view hiển thị chi tiết môn học
        request.getRequestDispatcher("course_detail.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");

        if (currentUser == null) {
            response.sendRedirect("login");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) {
            action = "";
        }

        String courseIdStr = request.getParameter("courseId");
        int courseId = 1;
        try {
            if (courseIdStr != null && !courseIdStr.trim().isEmpty()) {
                courseId = Integer.parseInt(courseIdStr);
            }
        } catch (NumberFormatException ignored) {}

        // =====================================================================
        // PHÂN QUYỀN 1: GIẢNG VIÊN (LECTURER)
        // =====================================================================
        if ("Lecturer".equalsIgnoreCase(currentUser.getRole())) {
            if ("create_assignment".equals(action)) {
                // Giảng viên giao bài tập mới theo môn
                String title = request.getParameter("title");
                String description = request.getParameter("description");
                String deadlineStr = request.getParameter("deadline");

                Timestamp deadline = null;
                try {
                    if (deadlineStr != null && !deadlineStr.trim().isEmpty()) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                        Date parsedDate = sdf.parse(deadlineStr);
                        deadline = new Timestamp(parsedDate.getTime());
                    }
                } catch (Exception e) {
                    deadline = new Timestamp(System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000)); // Mặc định 7 ngày sau
                }

                if (title != null && !title.trim().isEmpty()) {
                    Assignment a = new Assignment(0, currentUser.getUserId(), currentUser.getFullName(),
                                                  courseId, title.trim(), description, deadline, null);
                    boolean ok = assignmentDAO.insertAssignmentWithCourse(a);
                    if (ok) {
                        session.setAttribute("flashSuccess", "Đã tạo bài tập mới cho môn học thành công!");
                    } else {
                        session.setAttribute("flashError", "Lỗi tạo bài tập.");
                    }
                }
            } else if ("update_deadline".equals(action)) {
                // Giảng viên cập nhật hạn nộp (Deadline)
                try {
                    int assignmentId = Integer.parseInt(request.getParameter("assignmentId"));
                    String deadlineStr = request.getParameter("deadline");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                    Date parsedDate = sdf.parse(deadlineStr);
                    Timestamp deadline = new Timestamp(parsedDate.getTime());

                    boolean ok = assignmentDAO.updateDeadline(assignmentId, deadline);
                    if (ok) {
                        session.setAttribute("flashSuccess", "Đã cập nhật deadline bài tập #" + assignmentId + " thành công!");
                    } else {
                        session.setAttribute("flashError", "Cập nhật deadline thất bại.");
                    }
                } catch (Exception e) {
                    session.setAttribute("flashError", "Định dạng thời gian deadline không hợp lệ!");
                }
            } else if ("delete_assignment".equals(action)) {
                // Giảng viên xóa bài tập
                try {
                    int assignmentId = Integer.parseInt(request.getParameter("assignmentId"));
                    boolean ok = assignmentDAO.deleteAssignment(assignmentId);
                    if (ok) {
                        session.setAttribute("flashSuccess", "Đã xóa bài tập #" + assignmentId + "!");
                    } else {
                        session.setAttribute("flashError", "Không thể xóa bài tập.");
                    }
                } catch (Exception e) {
                    session.setAttribute("flashError", "Lỗi xóa bài tập: " + e.getMessage());
                }
            }
        } 
        // =====================================================================
        // PHÂN QUYỀN 2: SINH VIÊN (STUDENT) - UPLOAD KÉO THẢ & LƯU CSDL
        // =====================================================================
        else if ("Student".equalsIgnoreCase(currentUser.getRole())) {
            if ("submit_assignment".equals(action)) {
                String assignmentIdStr = request.getParameter("assignmentId");
                int assignmentId = 0;
                try {
                    if (assignmentIdStr != null && !assignmentIdStr.trim().isEmpty()) {
                        assignmentId = Integer.parseInt(assignmentIdStr.trim());
                    }
                } catch (NumberFormatException ignored) {}

                // =================================================================
                // KIỂM TRA HẠN NỘP (DEADLINE VALIDATION)
                // =================================================================
                Assignment targetAssignment = assignmentDAO.getAssignmentById(assignmentId);
                if (targetAssignment != null && targetAssignment.isExpired()) {
                    session.setAttribute("flashError", "❌ Bài tập #" + assignmentId + " (" + targetAssignment.getTitle() 
                            + ") đã hết hạn nộp (Deadline: " + targetAssignment.getDeadline() 
                            + ")! Bạn không thể nộp hoặc chỉnh sửa bài làm nữa.");
                    response.sendRedirect("course?id=" + courseId);
                    return;
                }

                Part filePart = null;
                try {
                    filePart = request.getPart("file");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (filePart != null && filePart.getSize() > 0) {
                    try {
                        String submittedName = filePart.getSubmittedFileName();
                        String originalFileName = Paths.get(submittedName).getFileName().toString();
                        long fileSize = filePart.getSize();

                        // 1. Tính toán mã băm SHA-256 từ nội dung byte thực tế của tệp
                        MessageDigest md = MessageDigest.getInstance("SHA-256");
                        try (InputStream is = filePart.getInputStream()) {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = is.read(buffer)) != -1) {
                                md.update(buffer, 0, read);
                            }
                        }
                        byte[] hashBytes = md.digest();
                        StringBuilder sb = new StringBuilder();
                        for (byte b : hashBytes) {
                            sb.append(String.format("%02x", b));
                        }
                        String sha256 = sb.toString();

                        // 2. Thư mục lưu trữ tệp trên server (/uploads/submissions)
                        String uploadDirReal = request.getServletContext().getRealPath("/uploads/submissions");
                        if (uploadDirReal == null) {
                            uploadDirReal = System.getProperty("user.dir") + "/uploads/submissions";
                        }
                        File uploadDir = new File(uploadDirReal);
                        if (!uploadDir.exists()) {
                            uploadDir.mkdirs();
                        }

                        // 3. Đặt tên tệp an toàn tránh xung đột
                        String cleanName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
                        String safeFileName = System.currentTimeMillis() + "_" + currentUser.getUsername() + "_" + cleanName;
                        File destFile = new File(uploadDir, safeFileName);
                        filePart.write(destFile.getAbsolutePath());

                        // 4. Đồng bộ thêm bản sao vào thư mục dự án nếu khả dụng
                        try {
                            String realCtx = request.getServletContext().getRealPath("/");
                            if (realCtx != null && realCtx.contains("target")) {
                                String srcPath = realCtx.substring(0, realCtx.indexOf("target")) + "src/main/webapp/uploads/submissions";
                                File srcDir = new File(srcPath);
                                if (!srcDir.exists()) srcDir.mkdirs();
                                Files.copy(destFile.toPath(), new File(srcDir, safeFileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (Exception ignored) {}

                        // 5. Lưu bản ghi vào bảng SUBMISSIONS trong CSDL SQL Server
                        Submission sub = new Submission();
                        sub.setStudentId(currentUser.getUserId());
                        sub.setAssignmentId(assignmentId);
                        sub.setFilePath("/uploads/submissions/" + safeFileName);
                        sub.setSha256Hash(sha256);
                        sub.setFileSize((int) fileSize);
                        sub.setSubmittedAt(new Timestamp(System.currentTimeMillis()));
                        sub.setStatus("Completed");

                        boolean ok = submissionDAO.insertSubmission(sub);
                        if (ok) {
                            session.setAttribute("flashSuccess", "🎉 Sinh viên <b>" + currentUser.getFullName() 
                                    + "</b> đã nộp bài thành công tệp <b>" + originalFileName + "</b> (" 
                                    + sub.getFormattedFileSize() + ")!<br/>"
                                    + "• Mã băm SHA-256: <code style='font-size:12px; font-weight:600;'>" + sha256 + "</code><br/>"
                                    + "• Đã ghi nhận vào Cơ sở dữ liệu SQL Server (bảng <code>SUBMISSIONS</code>).");
                        } else {
                            session.setAttribute("flashError", "Lỗi CSDL: Không thể lưu thông tin bài nộp vào database.");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        session.setAttribute("flashError", "Lỗi xử lý nộp bài: " + e.getMessage());
                    }
                } else {
                    session.setAttribute("flashError", "Vui lòng chọn hoặc kéo thả tệp bài làm trước khi nộp!");
                }
            }
        }

        // Điều hướng lại về trang chi tiết môn học
        response.sendRedirect("course?id=" + courseId);
    }

    // Helper: Cung cấp thông tin môn học tương thích 4 môn học chuẩn EduNext
    private Map<String, Object> getCourseInfo(int courseId) {
        Map<String, Object> c = new HashMap<>();
        c.put("id", courseId);
        switch (courseId) {
            case 1:
                c.put("code", "PRJ301");
                c.put("name", "Java Web Application Development");
                c.put("description", "Học phần Servlet, JSP, JDBC, MVC Model và Static Code Analysis AST.");
                c.put("totalSlots", 30);
                break;
            case 2:
                c.put("code", "CSD201");
                c.put("name", "Data Structures and Algorithms");
                c.put("description", "Cấu trúc dữ liệu nâng cao, Binary Trees, Graph Traversal, Sorting.");
                c.put("totalSlots", 30);
                break;
            case 3:
                c.put("code", "PRO192");
                c.put("name", "Object-Oriented Programming (Java)");
                c.put("description", "Lập trình hướng đối tượng, Kế thừa, Đa hình, Java Collections Framework.");
                c.put("totalSlots", 30);
                break;
            case 4:
                c.put("code", "DBI202");
                c.put("name", "Introduction to Databases");
                c.put("description", "Cơ sở dữ liệu quan hệ, SQL Server T-SQL, Chuẩn hóa 1NF/2NF/3NF, Index.");
                c.put("totalSlots", 30);
                break;
            default:
                c.put("code", "COURSE-" + courseId);
                c.put("name", "Specialized Subject " + courseId);
                c.put("description", "Khóa học tích hợp đào tạo chuyên sâu và phân tích mã nguồn AST.");
                c.put("totalSlots", 30);
                break;
        }
        return c;
    }

    /**
     * Tải tệp bài nộp của sinh viên (phục vụ Giảng viên chấm điểm hoặc Sinh viên tải lại bài làm)
     */
    private void handleDownloadSubmission(HttpServletRequest request, HttpServletResponse response, User currentUser) throws IOException {
        String subIdStr = request.getParameter("submissionId");
        int subId = 0;
        try {
            if (subIdStr != null && !subIdStr.trim().isEmpty()) {
                subId = Integer.parseInt(subIdStr.trim());
            }
        } catch (NumberFormatException ignored) {}

        Submission sub = submissionDAO.getSubmissionById(subId);
        if (sub == null || sub.getFilePath() == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy bài nộp.");
            return;
        }

        // Quyền bảo mật: Chỉ Giảng viên hoặc chính Sinh viên nộp bài mới được tải
        if (!"Lecturer".equalsIgnoreCase(currentUser.getRole()) && currentUser.getUserId() != sub.getStudentId()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền tải bài nộp này.");
            return;
        }

        String realPath = request.getServletContext().getRealPath(sub.getFilePath());
        File file = null;
        if (realPath != null) {
            file = new File(realPath);
        }
        if (file == null || !file.exists()) {
            String rootPath = request.getServletContext().getRealPath("/");
            if (rootPath != null && rootPath.contains("target")) {
                String srcPath = rootPath.substring(0, rootPath.indexOf("target")) + "src/main/webapp" + sub.getFilePath();
                file = new File(srcPath);
            }
        }

        if (file == null || !file.exists()) {
            HttpSession session = request.getSession();
            session.setAttribute("flashError", "Tệp bài nộp không tồn tại trên hệ thống lưu trữ máy chủ!");
            String cId = request.getParameter("courseId") != null ? request.getParameter("courseId") : "1";
            response.sendRedirect("course?id=" + cId);
            return;
        }

        String downloadFileName = sub.getFileNameOnly();
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + downloadFileName + "\"");
        response.setContentLengthLong(file.length());

        try (InputStream in = Files.newInputStream(file.toPath());
             OutputStream out = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
        }
    }
}

