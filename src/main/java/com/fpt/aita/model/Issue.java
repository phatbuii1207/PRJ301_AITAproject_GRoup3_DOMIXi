package com.fpt.aita.model;

/**
 * Model đại diện cho Lỗi/Cảnh báo phát hiện được trên mã nguồn (ISSUES)
 */
public class Issue {

    private int issueId;
    private int fileId;
    private String fileName;
    private Integer methodId;
    private String methodName;
    private int ruleId;
    private String ruleCode;
    private String ruleName;
    private String category;
    private Integer lineNumber;
    private String severity; // 'Critical', 'Warning', 'Info'
    private String message;
    private String aiExplanation;

    public Issue() {
    }

    public Issue(int issueId, int fileId, String fileName, Integer methodId, String methodName, 
                 int ruleId, String ruleCode, String ruleName, String category, 
                 Integer lineNumber, String severity, String message, String aiExplanation) {
        this.issueId = issueId;
        this.fileId = fileId;
        this.fileName = fileName;
        this.methodId = methodId;
        this.methodName = methodName;
        this.ruleId = ruleId;
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.category = category;
        this.lineNumber = lineNumber;
        this.severity = severity;
        this.message = message;
        this.aiExplanation = aiExplanation;
    }

    public int getIssueId() {
        return issueId;
    }

    public void setIssueId(int issueId) {
        this.issueId = issueId;
    }

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getMethodId() {
        return methodId;
    }

    public void setMethodId(Integer methodId) {
        this.methodId = methodId;
    }

    public String getMethodName() {
        return (methodName != null && !methodName.isEmpty()) ? methodName : "Toàn cục / Global";
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public int getRuleId() {
        return ruleId;
    }

    public void setRuleId(int ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAiExplanation() {
        return aiExplanation;
    }

    public void setAiExplanation(String aiExplanation) {
        this.aiExplanation = aiExplanation;
    }

    // Helper methods phục vụ hiển thị CSS class trên JSP
    public String getSeverityClass() {
        if ("Critical".equalsIgnoreCase(severity)) return "sev-critical";
        if ("Warning".equalsIgnoreCase(severity)) return "sev-warning";
        return "sev-info";
    }

    public String getSeverityBadgeColor() {
        if ("Critical".equalsIgnoreCase(severity)) return "#ef4444";
        if ("Warning".equalsIgnoreCase(severity)) return "#f59e0b";
        return "#3b82f6";
    }

    public String getSeverityIcon() {
        if ("Critical".equalsIgnoreCase(severity)) return "⛔";
        if ("Warning".equalsIgnoreCase(severity)) return "⚠️";
        return "ℹ️";
    }
}
