<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng nhập - MIXI ASSIGN</title>
    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }

        body {
            display: flex;
            height: 100vh;
            background-color: #ffffff;
        }

        /* Left Panel */
        .left-panel {
            flex: 1;
            background-color: #d4efff;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            padding: 40px;
            text-align: center;
        }

        .left-panel .logo-img {
            margin-bottom: 40px;
            display: flex;
            align-items: center;
            font-weight: 500;
            color: #333;
            font-size: 16px;
        }

        .left-panel h1 {
            color: #0076c8;
            font-size: 32px;
            font-weight: 800;
            margin-bottom: 20px;
            letter-spacing: 1px;
        }

        .left-panel p {
            color: #005a9e;
            font-size: 15px;
            max-width: 380px;
            line-height: 1.6;
        }

        /* Right Panel */
        .right-panel {
            flex: 1;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            padding: 40px;
            background-color: #ffffff;
        }

        .login-container {
            width: 100%;
            max-width: 420px;
            padding: 0 20px;
        }

        .brand-logo {
            font-size: 22px;
            font-weight: 800;
            margin-bottom: 24px;
        }
        
        .brand-logo .m { color: #f26522; }
        .brand-logo .i1 { color: #0076c8; }
        .brand-logo .x { color: #0076c8; }
        .brand-logo .i2 { color: #0076c8; }
        .brand-logo .assign { color: #0076c8; margin-left: 5px;}

        h2 {
            font-size: 24px;
            color: #111;
            margin-bottom: 10px;
            font-weight: 700;
        }

        .subtitle {
            color: #666;
            font-size: 14px;
            margin-bottom: 30px;
        }

        .error-message {
            background-color: #ffeef0;
            color: #dc3545;
            padding: 12px;
            border-radius: 6px;
            text-align: center;
            font-size: 14px;
            margin-bottom: 20px;
        }

        .form-group {
            margin-bottom: 16px;
        }

        .form-control {
            width: 100%;
            padding: 12px 16px;
            border: 1px solid #dcdfe6;
            border-radius: 6px;
            font-size: 14px;
            outline: none;
            transition: border-color 0.2s;
        }

        .form-control:focus {
            border-color: #0076c8;
        }

        .form-control::placeholder {
            color: #999;
        }

        .form-options {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 24px;
            font-size: 13px;
        }

        .checkbox-label {
            display: flex;
            align-items: center;
            color: #555;
            cursor: pointer;
        }

        .checkbox-label input {
            margin-right: 8px;
        }

        .forgot-password {
            color: #1a73e8;
            text-decoration: none;
            font-weight: 600;
        }

        .btn-submit {
            width: 100%;
            padding: 12px;
            background-color: #1a73e8;
            color: white;
            border: none;
            border-radius: 6px;
            font-size: 15px;
            font-weight: 600;
            cursor: pointer;
            transition: background-color 0.2s;
            margin-bottom: 30px;
        }

        .btn-submit:hover {
            background-color: #155db2;
        }

        .register-link {
            text-align: center;
            font-size: 14px;
            color: #666;
        }

        .register-link a {
            color: #1a73e8;
            text-decoration: none;
            font-weight: 600;
        }

        /* Responsive */
        @media (max-width: 768px) {
            body {
                flex-direction: column;
            }
            .left-panel, .right-panel {
                flex: none;
                width: 100%;
                min-height: 50vh;
            }
        }
    </style>
</head>
<body>

    <!-- Left Panel -->
    <div class="left-panel">
        <div class="logo-img">
            <span>Mixi Learning</span>
        </div>
        <h1>MIXI ASSIGN</h1>
        <p>Nền tảng học tập kiến tạo xã hội. Kết nối Giảng viên và<br>Sinh viên thông qua phương pháp học tập chủ động.</p>
    </div>

    <!-- Right Panel -->
    <div class="right-panel">
        <div class="login-container">
            <div class="brand-logo">
                <span class="m">M</span><span class="i1">I</span><span class="x">X</span><span class="i2">I</span> <span class="assign">ASSIGN</span>
            </div>
            
            <h2>Chào mừng trở lại! 👋</h2>
            <p class="subtitle">Vui lòng đăng nhập để tiếp tục vào hệ thống</p>

            <c:if test="${not empty sessionScope.flashSuccess}">
                <div style="background:#dcfce7; color:#166534; padding:10px 14px; border-radius:8px; margin-bottom:15px; font-size:14px; border:1px solid #bbf7d0;">
                    ${sessionScope.flashSuccess}
                </div>
                <c:remove var="flashSuccess" scope="session"/>
            </c:if>

            <c:if test="${not empty error}">
                <div class="error-message">
                    ${error}
                </div>
            </c:if>

            <form action="login" method="POST">
                <div class="form-group">
                    <input type="text" name="username" class="form-control" value="${cookie.remUser.value}" placeholder="Tên đăng nhập (Mã SV / GV)" required />
                </div>
                
                <div class="form-group">
                    <input type="password" name="password" class="form-control" placeholder="Mật khẩu" required />
                </div>
                
                <div class="form-options">
                    <label class="checkbox-label">
                        <input type="checkbox" name="remember" id="remember" value="yes" ${not empty cookie.remUser ? "checked" : ""}>
                        Ghi nhớ đăng nhập
                    </label>
                    <a href="#" class="forgot-password">Quên mật khẩu?</a>
                </div>
                
                <button type="submit" class="btn-submit">Đăng nhập</button>
            </form>

            <div class="register-link">
                Chưa có tài khoản? <a href="register">Đăng ký ngay</a>
            </div>
        </div>
    </div>

</body>
</html>
