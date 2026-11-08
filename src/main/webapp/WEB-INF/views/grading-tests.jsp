<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Test case bài tập</title><%@ include file="grading-style.jspf" %></head><body><main>
<a href="${pageContext.request.contextPath}/course?id=${assignment.courseId}">← Quay lại môn học</a>
<h1>Test case: <c:out value="${assignment.title}" /></h1>
<p class="notice">Điểm thang 10 = 10 × tổng trọng số test đạt / tổng trọng số. Chuẩn hóa xuống dòng và bỏ khoảng trắng cuối toàn bộ output; nội dung còn lại phải khớp. Chưa áp dụng sai số số thực.</p>
<c:choose><c:when test="${canManage}">
<form method="post" action="${pageContext.request.contextPath}/grading-tests">
<input type="hidden" name="assignmentId" value="${assignment.assignmentId}">
<input type="hidden" name="version" value="${configVersion}">
<input type="hidden" name="csrf" value="${sessionScope.gradingCsrf}">
<div class="card"><label>Main class (ví dụ Main hoặc demo.Main)</label><input name="mainClass" value="<c:out value='${mainClass}' />" maxlength="200" required>
<label for="driverSource">Driver Java cho bài nộp một file .java (tùy chọn)</label>
<textarea id="driverSource" name="driverSource" rows="16" spellcheck="false"><c:out value="${driverSource}" /></textarea>
<p class="muted">Driver do giảng viên viết, chứa main class phía trên và đọc stdin/ghi stdout theo bộ test. Để trống nếu chỉ chấm ZIP. Tối đa 64 KiB. Không đặt đáp án hoặc bí mật trong driver; mã driver cần chạy cùng mã sinh viên trong sandbox. Hệ thống từ chối bài nộp trùng kiểu/tên driver. Biên dịch tối đa 15 giây; mỗi lần chạy tối đa 5 giây.</p>
<p class="muted">Mỗi bài từ 1–20 test. Trọng số dương, tối đa 1000 và 2 chữ số thập phân. Input và đáp án mỗi test tối đa 64 KiB. Đáp án/test ẩn không đưa vào container.</p></div>
<div id="testRows">
<c:forEach items="${tests}" var="t"><section class="card test-row">
<div class="case-heading"><h2>Test case</h2><button type="button" class="secondary" onclick="this.closest('.test-row').remove()">Xóa dòng</button></div>
<label>Tên test</label><input name="caseName" maxlength="80" value="<c:out value='${t.name}' />" required>
<div class="grid"><div><label>Input (stdin)</label><textarea name="stdin">
<c:out value="${t.input}" /></textarea></div>
<div><label>Đáp án chuẩn (stdout)</label><textarea name="expected">
<c:out value="${t.expectedOutput}" /></textarea></div></div>
<div class="grid"><div><label>Trọng số</label><input type="number" name="weight" min="0.01" max="1000" step="0.01" value="${t.weight}" required></div>
<div><label>Hiển thị</label><select name="hidden"><option value="false" ${not t.hidden ? 'selected' : ''}>Công khai</option><option value="true" ${t.hidden ? 'selected' : ''}>Ẩn với sinh viên</option></select></div></div>
</section></c:forEach>
</div>
<button type="button" class="secondary" onclick="addTest()">Thêm test</button><button type="submit">Lưu bộ test</button>
<p class="muted">Lưu bộ test không tự chấm lại bài cũ. Mở kết quả bài nộp và chọn “Chấm lại” để áp dụng bộ test mới.</p>
</form>
<template id="testTemplate"><section class="card test-row">
<div class="case-heading"><h2>Test case</h2><button type="button" class="secondary" onclick="this.closest('.test-row').remove()">Xóa dòng</button></div>
<label>Tên test</label><input name="caseName" maxlength="80" required>
<div class="grid"><div><label>Input (stdin)</label><textarea name="stdin"></textarea></div><div><label>Đáp án chuẩn (stdout)</label><textarea name="expected"></textarea></div></div>
<div class="grid"><div><label>Trọng số</label><input type="number" name="weight" min="0.01" max="1000" step="0.01" value="1" required></div><div><label>Hiển thị</label><select name="hidden"><option value="false">Công khai</option><option value="true">Ẩn với sinh viên</option></select></div></div>
</section></template>
<script>function addTest(){if(document.querySelectorAll('.test-row').length>=20){alert('Tối đa 20 test case.');return;}document.getElementById('testRows').appendChild(document.getElementById('testTemplate').content.cloneNode(true));}if(!document.querySelector('.test-row'))addTest();</script>
</c:when><c:otherwise>
<p>Main class: <code><c:out value="${mainClass}" /></code>. Dưới đây chỉ có test công khai; bộ chấm có thể có thêm test ẩn.</p>
<c:forEach items="${tests}" var="t"><section class="card"><h2><c:out value="${t.name}" /></h2><div class="grid"><div>Input<pre><c:out value="${t.input}" /></pre></div><div>Đáp án mẫu<pre><c:out value="${t.expectedOutput}" /></pre></div></div></section></c:forEach>
<c:if test="${empty tests}"><p class="notice">Chưa có test công khai.</p></c:if>
</c:otherwise></c:choose>
</main></body></html>
