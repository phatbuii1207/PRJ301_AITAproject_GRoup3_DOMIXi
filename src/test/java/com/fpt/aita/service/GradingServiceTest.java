package com.fpt.aita.service;

import com.fpt.aita.model.*;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class GradingServiceTest {
    private GradingTestCase test(String input, String expected, String weight, boolean hidden) {
        return new GradingTestCase("case-" + input, input, expected, new BigDecimal(weight), hidden);
    }
    private GradingReport grade(GradingService.Executor run, GradingTestCase... cases) {
        return new GradingService(run).grade(Paths.get("submission.zip"), new GradingConfig("Main", 3, Arrays.asList(cases)));
    }

    @Test public void weightsDetermineScoreRatherThanJustTestCount() {
        GradingReport r = grade((p,m,i) -> new SandboxResult(SandboxResult.Status.SUCCESS, "5\n", 1),
                test("public", "5", "2", false), test("hidden", "6", "1", true));
        assertEquals(new BigDecimal("6.67"), r.getScore());
        assertEquals("PARTIAL", r.getVerdict()); assertEquals(1, r.getPassed()); assertEquals(2, r.getTotal());
    }

    @Test public void normalizationOnlyChangesLineEndingsAndFinalWhitespace() {
        assertTrue(GradingService.matches("a\r\nb\r\n \t", "a\nb"));
        assertFalse(GradingService.matches(" a", "a"));
        assertFalse(GradingService.matches("a  b", "a b"));
        assertFalse(GradingService.matches("a \nb", "a\nb"));
        assertFalse(GradingService.matches("5.0001", "5"));
        assertTrue(GradingService.matches("\n\t ", ""));
    }

    @Test public void stderrDoesNotChangeCorrectStdout() {
        GradingReport r = grade((p,m,i) -> new SandboxResult(SandboxResult.Status.SUCCESS, "5\n", "debug\n", 1),
                test("2 3", "5", "1", false));
        assertEquals("ACCEPTED", r.getVerdict()); assertEquals(new BigDecimal("10.00"), r.getScore());
        assertEquals("debug\n", r.getCases().get(0).getDiagnostics());
    }

    @Test public void compilerFailureStopsRemainingTests() {
        AtomicInteger called = new AtomicInteger();
        GradingReport r = grade((p,m,i) -> { called.incrementAndGet(); return new SandboxResult(SandboxResult.Status.COMPILE_ERROR, "error", 1); },
                test("1", "1", "1", false), test("2", "2", "1", false));
        assertEquals(1, called.get()); assertEquals("COMPILE_ERROR", r.getVerdict());
        assertEquals(new BigDecimal("0.00"), r.getScore());
        assertEquals(GradingCaseResult.Verdict.NOT_RUN, r.getCases().get(1).getVerdict());
    }

    @Test public void runtimeAndTimeoutAreNotComparedAsCorrectOutput() {
        AtomicInteger called = new AtomicInteger();
        GradingReport r = grade((p,m,i) -> new SandboxResult(called.getAndIncrement() == 0
                ? SandboxResult.Status.TIME_LIMIT_EXCEEDED : SandboxResult.Status.RUNTIME_ERROR, "", 5000),
                test("1", "", "1", false), test("2", "", "1", false));
        assertEquals(0, r.getPassed()); assertEquals(2, called.get()); assertEquals(new BigDecimal("0.00"), r.getScore());
    }

    @Test public void infrastructureFailureInvalidatesWholeScoreEvenAfterAPass() {
        AtomicInteger called = new AtomicInteger();
        GradingReport r = grade((p,m,i) -> new SandboxResult(called.getAndIncrement() == 0
                ? SandboxResult.Status.SUCCESS : SandboxResult.Status.SANDBOX_UNAVAILABLE, "ok", 1),
                test("1", "ok", "1", false), test("2", "ok", "1", false), test("3", "ok", "1", false));
        assertFalse(r.isComplete()); assertNull(r.getScore()); assertEquals("GRADING_ERROR", r.getVerdict());
        assertEquals(2, called.get()); assertEquals(GradingCaseResult.Verdict.NOT_RUN, r.getCases().get(2).getVerdict());
    }

    @Test public void hiddenInputsAnswersActualOutputAndDiagnosticsAreRedacted() {
        GradingReport original = grade((p,m,i) -> new SandboxResult(SandboxResult.Status.SUCCESS, "private-answer", "private-debug", 1),
                test("private-input", "private-answer", "1", true));
        GradingCaseResult safe = original.forStudent().getCases().get(0);
        assertEquals("", safe.getInput()); assertEquals("", safe.getExpectedOutput());
        assertEquals("", safe.getActualOutput()); assertEquals("", safe.getDiagnostics());
        assertFalse(safe.getName().contains("private"));
        assertEquals("private-input", original.getCases().get(0).getInput());
    }

    @Test public void missingConfigDoesNotRunAnythingOrAwardZero() {
        GradingReport r = new GradingService((p,m,i) -> { throw new AssertionError("Must not run"); })
                .grade(Paths.get("x.zip"), null);
        assertEquals("NOT_CONFIGURED", r.getVerdict()); assertNull(r.getScore());
    }

    @Test public void unexpectedExecutorFailureCannotBecomeAStudentZero() {
        GradingReport r = grade((p,m,i) -> { throw new IllegalStateException("Executor unavailable"); }, test("", "", "1", false));
        assertEquals("GRADING_ERROR", r.getVerdict()); assertNull(r.getScore());
    }

    @Test public void acceptedUsesAllWeightsAndCanHaveEmptyOutput() {
        GradingReport r = grade((p,m,i) -> new SandboxResult(SandboxResult.Status.SUCCESS, "", 1), test("", "", "0.01", false));
        assertEquals(new BigDecimal("10.00"), r.getScore());
    }

    @Test public void normalExitWithIncorrectTextIsWrongAnswer() {
        GradingReport r = grade((p,m,i) -> new SandboxResult(SandboxResult.Status.SUCCESS, "Ket qua: 5", 1), test("", "5", "1", false));
        assertEquals("WRONG_ANSWER", r.getVerdict()); assertEquals(new BigDecimal("0.00"), r.getScore());
    }

    @Test(expected=IllegalArgumentException.class) public void rejectsZeroWeight() { test("", "", "0", false); }
    @Test(expected=IllegalArgumentException.class) public void rejectsOverPreciseWeight() { test("", "", "0.001", false); }
    @Test(expected=IllegalArgumentException.class) public void rejectsEmptyTestBank() { new GradingConfig("Main", 0, Collections.emptyList()); }
    @Test(expected=IllegalArgumentException.class) public void rejectsMainClassInjection() { new GradingConfig("Main; touch /tmp/a", 0, Arrays.asList(test("", "", "1", false))); }
}
