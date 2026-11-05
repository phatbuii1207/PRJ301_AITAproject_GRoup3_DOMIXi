package com.fpt.aita.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Bộ đọc và bóc tách kết quả JSON trả về từ LLM (Gemini).
 * Sử dụng thư viện Gson, hỗ trợ loại bỏ markdown code fence.
 * Tương thích Java 1.8.
 */
public class AiResponseParser {

    private static final int MAX_EXPLANATION_LENGTH = 2000;

    /**
     * Bóc tách raw text từ LLM thành AiResult.
     *
     * @param raw chuỗi phản hồi từ AI
     * @return AiResult hợp lệ
     * @throws AiException nếu raw rỗng, không đúng định dạng JSON hoặc thiếu các trường bắt buộc
     */
    public static AiResult parse(String raw) throws AiException {
        if (raw == null || raw.trim().isEmpty()) {
            throw new AiException("Phản hồi từ AI rỗng (empty response)", false);
        }

        String cleaned = cleanMarkdownFence(raw.trim());

        JsonObject jsonObj;
        try {
            jsonObj = JsonParser.parseString(cleaned).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            throw new AiException("Phản hồi từ AI không đúng định dạng JSON: " + e.getMessage(), e, false, 0);
        } catch (IllegalStateException e) {
            throw new AiException("Phản hồi từ AI không phải là một JSON Object: " + e.getMessage(), e, false, 0);
        }

        // Kiểm tra và lấy trường explanation
        if (!jsonObj.has("explanation") || jsonObj.get("explanation").isJsonNull()) {
            throw new AiException("JSON thiếu trường 'explanation'", false);
        }
        String explanation = jsonObj.get("explanation").getAsString().trim();
        if (explanation.isEmpty()) {
            throw new AiException("Trường 'explanation' trong JSON bị rỗng", false);
        }
        if (explanation.length() > MAX_EXPLANATION_LENGTH) {
            explanation = explanation.substring(0, MAX_EXPLANATION_LENGTH) + "... [Đã cắt bớt độ dài]";
        }

        // Kiểm tra và lấy trường optimized_code
        if (!jsonObj.has("optimized_code") || jsonObj.get("optimized_code").isJsonNull()) {
            throw new AiException("JSON thiếu trường 'optimized_code'", false);
        }
        String optimizedCode = jsonObj.get("optimized_code").getAsString().trim();
        if (optimizedCode.isEmpty()) {
            throw new AiException("Trường 'optimized_code' trong JSON bị rỗng", false);
        }

        // Kiểm tra và lấy trường new_complexity
        if (!jsonObj.has("new_complexity") || jsonObj.get("new_complexity").isJsonNull()) {
            throw new AiException("JSON thiếu trường 'new_complexity'", false);
        }
        String newComplexity = jsonObj.get("new_complexity").getAsString().trim();
        if (newComplexity.isEmpty()) {
            throw new AiException("Trường 'new_complexity' trong JSON bị rỗng", false);
        }

        Double score = null;
        if (jsonObj.has("score") && !jsonObj.get("score").isJsonNull()) {
            try {
                score = jsonObj.get("score").getAsDouble();
            } catch (Exception ignored) {}
        }

        return AiResult.success(explanation, optimizedCode, newComplexity, score);
    }

    /**
     * Loại bỏ các rào chắn markdown ```json ... ``` hoặc ``` ... ``` nếu LLM bao quanh.
     */
    public static String cleanMarkdownFence(String text) {
        if (text == null) {
            return "";
        }
        String s = text.trim();

        // Xóa ```json ở đầu
        if (s.startsWith("```json")) {
            s = s.substring(7).trim();
        } else if (s.startsWith("```JSON")) {
            s = s.substring(7).trim();
        } else if (s.startsWith("```")) {
            s = s.substring(3).trim();
        }

        // Xóa ``` ở cuối
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3).trim();
        }

        return s;
    }

    /**
     * Hàm main chạy thử nghiệm các kịch bản: JSON đúng, JSON có rào markdown, thiếu trường, JSON hỏng.
     */
    public static void main(String[] args) {
        System.out.println("=== KIỂM TRA BỘ ĐỌC AiResponseParser ===\n");

        // 1. Trường hợp JSON hợp lệ chuẩn
        String jsonChuan = "{\n"
                + "  \"explanation\": \"Vòng lặp lồng nhau gây O(n^2), thay bằng HashMap để đạt O(n).\",\n"
                + "  \"optimized_code\": \"public void test() { ... }\",\n"
                + "  \"new_complexity\": \"O(n)\"\n"
                + "}";
        testCase("1. JSON Chuẩn", jsonChuan);

        // 2. Trường hợp có bọc markdown ```json ... ```
        String jsonBocMarkdown = "```json\n"
                + "{\n"
                + "  \"explanation\": \"Phát hiện nối chuỗi SQL, dùng PreparedStatement để chống SQL Injection.\",\n"
                + "  \"optimized_code\": \"try (PreparedStatement ps = conn.prepareStatement(sql)) { ... }\",\n"
                + "  \"new_complexity\": \"O(1)\"\n"
                + "}\n"
                + "```";
        testCase("2. JSON bọc rào Markdown ```json", jsonBocMarkdown);

        // 3. Trường hợp thiếu trường (thiếu optimized_code)
        String jsonThieuTruong = "{\n"
                + "  \"explanation\": \"Giải thích sơ bộ\",\n"
                + "  \"new_complexity\": \"O(n)\"\n"
                + "}";
        testCase("3. JSON thiếu trường 'optimized_code'", jsonThieuTruong);

        // 4. Trường hợp JSON bị hỏng cú pháp
        String jsonHong = "{\n"
                + "  \"explanation\": \"Bị ngắt quãng giữa chừng...\n";
        testCase("4. JSON cú pháp hỏng", jsonHong);
    }

    private static void testCase(String title, String input) {
        System.out.println("--- Test: " + title + " ---");
        try {
            AiResult result = parse(input);
            System.out.println("[PASS] Parse thành công:");
            System.out.println("  Success       : " + result.isSuccess());
            System.out.println("  Explanation   : " + result.getExplanation());
            System.out.println("  New Complexity: " + result.getNewComplexity());
            System.out.println("  Optimized Code: " + result.getOptimizedCode());
        } catch (AiException e) {
            System.out.println("[BẮT ĐƯỢC NGOẠI LỆ HỢP LỆ AiException]");
            System.out.println("  Thông báo lỗi : " + e.getMessage());
            System.out.println("  Retryable     : " + e.isRetryable());
        }
        System.out.println();
    }
}
