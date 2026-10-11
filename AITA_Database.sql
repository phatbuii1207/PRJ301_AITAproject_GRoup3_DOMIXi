USE [master]
GO

/*******************************************************************************
   DỰ ÁN: AITA - AI Static Code Analysis & AST (Tối ưu hóa thuật toán)
   Database: AST_CodeAnalysisDB
   Mô tả: File thiết lập CSDL và dữ liệu mẫu (75 Sinh viên, 3 Giảng viên, 5 Lớp học)
********************************************************************************/

IF EXISTS (SELECT name FROM master.dbo.sysdatabases WHERE name = N'AST_CodeAnalysisDB')
BEGIN
    ALTER DATABASE AST_CodeAnalysisDB SET OFFLINE WITH ROLLBACK IMMEDIATE;
    ALTER DATABASE AST_CodeAnalysisDB SET ONLINE;
    DROP DATABASE AST_CodeAnalysisDB;
END
GO

CREATE DATABASE AST_CodeAnalysisDB
GO

USE AST_CodeAnalysisDB
GO

/*******************************************************************************
   Xóa bảng cũ nếu tồn tại (theo mẫu chuẩn sample.sql)
********************************************************************************/
DECLARE @sql nvarchar(MAX) 
SET @sql = N'' 

SELECT @sql = @sql + N'ALTER TABLE ' + QUOTENAME(KCU1.TABLE_SCHEMA) 
    + N'.' + QUOTENAME(KCU1.TABLE_NAME) 
    + N' DROP CONSTRAINT ' 
    + QUOTENAME(rc.CONSTRAINT_NAME) + N'; ' + CHAR(13) + CHAR(10) 
FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS AS RC 
INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE AS KCU1 
    ON KCU1.CONSTRAINT_CATALOG = RC.CONSTRAINT_CATALOG  
    AND KCU1.CONSTRAINT_SCHEMA = RC.CONSTRAINT_SCHEMA 
    AND KCU1.CONSTRAINT_NAME = RC.CONSTRAINT_NAME 

EXECUTE(@sql) 
GO

DECLARE @sql2 NVARCHAR(max)=''

SELECT @sql2 += ' DROP TABLE ' + QUOTENAME(TABLE_SCHEMA) + '.'+ QUOTENAME(TABLE_NAME) + '; '
FROM   INFORMATION_SCHEMA.TABLES
WHERE  TABLE_TYPE = 'BASE TABLE'

EXEC Sp_executesql @sql2 
GO 

--------------------------------------------------------------------------------
-- 1. TẠO CÁC BẢNG (TABLES) THEO ĐÚNG ERD
--------------------------------------------------------------------------------

-- Bảng Lớp học (Hỗ trợ quản lý phân nhóm sinh viên)
CREATE TABLE [dbo].[CLASSES](
    [class_id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [class_code] [varchar](20) NOT NULL UNIQUE,
    [class_name] [nvarchar](100) NOT NULL
);

-- Bảng Người dùng (Giảng viên, Sinh viên, Quản trị viên)
CREATE TABLE [dbo].[USERS](
    [user_id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [username] [varchar](50) NOT NULL UNIQUE,
    [password_hash] [varchar](255) NOT NULL,
    [full_name] [nvarchar](100) NOT NULL,
    [role] [varchar](20) NOT NULL -- 'Student', 'Lecturer', 'Admin'
);

-- Bảng Liên kết Người dùng - Lớp học (Giảng viên phụ trách, Sinh viên tham gia lớp)
CREATE TABLE [dbo].[USER_CLASS](
    [user_id] [int] NOT NULL,
    [class_id] [int] NOT NULL,
    PRIMARY KEY ([user_id], [class_id]),
    FOREIGN KEY ([user_id]) REFERENCES [dbo].[USERS]([user_id]) ON DELETE CASCADE,
    FOREIGN KEY ([class_id]) REFERENCES [dbo].[CLASSES]([class_id]) ON DELETE CASCADE
);

-- Bảng Đề bài / Bài tập (Do Giảng viên tạo)
CREATE TABLE [dbo].[ASSIGNMENTS](
    [assignment_id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [lecturer_id] [int] NOT NULL,
    [title] [nvarchar](150) NOT NULL,
    [description] [nvarchar](500) NULL,
    [created_at] [datetime] NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY ([lecturer_id]) REFERENCES [dbo].[USERS]([user_id])
);

-- Bảng Bài nộp của Sinh viên
CREATE TABLE [dbo].[SUBMISSIONS](
    [submission_id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [student_id] [int] NOT NULL,
    [assignment_id] [int] NULL,
    [file_path] [varchar](255) NOT NULL,
    [sha256_hash] [varchar](64) NOT NULL,
    [file_size] [int] NULL,
    [submitted_at] [datetime] NOT NULL DEFAULT GETDATE(),
    [status] [varchar](20) NOT NULL DEFAULT 'Completed', -- 'Processing', 'Completed', 'Failed'
    FOREIGN KEY ([student_id]) REFERENCES [dbo].[USERS]([user_id]),
    FOREIGN KEY ([assignment_id]) REFERENCES [dbo].[ASSIGNMENTS]([assignment_id])
);

-- Bảng Các file mã nguồn trong bài nộp
CREATE TABLE [dbo].[SOURCE_FILES](
    [file_id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [submission_id] [int] NOT NULL,
    [file_name] [varchar](255) NOT NULL,
    [file_type] [varchar](10) NOT NULL DEFAULT 'java',
    [loc] [int] NOT NULL DEFAULT 0, -- Lines of Code
    FOREIGN KEY ([submission_id]) REFERENCES [dbo].[SUBMISSIONS]([submission_id]) ON DELETE CASCADE
);

-- Bảng Các hàm/phương thức bóc tách qua cây cú pháp trừu tượng AST
CREATE TABLE [dbo].[METHODS](
    [method_id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [file_id] [int] NOT NULL,
    [method_name] [varchar](100) NOT NULL,
    [start_line] [int] NULL,
    [end_line] [int] NULL,
    [loop_depth] [int] NOT NULL DEFAULT 0,
    [cyclomatic_complexity] [int] NOT NULL DEFAULT 1,
    [estimated_complexity] [varchar](20) NULL, -- 'O(1)', 'O(N)', 'O(N log N)', 'O(N^2)'
    [is_recursive] [bit] NOT NULL DEFAULT 0,
    FOREIGN KEY ([file_id]) REFERENCES [dbo].[SOURCE_FILES]([file_id]) ON DELETE CASCADE
);

-- Bảng Bộ luật kiểm tra mã nguồn tĩnh (Static Analysis Rules)
CREATE TABLE [dbo].[RULES](
    [rule_id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [rule_code] [varchar](20) NOT NULL UNIQUE,
    [category] [varchar](50) NOT NULL, -- 'Performance', 'Security', 'Code Smells', 'Architecture'
    [name] [nvarchar](150) NOT NULL,
    [weight] [int] NOT NULL DEFAULT 5,
    [enabled] [bit] NOT NULL DEFAULT 1
);

-- Bảng Lỗi / Cảnh báo phát hiện được trên từng dòng code
CREATE TABLE [dbo].[ISSUES](
    [issue_id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [file_id] [int] NOT NULL,
    [method_id] [int] NULL,
    [rule_id] [int] NOT NULL,
    [line_number] [int] NULL,
    [severity] [varchar](20) NOT NULL, -- 'Critical', 'Warning', 'Info'
    [message] [nvarchar](500) NOT NULL,
    [ai_explanation] [nvarchar](MAX) NULL,
    FOREIGN KEY ([file_id]) REFERENCES [dbo].[SOURCE_FILES]([file_id]),
    FOREIGN KEY ([method_id]) REFERENCES [dbo].[METHODS]([method_id]),
    FOREIGN KEY ([rule_id]) REFERENCES [dbo].[RULES]([rule_id])
);

-- Bảng Báo cáo tổng hợp chất lượng mã nguồn
CREATE TABLE [dbo].[ANALYSIS_REPORTS](
    [report_id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [submission_id] [int] NOT NULL UNIQUE,
    [quality_score] [int] NOT NULL DEFAULT 100,
    [total_issues] [int] NOT NULL DEFAULT 0,
    [created_at] [datetime] NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY ([submission_id]) REFERENCES [dbo].[SUBMISSIONS]([submission_id]) ON DELETE CASCADE
);

-- Bảng Đề xuất tối ưu hóa thuật toán từ AI
CREATE TABLE [dbo].[AI_OPTIMIZATIONS](
    [optimization_id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [method_id] [int] NOT NULL,
    [original_code] [nvarchar](MAX) NOT NULL,
    [optimized_code] [nvarchar](MAX) NOT NULL,
    [explanation] [nvarchar](MAX) NOT NULL,
    [created_at] [datetime] NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY ([method_id]) REFERENCES [dbo].[METHODS]([method_id]) ON DELETE CASCADE
);
GO

--------------------------------------------------------------------------------
-- 2. INSERT DỮ LIỆU MẪU (SEED DATA)
--------------------------------------------------------------------------------

-- 2.1. Thêm Lớp học (5 Lớp: PRJ301, CSD201, LAB211, PRF192, WED201)
INSERT INTO [dbo].[CLASSES] ([class_code], [class_name]) VALUES
('SE1801', 'PRJ301'),
('SE1802', 'CSD201'),
('SE1803', 'LAB211'),
('SE1804', 'PRF192'),
('SE1805', 'WED201');

-- 2.2. Thêm 3 Giảng viên (Lecturers) - đã xóa 2 giảng viên
-- Mật khẩu mặc định: '123456' (đã băm SHA-256: e10adc3949ba59abbe56e057f20f883e)
INSERT INTO [dbo].[USERS] ([username], [password_hash], [full_name], [role]) VALUES
('gv_namhn', 'e10adc3949ba59abbe56e057f20f883e', N'ThS. Hoàng Nhật Nam', 'Lecturer'),
('gv_lanpt', 'e10adc3949ba59abbe56e057f20f883e', N'TS. Phan Thị Lan', 'Lecturer'),
('gv_quanglm', 'e10adc3949ba59abbe56e057f20f883e', N'ThS. Lê Minh Quang', 'Lecturer');

-- 2.3. Thêm 75 Sinh viên (Students)
INSERT INTO [dbo].[USERS] ([username], [password_hash], [full_name], [role]) VALUES
('he180001', 'e10adc3949ba59abbe56e057f20f883e', N'Nguyễn Văn An', 'Student'),
('he180002', 'e10adc3949ba59abbe56e057f20f883e', N'Trần Thị Mai Bình', 'Student'),
('he180003', 'e10adc3949ba59abbe56e057f20f883e', N'Lê Hoàng Cường', 'Student'),
('he180004', 'e10adc3949ba59abbe56e057f20f883e', N'Phạm Tiến Dũng', 'Student'),
('he180005', 'e10adc3949ba59abbe56e057f20f883e', N'Hoàng Thu Giang', 'Student'),
('he180006', 'e10adc3949ba59abbe56e057f20f883e', N'Đỗ Minh Hải', 'Student'),
('he180007', 'e10adc3949ba59abbe56e057f20f883e', N'Vũ Ngọc Hân', 'Student'),
('he180008', 'e10adc3949ba59abbe56e057f20f883e', N'Ngô Thanh Hùng', 'Student'),
('he180009', 'e10adc3949ba59abbe56e057f20f883e', N'Bùi Quốc Khánh', 'Student'),
('he180010', 'e10adc3949ba59abbe56e057f20f883e', N'Đinh Bảo Lâm', 'Student'),
('he180011', 'e10adc3949ba59abbe56e057f20f883e', N'Dương Khánh Linh', 'Student'),
('he180012', 'e10adc3949ba59abbe56e057f20f883e', N'Lý Hoàng Long', 'Student'),
('he180013', 'e10adc3949ba59abbe56e057f20f883e', N'Mai Đức Mạnh', 'Student'),
('he180014', 'e10adc3949ba59abbe56e057f20f883e', N'Nguyễn Trà My', 'Student'),
('he180015', 'e10adc3949ba59abbe56e057f20f883e', N'Phan Đình Nam', 'Student'),
('he180016', 'e10adc3949ba59abbe56e057f20f883e', N'Tạ Thúy Nga', 'Student'),
('he180017', 'e10adc3949ba59abbe56e057f20f883e', N'Vương Trọng Nghĩa', 'Student'),
('he180018', 'e10adc3949ba59abbe56e057f20f883e', N'Trịnh Yến Nhi', 'Student'),
('he180019', 'e10adc3949ba59abbe56e057f20f883e', N'Hà Hồng Phong', 'Student'),
('he180020', 'e10adc3949ba59abbe56e057f20f883e', N'Lâm Gia Phúc', 'Student'),
('he180021', 'e10adc3949ba59abbe56e057f20f883e', N'Đoàn Minh Quân', 'Student'),
('he180022', 'e10adc3949ba59abbe56e057f20f883e', N'Cao Thái Sơn', 'Student'),
('he180023', 'e10adc3949ba59abbe56e057f20f883e', N'Tô Phương Thảo', 'Student'),
('he180024', 'e10adc3949ba59abbe56e057f20f883e', N'Trương Anh Thơ', 'Student'),
('he180025', 'e10adc3949ba59abbe56e057f20f883e', N'Quách Hữu Thịnh', 'Student'),
('he180026', 'e10adc3949ba59abbe56e057f20f883e', N'Chu Minh Thuận', 'Student'),
('he180027', 'e10adc3949ba59abbe56e057f20f883e', N'Nghiêm Quốc Toàn', 'Student'),
('he180028', 'e10adc3949ba59abbe56e057f20f883e', N'Thái Cát Tường', 'Student'),
('he180029', 'e10adc3949ba59abbe56e057f20f883e', N'Lương Tuấn Vũ', 'Student'),
('he180030', 'e10adc3949ba59abbe56e057f20f883e', N'Võ Hoàng Uyên', 'Student'),
('he180031', 'e10adc3949ba59abbe56e057f20f883e', N'Nguyễn Thanh Tùng', 'Student'),
('he180032', 'e10adc3949ba59abbe56e057f20f883e', N'Trần Ngọc Ánh', 'Student'),
('he180033', 'e10adc3949ba59abbe56e057f20f883e', N'Lê Minh Triết', 'Student'),
('he180034', 'e10adc3949ba59abbe56e057f20f883e', N'Phạm Gia Huy', 'Student'),
('he180035', 'e10adc3949ba59abbe56e057f20f883e', N'Hoàng Nhật Minh', 'Student'),
('he180036', 'e10adc3949ba59abbe56e057f20f883e', N'Đỗ Bích Phương', 'Student'),
('he180037', 'e10adc3949ba59abbe56e057f20f883e', N'Vũ Đình Trọng', 'Student'),
('he180038', 'e10adc3949ba59abbe56e057f20f883e', N'Ngô Bảo Châu', 'Student'),
('he180039', 'e10adc3949ba59abbe56e057f20f883e', N'Bùi Tiến Đạt', 'Student'),
('he180040', 'e10adc3949ba59abbe56e057f20f883e', N'Đinh Thùy Dung', 'Student'),
('he180041', 'e10adc3949ba59abbe56e057f20f883e', N'Dương Tuấn Kiệt', 'Student'),
('he180042', 'e10adc3949ba59abbe56e057f20f883e', N'Lý Gia Hân', 'Student'),
('he180043', 'e10adc3949ba59abbe56e057f20f883e', N'Mai Phương Thảo', 'Student'),
('he180044', 'e10adc3949ba59abbe56e057f20f883e', N'Nguyễn Đức Huy', 'Student'),
('he180045', 'e10adc3949ba59abbe56e057f20f883e', N'Phan Văn Hậu', 'Student'),
('he180046', 'e10adc3949ba59abbe56e057f20f883e', N'Tạ Quang Thắng', 'Student'),
('he180047', 'e10adc3949ba59abbe56e057f20f883e', N'Vương Đình Khôi', 'Student'),
('he180048', 'e10adc3949ba59abbe56e057f20f883e', N'Trịnh Thảo Nhi', 'Student'),
('he180049', 'e10adc3949ba59abbe56e057f20f883e', N'Hà Kiều Anh', 'Student'),
('he180050', 'e10adc3949ba59abbe56e057f20f883e', N'Lâm Khánh Chi', 'Student'),
('he180051', 'e10adc3949ba59abbe56e057f20f883e', N'Đoàn Văn Hậu', 'Student'),
('he180052', 'e10adc3949ba59abbe56e057f20f883e', N'Cao Xuân Trường', 'Student'),
('he180053', 'e10adc3949ba59abbe56e057f20f883e', N'Tô Vĩnh Diện', 'Student'),
('he180054', 'e10adc3949ba59abbe56e057f20f883e', N'Trương Bá Chi', 'Student'),
('he180055', 'e10adc3949ba59abbe56e057f20f883e', N'Quách Ngọc Tuyên', 'Student'),
('he180056', 'e10adc3949ba59abbe56e057f20f883e', N'Chu Đăng Khoa', 'Student'),
('he180057', 'e10adc3949ba59abbe56e057f20f883e', N'Nghiêm Xuân Tú', 'Student'),
('he180058', 'e10adc3949ba59abbe56e057f20f883e', N'Thái Bá Dũng', 'Student'),
('he180059', 'e10adc3949ba59abbe56e057f20f883e', N'Lương Thế Vinh', 'Student'),
('he180060', 'e10adc3949ba59abbe56e057f20f883e', N'Võ Nguyên Giáp', 'Student'),
('he180061', 'e10adc3949ba59abbe56e057f20f883e', N'Bùi Phương Nga', 'Student'),
('he180062', 'e10adc3949ba59abbe56e057f20f883e', N'Đỗ Quốc Anh', 'Student'),
('he180063', 'e10adc3949ba59abbe56e057f20f883e', N'Vũ Hà My', 'Student'),
('he180064', 'e10adc3949ba59abbe56e057f20f883e', N'Ngô Tùng Dương', 'Student'),
('he180065', 'e10adc3949ba59abbe56e057f20f883e', N'Phan Thanh Bình', 'Student'),
('he180066', 'e10adc3949ba59abbe56e057f20f883e', N'Trần Tuấn Kiệt', 'Student'),
('he180067', 'e10adc3949ba59abbe56e057f20f883e', N'Lê Minh Châu', 'Student'),
('he180068', 'e10adc3949ba59abbe56e057f20f883e', N'Nguyễn Trọng Phúc', 'Student'),
('he180069', 'e10adc3949ba59abbe56e057f20f883e', N'Hoàng Lan Hương', 'Student'),
('he180070', 'e10adc3949ba59abbe56e057f20f883e', N'Đinh Hữu Khang', 'Student'),
('he180071', 'e10adc3949ba59abbe56e057f20f883e', N'Dương Thảo Linh', 'Student'),
('he180072', 'e10adc3949ba59abbe56e057f20f883e', N'Lý Kiến Thành', 'Student'),
('he180073', 'e10adc3949ba59abbe56e057f20f883e', N'Mai Văn Toàn', 'Student'),
('he180074', 'e10adc3949ba59abbe56e057f20f883e', N'Trương Cẩm Nhung', 'Student'),
('he180075', 'e10adc3949ba59abbe56e057f20f883e', N'Võ Tấn Phát', 'Student');

-- 2.4. Phân công Lớp học trong bảng USER_CLASS
-- Giảng viên: 2 người phụ trách 2 lớp (user_id 1 dạy lớp 1, 2; user_id 2 dạy lớp 3, 4); 1 người phụ trách 1 lớp (user_id 3 dạy lớp 5)
-- Sinh viên: Mỗi lớp 15 sinh viên (Lớp 1: user_id 4..18; Lớp 2: user_id 19..33; Lớp 3: user_id 34..48; Lớp 4: user_id 49..63; Lớp 5: user_id 64..78)
INSERT INTO [dbo].[USER_CLASS] ([user_id], [class_id]) VALUES
-- Giảng viên phụ trách (2 giảng viên dạy 2 lớp, 1 giảng viên dạy 1 lớp)
(1, 1), (1, 2), -- ThS. Hoàng Nhật Nam dạy PRJ301 và CSD201
(2, 3), (2, 4), -- TS. Phan Thị Lan dạy LAB211 và PRF192
(3, 5),         -- ThS. Lê Minh Quang dạy WED201

-- Lớp 1: SE1801 (PRJ301) - 15 sinh viên
(4, 1), (5, 1), (6, 1), (7, 1), (8, 1), (9, 1), (10, 1), (11, 1), (12, 1), (13, 1), (14, 1), (15, 1), (16, 1), (17, 1), (18, 1),

-- Lớp 2: SE1802 (CSD201) - 15 sinh viên
(19, 2), (20, 2), (21, 2), (22, 2), (23, 2), (24, 2), (25, 2), (26, 2), (27, 2), (28, 2), (29, 2), (30, 2), (31, 2), (32, 2), (33, 2),

-- Lớp 3: SE1803 (LAB211) - 15 sinh viên
(34, 3), (35, 3), (36, 3), (37, 3), (38, 3), (39, 3), (40, 3), (41, 3), (42, 3), (43, 3), (44, 3), (45, 3), (46, 3), (47, 3), (48, 3),

-- Lớp 4: SE1804 (PRF192) - 15 sinh viên
(49, 4), (50, 4), (51, 4), (52, 4), (53, 4), (54, 4), (55, 4), (56, 4), (57, 4), (58, 4), (59, 4), (60, 4), (61, 4), (62, 4), (63, 4),

-- Lớp 5: SE1805 (WED201) - 15 sinh viên
(64, 5), (65, 5), (66, 5), (67, 5), (68, 5), (69, 5), (70, 5), (71, 5), (72, 5), (73, 5), (74, 5), (75, 5), (76, 5), (77, 5), (78, 5);

-- 2.5. Thêm Đề bài (Assignments)
INSERT INTO [dbo].[ASSIGNMENTS] ([lecturer_id], [title], [description], [created_at]) VALUES
(1, N'Assignment 1: Data Structures & Sorting Optimization', N'Cài đặt và tối ưu các thuật toán sắp xếp và tìm kiếm mảng dữ liệu.', '2026-09-01 08:00:00'),
(2, N'Assignment 2: Java Web MVC & JDBC Best Practices', N'Phát triển ứng dụng quản lý sinh viên với chuẩn MVC2, an toàn kết nối JDBC.', '2026-09-05 09:30:00'),
(3, N'Assignment 3: AST Static Code Parsing Implementation', N'Viết module bóc tách cây cú pháp trừu tượng phát hiện code smells.', '2026-09-10 14:00:00');

-- 2.6. Thêm Danh mục Luật phân tích tĩnh (Rules)
INSERT INTO [dbo].[RULES] ([rule_code], [category], [name], [weight], [enabled]) VALUES
('PERF01', 'Performance', N'Phát hiện lồng nhiều vòng lặp (Nested Loops Complexity O(N^2) trở lên)', 15, 1),
('PERF02', 'Performance', N'Cộng chuỗi trong vòng lặp thay vì dùng StringBuilder', 10, 1),
('SEC01', 'Security', N'Lỗ hổng SQL Injection (Ghép chuỗi trong câu truy vấn SQL)', 25, 1),
('SEC02', 'Security', N'Rò rỉ tài nguyên (Không giải phóng Connection, PreparedStatement, ResultSet)', 20, 1),
('CODE01', 'Code Smells', N'Độ phức tạp Cyclomatic quá cao (Vượt quá ngưỡng 10)', 10, 1),
('CODE02', 'Code Smells', N'Hàm quá dài hoặc vượt quá giới hạn dòng (God Method)', 5, 1),
('NAM01', 'NamingConvention', N'Vi phạm chuẩn đặt tên Java (CamelCase, PascalCase)', 5, 1),
('MVC01', 'Architecture', N'Vi phạm kiến trúc MVC2 (Truy vấn database trực tiếp trong JSP/View)', 15, 1);

-- 2.7. Thêm 30 Bài nộp (Submissions) của 30 sinh viên đầu tiên (student_id từ 4 đến 33)
INSERT INTO [dbo].[SUBMISSIONS] ([student_id], [assignment_id], [file_path], [sha256_hash], [file_size], [submitted_at], [status]) VALUES
(4, 1, '/uploads/submissions/he180001_lab1.zip', 'a8f5c1d3e2b4f6a7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1', 45210, '2026-09-12 10:15:00', 'Completed'),
(5, 1, '/uploads/submissions/he180002_lab1.zip', 'b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2', 38920, '2026-09-12 10:20:00', 'Completed'),
(6, 1, '/uploads/submissions/he180003_lab1.zip', 'c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3', 41200, '2026-09-12 11:05:00', 'Completed'),
(7, 1, '/uploads/submissions/he180004_lab1.zip', 'd3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4', 52140, '2026-09-12 14:30:00', 'Completed'),
(8, 1, '/uploads/submissions/he180005_lab1.zip', 'e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5', 47800, '2026-09-12 15:10:00', 'Completed'),
(9, 1, '/uploads/submissions/he180006_lab1.zip', 'f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6', 36500, '2026-09-12 16:45:00', 'Completed'),
(10, 1, '/uploads/submissions/he180007_lab1.zip', 'a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7', 43100, '2026-09-13 08:30:00', 'Completed'),
(11, 1, '/uploads/submissions/he180008_lab1.zip', 'b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8', 39800, '2026-09-13 09:12:00', 'Completed'),
(12, 1, '/uploads/submissions/he180009_lab1.zip', 'c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9', 49200, '2026-09-13 10:00:00', 'Completed'),
(13, 1, '/uploads/submissions/he180010_lab1.zip', 'd9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0', 37400, '2026-09-13 11:25:00', 'Completed'),
(14, 2, '/uploads/submissions/he180011_lab2.zip', 'e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1', 61200, '2026-09-13 13:40:00', 'Completed'),
(15, 2, '/uploads/submissions/he180012_lab2.zip', 'f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2', 58400, '2026-09-13 14:15:00', 'Completed'),
(16, 2, '/uploads/submissions/he180013_lab2.zip', 'a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3', 63900, '2026-09-13 15:50:00', 'Completed'),
(17, 2, '/uploads/submissions/he180014_lab2.zip', 'b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4', 55800, '2026-09-13 16:30:00', 'Completed'),
(18, 2, '/uploads/submissions/he180015_lab2.zip', 'c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5', 59100, '2026-09-14 08:20:00', 'Completed'),
(19, 2, '/uploads/submissions/he180016_lab2.zip', 'd5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6', 57300, '2026-09-14 09:05:00', 'Completed'),
(20, 2, '/uploads/submissions/he180017_lab2.zip', 'e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7', 62000, '2026-09-14 10:10:00', 'Completed'),
(21, 2, '/uploads/submissions/he180018_lab2.zip', 'f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8', 54200, '2026-09-14 11:35:00', 'Completed'),
(22, 2, '/uploads/submissions/he180019_lab2.zip', 'a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9', 58700, '2026-09-14 14:00:00', 'Completed'),
(23, 2, '/uploads/submissions/he180020_lab2.zip', 'b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0', 60500, '2026-09-14 15:20:00', 'Completed'),
(24, 3, '/uploads/submissions/he180021_lab3.zip', 'c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1', 72100, '2026-09-15 08:45:00', 'Completed'),
(25, 3, '/uploads/submissions/he180022_lab3.zip', 'd1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2', 68400, '2026-09-15 09:30:00', 'Completed'),
(26, 3, '/uploads/submissions/he180023_lab3.zip', 'e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3', 75300, '2026-09-15 10:15:00', 'Completed'),
(27, 3, '/uploads/submissions/he180024_lab3.zip', 'f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4', 69800, '2026-09-15 11:00:00', 'Completed'),
(28, 3, '/uploads/submissions/he180025_lab3.zip', 'a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5', 73400, '2026-09-15 13:50:00', 'Completed'),
(29, 3, '/uploads/submissions/he180026_lab3.zip', 'b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6', 71200, '2026-09-15 14:40:00', 'Completed'),
(30, 3, '/uploads/submissions/he180027_lab3.zip', 'c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7', 67900, '2026-09-15 15:30:00', 'Completed'),
(31, 3, '/uploads/submissions/he180028_lab3.zip', 'd7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8', 76500, '2026-09-15 16:15:00', 'Completed'),
(32, 3, '/uploads/submissions/he180029_lab3.zip', 'e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9', 70100, '2026-09-15 17:00:00', 'Completed'),
(33, 3, '/uploads/submissions/he180030_lab3.zip', 'f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0', 74200, '2026-09-15 17:45:00', 'Completed');

-- 2.8. Thêm File nguồn (Source Files)
INSERT INTO [dbo].[SOURCE_FILES] ([submission_id], [file_name], [file_type], [loc]) VALUES
(1, 'BubbleSortService.java', 'java', 115),
(1, 'SearchAlgorithm.java', 'java', 85),
(2, 'SortingUtils.java', 'java', 140),
(3, 'DataMatrixProcessor.java', 'java', 210),
(4, 'QuickSortOptimization.java', 'java', 95),
(5, 'ArrayDuplicateFinder.java', 'java', 120),
(11, 'DBContext.java', 'java', 65),
(11, 'UserDAO.java', 'java', 230),
(11, 'LoginServlet.java', 'java', 145),
(12, 'AccountDAO.java', 'java', 190),
(13, 'ProductService.java', 'java', 280),
(21, 'ASTParserEngine.java', 'java', 320),
(21, 'ComplexityCalculator.java', 'java', 185);

-- 2.9. Thêm Phương thức bóc tách AST (Methods)
INSERT INTO [dbo].[METHODS] ([file_id], [method_name], [start_line], [end_line], [loop_depth], [cyclomatic_complexity], [estimated_complexity], [is_recursive]) VALUES
(1, 'sortAscending', 15, 38, 2, 6, 'O(N^2)', 0),
(1, 'swapElements', 40, 48, 0, 1, 'O(1)', 0),
(2, 'binarySearchRecursive', 12, 29, 0, 3, 'O(log N)', 1),
(3, 'calculateMatrixMultiplication', 25, 75, 3, 12, 'O(N^3)', 0),
(5, 'hasDuplicateItems', 18, 42, 2, 5, 'O(N^2)', 0),
(7, 'getConnection', 18, 35, 0, 2, 'O(1)', 0),
(8, 'checkLoginUnsafe', 45, 70, 1, 4, 'O(N)', 0),
(8, 'getAllUsersLeak', 85, 125, 1, 3, 'O(N)', 0),
(10, 'updateBalance', 30, 65, 0, 5, 'O(1)', 0),
(12, 'parseSyntaxTree', 40, 110, 2, 14, 'O(N^2)', 1);

-- 2.10. Thêm Lỗi phát hiện được (Issues) kèm giải thích từ AI
INSERT INTO [dbo].[ISSUES] ([file_id], [method_id], [rule_id], [line_number], [severity], [message], [ai_explanation]) VALUES
(1, 1, 1, 22, 'Warning', N'Phát hiện lồng 2 vòng lặp for dẫn đến độ phức tạp O(N^2)', N'Thuật toán Bubble Sort duyệt qua 2 vòng lặp lồng nhau. Với tập dữ liệu lớn, độ trễ sẽ tăng theo hàm bậc 2. Khuyến nghị chuyển sang Merge Sort hoặc Quick Sort O(N log N).'),
(3, 4, 1, 35, 'Critical', N'Độ phức tạp O(N^3) lồng 3 vòng lặp gây nghẽn hiệu năng nghiêm trọng', N'Phép nhân ma trận truyền thống lồng 3 vòng for. Hãy xem xét thuật toán chia để trị Strassen hoặc áp dụng đa luồng (parallel processing) để giảm thời gian xử lý.'),
(5, 5, 1, 25, 'Warning', N'Tìm trùng lặp phần tử bằng 2 vòng lặp vét cạn O(N^2)', N'Phương thức so sánh từng cặp phần tử bằng 2 vòng for. Bạn nên dùng cấu trúc dữ liệu HashSet để kiểm tra sự tồn tại trong thời gian O(1), đưa độ phức tạp toàn bộ hàm về O(N).'),
(8, 7, 3, 52, 'Critical', N'Ghép trực tiếp biến username vào chuỗi SQL gây nguy cơ SQL Injection', N'Sử dụng String concatenation trong câu truy vấn "SELECT * FROM Users WHERE username = ''" + user + "''" cho phép kẻ tấn công chèn mã độc. Bắt buộc dùng PreparedStatement với dấu hỏi chấm (?).'),
(8, 8, 4, 110, 'Warning', N'Không đóng Connection và ResultSet trong khối finally/try-with-resources', N'Kết nối JDBC không được đóng đúng cách sẽ gây rò rỉ bộ nhớ (memory leak) và cạn kiệt connection pool của SQL Server khi có nhiều request đồng thời.'),
(12, 10, 5, 45, 'Warning', N'Độ phức tạp Cyclomatic là 14 (vượt ngưỡng cho phép 10)', N'Hàm parseSyntaxTree chứa quá nhiều nhánh rẽ if-else và switch lồng nhau. Hãy áp dụng mẫu thiết kế Strategy hoặc Factory Pattern để tách nhỏ logic.');

-- 2.11. Thêm Đề xuất Tối ưu hóa của AI (AI_Optimizations)
INSERT INTO [dbo].[AI_OPTIMIZATIONS] ([method_id], [original_code], [optimized_code], [explanation], [created_at]) VALUES
(5, 
N'public boolean hasDuplicateItems(int[] arr) {
    for (int i = 0; i < arr.length; i++) {
        for (int j = i + 1; j < arr.length; j++) {
            if (arr[i] == arr[j]) return true;
        }
    }
    return false;
}', 
N'public boolean hasDuplicateItems(int[] arr) {
    Set<Integer> seen = new HashSet<>();
    for (int num : arr) {
        if (!seen.add(num)) return true;
    }
    return false;
}', 
N'AI đã tối ưu độ phức tạp từ O(N^2) xuống O(N) nhờ sử dụng bảng băm (HashSet). Thao tác add và contains trên HashSet chỉ mất O(1), giúp tiết kiệm thời gian đáng kể khi mảng có hàng triệu phần tử.', 
'2026-09-12 10:25:00'),

(7, 
N'String sql = "SELECT * FROM Users WHERE username = ''" + username + "'' AND password = ''" + password + "''";
Statement st = connection.createStatement();
ResultSet rs = st.executeQuery(sql);', 
N'String sql = "SELECT * FROM Users WHERE username = ? AND password = ?";
PreparedStatement st = connection.prepareStatement(sql);
st.setString(1, username);
st.setString(2, password);
ResultSet rs = st.executeQuery();', 
N'AI đã chuyển câu truy vấn SQL sang dạng PreparedStatement có tham số hóa để ngăn chặn 100% tấn công SQL Injection và giúp Database Engine lưu cache kế hoạch thực thi (Execution Plan).', 
'2026-09-13 14:00:00');

-- 2.12. Thêm Báo cáo Đánh giá (Analysis Reports) cho 30 bài nộp
INSERT INTO [dbo].[ANALYSIS_REPORTS] ([submission_id], [quality_score], [total_issues], [created_at]) VALUES
(1, 75, 2, '2026-09-12 10:16:00'),
(2, 85, 1, '2026-09-12 10:21:00'),
(3, 70, 3, '2026-09-12 11:06:00'),
(4, 95, 0, '2026-09-12 14:31:00'),
(5, 80, 1, '2026-09-12 15:11:00'),
(6, 90, 1, '2026-09-12 16:46:00'),
(7, 88, 1, '2026-09-13 08:31:00'),
(8, 92, 0, '2026-09-13 09:13:00'),
(9, 78, 2, '2026-09-13 10:01:00'),
(10, 85, 1, '2026-09-13 11:26:00'),
(11, 65, 3, '2026-09-13 13:41:00'),
(12, 82, 1, '2026-09-13 14:16:00'),
(13, 74, 2, '2026-09-13 15:51:00'),
(14, 89, 1, '2026-09-13 16:31:00'),
(15, 96, 0, '2026-09-14 08:21:00'),
(16, 81, 1, '2026-09-14 09:06:00'),
(17, 72, 2, '2026-09-14 10:11:00'),
(18, 94, 0, '2026-09-14 11:36:00'),
(19, 87, 1, '2026-09-14 14:01:00'),
(20, 83, 1, '2026-09-14 15:21:00'),
(21, 68, 3, '2026-09-15 08:46:00'),
(22, 91, 1, '2026-09-15 09:31:00'),
(23, 76, 2, '2026-09-15 10:16:00'),
(24, 88, 1, '2026-09-15 11:01:00'),
(25, 93, 0, '2026-09-15 13:51:00'),
(26, 79, 2, '2026-09-15 14:41:00'),
(27, 86, 1, '2026-09-15 15:31:00'),
(28, 70, 3, '2026-09-15 16:16:00'),
(29, 97, 0, '2026-09-15 17:01:00'),
(30, 84, 1, '2026-09-15 17:46:00');
GO

--------------------------------------------------------------------------------
-- 3. KIỂM TRA TOÀN VẸN DỮ LIỆU SAU KHI TẠO
--------------------------------------------------------------------------------
SELECT 'CLASSES' AS [TableName], COUNT(*) AS [TotalRows] FROM [dbo].[CLASSES]
UNION ALL
SELECT 'USERS (Lecturers)', COUNT(*) FROM [dbo].[USERS] WHERE [role] = 'Lecturer'
UNION ALL
SELECT 'USERS (Students)', COUNT(*) FROM [dbo].[USERS] WHERE [role] = 'Student'
UNION ALL
SELECT 'USER_CLASS (Enrollments/Teachings)', COUNT(*) FROM [dbo].[USER_CLASS]
UNION ALL
SELECT 'ASSIGNMENTS', COUNT(*) FROM [dbo].[ASSIGNMENTS]
UNION ALL
SELECT 'SUBMISSIONS', COUNT(*) FROM [dbo].[SUBMISSIONS]
UNION ALL
SELECT 'SOURCE_FILES', COUNT(*) FROM [dbo].[SOURCE_FILES]
UNION ALL
SELECT 'METHODS', COUNT(*) FROM [dbo].[METHODS]
UNION ALL
SELECT 'RULES', COUNT(*) FROM [dbo].[RULES]
UNION ALL
SELECT 'ISSUES', COUNT(*) FROM [dbo].[ISSUES]
UNION ALL
SELECT 'AI_OPTIMIZATIONS', COUNT(*) FROM [dbo].[AI_OPTIMIZATIONS]
UNION ALL
SELECT 'ANALYSIS_REPORTS', COUNT(*) FROM [dbo].[ANALYSIS_REPORTS];
GO
