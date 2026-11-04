package com.fpt.aita.service;

import com.fpt.aita.ai.AiAnalysisService;
import com.fpt.aita.ai.AiClientFactory;
import com.fpt.aita.ai.AiConfig;
import com.fpt.aita.ai.FileMethodCodeProvider;
import com.fpt.aita.dal.AiOptimizationDAO;
import com.fpt.aita.util.ZipExtractorUtil;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;

/** Preserves the team's AST/AI pipeline. It never changes functional test scores. */
public final class SubmissionAnalysisService {
    public boolean analyze(int submissionId, Path source) throws Exception {
        boolean zip = source.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".zip");
        // Validate ZIP budgets before either host-side parser/extractor sees untrusted input.
        if (zip) {
            try (SandboxWorkspace validated = SandboxWorkspace.extract(source)) {
                return analyzeValidated(submissionId, source, true);
            }
        }
        if (Files.size(source) > 16L * 1024 * 1024) throw new IOException("Java source exceeds analysis limit");
        return analyzeValidated(submissionId, source, false);
    }

    private boolean analyzeValidated(int id, Path source, boolean zip) throws Exception {
        if (!AstAnalyzerService.getInstance().analyzeAndSave(id, source.toFile())) return false;
        AiConfig config = new AiConfig();
        String configured = config.getExtractedRoot();
        boolean temporary = configured == null || configured.trim().isEmpty();
        Path root = temporary ? Files.createTempDirectory("aita-ai-") : Paths.get(configured).toAbsolutePath();
        try {
            if (zip) ZipExtractorUtil.extract(source.toString(), root.toString(), id);
            else {
                Path directory = Files.createDirectories(root.resolve(String.valueOf(id)));
                Files.copy(source, directory.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
            AiOptimizationDAO dao = new AiOptimizationDAO();
            try {
                new AiAnalysisService(AiClientFactory.create(config), config, new FileMethodCodeProvider(root.toString()), dao)
                        .analyzeSubmission(id);
            } finally { dao.closeConnection(); }
            return true;
        } finally {
            if (temporary) Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file); return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult postVisitDirectory(Path dir, IOException failure) throws IOException {
                    if (failure != null) throw failure;
                    Files.delete(dir); return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
