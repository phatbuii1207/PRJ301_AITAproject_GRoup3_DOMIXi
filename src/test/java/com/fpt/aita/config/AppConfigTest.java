package com.fpt.aita.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class AppConfigTest {
    @Test public void parsesUtf8AsDataWithoutShellExpansion() throws Exception {
        Path file=Files.createTempFile("aita-config-", ".env");
        try {
            Files.write(file,("# comment\nexport NAME='Tiếng Việt'\nVALUE=\"a=b#c\"\nCOMMAND=$(touch /tmp/never-execute-aita)\ninvalid-key=x\n").getBytes(StandardCharsets.UTF_8));
            Map<String,String> values=AppConfig.read(file);
            assertEquals("Tiếng Việt",values.get("NAME"));assertEquals("a=b#c",values.get("VALUE"));
            assertEquals("$(touch /tmp/never-execute-aita)",values.get("COMMAND"));assertFalse(values.containsKey("invalid-key"));
        } finally { Files.deleteIfExists(file); }
    }
    @Test public void missingFileIsOptionalAndOversizedFileIsRejected() throws Exception {
        Path dir=Files.createTempDirectory("aita-config-");Path file=dir.resolve(".env");
        try { assertTrue(AppConfig.read(file).isEmpty());Files.write(file,new byte[65537]);
            try { AppConfig.read(file);fail("oversized configuration accepted"); } catch(IllegalArgumentException expected) { }
        } finally { Files.deleteIfExists(file);Files.delete(dir); }
    }
    @Test public void acceptsWindowsUtf8BomAndCrlf() throws Exception {
        Path file=Files.createTempFile("aita-windows-", ".env");
        try {
            Files.write(file,("\uFEFFDB_URL=jdbc:postgresql://example:6543/postgres?sslmode=require&prepareThreshold=0\r\nDB_PASSWORD='a=b#c'\r\n").getBytes(StandardCharsets.UTF_8));
            assertEquals("jdbc:postgresql://example:6543/postgres?sslmode=require&prepareThreshold=0",AppConfig.read(file).get("DB_URL"));
            assertEquals("a=b#c",AppConfig.read(file).get("DB_PASSWORD"));
        } finally {Files.delete(file);}
    }

}
