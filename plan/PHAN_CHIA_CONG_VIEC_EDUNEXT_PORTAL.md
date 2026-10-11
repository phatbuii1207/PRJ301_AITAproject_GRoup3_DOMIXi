# 👥 KẾ HOẠCH PHÂN CHIA NHIỆM VỤ 5 THÀNH VIÊN
## NÂNG CẤP CỔNG HỌC TẬP EDUNEXT & MODULE ĐĂNG KÝ (PRJ301)
* **Dự án:** AITA - AI Static Code Analysis Portal
* **Phong cách giao diện:** Mô phỏng FPT EduNext Portal (Hiện đại, Trực quan, Thân thiện sinh viên)

---

## 🎯 1. BẢNG PHÂN CHIA TRÁCH NHIỆM CHI TIẾT (5 THÀNH VIÊN)

| Thành viên | Trọng tâm phụ trách | File code & Deliverables chịu trách nhiệm chính |
| :---: | :--- | :--- |
| **TV 1** | **Auth & User Registration (Backend)** | `RegisterServlet.java`, hàm `register()`, `isUsernameExists()` trong `UserDAO.java`, validation tài khoản |
| **TV 2** | **Quản lý Môn học & Trang chủ EduNext (Backend)** | Entity `Course.java`, `CourseDAO.java`, `HomeServlet.java` (4 môn học chuẩn CNTT FPT) |
| **TV 3** | **Chi tiết Môn học & Phân quyền Bài tập (Backend)** | `CourseDetailServlet.java`, cập nhật `AssignmentDAO.java` (lọc bài tập theo môn, phân quyền GV/SV) |
| **TV 4** | **Frontend UI/UX Designer (EduNext Theme)** | `home.jsp` (4 Cards EduNext), `course_detail.jsp`, thiết kế lại `login.jsp`, `register.jsp`, CSS |
| **TV 5** | **Database, QA Tester & AI Debug Log** | Thêm bảng `COURSES` trong `AITA_Database.sql`, bộ `TEST_CASES.md`, viết `AI_DEBUGGING_LOG.md` (20% Rubric) |

---

### 👤 CHI TIẾT NHIỆM VỤ TỪNG BẠN:

#### 👤 THÀNH VIÊN 1: AUTHENTICATION & USER REGISTRATION (BACKEND)
- Viết `boolean isUsernameExists(String username)` và `boolean register(User u)` trong `UserDAO.java`.
- Tạo `RegisterServlet.java` (`/register`):
  - Kiểm tra validation: username $\ge 5$ ký tự, password $\ge 6$ ký tự, password trùng khớp repassword.
  - Gán vai trò mặc định `role = 'Student'`.
  - Đăng ký thành công $\rightarrow$ chuyển về `login.jsp` kèm thông báo chúc mừng.
- Phụ trách: `RegisterServlet.java`, `UserDAO.java`, `User.java`.

#### 👤 THÀNH VIÊN 2: QUẢN LÝ MÔN HỌC & TRANG CHỦ EDUNEXT (BACKEND)
- Tạo Entity Model `Course.java` (id, code, name, description, badge, totalSlots).
- Viết `CourseDAO.java` (`getAllCourses()`, `getCourseById()`, `searchCourses()`).
- Viết `HomeServlet.java` (`/home`): Nạp danh sách 4 môn học từ CSDL và chuyển tiếp sang `home.jsp`.
- Phụ trách: `Course.java`, `CourseDAO.java`, `HomeServlet.java`.

#### 👤 THÀNH VIÊN 3: CHI TIẾT MÔN HỌC & PHÂN QUYỀN BÀI TẬP (BACKEND)
- Cập nhật `AssignmentDAO.java`: Hàm `getAssignmentsByCourse(int courseId)` và gán `course_id` khi tạo bài tập mới.
- Viết `CourseDetailServlet.java` (`/course`):
  - Lấy thông tin môn học và danh sách bài tập thuộc môn đó.
  - Phân quyền nghiệp vụ:
    - **Giảng viên:** Giao bài tập mới theo môn, sửa deadline, xóa bài tập.
    - **Sinh viên:** Xem danh sách bài tập kèm deadline, nộp bài, xem lịch sử nộp.
- Phụ trách: `CourseDetailServlet.java`, cập nhật `AssignmentDAO.java`, `Assignment.java`.

#### 👤 THÀNH VIÊN 4: FRONTEND UI/UX DESIGNER (EDUNEXT STYLE)
- Thiết kế giao diện Đăng nhập / Đăng ký hiện đại (`login.jsp`, `register.jsp`) kiểu chia đôi màn hình (Split-screen).
- Thiết kế Trang chủ Dashboard (`home.jsp`): Sidebar bên trái, Header chào sinh viên, 4 Card môn học phong cách EduNext (`PRJ301`, `CSD201`, `PRO192`, `DBI202`) có nút *"Get started $\rightarrow$"*.
- Thiết kế Trang Chi tiết môn học (`course_detail.jsp`): Phân nhánh hiển thị trực quan cho Giảng viên và Sinh viên bằng JSTL/EL (bỏ Scriptlet).
- Phụ trách: `login.jsp`, `register.jsp`, `home.jsp`, `course_detail.jsp`, CSS.

#### 👤 THÀNH VIÊN 5: DATABASE ARCHITECT, QA TESTER & AI DEBUG LOG
- Cập nhật `AITA_Database.sql`: Thêm bảng `COURSES`, thêm cột `course_id` vào `ASSIGNMENTS`, nạp seed data 4 môn học.
- Soạn bảng Test Cases kiểm thử chức năng Đăng ký, xem 4 môn học, phân quyền Giảng viên/Sinh viên.
- **Biên soạn tài liệu `AI_DEBUGGING_LOG.md`:** Ghi lại 3+ tình huống debug bằng AI có đầy đủ bằng chứng để đạt điểm tối đa tiêu chí Process 20%.
- Phụ trách: `AITA_Database.sql`, `TEST_CASES_EDUNEXT.md`, `AI_DEBUGGING_LOG.md`.

---

## 🛡️ 2. MA TRẬN BẢO ĐẢM KHÔNG XUNG ĐỘT GIT (0% CONFLICT)

* **TV 1:** Chỉ viết hàm mới ở **cuối file** `UserDAO.java`, tạo mới `RegisterServlet.java`.
* **TV 2:** Toàn bộ tạo file mới (`Course.java`, `CourseDAO.java`, `HomeServlet.java`) $\rightarrow$ Tự động gộp code 100%.
* **TV 3:** Tạo mới `CourseDetailServlet.java`, thêm hàm mới ở **cuối file** `AssignmentDAO.java`.
* **TV 4:** Toàn quyền xử lý tầng View HTML/CSS (`.jsp`), không sửa vào logic Java của TV1, TV2, TV3.
* **TV 5:** File tài liệu riêng biệt (`AI_DEBUGGING_LOG.md`, test cases). File SQL do duy nhất TV5 cập nhật.

---

## 🚀 3. QUY TRÌNH PHỐI HỢP GIT (GIT FLOW)

1. **Bước 1:** TV 5 cập nhật CSDL và đẩy lên `main` đầu tiên.
2. **Bước 2:** TV 1, TV 2, TV 3 kéo `main` về, mỗi người tạo 1 nhánh (`feature/...`) để code backend của mình, xong lần lượt merge vào `main`.
3. **Bước 3:** TV 4 kéo `main` về, ghép toàn bộ giao diện HTML/CSS và hoàn thiện giao diện View.
