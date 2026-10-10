package com.fpt.aita.test;

import com.fpt.aita.dal.AssignmentDAO;
import com.fpt.aita.dal.UserDAO;
import com.fpt.aita.model.Assignment;
import com.fpt.aita.model.User;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

public class DemoMenuApp {

    private static Scanner scanner;
    private static final UserDAO userDAO = new UserDAO();
    private static final AssignmentDAO assignmentDAO = new AssignmentDAO();

    static {
        // Cấu hình UTF-8 cho Console Windows và Scanner nhập liệu Unikey
        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "chcp 65001 > nul").inheritIO().start().waitFor();
            }
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
        } catch (Exception ignored) {
        }

        try {
            scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            scanner = new Scanner(System.in);
        }
    }

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("   HỆ THỐNG QUẢN LÝ AITA - MENU ĐIỀU KHIỂN ");
            System.out.println("==========================================");
            System.out.println("1. Quản lý Người dùng (Users)");
            System.out.println("2. Quản lý Bài tập (Assignments)");
            System.out.println("0. Thoát chương trình");
            System.out.println("==========================================");
            System.out.print("Mời bạn chọn chức năng [0-2]: ");

            int choice = readInt();
            switch (choice) {
                case 1:
                    menuUser();
                    break;
                case 2:
                    menuAssignment();
                    break;
                case 0:
                    System.out.println("\n===> Tạm biệt! Kết thúc chương trình.");
                    return;
                default:
                    System.out.println("Lựa chọn không hợp lệ! Vui lòng nhập từ 0 đến 2.");
            }
        }
    }

    // =========================================================================
    // MENU QUẢN LÝ NGƯỜI DÙNG (USER)
    // =========================================================================
    private static void menuUser() {
        while (true) {
            System.out.println("\n------------------------------------------");
            System.out.println("         QUẢN LÝ NGƯỜI DÙNG (USER)        ");
            System.out.println("------------------------------------------");
            System.out.println("1. Xem danh sách tất cả người dùng");
            System.out.println("2. Tìm người dùng theo ID hoặc Tên đăng nhập");
            System.out.println("3. Thêm mới người dùng");
            System.out.println("4. Cập nhật thông tin người dùng");
            System.out.println("5. Xóa người dùng");
            System.out.println("0. Quay lại Menu chính");
            System.out.println("------------------------------------------");
            System.out.print("Chọn chức năng [0-5]: ");

            int choice = readInt();
            switch (choice) {
                case 1:
                    readAllUsers();
                    break;
                case 2:
                    findUser();
                    break;
                case 3:
                    createUser();
                    break;
                case 4:
                    updateUser();
                    break;
                case 5:
                    deleteUser();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Lựa chọn không hợp lệ! Vui lòng nhập lại.");
            }
        }
    }

    private static void readAllUsers() {
        System.out.println("\n--- DANH SÁCH NGƯỜI DÙNG HIỆN CÓ ---");
        List<User> list = userDAO.getAllUsers();
        if (list.isEmpty()) {
            System.out.println("Danh sách hiện đang trống!");
            return;
        }
        System.out.printf("%-6s | %-15s | %-25s | %-15s | %-10s\n", "ID", "Username", "Họ và tên", "Vai trò", "Mã lớp");
        System.out.println("----------------------------------------------------------------------------------");
        for (User u : list) {
            System.out.printf("%-6d | %-15s | %-25s | %-15s | %-10s\n",
                    u.getUserId(),
                    u.getUsername(),
                    u.getFullName(),
                    u.getRole(),
                    u.getClassId() == null ? "Chưa có" : u.getClassId());
        }
    }

    private static void findUser() {
        System.out.println("\n1. Tìm theo ID");
        System.out.println("2. Tìm theo Tên đăng nhập (Username)");
        System.out.print("Chọn phương thức tìm kiếm [1-2]: ");
        int type = readInt();
        User u = null;
        if (type == 1) {
            System.out.print("Nhập ID người dùng: ");
            int id = readInt();
            u = userDAO.getUserById(id);
        } else if (type == 2) {
            System.out.print("Nhập username: ");
            String username = scanner.nextLine().trim();
            u = userDAO.getUserByUsername(username);
        } else {
            System.out.println("Lựa chọn không hợp lệ!");
            return;
        }

        if (u != null) {
            System.out.println("===> Tìm thấy thông tin người dùng:");
            System.out.println("  Mã ID: " + u.getUserId());
            System.out.println("  Username: " + u.getUsername());
            System.out.println("  Họ và tên: " + u.getFullName());
            System.out.println("  Vai trò: " + u.getRole());
            System.out.println("  Mã lớp: " + (u.getClassId() == null ? "Chưa xếp lớp" : u.getClassId()));
        } else {
            System.out.println("===> Không tìm thấy người dùng phù hợp!");
        }
    }

    private static void createUser() {
        System.out.println("\n--- THÊM MỚI NGƯỜI DÙNG ---");
        System.out.print("Nhập username: ");
        String username = scanner.nextLine().trim();
        if (userDAO.getUserByUsername(username) != null) {
            System.out.println("Lỗi: Username đã tồn tại trong hệ thống!");
            return;
        }

        System.out.print("Nhập mật khẩu: ");
        String password = scanner.nextLine().trim();

        System.out.print("Nhập họ và tên: ");
        String fullName = scanner.nextLine().trim();

        System.out.print("Nhập vai trò (Student/Lecturer/Admin): ");
        String role = scanner.nextLine().trim();

        System.out.print("Nhập mã lớp (nhập 0 nếu chưa có): ");
        int classId = readInt();
        Integer finalClassId = (classId <= 0) ? null : classId;

        User user = new User(0, username, password, fullName, role, finalClassId);
        boolean success = userDAO.insertUser(user);
        if (success) {
            System.out.println("===> THÊM MỚI NGƯỜI DÙNG THÀNH CÔNG!");
        } else {
            System.out.println("===> Thêm mới thất bại! Vui lòng kiểm tra lại dữ liệu hoặc mã lớp.");
        }
    }

    private static void updateUser() {
        System.out.println("\n--- CẬP NHẬT THÔNG TIN NGƯỜI DÙNG ---");
        System.out.print("Nhập ID người dùng cần cập nhật: ");
        int id = readInt();
        User u = userDAO.getUserById(id);
        if (u == null) {
            System.out.println("Không tìm thấy người dùng có ID: " + id);
            return;
        }

        System.out.println("Đang chỉnh sửa: " + u.getUsername() + " (" + u.getFullName() + ")");
        System.out.print("Nhập họ tên mới (nhấn Enter để giữ nguyên): ");
        String name = scanner.nextLine().trim();
        if (!name.isEmpty()) {
            u.setFullName(name);
        }

        System.out.print("Nhập vai trò mới [Student/Lecturer/Admin] (nhấn Enter để giữ nguyên): ");
        String role = scanner.nextLine().trim();
        if (!role.isEmpty()) {
            u.setRole(role);
        }

        System.out.print("Nhập mã lớp mới (-1 để giữ nguyên, 0 để hủy lớp): ");
        int cId = readInt();
        if (cId == 0) {
            u.setClassId(null);
        } else if (cId > 0) {
            u.setClassId(cId);
        }

        boolean success = userDAO.updateUser(u);
        if (success) {
            System.out.println("===> CẬP NHẬT THÔNG TIN THÀNH CÔNG!");
        } else {
            System.out.println("===> Cập nhật thất bại!");
        }
    }

    private static void deleteUser() {
        System.out.println("\n--- XÓA NGƯỜI DÙNG ---");
        System.out.print("Nhập ID người dùng cần xóa: ");
        int id = readInt();
        User u = userDAO.getUserById(id);
        if (u == null) {
            System.out.println("Không tìm thấy người dùng có ID: " + id);
            return;
        }

        System.out.print("Bạn có chắc chắn muốn xóa người dùng '" + u.getUsername() + "'? (Y/N): ");
        String confirm = scanner.nextLine().trim();
        if (confirm.equalsIgnoreCase("Y")) {
            boolean success = userDAO.deleteUser(id);
            if (success) {
                System.out.println("===> ĐÃ XÓA NGƯỜI DÙNG THÀNH CÔNG!");
            } else {
                System.out.println("===> Xóa thất bại! (Người dùng có thể đang có dữ liệu liên kết)");
            }
        } else {
            System.out.println("Đã hủy thao tác xóa.");
        }
    }

    // =========================================================================
    // MENU QUẢN LÝ BÀI TẬP (ASSIGNMENT)
    // =========================================================================
    private static void menuAssignment() {
        while (true) {
            System.out.println("\n------------------------------------------");
            System.out.println("        QUẢN LÝ BÀI TẬP (ASSIGNMENT)      ");
            System.out.println("------------------------------------------");
            System.out.println("1. Xem danh sách tất cả bài tập");
            System.out.println("2. Xem chi tiết bài tập theo ID");
            System.out.println("3. Thêm mới bài tập");
            System.out.println("4. Cập nhật bài tập");
            System.out.println("5. Xóa bài tập");
            System.out.println("0. Quay lại Menu chính");
            System.out.println("------------------------------------------");
            System.out.print("Chọn chức năng [0-5]: ");

            int choice = readInt();
            switch (choice) {
                case 1:
                    readAllAssignments();
                    break;
                case 2:
                    readAssignmentDetail();
                    break;
                case 3:
                    createAssignment();
                    break;
                case 4:
                    updateAssignment();
                    break;
                case 5:
                    deleteAssignment();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Lựa chọn không hợp lệ! Vui lòng nhập lại.");
            }
        }
    }

    private static void readAllAssignments() {
        System.out.println("\n--- DANH SÁCH BÀI TẬP ---");
        List<Assignment> list = assignmentDAO.getAllAssignments();
        if (list.isEmpty()) {
            System.out.println("Hiện chưa có bài tập nào!");
            return;
        }
        System.out.printf("%-6s | %-30s | %-20s | %-20s\n", "ID", "Tiêu đề", "Giảng viên", "Ngày tạo");
        System.out.println("-----------------------------------------------------------------------------------");
        for (Assignment a : list) {
            System.out.printf("%-6d | %-30s | %-20s | %-20s\n",
                    a.getAssignmentId(),
                    truncate(a.getTitle(), 30),
                    a.getLecturerName() == null ? ("GV ID " + a.getLecturerId()) : a.getLecturerName(),
                    a.getCreatedAt());
        }
    }

    private static void readAssignmentDetail() {
        System.out.print("\nNhập ID bài tập cần xem: ");
        int id = readInt();
        Assignment a = assignmentDAO.getAssignmentById(id);
        if (a != null) {
            System.out.println("===> CHI TIẾT BÀI TẬP:");
            System.out.println("  Mã bài tập: " + a.getAssignmentId());
            System.out.println("  Tiêu đề: " + a.getTitle());
            System.out.println("  Giảng viên phụ trách: " + (a.getLecturerName() != null ? a.getLecturerName() : ("GV ID " + a.getLecturerId())));
            System.out.println("  Mô tả yêu cầu:\n" + a.getDescription());
            System.out.println("  Ngày tạo: " + a.getCreatedAt());
        } else {
            System.out.println("Không tìm thấy bài tập có ID: " + id);
        }
    }

    private static void createAssignment() {
        System.out.println("\n--- THÊM MỚI BÀI TẬP ---");
        System.out.print("Nhập ID Giảng viên ra đề: ");
        int lecturerId = readInt();
        User gv = userDAO.getUserById(lecturerId);
        if (gv == null) {
            System.out.println("Lỗi: Không tìm thấy người dùng có ID = " + lecturerId + " trong CSDL!");
            return;
        }

        System.out.print("Nhập tiêu đề bài tập: ");
        String title = scanner.nextLine().trim();

        System.out.print("Nhập mô tả đề bài: ");
        String desc = scanner.nextLine().trim();

        Assignment a = new Assignment(0, lecturerId, null, title, desc, null);
        boolean success = assignmentDAO.insertAssignment(a);
        if (success) {
            System.out.println("===> TẠO BÀI TẬP MỚI THÀNH CÔNG!");
        } else {
            System.out.println("===> Thêm mới bài tập thất bại!");
        }
    }

    private static void updateAssignment() {
        System.out.println("\n--- CẬP NHẬT BÀI TẬP ---");
        System.out.print("Nhập ID bài tập cần sửa: ");
        int id = readInt();
        Assignment a = assignmentDAO.getAssignmentById(id);
        if (a == null) {
            System.out.println("Không tìm thấy bài tập có ID: " + id);
            return;
        }

        System.out.println("Tiêu đề hiện tại: " + a.getTitle());
        System.out.print("Nhập tiêu đề mới (nhấn Enter để giữ nguyên): ");
        String newTitle = scanner.nextLine().trim();
        if (!newTitle.isEmpty()) {
            a.setTitle(newTitle);
        }

        System.out.println("Mô tả hiện tại: " + a.getDescription());
        System.out.print("Nhập mô tả mới (nhấn Enter để giữ nguyên): ");
        String newDesc = scanner.nextLine().trim();
        if (!newDesc.isEmpty()) {
            a.setDescription(newDesc);
        }

        boolean success = assignmentDAO.updateAssignment(a);
        if (success) {
            System.out.println("===> CẬP NHẬT BÀI TẬP THÀNH CÔNG!");
        } else {
            System.out.println("===> Cập nhật thất bại!");
        }
    }

    private static void deleteAssignment() {
        System.out.println("\n--- XÓA BÀI TẬP ---");
        System.out.print("Nhập ID bài tập cần xóa: ");
        int id = readInt();
        Assignment a = assignmentDAO.getAssignmentById(id);
        if (a == null) {
            System.out.println("Không tìm thấy bài tập có ID: " + id);
            return;
        }

        System.out.print("Bạn có chắc chắn muốn xóa bài tập '" + a.getTitle() + "'? (Y/N): ");
        String confirm = scanner.nextLine().trim();
        if (confirm.equalsIgnoreCase("Y")) {
            boolean success = assignmentDAO.deleteAssignment(id);
            if (success) {
                System.out.println("===> XÓA BÀI TẬP THÀNH CÔNG!");
            } else {
                System.out.println("===> Xóa thất bại! (Bài tập này có thể đã có sinh viên nộp bài)");
            }
        } else {
            System.out.println("Đã hủy thao tác xóa.");
        }
    }

    // =========================================================================
    // TIỆN ÍCH
    // =========================================================================
    private static int readInt() {
        while (true) {
            try {
                String line = scanner.nextLine().trim();
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.print("Vui lòng nhập số nguyên hợp lệ: ");
            }
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen - 3) + "..." : text;
    }
}

