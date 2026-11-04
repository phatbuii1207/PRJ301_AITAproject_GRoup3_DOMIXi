# Biên bản kiểm tra Docker Sandbox

> Biên bản lịch sử trước khi đổi database. Hệ thống hiện dùng PostgreSQL; xem [kiểm chứng mới](POSTGRESQL_VERIFICATION.md).

Ngày kiểm tra: 2026-10-29. Thực hiện trên máy local, không push GitHub.

## Kiểm tra lại để chuẩn bị báo cáo — 2026-11-01

- Chạy lại `mvn verify -Psandbox-it`: 6 unit + 11 Docker integration PASS, không skip.
- `./scripts/demo-sandbox.sh all`: cả 7 ca PASS (hello, TLE, files, network,
  compiler error, ZIP traversal, output flood).
- Chạy TLE đồng thời gọi `./scripts/demo-sandbox.sh inspect`: xác nhận live
  Network=none, user sandboxuser, RAM/swap 134217728, NanoCPUs 500000000,
  read-only rootfs/mount, PID 64 và security options đúng.
- Không còn container sandbox sau khi chạy. Lượt này không sử dụng/sửa database
  và không khởi động Tomcat; kiểm tra HTTP bên dưới là kết quả ngày 2026-10-29.
- Log demo nằm ở `target/sandbox-demo/results/`, gồm cả `live-inspect.txt`;
  ZIP/source mẫu ở `target/sandbox-demo/`. Cách trình bày và giải thích code ở
  [hướng dẫn báo cáo](HUONG_DAN_BAO_CAO_SANDBOX.md).

## Môi trường

- Docker Engine 29.9.0, Linux cgroup v2.
- Sandbox image build từ `eclipse-temurin:8-jdk-alpine`, Java runtime 1.8.0,
  user `sandboxuser`.
- Maven 3.9.9 / OpenJDK 17; class của service có major version 52 (Java 8).
- Kiểm tra HTTP bằng Tomcat 9.0.122 tạm, bind riêng `127.0.0.1:18881`.

## Kết quả

| Kiểm tra | Kết quả |
| --- | --- |
| `mvn verify -Psandbox-it` | 17 test PASS: 6 ZIP/unit + 11 Docker integration, không skip |
| Chạy lại 6 unit test trong chính JVM Java 8 | PASS |
| ZIP traversal, absolute path, tên file chèn compiler options, duplicate normalize, ZIP bomb, quá số entry | Bị từ chối |
| Chạy Java 8, package, thư mục có khoảng trắng và stdin | PASS |
| Inspect container khi chạy | Network none, RAM/swap 134217728, NanoCPUs 500000000, non-root, rootfs/source read-only, PID 64, cap-drop ALL, no-new-privileges |
| Vòng lặp vô hạn | TIME_LIMIT_EXCEEDED; container được xóa |
| Xóa source/ghi rootfs | Bị chặn; ZIP gốc không đổi; không có Docker socket |
| TCP tới IP bên ngoài | NETWORK_BLOCKED |
| Compiler error, System.exit(65), heap OOM | Phân loại lỗi; không trả SUCCESS |
| Output flood | OUTPUT_LIMIT_EXCEEDED, output bị giới hạn, container được xóa |
| Thiếu image | SANDBOX_UNAVAILABLE, không chạy Java trên host |
| Cancel/interrupt | Worker dừng, container được xóa, slot được giải phóng |
| Ba job đồng thời | Chỉ hai job chạy, job thứ ba SANDBOX_BUSY |

## Luồng HTTP thực tế

Đăng nhập tài khoản sinh viên demo, tạo bài tập test riêng với deadline tương lai,
nộp multipart ZIP qua `/course`, đi theo redirect và kiểm tra HTML/SQL Server:

- Bài in `<script>alert(1)</script>`: `SUCCESS`, DB `Completed`, HTTP khoảng 1.57 giây.
  Output hiển thị thành `&lt;script&gt;...`, không có script thô từ chương trình sinh viên.
- Bài vòng lặp vô hạn: `TIME_LIMIT_EXCEEDED`, DB `Failed`, HTTP khoảng 5.48 giây
  gồm cả tạo container và cleanup.
- Đã xóa bài tập/bài nộp test và các file upload tạo bởi test. Số bản ghi hai bảng
  trở lại số lượng ban đầu. Tomcat tạm đã dừng; không còn container có label sandbox.

Báo cáo máy đọc được nằm trong `target/surefire-reports/`,
`target/failsafe-reports/`, `target/sandbox-http-verification.json` (gitignored).
WAR đã build trong `target/`; chưa deploy thay bản ứng dụng NetBeans của người dùng.

## Giới hạn của kết quả kiểm tra

SUCCESS chỉ xác nhận compiler và chương trình exit 0, chưa so expected output.
Luồng upload dùng main class chung mặc định `Main`, stdin rỗng; không có queue hoặc
lưu verdict chi tiết dài hạn trong DB. Đã kiểm thử heap OOM; không khẳng định mọi
exit 137 là cgroup OOM. Chưa có watchdog riêng nếu JVM Tomcat chết đột ngột và chưa
thực hiện kiểm thử container escape/kernel exploit. Xem
[hướng dẫn vận hành](DOCKER_SANDBOX.md) để biết giới hạn và xử lý cleanup lỗi.
