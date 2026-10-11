# AITA - AI Static Code Analysis & AST Optimization Portal
**PRJ301 - SE20C - Group 3 (DOMIXi)**

## 📌 Giới thiệu Đề tài
Hệ thống Hỗ trợ Giảng viên và Sinh viên trong việc kiểm tra, đánh giá chất lượng mã nguồn Java thông qua phân tích cú pháp tĩnh (AST - Abstract Syntax Tree), phát hiện các lỗ hổng bảo mật (SQL Injection, Resource Leak), code smells, và đề xuất tối ưu hóa độ phức tạp thuật toán (O(N^2) -> O(N)) bằng AI.

## 🛠️ Công nghệ sử dụng
- **Backend:** Java EE 8 (Servlet 4.0, JSP, JSTL, JDBC)
- **Architecture:** MVC Pattern (Model - View - Controller / DAO Layer)
- **Database:** Microsoft SQL Server 2019+ (Database: AST_CodeAnalysisDB, 10 bảng chuẩn 3NF)
- **Build Tool:** Apache Maven
- **Server:** Apache Tomcat 9.0.x
- **IDE:** Apache NetBeans 17+ / JDK 1.8

## 🚀 Hướng dẫn Cài đặt & Chạy ứng dụng
1. **Thiết lập Database:**
   - Mở SQL Server Management Studio (SSMS).
   - Chạy file AITA_Database.sql (bấm F5) để khởi tạo CSDL AST_CodeAnalysisDB và dữ liệu mẫu.
2. **Cấu hình DBContext.java:**
   - Mở src/main/java/com/fpt/aita/dal/DBContext.java và chỉnh sửa thông tin tài khoản SQL Server (user, password, instanceName hoặc port 1433).
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

