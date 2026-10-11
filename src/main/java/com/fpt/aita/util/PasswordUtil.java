package com.fpt.aita.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Tiện ích hỗ trợ mã hóa mật khẩu.
 * LƯU Ý: Dữ liệu seed trong CSDL (AITA_Database.sql) dùng MD5 để băm mật khẩu.
 * Phương thức sha256Hex() được giữ nguyên tên nhưng thực hiện băm MD5 để tương thích.
 */
public class PasswordUtil {

    private PasswordUtil() {
        // Không khởi tạo đối tượng tiện ích
    }

    /**
     * Băm chuỗi mật khẩu thô sang chuỗi hex MD5 (32 ký tự thường)
     * để khớp với hash đã được lưu trong CSDL seed data.
     *
     * @param raw Mật khẩu dạng chuỗi thô
     * @return Chuỗi mã băm MD5 hex thường (32 ký tự)
     */
    public static String sha256Hex(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Không tìm thấy thuật toán MD5 trên hệ thống", e);
        }
    }
}

