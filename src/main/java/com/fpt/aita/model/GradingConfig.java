package com.fpt.aita.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GradingConfig {
    public static final int MAX_DRIVER_BYTES = 64 * 1024;
    private final String driverSource;
    private final String mainClass;
    private final int version;
    private final List<GradingTestCase> tests;
    public GradingConfig(String mainClass, int version, List<GradingTestCase> tests) {
        this(mainClass, version, tests, "");
    }
    public GradingConfig(String mainClass, int version, List<GradingTestCase> tests, String driverSource) {
        if (mainClass == null || mainClass.length() > 200
                || !mainClass.matches("[a-zA-Z_$][a-zA-Z0-9_$]*(\\.[a-zA-Z_$][a-zA-Z0-9_$]*)*")
                || tests == null || tests.isEmpty() || tests.size() > 20 || tests.contains(null)) {
            throw new IllegalArgumentException("Main class phải hợp lệ; mỗi bài cần từ 1 đến 20 test case.");
        }
        String driver = driverSource == null ? "" : driverSource;
        if (driver.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_DRIVER_BYTES) {
            throw new IllegalArgumentException("Driver tối đa 64 KiB.");
        }
        if (!driver.trim().isEmpty()) {
            try {
                com.github.javaparser.ast.CompilationUnit cu = com.github.javaparser.StaticJavaParser.parse(driver);
                String packageName = cu.getPackageDeclaration().map(p -> p.getNameAsString() + ".").orElse("");
                boolean entry = cu.getTypes().stream().anyMatch(t -> (packageName + t.getNameAsString()).equals(mainClass)
                        && t.isClassOrInterfaceDeclaration() && !t.asClassOrInterfaceDeclaration().isInterface()
                        && t.getMethodsByName("main").stream().anyMatch(m -> m.isPublic() && m.isStatic()
                            && m.getType().isVoidType() && m.getParameters().size() == 1
                            && ("String[]".equals(m.getParameter(0).getType().asString())
                                || "java.lang.String[]".equals(m.getParameter(0).getType().asString())
                                || (m.getParameter(0).isVarArgs() && ("String".equals(m.getParameter(0).getType().asString())
                                    || "java.lang.String".equals(m.getParameter(0).getType().asString()))))));
                if (!entry) throw new IllegalArgumentException("Driver phải khai báo main class với public static void main(String[] args).");
            } catch (com.github.javaparser.ParseProblemException ex) {
                throw new IllegalArgumentException("Cú pháp driver Java không hợp lệ.");
            }
        }
        this.driverSource = driver;
        this.mainClass = mainClass; this.version = version;
        this.tests = Collections.unmodifiableList(new ArrayList<>(tests));
    }
    public String getDriverSource() { return driverSource; }
    public boolean hasDriver() { return !driverSource.trim().isEmpty(); }
    public String getMainClass() { return mainClass; }
    public int getVersion() { return version; }
    public List<GradingTestCase> getTests() { return tests; }
}
