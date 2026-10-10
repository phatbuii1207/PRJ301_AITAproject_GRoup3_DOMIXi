<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>Đăng nhập - AITA Static Code Analysis</title>
        <style>
            body {
                font-family: 'Segoe UI', Arial, sans-serif;
                background: #0f172a;
                color: #fff;
                display: flex;
                justify-content: center;
                align-items: center;
                height: 100vh;
                margin: 0;
            }
            .login-card {
                background: #1e293b;
                padding: 40px;
                border-radius: 12px;
                box-shadow: 0 8px 24px rgba(0,0,0,0.5);
                width: 360px;
            }
            h2 {
                text-align: center;
                color: #38bdf8;
                margin-bottom: 24px;
            }
            .form-group {
                margin-bottom: 18px;
            }
            label {
                display: block;
                margin-bottom: 6px;
                font-size: 14px;
                color: #94a3b8;
            }
            input {
                width: 100%;
                padding: 10px;
                border-radius: 6px;
                border: 1px solid #334155;
                background: #0f172a;
                color: #fff;
                box-sizing: border-box;
            }
            button {
                width: 100%;
                padding: 12px;
                background: #0284c7;
                color: #fff;
                border: none;
                border-radius: 6px;
                font-weight: bold;
                cursor: pointer;
                font-size: 16px;
                margin-top: 10px;
            }
            button:hover {
                background: #0369a1;
            }
            .error {
                color: #f87171;
                background: rgba(239,68,68,0.1);
                padding: 10px;
                border-radius: 6px;
                margin-bottom: 16px;
                font-size: 14px;
                text-align: center;
            }
            .demo-box {
                margin-top: 20px;
                font-size: 12px;
                color: #64748b;
                border-top: 1px solid #334155;
                padding-top: 12px;
            }
        </style>
    </head>
    <body>
        <div class="login-card">
            <h2>AITA Portal</h2>
            <c:if test="${not empty error}">
                <div class="error">${error}</div>
            </c:if>
            <form action="login" method="POST">
                <div class="form-group">
                    <label>Tên đăng nhập:</label>
                    <input type="text" name="username" placeholder="vd: gv_namhn hoặc he180001" required />
                </div>
                <div class="form-group">
                    <label>Mật khẩu:</label>
                    <input type="password" name="password" placeholder="Mặc định: 123456" required />
                </div>
                <button type="submit">Đăng Nhập</button>
            </form>
            <div class="demo-box">
                <b>Tài khoản test trong SQL:</b><br/>
                - Giảng viên: <code>gv_namhn</code> / <code>123456</code><br/>
                - Sinh viên: <code>he180001</code> / <code>123456</code>
            </div>
        </div>
    </body>
</html>
