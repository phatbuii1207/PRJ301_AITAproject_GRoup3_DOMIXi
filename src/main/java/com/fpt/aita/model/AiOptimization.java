package com.fpt.aita.model;

import java.sql.Timestamp;

/**
 * Model đại diện cho Đề xuất Tối ưu hóa Thuật toán từ AI (AI_OPTIMIZATIONS)
 */
public class AiOptimization {

    private int optimizationId;
    private int methodId;
    private String methodName;
    private String fileName;
    private String originalCode;
    private String optimizedCode;
    private String explanation;
    private Timestamp createdAt;

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
    }
}
