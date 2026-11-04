package com.fpt.aita.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GradingConfig {
    private final String mainClass;
    private final int version;
    private final List<GradingTestCase> tests;
    public GradingConfig(String mainClass, int version, List<GradingTestCase> tests) {
        if (mainClass == null || mainClass.length() > 200
                || !mainClass.matches("[a-zA-Z_$][a-zA-Z0-9_$]*(\\.[a-zA-Z_$][a-zA-Z0-9_$]*)*")
                || tests == null || tests.isEmpty() || tests.size() > 20 || tests.contains(null)) {
            throw new IllegalArgumentException("Main class phải hợp lệ; mỗi bài cần từ 1 đến 20 test case.");
        }
        this.mainClass = mainClass; this.version = version;
        this.tests = Collections.unmodifiableList(new ArrayList<>(tests));
    }
    public String getMainClass() { return mainClass; }
    public int getVersion() { return version; }
    public List<GradingTestCase> getTests() { return tests; }
}
