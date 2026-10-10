# Kế Hoạch Chuẩn Bị & Báo Cáo Tuần 3 (Peer-Review 1) - Đồ Án AITA

**Dự án:** AITA - AI Static Code Analysis & AST Optimization Portal  
**Môn học:** PRJ301 (Java Web Application - RBL Curriculum)  
**Nhóm thực hiện:** Nhóm 3 (DOMIXi)  
**Mục tiêu cột mốc:** Hoàn thiện 100% tiêu chí Peer-Review 1 (Tuần 3) theo khung Rubric PRJ30x.

---

## 1. Tổng Quan Mục Tiêu & Tiêu Chí Đánh Giá Tuần 3

Theo đề cương RBL PRJ301, Tuần 1-3 tập trung vào **Nền tảng & Thiết kế (Identity, Course Management & UI)**:
- **Database Design (10đ Rubric):** CSDL chuẩn hóa 3NF, đầy đủ ràng buộc toàn vẹn, sơ đồ quan hệ ERD và nạp dữ liệu mẫu (Seed data).
- **Course / Assignment Management (CRUD):** Hoàn thiện đầy đủ 4 thao tác Tạo - Đọc - Sửa - Xóa đề bài tập từ CSDL qua mô hình MVC2 & JDBC DAO.
- **Identity & Authentication (Xác thực người dùng):** Đăng nhập, kiểm tra mật khẩu băm SHA-256, lưu phiên làm việc `HttpSession`, phân quyền `Lecturer` / `Student`.
- **UI/UX Prototype:** Giao diện trực quan, đồng bộ trải nghiệm cho cả Giảng viên và Sinh viên.

---

## 2. Danh Sách Hạng Mục & Trạng Thái Thực Hiện

| STT | Hạng mục công việc | Deliverables (Sản phẩm bàn giao) | Trạng thái | Ghi chú kỹ thuật |
| :---: | :--- | :--- | :---: | :--- |
| **1** | **Cơ sở dữ liệu (SQL Server)** | File `AITA_Database.sql` | ✅ **Hoàn thành** | 10 bảng chuẩn 3NF, seed data 5 GV và 30 SV |
| **2** | **Sơ đồ thực thể quan hệ (ERD)** | File `ERD.drawio`, ảnh `ERD.png` | ✅ **Hoàn thành** | Đã đồng bộ đủ 10 bảng, có cả `ASSIGNMENTS` & `CLASSES` |
| **3** | **Module CRUD Quản lý Bài tập** | `Assignment.java`, `AssignmentDAO.java`, `AssignmentServlet.java`, `assignments.jsp` | ✅ **Hoàn thành** | Đạt chuẩn PRG Pattern, PreparedStatement chống SQLi |
| **4** | **Bộ kịch bản Test CRUD** | Bảng 13 Test Case CRUD chi tiết | ✅ **Hoàn thành** | Đầy đủ Positive & Negative cases, sẵn sàng nộp file báo cáo |
| **5** | **Module Xác thực (Identity)** | `LoginServlet.java`, `UserDAO.java`, `login.jsp`, `home.jsp` | ✅ **Hoàn thành** | Đăng nhập session-based, mật khẩu SHA-256 |
| **6** | **Xử lý tiếng Việt (Encoding Filter)** | `EncodingFilter.java` | ✅ **Hoàn thành** | Chặn UTF-8 toàn bộ request/response |
| **7** | **Màn hình Prototype Sinh viên** | `student_assignments.jsp`, `StudentAssignmentServlet.java` | ✅ **Hoàn thành** | Đầy đủ giao diện xem bài tập từ DB, Modal nộp bài và băm SHA-256 demo |

---

## 3. Kế Hoạch Hành Động Chi Tiết (Action Plan)

### Bước 1: Hoàn thiện tài liệu nộp báo cáo (T-Minus 1 ngày)
- [ ] Mở file `ERD.drawio` trên phần mềm Draw.io $\rightarrow$ Chọn **File > Export as > PNG** $\rightarrow$ Lưu ảnh sơ đồ rõ nét để chèn vào slide thuyết trình.
- [ ] Tạo file tài liệu Báo cáo kiểm thử (`Test_Report.docx` hoặc Excel) với:
  - 13 Test Case CRUD module Bài tập.
  - 4 Test Case Xác thực Đăng nhập / Đăng xuất.
- [x] Tạo file giao diện mẫu `student_assignments.jsp` và Servlet điều hướng (Prototype giao diện Sinh viên xem đề bài từ CSDL, mở Modal nộp bài và băm SHA-256 demo) để Giảng viên thấy bức tranh hoàn chỉnh của hệ thống.

### Bước 2: Thiết lập môi trường Demo thực tế (Trước giờ G)
- [ ] Khởi động SQL Server, kiểm tra CSDL `AST_CodeAnalysisDB`.
- [ ] Chạy file `DBContext.java` để kiểm tra console báo: `===> KẾT NỐI DATABASE THÀNH CÔNG!`.
- [ ] Mở NetBeans $\rightarrow$ Vào tab **Services (Ctrl + 5)** $\rightarrow$ Mở rộng **Databases** $\rightarrow$ Chuột phải bảng `ASSIGNMENTS` chọn **View Data**.
- [ ] Khởi động server Apache Tomcat 9, chạy ứng dụng trên trình duyệt ở link: `http://localhost:8080/AITA_StaticCodeAnalysis/assignments`.

### Bước 3: Phân công vai trò thuyết trình trong nhóm (DOMIXi)
1. **Thành viên 1 (Mở đầu & CSDL):**
   - Giới thiệu đề tài AITA và mục tiêu giải quyết bài toán kiểm tra code tự động.
   - Thuyết trình sơ đồ quan hệ ERD (10 bảng, cấu trúc lưu trữ AST, Rules, Issues, Optimizations).
2. **Thành viên 2 (Demo Code & CRUD):**
   - Trình bày kiến trúc MVC2 (`AssignmentDAO` $\leftrightarrow$ `AssignmentServlet` $\leftrightarrow$ `assignments.jsp`).
   - Thao tác trực tiếp trên web: Thêm đề bài mới $\rightarrow$ Sửa $\rightarrow$ Xóa (đối chiếu thay đổi dữ liệu thời gian thực trên NetBeans View Data).
3. **Thành viên 3 (Authentication & Q&A):**
   - Demo luồng đăng nhập Giảng viên (`gv_namhn`), Sinh viên (`he180001`), báo lỗi sai pass.
   - Phụ trách trả lời các câu hỏi về bảo mật (PreparedStatement, SQL Injection, Session vs JWT).

---

## 4. Kịch Bản Trả Lời Vấn Đáp (Q&A Defense Cheat-Sheet)

> [!IMPORTANT]
> **Câu hỏi 1: Tại sao trong tài liệu ghi "JWT Authentication" mà nhóm lại dùng Session?**  
> **Trả lời:** *"Dạ thưa thầy/cô, ở mốc Tuần 3, nhóm ưu tiên làm chủ nền tảng **Servlet HttpSession** chuẩn của PRJ301 để phân quyền và quản lý phiên làm việc ổn định trên web. Cơ chế Token JWT đã được nhóm thiết kế trong kiến trúc và sẽ tích hợp ở giai đoạn sau khi xây dựng RESTful API kết nối với Engine AI."*

> [!TIP]
> **Câu hỏi 2: Tại sao nhóm không làm CRUD cho bảng `USERS`?**  
> **Trả lời:** *"Dạ thưa thầy/cô, trong hệ thống giáo dục AITA, danh sách Sinh viên và Giảng viên được đồng bộ từ hệ sinh thái quản lý đào tạo của nhà trường (đã nạp sẵn dữ liệu mẫu 30 sinh viên, 5 giảng viên theo lớp). Nhóm tập trung kiểm thử Xác thực/Phân quyền trên `USERS` và tập trung toàn bộ nghiệp vụ CRUD chuyên sâu vào module cốt lõi là Quản lý Đề bài (`ASSIGNMENTS`)."*

---

## 5. Lộ Trình Chuyển Tiếp Sang Tuần 4 - 6

Sau khi hoàn thành xuất sắc buổi Peer-Review 1 (Tuần 3), nhóm sẽ bước ngay vào giai đoạn Tuần 4-6 (Business Logic & Xử lý Artifact):
1. **Xây dựng Submission Portal:** Cho phép sinh viên upload file `.zip` chứa mã nguồn Java.
2. **Kiểm tra SHA-256 Hash:** Áp dụng thuật toán băm SHA-256 để chống gian lận và phát hiện bài nộp trùng lặp.
3. **Bộ lọc Filter:** Cưỡng bức kiểm tra bảo mật và định dạng file trước khi chuyển tiếp sang hàng đợi phân tích AST.
