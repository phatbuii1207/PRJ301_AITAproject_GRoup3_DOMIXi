package com.fpt.aita.service;

import com.fpt.aita.model.SandboxResult;
import com.fpt.aita.model.SandboxResult.Status;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

/** Real Docker tests; explicitly enabled with mvn verify -Psandbox-it. */
public class DockerSandboxIT {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final DockerSandboxService service = new DockerSandboxService();

    @Test public void runsJava8PackagedMainAndStandardInput() throws Exception {
        String source = "package demo; public class Main { public static void main(String[] a) throws Exception { "
                + "System.out.println(System.getProperty(\"java.version\")); "
                + "System.out.println(new java.util.Scanner(System.in).nextLine()); }}";
        SandboxResult result = service.execute(zip("My Project/demo/Main.java", source), "demo.Main",
                "hello sandbox\n".getBytes(StandardCharsets.UTF_8));
        assertStatus(Status.SUCCESS, result);
        assertTrue(result.getOutput(), result.getOutput().contains("1.8.0"));
        assertTrue(result.getOutput(), result.getOutput().contains("hello sandbox"));
    }

    @Test public void infiniteLoopTimesOutAndLiveDockerSettingsAreEnforced() throws Exception {
        Set<String> before = containers();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Path zip = fixture("tle");
        long started = System.nanoTime();
        Future<SandboxResult> future = executor.submit(() -> service.execute(zip));
        String ownContainer = null;
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
            while (System.nanoTime() < deadline && ownContainer == null) {
                for (String name : containers()) if (!before.contains(name)) ownContainer = name;
                if (ownContainer == null) Thread.sleep(50);
            }
            assertNotNull("Sandbox container never appeared", ownContainer);
            String settings = docker("inspect", "--format",
                    "{{.HostConfig.NetworkMode}}|{{.HostConfig.Memory}}|{{.HostConfig.MemorySwap}}|"
                    + "{{.HostConfig.NanoCpus}}|{{.Config.User}}|{{.HostConfig.ReadonlyRootfs}}|"
                    + "{{.HostConfig.AutoRemove}}|{{.HostConfig.PidsLimit}}|{{.HostConfig.CapDrop}}|"
                    + "{{.HostConfig.SecurityOpt}}|{{range .Mounts}}{{.Destination}}={{.RW}}{{end}}", ownContainer);
            assertTrue(settings, settings.contains("none|134217728|134217728|500000000|sandboxuser|true|true|64"));
            assertTrue(settings, settings.contains("ALL"));
            assertTrue(settings, settings.contains("no-new-privileges"));
            assertTrue(settings, settings.contains("/workspace=false"));
            SandboxResult result = future.get(12, TimeUnit.SECONDS);
            assertStatus(Status.TIME_LIMIT_EXCEEDED, result);
            assertTrue("Deadline not enforced", result.getExecutionTimeMs() >= 4900 && result.getExecutionTimeMs() < 7000);
            assertTrue("Cleanup took too long", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 10000);
            assertFalse("Timed-out container leaked", containers().contains(ownContainer));
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    @Test public void blocksFileDeletionAndPreservesOriginalSubmission() throws Exception {
        Path zip = fixture("file-deletion");
        byte[] original = Files.readAllBytes(zip);
        SandboxResult result = service.execute(zip);
        assertStatus(Status.SUCCESS, result);
        assertTrue(result.getOutput(), result.getOutput().contains("FILE_DELETION_BLOCKED"));
        assertTrue(result.getOutput(), result.getOutput().contains("ROOTFS_WRITE_BLOCKED"));
        assertTrue(result.getOutput(), result.getOutput().contains("DOCKER_SOCKET_ABSENT"));
        assertArrayEquals(original, Files.readAllBytes(zip));
    }

    @Test public void blocksOutboundNetwork() throws Exception {
        SandboxResult result = service.execute(fixture("network"));
        assertStatus(Status.SUCCESS, result);
        assertTrue(result.getOutput(), result.getOutput().contains("NETWORK_BLOCKED"));
    }

    @Test public void reportsCompilerFailureAndDoesNotRunMain() throws Exception {
        SandboxResult result = service.execute(zip("Main.java", "public class Main { broken syntax }"));
        assertStatus(Status.COMPILE_ERROR, result);
        assertTrue(result.getOutput(), result.getOutput().contains("error:"));
    }

    @Test public void runtimeExitCannotSpoofTimeout() throws Exception {
        SandboxResult result = service.execute(zip("Main.java",
                "public class Main { public static void main(String[] a) { System.exit(124); }}"));
        assertStatus(Status.RUNTIME_ERROR, result);
    }

    @Test public void runtimeExitCannotSpoofCompileError() throws Exception {
        SandboxResult result = service.execute(zip("Main.java",
                "public class Main { public static void main(String[] a) { System.exit(65); }}"));
        assertStatus(Status.RUNTIME_ERROR, result);
    }

    @Test public void outputFloodIsBoundedAndContainerRemoved() throws Exception {
        Set<String> before = containers();
        SandboxResult result = service.execute(zip("Main.java", "public class Main { public static void main(String[] a) { "
                + "while(true) System.out.println(\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"); }}"));
        assertStatus(Status.OUTPUT_LIMIT_EXCEEDED, result);
        assertTrue(result.getOutput().getBytes(StandardCharsets.UTF_8).length <= DockerSandboxService.OUTPUT_LIMIT_BYTES);
        assertEquals(before, containers());
    }

    @Test public void heapExhaustionDoesNotSucceedOrEscape() throws Exception {
        SandboxResult result = service.execute(zip("Main.java", "public class Main { public static void main(String[] a) { "
                + "java.util.List<byte[]> data=new java.util.ArrayList<byte[]>(); while(true) data.add(new byte[1024*1024]); }}"));
        assertStatus(Status.RUNTIME_ERROR, result);
        assertTrue(result.getOutput(), result.getOutput().contains("OutOfMemoryError"));
    }

    @Test public void missingImageFailsClosed() throws Exception {
        System.setProperty("aita.sandbox.image", "aita-sandbox-missing:" + java.util.UUID.randomUUID());
        DockerSandboxService missing;
        try { missing = new DockerSandboxService(); }
        finally { System.clearProperty("aita.sandbox.image"); }
        Set<String> before = containers();
        SandboxResult result = missing.execute(zip("Main.java", "public class Main { public static void main(String[] a) {} }"));
        assertStatus(Status.SANDBOX_UNAVAILABLE, result);
        assertEquals(before, containers());
    }

    @Test public void cancellationRemovesContainerAndReleasesSlot() throws Exception {
        Set<String> before = containers();
        Path zip = fixture("tle");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<SandboxResult> future = executor.submit(() -> service.execute(zip));
        try {
            awaitContainerCount(before.size() + 1);
            future.cancel(true);
        } finally {
            executor.shutdownNow();
            assertTrue("Cancelled worker did not stop", executor.awaitTermination(10, TimeUnit.SECONDS));
        }
        assertEquals("Cancelled container leaked", before, containers());
        assertStatus(Status.COMPILE_ERROR, service.execute(zip("Main.java", "invalid syntax")));
    }

    @Test public void onlyTwoSubmissionsCanRunConcurrently() throws Exception {
        Set<String> before = containers();
        Path zip = fixture("tle");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<SandboxResult> first = executor.submit(() -> service.execute(zip));
        Future<SandboxResult> second = executor.submit(() -> service.execute(zip));
        try {
            awaitContainerCount(before.size() + 2);
            assertStatus(Status.SANDBOX_BUSY, service.execute(zip));
            assertStatus(Status.TIME_LIMIT_EXCEEDED, first.get(12, TimeUnit.SECONDS));
            assertStatus(Status.TIME_LIMIT_EXCEEDED, second.get(12, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
        assertEquals("Concurrent containers leaked", before, containers());
    }

    private void awaitContainerCount(int expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
        while (System.nanoTime() < deadline) {
            if (containers().size() == expected) return;
            Thread.sleep(50);
        }
        fail("Expected " + expected + " sandbox containers");
    }

    private Path fixture(String name) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/sandbox/" + name + "/Main.java")) {
            assertNotNull(in);
            return zip("Main.java", new String(read(in), StandardCharsets.UTF_8));
        }
    }

    private Path zip(String name, String code) throws Exception {
        Path zip = temporary.newFile("submission " + java.util.UUID.randomUUID() + ".zip").toPath();
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            out.putNextEntry(new ZipEntry(name));
            out.write(code.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return zip;
    }

    private Set<String> containers() throws Exception {
        String listing = docker("ps", "-a", "--filter", "label=aita.sandbox=true", "--format", "{{.Names}}").trim();
        return listing.isEmpty() ? new HashSet<String>() : new HashSet<>(Arrays.asList(listing.split("\\R")));
    }

    private String docker(String... args) throws Exception {
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add("docker"); command.addAll(Arrays.asList(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        assertTrue("Docker command timed out", process.waitFor(10, TimeUnit.SECONDS));
        String output = new String(read(process.getInputStream()), StandardCharsets.UTF_8);
        assertEquals(output, 0, process.exitValue());
        return output;
    }

    private static byte[] read(InputStream in) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096]; int n;
        while ((n = in.read(buffer)) != -1) bytes.write(buffer, 0, n);
        return bytes.toByteArray();
    }

    private void assertStatus(Status expected, SandboxResult result) {
        assertEquals(result.getStatus() + ": " + result.getOutput(), expected, result.getStatus());
        assertEquals(expected == Status.SUCCESS, result.isSuccess());
    }
}
