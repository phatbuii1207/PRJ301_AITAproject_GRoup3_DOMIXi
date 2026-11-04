package com.fpt.aita.ai;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Kiểm thử đọc source an toàn và chặn path traversal.
 */
public class FileMethodCodeProviderTest {

    public static void main(String[] args) throws Exception {
        Path tempRoot = Files.createTempDirectory("aita-ai-provider-test");
        Path submissionRoot = Files.createDirectories(tempRoot.resolve("7"));
        Path nestedSource = Files.createDirectories(submissionRoot.resolve("src")).resolve("Example.java");
        Files.write(nestedSource, "line 1\nsafe code\nline 3\n".getBytes(StandardCharsets.UTF_8));

        Path outsideSource = tempRoot.resolve("outside.java");
        Files.write(outsideSource, "outside secret\n".getBytes(StandardCharsets.UTF_8));

        int passed = 0;
        int failed = 0;
        FileMethodCodeProvider provider = new FileMethodCodeProvider(tempRoot.toString());

        if ("safe code".equals(provider.getCode(7, "src/Example.java", 2, 2))) {
            passed++;
        } else {
            System.out.println("FAILED: không đọc được source hợp lệ trong thư mục bài nộp.");
            failed++;
        }

        if ("".equals(provider.getCode(7, "../outside.java", 1, 1))) {
            passed++;
        } else {
            System.out.println("FAILED: đường dẫn có '..' không bị từ chối.");
            failed++;
        }

        if ("".equals(provider.getCode(7, outsideSource.toAbsolutePath().toString(), 1, 1))) {
            passed++;
        } else {
            System.out.println("FAILED: đường dẫn tuyệt đối không bị từ chối.");
            failed++;
        }

        Files.deleteIfExists(nestedSource);
        Files.deleteIfExists(submissionRoot.resolve("src"));
        Files.deleteIfExists(outsideSource);
        Files.deleteIfExists(submissionRoot);
        Files.deleteIfExists(tempRoot);

        System.out.println("FileMethodCodeProviderTest: " + passed + " PASSED, " + failed + " FAILED");
        if (failed > 0) {
            throw new AssertionError("FileMethodCodeProviderTest thất bại.");
        }
    }
}
