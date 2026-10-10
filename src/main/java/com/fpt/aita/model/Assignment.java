package com.fpt.aita.model;

import java.sql.Timestamp;

public class Assignment {

    private int assignmentId;
    private int lecturerId;
    private String lecturerName; // Tên giảng viên (lấy qua JOIN)
    private String title;
    private String description;
    private Timestamp createdAt;

    public Assignment() {
    }

    public Assignment(int assignmentId, int lecturerId, String lecturerName, String title, String description, Timestamp createdAt) {
        this.assignmentId = assignmentId;
        this.lecturerId = lecturerId;
        this.lecturerName = lecturerName;
        this.title = title;
        this.description = description;
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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
