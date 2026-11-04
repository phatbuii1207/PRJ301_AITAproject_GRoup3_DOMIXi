package com.fpt.aita.ai;

import java.io.Serializable;

/**
 * Kết quả phân tích và tối ưu hóa code từ AI.
 * Tương thích Java 1.8.
 */
public class AiResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String explanation;      // Giải thích sư phạm chi tiết nguyên nhân và cách khắc phục
    private String optimizedCode;    // Mã nguồn sau khi đã được tối ưu hóa
    private String newComplexity;    // Độ phức tạp mới sau tối ưu (ví dụ: O(n))
    private String errorMessage;     // Thông báo lỗi nếu việc phân tích thất bại

    public AiResult() {
    }

    public AiResult(boolean success, String explanation, String optimizedCode, String newComplexity, String errorMessage) {
        this.success = success;
        this.explanation = explanation;
        this.optimizedCode = optimizedCode;
        this.newComplexity = newComplexity;
        this.errorMessage = errorMessage;
    }

    /**
     * Tạo kết quả thành công nhanh.
     */
    public static AiResult success(String explanation, String optimizedCode, String newComplexity) {
        return new AiResult(true, explanation, optimizedCode, newComplexity, null);
    }

    /**
     * Tạo kết quả thất bại nhanh.
     */
    public static AiResult failure(String errorMessage) {
        return new AiResult(false, null, null, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getOptimizedCode() {
        return optimizedCode;
    }

    public void setOptimizedCode(String optimizedCode) {
        this.optimizedCode = optimizedCode;
    }

    public String getNewComplexity() {
        return newComplexity;
    }

    public void setNewComplexity(String newComplexity) {
        this.newComplexity = newComplexity;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "AiResult{" +
                "success=" + success +
                ", explanation='" + (explanation != null ? (explanation.length() > 60 ? explanation.substring(0, 60) + "..." : explanation) : "null") + '\'' +
                ", newComplexity='" + newComplexity + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
