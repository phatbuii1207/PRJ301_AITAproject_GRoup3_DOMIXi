package com.fpt.aita.dal;

import com.fpt.aita.model.Assignment;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AssignmentDAO extends DBContext {

    // 1. READ: Lấy danh sách tất cả bài tập (JOIN với USERS để lấy tên Giảng viên)
    public List<Assignment> getAllAssignments() {
        List<Assignment> list = new ArrayList<>();
        String sql = "SELECT a.assignment_id, a.lecturer_id, u.full_name, a.title, a.description, a.created_at "
                + "FROM ASSIGNMENTS a "
                + "JOIN USERS u ON a.lecturer_id = u.user_id "
                + "ORDER BY a.assignment_id DESC";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Assignment(
                        rs.getInt("assignment_id"),
                        rs.getInt("lecturer_id"),
                        rs.getString("full_name"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // 2. READ: Tìm 1 bài tập theo ID (dùng khi bấm nút "Sửa")
    public Assignment getAssignmentById(int id) {
        String sql = "SELECT a.assignment_id, a.lecturer_id, u.full_name, a.title, a.description, a.created_at "
                + "FROM ASSIGNMENTS a "
                + "JOIN USERS u ON a.lecturer_id = u.user_id "
                + "WHERE a.assignment_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Assignment(
                        rs.getInt("assignment_id"),
                        rs.getInt("lecturer_id"),
                        rs.getString("full_name"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 3. CREATE: Thêm bài tập mới vào SQL Server
    public boolean insertAssignment(Assignment a) {
        String sql = "INSERT INTO ASSIGNMENTS (lecturer_id, title, description) VALUES (?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, a.getLecturerId());
            ps.setString(2, a.getTitle());
            ps.setString(3, a.getDescription());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 4. UPDATE: Cập nhật thông tin bài tập
    public boolean updateAssignment(Assignment a) {
        String sql = "UPDATE ASSIGNMENTS SET title = ?, description = ? WHERE assignment_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, a.getTitle());
            ps.setString(2, a.getDescription());
            ps.setInt(3, a.getAssignmentId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 5. DELETE: Xóa bài tập theo ID
    public boolean deleteAssignment(int id) {
        String sql = "DELETE FROM ASSIGNMENTS WHERE assignment_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
