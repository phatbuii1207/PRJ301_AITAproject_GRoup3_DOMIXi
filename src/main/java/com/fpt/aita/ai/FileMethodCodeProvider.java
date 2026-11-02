package com.fpt.aita.ai;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Đọc mã nguồn từ file đã giải nén trên đĩa dưới thư mục:
 *   {extractedRoot}/{submissionId}/
 * Tìm file theo tên một cách đệ quy (tên file có thể nằm sâu trong thư mục con).
 * Tương thích Java 1.8, không dùng Files.walk hay Stream API.
 */
public class FileMethodCodeProvider implements MethodCodeProvider {

    private final String extractedRoot;

    public FileMethodCodeProvider(String extractedRoot) {
        if (extractedRoot == null || extractedRoot.trim().isEmpty()) {
            throw new IllegalArgumentException("extractedRoot không được để trống");
        }
        this.extractedRoot = extractedRoot.trim();
    }

    @Override
    public String getCode(int submissionId, String fileName, int startLine, int endLine) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "";
        }
        if (startLine <= 0 || endLine < startLine) {
            return "";
        }

        // Thư mục gốc của bài nộp: {extractedRoot}/{submissionId}/
        File rootDir = new File(extractedRoot, String.valueOf(submissionId));
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            System.err.println("[FileMethodCodeProvider] Không tìm thấy thư mục: " + rootDir.getAbsolutePath());
            return "";
        }

        // Tìm đệ quy file theo tên
        File targetFile = findFileRecursive(rootDir, fileName.trim());
        if (targetFile == null) {
            System.err.println("[FileMethodCodeProvider] Không tìm thấy file '" + fileName + "' trong " + rootDir.getAbsolutePath());
            return "";
        }

        return readLines(targetFile, startLine, endLine);
    }

    /**
     * Tìm file theo tên (chính xác) đệ quy trong thư mục dir.
     * Không dùng java.nio.Files.walk, tương thích Java 1.8.
     */
    private File findFileRecursive(File dir, String targetName) {
        File[] entries = dir.listFiles();
        if (entries == null) {
            return null;
        }
        List<File> subdirs = new ArrayList<File>();
        for (File entry : entries) {
            if (entry.isFile() && entry.getName().equals(targetName)) {
                return entry;
            }
            if (entry.isDirectory()) {
                subdirs.add(entry);
            }
        }
        // Duyệt thư mục con sau (BFS-style: file trước, thư mục con sau)
        for (File subdir : subdirs) {
            File found = findFileRecursive(subdir, targetName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Đọc các dòng từ startLine đến endLine (1-indexed, bao gồm 2 đầu) từ file.
     * Đóng stream đúng cách bằng try-with-resources.
     */
    private String readLines(File file, int startLine, int endLine) {
        StringBuilder sb = new StringBuilder();
        int currentLine = 0;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                currentLine++;
                if (currentLine >= startLine && currentLine <= endLine) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(line);
                }
                if (currentLine > endLine) {
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("[FileMethodCodeProvider] Lỗi đọc file '" + file.getAbsolutePath() + "': " + e.getMessage());
        }
        return sb.toString();
    }
}
