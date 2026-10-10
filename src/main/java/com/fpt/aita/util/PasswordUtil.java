package com.fpt.aita.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Tiện ích hỗ trợ mã hóa mật khẩu bằng thuật toán SHA-256
 */
public class PasswordUtil {

    private PasswordUtil() {
        // Không khởi tạo đối tượng tiện ích
    }

    /**
     * Băm chuỗi mật khẩu thô sang chuỗi hex 64 ký tự thường bằng SHA-256
     *
     * @param raw Mật khẩu dạng chuỗi thô
     * @return Chuỗi mã băm SHA-256 hex thường (64 ký tự)
     */
    public static String sha256Hex(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Không tìm thấy thuật toán SHA-256 trên hệ thống", e);
        }
    }
}
