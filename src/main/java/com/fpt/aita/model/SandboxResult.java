package com.fpt.aita.model;

/** Execution outcome only. SUCCESS does not mean that an answer is correct. */
public final class SandboxResult {
    public enum Status {
        SUCCESS, COMPILE_ERROR, RUNTIME_ERROR, TIME_LIMIT_EXCEEDED,
        OUTPUT_LIMIT_EXCEEDED, INVALID_SUBMISSION, SANDBOX_UNAVAILABLE,
        SANDBOX_BUSY, INTERNAL_ERROR
    }

    private final Status status;
    private final String output;
    private final long executionTimeMs;

    public SandboxResult(Status status, String output, long executionTimeMs) {
        this.status = status;
        this.output = output == null ? "" : output;
        this.executionTimeMs = executionTimeMs;
    }

    public boolean isSuccess() { return status == Status.SUCCESS; }
    public Status getStatus() { return status; }
    public String getOutput() { return output; }
    public long getExecutionTimeMs() { return executionTimeMs; }
}
