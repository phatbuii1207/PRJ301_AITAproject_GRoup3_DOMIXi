package com.fpt.aita.model;

import java.math.BigDecimal;

public final class GradingCaseResult {
    public enum Verdict {
        ACCEPTED, WRONG_ANSWER, COMPILE_ERROR, RUNTIME_ERROR, TIME_LIMIT_EXCEEDED,
        OUTPUT_LIMIT_EXCEEDED, INVALID_SUBMISSION, INFRASTRUCTURE_ERROR, NOT_RUN
    }
    private final int order;
    private final GradingTestCase test;
    private final Verdict verdict;
    private final String actualOutput, diagnostics;
    private final long executionTimeMs;
    public GradingCaseResult(int order, GradingTestCase test, Verdict verdict,
            String actualOutput, String diagnostics, long executionTimeMs) {
        this.order = order; this.test = test; this.verdict = verdict;
        this.actualOutput = actualOutput; this.diagnostics = diagnostics; this.executionTimeMs = executionTimeMs;
    }
    public int getOrder() { return order; }
    public String getName() { return test.getName(); }
    public String getInput() { return test.getInput(); }
    public String getExpectedOutput() { return test.getExpectedOutput(); }
    public BigDecimal getWeight() { return test.getWeight(); }
    public boolean isHidden() { return test.isHidden(); }
    public Verdict getVerdict() { return verdict; }
    public String getActualOutput() { return actualOutput; }
    public String getDiagnostics() { return diagnostics; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public GradingCaseResult forStudent() {
        if (!isHidden()) return this;
        GradingTestCase redacted = new GradingTestCase("Test ẩn #" + order, "", "", getWeight(), true);
        return new GradingCaseResult(order, redacted, verdict, "", "", executionTimeMs);
    }
}
