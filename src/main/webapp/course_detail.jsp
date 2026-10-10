<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${course.code} - Chi Tiết Môn Học | EduNext</title>
    <style>
        :root {
            --bg-color: #f5f7f9;
            --sidebar-bg: #ffffff;
            --text-main: #1f2937;
            --text-muted: #6b7280;
            --primary-color: #5a67d8;
            --border-color: #e5e7eb;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
        }

        body {
            background-color: var(--bg-color);
            color: var(--text-main);
            display: flex;
            min-height: 100vh;
        }

        /* Sidebar - Same as home.jsp */
        .sidebar {
            width: 72px;
            background-color: var(--sidebar-bg);
            border-right: 1px solid var(--border-color);
            display: flex;
            flex-direction: column;
            align-items: center;
            padding: 20px 0;
            position: fixed;
            height: 100vh;
            z-index: 100;
        }

        .sidebar-logo {
            font-weight: bold;
            font-size: 11px;
            color: #f26522;
            text-align: center;
            margin-bottom: 30px;
            cursor: pointer;
        }
        .sidebar-logo span { color: #0076c8; }

        .nav-icon {
            width: 40px;
            height: 40px;
            border-radius: 8px;
            display: flex;
            justify-content: center;
            align-items: center;
            color: var(--text-muted);
            margin-bottom: 15px;
            cursor: pointer;
            transition: all 0.2s;
        }

        .nav-icon:hover, .nav-icon.active {
            background-color: #f3f4f6;
            color: var(--text-main);
        }
        
        .nav-icon svg { width: 22px; height: 22px; stroke-width: 1.5; }
        .nav-bottom { margin-top: auto; }

        /* Main Content */
        .main-content {
            flex: 1;
            margin-left: 72px;
            padding: 20px 40px;
            max-width: 1200px;
        }

        /* Flash Notifications */
        .flash-success {
            background-color: #dcfce7;
            color: #166534;
            padding: 12px 16px;
            border-radius: 8px;
            border: 1px solid #bbf7d0;
            margin-bottom: 20px;
            font-size: 14px;
        }
        .flash-error {
            background-color: #fee2e2;
            color: #991b1b;
            padding: 12px 16px;
            border-radius: 8px;
            border: 1px solid #fecaca;
            margin-bottom: 20px;
            font-size: 14px;
        }

        /* Course Header */
        .course-header {
            background: #ffffff;
            padding: 24px 30px;
            border-radius: 12px;
            border: 1px solid var(--border-color);
            margin-bottom: 24px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.02);
        }

        .course-badge {
            background: #e0e7ff;
            color: #4f46e5;
            padding: 6px 12px;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 700;
            display: inline-block;
            margin-bottom: 12px;
        }

        .course-header h1 {
            color: var(--text-main);
            margin-bottom: 8px;
            font-size: 24px;
            font-weight: 700;
        }

        .course-header p {
            color: var(--text-muted);
            font-size: 14px;
            margin-bottom: 16px;
        }
        
        .back-link {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            color: var(--primary-color);
            text-decoration: none;
            font-size: 14px;
            font-weight: 600;
        }
        
        .back-link:hover { text-decoration: underline; }

        /* Card Section */
        .card {
            background: #ffffff;
            padding: 24px 30px;
            border-radius: 12px;
            border: 1px solid var(--border-color);
            margin-bottom: 24px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.02);
        }

        .card h3 {
            color: var(--text-main);
            margin-bottom: 20px;
            font-size: 18px;
            font-weight: 700;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        /* Form */
        label {
            display: block;
            margin-bottom: 8px;
            font-size: 14px;
            font-weight: 600;
            color: var(--text-main);
        }

        input[type="text"], input[type="datetime-local"], textarea {
            width: 100%;
            padding: 10px 14px;
            border-radius: 8px;
            border: 1px solid var(--border-color);
            background: #f9fafb;
            color: var(--text-main);
            font-size: 14px;
            margin-bottom: 16px;
            outline: none;
            transition: border-color 0.2s;
        }

        input:focus, textarea:focus { border-color: var(--primary-color); }

        /* Buttons */
        .btn {
            padding: 10px 16px;
            border-radius: 8px;
            border: none;
            font-weight: 600;
            font-size: 14px;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            transition: all 0.2s;
        }

        .btn-primary { background: var(--primary-color); color: #fff; }
        .btn-primary:hover { background: #4c51bf; }
        
        .btn-danger { background: #fee2e2; color: #dc2626; }
        .btn-danger:hover { background: #fecaca; }

        /* Table */
        table {
            width: 100%;
            border-collapse: collapse;
        }

        th, td {
            padding: 14px 16px;
            text-align: left;
            border-bottom: 1px solid var(--border-color);
        }

        th {
            color: var(--text-muted);
            font-size: 13px;
            font-weight: 600;
            text-transform: uppercase;
            background: #f9fafb;
        }
        
        td { font-size: 14px; }
        td b { color: var(--text-main); font-size: 15px; }

        .deadline-text {
            color: #d97706;
            background: #fef3c7;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 13px;
            font-weight: 600;
        }
        
        .empty-state {
            text-align: center;
            padding: 40px 20px;
            color: var(--text-muted);
            font-size: 15px;
        }

    </style>
</head>
<body>

    <!-- Sidebar -->
    <div class="sidebar">
        <div class="sidebar-logo">FPT<br><span>Education</span></div>
        <div class="nav-icon"><svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg></div>
        <div class="nav-icon"><a href="home" style="color:inherit; text-decoration:none; display:flex;"><svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path></svg></a></div>
        <div class="nav-icon active"><svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg></div>
        <div class="nav-icon"><svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg></div>
        
        <div class="nav-icon nav-bottom"><svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg></div>
    </div>

    <!-- Main Content -->
    <div class="main-content">

        <!-- FLASH NOTIFICATION: Thay thế Scriptlet bằng JSTL/EL -->
        <c:if test="${not empty sessionScope.flashSuccess}">
            <div class="flash-success">✅ ${sessionScope.flashSuccess}</div>
            <c:remove var="flashSuccess" scope="session" />
        </c:if>
        
        <c:if test="${not empty sessionScope.flashError}">
            <div class="flash-error">❌ ${sessionScope.flashError}</div>
            <c:remove var="flashError" scope="session" />
        </c:if>

        <!-- COURSE INFO HEADER -->
        <div class="course-header">
            <div class="course-badge">${course.code}</div>
            <h1>${course.name}</h1>
            <p>${course.description} &bull; Tổng số buổi: <b>${course.totalSlots} Slots</b></p>
            <a href="home" class="back-link">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16"><path stroke-linecap="round" stroke-linejoin="round" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
                Quay lại Trang chủ
            </a>
        </div>

        <!-- ============================================================= -->
        <!-- PHÂN NHÁNH 1: GIẢNG VIÊN (LECTURER)                            -->
        <!-- ============================================================= -->
        <c:if test="${sessionScope.currentUser.role == 'Lecturer'}">
            <div class="card">
                <h3><svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="20" height="20"><path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"></path></svg> Giao Bài Tập Mới Cho Môn Học</h3>
                <form action="course" method="POST">
                    <input type="hidden" name="action" value="create_assignment" />
                    <input type="hidden" name="courseId" value="${courseId}" />
                    
                    <label>Tiêu đề bài tập:</label>
                    <input type="text" name="title" placeholder="vd: Lab 1: AST Code Complexity" required />
                    
                    <label>Mô tả & Yêu cầu:</label>
                    <textarea name="description" rows="3" placeholder="Nhập yêu cầu bài tập..."></textarea>
                    
                    <label>Hạn nộp (Deadline):</label>
                    <input type="datetime-local" name="deadline" />
                    
                    <button type="submit" class="btn btn-primary">Giao Bài Tập</button>
                </form>
            </div>
        </c:if>

        <!-- ============================================================= -->
        <!-- DANH SÁCH BÀI TẬP THUỘC MÔN HỌC                                -->
        <!-- ============================================================= -->
        <div class="card">
            <h3><svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="20" height="20"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg> Danh Sách Bài Tập Môn ${course.code} (${assignmentList.size()})</h3>
            
            <c:choose>
                <c:when test="${not empty assignmentList}">
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Tiêu Đề & Mô Tả</th>
                                <th>Giảng Viên</th>
                                <th>Hạn Nộp</th>
                                <th>Thao Tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach items="${assignmentList}" var="a">
                                <tr>
                                    <td style="color: var(--text-muted);">#${a.assignmentId}</td>
                                    <td>
                                        <b>${a.title}</b><br/>
                                        <span style="font-size:13px; color:var(--text-muted);">${a.description}</span>
                                    </td>
                                    <td>${a.lecturerName}</td>
                                    <td>
                                        <span class="deadline-text">
                                            <c:choose>
                                                <c:when test="${not empty a.deadline}">${a.deadline}</c:when>
                                                <c:otherwise>23:59 Chủ Nhật</c:otherwise>
                                            </c:choose>
                                        </span>
                                    </td>
                                    <td>
                                        <!-- PHÂN QUYỀN GIẢNG VIÊN -->
                                        <c:if test="${sessionScope.currentUser.role == 'Lecturer'}">
                                            <form action="course" method="POST" style="display:inline;" onsubmit="return confirm('Xác nhận xóa bài tập này?')">
                                                <input type="hidden" name="action" value="delete_assignment" />
                                                <input type="hidden" name="assignmentId" value="${a.assignmentId}" />
                                                <input type="hidden" name="courseId" value="${courseId}" />
                                                <button type="submit" class="btn btn-danger">Xóa</button>
                                            </form>
                                        </c:if>

                                        <!-- PHÂN QUYỀN SINH VIÊN -->
                                        <c:if test="${sessionScope.currentUser.role == 'Student'}">
                                            <form action="course" method="POST" style="display:inline;">
                                                <input type="hidden" name="action" value="submit_assignment" />
                                                <input type="hidden" name="assignmentId" value="${a.assignmentId}" />
                                                <input type="hidden" name="courseId" value="${courseId}" />
                                                <button type="submit" class="btn btn-primary">Nộp Bài</button>
                                            </form>
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </c:when>
                <c:otherwise>
                    <div class="empty-state">
                        Chưa có bài tập nào cho môn học này.
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
        
    </div>
</body>
</html>
