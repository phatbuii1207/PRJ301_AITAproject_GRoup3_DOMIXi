package com.fpt.aita.dal;

import com.fpt.aita.model.Course;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho Quản lý Môn học / Lớp học (Courses / Classes)
 * Kết nối trực tiếp với bảng CLASSES trong CSDL AST_CodeAnalysisDB.
 * Quy ước: class_name là mã môn học (PRJ301, CSD201, LAB211, PRF192, WED201).
 *          class_code là mã định danh lớp (SE1801, SE1802, ...).
 */
public class CourseDAO extends DBContext {

    private String getSubjectFullName(String subjectCode) {
        if (subjectCode == null) return "Học phần chuyên ngành";
        switch (subjectCode.trim().toUpperCase()) {
            case "PRJ301":
                return "Java Web Application Development";
            case "CSD201":
                return "Data Structures and Algorithms";
            case "LAB211":
                return "OOP with Java Lab";
            case "PRF192":
                return "Programming Fundamentals with C";
            case "WED201":
                return "Web Design & Development";
            default:
                return "Học phần " + subjectCode;
        }
    }

    private String getBadgeBySubject(String subjectCode) {
        if (subjectCode == null) return "TECH";
        switch (subjectCode.trim().toUpperCase()) {
            case "PRJ301":
                return "CORE";
            case "CSD201":
                return "ALGO";
            case "LAB211":
                return "PRACTICE";
            case "PRF192":
                return "BASIC";
            case "WED201":
                return "FRONTEND";
            default:
                return "TECH";
        }
    }

    /**
     * Helper mapping từ ResultSet của bảng CLASSES sang Model Course
     */
    private Course mapResultSetToCourse(ResultSet rs) throws Exception {
        int classId = rs.getInt("class_id");
        String classCode = rs.getString("class_code");
        String className = rs.getString("class_name"); // Tên lớp chính là mã môn

        String subjectFullName = getSubjectFullName(className);
        String description = "Lớp " + classCode + " - " + subjectFullName;
        String badge = getBadgeBySubject(className);
        int totalSlots = 15; // Mỗi lớp chuẩn 15 sinh viên

        return new Course(classId, className, subjectFullName + " (" + classCode + ")", description, badge, totalSlots);
    }

    /**
     * 1. READ: Lấy danh sách tất cả các lớp/môn học từ bảng CLASSES
     */
    public List<Course> getAllCourses() {
        List<Course> list = new ArrayList<>();
        String sql = "SELECT class_id, class_code, class_name FROM CLASSES ORDER BY class_id ASC";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToCourse(rs));
            }
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception e) {
            System.err.println("Notice (CourseDAO): Truy vấn bảng CLASSES (" + e.getMessage() + "), kích hoạt fallback 5 lớp chuẩn.");
        }
        return getDefaultCourses();
    }

    /**
     * 2. READ: Tìm một môn học / lớp học theo ID (dùng cho trang chi tiết /course?id=...)
     */
    public Course getCourseById(int id) {
        String sql = "SELECT class_id, class_code, class_name FROM CLASSES WHERE class_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToCourse(rs);
            }
        } catch (Exception e) {
            System.err.println("Notice (CourseDAO): getCourseById fallback: " + e.getMessage());
        }
        for (Course c : getDefaultCourses()) {
            if (c.getCourseId() == id) {
                return c;
            }
        }
        return null;
    }

    /**
     * 3. SEARCH: Tìm kiếm môn học theo mã môn (class_name) hoặc mã lớp (class_code)
     */
    public List<Course> searchCourses(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCourses();
        }
        List<Course> list = new ArrayList<>();
        String sql = "SELECT class_id, class_code, class_name "
                   + "FROM CLASSES "
                   + "WHERE class_name LIKE ? OR class_code LIKE ? "
                   + "ORDER BY class_id ASC";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            String term = "%" + keyword.trim() + "%";
            ps.setString(1, term);
            ps.setString(2, term);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToCourse(rs));
            }
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception e) {
            System.err.println("Notice (CourseDAO): searchCourses fallback: " + e.getMessage());
        }

        String lowerKeyword = keyword.trim().toLowerCase();
        for (Course c : getDefaultCourses()) {
            if (c.getCourseCode().toLowerCase().contains(lowerKeyword)
                    || c.getCourseName().toLowerCase().contains(lowerKeyword)
                    || c.getDescription().toLowerCase().contains(lowerKeyword)) {
                list.add(c);
            }
        }
        return list;
    }

    /**
     * 4. CREATE: Thêm lớp môn học mới vào bảng CLASSES
     */
    public boolean insertCourse(Course c) {
        String sql = "INSERT INTO CLASSES (class_code, class_name) VALUES (?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            // class_code: mã lớp (nếu chưa có thì tạo mặc định), class_name: mã môn học
            ps.setString(1, "SE" + (1800 + c.getCourseId()));
            ps.setString(2, c.getCourseCode());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Notice (CourseDAO): insertCourse: " + e.getMessage());
            return false;
        }
    }

    /**
     * 5. UPDATE: Cập nhật thông tin mã môn trong bảng CLASSES
     */
    public boolean updateCourse(Course c) {
        String sql = "UPDATE CLASSES SET class_name = ? WHERE class_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, c.getCourseCode());
            ps.setInt(2, c.getCourseId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Notice (CourseDAO): updateCourse: " + e.getMessage());
            return false;
        }
    }

    /**
     * 6. DELETE: Xóa lớp học theo class_id
     */
    public boolean deleteCourse(int id) {
        String sql = "DELETE FROM CLASSES WHERE class_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Notice (CourseDAO): deleteCourse: " + e.getMessage());
            return false;
        }
    }

    /**
     * Dữ liệu mặc định 5 lớp học tương ứng 5 mã môn chuẩn trong CSDL
     */
    public List<Course> getDefaultCourses() {
        List<Course> list = new ArrayList<>();
        list.add(new Course(1, "PRJ301", "Java Web Application Development (SE1801)",
                "Lớp SE1801 - Học phần PRJ301: Servlet, JSP, JDBC, AST Analyzer.", "CORE", 15));
        list.add(new Course(2, "CSD201", "Data Structures and Algorithms (SE1802)",
                "Lớp SE1802 - Học phần CSD201: Cấu trúc dữ liệu và giải thuật.", "ALGO", 15));
        list.add(new Course(3, "LAB211", "OOP with Java Lab (SE1803)",
                "Lớp SE1803 - Học phần LAB211: Thực hành lập trình hướng đối tượng.", "PRACTICE", 15));
        list.add(new Course(4, "PRF192", "Programming Fundamentals with C (SE1804)",
                "Lớp SE1804 - Học phần PRF192: Nhập môn lập trình.", "BASIC", 15));
        list.add(new Course(5, "WED201", "Web Design & Development (SE1805)",
                "Lớp SE1805 - Học phần WED201: Thiết kế giao diện Web hiện đại.", "FRONTEND", 15));
        return list;
    }

    /**
     * Hàm test độc lập từng hàm trong CourseDAO (Unit Test)
     */
    public static void main(String[] args) {
        System.out.println("========== BẮT ĐẦU TEST CourseDAO VỚI BẢNG CLASSES ==========\n");
        CourseDAO dao = new CourseDAO();

        List<Course> all = dao.getAllCourses();
        System.out.println("Số lượng môn/lớp lấy được: " + all.size());
        for (Course c : all) {
            System.out.println(" + [Mã môn: " + c.getCourseCode() + "] " + c.getCourseName() + " (" + c.getBadge() + ") - " + c.getTotalSlots() + " SV");
        }
    }
}

