package com.fpt.aita.ai;

import java.io.Serializable;

/**
 * Tổng kết kết quả sau khi analyzeSubmission chạy xong.
 * Tương thích Java 1.8.
 */
public class AiRunSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    private int totalMethods;   // Tổng số method đủ điều kiện đưa vào phân tích
    private int analyzed;       // Số method đã được gọi AI thành công
    private int fromCache;      // Số method lấy kết quả từ cache (findByOriginalCode)
    private int skipped;        // Số method bỏ qua vì đã có optimizationn (existsForMethod)
    private int failed;         // Số method AI trả lỗi (nhưng không làm dừng toàn bộ)

    public AiRunSummary() {
    }

    public int getTotalMethods() {
        return totalMethods;
    }

    public void setTotalMethods(int totalMethods) {
        this.totalMethods = totalMethods;
    }

    public int getAnalyzed() {
        return analyzed;
    }

    public void incrementAnalyzed() {
        this.analyzed++;
    }

    public int getFromCache() {
        return fromCache;
    }

    public void incrementFromCache() {
        this.fromCache++;
    }

    public int getSkipped() {
        return skipped;
    }

    public void incrementSkipped() {
        this.skipped++;
    }

    public int getFailed() {
        return failed;
    }

    public void incrementFailed() {
        this.failed++;
    }

    @Override
    public String toString() {
        return "AiRunSummary{" +
                "totalMethods=" + totalMethods +
                ", analyzed=" + analyzed +
                ", fromCache=" + fromCache +
                ", skipped=" + skipped +
                ", failed=" + failed +
                '}';
    }
}
