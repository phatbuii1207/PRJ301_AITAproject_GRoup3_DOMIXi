package com.fpt.aita.service;

import com.fpt.aita.model.SandboxResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class SandboxWorkspaceTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void rejectsTraversalWithoutWritingOutsideStaging() throws Exception {
        assertInvalid(archive("../Main.java", "class Main {}"));
        assertInvalid(archive("folder/../../Main.java", "class Main {}"));
        assertInvalid(archive("/Main.java", "class Main {}"));
        assertInvalid(archive("C:\\Main.java", "class Main {}"));
    }

    @Test public void rejectsCompilerArgumentInjectionAndEmptySource() throws Exception {
        assertInvalid(archive("Main\n-processor.java", "class Main {}"));
        assertInvalid(archive("Main\".java", "class Main {}"));
        assertInvalid(archive("readme.txt", "no java"));
    }

    @Test public void rejectsExpandedBombEvenWhenItsFileIsIgnored() throws Exception {
        Path zip = temporary.newFile().toPath();
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("ignored.bin"));
            byte[] zeros = new byte[8192];
            for (int i = 0; i <= SandboxWorkspace.MAX_EXPANDED_BYTES / zeros.length; i++) out.write(zeros);
            out.closeEntry();
        }
        assertInvalid(zip);
    }

    @Test public void rejectsTooManyEntriesAndNormalizedDuplicates() throws Exception {
        Path zip = temporary.newFile().toPath();
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (int i = 0; i <= SandboxWorkspace.MAX_ENTRIES; i++) {
                out.putNextEntry(new ZipEntry("ignored" + i)); out.closeEntry();
            }
        }
        assertInvalid(zip);
        zip = temporary.newFile().toPath();
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (String name : Arrays.asList("Main.java", "./Main.java")) {
                out.putNextEntry(new ZipEntry(name)); out.write("class Main {}".getBytes(StandardCharsets.UTF_8)); out.closeEntry();
            }
        }
        assertInvalid(zip);
    }

    @Test public void extractsOnlySourcesSupportsSpacesAndCleansUp() throws Exception {
        Path directory;
        try (SandboxWorkspace workspace = SandboxWorkspace.extract(archive("My Project/demo/Main.java", "package demo; class Main {}"))) {
            directory = workspace.getSources();
            assertTrue(Files.isRegularFile(directory.resolve("My Project/demo/Main.java")));
            assertEquals("\"/workspace/My Project/demo/Main.java\"",
                    Files.readAllLines(directory.resolve(".aita-sources.args"), StandardCharsets.UTF_8).get(0));
        }
        assertFalse(Files.exists(directory));
    }

    @Test public void rejectsMainClassInjectionBeforeDockerIsCalled() throws Exception {
        SandboxResult result = new DockerSandboxService().execute(archive("Main.java", "class Main {}"),
                "Main;touch /tmp/owned", new byte[0]);
        assertEquals(SandboxResult.Status.INVALID_SUBMISSION, result.getStatus());
    }

    Path archive(String name, String source) throws IOException {
        Path zip = temporary.newFile().toPath();
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            out.putNextEntry(new ZipEntry(name));
            out.write(source.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return zip;
    }

    private void assertInvalid(Path zip) throws Exception {
        try (SandboxWorkspace ignored = SandboxWorkspace.extract(zip)) {
            fail("Unsafe ZIP accepted: " + zip);
        } catch (SandboxWorkspace.InvalidSubmissionException expected) {
            assertFalse(expected.getMessage().isEmpty());
        }
    }
}
