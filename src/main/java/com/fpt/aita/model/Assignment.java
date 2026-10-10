package com.fpt.aita.model;

import java.sql.Timestamp;

public class Assignment {

    private int assignmentId;
    private int lecturerId;
    private String lecturerName; // Tên giảng viên (lấy qua JOIN)
    private int courseId;        // Mã môn học (Thành viên 3 phụ trách)
    private String title;
    private String description;
    private Timestamp deadline;  // Thời hạn nộp bài
    private Timestamp createdAt;

    public Assignment() {
    }

    // Constructor nguyên bản (bảo đảm tương thích 100% với code của các bạn khác)
    public Assignment(int assignmentId, int lecturerId, String lecturerName, String title, String description, Timestamp createdAt) {
        this.assignmentId = assignmentId;
        this.lecturerId = lecturerId;
        this.lecturerName = lecturerName;
        this.title = title;
        this.description = description;
        this.createdAt = createdAt;
    }

    // Constructor mở rộng có courseId và deadline (Thành viên 3)
    public Assignment(int assignmentId, int lecturerId, String lecturerName, int courseId, String title, String description, Timestamp deadline, Timestamp createdAt) {
        this.assignmentId = assignmentId;
        this.lecturerId = lecturerId;
        this.lecturerName = lecturerName;
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.createdAt = createdAt;
    }

    public int getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(int assignmentId) {
        this.assignmentId = assignmentId;
    }

    public int getLecturerId() {
        return lecturerId;
    }

    public void setLecturerId(int lecturerId) {
        this.lecturerId = lecturerId;
    }

    public String getLecturerName() {
        return lecturerName;
    }

    public void setLecturerName(String lecturerName) {
        this.lecturerName = lecturerName;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
