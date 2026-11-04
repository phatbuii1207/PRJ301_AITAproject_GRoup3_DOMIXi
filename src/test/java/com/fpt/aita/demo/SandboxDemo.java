package com.fpt.aita.demo;

import com.fpt.aita.model.SandboxResult;
import com.fpt.aita.service.DockerSandboxService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/** Trusted presentation CLI. Student Main runs through DockerSandboxService only. */
public final class SandboxDemo {
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.err.println("Usage: SandboxDemo <submission.zip> <EXPECTED_STATUS> [required-output]");
            System.exit(2);
        }
        SandboxResult.Status expected = SandboxResult.Status.valueOf(args[1]);
        System.out.println("Submission: " + Paths.get(args[0]).getFileName());
        System.out.println("Execution: DockerSandboxService -> Java 8 container (not host Main)");
        SandboxResult result = new DockerSandboxService().execute(Paths.get(args[0]), "Main", new byte[0]);
        String output = result.getOutput();
        System.out.println("Status: " + result.getStatus());
        System.out.println("isSuccess: " + result.isSuccess());
        System.out.println("Execution time: " + result.getExecutionTimeMs() + " ms (start + compile + run)");
        System.out.println("Captured output: " + output.getBytes(StandardCharsets.UTF_8).length + " bytes");
        System.out.println("Output preview:");
        System.out.print(output.substring(0, Math.min(output.length(), 1600)));
        if (!output.endsWith("\n")) System.out.println();
        if (output.length() > 1600) System.out.println("[Preview truncated; service capture limit is 64 KiB]");
        boolean passed = result.getStatus() == expected
                && (args.length < 3 || output.contains(args[2]));
        System.out.println("Demo check: " + (passed ? "PASS" : "FAIL") + " (expected " + expected + ")");
        if (!passed) System.exit(1);
    }
}
