# Đăng nhập ổn định với Supabase trên Windows / NetBeans

Sau khi pull bản sửa này, mỗi thành viên cần cập nhật **file `.env` riêng** và restart
Tomcat. Git không chuyển password hoặc `.env` từ máy người khác. Bản sửa không yêu cầu
Docker hay Python để chạy web/kết nối Supabase; Docker chỉ cần trên máy chấm sandbox.

## Các bước cho thành viên Windows

1. Trong NetBeans → Services → Servers, **Stop Tomcat**. Dừng cả các instance Tomcat
   cũ do IDE/terminal khác mở trên chính máy bạn để trả các kết nối cũ.
2. Mở PowerShell tại thư mục project, chạy `git pull --ff-only`.
3. Nếu đã có `.env` chứa tài khoản Supabase đúng, chạy:

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\configure-supabase.ps1
   ```

   Script chỉ sửa DB_URL/DB_HOST/DB_PORT/DB_NAME, giữ nguyên password và các khóa AI.
   Script dùng cổng 6543, `sslmode=require` và `prepareThreshold=0`, có thể chạy lại.
   Bản sao cấu hình cũ được giữ trong user profile dưới tên `.aita.env.backup-...`.
   Không cần chạy PowerShell bằng Administrator và không thay execution policy toàn máy.
   Nếu cấu hình riêng nằm ở nơi khác, thêm `-EnvFile 'D:\duong-dan\.env'` vào lệnh.
4. Nếu chưa có `.env`: copy `.env.example` thành `.env` rồi điền DB_USER/DB_PASSWORD
   được nhóm chia sẻ riêng. Không copy đè `.env` đang có password. Không dùng nguyên
   placeholder `YOUR_PROJECT_REF`. Nếu pooler host của project khác host mẫu, lấy
   host/user chính xác trong Supabase Dashboard → Connect → Transaction pooler.
5. Trong cấu hình server Tomcat của NetBeans, phần **Platform → VM Options**, thêm
   đường dẫn tuyệt đối tới file vừa chỉnh, ví dụ:

   ```text
   -Daita.env.file=D:/AITA/.env
   ```

   Đường dẫn có khoảng trắng cần đặt option trong dấu nháy. Kiểm tra xem server có
   biến `AITA_ENV_FILE` cũ hay không: biến đó có ưu tiên cao hơn VM option.
   Nếu không dùng option này, có thể đặt file riêng tại `%USERPROFILE%\.aita.env`;
   file `.env` trong working directory có ưu tiên trước file home.
6. Chuột phải project → **Clean and Build** → **Run**. Restart Tomcat thực sự;
   chỉ refresh tab trình duyệt không tải lại các class JDBC cũ.
7. Thử đăng nhập. Khi database tạm lỗi, giao diện báo không kết nối được CSDL
   (HTTP 503), không còn khẳng định tài khoản/password sai.

Nếu chỉ muốn sửa thủ công `.env`, cấu hình mẫu là:

```dotenv
DB_URL=jdbc:postgresql://YOUR_POOLER_HOST:6543/postgres?sslmode=require&prepareThreshold=0
DB_HOST=YOUR_POOLER_HOST
DB_PORT=6543
DB_NAME=postgres
DB_USER=postgres.YOUR_PROJECT_REF
DB_PASSWORD=your-private-database-password
AI_PROVIDER=mock
```

Không đặt password vào DB_URL. Không chạy `source .env` hoặc gõ nguyên DB_URL có
ký tự `&` thành lệnh PowerShell; đây là dữ liệu cấu hình để Java đọc.

## Nguyên nhân và phần đã sửa

Supabase session pooler cổng 5432 từng trả `EMAXCONNSESSION` với pool_size 15 của
project này. Đây là giới hạn kết nối database, không phải số tài khoản người dùng.
Các phiên của mọi máy cùng dùng pool đều có thể ảnh hưởng tới nhau.

Trước đây, constructor DAO mở và giữ connection, còn đường đăng nhập không đóng
connection đó. DAO gắn với servlet/singleton cũng có thể giữ kết nối đã mất hiệu lực.
Hiện các DAO thông thường không giữ connection; từng thao tác mở trong
try-with-resources và đóng trước khi trả về/khi lỗi. DAO chấm bài (GradingDAO)
giữ connection riêng trong phạm vi request/job và được close như trước để bảo toàn
transaction chấm điểm. Parser AST cũng đóng connection khi lưu tối ưu.

Cổng 6543 là transaction pooler. PostgreSQL JDBC phải tắt server-side prepared
statements bằng `prepareThreshold=0`; **PreparedStatement với tham số vẫn được dùng**,
không thay bằng nối chuỗi SQL. Pooler vẫn có giới hạn tải; không đảm bảo dịch vụ luôn
sẵn sàng khi mất mạng/Supabase ngừng hoạt động hoặc các máy khác chạy code cũ.

Tham khảo chính thức: [kết nối và các chế độ pooler của Supabase](https://supabase.com/docs/guides/database/connecting-to-postgres).

## Nếu vẫn không đăng nhập được

| Hiện tượng | Cách kiểm tra |
|---|---|
| `EMAXCONNSESSION` / đủ 15 phiên | DB_URL còn cổng 5432; DB_PORT riêng không ghi đè DB_URL. Chạy script và restart Tomcat. |
| `prepared statement ... already exists/does not exist` | DB_URL cổng 6543 cần `prepareThreshold=0`, sau đó restart. |
| Giao diện báo tạm không kết nối CSDL | Kiểm tra host/user/password, Internet và trạng thái Supabase; thử lại sau. |
| Sai tên đăng nhập hoặc mật khẩu | Database đã trả kết quả nhưng không tìm được account khớp; kiểm tra đúng database và tài khoản của nhóm. |
| Sửa `.env` vẫn dùng cấu hình cũ | Kiểm tra AITA_ENV_FILE, JVM DB_URL/DB_* hoặc file `.aita.env` khác. Đặt đường dẫn tuyệt đối và restart Tomcat. |
| File tạo bằng Notepad có BOM/CRLF | Parser đã hỗ trợ UTF-8 BOM và CRLF; lưu file ở UTF-8. |

DB_URL/DB_* ưu tiên biến môi trường → JVM properties → file cấu hình.
Không gửi password, ảnh `.env` hoặc file `.env` lên Git/issues khi báo lỗi.
Không cần import lại schema hay chạy migration trên Supabase đã có dữ liệu để áp dụng
bản sửa JDBC này. Không tự ngắt phiên database của thành viên khác.

Kết quả kiểm tra bản sửa: [JDBC_CONNECTION_VERIFICATION.md](JDBC_CONNECTION_VERIFICATION.md).
