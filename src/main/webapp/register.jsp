<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng ký tài khoản - MIXI ASSIGN</title>
    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }

        body {
            display: flex;
            min-height: 100vh;
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

        .register-container {
            width: 100%;
            max-width: 440px;
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
        .brand-logo .assign { color: #0076c8; margin-left: 5px; }

        h2 {
            font-size: 24px;
            color: #111;
            margin-bottom: 8px;
            font-weight: 700;
        }

        .subtitle {
            color: #666;
            font-size: 14px;
            margin-bottom: 24px;
        }

        .error-message {
            background-color: #ffeef0;
            color: #dc3545;
            padding: 12px;
            border-radius: 6px;
            text-align: center;
            font-size: 14px;
            margin-bottom: 20px;
            border: 1px solid #f5c6cb;
        }

        .form-group {
            margin-bottom: 16px;
        }

        .form-group label {
            display: block;
            margin-bottom: 6px;
            font-size: 13px;
            font-weight: 600;
            color: #374151;
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

        .btn-submit {
            width: 100%;
            padding: 13px;
            background-color: #1a73e8;
            color: white;
            border: none;
            border-radius: 6px;
            font-size: 15px;
            font-weight: 600;
            cursor: pointer;
            transition: background-color 0.2s;
            margin-top: 8px;
            margin-bottom: 24px;
        }

        .btn-submit:hover {
            background-color: #155db2;
        }

        .login-link {
            text-align: center;
            font-size: 14px;
            color: #666;
        }

        .login-link a {
            color: #1a73e8;
            text-decoration: none;
            font-weight: 600;
        }

        .login-link a:hover {
            text-decoration: underline;
        }

        /* Responsive */
        @media (max-width: 768px) {
            body {
                flex-direction: column;
            }
            .left-panel, .right-panel {
                flex: none;
                width: 100%;
                min-height: auto;
                padding: 30px 20px;
            }
        }
    </style>
</head>
<body>

    <!-- Left Panel -->
    <div class="left-panel">
        <div class="logo-img">
            <span>EduNext Learning</span>
        </div>
        <h1>MIXI ASSIGN</h1>
        <p>Nền tảng học tập kiến tạo xã hội. Kết nối Giảng viên và<br>Sinh viên thông qua phương pháp học tập chủ động.</p>
    </div>

    <!-- Right Panel -->
    <div class="right-panel">
        <div class="register-container">
            <div class="brand-logo">
                <span class="m">M</span><span class="i1">I</span><span class="x">X</span><span class="i2">I</span> <span class="assign">ASSIGN</span>
            </div>
            
            <h2>Đăng ký tài khoản 🚀</h2>
            <p class="subtitle">Tạo tài khoản học viên để truy cập cổng học tập EduNext</p>

            <c:if test="${not empty error}">
                <div class="error-message">
                    ${error}
                </div>
            </c:if>

            <form action="register" method="POST">
                <div class="form-group">
                    <label>Tên đăng nhập (Mã sinh viên):</label>
                    <input type="text" name="username" class="form-control" value="${draftUsername}" placeholder="vd: he180005" required autocomplete="username" />
                </div>

                <div class="form-group">
                    <label>Họ và tên đầy đủ:</label>
                    <input type="text" name="fullName" class="form-control" value="${draftFullName}" placeholder="vd: Nguyễn Văn An" required />
                </div>
                
                <div class="form-group">
                    <label>Mật khẩu:</label>
                    <input type="password" name="password" class="form-control" placeholder="Mật khẩu (tối thiểu 6 ký tự)" required autocomplete="new-password" />
                </div>

                <div class="form-group">
                    <label>Xác nhận mật khẩu:</label>
                    <input type="password" name="repassword" class="form-control" placeholder="Nhập lại mật khẩu" required autocomplete="new-password" />
                </div>
                
                <button type="submit" class="btn-submit">Đăng ký ngay</button>
            </form>

            <div class="login-link">
                Đã có tài khoản? <a href="login">Đăng nhập ngay</a>
            </div>
        </div>
    </div>

</body>
</html>
