package com.fpt.aita.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GradingReport {
    private final String verdict, mainClass, message;
    private final int configVersion;
    private final BigDecimal score;
    private final List<GradingCaseResult> cases;
    public GradingReport(String verdict, String mainClass, int configVersion, BigDecimal score,
            String message, List<GradingCaseResult> cases) {
        this.verdict = verdict; this.mainClass = mainClass; this.configVersion = configVersion;
        this.score = score; this.message = message;
        this.cases = Collections.unmodifiableList(new ArrayList<>(cases));
    }
    public static GradingReport unavailable(String verdict, String message) {
        return new GradingReport(verdict, "Main", 0, null, message, Collections.<GradingCaseResult>emptyList());
    }
    public String getVerdict() { return verdict; }
    public String getMainClass() { return mainClass; }
    public int getConfigVersion() { return configVersion; }
    public BigDecimal getScore() { return score; }
    public boolean isComplete() { return score != null; }
    public String getMessage() { return message; }
    public List<GradingCaseResult> getCases() { return cases; }
    public int getTotal() { return cases.size(); }
    public int getPassed() {
        int n = 0;
        for (GradingCaseResult c : cases) if (c.getVerdict() == GradingCaseResult.Verdict.ACCEPTED) n++;
        return n;
    }
    public GradingReport forStudent() {
        List<GradingCaseResult> safe = new ArrayList<>();
        for (GradingCaseResult c : cases) safe.add(c.forStudent());
        return new GradingReport(verdict, mainClass, configVersion, score, message, safe);
    }
}
