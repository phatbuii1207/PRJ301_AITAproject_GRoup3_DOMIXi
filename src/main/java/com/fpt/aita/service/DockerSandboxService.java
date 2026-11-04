package com.fpt.aita.service;

import com.fpt.aita.model.SandboxResult;
import com.fpt.aita.model.SandboxResult.Status;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs untrusted Java ONLY inside Docker. Never falls back to a host JVM.
 * Requires a local Linux Docker daemon and a prebuilt aita-sandbox:java8 image.
 */
public final class DockerSandboxService {
    public static final long TIMEOUT_MS = 5000;
    public static final int OUTPUT_LIMIT_BYTES = 64 * 1024;
    private static final Semaphore SLOTS = new Semaphore(2);
    private static final AtomicBoolean CLEANUP_FAILED = new AtomicBoolean();
    private static final Logger LOG = Logger.getLogger(DockerSandboxService.class.getName());
    private final String image;

    public DockerSandboxService() {
        String configured = System.getenv("AITA_SANDBOX_IMAGE");
        image = System.getProperty("aita.sandbox.image",
                configured == null || configured.isEmpty() ? "aita-sandbox:java8" : configured);
    }

    public SandboxResult execute(Path archive) {
        String main = System.getenv("AITA_SANDBOX_MAIN_CLASS");
        return execute(archive, main == null || main.isEmpty() ? "Main" : main, new byte[0]);
    }

    /** mainClass and input must come from the assignment's trusted configuration. */
    public SandboxResult execute(Path archive, String mainClass, byte[] input) {
        if (CLEANUP_FAILED.get()) return result(Status.SANDBOX_UNAVAILABLE,
                "Sandbox tạm khóa do lỗi dọn container. Quản trị viên cần kiểm tra Docker và khởi động lại ứng dụng.", 0);
        if (archive == null || mainClass == null || mainClass.length() > 200
                || !mainClass.matches("[a-zA-Z_$][a-zA-Z0-9_$]*(\\.[a-zA-Z_$][a-zA-Z0-9_$]*)*")
                || input == null || input.length > OUTPUT_LIMIT_BYTES) {
            return result(Status.INVALID_SUBMISSION, "Main class hoặc stdin không hợp lệ.", 0);
        }
        if (!SLOTS.tryAcquire()) return result(Status.SANDBOX_BUSY, "Sandbox đang bận. Vui lòng thử lại.", 0);
        String name = "aita-sandbox-" + UUID.randomUUID();
        SandboxWorkspace workspace = null;
        SandboxResult outcome = result(Status.INTERNAL_ERROR, "Sandbox chưa hoàn tất.", 0);
        boolean interrupted = false;
        boolean createAttempted = false;
        long executionStarted = 0;
        try {
            workspace = SandboxWorkspace.extract(archive);
            // Create first: the container identity is known before the attached CLI starts.
            // This avoids mistaking destruction of the CLI for destruction of the container.
            createAttempted = true;
            Capture created = run(createCommand(name, workspace.getSources(), mainClass),
                    new byte[0], TIMEOUT_MS, 8192);
            if (created.exitCode != 0 || created.timedOut || created.limitExceeded
                    || !created.errorOutput.isEmpty() || !created.output.trim().matches("[a-f0-9]{64}")) {
                outcome = result(Status.SANDBOX_UNAVAILABLE,
                        "Không tạo được sandbox. Kiểm tra Docker và image aita-sandbox:java8 đã build.", 0);
            } else {
                executionStarted = System.nanoTime();
                Capture execution = run(Arrays.asList("docker", "start", "--attach", "--interactive", name),
                        input, TIMEOUT_MS, OUTPUT_LIMIT_BYTES);
                long elapsed = elapsedMs(executionStarted);
                Status status;
                if (execution.limitExceeded) status = Status.OUTPUT_LIMIT_EXCEEDED;
                else if (execution.timedOut) status = Status.TIME_LIMIT_EXCEEDED;
                else if (execution.exitCode == 0) status = Status.SUCCESS;
                else if (execution.exitCode == 65) status = Status.COMPILE_ERROR;
                else if (execution.exitCode == 70 || execution.exitCode >= 128) status = Status.RUNTIME_ERROR;
                else status = Status.SANDBOX_UNAVAILABLE;
                outcome = new SandboxResult(status, status == Status.SUCCESS ? execution.output : execution.combined(),
                        execution.errorOutput, elapsed);
            }
        } catch (SandboxWorkspace.InvalidSubmissionException ex) {
            outcome = result(Status.INVALID_SUBMISSION, ex.getMessage(), 0);
        } catch (InterruptedException ex) {
            interrupted = true;
            outcome = result(Status.INTERNAL_ERROR, "Tác vụ sandbox đã bị hủy.", elapsedMs(executionStarted));
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Sandbox infrastructure error", ex);
            outcome = result(Status.SANDBOX_UNAVAILABLE,
                    "Không truy cập được Docker hoặc thư mục tạm của sandbox.", elapsedMs(executionStarted));
        } finally {
            // Interrupts must not skip container removal or leave a semaphore slot occupied.
            interrupted |= Thread.interrupted();
            boolean cleaned = !createAttempted || removeContainer(name);
            if (workspace != null && cleaned) {
                try { workspace.close(); }
                catch (IOException ex) { LOG.log(Level.WARNING, "Cannot clean sandbox staging directory", ex); }
            }
            // If Docker is unreachable, retain staging until the operator removes the container.
            if (!cleaned) {
                CLEANUP_FAILED.set(true);
                LOG.severe("Sandbox cleanup failed; operator must remove container " + name);
                outcome = result(Status.INTERNAL_ERROR,
                        "Không xác nhận được việc hủy container. Sandbox đã tạm khóa để quản trị viên kiểm tra.",
                        elapsedMs(executionStarted));
            }
            SLOTS.release();
            if (interrupted) Thread.currentThread().interrupt();
        }
        return outcome;
    }

    List<String> createCommand(String name, Path sources, String mainClass) {
        return new ArrayList<>(Arrays.asList("docker", "create", "--rm", "--pull", "never",
                "--name", name, "--label", "aita.sandbox=true",
                "--network", "none", "--memory", "128m", "--memory-swap", "128m",
                "--cpus", "0.5", "--pids-limit", "64", "--user", "sandboxuser",
                "--read-only", "--cap-drop", "ALL", "--security-opt", "no-new-privileges:true",
                "--ulimit", "nofile=128:128", "--log-driver", "none",
                "--tmpfs", "/tmp:rw,noexec,nosuid,nodev,size=32m,mode=1777",
                "--volume", sources.toAbsolutePath() + ":/workspace:ro",
                "--interactive", image, mainClass));
    }

    private boolean removeContainer(String name) {
        try {
            Capture removed = run(Arrays.asList("docker", "rm", "--force", name), new byte[0], 2000, 8192);
            return !removed.timedOut && (removed.exitCode == 0 || removed.combined().contains("No such container"));
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Cannot contact Docker to remove sandbox", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    private Capture run(List<String> command, final byte[] input, long timeout, int limit)
            throws IOException, InterruptedException {
        final Process process = new ProcessBuilder(command).start();
        final BoundedOutput output = new BoundedOutput(limit);
        Thread reader = new Thread(new Runnable() {
            @Override public void run() {
                try (InputStream stream = process.getInputStream()) {
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = stream.read(buffer)) != -1) output.append(buffer, n, false);
                } catch (IOException ex) { output.readFailed.set(true); }
            }
        }, "aita-sandbox-output");
        Thread errorReader = new Thread(new Runnable() {
            @Override public void run() {
                try (InputStream stream = process.getErrorStream()) {
                    byte[] buffer = new byte[4096]; int n;
                    while ((n = stream.read(buffer)) != -1) output.append(buffer, n, true);
                } catch (IOException ex) { output.readFailed.set(true); }
            }
        }, "aita-sandbox-stderr");
        Thread writer = new Thread(new Runnable() {
            @Override public void run() {
                try (java.io.OutputStream stream = process.getOutputStream()) { stream.write(input); }
                catch (IOException ignored) { /* Program may exit without reading its input. */ }
            }
        }, "aita-sandbox-input");
        reader.setDaemon(true);
        errorReader.setDaemon(true);
        writer.setDaemon(true);
        reader.start();
        errorReader.start();
        writer.start();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeout);
        boolean timedOut = false;
        try {
            while (!process.waitFor(25, TimeUnit.MILLISECONDS)) {
                if (output.exceeded.get()) break;
                if (System.nanoTime() >= deadline) { timedOut = true; break; }
            }
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(500, TimeUnit.MILLISECONDS);
            }
            reader.join(500);
            errorReader.join(500);
            writer.join(500);
            if (reader.isAlive() || errorReader.isAlive()) throw new IOException("Sandbox output stream did not close");
            if (output.readFailed.get() && !timedOut && !output.exceeded.get()) {
                throw new IOException("Cannot read sandbox output");
            }
            int exit = process.isAlive() ? -1 : process.exitValue();
            return new Capture(exit, timedOut, output.exceeded.get(), output.text(false), output.text(true));
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            try { process.getInputStream().close(); } catch (IOException ignored) { }
            try { process.getErrorStream().close(); } catch (IOException ignored) { }
            try { process.getOutputStream().close(); } catch (IOException ignored) { }
        }
    }

    private long elapsedMs(long start) {
        return start == 0 ? 0 : TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    private SandboxResult result(Status status, String output, long elapsed) {
        return new SandboxResult(status, output, elapsed);
    }

    private static final class Capture {
        final int exitCode;
        final boolean timedOut;
        final boolean limitExceeded;
        final String output;
        final String errorOutput;
        Capture(int exit, boolean timedOut, boolean exceeded, String output, String errorOutput) {
            this.exitCode = exit;
            this.timedOut = timedOut;
            this.limitExceeded = exceeded;
            this.output = output;
            this.errorOutput = errorOutput;
        }
        String combined() { return output + errorOutput; }
    }

    private static final class BoundedOutput {
        final AtomicBoolean exceeded = new AtomicBoolean();
        final AtomicBoolean readFailed = new AtomicBoolean();
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final ByteArrayOutputStream errors = new ByteArrayOutputStream();
        private final int limit;
        BoundedOutput(int limit) { this.limit = limit; }
        synchronized void append(byte[] buffer, int length, boolean stderr) {
            int remaining = limit - bytes.size() - errors.size();
            (stderr ? errors : bytes).write(buffer, 0, Math.min(remaining, length));
            if (length > remaining) exceeded.set(true);
        }
        synchronized String text(boolean stderr) {
            try {
                // Discard invalid/truncated UTF-8 rather than expanding it into replacement bytes.
                return StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(java.nio.charset.CodingErrorAction.IGNORE)
                        .onUnmappableCharacter(java.nio.charset.CodingErrorAction.IGNORE)
                        .decode(java.nio.ByteBuffer.wrap((stderr ? errors : bytes).toByteArray())).toString();
            } catch (java.nio.charset.CharacterCodingException impossibleWithIgnore) {
                throw new IllegalStateException(impossibleWithIgnore);
            }
        }
    }
}
