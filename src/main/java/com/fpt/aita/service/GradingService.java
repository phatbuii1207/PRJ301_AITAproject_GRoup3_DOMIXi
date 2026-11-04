package com.fpt.aita.service;

import com.fpt.aita.model.*;
import com.fpt.aita.model.GradingCaseResult.Verdict;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Answer keys and scoring stay in this trusted JVM; only source/stdin go to Docker. */
public final class GradingService {
    @FunctionalInterface public interface Executor {
        SandboxResult execute(Path archive, String mainClass, byte[] input);
    }
    private final Executor sandbox;
    public GradingService() { this(new DockerSandboxService()::execute); }
    public GradingService(Executor sandbox) { this.sandbox = sandbox; }

    public GradingReport grade(Path archive, GradingConfig config) {
        if (config == null) return GradingReport.unavailable("NOT_CONFIGURED", "Giảng viên chưa cấu hình test case cho bài tập.");
        List<GradingCaseResult> results = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO, earned = BigDecimal.ZERO;
        boolean stop = false, infrastructureFailure = false;
        int index = 0;
        for (GradingTestCase test : config.getTests()) {
            total = total.add(test.getWeight());
            Verdict verdict = Verdict.NOT_RUN;
            String actual = "", diagnostics = "";
            long time = 0;
            if (!stop) {
                SandboxResult run;
                try { run = sandbox.execute(archive, config.getMainClass(), test.getInput().getBytes(StandardCharsets.UTF_8)); }
                catch (RuntimeException ex) { run = null; }
                if (run == null) run = new SandboxResult(SandboxResult.Status.INTERNAL_ERROR, "Không hoàn tất thực thi sandbox.", 0);
                actual = run.getOutput(); diagnostics = run.getDiagnostics(); time = run.getExecutionTimeMs();
                switch (run.getStatus()) {
                    case SUCCESS: verdict = matches(actual, test.getExpectedOutput()) ? Verdict.ACCEPTED : Verdict.WRONG_ANSWER; break;
                    case COMPILE_ERROR: verdict = Verdict.COMPILE_ERROR; stop = true; break;
                    case INVALID_SUBMISSION: verdict = Verdict.INVALID_SUBMISSION; stop = true; break;
                    case RUNTIME_ERROR: verdict = Verdict.RUNTIME_ERROR; break;
                    case TIME_LIMIT_EXCEEDED: verdict = Verdict.TIME_LIMIT_EXCEEDED; break;
                    case OUTPUT_LIMIT_EXCEEDED: verdict = Verdict.OUTPUT_LIMIT_EXCEEDED; break;
                    default: verdict = Verdict.INFRASTRUCTURE_ERROR; infrastructureFailure = true; stop = true;
                }
                if (Thread.currentThread().isInterrupted()) { infrastructureFailure = true; stop = true; }
            }
            if (verdict == Verdict.ACCEPTED) earned = earned.add(test.getWeight());
            results.add(new GradingCaseResult(++index, test, verdict, actual, diagnostics, time));
        }
        if (infrastructureFailure) return new GradingReport("GRADING_ERROR", config.getMainClass(), config.getVersion(), null,
                "Chưa có điểm hợp lệ do lỗi hạ tầng/hủy tác vụ. Giảng viên có thể chấm lại.", results);
        BigDecimal score = earned.multiply(BigDecimal.TEN).divide(total, 2, RoundingMode.HALF_UP);
        String overall = "ACCEPTED";
        if (earned.compareTo(total) != 0) {
            overall = earned.signum() > 0 ? "PARTIAL" : results.get(0).getVerdict().name();
        }
        return new GradingReport(overall, config.getMainClass(), config.getVersion(), score,
                "Điểm = 10 × tổng trọng số test đạt / tổng trọng số; không dùng AI để tính điểm.", results);
    }

    public static boolean matches(String actual, String expected) { return normalize(actual).equals(normalize(expected)); }
    static String normalize(String text) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        int end = normalized.length();
        while (end > 0 && " \t\n".indexOf(normalized.charAt(end - 1)) >= 0) end--;
        return normalized.substring(0, end);
    }
}
