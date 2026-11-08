# Báo cáo AITA-FIX-001 / 002 / 003

Ngày kiểm chứng: 08/11/2026. Người thực hiện: agy (DB/DAO/model), Codex (AST/AI/controller/JSP/test và review), Claude (review).

## Nguyên nhân gốc và hành vi sau sửa

- AST áp LL01/LL02 cho mọi bài tập và tìm LinkedList bằng chuỗi từng dòng, nên MyBST cũng bị xử lý như bài LinkedList. LL01/LL02 nay chỉ áp khi assignment có `rubric_type=LINKEDLIST`; import/khởi tạo được tìm bằng node AST, bỏ qua comment và string literal.
- RULES thiếu LL01/LL02; `getRuleIdByCode` trả 1 khi không tìm thấy, làm lỗi bị ghi thành PERF01. Migration thêm hai rule; DAO trả -1, cả service và DAO bỏ qua rule ID không hợp lệ.
- Prompt, danh sách hàm và trọng số LinkedList trước đây dùng chung cho mọi bài. Nay NONE dùng đánh giá trung tính; BST có insert/search/delete/duyệt/height, Core Logic 50%, Edge Cases 25%, Quality 25%; thang mỗi hàm NONE/BST là 100. LinkedList giữ trọng số riêng.
- Cache AI trước đây chỉ so mã nguồn, thiếu điểm và loại rubric, đồng thời cache hit không tham gia tính quality_score. Nay cache khớp mã nguồn và rubric, lưu/tái sử dụng điểm; bản cũ không có điểm được phân tích lại. Điểm được tổng hợp theo tỷ lệ tổng điểm đạt/tổng điểm tối đa của các hàm được phân tích thành công, giới hạn 0–100. Đây là nhận xét tĩnh của AI, không thay thế điểm test case.
- CourseDetailServlet trước đây chỉ kiểm tra vai trò Lecturer ở POST/download. Nay kiểm tra phân công môn qua USER_CLASS; với sửa/xóa/deadline và download, môn được lấy từ assignment trong DB, không tin courseId gửi từ client. Danh sách phân công rỗng cũng bị từ chối.

## Thay đổi theo file

| File | Thay đổi |
| --- | --- |
| `database/migrations/003_rubric_type_and_ll_rules.sql` | Migration PostgreSQL thêm assignment rubric, hai rule, điểm/rubric cache; idempotent, nâng cột điểm AI lên decimal(6,2). |
| `database/sqlserver/migrations/003_rubric_type_and_ll_rules.sql` | Migration SQL Server tương ứng, kiểm tra cột/rule trước khi thêm. |
| `database/postgresql/schema.sql`, `AITA_Database.sql` | Schema khởi tạo đồng bộ ba cột mới; điểm GRADING_REPORTS giữ decimal(4,2), thang 10. |
| `database/postgresql/seed.sql` | Thêm LL01/LL02 vào dữ liệu mẫu. |
| `database/sqlserver/AITA_Database.sql` | Schema và rule mẫu tương ứng SQL Server. |
| `src/main/java/com/fpt/aita/model/Assignment.java` | rubricType mặc định NONE, getter/setter và constructor mở rộng. |
| `src/main/java/com/fpt/aita/model/AiOptimization.java` | Thêm Double score và rubricType. |
| `src/main/java/com/fpt/aita/dal/AssignmentDAO.java` | Đọc/ghi rubric, updateRubricType, lấy generated ID khi tạo assignment. |
| `src/main/java/com/fpt/aita/dal/AnalysisReportDAO.java` | Tra rubric theo submission; rule không tìm thấy trả -1; chặn insertIssue với ID không hợp lệ. |
| `src/main/java/com/fpt/aita/dal/AiOptimizationDAO.java` | Lưu/đọc điểm và rubric, cache theo cặp code/rubric; thứ tự latest có optimization_id để phân xử khi trùng timestamp. |
| `src/main/java/com/fpt/aita/service/AstAnalyzerService.java` | Gate LL01/LL02 theo rubric; phát hiện bằng ImportDeclaration/ObjectCreationExpr; bỏ qua lớp LinkedList tự khai báo trong cùng file hoặc import rõ từ package khác; không phạt rule không có ID. Thêm constructor tiêm DAO để kiểm thử. |
| `src/main/java/com/fpt/aita/ai/AiRequest.java` | Mang rubric tới AI, mặc định NONE. |
| `src/main/java/com/fpt/aita/ai/PromptBuilder.java` | Prompt/điểm tối đa theo rubric; BST/NONE không có chỉ dẫn LinkedList. Ví dụ JSON dùng score 80 cho BST/NONE, 0.8 cho LINKEDLIST. |
| `src/main/java/com/fpt/aita/ai/GeminiClient.java` | Dùng system instruction đúng rubric của request. |
| `src/main/java/com/fpt/aita/ai/MockAiClient.java` | Phản hồi mẫu theo rubric và đúng thang điểm; không ép LinkedList cho BST/NONE. |
| `src/main/java/com/fpt/aita/ai/AiAnalysisService.java` | Chọn hàm theo rubric; tính điểm cả cache hit/bản ghi sẵn có; không reuse điểm thiếu hoặc khác rubric; kiểm tra lỗi ghi cache. |
| `src/main/java/com/fpt/aita/controller/CourseDetailServlet.java` | Phân quyền quản trị/download theo môn; kiểm tra rubric hợp lệ; lưu rubric khi tạo/sửa. |
| `src/main/webapp/course_detail.jsp` | Select NONE/LINKEDLIST/BST ở tạo/sửa; giữ rubric khi nhân bản. Không thêm scriptlet. |
| `src/test/java/com/fpt/aita/service/AstRubricTest.java` | Test AST/rubric, MyBST import hợp lệ với NONE/BST, comment/string, khởi tạo nhiều dòng, lớp tự cài đặt, thiếu rule ID. |
| `src/test/java/com/fpt/aita/controller/CourseDetailAuthorizationTest.java` | Test giả courseId, chỉ có assignmentId, không được phân công, student quản trị, download trái môn, tạo/sửa hợp lệ và rubric không hợp lệ. |
| `src/test/java/com/fpt/aita/test/AiRubricTest.java` | Test prompt/thang điểm, ví dụ JSON, chọn hàm, cache và điểm có trọng số, cache khác rubric, legacy cache, lỗi ghi cache, mock. |
| `src/test/java/com/fpt/aita/test/AiAnalysisServiceTest.java` | Fake DAO/client theo hợp đồng mới; tiêm rubric DAO để kiểm thử không truy cập DB thật. |
| `src/test/java/com/fpt/aita/dal/PostgresJdbcIT.java` | Schema 15 bảng/96 cột, rubric DAO round-trip, rule lookup, điểm 100.00, cache khác rubric và trường hợp timestamp trùng. |

## Phát hiện trong review và xử lý

1. Claude phát hiện ví dụ JSON chung `"score": 0.8` gây mơ hồ thang điểm 100 cho BST/NONE. Đã đổi theo rubric và khóa bằng assertion trong AiRubricTest: BST/NONE không chứa ví dụ 0.8, có ví dụ 80; LinkedList vẫn có 0.8.
2. Cột AI score decimal(4,2) không chứa được 100.00. Agy đã nâng riêng AI_OPTIMIZATIONS.score lên decimal(6,2) trong cả hai migration và schema; không đổi thang điểm test case.
3. Test PostgreSQL thật phát hiện truy vấn chỉ ORDER BY created_at có thể chọn bản cũ score NULL khi timestamp trùng. Agy thêm optimization_id DESC ở cả 7 vị trí truy vấn latest; test giữ nguyên timestamp để khóa hồi quy này. Không thêm UNIQUE/upsert.

## Kết quả kiểm chứng

| Kiểm tra | Kết quả thật | Phạm vi chứng minh |
| --- | --- | --- |
| Maven `clean test-compile` theo đường dẫn AGENTS.md | PASS | Build source/test, target Java 1.8. |
| Maven verify với cả ba lớp IT được chọn rõ | PASS: 54 unit + 18 integration = **72 test**, 0 failure/error/skip | DockerSandboxIT 11, GradingDockerIT 4, PostgresJdbcIT 3; bao gồm hồi quy prompt theo review Claude. |
| Năm chương trình test AI chạy riêng | PASS: **26 mục kiểm tra** | PromptBuilder 3, AiAnalysisService 4, FileMethodCodeProvider 3, AiResponseParser 4, GeminiClientRetry 12; fake/mock, không gọi Gemini thật. |
| Compiler Java 8 thực tế trong image aita-sandbox:java8 | PASS | Biên dịch toàn bộ source production bằng javac 1.8, không chỉ kiểm tra target bytecode. |
| JVM Java 8 thực tế, JUnitCore | PASS: **54 test** | Chạy lại bộ unit trên Java 8; dùng cấu hình test riêng, không đọc .env thật. Đây là chạy lại cùng 54 test, không cộng vào số test duy nhất ở trên. |
| PostgreSQL 16 database tạm: fresh schema/seed rồi migration hai lần | PASS | 15 bảng/96 cột; 10 rule, không nhân đôi LL01/LL02; điểm AI decimal(6,2). |
| PostgreSQL schema/seed từ Git HEAD trước sửa rồi migration hai lần | PASS | Giữ nguyên số assignment cũ; tất cả nhận NONE; hai rule mới hiện diện đúng một lần. |
| DAO PostgreSQL thật | PASS | Rubric/ID tạo mới, rule không tồn tại=-1, điểm 100.00 round-trip, cache khác rubric trả null và latest ổn định khi timestamp trùng. |
| Review diff/JSP/Java API/token | PASS trong phạm vi đã đọc | Toàn bộ diff của hai agent và ba file test mới đã được đọc; không có thay đổi Docker/pom ngoài task. JSP không scriptlet; rà API mới và mẫu token phổ biến không phát hiện secret mới. Đây không phải chứng nhận security audit toàn repo. |
| Whitespace | PASS với `git -c core.whitespace=cr-at-eol diff --check` | Tôn trọng CRLF sẵn có ở một số file, không chuẩn hóa cả file ngoài phạm vi. |
| Dọn môi trường test | PASS | Container PostgreSQL tạm đã xóa; không còn container aita-sandbox của lượt test. Không sửa .env, không chạy migration/ghi test lên Supabase. |

Lệnh build chính:

```sh
/home/phambuuthinh/Downloads/netbeans-17-bin/netbeans/java/maven/bin/mvn clean test-compile
/home/phambuuthinh/Downloads/netbeans-17-bin/netbeans/java/maven/bin/mvn verify -Psandbox-it,postgres-it -Dit.test=DockerSandboxIT,GradingDockerIT,PostgresJdbcIT
```

Lệnh verify được chạy với `AITA_POSTGRES_TESTS=true`, `AI_PROVIDER=mock` và DB_URL/DB_USER/DB_PASSWORD chỉ trỏ tới container PostgreSQL tạm có tên database chứa `_test_`. Không chạy lệnh này với database thật. Phải chỉ rõ ba lớp IT: cấu hình include của profile postgres-it có thể làm profile kết hợp chỉ chọn PostgresJdbcIT nếu không có `-Dit.test`.

Bằng chứng local: `target/aita-fix-verification/summary.json`, `aita-fix-003-build.log`, `aita-fix-003-verify.log`, `aita-fix-java8-compile.log`, `aita-fix-java8-unit.log` và năm log main test trong cùng thư mục; JUnit XML tại `target/surefire-reports/`, `target/failsafe-reports/`. `review-tests.py` trong thư mục bằng chứng tái tạo database tạm, replay migration rồi chạy verify và tự dọn container. Tất cả là generated artifact gitignored.

Lượt PostgreSQL trước sửa ORDER BY đã **FAIL (1 error)** do chọn bản score NULL khi timestamp trùng; sau bản sửa của agy, cả ba PostgreSQL test và toàn bộ 72 test đã PASS. Một lượt harness Java 8 đầu chưa cô lập đường .env nên bị quyền đọc file của UID container chặn; harness được đổi sang cấu hình test riêng rồi chạy đúng 54 unit PASS. Không sửa quyền .env hoặc nới quyền container để vượt qua lỗi này.

## Giới hạn và rủi ro còn lại

- Chưa chạy bài MyBST thật của người dùng qua sandbox: cần driver Main, giao thức input/output và test case do giảng viên cung cấp. Các test Docker hạ tầng không chứng minh tính đúng đắn của bài BST đó.
- Deadline sandbox 5 giây bao gồm startup/biên dịch/chạy Main; không phải ngân sách runtime riêng. Create/extract/cleanup có ngân sách riêng, nên thời gian HTTP tổng có thể dài hơn 5 giây. Chưa thay đổi cơ chế này.
- Upload vẫn nằm trong webroot (`/uploads/submissions`, có đường fallback src/main/webapp). Chặn tại controller không chứng minh đường URL tĩnh đã được bảo vệ; chưa chuyển file ra ngoài webroot trong task này.
- `AiConfig` vẫn mặc định `gemini-1.5-flash`. Chưa gọi Gemini thật, chưa xác minh model còn khả dụng với tài khoản hiện tại hoặc chất lượng chấm thực tế. Test AI dùng fake/mock, test retry không gọi API.
- Các fallback catch SQLException chạy lại SQL cũ trong AiOptimizationDAO và AssignmentDAO có thể che lỗi SQL thật, hoặc ghi thiếu metadata trên schema cũ. Cần triển khai migration trước ứng dụng; chưa gỡ các fallback trong đợt này.
- Khi bản ghi AI cũ không dùng lại được, insertOptimization chèn thêm dòng cho cùng method_id, nên báo cáo có thể hiện trùng đề xuất. Thứ tự latest đã ổn định nhưng chưa có UNIQUE/upsert hoặc dọn trùng.
- Bản ghi assignment cũ nhận NONE. Giảng viên cần chọn đúng rubric và yêu cầu phân tích/chấm lại nếu muốn áp LinkedList/BST. Không tự sửa/xóa báo cáo cũ hoặc tự regrade dữ liệu thật.
- LL01 dùng cú pháp AST, chưa có symbol solver toàn project. Kiểu tên ngắn LinkedList ở file khác, kế thừa hoặc che khuất tên phức tạp vẫn có thể cần phân giải kiểu đầy đủ.
- AI chỉ đánh giá tối đa số hàm cấu hình và những hàm đủ điều kiện; quality_score không phải chứng nhận toàn bộ bài hoặc điểm chạy test. Tính Big-O từ độ sâu vòng lặp cũng là heuristic hiện có.
- Chưa chạy UI bằng trình duyệt/Tomcat end-to-end trong lượt này; phân quyền được kiểm thử qua servlet với fake DAO. Chưa kiểm thử giao diện trực quan hoặc biên dịch JSP bằng Jasper.
- SQL Server: đã đọc/review migration và schema; việc áp migration local, replay và kiểm tra kiểu điểm trên container sqlserver_aita do agy báo cáo. Codex chưa tự chạy lại SQL Server; Java application hiện dùng PostgreSQL.

Không commit, không push, không deploy trong task này. Các file cá nhân/tài liệu untracked có sẵn trước task được giữ nguyên; .env và credentials không được đưa vào báo cáo hoặc thay đổi Git.

## Kiểm chứng lại theo yêu cầu người dùng — 2026-11-08 22:55

Build sạch test-compile PASS; verify **72/72 test PASS** (54 unit + 11 DockerSandbox + 4 GradingDocker + 3 PostgreSQL), không failure/error/skip. Compiler Java 8 toàn production PASS; chạy lại 54 unit trên JVM Java 8 PASS; năm AI main suites 26/26 mục kiểm tra PASS. Fresh/upgrade migration PostgreSQL chạy hai lần PASS. Container database tạm đã xóa, không chạy test ghi dữ liệu lên DB thật. Không phát hiện lỗi mới, không sửa logic production, không commit/push. Evidence cập nhật tại `target/aita-fix-verification/`. Các giới hạn nêu trên vẫn còn.

## AITA-FIX-004/005/006 — chấm file Java đơn và lưu bài ngoài webroot

Cập nhật 2026-11-08: các giới hạn cũ về thiếu driver, timeout chung và upload trong webroot ở phần FIX-003 được thay thế bởi kết quả dưới đây. Các kết quả FIX-003 vẫn là bằng chứng của lượt kiểm tra trước.

FIX-004 bổ sung driver Java do giảng viên sở hữu bài tập cấu hình, lưu trong `GRADING_CONFIGS.driver_source` qua migration 004. Form JSTL/EL gửi và hiển thị source đã escape cho người quản lý; sinh viên không được lấy driver hay test ẩn qua trang cấu hình, không được POST sửa bộ test. File `.java` sinh viên được đọc UTF-8 có giới hạn, lấy tên compiler từ AST và ghép với driver trong ZIP riêng. Kiểm tra chặn ghi đè kiểu driver, helper, tên binary có `$`, cùng package và symlink. ZIP sinh viên giữ luồng cũ. Thiếu driver trả NOT_CONFIGURED/score NULL; lỗi đóng gói hoặc bài giả mạo không làm bỏ qua AST/AI.

Javac có ngân sách 15 giây; chương trình có 5 giây riêng, watchdog host 23 giây. Biên dịch lỗi/timeout trả COMPILE_ERROR; quá thời gian chạy trả TIME_LIMIT_EXCEEDED. Wrapper phân biệt student System.exit(124) với timeout của supervisor. Kiểm thử phát hiện cancel(true) và shutdownNow có thể gửi hai interrupt, làm dọn container bị gián đoạn rồi khóa sandbox; cleanup hiện thử lại tối đa ba lần và khôi phục cờ interrupt sau khi dọn. Bộ Docker đã kiểm chứng lại việc hủy và giải phóng slot. Driver BST mẫu ở `docs/grading-driver/Main.java`, hợp đồng API và cách nhập test ở README cùng thư mục.

FIX-005 của agy đưa bài nộp vào `AITA_UPLOAD_DIR` (mặc định `~/aita-uploads/submissions`), giữ đường dẫn logic `/uploads/submissions/...` trong DB. UploadStorage dùng CREATE_NEW để tránh ghi đè, whitelist tên, giới hạn 50 MiB, kiểm tra symlink và fallback bài cũ; controller dùng storage cho tải bài và giữ phân quyền. AuthFilter chặn URL uploads qua đường container đã chuẩn hóa và raw URL decode/matrix/dot segments. Controller ghi DB theo tên file thực tế do storage trả về khi trùng tên. Script migration copy và giữ nguồn, bỏ qua symlink nguồn/đích. Không tự chuyển hoặc xóa bài nộp thật trong lượt kiểm tra.

### Bằng chứng kiểm chứng FIX-004 và review FIX-005

- Maven `clean test-compile` PASS (23:20:54), verify kết thúc BUILD SUCCESS (23:22:36). Verify với năm IT chỉ định rõ: **88 unit + 25 integration = 113 test PASS**, không failure/error/skip. Phân bố IT: DockerSandbox12, GradingDocker4, SingleJavaDocker5, GradingQueueDriver1, PostgresJdbc3.
- Compiler Java 8 thực tế biên dịch toàn bộ production và các lớp unit; chạy cùng **88 unit trên JVM Java 8 PASS**. Đây là chạy lại bộ unit, không cộng vào 113 test duy nhất.
- Docker chạy fixture BST đúng đạt **10.00/10**, fixture sai đạt **3.33/10**; lỗi syntax ra COMPILE_ERROR; sleep4.75s được đủ runtime và PASS sau biên dịch; vòng lặp vô hạn ra TLE. Đáp án/trọng số không được đóng gói vào workspace container.
- PostgreSQL16 tạm: schema/seed hiện tại + migrations003/004 chạy hai lần PASS; schema/seed GitHEAD cũ + migrations chạy hai lần PASS và bảo toàn assignment. DAO lưu/đọc driver và khóa version, xóa driver khi cấu hình lại không có source đều PASS. Schema sau004 có15bảng/97cột.
- Queue thật + DAO + PostgreSQL + Docker + AST/mockAI: .java có driver chấm10; thiếu driver NOT_CONFIGURED/NULL; giả mạo Main INVALID_SUBMISSION; ba trường hợp vẫn có AST report.
- **Tomcat9 HTTP thật** trên instance riêng/database/upload root tạm: đăng nhập giảng viên, GET/POST cấu hình driver, biên dịch và render hai JSP grading bằng Jasper; POST student sửa driver403; CSRF sai403; student không thấy driver/test ẩn. Multipart nộp BST → storage ngoài webroot → Docker10/10 → AST → trang kết quả có điểm. Chủ bài/giảng viên đúng môn tải đúng bytes; sinh viên khác/giảng viên khác403.
- Với canary legacy còn trong webroot, **15 lượt HTTP** (raw, URLencoded, matrix, dotsegment, doubleSlash × anonymous/student/teacher) đều bị chặn, không trả nội dung canary. Unit kiểm tra thêm tên có control/CRLF, symlink, fallback và đường normalized. Đây là kiểm chứng URL trên Tomcat, không chỉ mock filter.
- Script migration được chạy trong project fixture tạm: copy đúng bytes, giữ file nguồn, không ghi đè file đích đã có, bỏ qua cả symlink nguồn và đích; canary đích ngoài thư mục không bị sửa.
- `git diff --check`, kiểm tra không scriptlet trong JSP grading và `bash -n scripts/migrate_uploads.sh` PASS. Không in/chép credential thật; helper dùng cấu hình riêng và DB tạm, đã dọn Tomcat/container/files sau kiểm tra.

Một lượt verify đầu dừng do test UploadStorage gọi chữ ký migration cũ, agy đồng bộ test/API. Lượt tiếp theo có 19 IT fail dây chuyền sau lỗi cancellation cleanup (sandbox fail-closed); đã sửa nguyên nhân và chạy lại đủ25IT PASS. Hai lần thử HTTP ban đầu kiểm tra khi queue vẫn đang chạy; helper đã poll trang kết quả đến khi hoàn tất, sau đó toàn luồng PASS. Các lượt lỗi này không được tính là PASS.

### Triển khai và giới hạn còn lại

- Phải áp migration004 lên DB trước khi chạy ứng dụng mới và rebuild image `aita-sandbox:java8`. PostgreSQL migration004 đã tự kiểm chứng trên DB tạm; SQLServer004 chỉ review mã, chưa tự chạy. Codex không áp migration004 lên database thật; agy báo đã áp trên hai container local, xem phần review agy bên dưới.
- Vẫn **biên dịch lại mỗi test trong container mới** để giữ cô lập, chưa biên dịch một lần cho cả bộ test. `execution_time_ms` vẫn bao gồm start/compile/run; bộ20test có thể mất lâu hơn5s tổng cộng. Host watchdog23s là ngân sách hạ tầng; timeout watchdog không được báo nhầm thành TLE của sinh viên.
- Driver là source tin cậy về quyền cấu hình, nhưng chạy cùng code sinh viên trong sandbox: không được chứa secret/đáp án. Container chỉ nhận input của test hiện tại và source cần chạy. Không coi driver là nơi giữ bí mật.
- Đã chạy fixture MyBST có hợp đồng API công khai trong docs; chưa chạy **file MyBST thật của người dùng**. Giảng viên phải điều chỉnh driver theo chữ ký và quy ước thực tế (height, duplicate, inorder).
- HTTP/Jasper đã kiểm chứng; chưa đánh giá giao diện trực quan bằng trình duyệt hoặc quy trình redeploy production. Thư mục lưu trữ và các thư mục cha phải do tài khoản dịch vụ/quản trị kiểm soát, không cho sinh viên ghi trực tiếp filesystem.
- Chưa gọi Gemini thật, chưa xác minh chất lượng/model hiện tại. Những rủi ro FIX-003 về fallback SQLException che lỗi SQL, bản ghi AI trùng cùng method_id, AST chưa symbol solver và cần chọn rubric/regrade vẫn còn.

Không commit, push, deploy, sửa `.env` hoặc ghi dữ liệu test lên DB thật; việc áp migration local của agy được ghi riêng bên dưới.

### Review chéo của agy đối với AITA-FIX-004 (2026-11-08)

- **Phạm vi & Tuân thủ quy chuẩn**:
  - Đúng thẩm quyền được phân công: Codex chỉ sửa `GradingConfig`, `GradingDAO`, `SandboxWorkspace`, `GradingService`, `DockerSandboxService`, `docker/sandbox/`, `grading-tests.jsp`, `GradingServlet`, và phần `process()` trong `GradingQueueManager`. Hoàn toàn không xâm phạm `CourseDetailServlet`, `AppConfig`, hay `UploadStorage` của agy.
  - Tuân thủ nghiêm ngặt **Java 8** (`-source 1.8 -target 1.8`).
  - JSP sạch 100%: `grading-tests.jsp` dùng hoàn toàn JSTL + EL (`<c:out>`), không chứa bất kỳ scriptlet (`<% %>`) nào.
  - Bảo mật sandbox: Cơ chế `packageSingleJava` dùng JavaParser bóc tách AST, chặn sinh viên đặt tên class/package trùng driver của giảng viên; tách biệt rõ ràng timeout giữa javac 15s (`COMPILE_ERROR`) và runtime 5s (`TIME_LIMIT_EXCEEDED`); wrapper `run-java.sh` ánh xạ mọi exit code khác 0 của sinh viên về 70, chống giả mạo exit code 124.
  - Cả hai migration `004_trusted_grading_driver.sql` (PostgreSQL & SQL Server) đều idempotent và đã được agy chạy thử nghiệm thành công trên cả 2 container `postgres_aita` và `sqlserver_aita`.
  - Kết quả kiểm chứng tổng thể do Codex chạy, agy đối chiếu báo cáo: **113/113 test PASS** (88 unit + 25 integration tests), kiểm chứng HTTP Tomcat live với 15 biến thể bypass đều bị chặn.
- **Kết luận review chéo của agy**: **ĐẠT trong phạm vi review và kiểm chứng đã ghi** — Sẵn sàng chuyển Claude nghiệm thu và chốt task.

Lượt cuối sau sửa collision/ancestor của005: clean test-compile23:20:54 PASS; verify23:22:36 **113/113 PASS (88unit+25IT)**, Java8 compiler+88unit PASS. HTTP Tomcat9 chạy lại trên WAR mới PASS cả download legacy fallback qua controller và trả403 khi file legacy là symlink, không trả bytes canary. Review005 của Codex hoàn tất, không còn finding chặn nghiệm thu trong phạm vi đã kiểm chứng. Evidence tại `target/aita-fix-verification/summary.json`, log final-clean/verify/final-java8/final-http và helper tái tạo môi trường tạm.

## Bộ demo nộp thử và chuẩn bị phát hành — AITA-DEMO-PUSH-007

Theo yêu cầu người dùng, bổ sung `docs/test-thu-bst/HUONG_DAN_TEST.md`, một file nộp mẫu `MyBST.java`, 8 cặp stdin/expected và manifest JSON. Hướng dẫn phân biệt driver giảng viên với file sinh viên, cấu hình rubric BST, deadline/ghi danh, test ẩn, chấm lại và xử lý lỗi. Script `scripts/verify_bst_demo.py` kiểm chứng toàn bộ bộ demo bằng container Java8 có giới hạn, không cần database hoặc .env: bản đúng8/8 đạt10.00; bản cố ý cho search luôn false đạt6/8 và7.50, sai đúng test02/07. Đã chạy thật16lượt Docker PASS theo kỳ vọng.

Rà soát Git trước commit: origin/main đồng bộ, không conflict; các file credentials .env không staged; kiểm tra mẫu khóa bí mật và diff whitespace PASS. Giữ lại các tài liệu cá nhân/cấu hình agent untracked ngoài commit. Người dùng đã cho phép commit/push sau kiểm tra, thay thế ràng buộc không commit/push của các task trước cho bước phát hành này.

Kiểm chứng trước push lúc23:34 ngày2026-11-08: Maven verify113/113PASS (88unit+25IT), fresh/upgrade migrations replay2lầnPASS, database tạm đã dọn. Bộ demo16lượt Docker đạt đúng kỳ vọng. Diff check giữ CRLF hiện có (`core.whitespace=cr-at-eol`) PASS.
