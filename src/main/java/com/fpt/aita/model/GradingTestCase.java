package com.fpt.aita.model;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

public final class GradingTestCase {
    private final String name, input, expectedOutput;
    private final BigDecimal weight;
    private final boolean hidden;

    public GradingTestCase(String name, String input, String expectedOutput, BigDecimal weight, boolean hidden) {
        if (name == null || name.trim().isEmpty() || name.length() > 80 || input == null || expectedOutput == null
                || input.getBytes(StandardCharsets.UTF_8).length > 65536
                || expectedOutput.getBytes(StandardCharsets.UTF_8).length > 65536
                || weight == null || weight.signum() <= 0 || weight.compareTo(new BigDecimal("1000")) > 0
                || weight.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Test cần tên (tối đa 80 ký tự), trọng số > 0 đến 1000 (2 số lẻ), input/đáp án tối đa 64 KiB.");
        }
        this.name = name.trim(); this.input = input; this.expectedOutput = expectedOutput;
        this.weight = weight; this.hidden = hidden;
    }
    public String getName() { return name; }
    public String getInput() { return input; }
    public String getExpectedOutput() { return expectedOutput; }
    public BigDecimal getWeight() { return weight; }
    public boolean isHidden() { return hidden; }
}
