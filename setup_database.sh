#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

SA_PASSWORD="$1"

if [ -z "$SA_PASSWORD" ]; then
    echo "=========================================================="
    echo " CÀI ĐẶT MICROSOFT SQL SERVER 2022 TRÊN DOCKER (AITA)    "
    echo "=========================================================="
    echo "Yêu cầu mật khẩu tài khoản 'sa':"
    echo " - Tối thiểu 8 ký tự"
    echo " - Gồm chữ HOA, chữ thường, chữ số và/hoặc ký tự đặc biệt"
    echo " Ví dụ: Admin@123456 hoặc YourPass@2024"
    echo "----------------------------------------------------------"
    read -rsp "Nhập mật khẩu cho tài khoản 'sa': " SA_PASSWORD
    echo ""
    if [ -z "$SA_PASSWORD" ]; then
        echo "Lỗi: Mật khẩu không được để trống!"
        exit 1
    fi
fi

echo "1. Tạo file .env..."
cat <<EOF > .env
MSSQL_SA_PASSWORD=$SA_PASSWORD
EOF

echo "2. Khởi chạy Docker Container SQL Server 2022..."
docker compose down || true
docker compose up -d

echo "3. Đang đợi SQL Server khởi động hoàn tất (khoảng 10-15s)..."
RETRIES=30
CONTAINER_NAME="sqlserver_aita"
SQLCMD="/opt/mssql-tools18/bin/sqlcmd"

until docker exec "$CONTAINER_NAME" $SQLCMD -S localhost -U sa -P "$SA_PASSWORD" -C -Q "SELECT 1" &> /dev/null || [ $RETRIES -eq 0 ]; do
    echo "Đang chờ SQL Server sẵn sàng... ($RETRIES)"
    RETRIES=$((RETRIES-1))
    sleep 2
done

if [ $RETRIES -eq 0 ]; then
    echo "Lỗi: SQL Server không thể khởi động hoặc sai quy tắc mật khẩu!"
    echo "Hãy kiểm tra logs bằng lệnh: docker logs $CONTAINER_NAME"
    exit 1
fi

echo "===> SQL Server đã sẵn sàng!"

echo "4. Copy và chạy file script AITA_Database.sql..."
docker cp AITA_Database.sql "$CONTAINER_NAME":/tmp/AITA_Database.sql
docker exec "$CONTAINER_NAME" $SQLCMD -S localhost -U sa -P "$SA_PASSWORD" -C -i /tmp/AITA_Database.sql

echo "5. Cập nhật cấu hình DBContext.java..."
DBCONTEXT_FILE="src/main/java/com/fpt/aita/dal/DBContext.java"
if [ -f "$DBCONTEXT_FILE" ]; then
    # Thay thế pass và url trong DBContext.java
    python3 - <<PYEOF
import re

file_path = "$DBCONTEXT_FILE"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

content = re.sub(r'String\s+pass\s*=\s*".*?";', 'String pass = "$SA_PASSWORD";', content)
content = re.sub(r'String\s+url\s*=\s*".*?";', 'String url = "jdbc:sqlserver://localhost:1433;databaseName=AST_CodeAnalysisDB;encrypt=true;trustServerCertificate=true";', content)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Đã cập nhật thành công DBContext.java với port 1433 và mật khẩu mới.")
PYEOF
fi

echo "=========================================================="
echo " HOÀN TẤT CÀI ĐẶT VÀ IMPORT DATABASE THÀNH CÔNG!          "
echo "=========================================================="
echo "Tài khoản kết nối:"
echo " - Host: localhost:1433"
echo " - User: sa"
echo " - Database: AST_CodeAnalysisDB"
echo "Bây giờ bạn có thể mở NetBeans, Clean & Build và Run project."
