# KẾ HOẠCH BÁO CÁO ĐỒ ÁN AITA - TUẦN 3 (PEER-REVIEW 1)

**Dự án:** AITA - AI Static Code Analysis & AST Optimization Portal  
**Môn học:** PRJ301 (Java Web Application)  
**Nhóm:** 3 (DOMIXi)  

---

## 1. MỤC TIÊU CỐT LÕI CỦA TUẦN 3
- Đạt điểm tối đa mục **Database Design (10đ Rubric)**: CSDL 10 bảng chuẩn 3NF, ERD đồng bộ 100%.
- Thuyết phục mục **Course Management (CRUD)**: Hoàn chỉnh Tạo, Đọc, Sửa, Xóa trên bảng `ASSIGNMENTS`.
- Hoàn thành mục **Identity & Authentication**: Đăng nhập, phân quyền Giảng viên / Sinh viên, quản lý Session.
- Chuẩn bị đầy đủ tài liệu kiểm thử (Báo cáo Test Case) và kịch bản vấn đáp.

---

## 2. CHECKLIST TRƯỚC KHI ĐI BÁO CÁO

### A. Tài liệu & Hình ảnh:
- [ ] Mở file `ERD.drawio` trên Draw.io -> Chọn **File > Export as > PNG** -> Lưu ảnh sơ đồ chèn vào Slide/Báo cáo.
- [ ] Bảng 13 Test Case CRUD module Bài tập (đã chuẩn bị sẵn trong báo cáo).
- [ ] Bảng 4 Test Case Xác thực Đăng nhập / Đăng xuất.

### B. Môi trường Máy tính (Laptop Demo):
- [ ] Chạy file `DBContext.java` để chắc chắn hiện: `===> KẾT NỐI DATABASE THÀNH CÔNG!`.
- [ ] Mở sẵn NetBeans tab **Services (Ctrl + 5)** -> Mở bảng `ASSIGNMENTS` chọn **View Data**.
- [ ] Chạy Tomcat và mở sẵn trình duyệt ở: `http://localhost:8080/AITA_StaticCodeAnalysis/assignments`.

---

## 3. PHÂN CÔNG THUYẾT TRÌNH (NHÓM 3)

1. **Người 1: Thuyết trình CSDL & Kiến trúc tổng quan**
   - Giới thiệu đề tài AITA và bài toán tối ưu hóa mã nguồn Java.
   - Thuyết trình sơ đồ ERD (10 bảng: AST, Methods, Rules, Issues, Submissions, Assignments, Classes).
2. **Người 2: Trực tiếp Demo Code CRUD**
   - Thao tác trên web: Thêm bài mới -> Bấm Sửa -> Bấm Xóa.
   - Qua NetBeans bấm F9 refresh để chứng minh dữ liệu ăn vào SQL Server thật.
   - Chỉ vào các dòng code trong `AssignmentServlet.java` và `AssignmentDAO.java`.
3. **Người 3: Demo Authentication, Phân quyền & Giao diện Sinh viên**
   - Thử đăng nhập GV (`gv_namhn`): Hệ thống tự động chuyển hướng vào Cổng Quản lý bài tập (`/assignments`).
   - Thử đăng nhập SV (`he180001`): Hệ thống tự động chuyển hướng vào Cổng Sinh viên (`/student-assignments`).
   - Demo giao diện Prototype Sinh viên: danh sách đề bài từ SQL Server, mở Modal "Nộp bài", chọn file và bấm nộp bài mô phỏng băm SHA-256.
   - Thử truy cập chéo URL: Tài khoản SV gõ `/assignments` sẽ bị chuyển hướng an toàn về `/student-assignments`.
   - Trả lời các câu hỏi về: PreparedStatement (chống SQLi), Session vs JWT, Phân quyền Role-based, Post-Redirect-Get pattern.

---

## 4. BÍ QUYẾT TRẢ LỜI VẤN ĐÁP ĂN ĐIỂM
- **Khi thầy cô hỏi về JWT:** Trả lời là nhóm ưu tiên làm chủ `HttpSession` chuẩn PRJ301 ở Tuần 3, Token JWT sẽ tích hợp khi làm API ở tuần 7-9.
- **Khi thầy cô hỏi về CRUD User:** Trả lời là tài khoản được nạp sẵn từ hệ thống đào tạo (Seed Data), nghiệp vụ CRUD chính của hệ thống là Quản lý Đề bài (`ASSIGNMENTS`).
