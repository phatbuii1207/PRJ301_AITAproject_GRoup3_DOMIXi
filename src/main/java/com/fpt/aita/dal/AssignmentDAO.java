package com.fpt.aita.dal;

import java.sql.Connection;

import com.fpt.aita.model.Assignment;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AssignmentDAO extends DBContext {

    // 1. READ: Lấy danh sách tất cả bài tập (JOIN với USERS để lấy tên Giảng viên)
    public List<Assignment> getAllAssignments() {
        List<Assignment> list = new ArrayList<>();
        String sql = "SELECT a.assignment_id, a.lecturer_id, u.full_name, a.title, a.description, a.created_at, a.rubric_type "
                + "FROM \"ASSIGNMENTS\" a "
                + "JOIN \"USERS\" u ON a.lecturer_id = u.user_id "
                + "ORDER BY a.assignment_id DESC";
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Assignment a = new Assignment(
                        rs.getInt("assignment_id"),
                        rs.getInt("lecturer_id"),
                        rs.getString("full_name"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at")
                );
                try {
                    a.setRubricType(rs.getString("rubric_type"));
                } catch (Exception ignored) {}
                list.add(a);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // 2. READ: Tìm 1 bài tập theo ID (dùng khi bấm nút "Sửa" hoặc kiểm tra Deadline)
    public Assignment getAssignmentById(int id) {
        String sql = "SELECT a.assignment_id, a.lecturer_id, u.full_name, a.course_id, a.title, a.description, a.deadline, a.created_at, a.rubric_type "
                + "FROM \"ASSIGNMENTS\" a "
                + "JOIN \"USERS\" u ON a.lecturer_id = u.user_id "
                + "WHERE a.assignment_id = ?";
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Assignment a = new Assignment(
                        rs.getInt("assignment_id"),
                        rs.getInt("lecturer_id"),
                        rs.getString("full_name"),
                        rs.getInt("course_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getTimestamp("deadline"),
                        rs.getTimestamp("created_at")
                );
                try {
                    a.setRubricType(rs.getString("rubric_type"));
                } catch (Exception ignored) {}
                return a;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 3. CREATE: Thêm bài tập mới vào PostgreSQL
    public boolean insertAssignment(Assignment a) {
        String sql = "INSERT INTO \"ASSIGNMENTS\" (lecturer_id, title, description, rubric_type) VALUES (?, ?, ?, ?)";
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getLecturerId());
            ps.setString(2, a.getTitle());
            ps.setString(3, a.getDescription());
            ps.setString(4, a.getRubricType());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) a.setAssignmentId(rs.getInt(1));
                }
                return true;
            }
        } catch (Exception e) {
            // Fallback nếu cột rubric_type chưa có
            String fallbackSql = "INSERT INTO \"ASSIGNMENTS\" (lecturer_id, title, description) VALUES (?, ?, ?)";
            try (Connection connection = openConnection();
                 PreparedStatement ps = connection.prepareStatement(fallbackSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, a.getLecturerId());
                ps.setString(2, a.getTitle());
                ps.setString(3, a.getDescription());
                int affected = ps.executeUpdate();
                if (affected > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) a.setAssignmentId(rs.getInt(1));
                    }
                    return true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    // 4. UPDATE: Cập nhật thông tin bài tập
    public boolean updateAssignment(Assignment a) {
        String sql = "UPDATE \"ASSIGNMENTS\" SET title = ?, description = ? WHERE assignment_id = ?";
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, a.getTitle());
            ps.setString(2, a.getDescription());
            ps.setInt(3, a.getAssignmentId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 4b. UPDATE TOÀN DIỆN: Cập nhật tiêu đề, mô tả và hạn nộp (Deadline) của bài tập
    public boolean updateAssignmentFull(Assignment a) {
        String sql = "UPDATE \"ASSIGNMENTS\" SET title = ?, description = ?, deadline = ?, rubric_type = ? WHERE assignment_id = ?";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, a.getTitle());
            ps.setString(2, a.getDescription());
            ps.setTimestamp(3, a.getDeadline());
            ps.setString(4, a.getRubricType());
            ps.setInt(5, a.getAssignmentId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Notice (AssignmentDAO): updateAssignmentFull fallback: " + e.getMessage());
            return updateAssignment(a);
        }
    }

    // 5. DELETE: Xóa bài tập theo ID
    public boolean deleteAssignment(int id) {
        String sql = "DELETE FROM \"ASSIGNMENTS\" WHERE assignment_id = ?";
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // =========================================================================
    // PHẦN VIỆC CỦA THÀNH VIÊN 3: CHI TIẾT MÔN HỌC & PHÂN QUYỀN BÀI TẬP
    // (Thêm ở cuối file để bảo đảm 0% conflict khi gộp nhánh Git)
    // =========================================================================

    // 6. Lấy danh sách bài tập theo Course ID
    public List<Assignment> getAssignmentsByCourse(int courseId) {
        List<Assignment> list = new ArrayList<>();
        // Truy vấn ưu tiên nếu DB đã có cột course_id
        String sqlWithCourse = "SELECT a.assignment_id, a.lecturer_id, u.full_name, a.title, a.description, a.created_at, "
                             + "a.course_id, a.deadline, a.rubric_type "
                             + "FROM \"ASSIGNMENTS\" a "
                             + "JOIN \"USERS\" u ON a.lecturer_id = u.user_id "
                             + "WHERE a.course_id = ? "
                             + "ORDER BY a.assignment_id DESC";

        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement(sqlWithCourse)) {
            ps.setInt(1, courseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Assignment a = new Assignment(
                        rs.getInt("assignment_id"),
                        rs.getInt("lecturer_id"),
                        rs.getString("full_name"),
                        rs.getInt("course_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getTimestamp("deadline"),
                        rs.getTimestamp("created_at")
                );
                try {
                    a.setRubricType(rs.getString("rubric_type"));
                } catch (Exception ignored) {}
                list.add(a);
            }
            return list;
        } catch (Exception e) {
            // Fallback an toàn nếu TV5 chưa chạy migration cột course_id vào CSDL
            System.err.println("Notice (TV3): Fallback getAssignmentsByCourse do CSDL chưa có cột course_id: " + e.getMessage());
            return getAllAssignments();
        }
    }

    // 7. Thêm bài tập mới có liên kết course_id và deadline
    public boolean insertAssignmentWithCourse(Assignment a) {
        String sql = "INSERT INTO \"ASSIGNMENTS\" (lecturer_id, title, description, course_id, deadline, rubric_type) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getLecturerId());
            ps.setString(2, a.getTitle());
            ps.setString(3, a.getDescription());
            ps.setInt(4, a.getCourseId());
            ps.setTimestamp(5, a.getDeadline());
            ps.setString(6, a.getRubricType());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) a.setAssignmentId(rs.getInt(1));
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            // Fallback nếu cột rubric_type hoặc deadline chưa được thêm vào CSDL
            String fallbackSql = "INSERT INTO \"ASSIGNMENTS\" (lecturer_id, title, description, course_id, deadline) VALUES (?, ?, ?, ?, ?)";
            try (Connection connection = openConnection();
                 PreparedStatement ps = connection.prepareStatement(fallbackSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, a.getLecturerId());
                ps.setString(2, a.getTitle());
                ps.setString(3, a.getDescription());
                ps.setInt(4, a.getCourseId());
                ps.setTimestamp(5, a.getDeadline());
                int affected = ps.executeUpdate();
                if (affected > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) a.setAssignmentId(rs.getInt(1));
                    }
                    return true;
                }
            } catch (Exception ex) {
                System.err.println("Notice (TV3): Fallback insertAssignment: " + ex.getMessage());
                return insertAssignment(a);
            }
            return false;
        }
    }

    // 8. Giảng viên cập nhật Deadline của bài tập
    public boolean updateDeadline(int assignmentId, java.sql.Timestamp deadline) {
        String sql = "UPDATE \"ASSIGNMENTS\" SET deadline = ? WHERE assignment_id = ?";
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, deadline);
            ps.setInt(2, assignmentId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 9. Cập nhật Rubric Type của bài tập (NONE / LINKEDLIST / BST)
    public boolean updateRubricType(int assignmentId, String rubricType) {
        if (assignmentId <= 0) {
            return true;
        }
        String normalized = (rubricType == null || rubricType.trim().isEmpty()) ? "NONE" : rubricType.trim();
        String sql = "UPDATE \"ASSIGNMENTS\" SET rubric_type = ? WHERE assignment_id = ?";
        try (Connection connection = openConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, normalized);
            ps.setInt(2, assignmentId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Notice (AssignmentDAO): updateRubricType: " + e.getMessage());
            return false;
        }
    }
}
