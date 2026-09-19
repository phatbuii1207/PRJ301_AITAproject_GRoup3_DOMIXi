<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Trang chủ - AITA Code Analysis</title>
    <style>
        body { font-family: 'Segoe UI', Arial, sans-serif; background: #0f172a; color: #fff; padding: 40px; }
        .container { max-width: 800px; margin: 0 auto; background: #1e293b; padding: 30px; border-radius: 12px; }
        .badge { background: #0284c7; padding: 4px 10px; border-radius: 4px; font-size: 14px; }
        .btn-logout { background: #ef4444; color: #fff; text-decoration: none; padding: 8px 16px; border-radius: 6px; display: inline-block; margin-top: 20px; }
    </style>
</head>
<body>
    <div class="container">
        <h2>🎉 Đăng nhập thành công từ SQL Server!</h2>
        <p>Xin chào: <b>${sessionScope.currentUser.fullName}</b></p>
        <p>Tên tài khoản: <code>${sessionScope.currentUser.username}</code></p>
        <p>Vai trò: <span class="badge">${sessionScope.currentUser.role}</span></p>
        <c:if test="${sessionScope.currentUser.classId != null}">
            <p>Mã lớp ID: <b>#${sessionScope.currentUser.classId}</b></p>
        </c:if>
        
        <hr style="border-color: #334155; margin: 20px 0;"/>
        <p><i>Hệ thống đã kết nối trực tiếp với CSDL <b>AST_CodeAnalysisDB</b> thông qua JDBC Driver.</i></p>
        <a href="logout" class="btn-logout">Đăng Xuất</a>
    </div>
</body>
</html>
