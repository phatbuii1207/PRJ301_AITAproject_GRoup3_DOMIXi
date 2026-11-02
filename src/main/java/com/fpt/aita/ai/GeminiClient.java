package com.fpt.aita.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

/**
 * Client kết nối và gửi yêu cầu tới Gemini REST API bằng HttpURLConnection.
 * Tương thích Java 1.8 (không dùng HttpClient, không dùng var).
 * Gửi API key qua HTTP header (x-goog-api-key), không đưa key vào query string URL.
 */
public class GeminiClient implements AiClient {

    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final AiConfig config;

    public GeminiClient(AiConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("AiConfig không được để null");
        }
        this.config = config;
    }

    @Override
    public AiResult analyze(AiRequest req) throws AiException {
        if (req == null) {
            throw new AiException("AiRequest không được để null", false);
        }

        if (!config.isConfigured()) {
            throw new AiException("Cấu hình Gemini chưa đầy đủ (thiếu provider=gemini, GEMINI_API_KEY hoặc GEMINI_MODEL)", false);
        }

        String systemInstruction = PromptBuilder.buildSystemInstruction();
        String userPrompt = PromptBuilder.buildUserPrompt(req);
        String requestBodyJson = buildRequestBody(systemInstruction, userPrompt);

        String model = config.getGeminiModel().trim();
        String targetUrl = GEMINI_API_BASE + model + ":generateContent";

        int maxRetries = config.getMaxRetries();
        int attempt = 0;

        while (true) {
            attempt++;
            try {
                String responseBody = executePost(targetUrl, requestBodyJson);
                String extractedText = extractCandidateText(responseBody);
                return AiResponseParser.parse(extractedText);
            } catch (AiException e) {
                // Kiểm tra xem lỗi có thể thử lại được không và còn lượt retry không
                if (e.isRetryable() && attempt <= maxRetries) {
                    long backoffMs = backoffMillis(attempt);
                    System.err.println("[GeminiClient] Gặp lỗi retryable (mã " + e.getStatusCode() + ": " + e.getMessage()
                            + "), thử lại lần " + attempt + "/" + maxRetries + " sau " + backoffMs + "ms");
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AiException("Tiến trình bị gián đoạn trong thời gian chờ thử lại", ie, false, 0);
                    }
                    continue;
                }
                throw e;
            }
        }
    }

    /**
     * Xây dựng JSON payload chuẩn cho endpoint generateContent của Gemini API.
     * Cấu trúc:
     * {
     *   "system_instruction": { "parts": [ { "text": "..." } ] },
     *   "contents": [ { "parts": [ { "text": "..." } ] } ],
     *   "generationConfig": { "response_mime_type": "application/json" }
     * }
     */
    private String buildRequestBody(String systemInstruction, String userPrompt) {
        JsonObject root = new JsonObject();

        // system_instruction
        JsonObject systemInstObj = new JsonObject();
        JsonArray sysParts = new JsonArray();
        JsonObject sysPartText = new JsonObject();
        sysPartText.addProperty("text", systemInstruction);
        sysParts.add(sysPartText);
        systemInstObj.add("parts", sysParts);
        root.add("system_instruction", systemInstObj);

        // contents
        JsonArray contents = new JsonArray();
        JsonObject userContent = new JsonObject();
        JsonArray userParts = new JsonArray();
        JsonObject userPartText = new JsonObject();
        userPartText.addProperty("text", userPrompt);
        userParts.add(userPartText);
        userContent.add("parts", userParts);
        contents.add(userContent);
        root.add("contents", contents);

        // generationConfig (yêu cầu response mime type là application/json)
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("response_mime_type", "application/json");
        root.add("generationConfig", generationConfig);

        return root.toString();
    }

    /**
     * Thực hiện HTTP POST bằng HttpURLConnection với API key truyền qua header `x-goog-api-key`.
     */
    private String executePost(String targetUrl, String jsonPayload) throws AiException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(targetUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(config.getTimeoutMs());
            conn.setReadTimeout(config.getTimeoutMs());
            conn.setDoOutput(true);
            conn.setDoInput(true);

            // Gửi header API Key và Content-Type (KHÔNG đưa key vào URL)
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("x-goog-api-key", config.getGeminiApiKey());

            // Gửi request body
            byte[] outputBytes = jsonPayload.getBytes("UTF-8");
            conn.setFixedLengthStreamingMode(outputBytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(outputBytes);
                os.flush();
            }

            int responseCode = conn.getResponseCode();

            if (responseCode >= 200 && responseCode < 300) {
                try (InputStream is = conn.getInputStream()) {
                    return readStream(is);
                }
            }

            // Xử lý khi HTTP code lỗi: đọc error stream để lấy thông báo
            String errorBody = "";
            try (InputStream es = conn.getErrorStream()) {
                if (es != null) {
                    errorBody = readStream(es);
                }
            } catch (Exception ignored) {
                // Bỏ qua lỗi đọc error stream
            }

            String sanitizedErrorMsg = sanitizeErrorMessage(errorBody, responseCode);

            // Xác định retryable: 429 (Rate limit) hoặc 5xx (Lỗi phía server Google)
            boolean retryable = isRetryableStatus(responseCode);
            throw new AiException(sanitizedErrorMsg, retryable, responseCode);

        } catch (SocketTimeoutException te) {
            // Lỗi timeout kết nối hoặc đọc stream -> retryable
            throw new AiException("Quá thời gian kết nối/đọc tới Gemini API (" + config.getTimeoutMs() + "ms): " + te.getMessage(), te, true, 408);
        } catch (AiException ae) {
            throw ae;
        } catch (Exception e) {
            // Lỗi IO hoặc mạng khác
            throw new AiException("Lỗi kết nối tới Gemini API: " + e.getMessage(), e, true, 0);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    static boolean isRetryableStatus(int responseCode) {
        return responseCode == 429 || (responseCode >= 500 && responseCode < 600);
    }

    static long backoffMillis(int attempt) {
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt phải lớn hơn 0");
        }
        return (long) attempt * 1000L;
    }

    /**
     * Đọc toàn bộ nội dung InputStream thành chuỗi UTF-8.
     */
    private String readStream(InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Bóc tách thông báo lỗi từ JSON trả về của Google để log an toàn, tuyệt đối không in API key.
     */
    private String sanitizeErrorMessage(String errorBody, int responseCode) {
        if (errorBody == null || errorBody.trim().isEmpty()) {
            return "Gemini API trả về mã lỗi HTTP " + responseCode;
        }
        try {
            JsonObject errJson = JsonParser.parseString(errorBody).getAsJsonObject();
            if (errJson.has("error")) {
                JsonObject errorObj = errJson.getAsJsonObject("error");
                String message = errorObj.has("message") ? errorObj.get("message").getAsString() : errorBody;
                return "Gemini API lỗi [HTTP " + responseCode + "]: " + message;
            }
        } catch (Exception ignored) {
            // Không parse được json thì dùng chuỗi cắt ngắn
        }
        String truncated = errorBody.length() > 300 ? errorBody.substring(0, 300) + "..." : errorBody;
        return "Gemini API lỗi [HTTP " + responseCode + "]: " + truncated.replaceAll("[\r\n]", " ");
    }

    /**
     * Bóc tách chuỗi text nằm trong candidates[0].content.parts[0].text từ phản hồi của Gemini.
     */
    private String extractCandidateText(String responseBody) throws AiException {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (!json.has("candidates") || !json.get("candidates").isJsonArray()) {
                throw new AiException("Phản hồi Gemini không có mảng 'candidates'", false);
            }
            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates.size() == 0) {
                throw new AiException("Mảng 'candidates' trong phản hồi Gemini rỗng (có thể bị bộ lọc an toàn chặn)", false);
            }

            JsonObject candidate = candidates.get(0).getAsJsonObject();
            if (!candidate.has("content") || !candidate.get("content").isJsonObject()) {
                throw new AiException("Candidate không có trường 'content'", false);
            }

            JsonObject content = candidate.getAsJsonObject("content");
            if (!content.has("parts") || !content.get("parts").isJsonArray()) {
                throw new AiException("Content không có mảng 'parts'", false);
            }

            JsonArray parts = content.getAsJsonArray("parts");
            if (parts.size() == 0) {
                throw new AiException("Mảng 'parts' rỗng trong content", false);
            }

            JsonElement partTextElem = parts.get(0).getAsJsonObject().get("text");
            if (partTextElem == null || partTextElem.isJsonNull()) {
                throw new AiException("Phần tử text trong parts[0] bị null", false);
            }

            return partTextElem.getAsString();
        } catch (JsonSyntaxException e) {
            throw new AiException("Không thể phân tích cú pháp JSON phản hồi từ Gemini: " + e.getMessage(), e, false, 0);
        } catch (IllegalStateException e) {
            throw new AiException("Cấu trúc phản hồi từ Gemini không đúng dạng mong đợi: " + e.getMessage(), e, false, 0);
        }
    }

    /**
     * Hàm main hỗ trợ kiểm thử:
     * 1. Thử nghiệm với API key sai để xác nhận thông báo lỗi rõ ràng, không crash, không retryable.
     * 2. Thử nghiệm với cấu hình thực tế từ môi trường (nếu có GEMINI_API_KEY).
     */
    public static void main(String[] args) {
        System.out.println("=== BẮT ĐẦU KIỂM THỬ GEMINI CLIENT ===\n");

        AiRequest request = new AiRequest();
        request.setMethodName("hasDuplicate");
        request.setEstimatedComplexity("O(n^2)");
        request.setCode("public boolean hasDuplicate(int[] nums) {\n"
                + "    for (int i = 0; i < nums.length; i++) {\n"
                + "        for (int j = i + 1; j < nums.length; j++) {\n"
                + "            if (nums[i] == nums[j]) return true;\n"
                + "        }\n"
                + "    }\n"
                + "    return false;\n"
                + "}");
        request.addViolation(new AiRequest.RuleViolation("PERF01", "HIGH", "Vòng lặp lồng nhau gây độ phức tạp O(n^2)", 2));

        // 1. Kiểm tra với API key sai (kịch bản lỗi không crash)
        System.out.println("--- Test 1: Kiểm thử với API Key sai ---");
        System.setProperty("GEMINI_API_KEY", "INVALID_TEST_KEY_123456");
        System.setProperty("GEMINI_MODEL", "gemini-1.5-flash");
        System.setProperty("AI_PROVIDER", "gemini");

        AiConfig invalidConfig = new AiConfig();
        GeminiClient invalidClient = new GeminiClient(invalidConfig);

        try {
            invalidClient.analyze(request);
            System.err.println("[FAIL] Lẽ ra phải ném AiException khi API key sai!");
        } catch (AiException e) {
            System.out.println("[PASS] Bắt được ngoại lệ như mong đợi:");
            System.out.println("  Mã HTTP   : " + e.getStatusCode());
            System.out.println("  Thông báo : " + e.getMessage());
            System.out.println("  Retryable : " + e.isRetryable() + " (Đúng chuẩn không thử lại với lỗi 400/401/403)");
        }
        System.out.println();

        // 2. Kiểm thử với cấu hình môi trường thực tế (nếu người dùng có set GEMINI_API_KEY thật)
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty() && !envKey.equals("INVALID_TEST_KEY_123456")) {
            System.out.println("--- Test 2: Kiểm thử với API Key thật từ môi trường ---");
            System.clearProperty("GEMINI_API_KEY"); // Xóa property giả lập để đọc từ getenv
            AiConfig realConfig = new AiConfig();
            GeminiClient realClient = new GeminiClient(realConfig);
            try {
                AiResult realResult = realClient.analyze(request);
                System.out.println("[PASS] Phân tích thành công từ Gemini API:");
                System.out.println("  Success       : " + realResult.isSuccess());
                System.out.println("  New Complexity: " + realResult.getNewComplexity());
                System.out.println("  Explanation   : " + realResult.getExplanation());
                System.out.println("  Optimized Code:\n" + realResult.getOptimizedCode());
            } catch (AiException e) {
                System.err.println("[FAIL] Gặp lỗi khi gọi API thật: " + e.getMessage());
            }
        } else {
            System.out.println("--- Test 2: Bỏ qua kiểm thử API key thật (chưa gán biến môi trường GEMINI_API_KEY).");
        }

        System.out.println("\n=== HOÀN TẤT KIỂM THỬ ===");
    }
}
