package com.fpt.aita.util;

import com.fpt.aita.config.AppConfig;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.Part;

/**
 * Quản lý lưu trữ tệp bài nộp ngoài webroot.
 * - Đọc cấu hình AITA_UPLOAD_DIR từ AppConfig (mặc định ~/aita-uploads/submissions).
 * - Chống path traversal, chặn symbolic link, whitelist [A-Za-z0-9._-], giới hạn 50MB.
 * - Giữ nguyên logic đường dẫn /uploads/submissions/ trong CSDL.
 * - Hỗ trợ fallback tìm bài nộp cũ trong webroot.
 */
public class UploadStorage {
    private static final Logger LOG = Logger.getLogger(UploadStorage.class.getName());
    public static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50MB
    public static final String PATH_PREFIX = "/uploads/submissions/";

    private static volatile UploadStorage instance;

    private final Path uploadDir;
    private volatile Path fallbackDir;

    public UploadStorage(Path uploadDir, Path fallbackDir) {
        if (uploadDir == null) {
            throw new IllegalArgumentException("uploadDir không được null");
        }
        this.uploadDir = uploadDir.toAbsolutePath().normalize();
        this.fallbackDir = fallbackDir != null ? fallbackDir.toAbsolutePath().normalize() : null;

        // Từ chối symlink ở các thành phần root/fallback và mọi ancestor trước khi ghi
        checkNoSymlinkInHierarchy(this.uploadDir, "Upload root directory");
        if (this.fallbackDir != null) {
            checkNoSymlinkInHierarchy(this.fallbackDir, "Fallback directory");
        }

        try {
            if (!Files.exists(this.uploadDir)) {
                Files.createDirectories(this.uploadDir);
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Không thể tạo thư mục upload: " + this.uploadDir, e);
        }
    }

    private static void checkNoSymlinkInHierarchy(Path dir, String name) {
        if (dir == null) return;
        Path cur = dir.toAbsolutePath().normalize();
        while (cur != null) {
            if (Files.isSymbolicLink(cur)) {
                throw new SecurityException(name + " chứa symbolic link tại: " + cur);
            }
            cur = cur.getParent();
        }
    }

    public static UploadStorage getInstance() {
        if (instance == null) {
            synchronized (UploadStorage.class) {
                if (instance == null) {
                    Path configured = resolveDefaultUploadDir();
                    instance = new UploadStorage(configured, null);
                }
            }
        }
        return instance;
    }

    public static synchronized void init(Path uploadDir, Path fallbackDir) {
        instance = new UploadStorage(uploadDir, fallbackDir);
    }

    public static Path resolveDefaultUploadDir() {
        String configured = AppConfig.get("AITA_UPLOAD_DIR", null);
        if (configured == null || configured.trim().isEmpty()) {
            return Paths.get(System.getProperty("user.home"), "aita-uploads", "submissions").toAbsolutePath().normalize();
        }
        configured = configured.trim();
        if (configured.equals("~")) {
            return Paths.get(System.getProperty("user.home")).toAbsolutePath().normalize();
        }
        if (configured.startsWith("~" + File.separator) || configured.startsWith("~/")) {
            String sub = configured.substring(2);
            return Paths.get(System.getProperty("user.home"), sub).toAbsolutePath().normalize();
        }
        return Paths.get(configured).toAbsolutePath().normalize();
    }

    public Path getUploadDir() {
        return uploadDir;
    }

    public Path getFallbackDir() {
        return fallbackDir;
    }

    public void setFallbackDir(Path fallbackDir) {
        if (fallbackDir != null) {
            checkNoSymlinkInHierarchy(fallbackDir, "Fallback directory");
        }
        this.fallbackDir = fallbackDir != null ? fallbackDir.toAbsolutePath().normalize() : null;
    }

    /**
     * Tạo tên file an toàn từ originalFileName và username.
     */
    public static String generateSafeFileName(String originalFileName, String username) {
        if (originalFileName == null) originalFileName = "submission.zip";
        if (username == null) username = "user";

        // Tách bỏ đường dẫn thư mục nếu client gửi kèm
        int lastSep = Math.max(originalFileName.lastIndexOf('/'), originalFileName.lastIndexOf('\\'));
        if (lastSep >= 0) {
            originalFileName = originalFileName.substring(lastSep + 1);
        }

        String cleanName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_").replaceAll("\\.{2,}", "_");
        String cleanUser = username.replaceAll("[^a-zA-Z0-9._-]", "_").replaceAll("\\.{2,}", "_");
        if (cleanName.isEmpty() || cleanName.equals("_")) cleanName = "submission.zip";
        if (cleanUser.isEmpty()) cleanUser = "user";

        String candidate = System.currentTimeMillis() + "_" + cleanUser + "_" + cleanName;
        if (candidate.length() > 200) {
            // Cắt ngắn nếu quá dài nhưng giữ extension
            String ext = cleanName.contains(".") ? cleanName.substring(cleanName.lastIndexOf('.')) : ".zip";
            candidate = System.currentTimeMillis() + "_" + cleanUser.substring(0, Math.min(20, cleanUser.length())) + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        }
        return candidate;
    }

    /**
     * Lưu file từ Part vào thư mục upload.
     */
    public Path save(Part part, String safeFileName) throws IOException {
        if (part == null) throw new IllegalArgumentException("Part cannot be null");
        validateFileName(safeFileName);
        long partSize = part.getSize();
        if (partSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Dung lượng tệp vượt quá giới hạn 50MB (" + partSize + " bytes)");
        }
        try (InputStream in = part.getInputStream()) {
            return save(in, safeFileName, partSize);
        }
    }

    /**
     * Lưu file từ InputStream vào thư mục upload.
     * Sử dụng StandardOpenOption.CREATE_NEW để nguyên tử hóa và chống TOCTOU symlink overwrite.
     */
    public Path save(InputStream in, String safeFileName, long expectedSize) throws IOException {
        if (in == null) throw new IllegalArgumentException("InputStream cannot be null");
        validateFileName(safeFileName);

        // Kiểm tra uploadDir và các ancestor không phải symlink
        checkNoSymlinkInHierarchy(uploadDir, "Upload root directory");

        Path target = uploadDir.resolve(safeFileName).normalize();
        if (!target.getParent().equals(uploadDir)) {
            throw new SecurityException("Phát hiện path traversal: " + safeFileName);
        }
        checkNoSymlinkInHierarchy(target, "Target path");

        // Nếu file đã tồn tại hoặc là symlink, từ chối ghi đè symlink
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isSymbolicLink(target)) {
                throw new SecurityException("Không được ghi đè symbolic link: " + target);
            }
            // Tạo tên ngẫu nhiên tránh va chạm tên
            String ext = safeFileName.contains(".") ? safeFileName.substring(safeFileName.lastIndexOf('.')) : ".zip";
            String base = safeFileName.contains(".") ? safeFileName.substring(0, safeFileName.lastIndexOf('.')) : safeFileName;
            String uniqueName = base + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            target = uploadDir.resolve(uniqueName).normalize();
        }

        // Ghi với CREATE_NEW nguyên tử, không đi theo symlink
        try (OutputStream out = Files.newOutputStream(target,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_FILE_SIZE) {
                    out.close();
                    Files.deleteIfExists(target);
                    throw new IllegalArgumentException("Dung lượng tệp vượt quá giới hạn 50MB");
                }
                out.write(buffer, 0, read);
            }
        }

        if (Files.isSymbolicLink(target)) {
            Files.deleteIfExists(target);
            throw new SecurityException("Phát hiện symbolic link sau khi ghi: " + target);
        }
        if (!target.toRealPath().getParent().equals(uploadDir.toRealPath())) {
            Files.deleteIfExists(target);
            throw new SecurityException("Real path của tệp thoát khỏi thư mục upload: " + target);
        }

        return target;
    }

    /**
     * Phân giải đường dẫn lưu trong DB thành Path trên đĩa (kiểm tra thư mục chính, sau đó fallback).
     */
    public Path resolve(String storedPath) {
        if (storedPath == null || storedPath.trim().isEmpty()) {
            return null;
        }
        String fileName = extractFileName(storedPath);
        if (fileName == null) {
            return null;
        }
        try {
            validateFileName(fileName);
        } catch (IllegalArgumentException e) {
            return null;
        }

        // 1. Kiểm tra thư mục upload chính
        Path primary = uploadDir.resolve(fileName).normalize();
        Path verifiedPrimary = verifyExistingFile(primary, uploadDir);
        if (verifiedPrimary != null) {
            return verifiedPrimary;
        }

        // 2. Kiểm tra fallbackDir được cấu hình
        if (fallbackDir != null && Files.isDirectory(fallbackDir)) {
            Path fallback = fallbackDir.resolve(fileName).normalize();
            Path verifiedFallback = verifyExistingFile(fallback, fallbackDir);
            if (verifiedFallback != null) {
                return verifiedFallback;
            }
        }

        // 3. Kiểm tra các thư mục webroot mặc định nếu fallbackDir chưa bắt được
        Path[] defaultCandidates = {
            Paths.get("src/main/webapp/uploads/submissions"),
            Paths.get("target/AITA_StaticCodeAnalysis-1.0-SNAPSHOT/uploads/submissions")
        };
        for (Path candDir : defaultCandidates) {
            if (Files.isDirectory(candDir)) {
                Path cand = candDir.resolve(fileName).normalize();
                Path verifiedCand = verifyExistingFile(cand, candDir);
                if (verifiedCand != null) {
                    return verifiedCand;
                }
            }
        }

        return null;
    }

    /**
     * Dùng cho GradingQueueManager: phân giải archive, nếu không tìm thấy hoặc không hợp lệ thì ném ngoại lệ.
     */
    public Path resolveArchive(String storedPath) throws Exception {
        if (storedPath == null || !storedPath.startsWith(PATH_PREFIX)) {
            throw new IllegalArgumentException("Invalid upload path: " + storedPath);
        }
        String fileName = extractFileName(storedPath);
        if (fileName == null) {
            throw new IllegalArgumentException("Invalid archive name");
        }
        validateFileName(fileName);
        Path resolved = resolve(storedPath);
        if (resolved == null || !Files.isRegularFile(resolved, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Archive unavailable");
        }
        return resolved;
    }

    private Path verifyExistingFile(Path file, Path baseDir) {
        try {
            if (!file.getParent().equals(baseDir)) {
                return null;
            }
            if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                return null;
            }
            checkNoSymlinkInHierarchy(file, "File");
            checkNoSymlinkInHierarchy(baseDir, "Base directory");

            if (Files.isSymbolicLink(file)) {
                throw new SecurityException("Chặn symbolic link: " + file);
            }
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                return null;
            }
            if (Files.size(file) > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("Tệp vượt quá 50MB: " + file);
            }
            if (!file.toRealPath().getParent().equals(baseDir.toRealPath())) {
                throw new SecurityException("Tệp thoát khỏi thư mục qua realPath: " + file);
            }
            return file;
        } catch (IOException e) {
            return null;
        }
    }

    public static String extractFileName(String storedPath) {
        if (storedPath == null) return null;
        String s = storedPath.trim();
        if (s.startsWith(PATH_PREFIX)) {
            s = s.substring(PATH_PREFIX.length());
        } else if (s.startsWith("uploads/submissions/")) {
            s = s.substring("uploads/submissions/".length());
        } else if (s.contains("/") || s.contains("\\")) {
            return null; // Đường dẫn lạ, không thuộc cấu trúc uploads/submissions/
        }
        return s.isEmpty() ? null : s;
    }

    /**
     * Whitelist nghiêm ngặt [A-Za-z0-9._-] với độ dài giới hạn (5 .. 255),
     * chống triệt để control characters, CRLF injection, null bytes, và path traversal.
     */
    public static void validateFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên tệp không được để trống");
        }
        if (fileName.length() < 5 || fileName.length() > 255) {
            throw new IllegalArgumentException("Độ dài tên tệp không hợp lệ (phải từ 5 đến 255 ký tự): " + fileName.length());
        }
        if (!fileName.matches("^[A-Za-z0-9._-]+$")) {
            throw new IllegalArgumentException("Tên tệp chứa ký tự không hợp lệ; chỉ chấp nhận [A-Za-z0-9._-]: " + fileName);
        }
        if (fileName.contains("..") || fileName.startsWith(".")) {
            throw new IllegalArgumentException("Tên tệp không được chứa '..' hoặc bắt đầu bằng dấu chấm: " + fileName);
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".zip") && !lower.endsWith(".java")) {
            throw new IllegalArgumentException("Định dạng tệp không được hỗ trợ (chỉ nhận .zip hoặc .java): " + fileName);
        }
    }

    /**
     * Di chuyển bài nộp từ thư mục cũ sang thư mục mới an toàn, không xóa bản gốc.
     */
    public static int migrateOldSubmissions(Path sourceDir, Path targetDir) throws IOException {
        if (sourceDir == null || !Files.isDirectory(sourceDir)) return 0;
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        int count = 0;
        try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(file)) {
                    String name = file.getFileName().toString();
                    try {
                        validateFileName(name);
                    } catch (IllegalArgumentException ignored) {
                        continue;
                    }
                    Path dest = targetDir.resolve(name);
                    if (!Files.exists(dest, LinkOption.NOFOLLOW_LINKS)) {
                        Files.copy(file, dest, StandardCopyOption.COPY_ATTRIBUTES);
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Bản tương thích 3 tham số. Luôn an toàn: không tự động xóa bản gốc khi chưa có xác nhận từ người dùng.
     */
    public static int migrateOldSubmissions(Path sourceDir, Path targetDir, boolean deleteSource) throws IOException {
        return migrateOldSubmissions(sourceDir, targetDir);
    }
}
