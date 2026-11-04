# Docker Sandbox — AITA / PRJ301

Module chạy mã Java không tin cậy bên trong Docker, tách khỏi JVM Tomcat.
Không có đường chạy dự phòng bằng Java trên host khi Docker lỗi.

## Chuẩn bị

- Host Linux, Docker Engine local đang chạy, hỗ trợ cgroup memory / swap / CPU / PID.
- Tomcat 9, Java 8 trở lên cho webapp; chương trình sinh viên luôn chạy Java 8 trong image.
- Maven 3; image sandbox được build trước, không tải image khi nhận bài.
- Tài khoản vận hành Tomcat có quyền gọi Docker. Quyền Docker daemon rất mạnh:
  máy demo phải được quản lý như một grading host tin cậy. Không mount Docker socket
  vào sandbox. Container chia sẻ kernel host, không phải ranh giới VM cho dịch vụ public nhiều tenant.
- Bind mount được giải quyết trên máy Docker daemon: cấu hình hiện tại yêu cầu daemon
  và thư mục tạm của Tomcat nằm trên cùng host. Không dùng remote `DOCKER_HOST`.

Từ thư mục project:

```bash
docker build -t aita-sandbox:java8 docker/sandbox
mvn clean verify -Psandbox-it
```

Nếu `mvn` chưa có trong PATH trên máy hiện tại, binary sẵn có ở:
`/home/phambuuthinh/.cache/csd-tools/apache-maven-3.9.9/bin/mvn`.

`mvn test` chỉ chạy kiểm tra ZIP không cần Docker. Profile `sandbox-it` chạy test
Docker thật và **fail** nếu chưa build image, không tự bỏ qua test.
Sau build, deploy WAR `target/AITA_StaticCodeAnalysis-1.0-SNAPSHOT.war` theo quy trình
Tomcat/NetBeans của nhóm; thay đổi source không tự cập nhật ứng dụng đang deploy.

## Luồng thực thi

1. `CourseDetailServlet` lưu bài nộp, tính SHA-256 theo luồng sẵn có.
2. Bài `.zip` được đưa vào `DockerSandboxService.execute(Path)`.
3. Service dành một trong hai slot dùng chung trong webapp, giải nén vào staging riêng.
4. Chỉ file `.java` được chép vào staging; service tạo manifest javac của riêng mình.
   Không chạy script, Maven, Gradle, JAR hoặc annotation processor do sinh viên cung cấp.
5. `docker create --rm ...` tạo container có tên UUID và toàn bộ cờ bảo mật;
   `docker start --attach --interactive` thu stdout/stderr. Hai bước tương đương
   cấu hình bảo mật của `docker run --rm ...`, nhưng giúp xác định container trước
   khi theo dõi tiến trình CLI và timeout.
6. Image biên dịch vào `/tmp/classes`, sau đó chạy main class.
7. Sau 5 giây kể từ khi bắt đầu attach/start, service trả `TIME_LIMIT_EXCEEDED`
   và gọi `docker rm --force <tên>` trong `finally`. Giới hạn 5 giây **bao gồm startup,
   javac và chạy main**; giải nén và create có deadline riêng 5 giây mỗi bước.
   Cleanup có ngân sách thêm tối đa khoảng 2 giây; thời gian HTTP tổng không phải 5 giây.
8. Container và staging được dọn ở cả nhánh thành công, lỗi, timeout, output flood,
   hoặc thread bị interrupt. `--rm` tự xóa container đã dừng, không tự dừng vòng lặp.
   Nếu không xác nhận được cleanup do daemon lỗi, service khóa các lần chạy tiếp theo,
   ghi tên container vào server log và giữ staging để quản trị viên xử lý.

`ProcessBuilder` nhận danh sách đối số, không ghép shell command từ đầu vào.
Docker stdin được ghi riêng, stdout/stderr được đọc riêng ngay khi tiến trình chạy
để tránh đầy pipe và deadlock. Output giữ tối đa 64 KiB; vượt giới hạn sẽ dừng job.
Docker logging driver `none` tránh output flood làm đầy log của daemon.

## Giới hạn và cờ bảo mật

| Cấu hình | Mục đích |
| --- | --- |
| `eclipse-temurin:8-jdk-alpine`, user `sandboxuser` UID/GID 10001 | Java 8, chạy non-root |
| `--network none` | Không có interface ra ngoài; loopback riêng trong container vẫn tồn tại |
| `--memory 128m --memory-swap 128m` | Cgroup RAM 128 MiB, không cho thêm swap |
| `--cpus 0.5` | CPU quota tương đương nửa một core, không phải giới hạn 50% toàn bộ host |
| `--volume <staging>:/workspace:ro` | Chỉ mount source của đúng bài nộp, read-only |
| `--read-only` | Root filesystem của image cũng read-only |
| `--tmpfs /tmp:rw,noexec,nosuid,nodev,size=32m,mode=1777` | Nơi compiler ghi class/tệp tạm, có giới hạn; tmpfs cũng tiêu tốn RAM cgroup |
| `--cap-drop ALL --security-opt no-new-privileges:true` | Bỏ capabilities, chặn nâng đặc quyền qua exec |
| `--pids-limit 64 --ulimit nofile=128:128` | Giới hạn tiến trình/thread và descriptor |
| `--rm --pull never` | Container tự xóa khi dừng; image phải có sẵn |

Giữ seccomp mặc định Docker, không dùng `--privileged`, host PID/network hay mount
thư mục project, `.env`, cấu hình Tomcat hoặc socket host vào sandbox.
Một lời gọi `Runtime.exec` vẫn có thể chạy chương trình **bên trong container**;
mục tiêu là giới hạn tác động bằng namespace, quyền filesystem và cgroup.

JVM dùng heap tối đa 48 MiB, metaspace runtime tối đa 32 MiB và code cache 16 MiB,
chừa RAM cho JVM native/stack/compiler. 128 MiB là RAM toàn container, không phải heap Java.
Các project quá lớn có thể compile error/OOM/TLE; không tự nâng hạn mức để chạy chúng.

ZIP tối đa 50 MiB, tối đa 1000 entry/200 file Java, tổng dữ liệu giải nén tối đa
16 MiB (tính cả file bị bỏ qua). Từ chối đường dẫn tuyệt đối, `..`, backslash,
đường dẫn trùng sau normalize và ký tự ngoài tập tên file được hỗ trợ.
Cho phép thư mục có dấu cách; tên file/thư mục Unicode chưa được hỗ trợ.
Không materialize symlink từ metadata ZIP: chỉ tạo file nguồn thông thường.

## Main class, stdin và kết quả

Mặc định cần `public static void main(String[] args)` trong `Main.java` không package.
ZIP có thể chứa thư mục bọc bên ngoài và nhiều file Java.

- `AITA_SANDBOX_MAIN_CLASS=demo.Main`: cấu hình phía server trước khi chạy Tomcat.
- `AITA_SANDBOX_IMAGE=aita-sandbox:java8`: đổi tên image đã build nếu cần.
- API `execute(Path, String, byte[])`: hỗ trợ main class và stdin (tối đa 64 KiB)
  từ cấu hình bài tập tin cậy. Form sinh viên không được nhập Docker flags hoặc lệnh.
- Luồng upload hiện tại dùng main class chung và stdin rỗng; chưa có cấu hình main/stdin
  riêng cho từng assignment hoặc bộ so sánh expected output.

`SandboxResult`: `isSuccess()`, `status`, `output`, `executionTimeMs`.

| Status | Ý nghĩa |
| --- | --- |
| `SUCCESS` | Compiler và chương trình kết thúc exit 0; **chưa xác nhận đúng đáp án** |
| `COMPILE_ERROR` | javac thất bại |
| `RUNTIME_ERROR` | Main không tồn tại, exception, exit khác 0, JVM OOM hoặc container bị kill |
| `TIME_LIMIT_EXCEEDED` | Service hết deadline 5 giây |
| `OUTPUT_LIMIT_EXCEEDED` | Output vượt 64 KiB |
| `INVALID_SUBMISSION` | ZIP, main class hoặc stdin không hợp lệ |
| `SANDBOX_BUSY` | Hai slot đang bận; người dùng cần thử nộp lại, hiện chưa có queue |
| `SANDBOX_UNAVAILABLE` | Thiếu image/Docker, lỗi hạ tầng, hoặc sandbox đang khóa do cleanup lỗi |
| `INTERNAL_ERROR` | Tác vụ bị hủy hoặc không xác nhận được cleanup |

Không khẳng định mọi exit 137 là memory limit: `--rm` khiến thông tin inspect sau
khi dừng có thể không còn, và exit code không chứng minh nguyên nhân bị kill.
Mã thoát của chương trình Java được wrapper quy về runtime error, tránh nhầm
`System.exit(65)` thành lỗi compiler.

Trang nộp bài hiển thị status/thời gian/output; output được escape bằng `c:out`.
DB giữ trạng thái coarse có sẵn (`Completed`/`Failed`), thêm giá trị `Pending` cho lỗi
hạ tầng hoặc sandbox bận, tất cả vừa cột varchar(20). Không đổi schema SQL Server.
Chi tiết sandbox chỉ hiển thị ở flash sau POST, chưa lưu vĩnh viễn vào database.
Các loại tệp khác vẫn được lưu theo luồng cũ và được thông báo chưa chạy sandbox.

## Demo bảo vệ và bằng chứng

Ba chương trình bắt buộc nằm trong `src/test/resources/sandbox/`:

| Demo | Kết quả cần thấy |
| --- | --- |
| `tle/Main.java`: vòng lặp vô tận | `TIME_LIMIT_EXCEEDED`, container không còn |
| `file-deletion/Main.java`: xóa `/workspace/Main.java`, ghi rootfs | `FILE_DELETION_BLOCKED`, `ROOTFS_WRITE_BLOCKED`, ZIP gốc không đổi |
| `network/Main.java`: TCP tới IP số bên ngoài | `NETWORK_BLOCKED`, đồng thời inspect xác nhận `NetworkMode=none` |

Chạy riêng từng case để quay video:

```bash
mvn verify -Psandbox-it -Dit.test=DockerSandboxIT#infiniteLoopTimesOutAndLiveDockerSettingsAreEnforced
mvn verify -Psandbox-it -Dit.test=DockerSandboxIT#blocksFileDeletionAndPreservesOriginalSubmission
mvn verify -Psandbox-it -Dit.test=DockerSandboxIT#blocksOutboundNetwork
```

Suite Docker còn kiểm tra Java 8, package/tên thư mục có khoảng trắng, stdin,
compiler error, exit code giả, output flood, heap OOM, image không có,
interrupt/cancel và giới hạn hai job đồng thời. TLE test inspect trực tiếp container
để xác nhận RAM, swap, CPU, user, network, rootfs, capabilities, PID và mount read-only.

Đọc báo cáo:

```bash
cat target/surefire-reports/com.fpt.aita.service.SandboxWorkspaceTest.txt
cat target/failsafe-reports/com.fpt.aita.service.DockerSandboxIT.txt
docker ps -a --filter label=aita.sandbox=true
```

Sau test, danh sách container sandbox phải rỗng. Các container database đang chạy
không được thay đổi. Khi Docker daemon lỗi/Tomcat bị kill đột ngột, quản trị viên cần
kiểm tra container có label `aita.sandbox=true`, dừng/xóa đúng job còn sót rồi dọn
thư mục `aita-sandbox-*` trong thư mục tạm và khởi động lại webapp để mở khóa.
Hiện deadline được host service giám sát; chưa có watchdog độc lập nếu host JVM chết.

## Tài liệu tham chiếu

- [Docker none network](https://docs.docker.com/engine/network/drivers/none/)
- [Docker memory/swap/CPU constraints](https://docs.docker.com/engine/containers/resource_constraints/)
- [Docker run, filesystem và security options](https://docs.docker.com/engine/containers/run/)
- [Java 8 Process — waitFor(timeout), stream buffering và destroyForcibly](https://docs.oracle.com/javase/8/docs/api/java/lang/Process.html)
