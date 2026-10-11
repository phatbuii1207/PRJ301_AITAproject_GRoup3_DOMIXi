<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:if test="${empty sessionScope.currentUser}">
    <c:redirect url="login"/>
</c:if>
<c:if test="${courseList == null}">
    <c:redirect url="home"/>
</c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard - EduNext Style</title>
    <style>
        :root {
            --bg-color: #f5f7f9;
            --sidebar-bg: #ffffff;
            --text-main: #1f2937;
            --text-muted: #6b7280;
            --primary-color: #5a67d8; /* Indigo active color */
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

        /* Sidebar */
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

        .nav-bottom {
            margin-top: auto;
        }

        /* Main Content */
        .main-content {
            flex: 1;
            margin-left: 72px;
            padding: 20px 40px;
        }

        /* Top Tabs */
        .top-nav {
            display: flex;
            align-items: center;
            gap: 20px;
            margin-bottom: 30px;
            position: relative;
        }

        .tab-btn {
            padding: 10px 16px;
            border-radius: 20px;
            font-size: 14px;
            font-weight: 600;
            color: var(--text-muted);
            cursor: pointer;
            text-decoration: none;
            transition: all 0.2s;
        }

        .tab-btn.active {
            background-color: #ffffff;
            color: var(--text-main);
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }

        .logout-btn {
            position: absolute;
            right: 0;
            font-size: 14px;
            color: #ef4444;
            text-decoration: none;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 6px;
        }

        /* Header Area */
        .header-area {
            display: flex;
            justify-content: space-between;
            align-items: flex-end;
            margin-bottom: 20px;
        }

        .welcome-text {
            font-size: 14px;
            color: var(--text-muted);
            margin-bottom: 4px;
        }

        .page-title {
            font-size: 28px;
            font-weight: 700;
            color: var(--text-main);
        }

        .refresh-btn {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 8px 16px;
            background: #ffffff;
            border: 1px solid var(--border-color);
            border-radius: 20px;
            font-size: 13px;
            font-weight: 600;
            color: var(--text-main);
            cursor: pointer;
        }

        /* Alert Box */
        .info-alert {
            background-color: #eff6ff;
            color: #3b82f6;
            padding: 12px 16px;
            border-radius: 8px;
            font-size: 13px;
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 30px;
        }

        /* Content Layout */
        .content-layout {
            display: flex;
            gap: 30px;
            align-items: flex-start;
        }

        /* Left Filter Sidebar */
        .filter-sidebar {
            width: 220px;
            background: #ffffff;
            border-radius: 12px;
            border: 1px solid var(--border-color);
            padding: 16px 0;
            flex-shrink: 0;
        }

        .filter-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0 20px 12px;
            font-weight: 700;
            font-size: 14px;
            border-bottom: 1px solid var(--border-color);
        }

        .filter-header .badge {
            background: #f3f4f6;
            color: var(--text-muted);
            padding: 2px 8px;
            border-radius: 12px;
            font-size: 12px;
        }

        .filter-group {
            padding: 12px 20px 0;
        }

        .filter-title {
            font-size: 11px;
            color: var(--text-muted);
            font-weight: 700;
            text-transform: uppercase;
            margin-bottom: 8px;
            margin-top: 12px;
        }

        .filter-item {
            padding: 10px 12px;
            border-radius: 6px;
            font-size: 14px;
            color: var(--text-main);
            cursor: pointer;
            transition: all 0.2s;
            margin-bottom: 4px;
            font-weight: 500;
        }

        .filter-item:hover {
            background-color: #f3f4f6;
        }

        .filter-item.active {
            background-color: var(--primary-color);
            color: #ffffff;
        }

        /* Right Courses Area */
        .courses-area {
            flex: 1;
        }

        .search-bar {
            background: #ffffff;
            border: 1px solid var(--border-color);
            border-radius: 20px;
            padding: 10px 16px;
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 24px;
            width: 100%;
        }

        .search-bar input {
            border: none;
            outline: none;
            width: 100%;
            font-size: 14px;
        }

        .courses-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
            gap: 24px;
        }

        /* Course Card */
        .course-card {
            background: #ffffff;
            border: 1px solid var(--border-color);
            border-radius: 12px;
            padding: 20px;
            display: flex;
            flex-direction: column;
            transition: box-shadow 0.2s, transform 0.2s;
        }

        .course-card:hover {
            box-shadow: 0 4px 12px rgba(0,0,0,0.05);
            transform: translateY(-2px);
        }

        .card-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 24px;
        }

        .tag-icon {
            width: 40px;
            height: 40px;
            border-radius: 8px;
            display: flex;
            justify-content: center;
            align-items: center;
            font-weight: bold;
            font-size: 14px;
        }
        
        .tag-blue { background-color: #e0e7ff; color: #4f46e5; }
        .tag-green { background-color: #dcfce7; color: #16a34a; }
        .tag-purple { background-color: #f3e8ff; color: #9333ea; }
        .tag-orange { background-color: #ffedd5; color: #ea580c; }

        .card-badge {
            background: #f3f4f6;
            color: var(--text-muted);
            padding: 4px 10px;
            border-radius: 12px;
            font-size: 11px;
            font-weight: 700;
        }

        .course-code {
            font-size: 12px;
            color: var(--text-muted);
            font-weight: 600;
            margin-bottom: 4px;
        }

        .course-title {
            font-size: 16px;
            font-weight: 700;
            color: var(--text-main);
            margin-bottom: 6px;
            line-height: 1.4;
        }

        .course-subtitle {
            font-size: 13px;
            color: var(--text-muted);
            flex-grow: 1;
            margin-bottom: 20px;
        }

        .card-footer {
            border-top: 1px solid var(--border-color);
            padding-top: 16px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .btn-start {
            display: flex;
            align-items: center;
            gap: 8px;
            color: var(--text-muted);
            text-decoration: none;
            font-size: 13px;
            font-weight: 600;
            transition: color 0.2s;
            width: 100%;
            justify-content: space-between;
        }

        .btn-start:hover {
            color: var(--primary-color);
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
        
        <a href="home" class="nav-icon" title="Người dùng: ${sessionScope.currentUser.fullName} (${sessionScope.currentUser.role})">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
        </a>
        <a href="home" class="nav-icon active" title="Trang chủ (My Courses)">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path></svg>
        </a>
        <a href="assignments" class="nav-icon" title="Quản lý Bài tập (${sessionScope.currentUser.role})">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
        </a>
        <a href="javascript:void(0)" onclick="alert('Lịch học kỳ FALL 2026 đang diễn ra theo kế hoạch đào tạo!')" class="nav-icon" title="Thời khóa biểu / Lịch học">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg>
        </a>
        
        <a href="logout" class="nav-icon nav-bottom" title="Đăng xuất (${sessionScope.currentUser.username})">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path></svg>
        </a>
    </div>

    <!-- Main Content -->
    <div class="main-content">
        <!-- Top Tabs -->
        <div class="top-nav">
            <a href="home" class="tab-btn active">Courses</a>
            <a href="assignments" class="tab-btn">Assignments (${sessionScope.currentUser.role})</a>
            <a href="javascript:void(0)" onclick="alert('Module Projects liên kết Static Code Analysis AST đang hoàn thiện!')" class="tab-btn">Projects</a>
            <a href="javascript:void(0)" onclick="alert('Tài liệu học tập và tiêu chuẩn Rubric RBL sẵn sàng tại thư mục docs!')" class="tab-btn">Reference</a>
            
            <a href="logout" class="logout-btn">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16"><path stroke-linecap="round" stroke-linejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path></svg>
                Logout
            </a>
        </div>

        <!-- Header -->
        <div class="header-area">
            <div>
                <div class="welcome-text">Welcome back, <b>${not empty sessionScope.currentUser ? sessionScope.currentUser.fullName : 'Student'}</b> (${sessionScope.currentUser.role})</div>
                <h1 class="page-title">My Courses</h1>
            </div>
            <button class="refresh-btn" onclick="location.href='home'">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16"><path stroke-linecap="round" stroke-linejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
                Refresh
            </button>
        </div>

        <!-- Info Alert -->
        <div class="info-alert">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="18" height="18"><path stroke-linecap="round" stroke-linejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            Dữ liệu kết nối SQL Server thời gian thực &bull; Cổng học tập EduNext
        </div>

        <!-- Layout Body -->
        <div class="content-layout">
            
            <!-- Left Filter -->
            <div class="filter-sidebar">
                <div class="filter-header">
                    Semesters <span class="badge">6</span>
                </div>
                <div class="filter-group">
                    <div class="filter-item" onclick="selectSemester(this, 'ALL')">ALL SEMESTERS</div>
                    
                    <div class="filter-title">SUMMER</div>
                    <div class="filter-item" onclick="selectSemester(this, 'SU26')">2026</div>
                    <div class="filter-item" onclick="selectSemester(this, 'SU25')">2025</div>
                    
                    <div class="filter-title">SPRING</div>
                    <div class="filter-item" onclick="selectSemester(this, 'SP26')">2026</div>
                    
                    <div class="filter-title">FALL</div>
                    <div class="filter-item active" onclick="selectSemester(this, 'FA26')">2026</div>
                    <div class="filter-item" onclick="selectSemester(this, 'FA25')">2025</div>
                </div>
            </div>

            <!-- Right Courses -->
            <div class="courses-area">
                <form action="home" method="GET" class="search-bar">
                    <svg fill="none" stroke="#9ca3af" viewBox="0 0 24 24" width="20" height="20" style="flex-shrink:0;"><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
                    <input type="text" name="search" id="courseSearchInput" value="${searchKeyword}" placeholder="Search courses (nhập tìm kiếm tức thì)..." oninput="liveFilterCourses(this.value)" autocomplete="off">
                    <c:if test="${not empty searchKeyword}">
                        <a href="home" style="color:var(--text-muted); text-decoration:none; font-size:13px; font-weight:600; white-space:nowrap; padding: 2px 6px;">✕ Xóa tìm kiếm</a>
                    </c:if>
                </form>

                <div class="courses-grid" id="courseGrid">
                    <c:choose>
                        <c:when test="${not empty courseList}">
                            <c:forEach items="${courseList}" var="c" varStatus="status">
                                <c:set var="tagColor" value="tag-blue"/>
                                <c:choose>
                                    <c:when test="${status.index % 4 == 1}"><c:set var="tagColor" value="tag-green"/></c:when>
                                    <c:when test="${status.index % 4 == 2}"><c:set var="tagColor" value="tag-purple"/></c:when>
                                    <c:when test="${status.index % 4 == 3}"><c:set var="tagColor" value="tag-orange"/></c:when>
                                    <c:otherwise><c:set var="tagColor" value="tag-blue"/></c:otherwise>
                                </c:choose>
                                <div class="course-card" onclick="location.href='course?id=${c.courseId}'" style="cursor: pointer;" title="Nhấn để xem chi tiết môn ${c.courseCode}">
                                    <div class="card-header">
                                        <div class="tag-icon ${tagColor}">
                                            <c:choose>
                                                <c:when test="${not empty c.courseCode && c.courseCode.length() >= 2}">${c.courseCode.substring(0, 2)}</c:when>
                                                <c:otherwise>IT</c:otherwise>
                                            </c:choose>
                                        </div>
                                        <div class="card-badge">${c.badge}</div>
                                    </div>
                                    <div class="course-code">${c.courseCode}</div>
                                    <div class="course-title">${c.courseName}</div>
                                    <div class="course-subtitle">${c.description}</div>
                                    <div class="card-footer">
                                        <a href="course?id=${c.courseId}" class="btn-start" onclick="event.stopPropagation();">
                                            <span><svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16" style="vertical-align: text-bottom; margin-right:4px;"><path stroke-linecap="round" stroke-linejoin="round" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path></svg> Get started</span>
                                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16"><path stroke-linecap="round" stroke-linejoin="round" d="M14 5l7 7m0 0l-7 7m7-7H3"></path></svg>
                                        </a>
                                    </div>
                                </div>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <div style="grid-column: 1 / -1; padding: 40px; text-align: center; background: #fff; border-radius: 12px; border: 1px solid var(--border-color); color: var(--text-muted);">
                                <p style="font-size: 16px; margin-bottom: 12px;">Không tìm thấy môn học nào phù hợp với từ khóa "<b>${searchKeyword}</b>".</p>
                                <a href="home" style="display: inline-block; padding: 8px 16px; background: var(--primary-color); color: #fff; border-radius: 8px; text-decoration: none; font-size: 14px; font-weight: 600;">Xem tất cả môn học</a>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
            
        </div>
    </div>

    <script>
        // Xử lý chuyển học kỳ linh hoạt
        function selectSemester(el, code) {
            document.querySelectorAll('.filter-item').forEach(item => item.classList.remove('active'));
            el.classList.add('active');
        }

        // Tìm kiếm tức thời (Client-side Realtime Instant Filter)
        function liveFilterCourses(query) {
            var term = (query || '').toLowerCase().trim();
            var cards = document.querySelectorAll('.course-card');
            cards.forEach(function(card) {
                var text = card.textContent.toLowerCase();
                card.style.display = text.indexOf(term) !== -1 ? '' : 'none';
            });
        }
    </script>
</body>
</html>
