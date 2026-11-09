# Nộp thử và chấm MyBST.java

Bộ này dùng để demo chức năng chấm test case thật. File nộp mẫu là `MyBST.java` trong cùng thư mục, Java 8, không cần `main()`, không cần ZIP. Mã này là lời giải mẫu đúng để kiểm tra hệ thống; không dùng làm bài đánh giá độc lập cho sinh viên.

## 1. Chuẩn bị ứng dụng — người chạy server

1. Cấu hình kết nối PostgreSQL trong `.env` riêng như hướng dẫn dự án; không gửi mật khẩu/API key cho bạn học. Đảm bảo database đã có schema và các bảng chấm bài từ migration001.
2. Với database đã có dữ liệu, áp lần lượt `database/migrations/003_rubric_type_and_ll_rules.sql` và `database/migrations/004_trusted_grading_driver.sql`. Nếu tạo database mới bằng schema/seed hiện tại, vẫn áp migration004. Không chạy lại seed trên database đang dùng.
3. Build lại sandbox từ gốc repository:
   ```bash
   docker build -t aita-sandbox:java8 docker/sandbox
   ```
4. Chọn `AITA_UPLOAD_DIR` ngoài webroot, tài khoản chạy Tomcat phải có quyền ghi. Để trống thì mặc định `~/aita-uploads/submissions`. Docker phải truy cập được thư mục tạm của ứng dụng. Build/deploy lại ứng dụng bằng NetBeans/Tomcat9 như dự án hiện tại. Khi demo không cần AI thật, có thể chọn `AI_PROVIDER=mock` trong cấu hình riêng rồi khởi động lại; điểm test case vẫn đến từ chạy code thật.

## 2. Giảng viên tạo bài và cấu hình bộ test

1. Đăng nhập giảng viên phụ trách môn; tạo bài tập mới có deadline tương lai, chọn rubric **BST**. Sinh viên dùng để thử phải được ghi danh trong môn đó.
2. Mở trang cấu hình test case của bài (`/grading-tests?assignmentId=<ID>` trên context ứng dụng). Người cấu hình cần là giảng viên sở hữu bài tập.
3. Điền **Main class: `Main`**. Dán toàn bộ nội dung [driver Main.java](../grading-driver/Main.java) vào ô **Driver Java**. Đây là mã giảng viên cấu hình, không phải file sinh viên nộp.
4. Tạo 8 dòng test theo bảng dưới. Mỗi dòng có trọng số **1**. Mở file `.in` và `.out`, copy **nội dung** vào Input và Đáp án; không nhập tên file. Giữ xuống dòng thật; `true`/`false` viết thường.
5. Lưu bộ test. `test-cases.json` chứa cùng dữ liệu để tham khảo/viết công cụ; giao diện hiện tại nhập từng dòng, chưa hỗ trợ import JSON.

| Tên test | Input | Đáp án | Hiển thị |
| --- | --- | --- | --- |
| 01-empty | [01-empty.in](test-cases/01-empty.in) | [01-empty.out](test-cases/01-empty.out) | Công khai |
| 02-insert-search | [02-insert-search.in](test-cases/02-insert-search.in) | [02-insert-search.out](test-cases/02-insert-search.out) | Công khai |
| 03-duplicates | [03-duplicates.in](test-cases/03-duplicates.in) | [03-duplicates.out](test-cases/03-duplicates.out) | Ẩn |
| 04-delete-leaf | [04-delete-leaf.in](test-cases/04-delete-leaf.in) | [04-delete-leaf.out](test-cases/04-delete-leaf.out) | Ẩn |
| 05-delete-one-child | [05-delete-one-child.in](test-cases/05-delete-one-child.in) | [05-delete-one-child.out](test-cases/05-delete-one-child.out) | Ẩn |
| 06-delete-root | [06-delete-root.in](test-cases/06-delete-root.in) | [06-delete-root.out](test-cases/06-delete-root.out) | Ẩn |
| 07-negative-skew | [07-negative-skew.in](test-cases/07-negative-skew.in) | [07-negative-skew.out](test-cases/07-negative-skew.out) | Ẩn |
| 08-delete-all | [08-delete-all.in](test-cases/08-delete-all.in) | [08-delete-all.out](test-cases/08-delete-all.out) | Ẩn |

Mỗi test chạy với cây mới. Quy ước: khóa trùng bỏ qua, xóa khóa không có không đổi cây, height cây rỗng0/một nút1, inorder tăng dần cách nhau một dấu cách. Điểm = 10 × số test đạt / 8; mỗi test tương đương1.25 điểm. Test “ẩn” chỉ ẩn trên giao diện sinh viên; vì đây là bộ demo public nên không dùng các đáp án này làm đề thi bí mật.

## 3. Bạn học nộp bài

1. Tải đúng [MyBST.java](MyBST.java) về máy, giữ tên `MyBST.java`, không đổi nội dung, không nộp `Main.java`.
2. Đăng nhập tài khoản sinh viên, vào môn/bài tập vừa cấu hình, chọn file và nộp trước deadline.
3. Chờ hàng đợi xử lý; nếu chuyển tới báo cáo AST/AI, mở **kết quả chấm test case** của bài nộp (`/grading-result?submissionId=<ID>`). Kỳ vọng **ACCEPTED, 8/8 test, 10.00/10**. Điểm AST/AI là báo cáo riêng, có thể khác10.
4. Giảng viên xem được đầy đủ test; sinh viên không thấy chi tiết test ẩn. Chủ bài hoặc giảng viên đúng môn tải lại được file; URL tĩnh `/uploads/submissions/...` phải bị chặn.

## 4. Thử trường hợp sai

Sao chép file mẫu ra thư mục khác rồi sửa `public boolean search(int value)` thành chỉ `return false;` (xóa phần thân cũ). Nộp bản sửa: kỳ vọng **PARTIAL, 6/8, 7.50/10** vì test02 và07 sai. Nộp lại bản gốc phải trở về10/10. Không sửa driver khi thử lỗi sinh viên.

Nếu có NOT_CONFIGURED: kiểm tra đã lưu driver và test. COMPILE_ERROR: kiểm tra API MyBST khớp driver và nộp đúng file. GRADING_ERROR/SANDBOX_UNAVAILABLE: kiểm tra Docker, image mới và log server. Java có5s chạy riêng, javac15s; hệ thống hiện biên dịch lại mỗi test nên toàn bộ8test có thể mất hàng chục giây. Sau khi sửa bộ test, giảng viên chọn “Chấm lại” để áp dụng cho bài đã nộp.

## 5. Kiểm tra bộ demo bằng Docker (tùy chọn)

Từ gốc repository, chạy `python3 scripts/verify_bst_demo.py`. Script chỉ ghép driver/file mẫu trong thư mục tạm và chạy Docker có giới hạn; không ghi database, không đọc `.env`. Kỳ vọng bản gốc8/8 và bản search sai6/8. Cần build image trước như bước1.

## Windows: build dừng ở Maven Surefire sau khi pull

Nếu `target/surefire-reports/*.txt` ghi `FileSystemException` / `A required privilege is not held by the client` tại `createSymbolicLink`, máy thiếu quyền tạo symlink cho fixture test. Bản sửa sau bộ demo tự đánh dấu riêng các test phụ thuộc symlink là skipped khi Windows không tạo được fixture; không cần chạy NetBeans bằng Administrator để build. Trên máy hỗ trợ symlink, các test bảo mật vẫn chạy đầy đủ. Pull bản mới rồi Clean and Build lại. Nếu vẫn fail, gửi file `.txt` có FAILURE/ERROR trong `target/surefire-reports`; dòng BUILD FAILURE cuối không đủ xác định nguyên nhân. Không tắt toàn bộ test để che lỗi.
