package com.fpt.aita.test;

import com.fpt.aita.ai.AiAnalysisService;
import com.fpt.aita.ai.AiClient;
import com.fpt.aita.ai.AiConfig;
import com.fpt.aita.ai.AiException;
import com.fpt.aita.ai.AiRequest;
import com.fpt.aita.ai.AiResult;
import com.fpt.aita.ai.AiRunSummary;
import com.fpt.aita.ai.MethodCodeProvider;
import com.fpt.aita.dal.AiOptimizationDAO;
import com.fpt.aita.dal.AiOptimizationDAO.IssueDTO;
import com.fpt.aita.dal.AiOptimizationDAO.MethodWithIssuesDTO;
import com.fpt.aita.model.AiOptimization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Kiểm thử tích hợp cho AiAnalysisService sử dụng Client giả lập (FakeAiClient)
 * và FakeAiOptimizationDAO (chạy hoàn toàn trên bộ nhớ trong, không cần DB thật và không cần API key).
 * Tương thích Java 1.8.
 */
public class AiAnalysisServiceTest {

    /**
     * FakeAiClient: Giả lập phản hồi từ AI Engine
     * Có thể cấu hình số lần ném lỗi 429 trước khi thành công, hoặc lỗi liên tục.
     */
    public static class FakeAiClient implements AiClient {
        private int errorCountBeforeSuccess = 0;
        private boolean alwaysFail = false;
        private int callCount = 0;

        public FakeAiClient(int errorCountBeforeSuccess, boolean alwaysFail) {
            this.errorCountBeforeSuccess = errorCountBeforeSuccess;
            this.alwaysFail = alwaysFail;
        }

        @Override
        public AiResult analyze(AiRequest req) throws AiException {
            callCount++;
            if (alwaysFail) {
                throw new AiException("Fake AI luôn lỗi (HTTP 500)", false, 500);
            }
            if (errorCountBeforeSuccess > 0) {
                errorCountBeforeSuccess--;
                // Lỗi 429 (retryable)
                throw new AiException("Fake AI Rate limit (HTTP 429)", true, 429);
            }
            return AiResult.success(
                    "Tối ưu bằng FakeAiClient cho " + req.getMethodName(),
                    "// Fake optimized code for " + req.getMethodName(),
                    "O(N)"
            );
        }

        public int getCallCount() {
            return callCount;
        }
    }

    /**
     * FakeAiOptimizationDAO: Giả lập DAO lưu trữ in-memory
     * Không đụng vào CSDL thật, hỗ trợ kiểm thử logic service trọn vẹn.
     */
    public static class FakeAiOptimizationDAO extends AiOptimizationDAO {
        private final Map<Integer, List<MethodWithIssuesDTO>> submissionMethods = new HashMap<Integer, List<MethodWithIssuesDTO>>();
        private final Set<Integer> existingOptimizations = new HashSet<Integer>();
        private final Map<String, AiOptimization> codeCache = new HashMap<String, AiOptimization>();
        private final Map<Integer, String> issueExplanations = new HashMap<Integer, String>();

        public void setupMethods(int submissionId, List<MethodWithIssuesDTO> methods) {
            submissionMethods.put(submissionId, methods);
        }

        @Override
        public List<MethodWithIssuesDTO> getMethodsWithIssues(int submissionId) {
            List<MethodWithIssuesDTO> list = submissionMethods.get(submissionId);
            return list != null ? list : Collections.<MethodWithIssuesDTO>emptyList();
        }

        @Override
        public boolean existsForMethod(int methodId) {
            return existingOptimizations.contains(methodId);
        }

        @Override
        public AiOptimization findByOriginalCode(String originalCode) {
            if (originalCode == null) return null;
            return codeCache.get(originalCode.trim());
        }

        @Override
        public boolean insertOptimization(AiOptimization o) {
            if (o == null) return false;
            existingOptimizations.add(o.getMethodId());
            if (o.getOriginalCode() != null && !o.getOriginalCode().trim().isEmpty()) {
                codeCache.put(o.getOriginalCode().trim(), o);
            }
            return true;
        }

        @Override
        public boolean updateIssueAiExplanation(int issueId, String aiExplanation) {
            issueExplanations.put(issueId, aiExplanation);
            return true;
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public void closeConnection() {
            // Không làm gì với in-memory
        }

        public Map<Integer, String> getIssueExplanations() {
            return issueExplanations;
        }
    }

    /**
     * FakeMethodCodeProvider giả lập cung cấp mã nguồn
     */
    public static class FakeMethodCodeProvider implements MethodCodeProvider {
        private final Map<String, String> codeMap = new HashMap<String, String>();

        public void setCode(String fileName, String code) {
            codeMap.put(fileName, code);
        }

        @Override
        public String getCode(int submissionId, String fileName, int startLine, int endLine) {
            String code = codeMap.get(fileName);
            return code != null ? code : "public void " + fileName + "() {}";
        }
    }

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("     BẮT ĐẦU KIỂM THỬ AiAnalysisService (IN-MEMORY) ");
        System.out.println("====================================================");

        int passed = 0;
        int failed = 0;

        // TEST 1: Lỗi liên tục thì ghi nhận failed, KHÔNG ném ngoại lệ ra ngoài
        try {
            System.out.print("[TEST 1] AI lỗi liên tục -> ghi nhận failed và không ném ngoại lệ... ");
            FakeAiOptimizationDAO dao = new FakeAiOptimizationDAO();
            FakeAiClient clientAlwaysFail = new FakeAiClient(0, true);
            FakeMethodCodeProvider provider = new FakeMethodCodeProvider();

            List<MethodWithIssuesDTO> methods = new ArrayList<MethodWithIssuesDTO>();
            MethodWithIssuesDTO m1 = new MethodWithIssuesDTO();
            m1.setMethodId(101);
            m1.setMethodName("methodFail");
            m1.setEstimatedComplexity("O(N^2)");
            m1.setFileName("FailService.java");
            methods.add(m1);

            dao.setupMethods(1, methods);

            AiConfig config = new AiConfig();
            AiAnalysisService service = new AiAnalysisService(clientAlwaysFail, config, provider, dao);

            AiRunSummary summary = service.analyzeSubmission(1);
            if (summary != null && summary.getFailed() == 1 && summary.getAnalyzed() == 0) {
                System.out.println("PASSED (Ghi nhận failed=1, chương trình chạy an toàn không crash)");
                passed++;
            } else {
                System.out.println("FAILED (Summary không khớp: " + summary + ")");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED (Ném ngoại lệ ra ngoài service): " + e.getMessage());
            failed++;
        }

        // TEST 2: Hai method trùng mã nguồn -> lần 2 nhận kết quả từ fromCache
        try {
            System.out.print("[TEST 2] Hai method trùng code -> lần 2 được tính fromCache... ");
            FakeAiOptimizationDAO dao = new FakeAiOptimizationDAO();
            FakeAiClient normalClient = new FakeAiClient(0, false);
            FakeMethodCodeProvider provider = new FakeMethodCodeProvider();

            String sharedCode = "public void duplicateAlgo() { int a = 1; }";
            provider.setCode("FileA.java", sharedCode);
            provider.setCode("FileB.java", sharedCode);

            List<MethodWithIssuesDTO> methods = new ArrayList<MethodWithIssuesDTO>();
            MethodWithIssuesDTO m1 = new MethodWithIssuesDTO();
            m1.setMethodId(201);
            m1.setMethodName("duplicateAlgo");
            m1.setEstimatedComplexity("O(N^2)");
            m1.setFileName("FileA.java");

            MethodWithIssuesDTO m2 = new MethodWithIssuesDTO();
            m2.setMethodId(202);
            m2.setMethodName("duplicateAlgo");
            m2.setEstimatedComplexity("O(N^2)");
            m2.setFileName("FileB.java");

            methods.add(m1);
            methods.add(m2);
            dao.setupMethods(2, methods);

            AiConfig config = new AiConfig();
            AiAnalysisService service = new AiAnalysisService(normalClient, config, provider, dao);

            AiRunSummary summary = service.analyzeSubmission(2);
            if (summary != null && summary.getAnalyzed() == 1 && summary.getFromCache() == 1) {
                System.out.println("PASSED (analyzed=1, fromCache=1, client gọi " + normalClient.getCallCount() + " lần)");
                passed++;
            } else {
                System.out.println("FAILED (Summary không khớp: " + summary + ")");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 3: Vượt quá giới hạn số method (AI_MAX_METHODS_PER_SUBMISSION)
        try {
            System.out.print("[TEST 3] Vượt quá giới hạn số method phân tích mỗi bài... ");
            FakeAiOptimizationDAO dao = new FakeAiOptimizationDAO();
            FakeAiClient client = new FakeAiClient(0, false);
            FakeMethodCodeProvider provider = new FakeMethodCodeProvider();

            // Giả lập 15 method phức tạp O(N^2)
            List<MethodWithIssuesDTO> methods = new ArrayList<MethodWithIssuesDTO>();
            for (int i = 1; i <= 15; i++) {
                MethodWithIssuesDTO m = new MethodWithIssuesDTO();
                m.setMethodId(300 + i);
                m.setMethodName("method" + i);
                m.setEstimatedComplexity("O(N^2)");
                m.setFileName("Service" + i + ".java");
                methods.add(m);
                provider.setCode("Service" + i + ".java", "public void method" + i + "() { /* unique code " + i + " */ }");
            }
            dao.setupMethods(3, methods);

            AiConfig config = new AiConfig(); // Mặc định maxMethods = 10
            AiAnalysisService service = new AiAnalysisService(client, config, provider, dao);

            AiRunSummary summary = service.analyzeSubmission(3);
            if (summary != null && summary.getTotalMethods() == config.getMaxMethodsPerSubmission()
                    && summary.getAnalyzed() == config.getMaxMethodsPerSubmission()) {
                System.out.println("PASSED (Cắt đúng giới hạn tối đa: " + config.getMaxMethodsPerSubmission() + " methods)");
                passed++;
            } else {
                System.out.println("FAILED (Số method phân tích vượt quá giới hạn: " + summary + ")");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 4: AI gặp lỗi 429 tạm thời rồi sau đó thành công ở lần gọi tiếp theo
        try {
            System.out.print("[TEST 4] AI gặp lỗi 429 một lần rồi thành công... ");
            FakeAiOptimizationDAO dao = new FakeAiOptimizationDAO();
            // Ném 429 ở lần gọi đầu tiên, sau đó thành công
            FakeAiClient retryClient = new FakeAiClient(1, false);
            FakeMethodCodeProvider provider = new FakeMethodCodeProvider();

            List<MethodWithIssuesDTO> methods = new ArrayList<MethodWithIssuesDTO>();
            MethodWithIssuesDTO m1 = new MethodWithIssuesDTO();
            m1.setMethodId(401);
            m1.setMethodName("methodRetry1");
            m1.setEstimatedComplexity("O(N^2)");
            m1.setFileName("Retry1.java");
            provider.setCode("Retry1.java", "public void methodRetry1() {}");

            MethodWithIssuesDTO m2 = new MethodWithIssuesDTO();
            m2.setMethodId(402);
            m2.setMethodName("methodRetry2");
            m2.setEstimatedComplexity("O(N^2)");
            m2.setFileName("Retry2.java");
            provider.setCode("Retry2.java", "public void methodRetry2() {}");

            methods.add(m1);
            methods.add(m2);
            dao.setupMethods(4, methods);

            AiConfig config = new AiConfig();
            AiAnalysisService service = new AiAnalysisService(retryClient, config, provider, dao);

            AiRunSummary summary = service.analyzeSubmission(4);
            // m1 bị 429 ném ngoại lệ -> service bắt riêng và failed=1, m2 gọi tiếp và thành công -> analyzed=1
            if (summary != null && summary.getFailed() == 1 && summary.getAnalyzed() == 1) {
                System.out.println("PASSED (Ghi nhận m1 failed=1 do 429, m2 analyzed=1 thành công)");
                passed++;
            } else {
                System.out.println("FAILED: " + summary);
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        System.out.println("====================================================");
        System.out.println("KẾT QUẢ KIỂM THỬ AiAnalysisService: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("====================================================");
    }
}
