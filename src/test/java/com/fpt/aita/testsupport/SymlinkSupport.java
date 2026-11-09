package com.fpt.aita.testsupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.Assume;

/** Skip only symlink-dependent tests where the OS cannot create the fixture. */
public final class SymlinkSupport {
    private SymlinkSupport() { }

    public static void createOrSkip(Path link, Path target) throws IOException {
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException ex) {
            Assume.assumeNoException("Filesystem does not support symbolic links", ex);
        } catch (IOException ex) {
            // Windows commonly reports FileSystemException for missing symlink privilege.
            // Keep unexpected filesystem errors visible on other systems.
            if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) throw ex;
            Assume.assumeNoException("Windows could not create the symbolic-link fixture; enable Developer Mode or grant symlink privilege", ex);
        }
    }
}
