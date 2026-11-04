package com.fpt.aita.ai;

/**
 * Cấu hình cho module AI (Gemini / Mock).
 * Đọc biến môi trường, JVM property, rồi file .env theo AppConfig.
 */
public class AiConfig {

    private final String geminiApiKey;
    private final String geminiModel;
    private final String aiProvider;
    private final int timeoutMs;
    private final int maxRetries;
    private final int maxMethodsPerSubmission;
    private final String extractedRoot;

    public AiConfig() {
        this.geminiApiKey = readConfig("GEMINI_API_KEY", "");
        this.geminiModel = readConfig("GEMINI_MODEL", "");
        this.aiProvider = readConfig("AI_PROVIDER", "mock");
        this.timeoutMs = parseInt(readConfig("AI_TIMEOUT_MS", "20000"), 20000);
        this.maxRetries = parseInt(readConfig("AI_MAX_RETRIES", "2"), 2);
        this.maxMethodsPerSubmission = parseInt(readConfig("AI_MAX_METHODS_PER_SUBMISSION", "10"), 10);
        this.extractedRoot = readConfig("AI_EXTRACTED_ROOT", "");
    }

    public static AiConfig fromEnv() {
        return new AiConfig();
    }

    /**
     * Đọc cấu hình chung và giữ quy tắc trim của module AI.
     *
     * @param key tên biến môi trường hoặc system property
     * @param defaultValue giá trị mặc định nếu không tìm thấy hoặc rỗng
     * @return giá trị cấu hình đã được trim
     */
    private String readConfig(String key, String defaultValue) {
        return com.fpt.aita.config.AppConfig.get(key, defaultValue).trim();
    }

    /**
     * Chuyển chuỗi sang số nguyên an toàn, có fallback về giá trị mặc định.
     */
    private int parseInt(String val, int defaultValue) {
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Kiểm tra cấu hình có hợp lệ để gọi API hay không.
     * Trả về true khi AI_PROVIDER=gemini, có API key và GEMINI_MODEL không bị để trống.
     */
    public boolean isConfigured() {
        if (!"gemini".equalsIgnoreCase(aiProvider)) {
            return false;
        }
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            return false;
        }
        if (geminiModel == null || geminiModel.trim().isEmpty()) {
            return false;
        }
        return true;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public String getGeminiModel() {
        return geminiModel;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getMaxMethodsPerSubmission() {
        return maxMethodsPerSubmission;
    }

    public String getExtractedRoot() {
        return extractedRoot;
    }

    /**
     * Che API key khi hiển thị thông tin, chỉ hiển thị 4 ký tự cuối.
     */
    private String maskApiKey(String key) {
        if (key == null || key.isEmpty()) {
            return "(none)";
        }
        if (key.length() <= 4) {
            return "****";
        }
        return "****" + key.substring(key.length() - 4);
    }

    @Override
    public String toString() {
        return "AiConfig{" +
                "geminiApiKey='" + maskApiKey(geminiApiKey) + '\'' +
                ", geminiModel='" + geminiModel + '\'' +
                ", aiProvider='" + aiProvider + '\'' +
                ", timeoutMs=" + timeoutMs +
                ", maxRetries=" + maxRetries +
                ", maxMethodsPerSubmission=" + maxMethodsPerSubmission +
                ", extractedRoot='" + extractedRoot + '\'' +
                ", configured=" + isConfigured() +
                '}';
    }
}
