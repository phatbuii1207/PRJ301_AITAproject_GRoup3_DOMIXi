package com.fpt.aita.model;

/**
 * Model đại diện cho một môn học trên EduNext Portal
 * Dành cho Thành viên 2 (PRJ301)
 */
public class Course {
    private int courseId;
    private String courseCode;
    private String courseName;
    private String description;
    private String badge;
    private int totalSlots;

    public Course() {
    }

    public Course(int courseId, String courseCode, String courseName, String description, String badge, int totalSlots) {
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.description = description;
        this.badge = badge;
        this.totalSlots = totalSlots;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(int totalSlots) {
        this.totalSlots = totalSlots;
    }
}
