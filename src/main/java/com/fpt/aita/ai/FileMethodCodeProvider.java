package com.fpt.aita.ai;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        Path submissionRoot;
        Path requestedPath;
        Path requestedRelativePath;
        try {
            requestedRelativePath = Paths.get(fileName.trim());
            if (requestedRelativePath.isAbsolute() || requestedRelativePath.getRoot() != null
                    || fileName.trim().matches("^[A-Za-z]:.*")) {
                System.err.println("[FileMethodCodeProvider] Từ chối đường dẫn tuyệt đối.");
                return "";
            }
            for (Path segment : requestedRelativePath) {
                if ("..".equals(segment.toString())) {
                    System.err.println("[FileMethodCodeProvider] Từ chối đường dẫn chứa '..'.");
                    return "";
                }
            }

            submissionRoot = Paths.get(extractedRoot).toAbsolutePath().normalize()
                    .resolve(String.valueOf(submissionId)).normalize();
            requestedPath = submissionRoot.resolve(requestedRelativePath).normalize();
            if (!requestedPath.startsWith(submissionRoot)) {
                System.err.println("[FileMethodCodeProvider] Từ chối đường dẫn nằm ngoài thư mục bài nộp.");
                return "";
            }
        } catch (RuntimeException e) {
            System.err.println("[FileMethodCodeProvider] Đường dẫn file không hợp lệ.");
            return "";
        }

        File rootDir = submissionRoot.toFile();
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            System.err.println("[FileMethodCodeProvider] Không tìm thấy thư mục: " + rootDir.getAbsolutePath());
            return "";
        }

        Path canonicalRoot;
        try {
            canonicalRoot = rootDir.getCanonicalFile().toPath();
        } catch (IOException e) {
            System.err.println("[FileMethodCodeProvider] Không thể xác thực thư mục bài nộp: " + e.getMessage());
            return "";
        }

        File targetFile = findSafeFile(requestedPath.toFile(), rootDir, requestedRelativePath,
                canonicalRoot);
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
    private File findSafeFile(File requestedFile, File rootDir, Path requestedRelativePath, Path canonicalRoot) {
        if (requestedFile.isFile() && isWithinRoot(requestedFile, canonicalRoot)) {
            return requestedFile;
        }
        Path fileName = requestedRelativePath.getFileName();
        if (fileName == null) {
            return null;
        }
        return findFileRecursive(rootDir, fileName.toString(), canonicalRoot, new HashSet<String>());
    }

    private File findFileRecursive(File dir, String targetName, Path canonicalRoot, Set<String> visited) {
        if (!isWithinRoot(dir, canonicalRoot)) {
            return null;
        }
        String canonicalDir;
        try {
            canonicalDir = dir.getCanonicalPath();
        } catch (IOException e) {
            return null;
        }
        if (!visited.add(canonicalDir)) {
            return null;
        }

        File[] entries = dir.listFiles();
        if (entries == null) {
            return null;
        }
        List<File> subdirs = new ArrayList<File>();
        for (File entry : entries) {
            if (!isWithinRoot(entry, canonicalRoot)) {
                continue;
            }
            if (entry.isFile() && entry.getName().equals(targetName)) {
                return entry;
            }
            if (entry.isDirectory()) {
                subdirs.add(entry);
            }
        }
        // Duyệt thư mục con sau (BFS-style: file trước, thư mục con sau)
        for (File subdir : subdirs) {
            File found = findFileRecursive(subdir, targetName, canonicalRoot, visited);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private boolean isWithinRoot(File file, Path canonicalRoot) {
        try {
            return file.getCanonicalFile().toPath().startsWith(canonicalRoot);
        } catch (IOException e) {
            return false;
        }
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
