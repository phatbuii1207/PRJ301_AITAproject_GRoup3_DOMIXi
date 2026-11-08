-- Migration 003 (SQL Server): Thêm rubric_type vào ASSIGNMENTS và nạp rules LL01, LL02
IF COL_LENGTH('dbo.ASSIGNMENTS', 'rubric_type') IS NULL
BEGIN
    ALTER TABLE dbo.ASSIGNMENTS ADD rubric_type varchar(20) NOT NULL CONSTRAINT DF_ASSIGNMENTS_rubric_type DEFAULT 'NONE';
END;

IF COL_LENGTH('dbo.AI_OPTIMIZATIONS', 'score') IS NULL
BEGIN
    ALTER TABLE dbo.AI_OPTIMIZATIONS ADD score decimal(6,2) NULL;
END
ELSE
BEGIN
    ALTER TABLE dbo.AI_OPTIMIZATIONS ALTER COLUMN score decimal(6,2) NULL;
END;

IF COL_LENGTH('dbo.AI_OPTIMIZATIONS', 'rubric_type') IS NULL
BEGIN
    ALTER TABLE dbo.AI_OPTIMIZATIONS ADD rubric_type varchar(20) NULL;
END;

IF NOT EXISTS (SELECT 1 FROM dbo.RULES WHERE rule_code = 'LL01')
BEGIN
    INSERT INTO dbo.RULES (rule_code, category, name, weight, enabled)
    VALUES ('LL01', 'Code Smells', N'Cấm sử dụng thư viện java.util.LinkedList có sẵn khi làm bài LinkedList', 20, 1);
END;

IF NOT EXISTS (SELECT 1 FROM dbo.RULES WHERE rule_code = 'LL02')
BEGIN
    INSERT INTO dbo.RULES (rule_code, category, name, weight, enabled)
    VALUES ('LL02', 'Code Smells', N'Thân phương thức rỗng hoặc chưa cài đặt (Empty Method Body)', 10, 1);
END;
