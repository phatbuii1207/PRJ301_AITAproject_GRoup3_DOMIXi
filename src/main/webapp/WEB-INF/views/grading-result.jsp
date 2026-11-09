<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<c:if test="${processing}"><meta http-equiv="refresh" content="3"></c:if>
<title>Kết quả chấm bài</title><%@ include file="grading-style.jspf" %></head><body><main>
<a href="${pageContext.request.contextPath}/course?id=${assignment.courseId}">← Quay lại môn học</a>
<h1>Kết quả chấm: <c:out value="${assignment.title}" /></h1><p>Bài nộp #${submission.submissionId}</p>
<c:if test="${assignment.rubricType == 'NONE'}"><a class="button" href="${pageContext.request.contextPath}/grading-tests?assignmentId=${assignment.assignmentId}">Xem test case</a></c:if>
<c:if test="${canManage}"><form method="post" action="${pageContext.request.contextPath}/grading-result" style="display:inline">
<input type="hidden" name="submissionId" value="${submission.submissionId}"><input type="hidden" name="csrf" value="${sessionScope.gradingCsrf}"><button type="submit">Chấm lại</button></form></c:if>
<c:choose><c:when test="${processing}"><p class="notice">Đang chờ hoặc đang chấm bài. Trang tự cập nhật sau 3 giây.</p></c:when>
<c:when test="${empty grade}"><p class="notice">Bài nộp chưa có kết quả chấm. Giảng viên có thể chọn chấm lại.</p></c:when>
<c:otherwise>
<section class="card"><strong><c:out value="${grade.verdict}" /></strong>
<c:choose><c:when test="${grade.complete}"><div class="score"><c:out value="${grade.score}" /> / 10</div>
<c:if test="${assignment.rubricType == 'NONE'}"><p>Đạt ${grade.passed}/${grade.total} test.</p></c:if>
<c:if test="${assignment.rubricType != 'NONE'}"><p>Điểm theo rubric <c:out value="${assignment.rubricType}"/>; không áp dụng test case.</p></c:if>
</c:when>
<c:otherwise><h2>Chưa có điểm hợp lệ</h2></c:otherwise></c:choose>
<p><c:out value="${grade.message}" /></p><p class="muted">Main: <c:out value="${grade.mainClass}" /> · Phiên bản bộ test: ${grade.configVersion}</p></section>
<c:forEach items="${grade.cases}" var="t"><section class="card">
<div class="case-heading"><h2><c:out value="${t.name}" /></h2><span class="badge"><c:out value="${t.verdict}" /></span></div>
<p>Trọng số: ${t.weight} · Thời gian: ${t.executionTimeMs} ms</p>
<c:choose><c:when test="${t.hidden and sessionScope.currentUser.role == 'Student'}"><p class="muted">Nội dung test ẩn và output không được công bố.</p></c:when>
<c:otherwise><div class="grid"><div>Input<pre><c:out value="${t.input}" /></pre></div><div>Đáp án<pre><c:out value="${t.expectedOutput}" /></pre></div></div>
Output thực tế<pre><c:out value="${t.actualOutput}" /></pre>
<c:if test="${not empty t.diagnostics}">Thông báo lỗi (stderr)<pre><c:out value="${t.diagnostics}" /></pre></c:if>
</c:otherwise></c:choose></section></c:forEach>
</c:otherwise></c:choose>
</main></body></html>
