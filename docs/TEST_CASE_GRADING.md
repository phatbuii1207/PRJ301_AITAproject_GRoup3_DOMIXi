# Chấm điểm Java bằng test case

Bộ chấm hoạt động độc lập với API key/AI. Java của sinh viên chạy trong Docker;
đáp án, trọng số, phép so sánh và điểm nằm ở backend. Không mount bộ đáp án vào container.

## Cài đặt

1. Trong đúng database PostgreSQL của ứng dụng, chạy
   `database/migrations/001_test_case_grading.sql` sau khi đã có schema AITA gốc.
   Script chỉ thêm 4 bảng, có transaction và có thể chạy lại; không xóa dữ liệu cũ.
   Chạy thêm `database/migrations/002_ai_new_complexity.sql` cho database cũ để
   tương thích cột new_complexity mà module AI của nhóm đang sử dụng.
2. Có sẵn image: `docker build -t aita-sandbox:java8 docker/sandbox`.
3. Build `mvn verify -Psandbox-it` và deploy WAR mới lên Tomcat 9.

Máy hiện tại dùng Maven tại
`/home/phambuuthinh/.cache/csd-tools/apache-maven-3.9.9/bin/mvn`.
Bốn bảng chấm đã nằm trong AITA_Database.sql PostgreSQL. Các kiểm tra SQL Server trước đây được lưu như lịch sử. Chưa kết nối
hay thay đổi database Azure của nhóm; người triển khai cần chạy migration ở đó.
File ZIP vẫn lưu theo đường dẫn upload local trên máy Tomcat hiện tại.

## Cách sử dụng

- Giảng viên tạo bài tập, mở **Test case** dưới tiêu đề bài tại trang môn học.
- Chỉ giảng viên tạo bài được sửa barem và yêu cầu chấm lại.
- Cấu hình main class (`Main`, hoặc tên đầy đủ như `demo.Main`).
- Thêm 1–20 test: tên, stdin, stdout kỳ vọng, trọng số và chế độ công khai/ẩn.
- Trọng số phải > 0, tối đa 1000, tối đa 2 chữ số thập phân. Mỗi input/đáp án
  tối đa 64 KiB. Các test được chạy theo thứ tự lưu trong form.
- Sinh viên nộp ZIP Java như trước. Upload trả về ngay và chuyển đến
  `/grading-result?submissionId=...`; trang tự cập nhật khi job còn hoạt động.
- Điểm và từng verdict được lưu DB, nên có thể mở lại sau khi đóng trang/restart.
- Mở lại kết quả từ link **Điểm test case/Kết quả test** trên trang môn học.
- Giảng viên chọn **Chấm lại** để dùng cấu hình test mới nhất cho bài đã nộp.
  Lưu barem không tự chấm lại mọi bài cũ.

Trang `/grading-tests?assignmentId=...` cho sinh viên cùng môn chỉ hiển thị test
công khai. Trang kết quả chỉ cho sinh viên xem bài của mình; giảng viên tạo bài
hoặc được phân công môn học có thể xem kết quả. Các POST cấu hình/chấm lại cần CSRF token.

## Quy tắc chấm

```text
Từng test: source + stdin -> Docker -> stdout và stderr riêng
Nếu chạy thành công: so stdout với đáp án -> ACCEPTED hoặc WRONG_ANSWER
Điểm = 10 × tổng trọng số test ACCEPTED / tổng trọng số tất cả test
```

Làm tròn điểm đến 2 số thập phân, HALF_UP. Ví dụ 5 test cùng trọng số 2:
đạt 4 test được 8.00/10. Nếu trọng số 2 và 1, chỉ đạt test trọng số 2 thì được 6.67/10.

Comparator đổi CRLF/CR thành LF, bỏ dấu cách/tab/xuống dòng ở cuối **toàn bộ output**.
Giữ nguyên khoảng trắng đầu, khoảng trắng ở giữa và cuối các dòng nội bộ; không
so dạng token, không tự bỏ lời dẫn và chưa hỗ trợ sai số số thực. Ví dụ:

| Output thực tế | Đáp án | Kết quả |
| --- | --- | --- |
| `5\r\n` | `5` | Đạt |
| `5 \t\n` | `5` | Đạt |
| ` 5` | `5` | Sai |
| `Kết quả: 5` | `5` | Sai |
| `5.0001` | `5` | Sai |

Chỉ stdout tham gia so sánh. Stderr được giữ làm diagnostics. Tổng stdout+stderr
của mỗi lần sandbox vẫn bị giới hạn chung 64 KiB, nên flood stderr cũng bị dừng.

| Verdict | Điểm/ứng xử |
| --- | --- |
| ACCEPTED | Test chạy thành công và khớp đáp án; nhận trọng số test |
| WRONG_ANSWER | Exit 0 nhưng đáp án khác; test 0 điểm |
| COMPILE_ERROR | Test 0 điểm, dừng các test còn lại và đánh dấu NOT_RUN |
| INVALID_SUBMISSION | ZIP không hợp lệ; dừng, các test sau NOT_RUN |
| RUNTIME_ERROR / TIME_LIMIT_EXCEEDED / OUTPUT_LIMIT_EXCEEDED | Test 0 điểm; vẫn thử test kế tiếp trong container mới |
| INFRASTRUCTURE_ERROR | Docker bận/mất kết nối/thiếu image/hủy job: dừng; **không công bố điểm số**, kể cả đã đạt một số test |
| NOT_CONFIGURED | Chưa có bộ test: chưa chạy, không gán điểm 0 |

Overall là ACCEPTED nếu đạt tất cả, PARTIAL nếu đạt một phần; nếu không đạt test nào,
dùng verdict đầu tiên. Khi chấm hoàn tất, SUBMISSIONS.status là Completed kể cả
điểm thấp/compile error: đây là trạng thái hoàn tất công việc, không phải verdict đúng/sai.
Lỗi hạ tầng là Failed với score NULL; chưa cấu hình là Pending với score NULL.

## Kết nối với sandbox và hàng đợi

`CourseDetailServlet` chỉ lưu ZIP + SHA-256 và enqueue submission ID. Không chạy
chấm toàn bộ test trong thread HTTP. Worker FIFO dùng queue có sức chứa 100 job,
không nhận trùng ID đang chờ/chấm. Mỗi thao tác DB mở connection riêng rồi đóng.

Worker lấy barem từ database, kiểm tra đường dẫn ZIP trong upload root và đối chiếu
SHA-256, sau đó gọi `GradingService`. File bị đổi hoặc không có trên host sẽ không
được tính điểm. Listener khởi tạo worker theo đường dẫn upload của Tomcat và ngắt
worker khi ứng dụng dừng. Sau khi lưu điểm test case, worker tiếp tục pipeline
AST/AI của nhóm qua SubmissionAnalysisService; kết quả nằm trong các trang báo cáo
AST/AI riêng. Lỗi phân tích bổ sung không ghi đè điểm chức năng đã chấm.
ZIP được kiểm tra giới hạn trước khi đi qua bộ parser/extractor trên host. File
`.java` đơn lẻ vẫn đi qua AST/AI và không được gán điểm test case (cần ZIP để chấm).

Bản này tái sử dụng API sandbox đang có: **mỗi test dùng container mới và biên dịch
lại source**, tránh giữ trạng thái của test trước. Mỗi lần có deadline 5 giây gồm
startup + compile + run. Không phải 5 giây cho toàn bộ bộ test; 20 test chạy vô hạn
có thể tốn khoảng 100 giây cộng chi phí chuẩn bị/dọn dẹp. Queue xử lý nền nên HTTP
không đợi toàn bộ thời gian đó. Có thể tối ưu compile một lần, chạy nhiều container
bằng artifact chỉ đọc trong phiên bản sau.

Queue ở RAM: restart sẽ mất job đang chờ; ID/điểm đã lưu còn trong DB, nhưng các bài
Pending/Processing bị gián đoạn cần giảng viên chấm lại. Chưa có queue phân tán,
retry tự động hoặc watchdog độc lập nếu host JVM chết đột ngột.

## Bảo mật test ẩn và lưu kết quả

- Cấu hình/đáp án chỉ được đọc trong JVM backend, không ghi vào ZIP/container.
- Mỗi test chạy trong container mới; không chia sẻ tmpfs giữa các test.
- Sinh viên không nhận input, expected output, actual output, diagnostics hoặc tên
  riêng của test ẩn trong request attributes/HTML. Backend tạo report đã redacted
  trước khi chuyển cho JSP; chỉ công bố thứ tự, verdict, trọng số và thời gian.
- Tất cả tên/input/đáp án/output trong JSP mới được escape qua `c:out`.
- Kết quả lưu snapshot tên/input/đáp án/trọng số/visibility và phiên bản barem của
  lần chấm. Sửa cấu hình không thay đổi report cũ. Chấm lại thay report hiện tại
  bằng snapshot mới; chưa lưu lịch sử mọi lần chấm.
- Lưu config/report có transaction. Version barem chống một form cũ ghi đè thay
  đổi mới của người khác. Lỗi DB không được biến thành điểm số giả.

## Demo bài cộng hai số

```bash
python3 scripts/prepare-grading-demo.py
```

ZIP/source mẫu tại `target/grading-demo/`. File `teacher-test-bank.txt` ở ngoài ZIP
để giảng viên cấu hình, không upload file đáp án cùng source.

Tạo bài có deadline tương lai, main class Main; thêm 5 test cùng trọng số 2:

| Input | Expected | Hiển thị |
| --- | --- | --- |
| `2 3` | `5` | Công khai |
| `-2 5` | `3` | Ẩn |
| `0 0` | `0` | Ẩn |
| `-4 -6` | `-10` | Ẩn |
| `1000000 2000000` | `3000000` | Ẩn |

- `correct-sum.zip`: 5/5, 10.00/10.
- `hardcoded-five.zip`: 1/5, 2.00/10, chứng minh exit 0 chưa đủ để đạt điểm.
- `compile-error.zip`: COMPILE_ERROR, 0.00/10, test sau NOT_RUN.
- `infinite-loop.zip`: mỗi test TLE, khoảng 25 giây cho 5 test rồi 0.00/10.

## Kiểm chứng 2026-11-04

`mvn verify -Psandbox-it`: **36 test PASS** (21 unit, 15 Docker integration), không skip.
21 unit test cũng PASS khi chạy trực tiếp trên JVM Java 8 trong image sandbox.
Các test mới kiểm tra trọng số/làm tròn, comparator, stderr riêng, compile-stop,
timeout/runtime, score NULL khi hạ tầng lỗi và redaction. Docker thật kiểm tra cộng
hai số, lời giải hardcode, compiler error và TLE.

Tomcat 9.0.122 tạm trên localhost đã được kiểm tra qua HTTP:

- Lưu barem, public/hidden, CSRF, phân quyền chủ bài và chống ghi phiên bản cũ.
- Upload chấm đúng 10.00, hardcode 2.00, compiler error 0.00.
- Không cấu hình test: NOT_CONFIGURED, không có điểm.
- Người khác không xem được kết quả; HTML sinh viên không có dữ liệu test ẩn.
- Snapshot phiên bản 1 không đổi khi lưu barem phiên bản 2; chấm lại dùng phiên bản 2.
- Lượt chấm bị gián đoạn không hiển thị điểm cũ như điểm của lượt mới.
- Output chứa script được escape; file bị đổi hash trả GRADING_ERROR, score NULL.

Tài khoản, bài tập, bài nộp và file test HTTP đã dọn; số bản ghi phục hồi. Server
test đã dừng. Log tóm tắt: `target/grading-http-verification.json`; JUnit XML ở
`target/surefire-reports/` và `target/failsafe-reports/`. Chưa thử trên Azure SQL thật.
