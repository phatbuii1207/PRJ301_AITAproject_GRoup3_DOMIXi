# 📖 TÀI LIỆU GIẢI THÍCH CHI TIẾT TỪNG DÒNG CODE DÀNH CHO THÀNH VIÊN 2
## DỰ ÁN: AITA EDUNEXT PORTAL (PRJ301 - JAVA WEB APPLICATION)

Tài liệu này được biên soạn đặc biệt cho **Thành viên số 2**, giải thích chi tiết **từng dòng code**, **tại sao lại viết như vậy**, và **cách trả lời khi giảng viên chỉ tay vào màn hình hỏi vấn đáp**.

---

# PHẦN 1: TẦNG MODEL - FILE `Course.java`
*Đường dẫn:* `src/main/java/com/fpt/aita/model/Course.java`  
*Vai trò trong MVC:* **Model (JavaBean / POJO)** - Đại diện cho một thực thể Môn học trong hệ thống, dùng để đóng gói và vận chuyển dữ liệu giữa Database và Giao diện.

```java
1:  package com.fpt.aita.model;
2:  
3:  public class Course {
4:      private int courseId;
5:      private String courseCode;
6:      private String courseName;
7:      private String description;
8:      private String badge;
9:      private int totalSlots;
10: 
11:     public Course() {
12:     }
13: 
14:     public Course(int courseId, String courseCode, String courseName, String description, String badge, int totalSlots) {
15:         this.courseId = courseId;
16:         this.courseCode = courseCode;
17:         this.courseName = courseName;
18:         this.description = description;
19:         this.badge = badge;
20:         this.totalSlots = totalSlots;
21:     }
22: 
23:     public int getCourseId() { return courseId; }
24:     public void setCourseId(int courseId) { this.courseId = courseId; }
25: 
26:     public String getCourseCode() { return courseCode; }
27:     public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
28: 
29:     public String getCourseName() { return courseName; }
30:     public void setCourseName(String courseName) { this.courseName = courseName; }
31: 
32:     public String getDescription() { return description; }
33:     public void setDescription(String description) { this.description = description; }
34: 
35:     public String getBadge() { return badge; }
36:     public void setBadge(String badge) { this.badge = badge; }
37: 
38:     public int getTotalSlots() { return totalSlots; }
39:     public void setTotalSlots(int totalSlots) { this.totalSlots = totalSlots; }
40: }
```

### 🔍 Giải thích chi tiết từng dòng:

* **Dòng 1: `package com.fpt.aita.model;`**
  * *Ý nghĩa:* Khai báo gói (package) chứa class này. 
  * *Tại sao dùng:* Tuân thủ quy chuẩn cấu trúc mã nguồn của Java EE và mô hình MVC2. Mọi thực thể đại diện cho dữ liệu đều phải nằm trong package `.model`.
* **Dòng 3: `public class Course {`**
  * *Ý nghĩa:* Khai báo một lớp công khai tên là `Course`.
  * *Tại sao dùng:* Từ khóa `public` giúp các class ở package khác (như `CourseDAO` ở `.dal`, `HomeServlet` ở `.controller`, hoặc file JSP ở tầng View) có thể truy cập và sử dụng được.
* **Dòng 4 - 9: Các thuộc tính `private ...;`**
  * `private int courseId;` $\rightarrow$ Khóa chính (Primary Key) định danh môn học trong CSDL.
  * `private String courseCode;` $\rightarrow$ Mã môn (ví dụ: `PRJ301`, `CSD201`).
  * `private String courseName;` $\rightarrow$ Tên đầy đủ của môn học (ví dụ: *Java Web Application*).
  * `private String description;` $\rightarrow$ Đoạn giới thiệu ngắn về nội dung môn.
  * `private String badge;` $\rightarrow$ Nhãn phân loại môn học trên EduNext (ví dụ: `TECH`, `CORE`, `BASIC`).
  * `private int totalSlots;` $\rightarrow$ Tổng số buổi học theo khung đào tạo (mặc định 20 slots).
  * *Tại sao dùng `private`?* Đây là nguyên lý **Đóng gói (Encapsulation)** của hướng đối tượng (OOP). Giấu các biến này để bên ngoài không tự tiện sửa đổi giá trị lung tung mà bắt buộc phải thông qua các phương thức Getter/Setter hợp lệ.
* **Dòng 11 - 12: `public Course() {}`**
  * *Ý nghĩa:* Hàm khởi tạo không tham số (No-argument Constructor).
  * *Tại sao bắt buộc phải có?* Tiêu chuẩn JavaBean và các framework (JSP, JSTL, Hibernate/JPA) khi khởi tạo đối tượng tự động đều bắt buộc class phải có constructor rỗng, nếu thiếu có thể văng lỗi `InstantiationException`.
* **Dòng 14 - 21: `public Course(int courseId, ...)`**
  * *Ý nghĩa:* Hàm khởi tạo đầy đủ tham số (Parameterized Constructor).
  * *Tại sao dùng:* Dùng trong tầng DAO khi đọc dữ liệu từ câu lệnh SQL `ResultSet`, chỉ cần gọi `new Course(...)` truyền toàn bộ cột vào 1 dòng duy nhất là có ngay đối tượng hoàn chỉnh.
  * Từ khóa `this.courseId = courseId;`: Phân biệt giữa biến thành viên của class (`this.courseId`) và tham số truyền vào hàm (`courseId`).
* **Dòng 23 - 39: Các cặp `get...()` và `set...()`**
  * *Getter:* Trả về giá trị của thuộc tính.
  * *Setter:* Cập nhật giá trị mới cho thuộc tính.
  * *Tại sao dùng:* Đây là cầu nối để Expression Language (EL) trên file JSP hoạt động. Khi trên file JSP bạn viết `${c.courseName}`, thực chất Tomcat đang tự động gọi hàm `c.getCourseName()` của class này!

---

# PHẦN 2: TẦNG DAO - FILE `CourseDAO.java`
*Đường dẫn:* `src/main/java/com/fpt/aita/dal/CourseDAO.java`  
*Vai trò trong MVC:* **DAO (Data Access Object)** - Chuyên trách việc kết nối CSDL, thực thi câu lệnh SQL và chuyển đổi dữ liệu SQL thô thành các đối tượng Java.

```java
1:  package com.fpt.aita.dal;
2:  
3:  import com.fpt.aita.model.Course;
4:  import java.sql.PreparedStatement;
5:  import java.sql.ResultSet;
6:  import java.util.ArrayList;
7:  import java.util.List;
8:  
9:  public class CourseDAO extends DBContext {
```

### 🔍 Giải thích dòng 1 - 9 (Khai báo & Kế thừa):
* **Dòng 3 - 7: Các lệnh `import`:**
  * `import com.fpt.aita.model.Course;`: Nhập class `Course` ở tầng Model vào để sử dụng.
  * `import java.sql.PreparedStatement;`: Thư viện JDBC thực thi truy vấn tham số hóa an toàn.
  * `import java.sql.ResultSet;`: Đối tượng chứa bảng kết quả trả về từ câu lệnh `SELECT`.
  * `import java.util.List; import java.util.ArrayList;`: Danh sách động để lưu nhiều môn học.
* **Dòng 9: `public class CourseDAO extends DBContext {`**
  * *Tại sao `extends DBContext`?* `DBContext` là lớp cha nắm giữ đối tượng `connection` (kết nối tới SQL Server). Kế thừa `DBContext` giúp `CourseDAO` sử dụng ngay biến `connection` có sẵn mà không cần phải viết lại code kết nối driver hay cấu hình mật khẩu sa/localhost.

---

### 🔍 Hàm 1: `getAllCourses()` - Lấy danh sách 4 môn học

```java
11:     public List<Course> getAllCourses() {
12:         List<Course> list = new ArrayList<>();
13:         String sql = "SELECT course_id, course_code, course_name, description, badge, total_slots FROM COURSES ORDER BY course_id ASC";
14:         try {
15:             PreparedStatement ps = connection.prepareStatement(sql);
16:             ResultSet rs = ps.executeQuery();
17:             while (rs.next()) {
18:                 list.add(new Course(
19:                     rs.getInt("course_id"),
20:                     rs.getString("course_code"),
21:                     rs.getString("course_name"),
22:                     rs.getString("description"),
23:                     rs.getString("badge"),
24:                     rs.getInt("total_slots")
25:                 ));
26:             }
27:         } catch (Exception e) {
28:             e.printStackTrace();
29:         }
30:         return list;
31:     }
```

* **Dòng 11: `public List<Course> getAllCourses()`**
  * Kiểu trả về là `List<Course>`: Trả về một danh sách các đối tượng môn học. Dùng `List` (Interface) thay vì `ArrayList` cụ thể để tuân thủ nguyên tắc *Programming to an Interface*.
* **Dòng 12: `List<Course> list = new ArrayList<>();`**
  * Khởi tạo một danh sách rỗng ban đầu. Nếu trong CSDL không có môn nào thì vẫn trả về list rỗng (size = 0) chứ không bao giờ trả về `null` (tránh lỗi `NullPointerException` ở tầng trên).
* **Dòng 13: `String sql = "SELECT ... FROM COURSES ORDER BY course_id ASC";`**
  * Câu lệnh SQL tường minh liệt kê chính xác các cột cần lấy, sắp xếp tăng dần theo `course_id`. Tuyệt đối không dùng `SELECT *` vì nếu thứ tự cột trong DB bị thay đổi thì code sẽ bị lỗi.
* **Dòng 14: `try {`**
  * Bọc khối lệnh vào `try-catch` để xử lý ngoại lệ `SQLException` nếu CSDL bị mất kết nối, server chết hoặc sai cú pháp bảng.
* **Dòng 15: `PreparedStatement ps = connection.prepareStatement(sql);`**
  * *Tại sao dùng `PreparedStatement` thay vì `Statement`?* Đây là tiêu chí ăn điểm lớn nhất của PRJ301! `PreparedStatement` được biên dịch sẵn trên CSDL, tăng tốc độ truy vấn và quan trọng nhất là **chống triệt để lỗ hổng SQL Injection**.
* **Dòng 16: `ResultSet rs = ps.executeQuery();`**
  * Gọi hàm `executeQuery()` để chạy câu lệnh `SELECT`. Kết quả trả về là con trỏ `ResultSet` đứng trước dòng đầu tiên của bảng dữ liệu.
* **Dòng 17: `while (rs.next()) {`**
  * Hàm `rs.next()` di chuyển con trỏ xuống từng dòng tiếp theo trong bảng kết quả. Nếu còn dòng dữ liệu thì trả về `true` (chạy tiếp vòng lặp), nếu hết thì trả về `false` (kết thúc lặp).
* **Dòng 18 - 25: `list.add(new Course(...));`**
  * Bóc tách dữ liệu từng cột theo tên cột:
    * `rs.getInt("course_id")`: Lấy giá trị số nguyên.
    * `rs.getString("course_code")`: Lấy giá trị chuỗi văn bản.
  * Đóng gói vào đối tượng `Course` mới và nạp vào danh sách `list`.
* **Dòng 28: `e.printStackTrace();`**
  * In chi tiết lỗi ra console của NetBeans nếu có sự cố để lập trình viên dễ debug.
* **Dòng 30: `return list;`**
  * Trả danh sách môn học đã nạp đầy đủ về cho Servlet.

---

### 🔍 Hàm 2: `getCourseById(int id)` - Tìm môn học theo ID

```java
33:     public Course getCourseById(int id) {
34:         String sql = "SELECT course_id, course_code, course_name, description, badge, total_slots FROM COURSES WHERE course_id = ?";
35:         try {
36:             PreparedStatement ps = connection.prepareStatement(sql);
37:             ps.setInt(1, id);
38:             ResultSet rs = ps.executeQuery();
39:             if (rs.next()) {
40:                 return new Course(
41:                     rs.getInt("course_id"),
42:                     rs.getString("course_code"),
43:                     rs.getString("course_name"),
44:                     rs.getString("description"),
45:                     rs.getString("badge"),
46:                     rs.getInt("total_slots")
47:                 );
48:             }
49:         } catch (Exception e) {
50:             e.printStackTrace();
51:         }
52:         return null;
53:     }
```

* **Dòng 34: `WHERE course_id = ?`**
  * Dấu hỏi chấm `?` là một **Placeholder (giá trị giữ chỗ)**.
* **Dòng 37: `ps.setInt(1, id);`**
  * *Tại sao dùng cái này?* Thay vì cộng chuỗi `"WHERE course_id = " + id`, ta dùng `setInt(1, id)` để JDBC tự động kiểm tra kiểu dữ liệu và escape an toàn. Số `1` đại diện cho dấu hỏi chấm thứ nhất trong câu lệnh SQL.
* **Dòng 39: `if (rs.next()) {`**
  * Vì `course_id` là khóa chính (Primary Key), kết quả tìm được tối đa chỉ có 1 bản ghi duy nhất, do đó dùng `if` thay vì `while`.
* **Dòng 52: `return null;`**
  * Nếu không tìm thấy môn học nào khớp với ID (ví dụ truyền ID = 99999), hàm trả về `null`.

---

### 🔍 Hàm 3: `searchCourses(String keyword)` - Tìm kiếm môn học

```java
55:     public List<Course> searchCourses(String keyword) {
56:         List<Course> list = new ArrayList<>();
57:         String sql = "SELECT course_id, course_code, course_name, description, badge, total_slots FROM COURSES "
58:                    + "WHERE course_code LIKE ? OR course_name LIKE ?";
59:         try {
60:             PreparedStatement ps = connection.prepareStatement(sql);
61:             String term = "%" + keyword + "%";
62:             ps.setString(1, term);
63:             ps.setString(2, term);
64:             ResultSet rs = ps.executeQuery();
65:             while (rs.next()) {
66:                 list.add(new Course(...));
67:             }
68:         } catch (Exception e) {
69:             e.printStackTrace();
70:         }
71:         return list;
72:     }
```

* **Dòng 58: `WHERE course_code LIKE ? OR course_name LIKE ?`**
  * Tìm kiếm linh hoạt: Người dùng có thể gõ mã môn (ví dụ `PRJ`) hoặc tên môn (ví dụ `Web`) thì đều tìm ra được.
* **Dòng 61: `String term = "%" + keyword + "%";`**
  * Ký tự `%` trong SQL biểu thị tìm kiếm tương đối (chứa từ khóa ở bất kỳ vị trí nào: đầu, giữa, cuối).
* **Dòng 62 - 63: `ps.setString(1, term); ps.setString(2, term);`**
  * Gán từ khóa tìm kiếm an toàn vào cả 2 dấu hỏi chấm.

---

# PHẦN 3: TẦNG CONTROLLER - FILE `HomeServlet.java`
*Đường dẫn:* `src/main/java/com/fpt/aita/controller/HomeServlet.java`  
*Vai trò trong MVC:* **Controller** - Trực tiếp tiếp nhận request từ trình duyệt, kiểm tra xác thực phiên (Session), gọi DAO lấy dữ liệu và điều hướng tới View (`home.jsp`).

```java
1:  package com.fpt.aita.controller;
2:  
3:  import com.fpt.aita.dal.CourseDAO;
4:  import com.fpt.aita.model.Course;
5:  import com.fpt.aita.model.User;
6:  import java.io.IOException;
7:  import java.util.List;
8:  import javax.servlet.ServletException;
9:  import javax.servlet.annotation.WebServlet;
10: import javax.servlet.http.HttpServlet;
11: import javax.servlet.http.HttpServletRequest;
12: import javax.servlet.http.HttpServletResponse;
13: import javax.servlet.http.HttpSession;
14: 
15: @WebServlet(name = "HomeServlet", urlPatterns = {"/home", "/courses"})
16: public class HomeServlet extends HttpServlet {
17: 
18:     private CourseDAO courseDAO = new CourseDAO();
```

### 🔍 Giải thích dòng 15 - 18:
* **Dòng 15: `@WebServlet(name = "HomeServlet", urlPatterns = {"/home", "/courses"})`**
  * *Tại sao dùng annotation này?* Đây là chuẩn Servlet 3.0+. Nó giúp cấu hình ánh xạ đường dẫn URL trực tiếp trong code Java mà không cần phải khai báo thủ công lằng nhằng trong file `web.xml`. Người dùng gõ `/home` hoặc `/courses` trên trình duyệt thì Servlet này đều tiếp nhận.
* **Dòng 16: `public class HomeServlet extends HttpServlet`**
  * Bắt buộc kế thừa `HttpServlet` để class này có thể hoạt động như một Web Controller, có khả năng xử lý các giao thức HTTP (GET, POST).
* **Dòng 18: `private CourseDAO courseDAO = new CourseDAO();`**
  * Khởi tạo đối tượng DAO dùng chung cho toàn bộ Servlet để truy xuất CSDL môn học.

---

### 🔍 Phương thức `doGet()` - Xử lý hiển thị trang chủ

```java
20:     @Override
21:     protected void doGet(HttpServletRequest request, HttpServletResponse response)
22:             throws ServletException, IOException {
23:         HttpSession session = request.getSession();
24:         User currentUser = (User) session.getAttribute("currentUser");
25: 
26:         // 1. Kiểm tra trạng thái đăng nhập
27:         if (currentUser == null) {
28:             response.sendRedirect("login");
29:             return;
30:         }
31: 
32:         // 2. Xử lý tìm kiếm nếu có
33:         String search = request.getParameter("search");
34:         List<Course> list;
35:         if (search != null && !search.trim().isEmpty()) {
36:             list = courseDAO.searchCourses(search.trim());
37:         } else {
38:             list = courseDAO.getAllCourses();
39:         }
40: 
41:         // 3. Đóng gói dữ liệu sang View
42:         request.setAttribute("courseList", list);
43:         request.setAttribute("searchKeyword", search);
44: 
45:         // 4. Chuyển tiếp tới giao diện home.jsp
46:         request.getRequestDispatcher("home.jsp").forward(request, response);
47:     }
48: }
```

* **Dòng 20 - 22: `protected void doGet(...)`**
  * Xử lý request dạng `GET` khi người dùng truy cập trang web bằng cách gõ URL hoặc bấm vào liên kết link.
* **Dòng 23: `HttpSession session = request.getSession();`**
  * Lấy phiên làm việc hiện tại của trình duyệt người dùng.
* **Dòng 24: `User currentUser = (User) session.getAttribute("currentUser");`**
  * Ép kiểu lấy đối tượng người dùng đang đăng nhập từ Session ra.
* **Dòng 27 - 30: `if (currentUser == null) { response.sendRedirect("login"); return; }`**
  * *Tại sao phải kiểm tra?* Đây là logic bảo mật quan trọng: Nếu người dùng chưa đăng nhập mà cố tình gõ link `/home`, hệ thống sẽ từ chối và "đá" ngay về trang đăng nhập `login`. Lệnh `return;` bắt buộc phải có để dừng thực thi toàn bộ code bên dưới.
* **Dòng 33: `String search = request.getParameter("search");`**
  * Đọc từ khóa người dùng gõ trên ô tìm kiếm môn học (`<input name="search">`).
* **Dòng 35 - 39:**
  * Nếu người dùng có gõ từ khóa tìm kiếm $\rightarrow$ Gọi `searchCourses()`.
  * Nếu không tìm kiếm (vào trang bình thường) $\rightarrow$ Gọi `getAllCourses()` để lấy đủ 4 môn học.
* **Dòng 42: `request.setAttribute("courseList", list);`**
  * *DÒNG QUAN TRỌNG NHẤT CỦA BACKEND:* Gắn danh sách môn học vào biến `courseList` trong phạm vi Request. Nhờ dòng này mà TV 4 ở file `home.jsp` mới có thể gọi `<c:forEach items="${courseList}" var="c">` để hiển thị các Card môn học lên giao diện!
* **Dòng 43: `request.setAttribute("searchKeyword", search);`**
  * Giữ lại từ khóa tìm kiếm trên ô input để người dùng biết mình vừa tìm cái gì.
* **Dòng 46: `request.getRequestDispatcher("home.jsp").forward(request, response);`**
  * *Tại sao dùng `forward` mà không dùng `sendRedirect`?* 
    * `sendRedirect`: Bắt trình duyệt gửi request mới, toàn bộ dữ liệu trong `request.setAttribute` sẽ bị xóa sạch!
    * `forward`: Chuyển tiếp nội bộ trên server, **giữ nguyên toàn bộ dữ liệu** của request hiện tại để file `home.jsp` đọc được dữ liệu `courseList`.

---

# 🎤 BÍ KÍP VẤN ĐÁP KHI GIẢNG VIÊN HỎI VÀO PHẦN CODE CỦA BẠN:

> ❓ **Thầy/Cô hỏi:** *"Em là TV 2, em phụ trách phần nào trong nhóm?"*  
> 🗣️ **Bạn trả lời:** *"Dạ thưa thầy/cô, em phụ trách toàn bộ module Quản lý Môn học (Course) và xây dựng Controller Trang chủ EduNext. Cụ thể em thiết kế Model `Course.java`, xây dựng tầng `CourseDAO.java` lấy dữ liệu 4 môn học từ SQL Server và viết `HomeServlet.java` quản lý phân phối dữ liệu sang View `home.jsp` cho bạn TV4 hiển thị ạ."*

> ❓ **Thầy/Cô hỏi:** *"Tại sao trong `CourseDAO` em lại dùng `PreparedStatement` thay vì `Statement` thường?"*  
> 🗣️ **Bạn trả lời:** *"Dạ, `PreparedStatement` có 2 ưu điểm vượt trội: thứ nhất là nó được SQL Server tiền biên dịch giúp tăng tốc độ thực thi khi gọi nhiều lần; thứ hai là nó sử dụng cơ chế tham số hóa (Parameterized Query) với các Placeholder `?`, giúp ngăn chặn 100% các cuộc tấn công SQL Injection do dữ liệu độc hại được tách biệt hoàn toàn khỏi câu lệnh SQL ạ."*

> ❓ **Thầy/Cô hỏi:** *"Tại sao trong `HomeServlet` dòng 46 em lại dùng `forward` mà không dùng `sendRedirect`?"*  
> 🗣️ **Bạn trả lời:** *"Dạ thưa thầy/cô, ở dòng 42 em vừa dùng `request.setAttribute("courseList", list)` để đóng gói danh sách môn học vào phạm vi Request. Nếu em dùng `sendRedirect`, trình duyệt sẽ tạo một Request hoàn toàn mới khiến danh sách môn học bị mất trắng. Em dùng `forward` để chuyển tiếp nội bộ trên server, giúp trang `home.jsp` nhận được trọn vẹn dữ liệu từ Servlet để render ra giao diện ạ."*
