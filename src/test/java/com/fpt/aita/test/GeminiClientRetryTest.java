package com.fpt.aita.ai;

/**
 * Kiểm thử phân loại retry và backoff mà không thực hiện HTTP request.
 */
public class GeminiClientRetryTest {

    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;

        int[] retryableStatuses = {429, 500, 503, 599};
        for (int status : retryableStatuses) {
            if (GeminiClient.isRetryableStatus(status)) {
                passed++;
            } else {
                System.out.println("FAILED: HTTP " + status + " phải được retry.");
                failed++;
            }
        }

        int[] nonRetryableStatuses = {400, 401, 403, 404, 499, 600};
        for (int status : nonRetryableStatuses) {
            if (!GeminiClient.isRetryableStatus(status)) {
                passed++;
            } else {
                System.out.println("FAILED: HTTP " + status + " không được retry.");
                failed++;
            }
        }

        if (GeminiClient.backoffMillis(1) == 1000L && GeminiClient.backoffMillis(2) == 2000L) {
            passed++;
        } else {
            System.out.println("FAILED: backoff phải tăng tuyến tính 1s, 2s.");
            failed++;
        }

        try {
            GeminiClient.backoffMillis(0);
            System.out.println("FAILED: attempt=0 phải bị từ chối.");
            failed++;
        } catch (IllegalArgumentException expected) {
            passed++;
        }

        System.out.println("GeminiClientRetryTest: " + passed + " PASSED, " + failed + " FAILED");
        if (failed > 0) {
            throw new AssertionError("GeminiClientRetryTest thất bại.");
        }
    }
}
