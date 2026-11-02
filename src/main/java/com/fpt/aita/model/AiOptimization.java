package com.fpt.aita.model;

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
    private String originalCode;
    private String optimizedCode;
    private String explanation;
    private Timestamp createdAt;

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

    @Override
    public String toString() {
        return "AiOptimization{" +
                "optimizationId=" + optimizationId +
                ", methodId=" + methodId +
                ", createdAt=" + createdAt +
                '}';
    }
}
