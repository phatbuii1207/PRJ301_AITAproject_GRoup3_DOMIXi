# Hướng dẫn tích hợp module AI

Module AI Java 8 nằm trong `src/main/java/com/fpt/aita/ai/`; model và DAO tương ứng là `com.fpt.aita.model.AiOptimization` và `com.fpt.aita.dal.AiOptimizationDAO`.

## Cấu hình client

`AiConfig.fromEnv()` đọc các giá trị từ environment variables, sau đó fallback sang Java system properties:

- `AI_PROVIDER`: `gemini` hoặc `mock`.
- `GEMINI_API_KEY`, `GEMINI_MODEL`: cấu hình bí mật và model cho Gemini. Đặt API key ngoài mã nguồn/repository; không đưa giá trị key vào tài liệu hoặc log.
- `AI_TIMEOUT_MS`, `AI_MAX_RETRIES`, `AI_MAX_METHODS_PER_SUBMISSION`.
- `AI_EXTRACTED_ROOT`: thư mục gốc chứa source đã giải nén.

`AiClientFactory.create(config)` chọn `GeminiClient` khi provider/model/key hợp lệ; nếu thiếu cấu hình thì trả `MockAiClient`. `AiAnalysisService.main()` dùng factory này, nhận `submissionId` từ `args[0]` và mặc định là `1`. Entry point này cần CSDL để lấy methods/issues.

## Quy ước file đã giải nén

`FileMethodCodeProvider` tìm file dưới:

```text
{AI_EXTRACTED_ROOT}/{submissionId}/
```

Thư mục con giữ nguyên cấu trúc bên trong bài nộp; `submissionId` là khóa số trong bảng `SUBMISSIONS`, không phải hash hoặc tên upload. `SOURCE_FILES.file_name` nên lưu đường dẫn tương đối trong submission (ví dụ `src/main/java/Example.java`) nếu có khả năng trùng tên file. Provider chuẩn hóa đường dẫn, từ chối đường dẫn tuyệt đối và thành phần `..`, đồng thời xác nhận file không thoát khỏi submission root.

Trong code upload hiện tại, `CourseDetailServlet` lưu file trong `/uploads/submissions` tên dạng `timestamp_username_tên-file`, không giải nén. Chưa tìm thấy ZipExtractor/worker trong source hiện tại. Người phụ trách upload/worker cần giải nén sau khi có submission ID và trước khi gọi AST/AI; không dùng trực tiếp tên tệp upload làm thư mục bài nộp.

## Cách gọi từ worker của Người 3

Thứ tự: giải nén vào thư mục theo submission ID → AST Engine hoàn tất việc ghi `SOURCE_FILES`, `METHODS`, `ISSUES` → gọi `analyzeSubmission(submissionId)`. Mẫu tích hợp:

```java
package com.fpt.aita.worker;

import com.fpt.aita.ai.AiAnalysisService;
import com.fpt.aita.ai.AiRunSummary;

public class SubmissionProcessor {
    private final AiAnalysisService aiService = new AiAnalysisService();

    public void processSubmission(int submissionId) {
        // Chỉ gọi sau khi giải nén và AST đã ghi dữ liệu xong.
        try {
            AiRunSummary summary = aiService.analyzeSubmission(submissionId);
            System.out.println("[Worker] AI submissionId=" + submissionId + ": " + summary);
        } catch (RuntimeException e) {
            System.err.println("[Worker] AI analysis failed: " + e.getMessage());
        }
    }
}
```

Đây là hợp đồng/mẫu gọi, không phải worker đang có trong repository. Hiện `analyzeSubmission()` chỉ được gọi từ `AiAnalysisService.main()`; chưa có lời gọi từ servlet hoặc queue worker. AI lỗi được tính vào `AiRunSummary.failed`, không nên làm AST/chấm bài thất bại.

`AiRunSummary` cung cấp `totalMethods`, `analyzed`, `fromCache`, `skipped`, `failed`. Service chỉ xét method có issue Critical/Warning hoặc complexity từ `O(N^2)` trở lên và giới hạn số method theo `AI_MAX_METHODS_PER_SUBMISSION`.

## Dữ liệu AI và báo cáo

`AiOptimizationDAO.insertOptimization()` ghi các cột hiện có trong `AI_OPTIMIZATIONS`: `method_id`, `original_code`, `optimized_code`, `explanation`, `created_at`. DAO không ghi `method_name`, `file_name` hoặc complexity vì các cột này không tồn tại trong bảng đó.

`AnalysisReportDAO.getOptimizations()` lấy:

- `methodName` từ `METHODS.method_name`.
- `fileName` từ `SOURCE_FILES.file_name`.
- `complexityBefore` từ `METHODS.estimated_complexity`.

Các giá trị trên được JOIN qua `AI_OPTIMIZATIONS.method_id -> METHODS.method_id -> SOURCE_FILES.file_id`, tiếp đến `SOURCE_FILES.submission_id -> ANALYSIS_REPORTS.submission_id`. Schema không có cột `complexity_after`; query report hiện không gán `complexityAfter`. JSP hiện dùng mặc định `O(N)` khi thiếu giá trị, không phải complexity AI đã lưu. Muốn hiển thị chính xác `AiResult.newComplexity`, cần thống nhất schema migration cùng người phụ trách report rồi cập nhật DAO/view.

Khi hiển thị `originalCode`, `optimizedCode` hoặc nội dung giải thích, JSP phải escape output (ví dụ JSTL `<c:out>`) để tránh XSS.

## Retry Gemini

`GeminiClient` retry HTTP 429 và 5xx; các status 400/401/403 không retry. Backoff hiện là tuyến tính theo số lượt: 1 giây, 2 giây, ... Các helper phân loại/backoff được kiểm thử không gọi mạng trong `src/test/java/com/fpt/aita/ai/GeminiClientRetryTest.java`.

## Trạng thái điểm nối cần phối hợp

- Chưa tìm thấy code insert vào `METHODS` hoặc `ISSUES`; `AiOptimizationDAO` chỉ đọc hai bảng này và cập nhật `ISSUES.ai_explanation`.
- Chưa tìm thấy hàng đợi/worker hoặc code giải nén.
- `StudentAssignmentServlet` chỉ đọc báo cáo/trạng thái; `AnalysisReportServlet` chỉ tải report/issues/optimizations để hiển thị.
- Cần Người 1 chốt dữ liệu AST; Người 3 chốt giải nén và thời điểm gọi service; Người 5 chốt cách lưu complexity sau khi schema được thống nhất.
