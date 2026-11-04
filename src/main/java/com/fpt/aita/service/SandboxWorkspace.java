package com.fpt.aita.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Extracts bounded Java source files into a fresh private staging directory. */
final class SandboxWorkspace implements AutoCloseable {
    static final long MAX_ARCHIVE_BYTES = 50L * 1024 * 1024;
    static final long MAX_EXPANDED_BYTES = 16L * 1024 * 1024;
    static final int MAX_ENTRIES = 1000;
    static final int MAX_JAVA_FILES = 200;
    private final Path root;
    private final Path sources;

    private SandboxWorkspace(Path root) throws IOException {
        this.root = root;
        sources = Files.createDirectory(root.resolve("sources"));
    }

    static SandboxWorkspace extract(Path archive) throws IOException {
        if (!Files.isRegularFile(archive, LinkOption.NOFOLLOW_LINKS)
                || Files.size(archive) > MAX_ARCHIVE_BYTES) {
            throw new InvalidSubmissionException("ZIP không hợp lệ hoặc vượt 50 MiB.");
        }
        SandboxWorkspace workspace = new SandboxWorkspace(Files.createTempDirectory("aita-sandbox-"));
        try {
            workspace.unpack(archive);
            return workspace;
        } catch (IOException | RuntimeException ex) {
            try { workspace.close(); } catch (IOException cleanup) { ex.addSuppressed(cleanup); }
            throw ex;
        }
    }

    Path getSources() { return sources; }

    private void unpack(Path archive) throws IOException {
        List<String> manifest = new ArrayList<>();
        Set<Path> seen = new HashSet<>();
        int count = 0;
        long expanded = 0;
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        try (InputStream file = Files.newInputStream(archive);
             ZipInputStream zip = new ZipInputStream(file, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if (++count > MAX_ENTRIES) {
                    throw new InvalidSubmissionException("ZIP có quá nhiều entry (tối đa 1000).");
                }
                String name = entry.getName();
                // A narrow filename grammar also makes javac's argument-file syntax unambiguous.
                if (name.length() > 240 || !name.matches("[a-zA-Z0-9_.$ /-]+")
                        || name.startsWith("/") || name.contains("\\")
                        || name.matches("(^|.*/)\\.\\.(/.*|$)")) {
                    throw new InvalidSubmissionException("Đường dẫn không an toàn trong ZIP.");
                }
                Path target = sources.resolve(name).normalize();
                if (!target.startsWith(sources) || target.equals(sources) || !seen.add(target)) {
                    throw new InvalidSubmissionException("ZIP chứa đường dẫn trùng hoặc không hợp lệ.");
                }
                boolean java = !entry.isDirectory() && name.endsWith(".java");
                if (java) {
                    if (manifest.size() >= MAX_JAVA_FILES) {
                        throw new InvalidSubmissionException("ZIP vượt 200 file Java.");
                    }
                    Files.createDirectories(target.getParent());
                    // Symlink metadata is never materialized: every extracted item is a regular file.
                    try (OutputStream out = Files.newOutputStream(target)) {
                        int n;
                        while ((n = zip.read(buffer)) != -1) {
                            expanded = checkBudget(expanded, n, deadline);
                            out.write(buffer, 0, n);
                        }
                    }
                    manifest.add("\"/workspace/" + name + "\"");
                } else {
                    // Count discarded entries as well, so an ignored ZIP bomb is still rejected.
                    int n;
                    while ((n = zip.read(buffer)) != -1) expanded = checkBudget(expanded, n, deadline);
                }
                zip.closeEntry();
                if (System.nanoTime() > deadline) throw new InvalidSubmissionException("Giải nén ZIP quá thời gian.");
            }
        } catch (java.util.zip.ZipException | IllegalArgumentException ex) {
            throw new InvalidSubmissionException("Không đọc được ZIP hợp lệ.");
        }
        if (manifest.isEmpty()) throw new InvalidSubmissionException("ZIP không chứa mã nguồn .java.");
        Files.write(sources.resolve(".aita-sources.args"), manifest, StandardCharsets.UTF_8);
        Files.walkFileTree(sources, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                permissions(dir, "rwxr-xr-x");
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                permissions(file, "r--r--r--");
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private long checkBudget(long expanded, int n, long deadline) throws IOException {
        if (expanded + n > MAX_EXPANDED_BYTES || System.nanoTime() > deadline) {
            throw new InvalidSubmissionException("ZIP vượt giới hạn giải nén (16 MiB / 5 giây).");
        }
        return expanded + n;
    }

    private static void permissions(Path file, String mode) throws IOException {
        if (Files.getFileStore(file).supportsFileAttributeView("posix")) {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString(mode));
        }
    }

    @Override public void close() throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException ex) throws IOException {
                if (ex != null) throw ex;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    static final class InvalidSubmissionException extends IOException {
        InvalidSubmissionException(String message) { super(message); }
    }
}
