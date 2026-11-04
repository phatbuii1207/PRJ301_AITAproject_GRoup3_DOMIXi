# Kiểm chứng tích hợp trước khi push

Đã ghép bộ chấm test case với 13 commit AST/AI mới từ origin/main.
Giữ pipeline chấm chức năng → lưu điểm → phân tích AST → tối ưu AI.
Lỗi AST/AI không ghi đè điểm chức năng. API AI chạy ở backend, không ở sandbox.
Chỉ một GradingQueueListener quản lý vòng đời worker; giữ API enqueue kèm đường dẫn.
File Java đơn vẫn được phân tích, ZIP được kiểm tra budget trước khi parse trên host.

## Kết quả

- Maven clean verify -Psandbox-it: 37 test PASS, không skip.
- Năm chương trình test main của module AI đều PASS: response parser, prompt builder,
  analysis service, retry policy và file provider/path traversal; không gọi API thật.
- HTTP Tomcat 9 + SQL Server + Docker: điểm đúng/đạt một phần được lưu đồng thời
  với SOURCE_FILES, METHODS, ANALYSIS_REPORTS và AI_OPTIMIZATIONS.
- Có kiểm tra ZIP chưa cấu hình test và file .java đơn: AST/AI vẫn hoạt động,
  không gán điểm chức năng giả.
- Kiểm tra AI bằng provider mock, chưa xác minh Gemini với API key thật/Azure SQL.
- Tài khoản, bài tập, bài nộp và file test đã dọn; số bản ghi phục hồi.

Chi tiết lượt chạy: target/merge-http-verification.json, target/*-merge-check.log
và báo cáo JUnit trong target/surefire-reports, target/failsafe-reports.

Local .env được giữ trên máy và bị gitignore theo cấu hình mới của nhóm.
Không đưa build artifacts, upload hay credential mới vào commit.
