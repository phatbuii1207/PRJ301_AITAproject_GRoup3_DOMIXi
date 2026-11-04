package com.fpt.aita.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/** Environment > JVM property > UTF-8 .env. Configuration files are data, never shell scripts. */
public final class AppConfig {
    private AppConfig() { }
    public static String get(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) value = System.getProperty(key);
        if (value == null || value.trim().isEmpty()) value = fileValues().get(key);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
    private static Map<String, String> fileValues() {
        String explicit = System.getenv("AITA_ENV_FILE");
        if (explicit == null || explicit.trim().isEmpty()) explicit = System.getProperty("aita.env.file", ".env");
        return read(Paths.get(explicit));
    }
    public static Map<String, String> read(Path file) {
        Map<String, String> values = new LinkedHashMap<>();
        if (!Files.isRegularFile(file)) return values;
        try {
            if (Files.size(file) > 65536) throw new IllegalArgumentException("Configuration file exceeds 64 KiB");
            for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = raw.trim();
                if (line.startsWith("export ")) line = line.substring(7).trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int equals = line.indexOf('=');
                if (equals <= 0) continue;
                String key = line.substring(0, equals).trim(), value = line.substring(equals + 1).trim();
                if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) value = value.substring(1, value.length() - 1);
                if (key.matches("[A-Za-z_][A-Za-z0-9_]*")) values.put(key, value);
            }
        } catch (IOException ex) { throw new IllegalStateException("Cannot read application configuration", ex); }
        return values;
    }
}
