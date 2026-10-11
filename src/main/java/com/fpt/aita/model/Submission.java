package com.fpt.aita.model;

import java.sql.Timestamp;

public class Submission {
    private int submissionId;
    private int studentId;
    private String studentName;
    private int assignmentId;
    private String assignmentTitle;
    private String filePath;
    private String sha256Hash;
    private int fileSize;
    private Timestamp submittedAt;
    private String status;

    public Submission() {
    }

    public Submission(int submissionId, int studentId, String studentName, int assignmentId, 
                      String assignmentTitle, String filePath, String sha256Hash, int fileSize, 
                      Timestamp submittedAt, String status) {
        this.submissionId = submissionId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.assignmentId = assignmentId;
        this.assignmentTitle = assignmentTitle;
        this.filePath = filePath;
        this.sha256Hash = sha256Hash;
        this.fileSize = fileSize;
        this.submittedAt = submittedAt;
        this.status = status;
    }

    public int getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(int submissionId) {
        this.submissionId = submissionId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public int getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(int assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getAssignmentTitle() {
        return assignmentTitle;
    }

    public void setAssignmentTitle(String assignmentTitle) {
        this.assignmentTitle = assignmentTitle;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public Timestamp getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Timestamp submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Lấy tên tệp rút gọn từ đường dẫn lưu trữ
     */
    public String getFileNameOnly() {
        if (filePath == null) return "unknown.zip";
        int lastSlash = filePath.lastIndexOf('/');
        if (lastSlash < 0) {
            lastSlash = filePath.lastIndexOf('\\');
        }
        String name = (lastSlash >= 0) ? filePath.substring(lastSlash + 1) : filePath;
        // Bỏ tiền tố timestamp nếu có để tên file tự nhiên
        int firstUnderscore = name.indexOf('_');
        if (firstUnderscore > 0 && name.length() > firstUnderscore + 1) {
            int secondUnderscore = name.indexOf('_', firstUnderscore + 1);
            if (secondUnderscore > 0 && name.length() > secondUnderscore + 1) {
                return name.substring(secondUnderscore + 1);
            }
        }
        return name;
    }

    /**
     * Định dạng kích thước tệp hiển thị thân thiện (KB, MB)
     */
    public String getFormattedFileSize() {
        if (fileSize <= 0) return "0 B";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
    }
}
