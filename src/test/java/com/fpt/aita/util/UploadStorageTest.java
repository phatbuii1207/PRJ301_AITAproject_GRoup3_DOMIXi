package com.fpt.aita.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class UploadStorageTest {
    private Path tempPrimaryDir;
    private Path tempFallbackDir;
    private UploadStorage storage;

    @Before
    public void setUp() throws Exception {
        tempPrimaryDir = Files.createTempDirectory("aita-upload-test-primary-");
        tempFallbackDir = Files.createTempDirectory("aita-upload-test-fallback-");
        storage = new UploadStorage(tempPrimaryDir, tempFallbackDir);
    }

    @After
    public void tearDown() throws Exception {
        deleteRecursively(tempPrimaryDir);
        deleteRecursively(tempFallbackDir);
    }

    private void deleteRecursively(Path root) throws IOException {
        if (root != null && Files.exists(root)) {
            Files.walk(root)
                 .sorted((a, b) -> b.compareTo(a))
                 .forEach(p -> {
                     try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                 });
        }
    }

    @Test
    public void testSaveAndResolveJavaFile() throws Exception {
        String content = "public class Solution { public static void main(String[] args) {} }";
        String fileName = "123_student1_Solution.java";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        Path saved = storage.save(new ByteArrayInputStream(bytes), fileName, bytes.length);
        assertNotNull(saved);
        assertTrue(Files.exists(saved));
        assertEquals(fileName, saved.getFileName().toString());

        Path resolved = storage.resolve("/uploads/submissions/" + fileName);
        assertNotNull(resolved);
        assertEquals(saved.toRealPath(), resolved.toRealPath());

        Path archive = storage.resolveArchive("/uploads/submissions/" + fileName);
        assertNotNull(archive);
        assertEquals(saved.toRealPath(), archive.toRealPath());
    }

    @Test
    public void testSaveAndResolveZipFile() throws Exception {
        byte[] dummyZip = new byte[]{0x50, 0x4b, 0x03, 0x04, 0x00, 0x00};
        String fileName = "456_student2_Project.zip";

        Path saved = storage.save(new ByteArrayInputStream(dummyZip), fileName, dummyZip.length);
        assertNotNull(saved);
        assertTrue(Files.exists(saved));

        Path resolved = storage.resolve("uploads/submissions/" + fileName);
        assertNotNull(resolved);
        assertEquals(saved.toRealPath(), resolved.toRealPath());
    }

    @Test
    public void testRejectPathTraversalInFileName() {
        String[] traversalNames = {
            "../evil.java",
            "../../etc/passwd.java",
            "subdir/evil.java",
            "subdir\\evil.java",
            "..\\evil.zip"
        };
        for (String name : traversalNames) {
            try {
                storage.save(new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)), name, 4);
                fail("Should reject path traversal name: " + name);
            } catch (Exception expected) {
                assertTrue(expected instanceof IllegalArgumentException || expected instanceof SecurityException);
            }
        }
    }

    @Test
    public void testRejectControlCharsCRLFAndHeaderInjection() {
        String[] dangerousNames = {
            "evil\r\nSet-Cookie:admin=1.java",
            "malware\n.java",
            "test\0null.zip",
            "hello world.java",
            "file;cat /etc/passwd.zip",
            "foo<script>.java"
        };
        for (String name : dangerousNames) {
            try {
                UploadStorage.validateFileName(name);
                fail("Should reject dangerous characters: " + name);
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("chỉ chấp nhận [A-Za-z0-9._-]")
                        || expected.getMessage().contains("không được hỗ trợ"));
            }
        }
    }

    @Test
    public void testRejectExtremeLengths() {
        // Quá ngắn (< 5 ký tự)
        try {
            UploadStorage.validateFileName(".zip");
            fail("Should reject too short");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Độ dài"));
        }

        // Quá dài (> 255 ký tự)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 260; i++) sb.append("a");
        sb.append(".java");
        try {
            UploadStorage.validateFileName(sb.toString());
            fail("Should reject too long");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Độ dài"));
        }
    }

    @Test
    public void testRejectUnsupportedExtension() {
        String[] badExtensions = {
            "hack.sh",
            "malware.exe",
            "script.jsp",
            "photo.png",
            "noextension"
        };
        for (String name : badExtensions) {
            try {
                storage.save(new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)), name, 4);
                fail("Should reject unsupported extension: " + name);
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("không được hỗ trợ"));
            } catch (Exception other) {
                fail("Expected IllegalArgumentException but got: " + other);
            }
        }
    }

    @Test
    public void testRejectFileSizeExceeding50MB() {
        // Tạo stream giả mạo > 50MB (50 * 1024 * 1024 + 1024 bytes)
        long excessiveSize = 50L * 1024 * 1024 + 1024;
        java.io.InputStream hugeStream = new java.io.InputStream() {
            private long count = 0;
            @Override
            public int read() {
                if (count >= excessiveSize) return -1;
                count++;
                return 'A';
            }
            @Override
            public int read(byte[] b, int off, int len) {
                if (count >= excessiveSize) return -1;
                int toRead = (int) Math.min(len, excessiveSize - count);
                for (int i = 0; i < toRead; i++) b[off + i] = 'A';
                count += toRead;
                return toRead;
            }
        };

        String name = "huge_file.java";
        try {
            storage.save(hugeStream, name, excessiveSize);
            fail("Should reject file exceeding 50MB limit");
        } catch (Exception expected) {
            assertTrue(expected instanceof IllegalArgumentException);
            assertTrue(expected.getMessage().contains("50MB"));
        }
        // Đảm bảo tệp bị xóa khi dung lượng vượt quá
        assertFalse(Files.exists(tempPrimaryDir.resolve(name)));
    }

    @Test
    public void testFallbackResolutionForOldSubmissions() throws Exception {
        String legacyName = "legacy_old_student_code.java";
        Path oldFile = tempFallbackDir.resolve(legacyName);
        Files.write(oldFile, "class Legacy {}".getBytes(StandardCharsets.UTF_8));

        // Chưa có ở primary
        assertFalse(Files.exists(tempPrimaryDir.resolve(legacyName)));

        // Resolve phải tìm thấy ở fallback
        Path resolved = storage.resolve("/uploads/submissions/" + legacyName);
        assertNotNull(resolved);
        assertEquals(oldFile.toRealPath(), resolved.toRealPath());

        Path archive = storage.resolveArchive("/uploads/submissions/" + legacyName);
        assertNotNull(archive);
        assertEquals(oldFile.toRealPath(), archive.toRealPath());
    }

    @Test
    public void testRejectSymbolicLink() throws Exception {
        Path targetFile = Files.createTempFile("target-real-", ".java");
        Files.write(targetFile, "class Real {}".getBytes(StandardCharsets.UTF_8));
        String linkName = "symlink_student_hack.java";
        Path symlink = tempPrimaryDir.resolve(linkName);

        try {
            Files.createSymbolicLink(symlink, targetFile);
            // Cố tình resolve symlink
            try {
                storage.resolve("/uploads/submissions/" + linkName);
                fail("Should reject symbolic link");
            } catch (SecurityException expected) {
                assertTrue(expected.getMessage().contains("symbolic link"));
            }
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException e) {
            // Hệ điều hành không hỗ trợ symlink (e.g. non-privileged Windows), bỏ qua
        } finally {
            Files.deleteIfExists(symlink);
            Files.deleteIfExists(targetFile);
        }
    }

    @Test
    public void testResolveMissingReturnsNullOrThrows() throws Exception {
        assertNull(storage.resolve("/uploads/submissions/non_existent.java"));
        try {
            storage.resolveArchive("/uploads/submissions/non_existent.java");
            fail("resolveArchive should throw exception for missing file");
        } catch (IllegalArgumentException expected) {
            assertEquals("Archive unavailable", expected.getMessage());
        }
    }

    @Test
    public void testMigrateOldSubmissions() throws Exception {
        Path srcDir = Files.createTempDirectory("aita-src-migrate-");
        Path destDir = Files.createTempDirectory("aita-dest-migrate-");
        try {
            Files.write(srcDir.resolve("sub1.java"), "class A{}".getBytes(StandardCharsets.UTF_8));
            Files.write(srcDir.resolve("sub2.zip"), new byte[]{1, 2, 3});
            Files.write(srcDir.resolve("ignored.txt"), "ignored".getBytes(StandardCharsets.UTF_8));

            int migrated = UploadStorage.migrateOldSubmissions(srcDir, destDir);
            assertEquals(2, migrated);
            assertTrue(Files.exists(destDir.resolve("sub1.java")));
            assertTrue(Files.exists(destDir.resolve("sub2.zip")));
            assertFalse(Files.exists(destDir.resolve("ignored.txt")));

            // Bản gốc vẫn được giữ nguyên
            assertTrue(Files.exists(srcDir.resolve("sub1.java")));
            assertTrue(Files.exists(srcDir.resolve("sub2.zip")));
        } finally {
            deleteRecursively(srcDir);
            deleteRecursively(destDir);
        }
    }

    @Test
    public void testMigrateOldSubmissionsThreeArgsCompatibility() throws Exception {
        Path srcDir = Files.createTempDirectory("aita-src-3arg-");
        Path destDir = Files.createTempDirectory("aita-dest-3arg-");
        try {
            Files.write(srcDir.resolve("sub_old.java"), "class Old{}".getBytes(StandardCharsets.UTF_8));
            int migrated = UploadStorage.migrateOldSubmissions(srcDir, destDir, true);
            assertEquals(1, migrated);
            assertTrue(Files.exists(destDir.resolve("sub_old.java")));
            assertTrue(Files.exists(srcDir.resolve("sub_old.java")));
        } finally {
            deleteRecursively(srcDir);
            deleteRecursively(destDir);
        }
    }

    @Test
    public void testRejectAncestorSymlink() throws Exception {
        Path realDir = Files.createTempDirectory("aita-real-ancestor-");
        Path symlinkDir = tempPrimaryDir.resolve("symlink-parent");
        try {
            Files.createSymbolicLink(symlinkDir, realDir);
            Path nestedUpload = symlinkDir.resolve("submissions");
            try {
                new UploadStorage(nestedUpload, null);
                fail("Should reject uploadDir having a symlink ancestor");
            } catch (SecurityException expected) {
                assertTrue(expected.getMessage().contains("symbolic link"));
            }
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException ignored) {
        } finally {
            Files.deleteIfExists(symlinkDir);
            deleteRecursively(realDir);
        }
    }

    @Test
    public void testGenerateSafeFileName() {
        String safe = UploadStorage.generateSafeFileName("../../bài tập @#$! Nguyễn Văn A.java", "user../hacker");
        assertFalse(safe.contains(".."));
        assertFalse(safe.contains("/"));
        assertFalse(safe.contains("\\"));
        assertTrue(safe.endsWith(".java"));
        assertTrue(safe.matches("^[A-Za-z0-9._-]+$"));
    }
}
