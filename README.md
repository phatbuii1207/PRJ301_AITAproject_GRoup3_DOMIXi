# AITA - AI Static Code Analysis & AST Optimization Portal
**PRJ301 - SE20C - Group 3 (DOMIXi)**

## 📌 Giới thiệu Đề tài
Hệ thống Hỗ trợ Giảng viên và Sinh viên trong việc kiểm tra, đánh giá chất lượng mã nguồn Java thông qua phân tích cú pháp tĩnh (AST - Abstract Syntax Tree), phát hiện các lỗ hổng bảo mật (SQL Injection, Resource Leak), code smells, và đề xuất tối ưu hóa độ phức tạp thuật toán (O(N^2) -> O(N)) bằng AI.

## 🛠️ Công nghệ sử dụng
- **Backend:** Java EE 8 (Servlet 4.0, JSP, JSTL, JDBC)
- **Architecture:** MVC Pattern (Model - View - Controller / DAO Layer)
- **Database:** PostgreSQL 16 (Database: AST_CodeAnalysisDB, 15 bảng / 93 thuộc tính)
- **Build Tool:** Apache Maven
- **Server:** Apache Tomcat 9.0.x
- **IDE:** Apache NetBeans 17+ / JDK 1.8

## Docker Sandbox cho bài nộp Java

Bộ chấm test case đã nối với sandbox: quản lý test công khai/ẩn, so stdout, tính
điểm theo trọng số trên thang 10 và lưu kết quả. Chạy migration
`database/migrations/001_test_case_grading.sql`, rồi xem
[hướng dẫn chấm điểm](docs/TEST_CASE_GRADING.md). Module chấm này không cần API key.
Database cũ cần thêm `database/migrations/002_ai_new_complexity.sql` cho cột kết quả tối ưu AI.

Bài ZIP Java được biên dịch/chạy trong container Java 8 non-root, không có mạng ra ngoài,
giới hạn 128 MiB RAM, 0.5 CPU và 5 giây. Build image trước khi dùng:

```bash
docker build -t aita-sandbox:java8 docker/sandbox
mvn clean verify -Psandbox-it
```

Xem [hướng dẫn Docker Sandbox](docs/DOCKER_SANDBOX.md) để cấu hình Tomcat, main class,
đọc kết quả và demo ba tình huống bảo mật. `SUCCESS` xác nhận chạy thành công;
chưa phải kết quả chấm đúng/sai đáp án.

Tập báo cáo bằng [kịch bản thuyết trình, giải thích code và demo](docs/HUONG_DAN_BAO_CAO_SANDBOX.md):
chạy `./scripts/demo-sandbox.sh all` để thử 7 ca và tạo ZIP mẫu dùng trên web.

## 🚀 Hướng dẫn Cài đặt & Chạy ứng dụng
1. **Thiết lập PostgreSQL (không cần Docker):**
   - Cài PostgreSQL 16 và tạo database `AST_CodeAnalysisDB` bằng pgAdmin/DBeaver.
   - Kết nối vào database đó, chạy toàn bộ `AITA_Database.sql` để tạo bảng và dữ liệu mẫu.
2. **Cấu hình JDBC:**
   - Copy `.env.example` thành `.env`, điền `DB_URL`, `DB_USER`, `DB_PASSWORD` của máy bạn hoặc database chung.
   - Với Tomcat/NetBeans, thêm VM option `-Daita.env.file=/đường/dẫn/tuyệt/đối/project/.env`, rồi khởi động lại Tomcat.
   - Không sửa username/password trong Java. Xem [hướng dẫn PostgreSQL cho cả nhóm](docs/POSTGRESQL_SETUP.md) và [kết quả kiểm chứng](docs/POSTGRESQL_VERIFICATION.md).
3. **Chạy ứng dụng trên NetBeans:**
   - Mở Project bằng NetBeans 17.
   - Chuột phải Project -> Clean and Build.
   - Chuột phải Project -> Run (hoặc bấm F6).

## 👥 Tài khoản thử nghiệm có sẵn trong CSDL
- **Giảng viên:** `gv_namhn`, `gv_quanglm`, `gv_lanpt` (Mật khẩu: `123456`)
- **Sinh viên:** `he180001` -> `he180030` (Mật khẩu: `123456`)

## 📋 Phân chia module & Hướng dẫn Test các luồng nghiệp vụ
### 1. Phân hệ Chi tiết môn học & Quản lý bài tập (`/course?id=...`)
- **Vai trò Giảng viên (`Lecturer`):**
  - Giao bài tập mới gắn trực tiếp theo mã môn (`course_id`) kèm hạn nộp (`deadline`).
  - Chỉnh sửa tiêu đề, mô tả và gia hạn deadline cho bài tập.
  - Xóa bài tập khỏi môn học.
  - Xem danh sách sinh viên đã nộp bài, kiểm tra mã băm bảo mật SHA-256 và **tải trực tiếp file bài làm** về máy.
- **Vai trò Sinh viên (`Student`):**
  - Xem danh sách bài tập của môn học kèm đếm ngược hạn nộp.
  - Tự động khóa nộp bài và hiện thông báo nếu bài tập đã quá hạn nộp.
  - Kéo thả / Chọn tệp mã nguồn (.java, .zip, .rar...) để nộp bài. Hệ thống tự động băm mã SHA-256 xác thực toàn vẹn dữ liệu và lưu vết vào CSDL `SUBMISSIONS`.
  - Xem lại lịch sử các lần nộp bài của bản thân.

