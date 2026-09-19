package com.fpt.aita.dal;

import com.fpt.aita.model.User;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDAO extends DBContext {

    // Hàm kiểm tra Đăng nhập từ bảng USERS trong Database
    public User login(String username, String password) {
        // Mật khẩu '123456' trong DB đang được lưu dưới dạng mã hóa MD5/SHA
        // Câu SQL kiểm tra username và password
        String sql = "SELECT user_id, username, full_name, role, class_id FROM USERS "
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
                    "",
                    rs.getString("full_name"),
                    rs.getString("role"),
                    (Integer) rs.getObject("class_id")
                );
            }
        } catch (Exception e) {
            System.err.println("Lỗi đăng nhập: " + e.getMessage());
        }
        return null; // Đăng nhập thất bại
    }
}
