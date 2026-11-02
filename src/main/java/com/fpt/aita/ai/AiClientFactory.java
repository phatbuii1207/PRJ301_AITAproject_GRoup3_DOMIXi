package com.fpt.aita.ai;

/**
 * Factory khởi tạo AiClient dựa trên cấu hình AiConfig.
 * Tương thích Java 1.8.
 */
public class AiClientFactory {

    private AiClientFactory() {
        // Chặn khởi tạo trực tiếp instance
    }

    /**
     * Khởi tạo client phù hợp.
     * Nếu config.isConfigured() là true (provider=gemini và có API key, model),
     * sau này sẽ trả GeminiClient. Tạm thời trả MockAiClient và đánh dấu TODO.
     *
     * @param config cấu hình AI
     * @return AiClient instance
     */
    public static AiClient create(AiConfig config) {
        if (config != null && config.isConfigured()) {
            return new GeminiClient(config);
        }
        return new MockAiClient();
    }
}
