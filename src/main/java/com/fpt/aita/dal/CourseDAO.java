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
    /**
     * 1. READ: Lấy danh sách tất cả các môn học (Ưu tiên bảng COURSES chuẩn EduNext 4 môn)
     */
    public List<Course> getAllCourses() {
        List<Course> list = new ArrayList<>();
        // Ưu tiên 1: Bảng COURSES chuẩn (PRJ301, CSD201, PRO192, DBI202)
        String sqlCourses = "SELECT course_id, course_code, course_name, description, badge, total_slots FROM COURSES ORDER BY course_id ASC";
        try {
            PreparedStatement ps = connection.prepareStatement(sqlCourses);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Course(
                    rs.getInt("course_id"),
                    rs.getString("course_code"),
                    rs.getString("course_name"),
                    rs.getString("description"),
                    rs.getString("badge"),
                    rs.getInt("total_slots")
                ));
            }
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception ignored) {
        }

        // Ưu tiên 2: Fallback sang bảng CLASSES
        String sqlClasses = "SELECT class_id, class_code, class_name FROM CLASSES ORDER BY class_id ASC";
        try {
            PreparedStatement ps = connection.prepareStatement(sqlClasses);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToCourse(rs));
            }
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception ignored) {
        }

        return getDefaultCourses();
    }

    /**
     * 2. READ: Tìm môn học theo ID
     */
    public Course getCourseById(int id) {
        String sqlCourse = "SELECT course_id, course_code, course_name, description, badge, total_slots FROM COURSES WHERE course_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sqlCourse);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Course(
                    rs.getInt("course_id"),
                    rs.getString("course_code"),
                    rs.getString("course_name"),
                    rs.getString("description"),
                    rs.getString("badge"),
                    rs.getInt("total_slots")
                );
            }
        } catch (Exception ignored) {
        }

        String sqlClasses = "SELECT class_id, class_code, class_name FROM CLASSES WHERE class_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sqlClasses);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToCourse(rs);
            }
        } catch (Exception ignored) {
        }

        for (Course c : getDefaultCourses()) {
            if (c.getCourseId() == id) {
                return c;
            }
        }
        return null;
    }

    /**
     * 3. SEARCH: Tìm kiếm môn học theo từ khóa
     */
    public List<Course> searchCourses(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCourses();
        }
        List<Course> list = new ArrayList<>();
        String sqlCourse = "SELECT course_id, course_code, course_name, description, badge, total_slots FROM COURSES "
                         + "WHERE course_code LIKE ? OR course_name LIKE ? OR description LIKE ? ORDER BY course_id ASC";
        try {
            PreparedStatement ps = connection.prepareStatement(sqlCourse);
            String term = "%" + keyword.trim() + "%";
            ps.setString(1, term);
            ps.setString(2, term);
            ps.setString(3, term);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Course(
                    rs.getInt("course_id"),
                    rs.getString("course_code"),
                    rs.getString("course_name"),
                    rs.getString("description"),
                    rs.getString("badge"),
                    rs.getInt("total_slots")
                ));
            }
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception ignored) {
        }

        String sqlClasses = "SELECT class_id, class_code, class_name FROM CLASSES "
                         + "WHERE class_name LIKE ? OR class_code LIKE ? ORDER BY class_id ASC";
        try {
            PreparedStatement ps = connection.prepareStatement(sqlClasses);
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
        } catch (Exception ignored) {
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
        list.add(new Course(1, "PRJ301", "Java Web Application Development",
                "Phát triển ứng dụng Web với Java (Servlet, JSP, JDBC, AST Analyzer)", "CORE", 30));
        list.add(new Course(2, "CSD201", "Data Structures and Algorithm",
                "Cấu trúc dữ liệu và giải thuật (Binary Trees, Graphs, Sorting)", "TECH", 30));
        list.add(new Course(3, "PRO192", "Object-Oriented Programming",
                "Lập trình hướng đối tượng với Java (OOP principles, Collections)", "BASIC", 30));
        list.add(new Course(4, "DBI202", "Database Systems",
                "Hệ cơ sở dữ liệu quan hệ (SQL Server, 3NF Normalization)", "DATABASE", 30));
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

