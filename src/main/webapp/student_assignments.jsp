<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cổng Sinh Viên - AITA Code Analysis</title>
    <style>
        * { box-sizing: border-box; }
        body { 
            font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif; 
            background: #0f172a; 
            color: #f8fafc; 
            margin: 0; 
            padding: 30px 20px; 
        }
        .container { max-width: 1100px; margin: 0 auto; }
        
        /* Top Navigation Header */
        .header { 
            display: flex; 
            justify-content: space-between; 
            align-items: center; 
            background: #1e293b; 
            padding: 18px 24px; 
            border-radius: 12px; 
            border: 1px solid #334155; 
            margin-bottom: 25px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.25);
        }
        .brand { display: flex; align-items: center; gap: 12px; }
        .brand h2 { margin: 0; color: #38bdf8; font-size: 20px; font-weight: 700; letter-spacing: -0.5px; }
        .user-meta { display: flex; align-items: center; gap: 14px; font-size: 14px; }
        .badge { 
            background: #0369a1; 
            color: #e0f2fe; 
            padding: 4px 10px; 
            border-radius: 20px; 
            font-size: 12px; 
            font-weight: 600; 
        }
        .badge-class {
            background: #334155;
            color: #cbd5e1;
            padding: 4px 10px;
            border-radius: 20px;
            font-size: 12px;
        }
        .nav-links { display: flex; gap: 10px; }
        .btn-nav { 
            color: #94a3b8; 
            text-decoration: none; 
            font-size: 13px; 
            padding: 6px 12px; 
            border-radius: 6px; 
            border: 1px solid #334155; 
            transition: all 0.2s;
        }
        .btn-nav:hover { color: #fff; background: #334155; }
        .btn-logout { background: rgba(239, 68, 68, 0.15); color: #f87171; border-color: rgba(239, 68, 68, 0.3); }
        .btn-logout:hover { background: #ef4444; color: #fff; }



        /* Success Alert */
        .alert-success {
            background: rgba(16, 185, 129, 0.15);
            border: 1px solid rgba(16, 185, 129, 0.4);
            color: #a7f3d0;
            padding: 18px 22px;
            border-radius: 10px;
            margin-bottom: 25px;
            line-height: 1.7;
            font-size: 14px;
        }
        .alert-success code {
            background: rgba(0, 0, 0, 0.3);
            padding: 2px 6px;
            border-radius: 4px;
            color: #6ee7b7;
            font-family: monospace;
        }

        /* Card Section */
        .card { 
            background: #1e293b; 
            padding: 24px; 
            border-radius: 12px; 
            border: 1px solid #334155; 
            box-shadow: 0 4px 16px rgba(0,0,0,0.2);
        }
        .card h3 { 
            margin: 0 0 18px 0; 
            color: #e2e8f0; 
            font-size: 18px; 
            display: flex; 
            align-items: center; 
            gap: 8px; 
        }

        /* Table */
        table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 14px; }
        th, td { padding: 14px 16px; text-align: left; border-bottom: 1px solid #334155; }
        th { 
            background: #0f172a; 
            color: #94a3b8; 
            font-weight: 600; 
            text-transform: uppercase; 
            font-size: 12px; 
            letter-spacing: 0.5px;
        }
        tr:hover td { background: rgba(255,255,255,0.02); }
        .assignment-title { color: #38bdf8; font-weight: 600; font-size: 15px; }
        .lecturer-tag { color: #cbd5e1; font-weight: 500; }
        .desc-text { color: #94a3b8; font-size: 13px; max-width: 320px; line-height: 1.5; }
        
        /* Submit Button */
        .btn-submit-work {
            background: #0284c7;
            color: #fff;
            padding: 8px 16px;
            border-radius: 6px;
            border: none;
            cursor: pointer;
            font-size: 13px;
            font-weight: 600;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            transition: all 0.2s;
        }
        .btn-submit-work:hover { background: #0369a1; transform: translateY(-1px); }

        /* Modal Popup */
        .modal-overlay {
            position: fixed;
            top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(0, 0, 0, 0.75);
            display: none;
            justify-content: center;
            align-items: center;
            z-index: 1000;
            padding: 20px;
        }
        .modal {
            background: #1e293b;
            border: 1px solid #475569;
            border-radius: 12px;
            width: 100%;
            max-width: 520px;
            padding: 24px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.5);
            animation: fadeIn 0.2s ease-out;
        }
        @keyframes fadeIn {
            from { opacity: 0; transform: scale(0.95); }
            to { opacity: 1; transform: scale(1); }
        }
        .modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
            border-bottom: 1px solid #334155;
            padding-bottom: 12px;
        }
        .modal-header h3 { margin: 0; color: #38bdf8; font-size: 18px; }
        .btn-close {
            background: none;
            border: none;
            color: #94a3b8;
            font-size: 20px;
            cursor: pointer;
        }
        .btn-close:hover { color: #fff; }
        .form-group { margin-bottom: 16px; }
        .form-group label { display: block; font-size: 13px; color: #cbd5e1; margin-bottom: 6px; font-weight: 500; }
        .form-group input[type="text"], .form-group textarea {
            width: 100%;
            padding: 10px;
            background: #0f172a;
            border: 1px solid #334155;
            border-radius: 6px;
            color: #fff;
            font-size: 13px;
        }
        .file-dropzone {
            border: 2px dashed #475569;
            padding: 24px;
            border-radius: 8px;
            text-align: center;
            background: #0f172a;
            cursor: pointer;
            transition: all 0.2s;
        }
        .file-dropzone:hover { border-color: #38bdf8; }
        .file-dropzone p { margin: 6px 0 0 0; color: #94a3b8; font-size: 13px; }
        .checkbox-label {
            display: flex;
            align-items: flex-start;
            gap: 8px;
            font-size: 12px;
            color: #94a3b8;
            line-height: 1.4;
            cursor: pointer;
        }
        .modal-actions {
            display: flex;
            justify-content: flex-end;
            gap: 10px;
            margin-top: 20px;
            border-top: 1px solid #334155;
            padding-top: 16px;
        }
        .btn-confirm {
            background: #0284c7;
            color: #fff;
            padding: 10px 18px;
            border: none;
            border-radius: 6px;
            font-weight: 600;
            cursor: pointer;
            font-size: 14px;
        }
        .btn-confirm:hover { background: #0369a1; }
        .btn-cancel-modal {
            background: #475569;
            color: #fff;
            padding: 10px 16px;
            border: none;
            border-radius: 6px;
            font-size: 14px;
            cursor: pointer;
        }
        .btn-cancel-modal:hover { background: #64748b; }
    </style>
</head>
<body>

<div class="container">

    <!-- TOP HEADER -->
    <div class="header">
        <div class="brand">
            <h2>🤖 AITA Portal</h2>
            <span class="badge">Khu vực Sinh viên</span>
            <c:if test="${sessionScope.currentUser.classId != null}">
                <span class="badge-class">Mã lớp #${sessionScope.currentUser.classId}</span>
            </c:if>
        </div>
        <div class="user-meta">
            <span>Chào bạn, <b>${sessionScope.currentUser.fullName}</b> (<code>${sessionScope.currentUser.username}</code>)</span>
            <div class="nav-links">
                <a href="home.jsp" class="btn-nav">🏠 Trang chủ</a>
                <a href="logout" class="btn-nav btn-logout">🚪 Đăng xuất</a>
            </div>
        </div>
    </div>

    <!-- SUCCESS ALERT IF SUBMITTED -->
    <c:if test="${not empty sessionScope.flashSuccess}">
        <div class="alert-success">
            ${sessionScope.flashSuccess}
        </div>
        <c:remove var="flashSuccess" scope="session"/>
    </c:if>



    <!-- ASSIGNMENTS CARD -->
    <div class="card">
        <h3>📋 Danh Sách Đề Bài Tập Đang Mở</h3>
        
        <c:choose>
            <c:when test="${empty assignmentList}">
                <p style="color: #94a3b8; text-align: center; padding: 30px;">Hiện tại chưa có bài tập nào được giao từ Giảng viên.</p>
            </c:when>
            <c:otherwise>
                <table>
                    <thead>
                        <tr>
                            <th style="width: 70px;">Mã Đề</th>
                            <th style="width: 220px;">Tên Bài Tập</th>
                            <th style="width: 170px;">Giảng Viên Giao</th>
                            <th>Yêu Cầu / Mô Tả</th>
                            <th style="width: 140px;">Thao Tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach items="${assignmentList}" var="item">
                            <tr>
                                <td><b>#${item.assignmentId}</b></td>
                                <td>
                                    <div class="assignment-title">${item.title}</div>
                                </td>
                                <td>
                                    <span class="lecturer-tag">👨‍🏫 ${item.lecturerName}</span>
                                </td>
                                <td>
                                    <div class="desc-text">${item.description}</div>
                                </td>
                                <td>
                                    <button type="button" class="btn-submit-work" onclick="openSubmitModal(${item.assignmentId}, '${item.title}')">
                                        📤 Nộp bài
                                    </button>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>

</div>

<!-- SUBMISSION MODAL (PROTOTYPE DEMO) -->
<div class="modal-overlay" id="submitModal">
    <div class="modal">
        <div class="modal-header">
            <h3>📤 Nộp Bài Tập (AITA Submission Portal)</h3>
            <button class="btn-close" onclick="closeSubmitModal()">&times;</button>
        </div>
        <form action="student-assignments" method="POST">
            <input type="hidden" name="assignmentId" id="modalAssignmentId" value="" />
            <input type="hidden" name="assignmentTitle" id="modalAssignmentTitle" value="" />

            <div class="form-group">
                <label>Đề bài đang nộp:</label>
                <input type="text" id="displayAssignmentTitle" readonly style="background: #1e293b; color: #38bdf8; font-weight: bold; border-color: #334155;" />
            </div>

            <div class="form-group">
                <label>Chọn tệp mã nguồn Java (.zip hoặc .java):</label>
                <div class="file-dropzone" onclick="document.getElementById('fileInput').click()">
                    <span style="font-size: 28px;">📦</span>
                    <p id="fileNameDisplay">Nhấp để chọn tệp tải lên hoặc kéo thả vào đây</p>
                    <small style="color: #64748b;">Hỗ trợ: .zip, .java (Dung lượng tối đa: 15MB)</small>
                </div>
                <input type="file" id="fileInput" name="submissionFile" accept=".zip,.java" style="display: none;" onchange="handleFileSelected(this)" />
                <input type="hidden" name="fileName" id="hiddenFileName" value="" />
            </div>

            <div class="form-group">
                <label class="checkbox-label">
                    <input type="checkbox" required checked />
                    <span>Tôi cam kết mã nguồn nộp là bài làm cá nhân, tuân thủ nguyên tắc liêm chính học thuật và cho phép hệ thống AITA băm mã SHA-256 đối chiếu đạo văn.</span>
                </label>
            </div>

            <div class="modal-actions">
                <button type="button" class="btn-cancel-modal" onclick="closeSubmitModal()">Hủy</button>
                <button type="submit" class="btn-confirm">Xác Nhận Nộp Bài</button>
            </div>
        </form>
    </div>
</div>

<script>
    function openSubmitModal(id, title) {
        document.getElementById('modalAssignmentId').value = id;
        document.getElementById('modalAssignmentTitle').value = title;
        document.getElementById('displayAssignmentTitle').value = '#' + id + ' - ' + title;
        document.getElementById('fileNameDisplay').innerText = 'Nhấp để chọn tệp tải lên hoặc kéo thả vào đây';
        document.getElementById('hiddenFileName').value = '';
        document.getElementById('fileInput').value = '';
        document.getElementById('submitModal').style.display = 'flex';
    }

    function closeSubmitModal() {
        document.getElementById('submitModal').style.display = 'none';
    }

    function handleFileSelected(input) {
        if (input.files && input.files[0]) {
            const fName = input.files[0].name;
            document.getElementById('fileNameDisplay').innerHTML = '✅ Đã chọn tệp: <b>' + fName + '</b>';
            document.getElementById('hiddenFileName').value = fName;
        }
    }

    // Đóng modal khi bấm ra ngoài vùng popup
    window.onclick = function(event) {
        const modal = document.getElementById('submitModal');
        if (event.target === modal) {
            closeSubmitModal();
        }
    };
</script>

</body>
</html>
