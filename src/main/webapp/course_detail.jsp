<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${course.code} - Chi Tiết Môn Học | EduNext</title>
    <link href="https://fonts.googleapis.com/css2?family=Segoe+UI:wght@400;600;700&display=swap" rel="stylesheet">
    <style>
        body { font-family: 'Segoe UI', Arial, sans-serif; background: #0f172a; color: #fff; margin: 0; padding: 24px; }
        .container { max-width: 1000px; margin: 0 auto; }
        .course-header { background: #1e293b; padding: 24px; border-radius: 12px; margin-bottom: 24px; border: 1px solid #334155; }
        .badge { background: #0284c7; padding: 4px 10px; border-radius: 4px; font-size: 13px; font-weight: bold; }
        .card { background: #1e293b; padding: 20px; border-radius: 12px; margin-bottom: 16px; border: 1px solid #334155; }
        .btn { padding: 8px 16px; border-radius: 6px; border: none; font-weight: bold; cursor: pointer; text-decoration: none; display: inline-block; }
        .btn-primary { background: #0284c7; color: #fff; }
        .btn-danger { background: #ef4444; color: #fff; }
        .flash-success { background: rgba(16, 185, 129, 0.2); border: 1px solid #10b981; color: #34d399; padding: 12px; border-radius: 8px; margin-bottom: 20px; }
        .flash-error { background: rgba(239, 68, 68, 0.2); border: 1px solid #ef4444; color: #f87171; padding: 12px; border-radius: 8px; margin-bottom: 20px; }
        table { width: 100%; border-collapse: collapse; margin-top: 14px; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #334155; }
        th { color: #94a3b8; font-size: 13px; }
        input, textarea { width: 100%; padding: 8px; border-radius: 6px; border: 1px solid #334155; background: #0f172a; color: #fff; box-sizing: border-box; margin-bottom: 10px; }
    </style>
</head>
<body>
    <div class="container">
        <!-- FLASH NOTIFICATION -->
        <c:if test="${not empty sessionScope.flashSuccess}">
            <div class="flash-success">${sessionScope.flashSuccess}</div>
            <% session.removeAttribute("flashSuccess"); %>
        </c:if>
        <c:if test="${not empty sessionScope.flashError}">
            <div class="flash-error">${sessionScope.flashError}</div>
            <% session.removeAttribute("flashError"); %>
        </c:if>

        <!-- COURSE INFO HEADER -->
        <div class="course-header">
            <span class="badge">${course.code}</span>
            <h1 style="color: #38bdf8; margin: 10px 0 6px 0;">${course.name}</h1>
            <p style="color: #94a3b8; margin: 0;">${course.description} &bull; Tổng số buổi: <b>${course.totalSlots} Slots</b></p>
            <div style="margin-top: 14px;">
                <a href="home" style="color: #38bdf8; text-decoration: none; font-size: 14px;">&larr; Quay lại Trang chủ</a>
            </div>
        </div>

        <!-- ============================================================= -->
        <!-- PHÂN NHÁNH 1: GIẢNG VIÊN (LECTURER)                            -->
        <!-- ============================================================= -->
        <c:if test="${sessionScope.currentUser.role == 'Lecturer'}">
            <div class="card">
                <h3 style="color: #38bdf8; margin-top: 0;">➕ Giao Bài Tập Mới Cho Môn Học</h3>
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
            <h3 style="color: #38bdf8; margin-top: 0;">📚 Danh Sách Bài Tập Môn ${course.code} (${assignmentList.size()})</h3>
            <c:choose>
                <c:when test="${not empty assignmentList}">
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Tiêu Đề</th>
                                <th>Giảng Viên</th>
                                <th>Hạn Nộp (Deadline)</th>
                                <th>Thao Tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach items="${assignmentList}" var="a">
                                <tr>
                                    <td>#${a.assignmentId}</td>
                                    <td><b>${a.title}</b><br/><span style="font-size:12px; color:#94a3b8;">${a.description}</span></td>
                                    <td>${a.lecturerName}</td>
                                    <td style="color: #f59e0b;">
                                        <c:choose>
                                            <c:when test="${not empty a.deadline}">${a.deadline}</c:when>
                                            <c:otherwise>23:59 Chủ Nhật</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <!-- PHÂN QUYỀN GIẢNG VIÊN -->
                                        <c:if test="${sessionScope.currentUser.role == 'Lecturer'}">
                                            <form action="course" method="POST" style="display:inline;" onsubmit="return confirm('Xóa bài tập này?')">
                                                <input type="hidden" name="action" value="delete_assignment" />
                                                <input type="hidden" name="assignmentId" value="${a.assignmentId}" />
                                                <input type="hidden" name="courseId" value="${courseId}" />
                                                <button type="submit" class="btn btn-danger" style="padding: 4px 8px; font-size: 12px;">Xóa</button>
                                            </form>
                                        </c:if>

                                        <!-- PHÂN QUYỀN SINH VIÊN -->
                                        <c:if test="${sessionScope.currentUser.role == 'Student'}">
                                            <form action="course" method="POST" style="display:inline;">
                                                <input type="hidden" name="action" value="submit_assignment" />
                                                <input type="hidden" name="assignmentId" value="${a.assignmentId}" />
                                                <input type="hidden" name="courseId" value="${courseId}" />
                                                <button type="submit" class="btn btn-primary" style="padding: 4px 10px; font-size: 12px;">Nộp Bài</button>
                                            </form>
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </c:when>
                <c:otherwise>
                    <p style="color: #94a3b8; text-align: center; padding: 20px;">Chưa có bài tập nào cho môn học này.</p>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</body>
</html>
