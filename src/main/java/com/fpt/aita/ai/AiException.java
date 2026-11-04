package com.fpt.aita.ai;

/**
 * Ngoại lệ xảy ra trong quá trình gọi module AI.
 * Có cờ retryable để biểu thị request có thể thử lại hay không (ví dụ HTTP 429, 5xx, timeout).
 * Tương thích Java 1.8.
 */
public class AiException extends Exception {
    private static final long serialVersionUID = 1L;

    private final boolean retryable;
    private final int statusCode;

    public AiException(String message) {
        super(message);
        this.retryable = false;
        this.statusCode = 0;
    }

    public AiException(String message, Throwable cause) {
        super(message, cause);
        this.retryable = false;
        this.statusCode = 0;
    }

    public AiException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
        this.statusCode = 0;
    }

    public AiException(String message, boolean retryable, int statusCode) {
        super(message);
        this.retryable = retryable;
        this.statusCode = statusCode;
    }

    public AiException(String message, Throwable cause, boolean retryable, int statusCode) {
        super(message, cause);
        this.retryable = retryable;
        this.statusCode = statusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
