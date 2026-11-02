-- ============================================================================
-- SCRIPT MOCK DATA BỔ SUNG CHO BÁO CÁO PHÂN TÍCH (ANALYSIS_REPORTS, ISSUES, AI_OPTIMIZATIONS)
-- Phục vụ kiểm thử giao diện Báo cáo Đánh giá Mã nguồn (View + Controller)
-- ============================================================================

USE [AST_CodeAnalysisDB];
GO

-- 1. Đảm bảo các Rules cơ bản có mặt đầy đủ
IF NOT EXISTS (SELECT 1 FROM [dbo].[RULES] WHERE [rule_code] = 'RULE_CLEAN_NAMING')
BEGIN
    INSERT INTO [dbo].[RULES] ([rule_code], [category], [name], [weight], [enabled]) VALUES
    ('RULE_CLEAN_NAMING', 'Code Smells', N'Quy ước đặt tên biến/hàm không theo chuẩn Java Naming Convention', 3, 1);
END
GO

IF NOT EXISTS (SELECT 1 FROM [dbo].[RULES] WHERE [rule_code] = 'RULE_MEM_LEAK')
BEGIN
    INSERT INTO [dbo].[RULES] ([rule_code], [category], [name], [weight], [enabled]) VALUES
    ('RULE_MEM_LEAK', 'Performance', N'Khởi tạo mảng bộ nhớ quá lớn trong vòng lặp gây nguy cơ OutOfMemoryError', 8, 1);
END
GO

-- 2. Mock thêm phương thức và đề xuất tối ưu cho Bài nộp 1 (BubbleSortService.java - Submission 1)
-- Method 1: sortAscending (file_id 1)
IF NOT EXISTS (SELECT 1 FROM [dbo].[AI_OPTIMIZATIONS] WHERE [method_id] = 1)
BEGIN
    INSERT INTO [dbo].[AI_OPTIMIZATIONS] ([method_id], [original_code], [optimized_code], [explanation], [created_at])
    VALUES (
        1,
        N'public void sortAscending(int[] arr) {
    int n = arr.length;
    for (int i = 0; i < n - 1; i++) {
        for (int j = 0; j < n - i - 1; j++) {
            if (arr[j] > arr[j + 1]) {
                int temp = arr[j];
                arr[j] = arr[j + 1];
                arr[j + 1] = temp;
            }
        }
    }
}',
        N'public void sortAscending(int[] arr) {
    if (arr == null || arr.length <= 1) return;
    // Tối ưu thuật toán: Sử dụng Dual-Pivot Quicksort độ phức tạp O(N log N)
    Arrays.sort(arr);
}',
        N'AI đã phát hiện thuật toán Bubble Sort với 2 vòng for lồng nhau dẫn đến độ phức tạp O(N^2). Với mảng 100,000 phần tử, thời gian chạy có thể mất vài giây. Việc chuyển sang Dual-Pivot Quicksort giúp giảm độ phức tạp xuống O(N log N), tăng tốc độ xử lý hơn 150 lần.',
        GETDATE()
    );
END
GO

-- Thêm issue thứ 2 cho Submission 1 (file_id 1) để bảng lỗi có nhiều dòng trực quan
IF NOT EXISTS (SELECT 1 FROM [dbo].[ISSUES] WHERE [file_id] = 1 AND [line_number] = 18)
BEGIN
    INSERT INTO [dbo].[ISSUES] ([file_id], [method_id], [rule_id], [line_number], [severity], [message], [ai_explanation])
    VALUES (
        1, 1, 6, 18, 'Info',
        N'Biến cờ hiệu hoặc biến đếm đặt tên ngắn (i, j, n) gây khó bảo trì trong khối code phức tạp',
        N'Quy chuẩn Clean Code của Oracle khuyến nghị đặt tên biến có ý nghĩa ngữ cảnh như outerIndex, innerIndex để người đọc code hiểu rõ phạm vi xử lý.'
    );
END
GO

-- 3. Mock thêm dữ liệu cho Bài nộp 3 (DataMatrixProcessor.java - Submission 3) - Điểm kém (45 điểm, màu Đỏ)
-- Method 4: calculateMatrixMultiplication (file_id 3, belongs to submission 2 or 3)
-- Cập nhật điểm của submission 3 xuống 45 để kiểm thử trạng thái Điểm Kém (Bad / Red)
UPDATE [dbo].[ANALYSIS_REPORTS]
SET [quality_score] = 45, [total_issues] = 3
WHERE [submission_id] = 3;
GO

IF NOT EXISTS (SELECT 1 FROM [dbo].[AI_OPTIMIZATIONS] WHERE [method_id] = 4)
BEGIN
    INSERT INTO [dbo].[AI_OPTIMIZATIONS] ([method_id], [original_code], [optimized_code], [explanation], [created_at])
    VALUES (
        4,
        N'public int[][] calculateMatrixMultiplication(int[][] a, int[][] b) {
    int n = a.length;
    int[][] c = new int[n][n];
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            c[i][j] = 0;
            for (int k = 0; k < n; k++) {
                c[i][j] += a[i][k] * b[k][j];
            }
        }
    }
    return c;
}',
        N'public int[][] calculateMatrixMultiplication(int[][] a, int[][] b) {
    int n = a.length;
    int[][] c = new int[n][n];
    // Tối ưu hóa Cache Locality (truy cập bộ nhớ đệm tuần tự): Đổi thứ tự vòng for i-k-j
    for (int i = 0; i < n; i++) {
        for (int k = 0; k < n; k++) {
            int r = a[i][k];
            for (int j = 0; j < n; j++) {
                c[i][j] += r * b[k][j];
            }
        }
    }
    return c;
}',
        N'Thuật toán nhân ma trận ban đầu duyệt i-j-k gây lỗi Cache Miss nghiêm trọng trong kiến trúc vi xử lý hiện đại vì mảng b[k][j] bị nhảy dòng liên tục. Đổi thứ tự sang i-k-j tận dụng tối đa CPU L1/L2 Cache Line, tăng hiệu năng gấp 10-20 lần mà không cần thay đổi cấu trúc dữ liệu.',
        GETDATE()
    );
END
GO

-- 4. Cập nhật bài nộp 5 (Submission 5) có trạng thái "Processing" để kiểm thử màn hình "Đang chấm"
UPDATE [dbo].[SUBMISSIONS]
SET [status] = 'Processing'
WHERE [submission_id] = 5;

-- Xóa tạm analysis_report của submission 5 (nếu cần test giao diện Đang chấm)
-- DELETE FROM [dbo].[ANALYSIS_REPORTS] WHERE [submission_id] = 5;

PRINT N'===> ĐÃ BỔ SUNG MOCK DATA PHÂN TÍCH MÃ NGUỒN VÀ AI OPTIMIZATION THÀNH CÔNG!';
GO
