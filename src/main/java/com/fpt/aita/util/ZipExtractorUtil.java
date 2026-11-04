package com.fpt.aita.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * THÀNH VIÊN 3: Tiện ích giải nén file .zip bài nộp của sinh viên.
 * - Chống Zip-Slip: kiểm tra canonical path trước khi ghi file.
 * - Tương thích Java 1.8 (không dùng Files.walk, Stream API, v.v.)
 * - Trả về danh sách đường dẫn tương đối của các file .java được giải nén.
 */
public final class ZipExtractorUtil {

    private static final int BUFFER_SIZE = 8192;

    // Giới hạn bảo vệ: tối đa 50 MB khi giải nén và 1000 entries
    private static final long MAX_TOTAL_SIZE = 50L * 1024 * 1024;
    private static final int  MAX_ENTRIES    = 1000;

    private ZipExtractorUtil() {
        // Utility class — không cho khởi tạo
    }

    /**
     * Giải nén file zip vào thư mục đích.
     *
     * @param zipFilePath  Đường dẫn tuyệt đối tới file .zip cần giải nén
     * @param extractedRoot Thư mục gốc lưu kết quả giải nén (tuyệt đối hoặc tương đối từ JVM working dir)
     * @param submissionId  ID bài nộp — kết quả sẽ nằm trong {extractedRoot}/{submissionId}/
     * @return Danh sách đường dẫn tương đối (tính từ destDir) của các file .java được giải nén.
     *         Trả về danh sách rỗng nếu zip hỏng hoặc không có file .java nào.
     */
    public static List<String> extract(String zipFilePath, String extractedRoot, int submissionId) {
        List<String> javaFiles = new ArrayList<String>();

        if (zipFilePath == null || zipFilePath.trim().isEmpty()) {
            System.err.println("[ZipExtractor] zipFilePath trống, bỏ qua giải nén.");
            return javaFiles;
        }

        File zipFile = new File(zipFilePath);
        if (!zipFile.exists() || !zipFile.isFile()) {
            System.err.println("[ZipExtractor] Không tìm thấy file zip: " + zipFilePath);
            return javaFiles;
        }

        // Tạo thư mục đích: {extractedRoot}/{submissionId}/
        File destDir = new File(extractedRoot, String.valueOf(submissionId));
        if (!destDir.exists()) {
            boolean created = destDir.mkdirs();
            if (!created) {
                System.err.println("[ZipExtractor] Không thể tạo thư mục đích: " + destDir.getAbsolutePath());
                return javaFiles;
            }
        }

        String canonicalDest;
        try {
            canonicalDest = destDir.getCanonicalPath();
        } catch (IOException e) {
            System.err.println("[ZipExtractor] Không thể lấy canonical path của destDir: " + e.getMessage());
            return javaFiles;
        }

        // Giải nén
        InputStream fis = null;
        ZipInputStream zis = null;
        try {
            fis = new FileInputStream(zipFile);
            zis = new ZipInputStream(fis);

            long totalBytesExtracted = 0;
            int entryCount = 0;
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_ENTRIES) {
                    System.err.println("[ZipExtractor] Vượt quá giới hạn " + MAX_ENTRIES + " entries, dừng giải nén.");
                    break;
                }

                String entryName = sanitizeEntryName(entry.getName());
                if (entryName == null || entryName.isEmpty()) {
                    zis.closeEntry();
                    continue;
                }

                File targetFile = new File(destDir, entryName);

                // ===== CHỐNG ZIP-SLIP =====
                String canonicalTarget;
                try {
                    canonicalTarget = targetFile.getCanonicalPath();
                } catch (IOException e) {
                    System.err.println("[ZipExtractor] Bỏ qua entry không hợp lệ: " + entryName);
                    zis.closeEntry();
                    continue;
                }

                if (!canonicalTarget.startsWith(canonicalDest + File.separator)
                        && !canonicalTarget.equals(canonicalDest)) {
                    System.err.println("[ZipExtractor] CẢNH BÁO Zip-Slip bị chặn: " + entryName);
                    zis.closeEntry();
                    continue;
                }
                // ==========================

                if (entry.isDirectory()) {
                    if (!targetFile.exists()) {
                        targetFile.mkdirs();
                    }
                    zis.closeEntry();
                    continue;
                }

                // Tạo thư mục cha nếu chưa có
                File parentDir = targetFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                // Ghi file
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(targetFile);
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        totalBytesExtracted += len;
                        if (totalBytesExtracted > MAX_TOTAL_SIZE) {
                            System.err.println("[ZipExtractor] Vượt quá giới hạn kích thước " + MAX_TOTAL_SIZE / (1024 * 1024) + " MB.");
                            return javaFiles; // Trả về những gì đã giải nén được
                        }
                        fos.write(buffer, 0, len);
                    }
                } finally {
                    if (fos != null) {
                        try { fos.close(); } catch (IOException ignored) {}
                    }
                }

                // Ghi nhận file .java
                if (entryName.toLowerCase().endsWith(".java")) {
                    javaFiles.add(entryName);
                }

                zis.closeEntry();
            }

            System.out.println("[ZipExtractor] Giải nén thành công submissionId=" + submissionId
                    + " | " + javaFiles.size() + " file .java | Tổng " + totalBytesExtracted / 1024 + " KB"
                    + " | Thư mục: " + destDir.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("[ZipExtractor] Lỗi khi giải nén submissionId=" + submissionId + ": " + e.getMessage());
            // Trả về danh sách những gì đã giải nén được (có thể rỗng)
        } finally {
            if (zis != null) {
                try { zis.close(); } catch (IOException ignored) {}
            }
            if (fis != null) {
                try { fis.close(); } catch (IOException ignored) {}
            }
        }

        return javaFiles;
    }

    /**
     * Làm sạch tên entry: loại bỏ ký tự điều khiển, chuẩn hoá dấu phân cách.
     */
    private static String sanitizeEntryName(String name) {
        if (name == null) return null;
        // Chuẩn hoá dấu / và \
        String cleaned = name.replace('\\', '/');
        // Loại bỏ tiền tố ../ (đề phòng path traversal)
        while (cleaned.startsWith("../") || cleaned.startsWith("./")) {
            cleaned = cleaned.replaceFirst("^\\.\\.?/", "");
        }
        // Xoá ký tự điều khiển
        cleaned = cleaned.replaceAll("[\\x00-\\x1f\\x7f]", "");
        return cleaned.trim();
    }
}
