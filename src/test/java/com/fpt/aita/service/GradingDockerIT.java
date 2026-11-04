package com.fpt.aita.service;

import com.fpt.aita.model.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class GradingDockerIT {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private GradingTestCase test(String input, String expected, boolean hidden) {
        return new GradingTestCase("Sum", input, expected, new BigDecimal("2"), hidden);
    }
    private Path archive(String source) throws Exception {
        Path path = temporary.newFile("sum.zip").toPath();
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(path))) {
            out.putNextEntry(new ZipEntry("Main.java")); out.write(source.getBytes(StandardCharsets.UTF_8)); out.closeEntry();
        }
        return path;
    }
    private GradingConfig config() {
        return new GradingConfig("Main", 1, Arrays.asList(test("2 3\n", "5", false), test("-2 5\n", "3", true),
                test("0 0\n", "0", true), test("-4 -6\n", "-10", true), test("1000000 2000000\n", "3000000", true)));
    }

    @Test public void realSumPassesFiveTestsAndStderrDoesNotPolluteComparison() throws Exception {
        Path zip = archive("public class Main { public static void main(String[] a) { java.util.Scanner s=new java.util.Scanner(System.in);"
                + "System.err.println(\"debug-only\"); System.out.println(s.nextLong()+s.nextLong()); }}");
        GradingReport r = new GradingService().grade(zip, config());
        assertEquals("ACCEPTED", r.getVerdict()); assertEquals(new BigDecimal("10.00"), r.getScore());
        assertEquals(5, r.getPassed()); assertEquals("debug-only\n", r.getCases().get(0).getDiagnostics());
    }

    @Test public void hardcodedAnswerDoesNotGetFullScore() throws Exception {
        Path zip = archive("public class Main { public static void main(String[] a) { System.out.println(5); }}");
        GradingReport r = new GradingService().grade(zip, config());
        assertEquals("PARTIAL", r.getVerdict()); assertEquals(new BigDecimal("2.00"), r.getScore()); assertEquals(1, r.getPassed());
    }

    @Test public void compilerErrorSkipsOtherTests() throws Exception {
        GradingReport r = new GradingService().grade(archive("public class Main { syntax error }"), config());
        assertEquals("COMPILE_ERROR", r.getVerdict()); assertEquals(new BigDecimal("0.00"), r.getScore());
        assertEquals(GradingCaseResult.Verdict.NOT_RUN, r.getCases().get(4).getVerdict());
    }

    @Test public void timeoutEarnsNoPointsAndIsExplicit() throws Exception {
        GradingConfig c = new GradingConfig("Main", 1, Arrays.asList(test("", "", false)));
        GradingReport r = new GradingService().grade(archive("public class Main { public static void main(String[] a) { while(true){} }}"), c);
        assertEquals("TIME_LIMIT_EXCEEDED", r.getVerdict()); assertEquals(new BigDecimal("0.00"), r.getScore());
    }
}
