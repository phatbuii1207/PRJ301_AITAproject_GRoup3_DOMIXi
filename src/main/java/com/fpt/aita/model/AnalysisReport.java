package com.fpt.aita.model;

import java.sql.Timestamp;

/**
 * Model đại diện cho Báo cáo Đánh giá Chất lượng Mã nguồn (ANALYSIS_REPORTS)
 */
public class AnalysisReport {

    private int reportId;
    private int submissionId;
    private int qualityScore;
    private int totalIssues;
    private Timestamp createdAt;

    // Các trường thông tin bổ trợ nạp từ SUBMISSIONS, USERS, ASSIGNMENTS
    private int studentId;
    private String studentName;
    private String studentUsername;
    private int assignmentId;
    private String assignmentTitle;
    private String filePath;
    private String fileName;
    private int fileSize;
    private Timestamp submittedAt;
    private String submissionStatus;

    // Thống kê phân loại lỗi
    private int criticalCount;
    private int warningCount;
    private int infoCount;
    private int loc;

    public AnalysisReport() {
    }

    public AnalysisReport(int reportId, int submissionId, int qualityScore, int totalIssues, Timestamp createdAt) {
        this.reportId = reportId;
        this.submissionId = submissionId;
        this.qualityScore = qualityScore;
        this.totalIssues = totalIssues;
        this.createdAt = createdAt;
    }

    public int getReportId() {
        return reportId;
    }

    public void setReportId(int reportId) {
        this.reportId = reportId;
    }

    public int getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(int submissionId) {
        this.submissionId = submissionId;
    }

    public int getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(int qualityScore) {
        this.qualityScore = qualityScore;
    }

    public int getTotalIssues() {
        return totalIssues;
    }

    public void setTotalIssues(int totalIssues) {
        this.totalIssues = totalIssues;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
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

    public String getStudentUsername() {
        return studentUsername;
    }

    public void setStudentUsername(String studentUsername) {
        this.studentUsername = studentUsername;
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

    public String getFileName() {
        if (fileName != null && !fileName.isEmpty()) {
            return fileName;
        }
        if (filePath != null) {
            int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
            return (lastSlash >= 0) ? filePath.substring(lastSlash + 1) : filePath;
        }
        return "submission.zip";
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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

    public String getSubmissionStatus() {
        return submissionStatus;
    }

    public void setSubmissionStatus(String submissionStatus) {
        this.submissionStatus = submissionStatus;
    }

    public int getCriticalCount() {
        return criticalCount;
    }

    public void setCriticalCount(int criticalCount) {
        this.criticalCount = criticalCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public int getInfoCount() {
        return infoCount;
    }

    public void setInfoCount(int infoCount) {
        this.infoCount = infoCount;
    }

    public int getLoc() {
        return loc;
    }

    public void setLoc(int loc) {
        this.loc = loc;
    }

    // Helper methods phân loại điểm cho giao diện
    public String getScoreClass() {
        if (qualityScore >= 80) return "good";
        if (qualityScore >= 50) return "mid";
        return "bad";
    }

    public String getScoreColor() {
        if (qualityScore >= 80) return "#10b981"; // Emerald green
        if (qualityScore >= 50) return "#f59e0b"; // Amber orange
        return "#ef4444"; // Red
    }

    public String getScoreLabel() {
        if (qualityScore >= 90) return "Xuất Sắc (Clean Code)";
        if (qualityScore >= 80) return "Tốt (Đạt Tiêu Chuẩn)";
        if (qualityScore >= 65) return "Khá (Cần Tối Ưu Thêm)";
        if (qualityScore >= 50) return "Trung Bình (Nhiều Cảnh Báo)";
        return "Kém (Có Lỗi Nghiêm Trọng)";
    }
}
