package com.fpt.aita.ai;

/**
 * Interface cung cấp mã nguồn của một phương thức từ nguồn bất kỳ
 * (file hệ thống, database, bộ nhớ...) theo dòng bắt đầu và kết thúc.
 * Tương thích Java 1.8.
 */
public interface MethodCodeProvider {

    /**
     * Đọc đoạn mã nguồn từ startLine đến endLine (1-indexed, bao gồm 2 đầu).
     *
     * @param submissionId ID bài nộp (dùng xác định thư mục giải nén)
     * @param fileName     Tên file (ví dụ BubbleSortService.java)
     * @param startLine    Dòng bắt đầu (1-indexed)
     * @param endLine      Dòng kết thúc (1-indexed)
     * @return Mã nguồn đoạn từ startLine đến endLine; trả chuỗi rỗng nếu không tìm thấy
     */
    String getCode(int submissionId, String fileName, int startLine, int endLine);
}
