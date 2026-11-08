#!/usr/bin/env bash
# ==============================================================================
# AITA - Di chuyển bài nộp cũ từ webroot sang thư mục ngoài webroot (UploadStorage)
# An toàn: sao chép bảo lưu thuộc tính, không xóa bản gốc khi chưa có xác nhận.
# ==============================================================================

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_TARGET="$HOME/aita-uploads/submissions"

# Đọc AITA_UPLOAD_DIR từ .env nếu có
TARGET_DIR="${AITA_UPLOAD_DIR:-}"
if [ -z "$TARGET_DIR" ] && [ -f "$PROJECT_DIR/.env" ]; then
    TARGET_DIR="$(grep -E '^AITA_UPLOAD_DIR=' "$PROJECT_DIR/.env" | cut -d'=' -f2- | tr -d '"' | tr -d "'" || true)"
fi
if [ -z "$TARGET_DIR" ]; then
    TARGET_DIR="$DEFAULT_TARGET"
fi

# Mở rộng ~ nếu có
TARGET_DIR="${TARGET_DIR/#\~/$HOME}"

echo "=== AITA Migration: Chuyển bài nộp cũ sang thư mục ngoài webroot ==="
echo "Thư mục đích: $TARGET_DIR"
mkdir -p "$TARGET_DIR"

SOURCES=(
    "$PROJECT_DIR/src/main/webapp/uploads/submissions"
    "$PROJECT_DIR/target/AITA_StaticCodeAnalysis-1.0-SNAPSHOT/uploads/submissions"
)

TOTAL_COPIED=0

for SRC in "${SOURCES[@]}"; do
    if [ -d "$SRC" ]; then
        echo "Kiểm tra thư mục nguồn: $SRC"
        shopt -s nullglob
        FILES=("$SRC"/*.zip "$SRC"/*.java)
        shopt -u nullglob
        for FILE in "${FILES[@]}"; do
            [ -f "$FILE" ] || continue
            FNAME="$(basename "$FILE")"
            if [ -L "$FILE" ]; then
                echo "  ! Bỏ qua symlink nguồn: $FNAME"
                continue
            fi
            DEST="$TARGET_DIR/$FNAME"
            if [ -L "$DEST" ]; then
                echo "  ! Cảnh báo: đích là symlink, bỏ qua: $FNAME"
                continue
            fi
            if [ ! -f "$DEST" ]; then
                cp -P -p "$FILE" "$DEST"
                echo "  -> Đã chép: $FNAME"
                TOTAL_COPIED=$((TOTAL_COPIED + 1))
            else
                echo "  == Đã tồn tại tại đích, bỏ qua: $FNAME"
            fi
        done
    fi
done

echo "------------------------------------------------------------------"
echo "Đã chuyển thành công $TOTAL_COPIED tệp sang: $TARGET_DIR"
echo "LƯU Ý BẢO MẬT: Bản gốc tại webroot VẪN ĐƯỢC GIỮ NGUYÊN (không tự động xóa)."
echo "Sau khi kiểm chứng đầy đủ bài nộp tải về bình thường, người dùng có thể xóa thủ công thư mục cũ nếu muốn."
