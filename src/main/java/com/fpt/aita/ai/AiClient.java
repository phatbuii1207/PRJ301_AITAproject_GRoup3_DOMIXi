package com.fpt.aita.ai;

/**
 * Interface cho các AI Client (Gemini, Mock, ...).
 * Tương thích Java 1.8.
 */
public interface AiClient {

    /**
     * Phân tích phương thức và đề xuất giải thích, code tối ưu.
     *
     * @param req thông tin yêu cầu phân tích
     * @return kết quả phân tích từ AI
     * @throws AiException khi có lỗi xảy ra trong quá trình xử lý hoặc kết nối
     */
    AiResult analyze(AiRequest req) throws AiException;
}
