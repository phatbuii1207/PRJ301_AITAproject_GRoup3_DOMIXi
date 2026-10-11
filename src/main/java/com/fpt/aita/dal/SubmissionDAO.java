package com.fpt.aita.dal;

import com.fpt.aita.model.Submission;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubmissionDAO extends DBContext {

    /**
     * Thêm mới một bản ghi nộp bài của sinh viên vào CSDL SQL Server
     */
    public boolean insertSubmission(Submission s) {
        String sql = "INSERT INTO SUBMISSIONS (student_id, assignment_id, file_path, sha256_hash, file_size, submitted_at, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, s.getStudentId());
            ps.setInt(2, s.getAssignmentId());
            ps.setString(3, s.getFilePath());
            ps.setString(4, s.getSha256Hash());
            ps.setInt(5, s.getFileSize());
            ps.setTimestamp(6, s.getSubmittedAt() != null ? s.getSubmittedAt() : new Timestamp(System.currentTimeMillis()));
            ps.setString(7, s.getStatus() != null ? s.getStatus() : "Completed");
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Lấy bài nộp gần nhất của sinh viên cho một bài tập cụ thể
     */
    public Submission getLatestSubmission(int studentId, int assignmentId) {
        String sql = "SELECT TOP 1 s.submission_id, s.student_id, u.full_name, s.assignment_id, a.title, "
                   + "s.file_path, s.sha256_hash, s.file_size, s.submitted_at, s.status "
                   + "FROM SUBMISSIONS s "
                   + "JOIN USERS u ON s.student_id = u.user_id "
                   + "LEFT JOIN ASSIGNMENTS a ON s.assignment_id = a.assignment_id "
                   + "WHERE s.student_id = ? AND s.assignment_id = ? "
                   + "ORDER BY s.submitted_at DESC";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, studentId);
            ps.setInt(2, assignmentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Submission(
                        rs.getInt("submission_id"),
                        rs.getInt("student_id"),
                        rs.getString("full_name"),
                        rs.getInt("assignment_id"),
                        rs.getString("title"),
                        rs.getString("file_path"),
                        rs.getString("sha256_hash"),
                        rs.getInt("file_size"),
                        rs.getTimestamp("submitted_at"),
                        rs.getString("status")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy danh đồ (Map) bài nộp của sinh viên cho toàn bộ môn học: Key là assignment_id
     */
    public Map<Integer, Submission> getStudentSubmissionsForCourse(int studentId, int courseId) {
        Map<Integer, Submission> map = new HashMap<>();
        String sql = "SELECT s.submission_id, s.student_id, u.full_name, s.assignment_id, a.title, "
                   + "s.file_path, s.sha256_hash, s.file_size, s.submitted_at, s.status "
                   + "FROM SUBMISSIONS s "
                   + "JOIN USERS u ON s.student_id = u.user_id "
                   + "JOIN ASSIGNMENTS a ON s.assignment_id = a.assignment_id "
                   + "WHERE s.student_id = ? AND a.course_id = ? "
                   + "ORDER BY s.submitted_at ASC";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Submission sub = new Submission(
                        rs.getInt("submission_id"),
                        rs.getInt("student_id"),
                        rs.getString("full_name"),
                        rs.getInt("assignment_id"),
                        rs.getString("title"),
                        rs.getString("file_path"),
                        rs.getString("sha256_hash"),
                        rs.getInt("file_size"),
                        rs.getTimestamp("submitted_at"),
                        rs.getString("status")
                );
                map.put(sub.getAssignmentId(), sub);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * Lấy tất cả bài nộp theo bài tập (dành cho giảng viên theo dõi tiến độ nộp bài)
     */
    public List<Submission> getSubmissionsByAssignment(int assignmentId) {
        List<Submission> list = new ArrayList<>();
        String sql = "SELECT s.submission_id, s.student_id, u.full_name, s.assignment_id, a.title, "
                   + "s.file_path, s.sha256_hash, s.file_size, s.submitted_at, s.status "
                   + "FROM SUBMISSIONS s "
                   + "JOIN USERS u ON s.student_id = u.user_id "
                   + "JOIN ASSIGNMENTS a ON s.assignment_id = a.assignment_id "
                   + "WHERE s.assignment_id = ? "
                   + "ORDER BY s.submitted_at DESC";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, assignmentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Submission(
                        rs.getInt("submission_id"),
                        rs.getInt("student_id"),
                        rs.getString("full_name"),
                        rs.getInt("assignment_id"),
                        rs.getString("title"),
                        rs.getString("file_path"),
                        rs.getString("sha256_hash"),
                        rs.getInt("file_size"),
                        rs.getTimestamp("submitted_at"),
                        rs.getString("status")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Tìm kiếm một bài nộp theo ID (phục vụ tính năng tải file và kiểm tra)
     */
    public Submission getSubmissionById(int submissionId) {
        String sql = "SELECT s.submission_id, s.student_id, u.full_name, s.assignment_id, a.title, "
                   + "s.file_path, s.sha256_hash, s.file_size, s.submitted_at, s.status "
                   + "FROM SUBMISSIONS s "
                   + "JOIN USERS u ON s.student_id = u.user_id "
                   + "LEFT JOIN ASSIGNMENTS a ON s.assignment_id = a.assignment_id "
                   + "WHERE s.submission_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, submissionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Submission(
                        rs.getInt("submission_id"),
                        rs.getInt("student_id"),
                        rs.getString("full_name"),
                        rs.getInt("assignment_id"),
                        rs.getString("title"),
                        rs.getString("file_path"),
                        rs.getString("sha256_hash"),
                        rs.getInt("file_size"),
                        rs.getTimestamp("submitted_at"),
                        rs.getString("status")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy danh đồ bài nộp của toàn bộ môn học nhóm theo assignment_id (dành cho Giảng viên)
     */
    public Map<Integer, List<Submission>> getSubmissionsMapByCourse(int courseId) {
        Map<Integer, List<Submission>> map = new HashMap<>();
        String sql = "SELECT s.submission_id, s.student_id, u.full_name, s.assignment_id, a.title, "
                   + "s.file_path, s.sha256_hash, s.file_size, s.submitted_at, s.status "
                   + "FROM SUBMISSIONS s "
                   + "JOIN USERS u ON s.student_id = u.user_id "
                   + "JOIN ASSIGNMENTS a ON s.assignment_id = a.assignment_id "
                   + "WHERE a.course_id = ? "
                   + "ORDER BY s.submitted_at DESC";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, courseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Submission sub = new Submission(
                        rs.getInt("submission_id"),
                        rs.getInt("student_id"),
                        rs.getString("full_name"),
                        rs.getInt("assignment_id"),
                        rs.getString("title"),
                        rs.getString("file_path"),
                        rs.getString("sha256_hash"),
                        rs.getInt("file_size"),
                        rs.getTimestamp("submitted_at"),
                        rs.getString("status")
                );
                if (!map.containsKey(sub.getAssignmentId())) {
                    map.put(sub.getAssignmentId(), new ArrayList<>());
                }
                map.get(sub.getAssignmentId()).add(sub);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }
}

