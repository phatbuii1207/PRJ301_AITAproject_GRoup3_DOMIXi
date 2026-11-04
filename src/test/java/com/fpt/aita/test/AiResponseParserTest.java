package com.fpt.aita.test;

import com.fpt.aita.ai.AiException;
import com.fpt.aita.ai.AiResponseParser;
import com.fpt.aita.ai.AiResult;

/**
 * Kiểm thử đơn vị cho AiResponseParser.
 * Tương thích Java 1.8.
 */
public class AiResponseParserTest {

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("     BẮT ĐẦU KIỂM THỬ AiResponseParser             ");
        System.out.println("====================================================");

        int passed = 0;
        int failed = 0;

        // TEST 1: JSON đúng chuẩn với đầy đủ 3 trường
        try {
            System.out.print("[TEST 1] Parse JSON hợp lệ chuẩn 3 trường... ");
            String validJson = "{\n"
                    + "  \"explanation\": \"Sử dụng HashMap để giảm độ phức tạp từ O(N^2) xuống O(N).\",\n"
                    + "  \"optimized_code\": \"public void test() { Map<Integer, Integer> map = new HashMap<>(); }\",\n"
                    + "  \"new_complexity\": \"O(N)\"\n"
                    + "}";
            AiResult res = AiResponseParser.parse(validJson);
            if (res != null && res.isSuccess()
                    && "O(N)".equals(res.getNewComplexity())
                    && res.getExplanation().contains("HashMap")
                    && res.getOptimizedCode().contains("HashMap")) {
                System.out.println("PASSED");
                passed++;
            } else {
                System.out.println("FAILED (Kết quả parse không khớp)");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 2: JSON được bọc trong markdown fence ```json ... ```
        try {
            System.out.print("[TEST 2] Parse JSON có rào markdown ```json... ");
            String markdownFencedJson = "```json\n"
                    + "{\n"
                    + "  \"explanation\": \"Dùng PreparedStatement tránh SQL Injection.\",\n"
                    + "  \"optimized_code\": \"PreparedStatement ps = conn.prepareStatement(sql);\",\n"
                    + "  \"new_complexity\": \"O(1)\"\n"
                    + "}\n"
                    + "```";
            AiResult res = AiResponseParser.parse(markdownFencedJson);
            if (res != null && res.isSuccess()
                    && "O(1)".equals(res.getNewComplexity())
                    && res.getOptimizedCode().contains("PreparedStatement")) {
                System.out.println("PASSED (Tự động gỡ rào markdown thành công)");
                passed++;
            } else {
                System.out.println("FAILED (Không xử lý được rào markdown)");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 3: JSON thiếu trường bắt buộc (thiếu optimized_code)
        try {
            System.out.print("[TEST 3] Parse JSON thiếu trường bắt buộc (optimized_code)... ");
            String missingFieldJson = "{\n"
                    + "  \"explanation\": \"Giải thích nhưng không đưa code sửa.\",\n"
                    + "  \"new_complexity\": \"O(N)\"\n"
                    + "}";
            try {
                AiResponseParser.parse(missingFieldJson);
                System.out.println("FAILED (Lẽ ra phải ném AiException khi thiếu trường)");
                failed++;
            } catch (AiException ae) {
                if (ae.getMessage() != null && ae.getMessage().contains("optimized_code")) {
                    System.out.println("PASSED (Bắt đúng ngoại lệ: " + ae.getMessage() + ")");
                    passed++;
                } else {
                    System.out.println("PASSED (Bắt được AiException)");
                    passed++;
                }
            }
        } catch (Exception e) {
            System.out.println("FAILED với ngoại lệ không mong muốn: " + e.getMessage());
            failed++;
        }

        // TEST 4: Chuỗi JSON bị hỏng cú pháp (cắt ngang / thiếu ngoặc)
        try {
            System.out.print("[TEST 4] Parse chuỗi JSON cú pháp hỏng (syntax error)... ");
            String malformedJson = "{\n"
                    + "  \"explanation\": \"Bị ngắt giữa chừng, không đóng ngoặc...";
            try {
                AiResponseParser.parse(malformedJson);
                System.out.println("FAILED (Lẽ ra phải ném AiException cú pháp hỏng)");
                failed++;
            } catch (AiException ae) {
                System.out.println("PASSED (Bắt đúng ngoại lệ JSON hỏng: " + ae.getMessage() + ")");
                passed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED với ngoại lệ không mong muốn: " + e.getMessage());
            failed++;
        }

        System.out.println("====================================================");
        System.out.println("KẾT QUẢ KIỂM THỬ AiResponseParser: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("====================================================");
    }
}

