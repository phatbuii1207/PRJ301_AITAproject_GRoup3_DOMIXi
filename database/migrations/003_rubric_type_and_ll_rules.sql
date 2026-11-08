-- Migration 003: Thêm rubric_type vào ASSIGNMENTS và nạp rules LL01, LL02
BEGIN;

-- 1. Thêm cột rubric_type vào ASSIGNMENTS (idempotent)
ALTER TABLE "ASSIGNMENTS" ADD COLUMN IF NOT EXISTS rubric_type varchar(20) NOT NULL DEFAULT 'NONE';

-- 2. Thêm cột score và rubric_type vào AI_OPTIMIZATIONS (hỗ trợ cache theo rubric, score thang 100 max 100.00)
ALTER TABLE "AI_OPTIMIZATIONS" ADD COLUMN IF NOT EXISTS score decimal(6,2) NULL;
ALTER TABLE "AI_OPTIMIZATIONS" ALTER COLUMN score TYPE decimal(6,2);
ALTER TABLE "AI_OPTIMIZATIONS" ADD COLUMN IF NOT EXISTS rubric_type varchar(20) NULL;

-- 3. Thêm rule LL01 và LL02 vào RULES (idempotent)
INSERT INTO "RULES" (rule_code, category, name, weight, enabled)
VALUES
    ('LL01', 'Code Smells', 'Cấm sử dụng thư viện java.util.LinkedList có sẵn khi làm bài LinkedList', 20, true),
    ('LL02', 'Code Smells', 'Thân phương thức rỗng hoặc chưa cài đặt (Empty Method Body)', 10, true)
ON CONFLICT (rule_code) DO NOTHING;

COMMIT;
