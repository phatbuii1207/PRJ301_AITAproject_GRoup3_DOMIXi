package com.fpt.aita.controller;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * StudentAssignmentServlet: Chuyển hướng toàn bộ truy cập cũ về Cổng EduNext Home
 * Giao diện và nghiệp vụ Nộp bài tập của Sinh viên đã được tích hợp tập trung vào từng Môn học (/course?id=...)
 */
@WebServlet(name = "StudentAssignmentServlet", urlPatterns = {"/student-assignments", "/submissions"})
public class StudentAssignmentServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("home");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("home");
    }
}
