# Giải thích module AI — tài liệu ôn bảo vệ của Người 2

Tài liệu này giải thích module tích hợp Gemini/Mock trong dự án PRJ301 AITA. Mục tiêu là giúp trình bày được luồng xử lý, vai trò từng lớp, các quyết định thiết kế và giới hạn hiện tại. Code dùng Java 8.

## 1. Sơ đồ luồng tổng thể

```text
Worker (sau khi AST đã ghi dữ liệu) hoặc chạy AiAnalysisService.main(args)
  |
  | analyzeSubmission(submissionId)
  v
AiOptimizationDAO.getMethodsWithIssues(submissionId)
  |-- METHODS + SOURCE_FILES: method_id, method_name, file_name,
  |   start_line, end_line, estimated_complexity
  `-- ISSUES + RULES: issue_id, rule_code, severity, message, line_number
  |
  v
AiAnalysisService lọc method có Critical/Warning hoặc complexity cao,
ưu tiên lỗi nghiêm trọng, giới hạn số method theo cấu hình
  |
  +--> bỏ qua nếu method đã có AI_OPTIMIZATIONS
  |
  v
MethodCodeProvider.getCode(...)
  `-- FileMethodCodeProvider đọc đoạn dòng trong
      {AI_EXTRACTED_ROOT}/{submissionId}/...
  |
  v
PromptBuilder
  |-- buildSystemInstruction(): vai trò, quy tắc và JSON schema
  `-- buildUserPrompt(AiRequest): vi phạm + code được bọc trong
      <<<STUDENT_CODE_START>>> ... <<<STUDENT_CODE_END>>>
  |
  v
AiClient.analyze(AiRequest)
  |-- GeminiClient: HTTP POST tới Gemini REST API
  `-- MockAiClient: phản hồi giả lập/offline
  |
  v
GeminiClient lấy text từ response; AiResponseParser.parse(...)
kiểm tra JSON, các trường explanation / optimized_code / new_complexity
  |
  v
AiAnalysisService
  |-- AiOptimizationDAO.insertOptimization(...)
  |     -> AI_OPTIMIZATIONS(method_id, original_code, optimized_code,
  |                         explanation, created_at)
  `-- AiOptimizationDAO.updateIssueAiExplanation(...)
        -> ISSUES.ai_explanation cho từng issue của method
  |
  v
AiRunSummary: totalMethods, analyzed, fromCache, skipped, failed
```

### Cấu hình và API key

`AiConfig` đọc các tên cấu hình sau từ `System.getenv()`; nếu không có hoặc rỗng thì đọc `System.getProperty()`:

| Tên | Tác dụng | Mặc định |
|---|---|---|
| `GEMINI_API_KEY` | Bí mật xác thực Gemini | Rỗng |
| `GEMINI_MODEL` | Tên model Gemini | Rỗng |
| `AI_PROVIDER` | Chọn `gemini` hoặc `mock` | `mock` |
| `AI_TIMEOUT_MS` | Timeout kết nối/đọc | `20000` |
| `AI_MAX_RETRIES` | Số lần retry tối đa ngoài lần gọi đầu | `2` |
| `AI_MAX_METHODS_PER_SUBMISSION` | Số method tối đa mỗi bài | `10` |
| `AI_EXTRACTED_ROOT` | Thư mục gốc source đã giải nén | Rỗng |

Key không hardcode trong source vì source có thể được commit/chia sẻ; key được cung cấp ở môi trường chạy. `GeminiClient` gửi key qua HTTP header `x-goog-api-key`, không ghép key vào URL hoặc prompt. `AiConfig.toString()` che bớt key, nhưng khi bảo vệ vẫn không nên log toàn bộ cấu hình hoặc hiển thị bất kỳ phần nào của key.

`AiClientFactory.create(config)` chọn `GeminiClient` khi `AI_PROVIDER=gemini` và key/model hợp lệ; nếu không, chọn `MockAiClient`. Chú ý: phần JavaDoc của `AiClientFactory` còn câu mô tả cũ “sau này sẽ trả GeminiClient”; logic hiện tại đã trả `GeminiClient`.

## 2. File, vai trò và public API

Mô tả dưới đây tập trung vào các public method/constructor trong source hiện tại. Private helper không phải API gọi từ lớp khác.

### 2.1 Các file trong `com.fpt.aita.ai`

#### `AiClient.java`

Interface chung để service không phụ thuộc trực tiếp vào nhà cung cấp AI. Nhờ vậy có thể thay Gemini bằng mock hoặc implementation khác.

| Public method | Tham số | Trả về / tác dụng |
|---|---|---|
| `analyze(AiRequest req)` | Yêu cầu phân tích | Trả `AiResult`; có thể ném `AiException`. |

#### `AiClientFactory.java`

Tạo client theo `AiConfig`; giữ quyết định chọn Gemini/mock ở một nơi thay vì rải `if` khắp service.

| Public method | Tham số | Trả về / tác dụng |
|---|---|---|
| `create(AiConfig config)` | Cấu hình AI | `GeminiClient` khi cấu hình Gemini hợp lệ, ngược lại `MockAiClient`. |

Constructor của factory là private để không tạo instance vô ích.

#### `AiConfig.java`

Đọc cấu hình AI từ environment/system properties và cung cấp giá trị có mặc định. Key là cấu hình môi trường, không nằm trong source.

| Public method/constructor | Tham số | Trả về / tác dụng |
|---|---|---|
| `AiConfig()` | Không | Tạo cấu hình từ environment rồi system properties. |
| `fromEnv()` | Không | Factory method tạo cấu hình bằng cơ chế đọc trên. |
| `isConfigured()` | Không | `true` nếu provider là Gemini và có key/model. |
| `getGeminiApiKey()` | Không | Trả key cho Gemini client; không nên log. |
| `getGeminiModel()` | Không | Trả tên model. |
| `getAiProvider()` | Không | Trả provider được cấu hình. |
| `getTimeoutMs()` | Không | Trả timeout millisecond. |
| `getMaxRetries()` | Không | Trả số lần retry tối đa. |
| `getMaxMethodsPerSubmission()` | Không | Trả giới hạn method mỗi submission. |
| `getExtractedRoot()` | Không | Trả thư mục gốc source đã giải nén. |
| `toString()` | Không | Trả thông tin cấu hình với key bị che bớt; vẫn không nên in log. |

#### `AiException.java`

Ngoại lệ riêng của module, mang thêm cờ `retryable` và HTTP `statusCode` để client biết lỗi nào đáng thử lại.

| Public constructor/method | Tham số | Trả về / tác dụng |
|---|---|---|
| `AiException(String message)` | Thông báo | Tạo lỗi không retry, status 0. |
| `AiException(String message, Throwable cause)` | Thông báo, nguyên nhân | Tạo lỗi không retry có cause. |
| `AiException(String message, boolean retryable)` | Thông báo, có retry không | Tạo lỗi với status 0. |
| `AiException(String message, boolean retryable, int statusCode)` | Thông báo, retryable, HTTP status | Tạo lỗi HTTP có phân loại retry. |
| `AiException(String message, Throwable cause, boolean retryable, int statusCode)` | Thông báo, cause, retryable, status | Tạo lỗi đầy đủ thông tin. |
| `isRetryable()` | Không | Trả lỗi có được phép thử lại không. |
| `getStatusCode()` | Không | Trả HTTP status, hoặc 0 nếu không có status HTTP. |

#### `AiRequest.java`

DTO đầu vào AI, đóng gói tên method, đoạn code, complexity hiện tại và danh sách vi phạm. Dùng `Serializable` để dễ truyền/lưu trong môi trường Java.

| Public method/constructor | Tham số | Trả về / tác dụng |
|---|---|---|
| `AiRequest()` | Không | Tạo request với danh sách vi phạm rỗng. |
| `AiRequest(String methodName, String code, String estimatedComplexity, List<RuleViolation> violations)` | Metadata, code và violations | Tạo request; sao chép danh sách đầu vào. |
| `getMethodName()` / `setMethodName(String)` | setter nhận tên method | Đọc/cập nhật tên method. |
| `getCode()` / `setCode(String)` | setter nhận code | Đọc/cập nhật đoạn source. |
| `getEstimatedComplexity()` / `setEstimatedComplexity(String)` | setter nhận complexity | Đọc/cập nhật Big-O hiện tại. |
| `getViolations()` / `setViolations(List<RuleViolation>)` | setter nhận list | Đọc/cập nhật danh sách lỗi; setter tạo list mới khi gán. |
| `addViolation(RuleViolation)` | Một vi phạm | Thêm vi phạm khác null. |
| `toString()` | Không | Tóm tắt tên/complexity/số vi phạm, không in code. |

`AiRequest.RuleViolation` là DTO lồng nhau:

| Public method/constructor | Tham số | Trả về / tác dụng |
|---|---|---|
| `RuleViolation()` | Không | Constructor rỗng. |
| `RuleViolation(String ruleCode, String severity, String message, int lineNumber)` | Mã rule, mức độ, thông điệp, số dòng | Tạo một lỗi. |
| `getRuleCode()` / `setRuleCode(String)` | Setter nhận mã rule | Đọc/cập nhật mã rule. |
| `getSeverity()` / `setSeverity(String)` | Setter nhận mức độ | Đọc/cập nhật mức độ. |
| `getMessage()` / `setMessage(String)` | Setter nhận thông điệp | Đọc/cập nhật nội dung vi phạm. |
| `getLineNumber()` / `setLineNumber(int)` | Setter nhận dòng | Đọc/cập nhật vị trí vi phạm. |
| `toString()` | Không | Trả biểu diễn thông tin vi phạm. |

#### `AiResult.java`

DTO kết quả chuẩn hóa, không phụ thuộc JSON cụ thể của Gemini. Chứa thành công/thất bại, lời giải thích, code mới, Big-O mới hoặc lỗi.

| Public method/constructor | Tham số | Trả về / tác dụng |
|---|---|---|
| `AiResult()` | Không | Constructor rỗng. |
| `AiResult(boolean success, String explanation, String optimizedCode, String newComplexity, String errorMessage)` | Toàn bộ trường kết quả | Tạo kết quả đầy đủ. |
| `success(String explanation, String optimizedCode, String newComplexity)` | Ba trường thành công | Factory method cho kết quả thành công. |
| `failure(String errorMessage)` | Lỗi | Factory method cho kết quả thất bại. |
| `isSuccess()` / `setSuccess(boolean)` | setter nhận boolean | Đọc/cập nhật trạng thái. |
| `getExplanation()` / `setExplanation(String)` | setter nhận text | Đọc/cập nhật giải thích. |
| `getOptimizedCode()` / `setOptimizedCode(String)` | setter nhận code | Đọc/cập nhật code tối ưu. |
| `getNewComplexity()` / `setNewComplexity(String)` | setter nhận Big-O mới | Đọc/cập nhật complexity mới. |
| `getErrorMessage()` / `setErrorMessage(String)` | setter nhận lỗi | Đọc/cập nhật thông báo lỗi. |
| `toString()` | Không | Tóm tắt kết quả, cắt ngắn explanation dài. |

#### `AiResponseParser.java`

Chuyển text JSON do model trả về thành `AiResult`; kiểm tra đủ trường, không để JSON lỗi/thiếu field thành kết quả thành công giả.

| Public method | Tham số | Trả về / tác dụng |
|---|---|---|
| `parse(String raw)` | Text model trả về | Trả `AiResult`; ném `AiException` nếu rỗng, JSON sai hoặc thiếu field cần thiết. |
| `cleanMarkdownFence(String text)` | Text | Loại bỏ rào ```json/``` nếu model bao JSON bằng Markdown. |
| `main(String[] args)` | Tham số dòng lệnh | Chạy ví dụ parser: JSON chuẩn, có Markdown, thiếu field, JSON lỗi. |

Các field bắt buộc là `explanation`, `optimized_code`, `new_complexity`; explanation bị giới hạn chiều dài.

#### `AiRunSummary.java`

Tổng hợp số method được xử lý; giúp worker/log biết kết quả từng submission thay vì chỉ nhận một cờ thành công chung.

| Public method | Tham số | Trả về / tác dụng |
|---|---|---|
| `AiRunSummary()` | Không | Tạo bộ đếm ban đầu bằng 0. |
| `getTotalMethods()` / `setTotalMethods(int)` | setter nhận số method | Đọc/cập nhật số method đủ điều kiện được chọn. |
| `getAnalyzed()` / `incrementAnalyzed()` | Không | Đọc/tăng số phân tích AI thành công. |
| `getFromCache()` / `incrementFromCache()` | Không | Đọc/tăng số kết quả lấy từ cache. |
| `getSkipped()` / `incrementSkipped()` | Không | Đọc/tăng số method đã có tối ưu nên bỏ qua. |
| `getFailed()` / `incrementFailed()` | Không | Đọc/tăng số method thất bại. |
| `toString()` | Không | In gọn các bộ đếm. |

#### `MethodCodeProvider.java`

Abstraction để service không phụ thuộc source nằm trên filesystem; có thể thay bằng provider từ DB hoặc bộ nhớ.

| Public method | Tham số | Trả về / tác dụng |
|---|---|---|
| `getCode(int submissionId, String fileName, int startLine, int endLine)` | Bài nộp, file, khoảng dòng 1-indexed | Trả đoạn code; provider cụ thể quyết định cách lấy. |

#### `FileMethodCodeProvider.java`

Đọc code từ `{AI_EXTRACTED_ROOT}/{submissionId}/`, theo khoảng dòng trong DB. Kiểm tra path trước khi mở file và chỉ cho phép file nằm trong root bài nộp.

| Public method/constructor | Tham số | Trả về / tác dụng |
|---|---|---|
| `FileMethodCodeProvider(String extractedRoot)` | Thư mục gốc đã giải nén | Tạo provider; từ chối root null/rỗng. |
| `getCode(int submissionId, String fileName, int startLine, int endLine)` | Bài nộp, path tương đối, dòng đầu/cuối | Trả đoạn code; trả chuỗi rỗng khi input/path/file không hợp lệ hoặc không đọc được. |

#### `PromptBuilder.java`

Tập trung tạo system instruction và user prompt để mọi client gửi cùng hợp đồng. Code người học được coi là dữ liệu không tin cậy, không phải chỉ dẫn.

| Public method | Tham số | Trả về / tác dụng |
|---|---|---|
| `buildSystemInstruction()` | Không | Trả chỉ dẫn vai trò, quy tắc giữ hành vi và schema JSON. |
| `buildUserPrompt(AiRequest req)` | Request | Trả prompt chứa metadata, violations và code có delimiters; code dài bị cắt ở 4000 ký tự. |

#### `GeminiClient.java`

Implementation gọi Gemini REST API bằng `HttpURLConnection`, tương thích Java 8. Gửi API key trong header, parse cấu trúc response Gemini rồi giao phần JSON model cho `AiResponseParser`.

| Public method/constructor | Tham số | Trả về / tác dụng |
|---|---|---|
| `GeminiClient(AiConfig config)` | Cấu hình | Tạo client; từ chối config null. |
| `analyze(AiRequest req)` | Request | Gọi Gemini, trả `AiResult`; lỗi được biểu diễn bằng `AiException`. |
| `main(String[] args)` | Tham số dòng lệnh | Demo client có thể tạo request thật. Không dùng làm test offline; khi environment có cấu hình key thật, main có nhánh gọi API thật. |

Retry hiện áp dụng cho HTTP 429 và 5xx; backoff tuyến tính: attempt 1 chờ 1 giây, attempt 2 chờ 2 giây. HTTP 400/401/403 không retry vì request/auth thường sai và gửi lại không giải quyết được nguyên nhân. Timeout được đánh dấu retryable.

#### `MockAiClient.java`

Client giả lập kết quả theo rule code phổ biến như performance/security/resource; hỗ trợ chạy offline và test logic service mà không gọi API/quota.

| Public method | Tham số | Trả về / tác dụng |
|---|---|---|
| `analyze(AiRequest req)` | Request | Trả kết quả giả lập theo rule; request null trả failure. |
| `main(String[] args)` | Tham số dòng lệnh | Chạy ví dụ mock với một request. |

#### `AiAnalysisService.java`

Điều phối toàn bộ luồng mỗi submission: lấy methods/issues, lọc và xếp thứ tự, lấy code, cache, gọi client, ghi kết quả và tổng hợp lỗi. Mỗi method được xử lý riêng để một lỗi không dừng cả submission.

| Public method/constructor | Tham số | Trả về / tác dụng |
|---|---|---|
| `AiAnalysisService()` | Không | Tạo config/client/DAO và provider theo `AI_EXTRACTED_ROOT`. |
| `AiAnalysisService(AiClient, AiConfig, MethodCodeProvider, AiOptimizationDAO)` | Các dependency | Constructor injection cho test hoặc cấu hình tùy biến. |
| `analyzeSubmission(int submissionId)` | ID bài nộp | Phân tích các method đủ điều kiện; trả `AiRunSummary`. |
| `main(String[] args)` | `args[0]` là ID tùy chọn | Tạo config qua `AiConfig.fromEnv()`, client qua factory, mặc định ID 1, chạy và in summary. Cần DB có dữ liệu. |

Service ưu tiên method có Critical, sau đó Warning/complexity cao; chỉ chọn tối đa `AI_MAX_METHODS_PER_SUBMISSION`. Nếu method đã có optimization thì `skipped`; nếu trùng `original_code` thì dùng cache; lỗi riêng method tăng `failed`.

### 2.2 `AiOptimization.java`

Model Java Serializable cho kết quả `AI_OPTIMIZATIONS` và metadata dùng trên UI. Có `optimizationId`, `methodId`, `methodName`, `fileName`, `originalCode`, `optimizedCode`, `explanation`, `createdAt`, `complexityBefore`, `complexityAfter`.

| Public method/constructor | Tham số | Trả về / tác dụng |
|---|---|---|
| `AiOptimization()` | Không | Constructor rỗng cho JDBC/bean mapping. |
| `AiOptimization(int methodId, String originalCode, String optimizedCode, String explanation)` | Bốn trường kết quả | Constructor tiện dùng khi service tạo optimization. |
| `AiOptimization(int optimizationId, int methodId, String originalCode, String optimizedCode, String explanation, Timestamp createdAt)` | Trường DB của phần AI | Constructor ánh xạ bản ghi AI. |
| `AiOptimization(int optimizationId, int methodId, String methodName, String fileName, String originalCode, String optimizedCode, String explanation, Timestamp createdAt)` | Các trường AI + metadata UI | Constructor dùng bởi report DAO. |
| `getOptimizationId()` / `setOptimizationId(int)` | setter nhận ID | Đọc/cập nhật ID optimization. |
| `getMethodId()` / `setMethodId(int)` | setter nhận ID | Đọc/cập nhật method liên kết. |
| `getMethodName()` / `setMethodName(String)` | setter nhận tên | Đọc/cập nhật tên method. |
| `getFileName()` / `setFileName(String)` | setter nhận file | Đọc/cập nhật tên/path file. |
| `getOriginalCode()` / `setOriginalCode(String)` | setter nhận code | Đọc/cập nhật code ban đầu. |
| `getOptimizedCode()` / `setOptimizedCode(String)` | setter nhận code | Đọc/cập nhật code gợi ý. |
| `getExplanation()` / `setExplanation(String)` | setter nhận text | Đọc/cập nhật giải thích. |
| `getCreatedAt()` / `setCreatedAt(Timestamp)` | setter nhận timestamp | Đọc/cập nhật thời điểm tạo. |
| `getComplexityBefore()` / `setComplexityBefore(String)` | setter nhận Big-O | Đọc/cập nhật complexity trước. |
| `getComplexityAfter()` / `setComplexityAfter(String)` | setter nhận Big-O | Đọc/cập nhật complexity sau. |
| `toString()` | Không | Tóm tắt ID và thời điểm tạo. |

**Lưu ý bảo vệ:** hai field complexity là metadata trên model, không đồng nghĩa đã có cột DB tương ứng. `AnalysisReportDAO` hiện lấy `complexityBefore` từ `METHODS.estimated_complexity`, không có cột/schema `complexity_after` để lưu `complexityAfter`.

### 2.3 `AiOptimizationDAO.java`

DAO kết nối DB qua `DBContext`; đọc methods/issues, kiểm tra cache, ghi optimization và cập nhật giải thích issue. Bảng AI hiện ghi `method_id`, `original_code`, `optimized_code`, `explanation`, `created_at`; các thông tin method/file được JOIN từ bảng AST.

#### Public API của DAO

| Public method | Tham số | Trả về / tác dụng |
|---|---|---|
| `isConnected()` | Không | Cho biết connection khác null. |
| `closeConnection()` | Không | Đóng kết nối nếu còn mở. |
| `insertOptimization(AiOptimization o)` | Optimization | Chèn bản ghi AI; trả boolean thành công. |
| `getByMethodId(int methodId)` | ID method | Trả optimization mới nhất hoặc null. |
| `getBySubmissionId(int submissionId)` | ID bài nộp | Trả danh sách optimization JOIN qua METHODS và SOURCE_FILES. |
| `findByOriginalCode(String code)` | Code gốc | Tìm bản ghi mới nhất có code giống để cache; null nếu không tìm thấy. |
| `updateIssueAiExplanation(int issueId, String text)` | ID issue, giải thích | Cập nhật `ISSUES.ai_explanation`; trả boolean. |
| `getMethodsWithIssues(int submissionId)` | ID bài nộp | Trả method DTO đã gắn các issue/rule liên quan. |
| `existsForMethod(int methodId)` | ID method | Kiểm tra đã có optimization cho method. |
| `main(String[] args)` | Tham số không dùng | Demo DB có thao tác INSERT thử; **không chạy tùy tiện trên DB dùng chung**. |

#### DTO lồng nhau

`MethodWithIssuesDTO` chứa `methodId`, `fileId`, `fileName`, `methodName`, `startLine`, `endLine`, `estimatedComplexity`, `issues`.

| Public method | Tác dụng |
|---|---|
| `MethodWithIssuesDTO()` | Tạo DTO; issues ban đầu là list rỗng. |
| `getMethodId()` / `setMethodId(int)` | Đọc/gán method ID. |
| `getFileId()` / `setFileId(int)` | Đọc/gán file ID. |
| `getFileName()` / `setFileName(String)` | Đọc/gán tên/path file. |
| `getMethodName()` / `setMethodName(String)` | Đọc/gán tên method. |
| `getStartLine()` / `setStartLine(int)` | Đọc/gán dòng bắt đầu. |
| `getEndLine()` / `setEndLine(int)` | Đọc/gán dòng kết thúc. |
| `getEstimatedComplexity()` / `setEstimatedComplexity(String)` | Đọc/gán Big-O hiện tại. |
| `getIssues()` / `setIssues(List<IssueDTO>)` | Đọc/gán các issue; null setter thành list rỗng. |
| `addIssue(IssueDTO)` | Thêm issue khác null. |
| `toString()` | Tóm tắt thông tin DTO. |

`IssueDTO` chứa `issueId`, `ruleCode`, `severity`, `message`, `lineNumber`.

| Public method | Tác dụng |
|---|---|
| `IssueDTO()` | Constructor rỗng. |
| `IssueDTO(int issueId, String ruleCode, String severity, String message, int lineNumber)` | Tạo DTO từ thông tin issue. |
| `getIssueId()` / `setIssueId(int)` | Đọc/gán issue ID. |
| `getRuleCode()` / `setRuleCode(String)` | Đọc/gán mã rule. |
| `getSeverity()` / `setSeverity(String)` | Đọc/gán mức nghiêm trọng. |
| `getMessage()` / `setMessage(String)` | Đọc/gán thông điệp. |
| `getLineNumber()` / `setLineNumber(int)` | Đọc/gán dòng lỗi. |
| `toString()` | Tóm tắt rule, severity và line. |

### 2.4 Các file test

Các test module AI hiện là chương trình `main()` tự kiểm tra, không phải JUnit. Vì vậy Maven Surefire có thể báo `Tests run: 0`; muốn xem assertion phải chạy main trực tiếp.

| File | Mục đích | Public method |
|---|---|---|
| `src/test/java/com/fpt/aita/test/AiResponseParserTest.java` | Kiểm tra JSON hợp lệ, Markdown fence, thiếu trường và JSON lỗi. | `main(String[] args)`: chạy bốn trường hợp, in số pass/fail. |
| `src/test/java/com/fpt/aita/test/PromptBuilderTest.java` | Kiểm tra delimiters chống prompt injection, cắt code dài và nội dung prompt. | `main(String[] args)`: chạy ba nhóm assertion. |
| `src/test/java/com/fpt/aita/test/AiAnalysisServiceTest.java` | Kiểm tra service offline bằng client/DAO/provider giả: lỗi, cache, giới hạn method, lỗi 429. | `main(String[] args)`: chạy bốn case. |
| `src/test/java/com/fpt/aita/test/GeminiClientRetryTest.java` | Kiểm tra status retry và backoff, không gọi HTTP. | `main(String[] args)`: kiểm tra 429/5xx, status không retry và thời gian chờ. |
| `src/test/java/com/fpt/aita/test/FileMethodCodeProviderTest.java` | Kiểm tra đọc source hợp lệ và từ chối path traversal/absolute path. | `main(String[] args) throws Exception`: tạo fixture tạm, chạy ba assertion rồi dọn fixture. |

Các public helper bên trong `AiAnalysisServiceTest`:

| Public method/constructor | Tác dụng |
|---|---|
| `FakeAiClient(int errorCountBeforeSuccess, boolean alwaysFail)` | Cấu hình số lỗi retry giả lập và chế độ luôn lỗi. |
| `FakeAiClient.analyze(AiRequest)` | Đếm lần gọi, giả lỗi hoặc trả kết quả giả. |
| `FakeAiClient.getCallCount()` | Cho test kiểm tra số lần gọi client. |
| `FakeAiOptimizationDAO.setupMethods(int, List<MethodWithIssuesDTO>)` | Nạp methods giả theo submission ID. |
| `FakeAiOptimizationDAO.getMethodsWithIssues(int)` | Trả methods giả. |
| `FakeAiOptimizationDAO.existsForMethod(int)` | Kiểm tra set optimization trong bộ nhớ. |
| `FakeAiOptimizationDAO.findByOriginalCode(String)` | Tra cache giả theo code. |
| `FakeAiOptimizationDAO.insertOptimization(AiOptimization)` | Ghi optimization vào cấu trúc in-memory. |
| `FakeAiOptimizationDAO.updateIssueAiExplanation(int, String)` | Lưu explanation issue giả trong map. |
| `FakeAiOptimizationDAO.isConnected()` | Trả true cho service test; constructor cha vẫn có thể log thiếu JDBC driver nếu driver không nằm trong classpath. |
| `FakeAiOptimizationDAO.closeConnection()` | Override để không đụng connection DB trong test. |
| `FakeAiOptimizationDAO.getIssueExplanations()` | Trả map explanation để assertion. |
| `FakeMethodCodeProvider.setCode(String, String)` | Gán code giả theo file name. |
| `FakeMethodCodeProvider.getCode(int, String, int, int)` | Lấy code giả theo file name. |

## 3. Thay đổi so với `origin/main`

Đối chiếu được thực hiện bằng `git diff origin/main --stat`, `git diff origin/main --name-status` và lịch sử `git log`. Snapshot thống kê trước khi tạo file giải thích này là **29 file, 3250 insertions, 5954 deletions**; số xóa chủ yếu do `repomix-output.xml` bị xóa. File giải thích này hiện được tạo mới và chưa nằm trong stat Git.

### Nhóm A — File AI mới so với `origin/main`

Commit `87a91c1` (`feat(ai): module tich hop AI (Gemini) cho Nguoi 2`) thêm:

- `src/main/java/com/fpt/aita/ai/AiAnalysisService.java`
- `src/main/java/com/fpt/aita/ai/AiClient.java`
- `src/main/java/com/fpt/aita/ai/AiClientFactory.java`
- `src/main/java/com/fpt/aita/ai/AiConfig.java`
- `src/main/java/com/fpt/aita/ai/AiException.java`
- `src/main/java/com/fpt/aita/ai/AiRequest.java`
- `src/main/java/com/fpt/aita/ai/AiResponseParser.java`
- `src/main/java/com/fpt/aita/ai/AiResult.java`
- `src/main/java/com/fpt/aita/ai/AiRunSummary.java`
- `src/main/java/com/fpt/aita/ai/FileMethodCodeProvider.java`
- `src/main/java/com/fpt/aita/ai/GeminiClient.java`
- `src/main/java/com/fpt/aita/ai/MethodCodeProvider.java`
- `src/main/java/com/fpt/aita/ai/MockAiClient.java`
- `src/main/java/com/fpt/aita/ai/PromptBuilder.java`
- `src/main/java/com/fpt/aita/dal/AiOptimizationDAO.java`
- `src/test/java/com/fpt/aita/test/AiAnalysisServiceTest.java`
- `src/test/java/com/fpt/aita/test/AiResponseParserTest.java`
- `src/test/java/com/fpt/aita/test/PromptBuilderTest.java`
- `plan/AI_INTEGRATION_GUIDE.md`
- `plan/TEST_AI_MODULE.md`

Lý do: tạo contract giữa service và provider AI, tích hợp Gemini, lưu/đọc kết quả, và có kiểm thử độc lập. Nếu thiếu service/client/DAO thì không có luồng từ AST đến API và DB; nếu thiếu parser/validator thì response không đáng tin có thể được lưu; nếu thiếu prompt delimiters thì chỉ dẫn giả trong code người học có thể bị hiểu nhầm là chỉ dẫn hệ thống; nếu thiếu test thì regression của parser/prompt/service khó phát hiện.

Hai test bổ sung sau đó trong commit `37593cd`:

- `src/test/java/com/fpt/aita/test/FileMethodCodeProviderTest.java`
- `src/test/java/com/fpt/aita/test/GeminiClientRetryTest.java`

Chúng kiểm tra path traversal và retry/backoff mà không cần gọi mạng thật.

### Nhóm B — File AI của tôi được sửa/hoàn thiện

| File/commit | Thay đổi | Nếu không có thay đổi |
|---|---|---|
| `src/main/java/com/fpt/aita/model/AiOptimization.java` — merge `fe26eb0`, sửa conflict `9812a93` | Hợp nhất trường AI với metadata UI: giữ Serializable/serialVersionUID, các constructor của phần AI, thêm method/file name và complexity fields/accessors. | Dấu conflict khiến Java không biên dịch; thiếu metadata khiến report DAO/UI không tạo được model cần thiết. |
| `AiAnalysisService.java`, `AiConfig.java` — `37593cd` | `main()` tạo config qua `AiConfig.fromEnv()` và client qua factory, nhận submission ID từ args, mặc định 1. | Demo tiếp tục ép dùng Mock và submission ID hardcode; không thử đúng lựa chọn Gemini theo cấu hình. |
| `GeminiClient.java` — `37593cd` | Tách helper phân loại retry và tính backoff để test độc lập, không cần HTTP thật. | Chính sách retry khó xác minh mà không gọi mạng/dùng key. |
| `FileMethodCodeProvider.java` — `37593cd` | Chuẩn hóa path, chặn absolute path/`..`, kiểm tra file thực tế không thoát submission root. | Tên/path do DB cung cấp có thể đọc file ngoài thư mục source của submission. |
| `plan/AI_INTEGRATION_GUIDE.md`, `plan/TEST_AI_MODULE.md` — `37593cd` | Cập nhật quy ước thư mục, điểm gọi worker, giới hạn schema và kết quả test. | Người tích hợp dễ gọi sai thời điểm/đường dẫn hoặc hiểu nhầm complexity đã được persist. |

### Nhóm C — Thay đổi không phải tôi sửa trong công việc AI này

- **Không sửa servlet, JSP, `AnalysisReportDAO`, `Issue`, `AnalysisReport` hoặc `DBContext`** trong phần việc AI/document này.
- `git diff origin/main` có `src/main/java/com/fpt/aita/dal/DBContext.java` khác base và worktree cũng báo file này đang modified. Đây là thay đổi có sẵn ngoài task, không được chỉnh hoặc quy là thay đổi AI.
- `.env.example` được thêm, `.gitignore` cập nhật và `.env` được gỡ khỏi nhánh bởi commit cấu hình/bảo mật (`650f851`, `f73c8bc`); đây không phải code AI. Không đọc nội dung `.env`.
- `repomix-output.xml` bị xóa trong nhánh và `repomix-output.md` có mặt ở worktree; đây là thay đổi ngoài module AI.
- `pom.xml` được chỉnh trong commit `87a91c1` để bổ sung cấu hình/phụ thuộc build cần cho module AI (Gson xử lý JSON); đây là thay đổi build dùng chung, không phải file Java của thành viên khác.

## 4. Mười câu hỏi giảng viên có thể hỏi

1. **Vì sao tách `AiClient` thành interface?**
   Để service phụ thuộc vào hợp đồng chung, có thể thay Gemini bằng Mock/test client mà không đổi logic service.

2. **API key lấy từ đâu và vì sao không hardcode?**
   `AiConfig` đọc `GEMINI_API_KEY` từ environment, fallback Java system property. Không hardcode để tránh lộ key qua Git/source; Gemini nhận key qua header.

3. **Vì sao cần `MockAiClient`?**
   Chạy demo/test offline, không tốn quota và không phụ thuộc mạng/API key thật.

4. **Khối `STUDENT_CODE_START/END` chống prompt injection thế nào?**
   Delimiters cùng chỉ dẫn rõ ràng đánh dấu source là dữ liệu không tin cậy; model được yêu cầu bỏ qua lệnh nằm trong code. Đây là biện pháp giảm rủi ro, không phải bảo đảm an toàn tuyệt đối.

5. **Vì sao giới hạn source 4000 ký tự?**
   Giảm payload/token, thời gian/chi phí API và tránh gửi method quá dài; prompt thêm ghi chú khi code bị cắt.

6. **Vì sao retry 429/5xx nhưng không retry 400/401/403?**
   429 là rate limit, 5xx thường là lỗi tạm thời phía dịch vụ; thử lại có thể thành công. 400/401/403 thường do request sai hoặc quyền/key sai; gửi lại nguyên request không sửa được lỗi.

7. **Backoff hiện là exponential hay linear?**
   Linear: `attempt * 1000ms`, tức 1s, 2s, ... Không nên gọi là exponential.

8. **Path traversal được chặn ra sao?**
   Provider từ chối absolute path và segment `..`, normalize/resolve vào submission root, sau đó kiểm tra canonical path để file/symlink không trỏ ra ngoài.

9. **Service chọn method nào và tránh gọi AI lặp lại thế nào?**
   Chọn method có Critical/Warning hoặc độ phức tạp từ O(N²) trở lên, ưu tiên lỗi nghiêm trọng, áp giới hạn cấu hình; bỏ qua method đã tối ưu và tra cache theo code gốc.

10. **`new_complexity` được lưu ở đâu?**
    Parser đưa vào `AiResult`, nhưng DAO hiện chỉ ghi code/giải thích/method/time. Schema không có `complexity_after`; report DAO lấy `complexityBefore` từ `METHODS.estimated_complexity`. Cần thống nhất migration và cập nhật DAO/report để lưu Big-O mới thật.

## 5. Cách chạy demo

### 5.1 Chuẩn bị

1. Cấu hình `GEMINI_API_KEY` qua nơi quản lý environment/secret của máy hoặc cấu hình chạy IDE; không ghi key vào source, tài liệu chia sẻ, lệnh commit hoặc log.
2. Đặt các biến không bí mật cho tiến trình Java, ví dụ:

   ```powershell
   $env:AI_PROVIDER = 'gemini'
   $env:GEMINI_MODEL = '<tên-model-được-cấp>'
   $env:AI_EXTRACTED_ROOT = 'C:\duong-dan\extracted_submissions'
   $env:AI_MAX_RETRIES = '2'
   $env:AI_MAX_METHODS_PER_SUBMISSION = '10'
   ```

   Nếu `GEMINI_API_KEY` hoặc model chưa được cấu hình hợp lệ, factory chọn Mock. Vẫn cần DB và dữ liệu methods/issues vì `AiAnalysisService` đọc submission qua DAO.

3. Chuẩn bị DB có submission và các dòng `SOURCE_FILES`, `METHODS`, `ISSUES` hợp lệ. Source đã giải nén phải nằm ở:

   ```text
   {AI_EXTRACTED_ROOT}\{submissionId}\...
   ```

   `SOURCE_FILES.file_name` phải khớp tên/path tương đối trong thư mục đó; method cần có start/end line hợp lệ.

4. Build project bằng Maven đã cài trên máy:

   ```powershell
   & "C:\Program Files\NetBeans-17\netbeans\java\maven\bin\mvn.cmd" clean package
   ```

### 5.2 Chạy `AiAnalysisService`

Trong PowerShell từ thư mục gốc dự án, dùng classpath gồm class đã build và thư viện trong WAR:

```powershell
$cp = 'target\classes;target\AITA_StaticCodeAnalysis-1.0-SNAPSHOT\WEB-INF\lib\*'
java -cp $cp com.fpt.aita.ai.AiAnalysisService 123
```

Thay `123` bằng `submissionId` cần chạy. Bỏ đối số thì main dùng ID `1`. Kết quả kỳ vọng: log quá trình, rồi summary dạng:

```text
AiRunSummary submissionId=123: AiRunSummary{totalMethods=..., analyzed=..., fromCache=..., skipped=..., failed=...}
```

Số thực tế phụ thuộc dữ liệu DB, source file, cấu hình AI và quota. Chạy Mock vẫn cần DB; Mock chỉ thay lời gọi API. Lần chạy tiếp có thể thấy `skipped` nếu method đã có bản ghi. Không chạy `AiOptimizationDAO.main()` trên DB dùng chung vì demo DAO có thể INSERT. Không chạy `GeminiClient.main()` để test offline: có nhánh dùng cấu hình môi trường và gọi Gemini thật nếu key môi trường tồn tại.

### 5.3 Kết quả test đã xác minh

Ở lần kiểm thử gần nhất, `mvn clean package` build thành công. Maven Surefire báo 0 tests vì các test là `main()`; khi chạy trực tiếp, kết quả:

| Test | Kết quả |
|---|---:|
| `AiResponseParserTest` | 4 passed, 0 failed |
| `PromptBuilderTest` | 3 passed, 0 failed |
| `AiAnalysisServiceTest` | 4 passed, 0 failed |
| `GeminiClientRetryTest` | 12 passed, 0 failed |
| `FileMethodCodeProviderTest` | 3 passed, 0 failed |

`AiAnalysisServiceTest` có thể in cảnh báo thiếu JDBC driver do test chạy với classpath tối thiểu; các fake DAO và assertions vẫn chạy offline. Cần phân biệt đây là main-based test, không phải test được Surefire tự thực thi.
