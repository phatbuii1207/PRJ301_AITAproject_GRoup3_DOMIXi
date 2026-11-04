package com.fpt.aita.service;

import com.fpt.aita.dal.AnalysisReportDAO;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * THÀNH VIÊN 1: ENGINE BÓC TÁCH MÃ NGUỒN TĨNH AST & ĐỐI SOÁT RULES
 * - Phân tích cú pháp Java bằng AST (Abstract Syntax Tree).
 * - Bóc tách danh sách file (SOURCE_FILES), danh sách hàm (METHODS).
 * - Đo lường độ phức tạp thuật toán (O(1), O(N), O(N^2)).
 * - Quét các lỗi bảo mật, code smells (ISSUES) theo bảng RULES.
 * - Tự động tổng hợp và ghi bản ghi vào CSDL (ANALYSIS_REPORTS).
 */
public class AstAnalyzerService {

    private static volatile AstAnalyzerService instance;
    private final AnalysisReportDAO reportDAO;

    private AstAnalyzerService() {
        this.reportDAO = new AnalysisReportDAO();
    }

    public static AstAnalyzerService getInstance() {
        if (instance == null) {
            synchronized (AstAnalyzerService.class) {
                if (instance == null) {
                    instance = new AstAnalyzerService();
                }
            }
        }
        return instance;
    }

    /**
     * Phân tích toàn bộ bài nộp (hỗ trợ cả file .java đơn lẻ hoặc file nén .zip)
     */
    public boolean analyzeAndSave(int submissionId, File submissionFile) {
        if (submissionFile == null || !submissionFile.exists()) {
            System.err.println("AstAnalyzer: File bài nộp không tồn tại!");
            return false;
        }

        try {
            // Dọn dẹp dữ liệu phân tích cũ nếu có
            reportDAO.cleanOldAnalysisData(submissionId);

            List<File> javaFiles = new ArrayList<>();
            File tempExtractDir = null;

            if (submissionFile.getName().toLowerCase().endsWith(".zip")) {
                tempExtractDir = Files.createTempDirectory("aita_ast_zip_").toFile();
                extractJavaFilesFromZip(submissionFile, tempExtractDir, javaFiles);
            } else if (submissionFile.getName().toLowerCase().endsWith(".java")) {
                javaFiles.add(submissionFile);
            }

            if (javaFiles.isEmpty()) {
                System.out.println("AstAnalyzer: Không tìm thấy file mã nguồn .java trong bài nộp #" + submissionId);
                reportDAO.insertAnalysisReport(submissionId, 100, 0);
                return true;
            }

            // Đọc ID của các bộ luật từ CSDL
            int rulePerf01 = reportDAO.getRuleIdByCode("PERF01"); // Nested Loops O(N^2)
            int rulePerf02 = reportDAO.getRuleIdByCode("PERF02"); // String concat in loop
            int ruleSec01 = reportDAO.getRuleIdByCode("SEC01");   // SQL Injection
            int ruleSec02 = reportDAO.getRuleIdByCode("SEC02");   // Resource Leak
            int ruleCode01 = reportDAO.getRuleIdByCode("CODE01"); // Cyclomatic Complexity > 10
            int ruleCode02 = reportDAO.getRuleIdByCode("CODE02"); // God Method (> 50 lines)
            int ruleNam01 = reportDAO.getRuleIdByCode("NAM01");   // Naming convention

            int totalIssuesCount = 0;
            int criticalCount = 0;
            int warningCount = 0;
            int infoCount = 0;

            for (File jf : javaFiles) {
                List<String> lines = Files.readAllLines(jf.toPath(), StandardCharsets.UTF_8);
                int loc = lines.size();
                String fileName = jf.getName();

                int fileId = reportDAO.insertSourceFile(submissionId, fileName, loc);
                if (fileId <= 0) continue;

                CompilationUnit cu;
                try {
                    cu = StaticJavaParser.parse(jf);
                } catch (Exception parseEx) {
                    System.err.println("Lỗi cú pháp khi parse AST file " + fileName + ": " + parseEx.getMessage());
                    // Ghi nhận lỗi cú pháp
                    reportDAO.insertIssue(fileId, null, ruleCode01, 1, "Warning",
                            "File có lỗi cú pháp biên dịch: " + parseEx.getMessage(),
                            "Kiểm tra lại cú pháp Java của file để trình biên dịch có thể sinh cây AST hoàn chỉnh.");
                    totalIssuesCount++;
                    warningCount++;
                    continue;
                }

                List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
                for (MethodDeclaration md : methods) {
                    String methodName = md.getNameAsString();
                    int startLine = md.getBegin().map(p -> p.line).orElse(1);
                    int endLine = md.getEnd().map(p -> p.line).orElse(startLine);

                    // 1. Tính toán độ sâu vòng lặp lồng nhau (Loop Nesting Depth)
                    int maxLoopDepth = calculateMaxLoopDepth(md);

                    // 2. Dự đoán độ phức tạp thuật toán (Estimated Complexity)
                    String estimatedComplexity = "O(1)";
                    if (maxLoopDepth == 1) {
                        estimatedComplexity = "O(N)";
                    } else if (maxLoopDepth == 2) {
                        estimatedComplexity = "O(N^2)";
                    } else if (maxLoopDepth >= 3) {
                        estimatedComplexity = "O(N^" + maxLoopDepth + ")";
                    }

                    // 3. Tính toán độ phức tạp Cyclomatic (Cyclomatic Complexity)
                    int cyclomatic = calculateCyclomaticComplexity(md);

                    // 4. Kiểm tra đệ quy (isRecursive)
                    boolean isRecursive = md.findAll(MethodCallExpr.class).stream()
                            .anyMatch(call -> call.getNameAsString().equals(methodName));

                    int methodId = reportDAO.insertMethod(fileId, methodName, startLine, endLine,
                            maxLoopDepth, cyclomatic, estimatedComplexity, isRecursive);

                    // 5. QUÉT ĐỐI CHIẾU CÁC QUY TẮC RULES
                    // Rule PERF01: Nested Loops O(N^2) trở lên
                    if (maxLoopDepth >= 2) {
                        reportDAO.insertIssue(fileId, methodId, rulePerf01, startLine, "Warning",
                                "Phát hiện " + maxLoopDepth + " vòng lặp lồng nhau trong hàm '" + methodName + "' (Độ phức tạp " + estimatedComplexity + ").",
                                "Vòng lặp lồng nhau có chi phí tính toán cao. Hãy cân nhắc dùng cấu trúc dữ liệu băm (HashMap/HashSet) để giảm độ phức tạp về O(N).");
                        totalIssuesCount++;
                        warningCount++;
                    }

                    // Rule PERF02: Ghép chuỗi trong vòng lặp
                    if (hasStringConcatInLoop(md)) {
                        reportDAO.insertIssue(fileId, methodId, rulePerf02, startLine, "Warning",
                                "Ghép chuỗi (+) trong vòng lặp của hàm '" + methodName + "'.",
                                "Cộng chuỗi trực tiếp trong vòng lặp tạo nhiều đối tượng String rác trong Heap. Hãy sử dụng StringBuilder để tối ưu bộ nhớ.");
                        totalIssuesCount++;
                        warningCount++;
                    }

                    // Rule SEC01: Lỗ hổng SQL Injection (Ghép chuỗi trong SQL)
                    if (hasSqlInjectionRisk(md)) {
                        reportDAO.insertIssue(fileId, methodId, ruleSec01, startLine, "Critical",
                                "Nguy cơ SQL Injection: Phát hiện ghép chuỗi trực tiếp vào câu lệnh SQL trong hàm '" + methodName + "'.",
                                "Việc nối chuỗi trực tiếp vào câu truy vấn cho phép kẻ tấn công phá vỡ cấu trúc SQL. Bắt buộc dùng PreparedStatement với tham số (?).");
                        totalIssuesCount++;
                        criticalCount++;
                    }

                    // Rule SEC02: Rò rỉ tài nguyên kết nối (Resource Leak)
                    if (hasResourceLeakRisk(md)) {
                        reportDAO.insertIssue(fileId, methodId, ruleSec02, startLine, "Warning",
                                "Nguy cơ rò rỉ tài nguyên (Resource Leak): Mở kết nối Database hoặc luồng I/O trong hàm '" + methodName + "' mà không đóng an toàn.",
                                "Hãy sử dụng cấu trúc try-with-resources để đảm bảo Connection, PreparedStatement, ResultSet luôn được giải phóng.");
                        totalIssuesCount++;
                        warningCount++;
                    }

                    // Rule CODE01: Độ phức tạp Cyclomatic quá cao (> 10)
                    if (cyclomatic > 10) {
                        reportDAO.insertIssue(fileId, methodId, ruleCode01, startLine, "Warning",
                                "Độ phức tạp Cyclomatic của hàm '" + methodName + "' quá cao (" + cyclomatic + " > 10).",
                                "Hàm có quá nhiều nhánh rẽ (if/else/switch/loop), khó kiểm thử và bảo trì. Hãy phân rã thành các hàm nhỏ hơn.");
                        totalIssuesCount++;
                        warningCount++;
                    }

                    // Rule CODE02: Hàm quá dài (God Method > 50 dòng)
                    int methodLines = endLine - startLine + 1;
                    if (methodLines > 50) {
                        reportDAO.insertIssue(fileId, methodId, ruleCode02, startLine, "Info",
                                "Hàm '" + methodName + "' quá dài (" + methodLines + " dòng, vượt quá giới hạn khuyến nghị 50 dòng).",
                                "Hàm quá dài vi phạm nguyên lý Single Responsibility. Nên chia nhỏ thành các hàm có mục tiêu rõ ràng.");
                        totalIssuesCount++;
                        infoCount++;
                    }

                    // Rule NAM01: Chuẩn đặt tên hàm (CamelCase)
                    if (Character.isUpperCase(methodName.charAt(0)) || methodName.contains("_")) {
                        reportDAO.insertIssue(fileId, methodId, ruleNam01, startLine, "Info",
                                "Tên hàm '" + methodName + "' không tuân theo chuẩn camelCase của Java.",
                                "Tên hàm trong Java nên bắt đầu bằng chữ cái thường và viết hoa chữ cái đầu của các từ tiếp theo (vd: calculateTotal).");
                        totalIssuesCount++;
                        infoCount++;
                    }
                }
            }

            // Tính điểm chất lượng mã nguồn (Quality Score): tối đa 100
            int penalty = (criticalCount * 25) + (warningCount * 10) + (infoCount * 5);
            int qualityScore = Math.max(0, 100 - penalty);

            // Ghi báo cáo tổng hợp vào bảng ANALYSIS_REPORTS
            reportDAO.insertAnalysisReport(submissionId, qualityScore, totalIssuesCount);

            // Dọn dẹp thư mục giải nén tạm
            if (tempExtractDir != null && tempExtractDir.exists()) {
                deleteDirectory(tempExtractDir);
            }

            System.out.println("AstAnalyzer: Phân tích AST thành công cho bài nộp #" + submissionId 
                    + " -> Tổng lỗi: " + totalIssuesCount + ", Điểm: " + qualityScore);
            return true;

        } catch (Exception e) {
            System.err.println("AstAnalyzer: Lỗi ngoài dự kiến khi phân tích: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Tính toán số vòng lặp lồng nhau sâu nhất trong hàm
     */
    private int calculateMaxLoopDepth(MethodDeclaration md) {
        int maxDepth = 0;
        List<Statement> allStmts = md.findAll(Statement.class);
        for (Statement stmt : allStmts) {
            if (isLoop(stmt)) {
                int depth = 1;
                Node current = stmt.getParentNode().orElse(null);
                while (current != null && current != md) {
                    if (current instanceof Statement && isLoop((Statement) current)) {
                        depth++;
                    }
                    current = current.getParentNode().orElse(null);
                }
                if (depth > maxDepth) {
                    maxDepth = depth;
                }
            }
        }
        return maxDepth;
    }

    private boolean isLoop(Statement s) {
        return s instanceof ForStmt || s instanceof ForEachStmt || s instanceof WhileStmt || s instanceof DoStmt;
    }

    /**
     * Tính độ phức tạp Cyclomatic
     */
    private int calculateCyclomaticComplexity(MethodDeclaration md) {
        int complexity = 1;
        complexity += md.findAll(IfStmt.class).size();
        complexity += md.findAll(ForStmt.class).size();
        complexity += md.findAll(ForEachStmt.class).size();
        complexity += md.findAll(WhileStmt.class).size();
        complexity += md.findAll(DoStmt.class).size();
        complexity += md.findAll(CatchClause.class).size();
        complexity += md.findAll(SwitchEntry.class).stream()
                .filter(se -> !se.getLabels().isEmpty()).count();
        complexity += md.findAll(ConditionalExpr.class).size();
        return complexity;
    }

    /**
     * Kiểm tra có ghép chuỗi trong vòng lặp hay không
     */
    private boolean hasStringConcatInLoop(MethodDeclaration md) {
        List<AssignExpr> assigns = md.findAll(AssignExpr.class);
        for (AssignExpr a : assigns) {
            if (a.getOperator() == AssignExpr.Operator.PLUS) {
                // Kiểm tra xem phép gán có nằm trong vòng lặp không
                Node parent = a.getParentNode().orElse(null);
                while (parent != null && parent != md) {
                    if (parent instanceof Statement && isLoop((Statement) parent)) {
                        return true;
                    }
                    parent = parent.getParentNode().orElse(null);
                }
            }
        }
        return false;
    }

    /**
     * Kiểm tra nguy cơ SQL Injection bằng việc ghép chuỗi
     */
    private boolean hasSqlInjectionRisk(MethodDeclaration md) {
        List<MethodCallExpr> calls = md.findAll(MethodCallExpr.class);
        for (MethodCallExpr call : calls) {
            String name = call.getNameAsString();
            if ("executeQuery".equals(name) || "executeUpdate".equals(name) || "execute".equals(name)) {
                if (!call.getArguments().isEmpty()) {
                    if (call.getArgument(0) instanceof BinaryExpr) {
                        BinaryExpr be = (BinaryExpr) call.getArgument(0);
                        if (be.getOperator() == BinaryExpr.Operator.PLUS) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Kiểm tra nguy cơ rò rỉ tài nguyên (mở Connection/Statement mà không bọc trong try-with-resources)
     */
    private boolean hasResourceLeakRisk(MethodDeclaration md) {
        String methodBody = md.toString();
        boolean createsResource = methodBody.contains("DriverManager.getConnection")
                || methodBody.contains("createStatement")
                || methodBody.contains("prepareStatement");
        if (createsResource) {
            List<TryStmt> tryStmts = md.findAll(TryStmt.class);
            boolean usesTryWithResources = tryStmts.stream().anyMatch(t -> !t.getResources().isEmpty());
            boolean callsClose = methodBody.contains(".close()");
            return !usesTryWithResources && !callsClose;
        }
        return false;
    }

    /**
     * Giải nén lấy tất cả các file .java từ file zip
     */
    private void extractJavaFilesFromZip(File zipFile, File destDir, List<File> javaFiles) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!entry.isDirectory() && name.toLowerCase().endsWith(".java")) {
                    File outFile = new File(destDir, new File(name).getName());
                    try (java.io.InputStream in = zip.getInputStream(entry);
                         FileOutputStream out = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = in.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                    }
                    javaFiles.add(outFile);
                }
            }
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }

    /**
     * Tự động sinh bản ghi tối ưu hóa thuật toán mẫu khi phát hiện vòng lặp O(N^2)
     */
    private void generateSampleOptimization(int methodId, String originalCode) {
        String optimizedCode = "// [AI Optimization] Sử dụng cấu trúc dữ liệu băm để tối ưu độ phức tạp\n"
                + "Set<Object> visited = new HashSet<>();\n"
                + "for (Object item : collection) {\n"
                + "    if (!visited.add(item)) {\n"
                + "        return true; // Tìm thấy phần tử trùng trong O(1)\n"
                + "    }\n"
                + "}\n"
                + "return false;";

        String explanation = "AI phát hiện phương thức có vòng lặp lồng nhau gây độ phức tạp O(N^2). "
                + "Đề xuất chuyển đổi thuật toán vét cạn sang sử dụng bảng băm (HashSet/HashMap) "
                + "để giảm độ phức tạp thời gian từ O(N^2) xuống O(N).";

        String sql = "INSERT INTO \"AI_OPTIMIZATIONS\" (method_id, original_code, optimized_code, explanation, new_complexity, created_at) "
                   + "VALUES (?, ?, ?, ?, 'O(N)', LOCALTIMESTAMP)";
        try (java.sql.PreparedStatement ps = reportDAO.getConnection().prepareStatement(sql)) {
            ps.setInt(1, methodId);
            ps.setString(2, originalCode);
            ps.setString(3, optimizedCode);
            ps.setString(4, explanation);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }
}

