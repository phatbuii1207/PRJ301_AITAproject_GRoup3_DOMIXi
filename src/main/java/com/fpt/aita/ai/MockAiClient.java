package com.fpt.aita.ai;

import java.util.List;

/**
 * Bản client giả lập để chạy offline hoặc test mà không cần gọi mạng / API bên ngoài.
 * Trả kết quả cố định theo mã vi phạm (ruleCode).
 * Tương thích Java 1.8.
 */
public class MockAiClient implements AiClient {

    @Override
    public AiResult analyze(AiRequest req) throws AiException {
        if (req == null) {
            return AiResult.failure("AiRequest không được để null");
        }

        String primaryRule = extractPrimaryRule(req);
        String methodName = (req.getMethodName() != null && !req.getMethodName().isEmpty())
                ? req.getMethodName()
                : "processData";

        // Tùy theo ruleCode trả về giải thích sư phạm và code tối ưu phù hợp
        if ("PERF01".equalsIgnoreCase(primaryRule)) {
            String explanation = "Phương thức '" + methodName + "' đang sử dụng vòng lặp lồng nhau (nested loops) "
                    + "để tìm kiếm hoặc so khớp phần tử, dẫn đến độ phức tạp O(n^2). "
                    + "Khắc phục: Sử dụng cấu trúc dữ liệu HashMap hoặc HashSet để lưu vết và tra cứu với thời gian O(1), "
                    + "từ đó giảm tổng độ phức tạp thuật toán xuống còn O(n).";

            String optimizedCode = "// Tối ưu bằng HashMap/HashSet để giảm độ phức tạp O(n^2) xuống O(n)\n"
                    + "public void " + methodName + "(int[] arr, int target) {\n"
                    + "    java.util.Map<Integer, Integer> map = new java.util.HashMap<Integer, Integer>();\n"
                    + "    for (int i = 0; i < arr.length; i++) {\n"
                    + "        int complement = target - arr[i];\n"
                    + "        if (map.containsKey(complement)) {\n"
                    + "            // Tìm thấy cặp thỏa mãn tại index map.get(complement) và i\n"
                    + "            return;\n"
                    + "        }\n"
                    + "        map.put(arr[i], i);\n"
                    + "    }\n"
                    + "}";

            return AiResult.success(explanation, optimizedCode, "O(n)");
        }

        if ("SEC01".equalsIgnoreCase(primaryRule)) {
            String explanation = "Phương thức '" + methodName + "' có nguy cơ bị lỗi SQL Injection do nối chuỗi trực tiếp "
                    + "vào câu lệnh SQL. "
                    + "Khắc phục: Luôn sử dụng PreparedStatement với tham số '?' và try-with-resources để tự động đóng kết nối.";

            String optimizedCode = "String sql = \"SELECT * FROM Users WHERE username = ? AND password = ?\";\n"
                    + "try (Connection conn = DBContext.getConnection();\n"
                    + "     PreparedStatement ps = conn.prepareStatement(sql)) {\n"
                    + "    ps.setString(1, username);\n"
                    + "    ps.setString(2, password);\n"
                    + "    try (ResultSet rs = ps.executeQuery()) {\n"
                    + "        if (rs.next()) {\n"
                    + "            // Xử lý dữ liệu\n"
                    + "        }\n"
                    + "    }\n"
                    + "}";

            return AiResult.success(explanation, optimizedCode, "O(1)");
        }

        if ("RES01".equalsIgnoreCase(primaryRule)) {
            String explanation = "Phát hiện tài nguyên IO/Database không được đóng đúng cách khi xảy ra exception. "
                    + "Khắc phục: Sử dụng cú pháp try-with-resources (Java 7+) để tài nguyên tự động giải phóng.";

            String optimizedCode = "try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(filePath))) {\n"
                    + "    String line;\n"
                    + "    while ((line = br.readLine()) != null) {\n"
                    + "        // Đọc từng dòng an toàn\n"
                    + "    }\n"
                    + "}";

            return AiResult.success(explanation, optimizedCode, "O(n)");
        }

        // Trường hợp mặc định nếu không khớp rule cụ thể
        String defaultExplanation = "Phương thức '" + methodName + "' đã được kiểm tra sơ bộ. "
                + "Khuyến nghị: Rà soát điều kiện biên, kiểm tra null trước khi gọi phương thức, "
                + "và tối ưu hóa thuật toán để đạt hiệu năng tốt hơn.";

        String defaultOptimizedCode = req.getCode() != null ? req.getCode() : "// Không có mã nguồn gốc";
        String currentComplexity = (req.getEstimatedComplexity() != null && !req.getEstimatedComplexity().isEmpty())
                ? req.getEstimatedComplexity()
                : "O(n)";

        return AiResult.success(defaultExplanation, defaultOptimizedCode, currentComplexity);
    }

    /**
     * Lấy mã vi phạm chính từ danh sách lỗi trong request.
     */
    private String extractPrimaryRule(AiRequest req) {
        List<AiRequest.RuleViolation> violations = req.getViolations();
        if (violations != null && !violations.isEmpty()) {
            for (AiRequest.RuleViolation v : violations) {
                if (v != null && v.getRuleCode() != null && !v.getRuleCode().trim().isEmpty()) {
                    return v.getRuleCode().trim();
                }
            }
        }
        return "";
    }

    /**
     * Hàm main nhỏ để tự kiểm tra nhanh mock AI client.
     */
    public static void main(String[] args) {
        try {
            AiClient client = new MockAiClient();
            AiRequest request = new AiRequest();
            request.setMethodName("findTwoSum");
            request.setEstimatedComplexity("O(n^2)");
            request.setCode("for (int i=0; i<n; i++) { for (int j=i+1; j<n; j++) { ... } }");
            request.addViolation(new AiRequest.RuleViolation("PERF01", "HIGH", "Nested loop detected causing quadratic complexity", 12));

            AiResult result = client.analyze(request);

            System.out.println("=== KẾT QUẢ KIỂM TRA MOCK AI CLIENT ===");
            System.out.println("Success       : " + result.isSuccess());
            System.out.println("New Complexity: " + result.getNewComplexity());
            System.out.println("Explanation   :\n" + result.getExplanation());
            System.out.println("Optimized Code:\n" + result.getOptimizedCode());
        } catch (AiException e) {
            System.err.println("Lỗi phân tích: " + e.getMessage());
        }
    }
}
