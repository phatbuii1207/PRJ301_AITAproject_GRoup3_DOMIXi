package com.fpt.aita.dal;

import com.fpt.aita.model.User;
import com.fpt.aita.util.PasswordUtil;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDAO extends DBContext {

    // 1. Xác thực đăng nhập
    public User login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return null;
        }
        String sql = "SELECT u.user_id, u.username, u.password_hash, u.full_name, u.role, "
                + "(SELECT TOP 1 uc.class_id FROM USER_CLASS uc WHERE uc.user_id = u.user_id) AS class_id "
                + "FROM USERS u "
                + "WHERE u.username = ? AND u.password_hash = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.sha256Hex(password));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        (Integer) rs.getObject("class_id"));
            }
        } catch (Exception e) {
            System.err.println("Lỗi đăng nhập: " + e.getMessage());
        }
        return null; // Đăng nhập thất bại
    }

    // 2. READ: Lấy toàn bộ danh sách Users (kèm class_id từ USER_CLASS)
    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT u.user_id, u.username, u.password_hash, u.full_name, u.role, "
                + "(SELECT TOP 1 uc.class_id FROM USER_CLASS uc WHERE uc.user_id = u.user_id) AS class_id "
                + "FROM USERS u ORDER BY u.user_id DESC";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        (Integer) rs.getObject("class_id")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // 3. READ: Tìm User theo ID
    public User getUserById(int userId) {
        String sql = "SELECT u.user_id, u.username, u.password_hash, u.full_name, u.role, "
                + "(SELECT TOP 1 uc.class_id FROM USER_CLASS uc WHERE uc.user_id = u.user_id) AS class_id "
                + "FROM USERS u WHERE u.user_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        (Integer) rs.getObject("class_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 4. READ: Tìm User theo Username
    public User getUserByUsername(String username) {
        String sql = "SELECT u.user_id, u.username, u.password_hash, u.full_name, u.role, "
                + "(SELECT TOP 1 uc.class_id FROM USER_CLASS uc WHERE uc.user_id = u.user_id) AS class_id "
                + "FROM USERS u WHERE u.username = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        (Integer) rs.getObject("class_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 5. CREATE: Thêm mới User vào CSDL (bảng USERS và liên kết USER_CLASS nếu có)
    public boolean insertUser(User user) {
        String sql = "INSERT INTO USERS (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                if (user.getClassId() != null) {
                    try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int generatedUserId = generatedKeys.getInt(1);
                            user.setUserId(generatedUserId);
                            insertUserClass(generatedUserId, user.getClassId());
                        }
                    }
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 6. UPDATE: Cập nhật thông tin User (họ tên, vai trò và lớp học)
    public boolean updateUser(User user) {
        String sql = "UPDATE USERS SET full_name = ?, role = ? WHERE user_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getRole());
            ps.setInt(3, user.getUserId());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                if (user.getClassId() != null) {
                    // Cập nhật lại lớp trong bảng USER_CLASS
                    String delSql = "DELETE FROM USER_CLASS WHERE user_id = ?";
                    try (PreparedStatement psDel = connection.prepareStatement(delSql)) {
                        psDel.setInt(1, user.getUserId());
                        psDel.executeUpdate();
                    }
                    insertUserClass(user.getUserId(), user.getClassId());
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 7. UPDATE: Cập nhật / Đổi mật khẩu
    public boolean updatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE USERS SET password_hash = ? WHERE user_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 8. DELETE: Xóa User theo ID (xóa liên kết USER_CLASS trước)
    public boolean deleteUser(int userId) {
        String sqlClass = "DELETE FROM USER_CLASS WHERE user_id = ?";
        String sqlUser = "DELETE FROM USERS WHERE user_id = ?";
        try {
            try (PreparedStatement psClass = connection.prepareStatement(sqlClass)) {
                psClass.setInt(1, userId);
                psClass.executeUpdate();
            }
            try (PreparedStatement psUser = connection.prepareStatement(sqlUser)) {
                psUser.setInt(1, userId);
                return psUser.executeUpdate() > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 9. Kiểm tra username đã tồn tại trong CSDL chưa
    public boolean isUsernameExists(String username) {
        if (connection == null) {
            System.err.println("isUsernameExists: chưa có kết nối CSDL");
            return true;
        }
        String sql = "SELECT 1 FROM USERS WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

    // 10. Đăng ký tài khoản người dùng mới (vai trò mặc định Student, không gán class_id trực tiếp trên USERS)
    public boolean register(User u) {
        if (connection == null) {
            System.err.println("register: chưa có kết nối CSDL");
            return false;
        }
        String sql = "INSERT INTO USERS (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getFullName());
            ps.setString(4, u.getRole());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                if (u.getClassId() != null) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            int newUserId = rs.getInt(1);
                            insertUserClass(newUserId, u.getClassId());
                        }
                    }
                }
                return true;
            }
            return false;
        } catch (java.sql.SQLException e) {
            int errorCode = e.getErrorCode();
            if (errorCode == 2627 || errorCode == 2601) {
                // Vi phạm ràng buộc UNIQUE username
                return false;
            }
            e.printStackTrace();
            return false;
        }
    }

    // 11. Helper: Gán người dùng vào lớp (USER_CLASS)
    public boolean insertUserClass(int userId, int classId) {
        String sql = "INSERT INTO USER_CLASS (user_id, class_id) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, classId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("insertUserClass: " + e.getMessage());
            return false;
        }
    }

    // 12. Lấy danh sách ID các lớp mà user phụ trách / tham gia (hỗ trợ Giảng viên dạy 2 lớp)
    public List<Integer> getClassIdsByUserId(int userId) {
        List<Integer> list = new ArrayList<>();
        String sql = "SELECT class_id FROM USER_CLASS WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getInt("class_id"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}

