# Kiểm tra lại logic chấm bài

> Biên bản lịch sử trước khi đổi database. Hệ thống hiện dùng PostgreSQL; xem [kiểm chứng mới](POSTGRESQL_VERIFICATION.md).

Lượt kiểm tra: 2026-11-04 21:39 (local). Không sửa logic production, không commit/push.

## Tự động

`mvn verify -Psandbox-it`: 36 test PASS, không skip.

| Suite | Số test | Kết quả |
| --- | --- | --- |
| GradingServiceTest | 15 | PASS |
| SandboxWorkspaceTest | 6 | PASS |
| GradingDockerIT | 4 | PASS |
| DockerSandboxIT | 11 | PASS |

## HTTP thật trên Tomcat 9

Tạo giảng viên/sinh viên và bài tập tạm, cấu hình 5 test có trọng số bằng nhau,
nộp ZIP qua servlet, đợi worker chạy Docker rồi đối chiếu điểm/verdict từ SQL Server
và trang kết quả. Với TLE, dùng một test để kiểm tra deadline riêng.

| Bài nộp | Điểm | Verdict |
| --- | --- | --- |
| correct | 10.00 / 10 | ACCEPTED |
| four-of-five | 8.00 / 10 | PARTIAL |
| hardcoded | 2.00 / 10 | PARTIAL |
| wrong | 0.00 / 10 | WRONG_ANSWER |
| compile | 0.00 / 10 | COMPILE_ERROR |
| runtime | 0.00 / 10 | RUNTIME_ERROR |
| tle | 0.00 / 10 | TIME_LIMIT_EXCEEDED |

Các kiểm tra bổ sung đều PASS:

- Test công khai không lộ nội dung test ẩn.
- Sinh viên khác không xem được bài nộp; HTML của chủ bài không có input/đáp án/output test ẩn.
- Thiếu CSRF bị từ chối; stderr không làm sai phép so stdout.
- Đổi barem không sửa snapshot kết quả cũ.
- Chưa cấu hình test: NOT_CONFIGURED, score NULL.
- Chấm lại file bị thay hash: GRADING_ERROR, score NULL.

Server test đã dừng; tài khoản, bài tập, bài nộp và file test đã dọn.
Số bản ghi các bảng trở về ban đầu; không còn container sandbox.
Chỉ kiểm tra SQL Server local, chưa kiểm tra Azure SQL/AI API.

Raw HTTP summary: [grading-recheck-http.json](../target/grading-recheck-http.json).
JUnit reports: `target/surefire-reports/` và `target/failsafe-reports/`.
