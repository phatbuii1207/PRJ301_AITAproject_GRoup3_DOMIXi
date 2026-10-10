<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>Trang chủ - AITA Code Analysis</title>
        <style>
            body {
                font-family: 'Segoe UI', Arial, sans-serif;
                background: #0f172a;
                color: #fff;
                padding: 40px;
            }
            .container {
                max-width: 800px;
                margin: 0 auto;
                background: #1e293b;
                padding: 30px;
                border-radius: 12px;
            }
            .badge {
                background: #0284c7;
                padding: 4px 10px;
                border-radius: 4px;
                font-size: 14px;
            }
            .btn-logout {
                background: #ef4444;
                color: #fff;
                text-decoration: none;
                padding: 8px 16px;
                border-radius: 6px;
                display: inline-block;
                margin-top: 20px;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <c:choose>
                <c:when test="${not empty sessionScope.currentUser}">
                    <h2>🎉 Đăng nhập thành công từ SQL Server!</h2>
                    <p>Xin chào: <b>${sessionScope.currentUser.fullName}</b></p>
                    <p>Tên tài khoản: <code>${sessionScope.currentUser.username}</code></p>
                    <p>Vai trò hệ thống: <span class="badge">${sessionScope.currentUser.role}</span></p>
                        <c:if test="${sessionScope.currentUser.classId != null}">
                        <p>Mã lớp ID: <b>#${sessionScope.currentUser.classId}</b></p>
                    </c:if>

                    <hr style="border-color: #334155; margin: 25px 0;"/>

                    <div style="margin-bottom: 25px;">
                        <h3>Lối tắt theo phân quyền (Role Navigation):</h3>
                        <c:choose>
                            <c:when test="${sessionScope.currentUser.role == 'Lecturer'}">
                                <a href="assignments" style="background: #0284c7; color: #fff; text-decoration: none; padding: 12px 20px; border-radius: 8px; font-weight: bold; display: inline-block;">
                                    📚 Đi đến Quản Lý Đề Bài Tập (CRUD Giảng Viên) &rarr;
                                </a>
                            </c:when>
                            <c:otherwise>
                                <a href="student-assignments" style="background: #0284c7; color: #fff; text-decoration: none; padding: 12px 20px; border-radius: 8px; font-weight: bold; display: inline-block;">
                                    💻 Đi đến Danh Sách Bài Tập & Nộp Bài (Sinh Viên) &rarr;
                                </a>
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <p style="color: #94a3b8; font-size: 13px;"><i>Hệ thống kết nối trực tiếp CSDL <b>AST_CodeAnalysisDB</b> thông qua JDBC Driver.</i></p>
                    <a href="logout" class="btn-logout">🚪 Đăng Xuất</a>
                </c:when>
                <c:otherwise>
                    <h2>⚠️ Bạn chưa đăng nhập</h2>
                    <p>Vui lòng đăng nhập để truy cập hệ thống AITA.</p>
                    <a href="login" style="background: #0284c7; color: #fff; text-decoration: none; padding: 10px 20px; border-radius: 6px; display: inline-block;">Đăng nhập ngay</a>
                </c:otherwise>
            </c:choose>
        </div>
    </body>
</html>
