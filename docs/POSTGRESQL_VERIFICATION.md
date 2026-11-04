# Kiểm chứng chuyển PostgreSQL — 2026-11-04

Đã chuyển database SQL Server local sang PostgreSQL 16 local, giữ nguyên nguồn.
Tổng cộng **15 bảng / 93 cột / 577 bản ghi**. Đợt chuyển đổi ban đầu chưa push; sau đó người dùng đã yêu cầu kết nối ứng dụng và push GitHub.

| Bảng | Số dòng |
|---|---:|
| CLASSES | 5 |
| USERS | 79 |
| USER_CLASS | 385 |
| ASSIGNMENTS | 8 |
| SUBMISSIONS | 31 |
| SOURCE_FILES | 13 |
| METHODS | 10 |
| RULES | 8 |
| ISSUES | 6 |
| ANALYSIS_REPORTS | 30 |
| AI_OPTIMIZATIONS | 2 |
| GRADING_CONFIGS | 0 |
| GRADING_TEST_CASES | 0 |
| GRADING_REPORTS | 0 |
| GRADING_CASE_RESULTS | 0 |

Helper kiểm tra tên/thứ tự cột, nullable, identity, kiểu dữ liệu tương ứng,
độ dài/precision, PK/FK/cascade; so số dòng và SHA-256 nội dung toàn bảng theo PK.
ID được copy nguyên; sequence được đặt theo last_value của SQL Server, kể cả
khoảng trống do dữ liệu đã xóa. Chi tiết machine-readable tại
`target/postgresql-migration-verification.json`. Timestamp được so sau khi làm tròn
microsecond, xem giới hạn kiểu dữ liệu trong [hướng dẫn setup](POSTGRESQL_SETUP.md).

Đã chạy **41 test, 0 lỗi, 0 bỏ qua**: 24 unit, 15 Docker, 2 PostgreSQL JDBC.
Ngoài ra 5 bộ test AI main (26 assertions) đều PASS, không gọi Gemini thật.
JDBC được thử trên database PostgreSQL tạm riêng, bao gồm Unicode, login không
phân biệt hoa/thường, generated ID, CRUD, booleans, optimistic version/rollback,
NULL score, snapshot test và AST/AI mock. Schema+seed chạy hai lần, migration
bổ sung chạy lại và fixture PostgreSQL riêng đều PASS. WAR chứa PostgreSQL
42.7.14, không chứa MSSQL driver.

HTTP Tomcat 9 port 18884 + PostgreSQL + Docker thật:

- Đăng nhập, tạo bài tập, CSRF, lưu test công khai/ẩn: PASS.
- Nộp ZIP qua web, worker xử lý và lưu kết quả PostgreSQL: PASS.
- Bộ test thử gồm 2 ca cùng trọng số: sum đúng 10.00 / hardcode 5.00 / sai 0.00.
- AST và AI mock được lưu đồng thời với kết quả chấm: PASS.
- Sinh viên khác nhận 403; chủ bài không thấy tên/nội dung test ẩn; giảng viên thấy: PASS.

Biên bản HTTP: `target/postgresql-http-verification.json`.
Tomcat thử đã dừng, database tạm đã xóa, file upload do test tạo trong source
đã dọn. Số dòng database thật sau tất cả kiểm tra vẫn khớp 577.
Không xóa/sửa database SQL Server nguồn và không thay đổi các dịch vụ PostgreSQL
khác trên máy. Database AITA mới đang hoạt động tại localhost:55433;
thông tin riêng nằm trong `.env` đã gitignore.

Chưa deploy Azure/server chung, chưa gọi Gemini thật. Thành viên không cần Docker
để kết nối PostgreSQL; chỉ máy chạy sandbox cần Docker. Việc chuyển database không
di chuyển các ZIP cũ hoặc tạo shared storage cho file bài nộp.

## Xác minh kết nối ứng dụng trước khi push

Tomcat local đã được cấu hình `bin/setenv.sh` đọc `.env` riêng qua AITA_ENV_FILE;
Ứng dụng vừa build chạy tại `http://localhost:8080/AITA_StaticCodeAnalysis/login`. Đã kiểm tra JDBC trực tiếp,
đăng nhập giảng viên/sinh viên, trang home và trang môn học trên database thật: PASS.
15 bảng/93 cột/577 bản ghi vẫn giữ nguyên sau kiểm tra. Tomcat local cổng 8080
được để chạy phục vụ người dùng; Tomcat thử cổng 18884 đã dừng.
Biên bản local: `target/postgresql-live-connection.json`. Cấu hình/password riêng
và WAR không được đưa lên Git.
