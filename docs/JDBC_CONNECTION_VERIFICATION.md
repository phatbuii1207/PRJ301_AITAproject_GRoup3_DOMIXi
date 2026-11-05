# Kiểm chứng sửa JDBC và đăng nhập Supabase — 2026-11-05

Vấn đề: DAO mở connection từ constructor, giữ nó trong servlet/singleton hoặc không
đóng sau request đăng nhập. Kết nối lỗi có thể bị giữ lại; kết nối bị bỏ quên làm cạn
session pooler, và lỗi database bị hiển thị thành sai tên đăng nhập/password.

DAO thông thường hiện stateless; connection/statement được đóng trong từng thao tác.
GradingDAO vẫn giữ transaction trên connection riêng được close ở ranh giới job/request.
UserDAO phân biệt không tìm được account với SQLException; LoginServlet trả HTTP503
và thông báo database tạm lỗi khi không xác thực được do hạ tầng. Không thay đổi
thuộc tính database, cơ chế hash password hay quy tắc phân quyền.

Đã kiểm tra:

- **50 JUnit/integration tests PASS, không skip**: 32 unit, 3 PostgreSQL JDBC,
  15 Docker. 32 unit cũng chạy trên JVM Java 8 thật với mạng bị tắt.
- Fake JDBC: constructor DAO không giữ session; 50 lần login đóng đủ connection/
  statement; lỗi khi connect/query vẫn giải phóng tài nguyên và request kế tiếp phục hồi.
- PostgreSQL tạm riêng: 48 login trên 6 worker dùng chung DAO, không tăng số client
  còn mở; CRUD/Unicode/generated IDs, transaction barem/snapshot và AST/AI mock PASS.
- HTTP Supabase thật: **36/36 login, 6 worker đồng thời**, gồm giảng viên/sinh viên,
  mở trang home/môn học và logout. Password sai vẫn hiển thị lỗi account bình thường.
  Kết quả local: `target/supabase-login-stress.json`.
- HTTP server thử riêng: login thành công → chuyển cấu hình sang cổng DB không tồn
  tại → nhận HTTP503 với thông báo database → trả cấu hình đúng → login lại thành công,
  không restart server. Kết quả: `target/jdbc-http-recovery.json`.
- Parser cấu hình hỗ trợ UTF-8 BOM/CRLF của Windows và password có dấu `=`, `#` khi quote.
- PowerShell 7.6.9 portable: script cập nhật file BOM/CRLF, password quote chứa ký tự
  đặc biệt, giữ tham số URL khác, chạy lại idempotent; từ chối host không phải Supabase
  hoặc thiếu credentials mà không sửa file. Kết quả local:
  `target/windows-powershell-config-verification.json`. Runtime kiểm tra trên Linux;
  script giữ cú pháp tương thích Windows PowerShell 5.1, chưa chạy desktop Windows.
- 5 bộ kiểm thử AI dạng main đều PASS, không gọi Gemini thật.

Lần chạy Docker đầu tiên gặp lỗi xác nhận cleanup khi hủy container; không còn
container rò rỉ. Chạy lại đầy đủ bộ test trong JVM mới PASS; lần này không sửa logic
sandbox. Các dữ liệu kiểm thử ghi vào PostgreSQL tạm riêng; database tạm và server
HTTP thử đã dọn. Kiểm tra Supabase chỉ đọc dữ liệu; không chạy migration/reset cloud.

Cấu hình triển khai: `.env.example` dùng pooler6543/SSL/prepareThreshold0;
`scripts/configure-supabase.ps1` cập nhật cấu hình hiện có và giữ credentials.
Xem [hướng dẫn cho nhóm Windows](SUPABASE_WINDOWS.md). Sau khi pull cần cập nhật
`.env` riêng và restart Tomcat trên từng máy để kết thúc connection từ code cũ.

Giới hạn: chưa có desktop Windows để xác minh thao tác UI NetBeans trên Windows;
không đảm bảo đăng nhập khi Supabase/network ngừng phục vụ hoặc vượt giới hạn tải.
