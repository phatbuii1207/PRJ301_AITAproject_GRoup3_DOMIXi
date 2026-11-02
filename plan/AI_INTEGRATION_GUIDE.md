# HƯỚNG DẪN TÍCH HỢP MODULE AI (AITA GEMINI & MOCK)

Tài liệu này cung cấp hướng dẫn tích hợp chi tiết dành cho **Người 3 (Hàng đợi chấm bài / Background Worker)** và **Người 5 (Giao diện hiển thị báo cáo / JSP & Controller)** cùng các câu hỏi vấn đáp bảo vệ đồ án.

---

## 1. Hướng dẫn cấu hình API Key & Môi trường

Module AI hỗ trợ 2 chế độ:
- `AI_PROVIDER=mock`: Chạy offline giả lập, không tốn quota, không cần kết nối mạng.
- `AI_PROVIDER=gemini`: Gọi Google Gemini API chính thức.

Module tự động ưu tiên đọc từ **System Environment Variable** (`System.getenv`), nếu không tìm thấy sẽ đọc từ **Java System Property** (`System.getProperty`).

### Cách 1: Cấu hình cho Tomcat trong NetBeans (Khuyên dùng khi Dev/Test)
Do Apache Tomcat chạy dưới NetBeans thường không tự nạp file `.env`, bạn thêm trực tiếp vào VM Options của Server:
1. Mở NetBeans -> Chuyển sang tab **Services** -> Mở rộng mục **Servers** -> Chuột phải vào **Apache Tomcat** -> Chọn **Properties**.
2. Chọn tab **Platform**.
3. Tại ô **VM Options**, nhập các tham số:
   ```text
   -DGEMINI_API_KEY=AIzaSyYourActualApiKeyHere -DGEMINI_MODEL=gemini-1.5-flash -DAI_PROVIDER=gemini -DAI_TIMEOUT_MS=20000 -DAI_MAX_RETRIES=2 -DAI_MAX_METHODS_PER_SUBMISSION=10 -DAI_EXTRACTED_ROOT=C:/Users/Admin/Downloads/PRJ301_AITA/extracted_submissions
   ```
4. Khởi động lại Tomcat.

### Cách 2: Biến môi trường hệ điều hành (Windows PowerShell / CMD)
```powershell
[System.Environment]::SetEnvironmentVariable('GEMINI_API_KEY', 'AIzaSyYourActualApiKeyHere', 'User')
[System.Environment]::SetEnvironmentVariable('GEMINI_MODEL', 'gemini-1.5-flash', 'User')
[System.Environment]::SetEnvironmentVariable('AI_PROVIDER', 'gemini', 'User')
```

### Cách 3: Cấu hình qua Docker (`.env` file)
Tạo file `.env` ngang hàng file `docker-compose.yml` (file này đã được đưa vào `.gitignore` để không bị lộ lên Git):
```properties
GEMINI_API_KEY=AIzaSyYourActualApiKeyHere
GEMINI_MODEL=gemini-1.5-flash
AI_PROVIDER=gemini
AI_TIMEOUT_MS=20000
AI_MAX_RETRIES=2
AI_MAX_METHODS_PER_SUBMISSION=10
AI_EXTRACTED_ROOT=/app/extracted_submissions
```

---

## 2. Hướng dẫn dành cho Người 3 (Hàng đợi chấm bài / Background Worker)

### Bước gọi tích hợp:
Sau khi Worker giải nén tệp zip và AST Engine (Người 1) đã phân tích cú pháp, trích xuất methods và ghi nhận issues vào CSDL, Người 3 chỉ cần gọi **duy nhất 1 hàm**:

```java
package com.fpt.aita.worker; // Ví dụ package worker của Người 3

import com.fpt.aita.ai.AiAnalysisService;
import com.fpt.aita.ai.AiRunSummary;

public class SubmissionProcessor {

    private final AiAnalysisService aiService = new AiAnalysisService();

    public void processSubmission(int submissionId) {
        // Bước 1: Người 3 giải nén mã nguồn vào AI_EXTRACTED_ROOT/{submissionId}/
        // Bước 2: Người 1 (AST Engine) phân tích mã nguồn -> lưu METHODS, ISSUES
        
        // Bước 3: GỌI AI PHÂN TÍCH VÀ ĐỀ XUẤT TỐI ƯU
        try {
            System.out.println("[Worker] Bắt đầu chạy phân tích AI cho bài nộp: " + submissionId);
            AiRunSummary summary = aiService.analyzeSubmission(submissionId);
            
            System.out.println("[Worker] Hoàn tất AI cho bài nộp " + submissionId + ": " + summary);
            // Ghi log kết quả (không bắt buộc)
        } catch (Exception e) {
            // AiAnalysisService đã tự try/catch từng method bên trong,
            // nhưng bọc ngoài để tuyệt đối đảm bảo thread worker không bao giờ bị dừng đột ngột.
            System.err.println("[Worker] Lỗi ngoài dự kiến khi chạy AI: " + e.getMessage());
        }

        // Bước 4: Người 3 cập nhật SUBMISSIONS.status = 'Completed'
        // QUY TẮC: AI hỏng hoặc timeout KHÔNG ĐƯỢC làm bài nộp chuyển sang status 'Failed'.
    }
}
```

### Ý nghĩa các trường trong `AiRunSummary`:
- `totalMethods`: Số lượng phương thức đủ điều kiện đưa vào xử lý AI (có lỗi Critical/Warning hoặc O(N²) trở lên, tối đa bằng `AI_MAX_METHODS_PER_SUBMISSION`).
- `analyzed`: Số lượng phương thức đã được gọi LLM và tối ưu thành công.
- `fromCache`: Số lượng phương thức được tái sử dụng kết quả ngay từ cache (do trùng mã nguồn với một hàm đã từng tối ưu trước đó).
- `skipped`: Số lượng phương thức bị bỏ qua do đã có bản ghi trong bảng `AI_OPTIMIZATIONS`.
- `failed`: Số lượng phương thức mà AI trả lỗi hoặc timeout (hệ thống tự bỏ qua và tiếp tục phân tích method kế tiếp).

---

## 3. Hướng dẫn dành cho Người 5 (Giao diện hiển thị báo cáo / Controller & JSP)

### A. Truy vấn dữ liệu từ `AiOptimizationDAO`:
Trong Controller/Servlet phụ trách trang xem chi tiết báo cáo chấm bài (`SubmissionDetailController`):

```java
import com.fpt.aita.dal.AiOptimizationDAO;
import com.fpt.aita.model.AiOptimization;

// ... trong doGet()
int submissionId = Integer.parseInt(request.getParameter("id"));
AiOptimizationDAO aiDAO = new AiOptimizationDAO();

// Cách A: Lấy toàn bộ danh sách tối ưu hóa AI của bài nộp này
List<AiOptimization> aiList = aiDAO.getBySubmissionId(submissionId);
request.setAttribute("aiOptimizations", aiList);

// Cách B: Lấy tối ưu hóa cụ thể của 1 method khi người dùng click xem chi tiết method
int methodId = Integer.parseInt(request.getParameter("methodId"));
AiOptimization opt = aiDAO.getByMethodId(methodId);
request.setAttribute("methodAiOpt", opt);
```

### B. Hiển thị trên JSP (Lưu ý quan trọng về An toàn bảo mật):
> [!CAUTION]
> **Phòng chống tấn công XSS:** Nội dung do AI sinh ra (`explanation`, `optimized_code`) hoặc code sinh viên nộp lên (`original_code`) là dữ liệu từ nguồn bên ngoài. **BẮT BUỘC** phải escape thông qua `<c:out>` của JSTL và bao quanh bởi thẻ `<pre><code>` để giữ nguyên format thụt lề dòng code.

```jsp
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="ai-optimization-card">
    <h3>Đề xuất tối ưu hóa từ AITA Tutor (AI Assistant)</h3>

    <c:if test="${not empty methodAiOpt}">
        <!-- Giải thích sư phạm -->
        <div class="ai-explanation alert alert-info">
            <strong>Phân tích nguyên nhân & giải pháp:</strong>
            <p><c:out value="${methodAiOpt.explanation}" /></p>
        </div>

        <div class="row">
            <!-- Code gốc của sinh viên -->
            <div class="col-md-6">
                <h5>Mã nguồn ban đầu:</h5>
                <pre class="bg-light p-3 border rounded"><code><c:out value="${methodAiOpt.originalCode}" /></code></pre>
            </div>

            <!-- Code đã tối ưu từ AI -->
            <div class="col-md-6">
                <h5>Mã nguồn đề xuất tối ưu:</h5>
                <pre class="bg-dark text-white p-3 border rounded"><code><c:out value="${methodAiOpt.optimizedCode}" /></code></pre>
            </div>
        </div>
    </c:if>

    <c:if test="${empty methodAiOpt}">
        <p class="text-muted">Phương thức này đạt tiêu chuẩn hoặc chưa có đề xuất AI.</p>
    </c:if>
</div>
```

---

## 4. Giới hạn đã biết & Bộ câu hỏi vấn đáp bảo vệ đồ án

### Câu hỏi 1: Vì sao lại lưu API Key trong biến môi trường hoặc VM Options mà không hardcode vào file Java hay file `.properties`?
- **Trả lời gợi ý:**
  - **Bảo mật mã nguồn:** Hardcode API key vào source code rất dễ bị lộ khi commit lên Git repository công khai hoặc chia sẻ nhóm.
  - **Nguyên lý 12-Factor App:** Tách biệt cấu hình (Config) khỏi mã nguồn (Code). Cùng một bản build `.war` có thể chạy ở môi trường Dev (dùng `MockAiClient` hoặc test key) và Production (dùng key chính thức) mà không cần biên dịch lại mã nguồn.
  - **Linh hoạt quản trị:** Người quản trị máy chủ (hoặc Docker) có thể đổi key định kỳ (key rotation) mà không cần can thiệp vào mã nguồn dự án.

### Câu hỏi 2: Nếu AI gợi ý sai cú pháp, bịa đặt giải thuật (hallucination) hoặc bị lỗi mạng/timeout thì hệ thống xử lý thế nào?
- **Trả lời gợi ý:**
  - **Không làm hỏng kết quả chấm bài:** Kết quả cốt lõi của bài tập dựa trên AST Engine (phân tích tĩnh kiểm tra quy tắc cú pháp). AI chỉ đóng vai trò là "Trợ giảng gợi ý" (Tutor recommendation). Nếu AI lỗi (429, timeout, network fail), hàm `AiAnalysisService` tự bắt ngoại lệ cục bộ, ghi nhận `failed` và bài nộp vẫn chuyển sang trạng thái hoàn tất (`Completed`) bình thường.
  - **Kỹ thuật Prompt Chặt chẽ:** Đã sử dụng System Instruction ép model trả về định dạng JSON thuần túy, cấm sinh mã ngoài phạm vi, giữ nguyên chữ ký hàm.
  - **Bộ parser khắt khe (`AiResponseParser`):** Kiểm tra đầy đủ 3 trường `explanation`, `optimized_code`, `new_complexity`. Nếu JSON hỏng hoặc rỗng sẽ lập tức từ chối và không ghi dữ liệu rác vào CSDL.

### Câu hỏi 3: Nếu nhiều sinh viên nộp bài cùng một lúc (hàng chục bài liên tiếp), hệ thống xử lý ra sao để không bị vượt Rate Limit (lỗi HTTP 429) của Google Gemini?
- **Trả lời gợi ý:**
  - **Cơ chế Cache thông minh (`findByOriginalCode`):** Nhiều sinh viên thường nộp cùng một đoạn mã thuật toán tương tự hoặc các bài tập mẫu giống nhau. Khi phát hiện mã nguồn đã có trong CSDL, hệ thống lập tức tái sử dụng kết quả cũ mà không tốn lượt gọi API (`fromCache`).
  - **Lọc và Giới hạn theo mức độ nghiêm trọng:** Hệ thống chỉ gửi những method thực sự có vấn đề (lỗi `Critical`, `Warning` hoặc độ phức tạp $\ge O(N^2)$), ưu tiên lỗi nghiêm trọng nhất và giới hạn tối đa `AI_MAX_METHODS_PER_SUBMISSION` (mặc định 10 method/bài).
  - **Cơ chế Retry Exponential Backoff:** `GeminiClient` tích hợp sẵn cơ chế tự động thử lại khi gặp mã HTTP 429 hoặc lỗi 5xx với thời gian chờ tăng dần (1s, 2s...).
  - **Kiến trúc Hàng đợi (Queue/Worker):** Người 3 xử lý tuần tự qua background worker, tránh việc nhiều request HTTP đồng thời cùng lúc bắn ồ ạt lên Gemini API.

