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

        /* Status Badges */
        .badge-submitted {
            background: #dcfce7;
            color: #166534;
            font-weight: 700;
            font-size: 12px;
            padding: 4px 10px;
            border-radius: 20px;
            display: inline-block;
        }

        .badge-pending {
            background: #fef3c7;
            color: #92400e;
            font-weight: 700;
            font-size: 12px;
            padding: 4px 10px;
            border-radius: 20px;
            display: inline-block;
        }

        .btn-secondary {
            background: #f1f5f9;
            color: #334155;
            border: 1px solid #cbd5e1;
        }
        .btn-secondary:hover {
            background: #e2e8f0;
            color: #1e293b;
        }

        /* Modal Backdrop & Box */
        .modal-backdrop {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(15, 23, 42, 0.6);
            backdrop-filter: blur(4px);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 9999;
        }

        .modal-box {
            background: #ffffff;
            width: 92%;
            max-width: 520px;
            border-radius: 16px;
            padding: 28px;
            box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
            animation: modalIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
        }

        @keyframes modalIn {
            from { opacity: 0; transform: scale(0.95) translateY(-10px); }
            to { opacity: 1; transform: scale(1) translateY(0); }
        }

        .modal-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 20px;
        }

        .btn-close {
            background: none;
            border: none;
            font-size: 26px;
            color: #94a3b8;
            cursor: pointer;
            line-height: 1;
            padding: 0 4px;
        }
        .btn-close:hover { color: #1e293b; }

        /* Drag & Drop Zone */
        .drop-zone {
            border: 2px dashed #cbd5e1;
            border-radius: 12px;
            background: #f8fafc;
            padding: 34px 20px;
            text-align: center;
            cursor: pointer;
            transition: all 0.2s ease;
            margin-bottom: 16px;
        }

        .drop-zone:hover, .drop-zone.drag-over {
            border-color: #0076c8;
            background: #eff6ff;
            box-shadow: 0 0 0 4px rgba(0, 118, 200, 0.1);
        }

        .drop-icon {
            color: #0076c8;
            margin-bottom: 12px;
            display: flex;
            justify-content: center;
        }

        .drop-title {
            font-size: 15px;
            font-weight: 700;
            color: #1e293b;
            margin-bottom: 6px;
        }

        .drop-sub {
            font-size: 13px;
            color: #64748b;
            margin-bottom: 8px;
        }

        .drop-browse {
            color: #0076c8;
            font-weight: 600;
            text-decoration: underline;
            cursor: pointer;
        }

        .drop-types {
            font-size: 12px;
            color: #94a3b8;
        }

        /* File Preview Card */
        .file-preview-card {
            background: #f8fafc;
            border: 1px solid #e2e8f0;
            border-radius: 12px;
            padding: 14px 18px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 16px;
        }

        .btn-remove-file {
            background: none;
            border: none;
            font-size: 20px;
            color: #94a3b8;
            cursor: pointer;
            padding: 4px 8px;
            border-radius: 6px;
        }
        .btn-remove-file:hover {
            color: #ef4444;
            background: #fee2e2;
        }

    </style>
</head>
<body>

    <!-- Sidebar -->
    <div class="sidebar">
        <a href="home" class="sidebar-logo" style="text-decoration: none; display: block;" title="MIXI ASSIGN Dashboard">
            <span style="color: #f26522; font-weight: 800; font-size: 13px;">MIXI</span><br>
            <span style="color: #0076c8; font-size: 10px; font-weight: 700;">ASSIGN</span>
        </a>
        <a href="home" class="nav-icon" title="Hồ sơ: ${sessionScope.currentUser.fullName} (${sessionScope.currentUser.role})">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
        </a>
        <a href="home" class="nav-icon" title="Trang chủ (My Courses)">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path></svg>
        </a>
        <div class="nav-icon active" title="Chi tiết Môn học & Bài tập (${course.code})">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
        </div>
        <a href="javascript:void(0)" onclick="alert('Lịch học kỳ FALL 2026 đang diễn ra theo kế hoạch đào tạo!')" class="nav-icon" title="Thời khóa biểu / Lịch học">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg>
        </a>
        
        <a href="logout" class="nav-icon nav-bottom" title="Đăng xuất (${sessionScope.currentUser.username})">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path></svg>
        </a>
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
                                <c:if test="${sessionScope.currentUser.role == 'Student'}">
                                    <th>Trạng Thái</th>
                                </c:if>
                                <th>Thao Tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach items="${assignmentList}" var="a">
                                <c:set var="sub" value="${submissionMap[a.assignmentId]}" />
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
                                    <c:if test="${sessionScope.currentUser.role == 'Student'}">
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty sub}">
                                                    <span class="badge-submitted">✅ Đã nộp bài</span>
                                                    <div style="font-size:12px; color:#334155; margin-top:5px; display:flex; align-items:center; gap:4px;">
                                                        <span>📁</span> <b>${sub.fileNameOnly}</b> <span style="color:#64748b;">(${sub.formattedFileSize})</span>
                                                    </div>
                                                    <div style="font-size:11px; color:#0284c7; margin-top:3px;" title="Mã băm SHA-256 toàn vẹn: ${sub.sha256Hash}">
                                                        SHA: <code>${sub.sha256Hash.substring(0, 14)}...</code>
                                                    </div>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge-pending">⏳ Chưa nộp</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                    </c:if>
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
                                            <c:choose>
                                                <c:when test="${not empty sub}">
                                                    <button type="button" class="btn btn-secondary" onclick="openSubmitModal(${a.assignmentId}, '${a.title}')">
                                                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="15" height="15"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
                                                        Nộp lại
                                                    </button>
                                                </c:when>
                                                <c:otherwise>
                                                    <button type="button" class="btn btn-primary" onclick="openSubmitModal(${a.assignmentId}, '${a.title}')">
                                                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="15" height="15"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"></path></svg>
                                                        Nộp Bài
                                                    </button>
                                                </c:otherwise>
                                            </c:choose>
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

    <!-- ============================================================= -->
    <!-- MODAL NỘP BÀI TẬP KÉO THẢ TỆP (DRAG & DROP SUBMISSION MODAL)   -->
    <!-- ============================================================= -->
    <div id="submitModal" class="modal-backdrop" style="display: none;" onclick="closeModalOnBackdrop(event)">
        <div class="modal-box">
            <div class="modal-header">
                <div>
                    <h3 id="modalTitle" style="margin:0; font-size:18px; color:var(--text-main); display:flex; align-items:center; gap:8px;">
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="22" height="22" style="color: var(--primary-color);"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"></path></svg>
                        Nộp Bài Tập
                    </h3>
                    <p id="modalSubTitle" style="margin:4px 0 0 0; font-size:13px; color:var(--text-muted);"></p>
                </div>
                <button type="button" class="btn-close" onclick="closeSubmitModal()">&times;</button>
            </div>
            
            <form id="submissionForm" action="course" method="POST" enctype="multipart/form-data" onsubmit="return validateBeforeSubmit();">
                <input type="hidden" name="action" value="submit_assignment" />
                <input type="hidden" name="courseId" value="${courseId}" />
                <input type="hidden" name="assignmentId" id="modalAssignmentId" value="" />
                
                <!-- VÙNG KÉO & THẢ FILE (DRAG & DROP ZONE) -->
                <div class="drop-zone" id="dropZone">
                    <input type="file" name="file" id="modalFileInput" style="display: none;" onchange="handleFileSelected(this.files)" required />
                    
                    <div class="drop-icon">
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="46" height="46"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path></svg>
                    </div>
                    <div class="drop-title">Kéo & thả file bài làm vào đây</div>
                    <div class="drop-sub">hoặc <span class="drop-browse" onclick="triggerFileInput(event)">chọn tệp từ máy tính</span></div>
                    <div class="drop-types">Hỗ trợ: .zip, .rar, .tar.gz, .java, .pdf, .docx, .sql (Tối đa 50MB)</div>
                </div>

                <!-- KHUNG XEM TRƯỚC FILE ĐÃ CHỌN -->
                <div class="file-preview-card" id="filePreviewCard" style="display: none;">
                    <div style="display: flex; align-items: center; gap: 12px; overflow: hidden;">
                        <div style="width: 42px; height: 42px; border-radius: 8px; background: #e0e7ff; color: #4338ca; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0;">
                            📄
                        </div>
                        <div style="overflow: hidden;">
                            <div id="previewFileName" style="font-weight: 700; font-size: 14px; color: #1e293b; white-space: nowrap; text-overflow: ellipsis; overflow: hidden;">filename.zip</div>
                            <div id="previewFileSize" style="font-size: 12px; color: #64748b; margin-top: 2px;">1.2 MB</div>
                        </div>
                    </div>
                    <button type="button" class="btn-remove-file" onclick="removeSelectedFile(event)" title="Xóa file & chọn lại">&times;</button>
                </div>

                <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px;">
                    <button type="button" class="btn btn-secondary" onclick="closeSubmitModal()">Hủy</button>
                    <button type="submit" class="btn btn-primary" id="btnSubmitFile" disabled>
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                        Xác Nhận Nộp Bài
                    </button>
                </div>
            </form>
        </div>
    </div>

    <!-- SCRIPT XỬ LÝ SỰ KIỆN KÉO THẢ VÀ MODAL -->
    <script>
        function openSubmitModal(assignmentId, title) {
            document.getElementById('modalAssignmentId').value = assignmentId;
            document.getElementById('modalTitle').innerHTML = '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="22" height="22" style="color: var(--primary-color);"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"></path></svg> Nộp Bài: #' + assignmentId;
            document.getElementById('modalSubTitle').innerText = title || '';
            
            // Reset dữ liệu tệp chọn trước đó
            removeSelectedFile();
            
            document.getElementById('submitModal').style.display = 'flex';
        }

        function closeSubmitModal() {
            document.getElementById('submitModal').style.display = 'none';
        }

        function closeModalOnBackdrop(e) {
            if (e.target.id === 'submitModal') {
                closeSubmitModal();
            }
        }

        function triggerFileInput(e) {
            if (e) {
                e.preventDefault();
                e.stopPropagation();
            }
            document.getElementById('modalFileInput').click();
        }

        function handleFileSelected(files) {
            if (files && files.length > 0) {
                const file = files[0];
                document.getElementById('previewFileName').innerText = file.name;
                
                let sizeStr = '';
                if (file.size < 1024) sizeStr = file.size + ' B';
                else if (file.size < 1024 * 1024) sizeStr = (file.size / 1024).toFixed(1) + ' KB';
                else sizeStr = (file.size / (1024 * 1024)).toFixed(2) + ' MB';
                document.getElementById('previewFileSize').innerText = sizeStr;
                
                document.getElementById('dropZone').style.display = 'none';
                document.getElementById('filePreviewCard').style.display = 'flex';
                document.getElementById('btnSubmitFile').disabled = false;
            }
        }

        function removeSelectedFile(e) {
            if (e) {
                e.preventDefault();
                e.stopPropagation();
            }
            const fileInput = document.getElementById('modalFileInput');
            if (fileInput) fileInput.value = '';
            
            const dropZone = document.getElementById('dropZone');
            const preview = document.getElementById('filePreviewCard');
            const submitBtn = document.getElementById('btnSubmitFile');
            
            if (dropZone) dropZone.style.display = 'block';
            if (preview) preview.style.display = 'none';
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.innerHTML = '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg> Xác Nhận Nộp Bài';
            }
        }

        function validateBeforeSubmit() {
            const fileInput = document.getElementById('modalFileInput');
            if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
                alert('Vui lòng kéo thả hoặc chọn tệp bài làm trước khi nộp!');
                return false;
            }
            const btn = document.getElementById('btnSubmitFile');
            btn.disabled = true;
            btn.innerHTML = 'Đang tải lên & tính SHA-256...';
            return true;
        }

        // Thiết lập sự kiện Drag & Drop native trên Drop-Zone
        document.addEventListener('DOMContentLoaded', function() {
            const dropZone = document.getElementById('dropZone');
            const fileInput = document.getElementById('modalFileInput');
            if (!dropZone || !fileInput) return;

            ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(function(eventName) {
                dropZone.addEventListener(eventName, function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                }, false);
            });

            ['dragenter', 'dragover'].forEach(function(eventName) {
                dropZone.addEventListener(eventName, function() {
                    dropZone.classList.add('drag-over');
                }, false);
            });

            ['dragleave', 'drop'].forEach(function(eventName) {
                dropZone.addEventListener(eventName, function() {
                    dropZone.classList.remove('drag-over');
                }, false);
            });

            dropZone.addEventListener('drop', function(e) {
                const dt = e.dataTransfer;
                if (dt && dt.files && dt.files.length > 0) {
                    fileInput.files = dt.files;
                    handleFileSelected(dt.files);
                }
            }, false);

            dropZone.addEventListener('click', function(e) {
                if (!e.target.classList.contains('drop-browse')) {
                    fileInput.click();
                }
            });
        });
    </script>
</body>
</html>
