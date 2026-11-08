# Driver chấm một file Java

Áp migration `database/migrations/004_trusted_grading_driver.sql` lên PostgreSQL trước khi dùng app mới (bản SQL Server tương ứng nằm trong `database/sqlserver/migrations/`). Không thay đổi file_path hoặc bài nộp cũ. Chạy lại migration an toàn.

Giảng viên sở hữu bài tập mở “Cấu hình test case”, để Main class là `Main`, dán nội dung `Main.java` ở thư mục này vào ô driver và nhập stdin/expected như bên dưới. Lưu rồi chọn “Chấm lại” bài nộp .java. Sinh viên không được sửa/lấy driver qua form cấu hình; ZIP giữ main của bài nộp, không ghép driver. Không có driver cho file .java thì NOT_CONFIGURED, không gán điểm 0.

Driver mẫu giả định bài nộp công bố API:

```java
public class MyBST {
    public void insert(int value);
    public boolean search(int value);
    public void delete(int value);
    public String inorder(); // số tăng dần, cách nhau một dấu cách, không dấu cách cuối
    public int height();    // cây rỗng = 0, một nút = 1
}
```

Các khai báo trên mô tả hợp đồng, không phải file Java có thể biên dịch. Cần điều chỉnh driver nếu bài MyBST thật dùng chữ ký khác, in trực tiếp khi duyệt, hoặc định nghĩa height khác; hệ thống không tự đoán. Không đặt đáp án/bí mật/token trong driver: mã này chạy cùng code sinh viên, sinh viên có thể đọc source/class trong sandbox. Đáp án so sánh và trọng số chỉ ở JVM tin cậy; container chỉ nhận nguồn và stdin của test hiện tại.

| Test | stdin | expected stdout |
| --- | --- | --- |
| Cây rỗng | `height search 7` | `0` rồi `false`, mỗi giá trị một dòng |
| Thêm/tìm/duyệt | `insert 5 insert 3 insert 7 insert 3 search 3 search 9 inorder height` | `true` rồi `false` rồi `3 5 7` rồi `2` |
| Xóa gốc có hai con | `insert 5 insert 3 insert 7 delete 5 inorder search 5 height` | `3 7` rồi `false` rồi `2` |

Quy ước mẫu: khóa trùng bị bỏ qua, xóa khóa không có giữ nguyên cây. Nhập expected bằng xuống dòng thật.

Mã sinh viên được đọc UTF-8 có giới hạn; tên compiler lấy theo public type trong AST thay vì prefix lưu file do server thêm. Main/kiểu trợ giúp của driver trong cùng package được bảo vệ; không cho ghi đè file driver. Source lỗi cú pháp được chuyển tới javac trong Docker và trả COMPILE_ERROR, không chạy trên host. Driver và nguồn nằm trong ZIP/workspace tạm riêng, xóa khi xong.

Mỗi test chạy trong container mới với network none, RAM 128 MiB, CPU 0.5, PID 64, rootfs và source chỉ đọc, cap-drop ALL. Javac có 15 giây; JVM chạy có 5 giây riêng; watchdog host 23 giây bảo vệ hạ tầng. Compile timeout được ghi COMPILE_ERROR kèm thông báo; runtime timeout là TIME_LIMIT_EXCEEDED. execution_time_ms hiện đo toàn lượt start/compile/run, không chỉ thời gian Java. Vẫn biên dịch lại cho từng test: chia sẻ compiled artifact an toàn giữa các container cần giao thức staging/lifecycle riêng, nên chưa thêm trong phạm vi này. Tối đa 20 test có thể mất hơn 5 giây tổng cộng. Rebuild image: `docker build -t aita-sandbox:java8 docker/sandbox`.
