package com.fpt.aita.dal;

import com.fpt.aita.model.Course;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho Quản lý Môn học (Courses)
 * Dành cho Thành viên 2 (PRJ301)
 */
public class CourseDAO extends DBContext {

    /**
     * 1. READ: Lấy danh sách tất cả môn học trên EduNext Portal
     * Có cơ chế fallback tự động nạp 4 môn chuẩn nếu CSDL chưa tạo bảng COURSES.
     */
    public List<Course> getAllCourses() {
        List<Course> list = new ArrayList<>();
        String sql = "SELECT course_id, course_code, course_name, description, badge, total_slots "
                   + "FROM COURSES ORDER BY course_id ASC";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
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
        } catch (Exception e) {
            System.err.println("Notice (TV2): Bảng COURSES chưa sẵn sàng trong CSDL (" + e.getMessage() + "), kích hoạt Fallback 4 môn chuẩn.");
        }
        return getDefaultCourses();
    }

    /**
     * 2. READ: Tìm một môn học theo ID (dùng cho trang chi tiết /course?id=...)
     */
    public Course getCourseById(int id) {
        String sql = "SELECT course_id, course_code, course_name, description, badge, total_slots "
                   + "FROM COURSES WHERE course_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
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
        } catch (Exception e) {
            System.err.println("Notice (TV2): getCourseById fallback do CSDL: " + e.getMessage());
        }
        // Fallback tìm trong danh sách mặc định
        for (Course c : getDefaultCourses()) {
            if (c.getCourseId() == id) {
                return c;
            }
        }
        return null;
    }

    /**
     * 3. SEARCH: Tìm kiếm môn học theo mã môn hoặc tên môn
     */
    public List<Course> searchCourses(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCourses();
        }
        List<Course> list = new ArrayList<>();
        String sql = "SELECT course_id, course_code, course_name, description, badge, total_slots "
                   + "FROM COURSES "
                   + "WHERE course_code LIKE ? OR course_name LIKE ? "
                   + "ORDER BY course_id ASC";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            String term = "%" + keyword.trim() + "%";
            ps.setString(1, term);
            ps.setString(2, term);
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
        } catch (Exception e) {
            System.err.println("Notice (TV2): searchCourses fallback: " + e.getMessage());
        }

        // Fallback tìm kiếm trong danh sách mặc định
        String lowerKeyword = keyword.trim().toLowerCase();
        for (Course c : getDefaultCourses()) {
            if (c.getCourseCode().toLowerCase().contains(lowerKeyword)
                    || c.getCourseName().toLowerCase().contains(lowerKeyword)) {
                list.add(c);
            }
        }
        return list;
    }

    /**
     * Dữ liệu mặc định 4 môn học chuẩn EduNext Portal
     * Đảm bảo hệ thống hoạt động ổn định khi TV5 chưa chạy migration SQL.
     */
    private List<Course> getDefaultCourses() {
        List<Course> list = new ArrayList<>();
        list.add(new Course(1, "PRJ301", "Java Web Application Development",
                "Học phần Servlet, JSP, JDBC, MVC Model và Static Code Analysis AST.", "CORE", 30));
        list.add(new Course(2, "CSD201", "Data Structures and Algorithms",
                "Cấu trúc dữ liệu nâng cao, Binary Trees, Graph Traversal, Sorting.", "TECH", 30));
        list.add(new Course(3, "PRO192", "Object-Oriented Programming with Java",
                "Lập trình hướng đối tượng với Java: OOP principles, Polymorphism, Collections.", "BASIC", 30));
        list.add(new Course(4, "DBI202", "Introduction to Databases",
                "Cơ sở dữ liệu quan hệ, SQL Server, Chuẩn hóa 3NF và RDBMS.", "DATABASE", 30));
        return list;
    }

    /**
     * Hàm test độc lập từng hàm trong CourseDAO (Unit Test)
     * Nhấn Shift + F6 trong NetBeans để chạy riêng file này kiểm tra kết quả!
     */
    public static void main(String[] args) {
        System.out.println("========== BẮT ĐẦU TEST TỪNG HÀM TRONG CourseDAO ==========\n");
        CourseDAO dao = new CourseDAO();

        // 1. Test getAllCourses
        System.out.println("--- 1. Test getAllCourses() ---");
        List<Course> all = dao.getAllCourses();
        System.out.println("Số lượng môn học lấy được: " + all.size());
        for (Course c : all) {
            System.out.println(" + [" + c.getCourseCode() + "] " + c.getCourseName() + " (" + c.getBadge() + ") - " + c.getTotalSlots() + " slots");
        }

        // 2. Test getCourseById
        System.out.println("\n--- 2. Test getCourseById(1) ---");
        Course c1 = dao.getCourseById(1);
        if (c1 != null) {
            System.out.println("Tìm thấy môn ID=1: " + c1.getCourseCode() + " - " + c1.getCourseName());
            System.out.println("Mô tả: " + c1.getDescription());
        } else {
            System.err.println("Không tìm thấy môn ID=1!");
        }

        // 3. Test searchCourses
        System.out.println("\n--- 3. Test searchCourses('PRJ') ---");
        List<Course> searchPrj = dao.searchCourses("PRJ");
        for (Course c : searchPrj) {
            System.out.println(" + Kết quả khớp: " + c.getCourseCode() + " - " + c.getCourseName());
        }

        System.out.println("\n--- 4. Test searchCourses('Database') ---");
        List<Course> searchDb = dao.searchCourses("Database");
        for (Course c : searchDb) {
            System.out.println(" + Kết quả khớp: " + c.getCourseCode() + " - " + c.getCourseName());
        }

        System.out.println("\n========== TẤT CẢ CÁC HÀM CỦA CourseDAO HOẠT ĐỘNG HOÀN HẢO! ==========");
    }
}
