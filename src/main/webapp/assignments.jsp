<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Quản lý Bài tập - AITA Code Analysis</title>
    <style>
        body { font-family: 'Segoe UI', Arial, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 30px; }
        .container { max-width: 1000px; margin: 0 auto; }
        .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; }
        .card { background: #1e293b; padding: 25px; border-radius: 10px; margin-bottom: 30px; border: 1px solid #334155; }
        h2, h3 { color: #38bdf8; margin-top: 0; }
        input[type="text"], textarea { width: 100%; padding: 10px; border-radius: 6px; border: 1px solid #334155; background: #0f172a; color: #fff; box-sizing: border-box; margin-bottom: 15px; }
        button { background: #0284c7; color: #fff; border: none; padding: 10px 20px; border-radius: 6px; font-weight: bold; cursor: pointer; }
        button:hover { background: #0369a1; }
        .btn-cancel { background: #64748b; margin-left: 10px; text-decoration: none; padding: 10px 18px; border-radius: 6px; color: #fff; display: inline-block; font-size: 14px; }
        table { width: 100%; border-collapse: collapse; margin-top: 15px; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #334155; }
        th { background: #0f172a; color: #94a3b8; }
        .btn-edit { background: #eab308; color: #000; padding: 6px 12px; border-radius: 4px; text-decoration: none; font-size: 13px; font-weight: bold; }
        .btn-delete { background: #ef4444; color: #fff; padding: 6px 12px; border-radius: 4px; text-decoration: none; font-size: 13px; font-weight: bold; margin-left: 5px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div>
                <h2>📚 Quản Lý Đề Bài / Bài Tập (CRUD Giảng Viên)</h2>
                <div style="font-size: 13px; color: #94a3b8; margin-top: 4px;">
                    Giảng viên: <b style="color: #fff;">${sessionScope.currentUser.fullName}</b> 
                    <span style="background: #0284c7; color: #fff; padding: 2px 8px; border-radius: 4px; font-size: 11px; margin-left: 6px;">Lecturer</span>
                </div>
            </div>
            <div style="display: flex; gap: 12px; align-items: center;">
                <a href="home.jsp" style="color: #38bdf8; text-decoration: none; font-size: 14px;">🏠 Về Trang Chủ</a>
                <a href="logout" style="background: rgba(239,68,68,0.2); color: #f87171; border: 1px solid rgba(239,68,68,0.4); text-decoration: none; padding: 6px 12px; border-radius: 6px; font-size: 13px;">🚪 Đăng Xuất</a>
            </div>
        </div>

        <!-- FORM THÊM MỚI HOẶC CHỈNH SỬA -->
        <div class="card">
            <h3>${not empty editAssignment ? "✏️ Chỉnh Sửa Bài Tập" : "➕ Thêm Đề Bài Mới"}</h3>
            <form action="assignments" method="POST">
                <input type="hidden" name="action" value="${not empty editAssignment ? 'update' : 'create'}" />
                <c:if test="${not empty editAssignment}">
                    <input type="hidden" name="id" value="${editAssignment.assignmentId}" />
                </c:if>

                <label>Tiêu đề bài tập:</label>
                <input type="text" name="title" value="${editAssignment.title}" placeholder="Nhập tiêu đề..." required />

                <label>Mô tả chi tiết:</label>
                <textarea name="description" rows="3" placeholder="Nhập yêu cầu đề bài...">${editAssignment.description}</textarea>

                <button type="submit">${not empty editAssignment ? "Cập Nhật" : "Lưu Bài Tập"}</button>
                <c:if test="${not empty editAssignment}">
                    <a href="assignments" class="btn-cancel">Hủy</a>
                </c:if>
            </form>
        </div>

        <!-- BẢNG DANH SÁCH BÀI TẬP TỪ SQL SERVER -->
        <div class="card">
            <h3>📋 Danh Sách Bài Tập Trong Database</h3>
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Tiêu Đề</th>
                        <th>Giảng Viên Tạo</th>
                        <th>Mô Tả</th>
                        <th>Thao Tác</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${assignmentList}" var="item">
                        <tr>
                            <td><b>#${item.assignmentId}</b></td>
                            <td style="color: #38bdf8; font-weight: bold;">${item.title}</td>
                            <td>${item.lecturerName}</td>
                            <td>${item.description}</td>
                            <td>
                                <a href="assignments?action=edit&id=${item.assignmentId}" class="btn-edit">Sửa</a>
                                <a href="assignments?action=delete&id=${item.assignmentId}" class="btn-delete" 
                                   onclick="return confirm('Bạn có chắc muốn xóa bài tập #${item.assignmentId}?')">Xóa</a>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</body>
</html>
