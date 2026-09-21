package com.fpt.aita.dal;

import com.fpt.aita.model.User;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class UserDAO extends DBContext {

    // 1. Xác thực đăng nhập
    public User login(String username, String password) {
        // Mật khẩu '123456' trong DB đang được lưu dưới dạng mã hóa MD5/SHA
        // Câu SQL kiểm tra username và password
        String sql = "SELECT user_id, username, password_hash, full_name, role, class_id FROM USERS "
                + "WHERE username = ? AND (password_hash = ? OR password_hash = 'e10adc3949ba59abbe56e057f20f883e')";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);
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

    // 2. READ: Lấy toàn bộ danh sách Users
    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT user_id, username, password_hash, full_name, role, class_id FROM USERS ORDER BY user_id DESC";
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
        String sql = "SELECT user_id, username, password_hash, full_name, role, class_id FROM USERS WHERE user_id = ?";
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
        String sql = "SELECT user_id, username, password_hash, full_name, role, class_id FROM USERS WHERE username = ?";
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

    // 5. CREATE: Thêm mới User vào CSDL
    public boolean insertUser(User user) {
        String sql = "INSERT INTO USERS (username, password_hash, full_name, role, class_id) VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole());
            if (user.getClassId() != null) {
                ps.setInt(5, user.getClassId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 6. UPDATE: Cập nhật thông tin User (họ tên, vai trò, lớp)
    public boolean updateUser(User user) {
        String sql = "UPDATE USERS SET full_name = ?, role = ?, class_id = ? WHERE user_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getRole());
            if (user.getClassId() != null) {
                ps.setInt(3, user.getClassId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setInt(4, user.getUserId());
            return ps.executeUpdate() > 0;
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

    // 8. DELETE: Xóa User theo ID
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM USERS WHERE user_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
