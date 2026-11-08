package com.fpt.aita.ai;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Đối tượng chứa dữ liệu yêu cầu phân tích code gửi tới AI.
 * Tương thích Java 1.8.
 */
public class AiRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String rubricType = "NONE";
    private String methodName;
    private String code;
    private String estimatedComplexity;
    private List<RuleViolation> violations;

    public AiRequest() {
        this.violations = new ArrayList<RuleViolation>();
    }

    public AiRequest(String methodName, String code, String estimatedComplexity, List<RuleViolation> violations) {
        this.methodName = methodName;
        this.code = code;
        this.estimatedComplexity = estimatedComplexity;
        this.violations = (violations != null) ? new ArrayList<RuleViolation>(violations) : new ArrayList<RuleViolation>();
    }

    public String getRubricType() { return rubricType; }

    public void setRubricType(String rubricType) {
        this.rubricType = PromptBuilder.normalizeRubricType(rubricType);
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEstimatedComplexity() {
        return estimatedComplexity;
    }

    public void setEstimatedComplexity(String estimatedComplexity) {
        this.estimatedComplexity = estimatedComplexity;
    }

    public List<RuleViolation> getViolations() {
        return violations;
    }

    public void setViolations(List<RuleViolation> violations) {
        this.violations = (violations != null) ? new ArrayList<RuleViolation>(violations) : new ArrayList<RuleViolation>();
    }

    public void addViolation(RuleViolation violation) {
        if (violation != null) {
            this.violations.add(violation);
        }
    }

    /**
     * Thông tin chi tiết một vi phạm / lỗi cần AI phân tích.
     */
    public static class RuleViolation implements Serializable {
        private static final long serialVersionUID = 1L;

        private String ruleCode;
        private String severity;
        private String message;
        private int lineNumber;

        public RuleViolation() {
        }

        public RuleViolation(String ruleCode, String severity, String message, int lineNumber) {
            this.ruleCode = ruleCode;
            this.severity = severity;
            this.message = message;
            this.lineNumber = lineNumber;
        }

        public String getRuleCode() {
            return ruleCode;
        }

        public void setRuleCode(String ruleCode) {
            this.ruleCode = ruleCode;
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

        public int getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        @Override
        public String toString() {
            return "RuleViolation{" +
                    "ruleCode='" + ruleCode + '\'' +
                    ", severity='" + severity + '\'' +
                    ", message='" + message + '\'' +
                    ", lineNumber=" + lineNumber +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "AiRequest{" +
                "methodName='" + methodName + '\'' +
                ", estimatedComplexity='" + estimatedComplexity + '\'' +
                ", violationsCount=" + (violations != null ? violations.size() : 0) +
                '}';
    }
}
