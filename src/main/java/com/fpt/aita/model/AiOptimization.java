package com.fpt.aita.model;

<<<<<<< HEAD
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model ánh xạ bảng AI_OPTIMIZATIONS trong cơ sở dữ liệu.
 * Tương thích Java 1.8.
 */
public class AiOptimization implements Serializable {
    private static final long serialVersionUID = 1L;

    private int optimizationId;
    private int methodId;
=======
import java.sql.Timestamp;

/**
 * Model đại diện cho Đề xuất Tối ưu hóa Thuật toán từ AI (AI_OPTIMIZATIONS)
 */
public class AiOptimization {

    private int optimizationId;
    private int methodId;
    private String methodName;
    private String fileName;
>>>>>>> origin/main
    private String originalCode;
    private String optimizedCode;
    private String explanation;
    private Timestamp createdAt;

<<<<<<< HEAD
    public AiOptimization() {
    }

    public AiOptimization(int methodId, String originalCode, String optimizedCode, String explanation) {
        this.methodId = methodId;
        this.originalCode = originalCode;
        this.optimizedCode = optimizedCode;
        this.explanation = explanation;
    }

    public AiOptimization(int optimizationId, int methodId, String originalCode, String optimizedCode, String explanation, Timestamp createdAt) {
        this.optimizationId = optimizationId;
        this.methodId = methodId;
=======
    // Thông tin bổ trợ bóc tách từ METHODS
    private String complexityBefore;
    private String complexityAfter;

    public AiOptimization() {
    }

    public AiOptimization(int optimizationId, int methodId, String methodName, String fileName,
                          String originalCode, String optimizedCode, String explanation, Timestamp createdAt) {
        this.optimizationId = optimizationId;
        this.methodId = methodId;
        this.methodName = methodName;
        this.fileName = fileName;
>>>>>>> origin/main
        this.originalCode = originalCode;
        this.optimizedCode = optimizedCode;
        this.explanation = explanation;
        this.createdAt = createdAt;
    }

    public int getOptimizationId() {
        return optimizationId;
    }

    public void setOptimizationId(int optimizationId) {
        this.optimizationId = optimizationId;
    }

    public int getMethodId() {
        return methodId;
    }

    public void setMethodId(int methodId) {
        this.methodId = methodId;
    }

<<<<<<< HEAD
=======
    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

>>>>>>> origin/main
    public String getOriginalCode() {
        return originalCode;
    }

    public void setOriginalCode(String originalCode) {
        this.originalCode = originalCode;
    }

    public String getOptimizedCode() {
        return optimizedCode;
    }

    public void setOptimizedCode(String optimizedCode) {
        this.optimizedCode = optimizedCode;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

<<<<<<< HEAD
    @Override
    public String toString() {
        return "AiOptimization{" +
                "optimizationId=" + optimizationId +
                ", methodId=" + methodId +
                ", createdAt=" + createdAt +
                '}';
=======
    public String getComplexityBefore() {
        return complexityBefore;
    }

    public void setComplexityBefore(String complexityBefore) {
        this.complexityBefore = complexityBefore;
    }

    public String getComplexityAfter() {
        return complexityAfter;
    }

    public void setComplexityAfter(String complexityAfter) {
        this.complexityAfter = complexityAfter;
>>>>>>> origin/main
    }
}
