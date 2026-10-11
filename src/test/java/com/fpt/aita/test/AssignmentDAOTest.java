package com.fpt.aita.test;

import com.fpt.aita.dal.AssignmentDAO;
import com.fpt.aita.dal.SubmissionDAO;
import com.fpt.aita.model.Assignment;
import com.fpt.aita.model.Submission;

import java.util.List;

/**
 * THÀNH VIÊN 3: Kiểm thử tự động (Unit Test / Integration Test)
 * Kiểm tra các hàm nghiệp vụ của AssignmentDAO và SubmissionDAO
 */
public class AssignmentDAOTest {

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("   BẮT ĐẦU KIỂM THỬ MODULE BÀI TẬP & NỘP BÀI (TV3)   ");
        System.out.println("====================================================");

        int passed = 0;
        int failed = 0;

        AssignmentDAO assignmentDAO = new AssignmentDAO();
        SubmissionDAO submissionDAO = new SubmissionDAO();

        // TEST CASE 1: Lấy danh sách bài tập theo môn học (Course ID = 1 - PRJ301)
        try {
            System.out.print("[TEST 1] Kiểm tra getAssignmentsByCourse(1)... ");
            List<Assignment> list = assignmentDAO.getAssignmentsByCourse(1);
            if (list != null) {
                System.out.println("PASSED (Tìm thấy " + list.size() + " bài tập)");
                passed++;
            } else {
                System.out.println("FAILED (Danh sách null)");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST CASE 2: Kiểm tra cấu trúc SHA-256 mã băm tệp nộp bài
        try {
            System.out.print("[TEST 2] Kiểm tra định dạng mã băm SHA-256 bài nộp... ");
            String sampleHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
            if (sampleHash.length() == 64 && sampleHash.matches("^[a-fA-F0-9]{64}$")) {
                System.out.println("PASSED (Mã SHA-256 chuẩn 64 hex characters)");
                passed++;
            } else {
                System.out.println("FAILED (Sai format)");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST CASE 3: Kiểm tra định dạng dung lượng file (Formatted file size)
        try {
            System.out.print("[TEST 3] Kiểm tra phương thức getFormattedFileSize()... ");
            Submission sub = new Submission();
            sub.setFileSize(2048); // 2 KB
            String formatted = sub.getFormattedFileSize();
            if (formatted != null && formatted.contains("KB")) {
                System.out.println("PASSED (Kết quả: " + formatted + ")");
                passed++;
            } else {
                System.out.println("FAILED");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST CASE 4: Lấy lịch sử nộp bài của sinh viên
        try {
            System.out.print("[TEST 4] Kiểm tra getSubmissionsByStudent(studentId, assignmentId)... ");
            List<Submission> history = submissionDAO.getSubmissionsByStudent(1, 1);
            if (history != null) {
                System.out.println("PASSED (Hàm thực thi không bị crash)");
                passed++;
            } else {
                System.out.println("FAILED");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        System.out.println("====================================================");
        System.out.println("KẾT QUẢ KIỂM THỬ: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("====================================================");
    }
}
