<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Báo Cáo Đánh Giá Mã Nguồn #${report != null ? report.reportId : submission.submissionId} - AITA Platform</title>
    <!-- Thư viện JSDiff tính toán Code Diff phía Client -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jsdiff/5.2.0/diff.min.js"></script>
    <style>
        :root {
            --bg-body: #0b1120;
            --bg-card: #1e293b;
            --bg-card-alt: #0f172a;
            --border-color: #334155;
            --text-main: #f8fafc;
            --text-muted: #94a3b8;
            --primary: #38bdf8;
            --primary-hover: #0284c7;
            --good: #10b981;
            --mid: #f59e0b;
            --bad: #ef4444;
            --info: #3b82f6;
        }

        * { box-sizing: border-box; }
        body {
            font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif;
            background-color: var(--bg-body);
            color: var(--text-main);
            margin: 0;
            padding: 24px 20px;
            line-height: 1.5;
        }
        .container { max-width: 1200px; margin: 0 auto; }

        /* HEADER & TOP NAV */
        .top-nav {
            display: flex;
            justify-content: space-between;
            align-items: center;
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            padding: 16px 24px;
            border-radius: 12px;
            margin-bottom: 24px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.3);
        }
        .breadcrumbs {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 14px;
            color: var(--text-muted);
        }
        .breadcrumbs a {
            color: var(--primary);
            text-decoration: none;
            transition: color 0.2s;
        }
        .breadcrumbs a:hover { text-decoration: underline; }
        .breadcrumbs span.separator { color: #64748b; }
        .nav-actions { display: flex; align-items: center; gap: 12px; }
        .btn {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 8px 16px;
            border-radius: 6px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            text-decoration: none;
            transition: all 0.2s;
            border: 1px solid transparent;
        }
        .btn-outline {
            background: transparent;
            color: var(--text-main);
            border-color: var(--border-color);
        }
        .btn-outline:hover {
            background: #334155;
            color: #fff;
        }
        .btn-primary {
            background: #0284c7;
            color: #fff;
        }
        .btn-primary:hover { background: #0369a1; }

        /* CARDS */
        .card {
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            padding: 24px;
            margin-bottom: 24px;
            box-shadow: 0 4px 16px rgba(0,0,0,0.25);
        }
        .card-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-bottom: 1px solid var(--border-color);
            padding-bottom: 16px;
            margin-bottom: 20px;
        }
        .card-title {
            margin: 0;
            font-size: 18px;
            color: #fff;
            display: flex;
            align-items: center;
            gap: 10px;
            font-weight: 700;
        }

        /* METADATA BAR */
        .meta-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 16px;
        }
        .meta-item {
            background: var(--bg-card-alt);
            padding: 14px 18px;
            border-radius: 8px;
            border: 1px solid #1e293b;
        }
        .meta-label {
            font-size: 12px;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 0.5px;
            margin-bottom: 4px;
        }
        .meta-val {
            font-size: 15px;
            font-weight: 600;
            color: #e2e8f0;
            word-break: break-all;
        }

        /* SCORE SECTION (THÀNH PHẦN 1) */
        .score-hero-container {
            display: grid;
            grid-template-columns: 280px 1fr;
            gap: 28px;
            align-items: center;
        }
        @media (max-width: 820px) {
            .score-hero-container { grid-template-columns: 1fr; }
        }
        .gauge-wrapper {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            background: var(--bg-card-alt);
            border-radius: 12px;
            padding: 24px;
            border: 1px solid var(--border-color);
        }
        .radial-gauge {
            position: relative;
            width: 170px;
            height: 170px;
        }
        .radial-gauge svg {
            width: 100%;
            height: 100%;
            transform: rotate(-90deg);
        }
        .gauge-bg {
            fill: none;
            stroke: #1e293b;
            stroke-width: 12;
        }
        .gauge-progress {
            fill: none;
            stroke-width: 12;
            stroke-linecap: round;
            transition: stroke-dashoffset 1s ease-in-out;
        }
        .gauge-center-text {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            text-align: center;
        }
        .gauge-score-number {
            font-size: 44px;
            font-weight: 800;
            line-height: 1;
        }
        .gauge-score-total {
            font-size: 14px;
            color: var(--text-muted);
            font-weight: 600;
        }
        .score-grade-badge {
            margin-top: 14px;
            padding: 6px 16px;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        .good { color: var(--good); }
        .mid { color: var(--mid); }
        .bad { color: var(--bad); }
        .badge-good { background: rgba(16, 185, 129, 0.15); color: #34d399; border: 1px solid rgba(16, 185, 129, 0.3); }
        .badge-mid { background: rgba(245, 158, 11, 0.15); color: #fbbf24; border: 1px solid rgba(245, 158, 11, 0.3); }
        .badge-bad { background: rgba(239, 68, 68, 0.15); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.3); }

        .metrics-cards {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
            gap: 16px;
        }
        .metric-box {
            background: var(--bg-card-alt);
            border: 1px solid var(--border-color);
            border-radius: 10px;
            padding: 16px 20px;
            position: relative;
            overflow: hidden;
        }
        .metric-box::before {
            content: '';
            position: absolute;
            left: 0;
            top: 0;
            bottom: 0;
            width: 4px;
        }
        .metric-box.total::before { background: var(--primary); }
        .metric-box.critical::before { background: var(--bad); }
        .metric-box.warning::before { background: var(--mid); }
        .metric-box.info::before { background: var(--info); }

        .metric-box-title { font-size: 13px; color: var(--text-muted); margin-bottom: 6px; }
        .metric-box-val { font-size: 28px; font-weight: 800; }

        /* FILTER BUTTONS (THÀNH PHẦN 2) */
        .filter-bar {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            margin-bottom: 18px;
        }
        .filter-btn {
            background: var(--bg-card-alt);
            border: 1px solid var(--border-color);
            color: var(--text-muted);
            padding: 7px 16px;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s;
        }
        .filter-btn:hover { color: #fff; border-color: #64748b; }
        .filter-btn.active {
            background: #0284c7;
            color: #fff;
            border-color: #0284c7;
        }

        /* ISSUES TABLE */
        .table-responsive { width: 100%; overflow-x: auto; }
        table.issues-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 13.5px;
        }
        table.issues-table th {
            background: var(--bg-card-alt);
            color: var(--text-muted);
            font-weight: 600;
            text-transform: uppercase;
            font-size: 12px;
            letter-spacing: 0.5px;
            padding: 12px 14px;
            text-align: left;
            border-bottom: 2px solid var(--border-color);
        }
        table.issues-table td {
            padding: 14px;
            border-bottom: 1px solid #283548;
            vertical-align: top;
        }
        table.issues-table tr:hover td {
            background: rgba(255, 255, 255, 0.02);
        }

        /* SEVERITY BADGES */
        .sev-pill {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            padding: 3px 10px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 700;
            white-space: nowrap;
        }
        .sev-Critical {
            background: rgba(239, 68, 68, 0.15);
            color: #f87171;
            border: 1px solid rgba(239, 68, 68, 0.3);
        }
        .sev-Warning {
            background: rgba(245, 158, 11, 0.15);
            color: #fbbf24;
            border: 1px solid rgba(245, 158, 11, 0.3);
        }
        .sev-Info {
            background: rgba(59, 130, 246, 0.15);
            color: #60a5fa;
            border: 1px solid rgba(59, 130, 246, 0.3);
        }

        .code-tag {
            background: #0f172a;
            color: #38bdf8;
            padding: 2px 7px;
            border-radius: 4px;
            font-family: 'Consolas', 'Monaco', monospace;
            font-size: 12.5px;
            border: 1px solid #334155;
        }
        .line-pill {
            background: #334155;
            color: #f1f5f9;
            padding: 2px 8px;
            border-radius: 4px;
            font-family: monospace;
            font-size: 12px;
            font-weight: 600;
        }

        /* AI EXPLANATION BOX TRONG BẢNG */
        .ai-explain-btn {
            background: rgba(14, 165, 233, 0.12);
            color: #38bdf8;
            border: 1px solid rgba(14, 165, 233, 0.3);
            border-radius: 6px;
            padding: 4px 10px;
            font-size: 12px;
            cursor: pointer;
            margin-top: 6px;
            display: inline-flex;
            align-items: center;
            gap: 4px;
            transition: all 0.2s;
        }
        .ai-explain-btn:hover {
            background: #0284c7;
            color: #fff;
        }
        .ai-explain-content {
            margin-top: 8px;
            padding: 10px 14px;
            background: #090e17;
            border-left: 3px solid #38bdf8;
            border-radius: 4px;
            font-size: 13px;
            line-height: 1.6;
            color: #cbd5e1;
        }

        /* CODE DIFF SECTION (THÀNH PHẦN 3) */
        .diff-container {
            background: #090e17;
            border: 1px solid var(--border-color);
            border-radius: 10px;
            overflow: hidden;
            font-family: 'Consolas', 'Courier New', monospace;
            font-size: 13px;
        }
        .diff-header-bar {
            display: grid;
            grid-template-columns: 1fr 1fr;
            background: #131c2e;
            border-bottom: 1px solid var(--border-color);
            padding: 10px 16px;
            font-weight: 600;
            font-size: 13px;
        }
        .diff-header-bar > div {
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .diff-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            min-height: 250px;
            max-height: 520px;
            overflow-y: auto;
        }
        @media (max-width: 860px) {
            .diff-header-bar, .diff-grid { grid-template-columns: 1fr; }
        }
        .diff-column {
            overflow-x: auto;
            padding: 12px 0;
        }
        .diff-column.old-col {
            border-right: 1px solid var(--border-color);
            background: rgba(15, 23, 42, 0.6);
        }
        .diff-column.new-col {
            background: rgba(15, 23, 42, 0.9);
        }
        .diff-line {
            display: flex;
            padding: 2px 14px;
            white-space: pre-wrap;
            word-break: break-all;
            line-height: 1.5;
            margin: 0;
            font-family: 'Consolas', 'Courier New', monospace;
        }
        .diff-line .line-num {
            width: 38px;
            color: #475569;
            user-select: none;
            text-align: right;
            padding-right: 12px;
            flex-shrink: 0;
        }
        .diff-line .line-prefix {
            width: 16px;
            user-select: none;
            font-weight: bold;
            flex-shrink: 0;
        }
        .diff-line .line-code {
            flex-grow: 1;
        }

        /* Highlighting theo đặc tả jsdiff */
        .del {
            background-color: rgba(239, 68, 68, 0.18) !important;
            color: #fca5a5;
        }
        .del .line-prefix { color: #f87171; }
        .add {
            background-color: rgba(16, 185, 129, 0.18) !important;
            color: #86efac;
        }
        .add .line-prefix { color: #4ade80; }
        .same {
            color: #cbd5e1;
        }

        /* OPTIMIZATION INFO BANNER */
        .opt-banner {
            background: linear-gradient(135deg, rgba(14, 165, 233, 0.1) 0%, rgba(59, 130, 246, 0.05) 100%);
            border: 1px solid rgba(14, 165, 233, 0.3);
            border-radius: 8px;
            padding: 16px 20px;
            margin-bottom: 18px;
        }
        .opt-banner-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 8px;
        }
        .opt-method-name {
            font-size: 16px;
            font-weight: 700;
            color: #38bdf8;
        }
        .complexity-tag {
            background: #0f172a;
            border: 1px solid #38bdf8;
            color: #7dd3fc;
            padding: 3px 10px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 600;
        }
        .opt-explanation-text {
            color: #cbd5e1;
            font-size: 13.5px;
            line-height: 1.6;
        }

        /* PENDING STATE STYLES */
        .pending-hero {
            text-align: center;
            padding: 60px 20px;
        }
        .pending-spinner {
            width: 70px;
            height: 70px;
            border: 6px solid #1e293b;
            border-top: 6px solid #38bdf8;
            border-radius: 50%;
            animation: spin 1s linear infinite;
            margin: 0 auto 24px;
        }
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
        .pipeline-steps {
            max-width: 650px;
            margin: 30px auto;
            text-align: left;
            background: var(--bg-card-alt);
            border: 1px solid var(--border-color);
            border-radius: 10px;
            padding: 20px 24px;
        }
        .step-item {
            display: flex;
            align-items: center;
            gap: 14px;
            padding: 12px 0;
            border-bottom: 1px solid #1e293b;
        }
        .step-item:last-child { border-bottom: none; }
        .step-icon {
            font-size: 20px;
            width: 32px;
            text-align: center;
        }
        .step-text h4 { margin: 0 0 4px 0; font-size: 14px; color: #f1f5f9; }
        .step-text p { margin: 0; font-size: 12px; color: var(--text-muted); }

        /* MULTI METHOD TAB SWITCHER */
        .method-tabs {
            display: flex;
            gap: 8px;
            margin-bottom: 14px;
            overflow-x: auto;
        }
        .method-tab {
            background: var(--bg-card-alt);
            border: 1px solid var(--border-color);
            color: var(--text-muted);
            padding: 6px 14px;
            border-radius: 6px;
            font-size: 13px;
            cursor: pointer;
            transition: all 0.2s;
        }
        .method-tab.active {
            background: #0369a1;
            color: #fff;
            border-color: #38bdf8;
            font-weight: 600;
        }
    </style>
</head>
<body>

<div class="container">

    <!-- TOP NAVIGATION BAR -->
    <div class="top-nav">
        <div class="breadcrumbs">
            <a href="home">Trang Chủ</a>
            <span class="separator">/</span>
            <a href="home">Môn Học</a>
            <span class="separator">/</span>
            <span>Báo Cáo Đánh Giá Bài Nộp #${report != null ? report.submissionId : submission.submissionId}</span>
        </div>
        <div class="nav-actions">
            <button type="button" class="btn btn-outline" onclick="window.print()">
                🖨️ In / Xuất PDF
            </button>
            <a href="javascript:history.back()" class="btn btn-primary">
                ⬅ Quay Lại
            </a>
        </div>
    </div>

    <!-- TRƯỜNG HỢP 1: BÁO CÁO ĐANG TRONG HÀNG ĐỢI XỬ LÝ (PENDING STATE) -->
    <c:if test="${isPending == true}">
        <div class="card pending-hero">
            <div class="pending-spinner"></div>
            <h2 style="color: #38bdf8; margin-bottom: 8px;">⏳ Đang Phân Tích Mã Nguồn...</h2>
            <p style="color: var(--text-muted); max-width: 550px; margin: 0 auto 20px;">
                Hệ thống AITA đang chạy quy trình kiểm tra tĩnh qua AST và khởi tạo mô hình AI tối ưu thuật toán cho bài nộp 
                <b>#${submission.submissionId}</b>.
            </p>

            <div class="pipeline-steps">
                <div class="step-item">
                    <div class="step-icon">⚡</div>
                    <div class="step-text">
                        <h4>1. Khảo Sát Cú Pháp Trừu Tượng (AST & Rules Engine)</h4>
                        <p style="color: #34d399;">✔ Đang quét độ phức tạp Cyclomatic, lồng vòng lặp và lỗi bảo mật.</p>
                    </div>
                </div>
                <div class="step-item">
                    <div class="step-icon">🤖</div>
                    <div class="step-text">
                        <h4>2. Phân Tích & Tối Ưu Thuật Toán AI (Gemini Engine)</h4>
                        <p style="color: #fbbf24;">⏳ Đang xếp hàng đợi gợi ý mã nguồn tối ưu song song.</p>
                    </div>
                </div>
                <div class="step-item">
                    <div class="step-icon">📊</div>
                    <div class="step-text">
                        <h4>3. Tổng Hợp Báo Cáo Chất Lượng & Điểm Số</h4>
                        <p>Chờ hoàn tất bước 1 và 2 để sinh bảng điểm và mã chênh lệch.</p>
                    </div>
                </div>
            </div>

            <button type="button" class="btn btn-primary" onclick="window.location.reload();" style="margin-top: 10px;">
                🔄 Làm mới trạng thái (Refresh)
            </button>
            <script>
                // Tự động kiểm tra làm mới sau mỗi 6 giây
                setTimeout(function() { window.location.reload(); }, 6000);
            </script>
        </div>
    </c:if>

    <!-- TRƯỜNG HỢP 2: BÁO CÁO ĐÃ CÓ KẾT QUẢ ĐẦY ĐỦ -->
    <c:if test="${isPending != true}">

        <!-- META INFORMATION CARD -->
        <div class="card" style="padding: 18px 24px;">
            <div class="meta-grid">
                <div class="meta-item">
                    <div class="meta-label">Sinh Viên Nộp Bài</div>
                    <div class="meta-val">
                        👨‍🎓 <c:out value="${report.studentName}"/>
                        <span style="font-size: 12px; color: var(--text-muted);">
                            (<c:out value="${not empty report.studentUsername ? report.studentUsername : 'ID #'.concat(report.studentId)}"/>)
                        </span>
                    </div>
                </div>
                <div class="meta-item">
                    <div class="meta-label">Đề Bài Tập</div>
                    <div class="meta-val">
                        📝 <c:out value="${report.assignmentTitle}"/>
                    </div>
                </div>
                <div class="meta-item">
                    <div class="meta-label">Tệp Nguồn / Dung Lượng</div>
                    <div class="meta-val">
                        📁 <c:out value="${report.fileName}"/>
                        <span style="font-size: 12px; color: var(--text-muted);">
                            (<fmt:formatNumber value="${report.fileSize / 1024}" maxFractionDigits="1"/> KB)
                        </span>
                    </div>
                </div>
                <div class="meta-item">
                    <div class="meta-label">Thời Gian Phân Tích</div>
                    <div class="meta-val">
                        ⏰ <fmt:formatDate value="${report.createdAt}" pattern="dd/MM/yyyy HH:mm:ss"/>
                    </div>
                </div>
            </div>
        </div>

        <!-- THÀNH PHẦN 1: ĐIỂM CHẤT LƯỢNG MÃ NGUỒN (0 - 100) -->
        <div class="card">
            <div class="card-header">
                <h3 class="card-title">
                    <span>🏆</span> Điểm Chất Lượng Mã Nguồn (Code Quality Score)
                </h3>
                <span class="score-grade-badge badge-${report.scoreClass}">
                    <c:out value="${report.scoreLabel}"/>
                </span>
            </div>

            <div class="score-hero-container">
                <!-- Vòng tròn điểm số SVG Radial Gauge -->
                <div class="gauge-wrapper">
                    <div class="radial-gauge">
                        <svg viewBox="0 0 100 100">
                            <circle class="gauge-bg" cx="50" cy="50" r="42" />
                            <!-- Chu vi hình tròn r=42 là 263.89 -->
                            <circle id="scoreProgressCircle" class="gauge-progress" cx="50" cy="50" r="42"
                                    stroke="${report.scoreColor}"
                                    stroke-dasharray="263.89"
                                    stroke-dashoffset="263.89" />
                        </svg>
                        <div class="gauge-center-text">
                            <div class="gauge-score-number ${report.scoreClass}">
                                <c:out value="${report.qualityScore}"/>
                            </div>
                            <div class="gauge-score-total">/ 100 điểm</div>
                        </div>
                    </div>
                    <div style="font-size: 12px; color: var(--text-muted); margin-top: 12px; text-align: center;">
                        Dựa trên phân tích tĩnh AST & Trọng số vi phạm
                    </div>
                </div>

                <!-- Thẻ thống kê chỉ số tổng quan -->
                <div class="metrics-cards">
                    <div class="metric-box total">
                        <div class="metric-box-title">Tổng Vấn Đề Phát Hiện</div>
                        <div class="metric-box-val" style="color: var(--primary);">
                            <c:out value="${issueTotal}"/>
                        </div>
                        <div style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">Tổng số lỗi & cảnh báo</div>
                    </div>
                    <div class="metric-box critical">
                        <div class="metric-box-title">Lỗi Nghiêm Trọng (Critical)</div>
                        <div class="metric-box-val" style="color: var(--bad);">
                            <c:out value="${criticalCount}"/>
                        </div>
                        <div style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">Nguy cơ bảo mật & crash</div>
                    </div>
                    <div class="metric-box warning">
                        <div class="metric-box-title">Cảnh Báo Hiệu Năng (Warning)</div>
                        <div class="metric-box-val" style="color: var(--mid);">
                            <c:out value="${warningCount}"/>
                        </div>
                        <div style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">Vòng lặp & cấu trúc O(N^2+)</div>
                    </div>
                    <div class="metric-box info">
                        <div class="metric-box-title">Quy Chuẩn Code (Info)</div>
                        <div class="metric-box-val" style="color: var(--info);">
                            <c:out value="${infoCount}"/>
                        </div>
                        <div style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">Quy ước đặt tên & code smell</div>
                    </div>
                </div>
            </div>
        </div>

        <!-- THÀNH PHẦN 2: DANH SÁCH LỖI TRỰC QUAN (ISSUES TABLE) -->
        <div class="card">
            <div class="card-header">
                <h3 class="card-title">
                    <span>🔍</span> Danh Sách Lỗi & Cảnh Báo Phát Hiện (Static Analysis Issues)
                </h3>
                <div style="font-size: 13px; color: var(--text-muted);">
                    <span id="issueFilterSummary">Hiển thị <b><c:out value="${issueTotal}"/></b>/<b><c:out value="${issueTotal}"/></b> vấn đề</span>
                </div>
            </div>

            <!-- Bộ lọc mức độ lỗi trực quan -->
            <div class="filter-bar">
                <button type="button" class="filter-btn active" onclick="filterIssues('ALL', this)">
                    Tất Cả (<c:out value="${issueTotal}"/>)
                </button>
                <button type="button" class="filter-btn" onclick="filterIssues('Critical', this)">
                    ⛔ Critical (<c:out value="${criticalCount}"/>)
                </button>
                <button type="button" class="filter-btn" onclick="filterIssues('Warning', this)">
                    ⚠️ Warning (<c:out value="${warningCount}"/>)
                </button>
                <button type="button" class="filter-btn" onclick="filterIssues('Info', this)">
                    ℹ️ Info (<c:out value="${infoCount}"/>)
                </button>
            </div>

            <div class="table-responsive">
                <table class="issues-table">
                    <thead>
                        <tr>
                            <th style="width: 120px;">Mức Độ</th>
                            <th style="width: 220px;">Quy Tắc Vi Phạm</th>
                            <th style="width: 160px;">Vị Trí Hàm</th>
                            <th style="width: 80px;">Dòng</th>
                            <th>Mô Tả Chi Tiết & Khuyến Nghị AI</th>
                        </tr>
                    </thead>
                    <tbody id="issuesTableBody">
                        <c:choose>
                            <c:when test="${empty issues}">
                                <tr>
                                    <td colspan="5" style="text-align: center; padding: 28px; color: var(--text-muted);">
                                        Chưa có lỗi nào được ghi nhận trong danh sách phân tích tĩnh. Điều này không khẳng định mã nguồn không có lỗi; xem nhận xét AI theo từng hàm bên dưới (nếu có).
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="i" items="${issues}">
                                    <tr class="issue-row sev-row-${i.severity}" data-severity="${i.severity}">
                                        <td>
                                            <span class="sev-pill sev-${i.severity}">
                                                ${i.severityIcon} <c:out value="${i.severity}"/>
                                            </span>
                                        </td>
                                        <td>
                                            <div style="font-weight: 600; color: #f1f5f9;">
                                                <c:out value="${not empty i.ruleName ? i.ruleName : i.ruleCode}"/>
                                            </div>
                                            <div style="font-size: 11px; color: var(--text-muted); margin-top: 3px;">
                                                Mã: <code><c:out value="${i.ruleCode}"/></code>
                                                <span style="margin: 0 4px;">•</span>
                                                Nhóm: <c:out value="${i.category}"/>
                                            </div>
                                        </td>
                                        <td>
                                            <span class="code-tag">
                                                <c:out value="${i.methodName}"/>()
                                            </span>
                                            <c:if test="${not empty i.fileName}">
                                                <div style="font-size: 11px; color: var(--text-muted); margin-top: 3px;">
                                                    📁 <c:out value="${i.fileName}"/>
                                                </div>
                                            </c:if>
                                        </td>
                                        <td>
                                            <span class="line-pill">L.<c:out value="${i.lineNumber}"/></span>
                                        </td>
                                        <td>
                                            <div style="color: #e2e8f0; font-weight: 500;">
                                                <c:out value="${i.message}"/>
                                            </div>

                                            <c:if test="${not empty i.aiExplanation}">
                                                <div class="ai-explain-content">
                                                    <b>🤖 AI giải thích / đề xuất:</b><br/>
                                                    <c:out value="${i.aiExplanation}"/>
                                                </div>
                                            </c:if>
                                            <c:if test="${empty i.aiExplanation}">
                                                <div style="font-size: 12px; color: var(--text-muted); margin-top: 8px;">
                                                    AI chưa có nhận xét riêng cho lỗi này.
                                                </div>
                                            </c:if>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>
            <c:if test="${not empty optimizations}">
                <div style="margin-top: 22px;">
                    <h4 style="margin: 0 0 12px; color: #38bdf8;">🤖 Nhận xét AI theo từng hàm</h4>
                    <c:forEach var="op" items="${optimizations}">
                        <div class="ai-explain-content" style="display: block; margin-top: 10px;">
                            <b><c:out value="${op.methodName}"/>():</b>
                            <c:out value="${op.explanation}"/>
                        </div>
                    </c:forEach>
                </div>
            </c:if>
        </div>

        <!-- THÀNH PHẦN 3: SO SÁNH MÃ NGUỒN CODE DIFF (SIDE-BY-SIDE VỚI JSDIFF) -->
        <div class="card">
            <div class="card-header">
                <h3 class="card-title">
                    <span>⚡</span> Tối Ưu Thuật Toán AI & So Sánh Mã Nguồn (Code Diff)
                </h3>
                <span style="font-size: 12px; color: var(--text-muted);">
                    Tự động tạo bởi Mô hình Phân tích Thuật toán AI
                </span>
            </div>

            <c:choose>
                <c:when test="${empty opt && empty optimizations}">
                    <div style="text-align: center; padding: 40px; color: var(--text-muted); background: var(--bg-card-alt); border-radius: 8px;">
                        ℹ️ Không có đoạn mã nào cần đề xuất tái cấu trúc thuật toán từ AI cho bài nộp này.
                    </div>
                </c:when>
                <c:otherwise>
                    <!-- Bộ chuyển tab nếu bài có nhiều phương thức được tối ưu -->
                    <c:if test="${not empty optimizations && optimizations.size() > 1}">
                        <div class="method-tabs">
                            <c:forEach var="op" items="${optimizations}" varStatus="status">
                                <button type="button" class="method-tab ${status.first ? 'active' : ''}"
                                        onclick="switchOptimization(${status.index}, this)">
                                    ${op.methodName}()
                                </button>
                            </c:forEach>
                        </div>
                    </c:if>

                    <!-- Khung giải thích lý do tối ưu của AI -->
                    <div class="opt-banner">
                        <div class="opt-banner-header">
                            <div class="opt-method-name">
                                🚀 Phương thức: <span id="displayMethodName"><c:out value="${opt.methodName}"/></span>()
                            </div>
                            <span class="complexity-tag" id="displayComplexity">
                                Độ phức tạp: <c:out value="${not empty opt.complexityBefore ? opt.complexityBefore : '—'}"/> ➔ <c:out value="${not empty opt.complexityAfter ? opt.complexityAfter : '—'}"/>
                            </span>
                        </div>
                        <div class="opt-explanation-text" id="displayExplanation">
                            <c:out value="${opt.explanation}"/>
                        </div>
                    </div>

                    <!-- KHUNG HIỂN THỊ CODE DIFF 2 CỘT SONG SONG -->
                    <div class="diff-container">
                        <div class="diff-header-bar">
                            <div style="color: #fca5a5;">
                                <span>🟥</span> <b>Mã Nguồn Gốc (Original Code - Bài nộp)</b>
                            </div>
                            <div style="color: #86efac;">
                                <span>🟩</span> <b>Mã Đã Tối Ưu (AI Refactored Code)</b>
                            </div>
                        </div>
                        <div class="diff-grid" id="diffGridOutput">
                            <!-- Kết quả diff sẽ được render động an toàn qua jsdiff -->
                        </div>
                    </div>

                    <!-- Hidden storage an toàn (chống XSS bằng c:out) -->
                    <div id="optimizationsDataStore" style="display: none;">
                        <c:forEach var="op" items="${not empty optimizations ? optimizations : [opt]}" varStatus="status">
                            <div id="optData_${status.index}">
                                <input type="hidden" class="store-method" value="<c:out value="${op.methodName}"/>" />
                                <input type="hidden" class="store-file" value="<c:out value="${op.fileName}"/>" />
                                <input type="hidden" class="store-complexity" value="<c:out value="${not empty op.complexityBefore ? op.complexityBefore : '—'}"/> ➔ <c:out value="${not empty op.complexityAfter ? op.complexityAfter : '—'}"/>" />
                                <textarea class="store-explanation"><c:out value="${op.explanation}"/></textarea>
                                <textarea class="store-old-code"><c:out value="${op.originalCode}"/></textarea>
                                <textarea class="store-new-code"><c:out value="${op.optimizedCode}"/></textarea>
                            </div>
                        </c:forEach>
                    </div>

                    <!-- Textarea mặc định theo chuẩn Bước 3 của đề bài -->
                    <textarea id="oldCode" hidden><c:out value="${opt.originalCode}"/></textarea>
                    <textarea id="newCode" hidden><c:out value="${opt.optimizedCode}"/></textarea>
                </c:otherwise>
            </c:choose>
        </div>

    </c:if>

</div>

<script>
    // 1. ANIMATION HIỆU ỨNG VÒNG TRÒN ĐIỂM SỐ
    window.addEventListener('DOMContentLoaded', function() {
        var circle = document.getElementById('scoreProgressCircle');
        if (circle) {
            var score = ${report != null ? report.qualityScore : 0};
            var radius = 42;
            var circumference = 2 * Math.PI * radius; // 263.89
            var offset = circumference - (score / 100) * circumference;
            setTimeout(function() {
                circle.style.strokeDashoffset = offset;
            }, 150);
        }

        // Khởi chạy Code Diff
        renderDiffFromStore(0);
    });

    // 2. BỘ LỌC DANH SÁCH LỖI THEO SEVERITY
    function filterIssues(severity, btn) {
        // Cập nhật trạng thái active của nút
        var buttons = document.querySelectorAll('.filter-btn');
        buttons.forEach(function(b) { b.classList.remove('active'); });
        btn.classList.add('active');

        // Lọc từng hàng trong bảng
        var rows = document.querySelectorAll('#issuesTableBody tr.issue-row');
        var visibleCount = 0;
        var emptyRow = document.getElementById('issueFilterEmpty');
        rows.forEach(function(row) {
            var rowSev = (row.getAttribute('data-severity') || '').toLowerCase();
            if (severity === 'ALL' || rowSev === severity.toLowerCase()) {
                row.style.display = '';
                visibleCount++;
            } else {
                row.style.display = 'none';
            }
        });
        if (rows.length > 0 && visibleCount === 0) {
            if (!emptyRow) {
                emptyRow = document.createElement('tr');
                emptyRow.id = 'issueFilterEmpty';
                var cell = document.createElement('td');
                cell.colSpan = 5;
                cell.style.textAlign = 'center';
                cell.style.padding = '28px';
                cell.style.color = 'var(--text-muted)';
                emptyRow.appendChild(cell);
                document.getElementById('issuesTableBody').appendChild(emptyRow);
            }
            emptyRow.firstChild.textContent = 'Không có lỗi mức độ ' + severity + '.';
        } else if (emptyRow) {
            emptyRow.remove();
        }
        var summary = document.getElementById('issueFilterSummary');
        if (summary) {
            summary.textContent = 'Hiển thị ' + visibleCount + '/' + rows.length + ' vấn đề';
        }
    }

    // 3. CHUYỂN ĐỔI TAB TỐI ƯU HÓA PHƯƠNG THỨC
    function switchOptimization(index, tabBtn) {
        var tabs = document.querySelectorAll('.method-tab');
        tabs.forEach(function(t) { t.classList.remove('active'); });
        tabBtn.classList.add('active');
        renderDiffFromStore(index);
    }

    // 5. RENDER CODE DIFF 2 CỘT BẰNG JSDIFF (AN TOÀN TUYỆT ĐỐI CHỐNG XSS VỚI textContent)
    function renderDiffFromStore(index) {
        var dataBox = document.getElementById('optData_' + index);
        if (!dataBox) return;

        var methodName = dataBox.querySelector('.store-method').value;
        var complexity = dataBox.querySelector('.store-complexity').value;
        var explanation = dataBox.querySelector('.store-explanation').value;
        var oldCode = dataBox.querySelector('.store-old-code').value;
        var newCode = dataBox.querySelector('.store-new-code').value;

        // Cập nhật banner thông tin
        var displayMethod = document.getElementById('displayMethodName');
        var displayComp = document.getElementById('displayComplexity');
        var displayExp = document.getElementById('displayExplanation');
        if (displayMethod) displayMethod.textContent = methodName;
        if (displayComp) displayComp.textContent = 'Độ phức tạp: ' + complexity;
        if (displayExp) displayExp.textContent = explanation;

        // Render ra khung Diff 2 cột song song
        renderSideBySideDiff(oldCode, newCode);
    }

    function renderSideBySideDiff(oldCode, newCode) {
        var container = document.getElementById('diffGridOutput');
        if (!container) return;
        container.innerHTML = '';

        var leftCol = document.createElement('div');
        leftCol.className = 'diff-column old-col';

        var rightCol = document.createElement('div');
        rightCol.className = 'diff-column new-col';

        if (typeof Diff === 'undefined') {
            container.innerHTML = '<div style="padding:20px; color:#ef4444;">Không tải được thư viện Diff.</div>';
            return;
        }

        // Tính toán các khối dòng thay đổi bằng Diff.diffLines
        var diffParts = Diff.diffLines(oldCode, newCode);

        var oldLineNum = 1;
        var newLineNum = 1;

        diffParts.forEach(function(part) {
            var rawLines = part.value.split('\n');
            if (rawLines[rawLines.length - 1] === '') {
                rawLines.pop();
            }

            rawLines.forEach(function(lineText) {
                if (part.removed) {
                    // Dòng bị xóa hoặc thay thế ở bản cũ
                    var leftRow = createDiffLineElement(oldLineNum++, '-', lineText, 'del');
                    var rightRow = createDiffLineElement('', ' ', '', 'del empty-placeholder');
                    leftCol.appendChild(leftRow);
                    rightCol.appendChild(rightRow);
                } else if (part.added) {
                    // Dòng được thêm mới ở bản tối ưu
                    var leftRow = createDiffLineElement('', ' ', '', 'add empty-placeholder');
                    var rightRow = createDiffLineElement(newLineNum++, '+', lineText, 'add');
                    leftCol.appendChild(leftRow);
                    rightCol.appendChild(rightRow);
                } else {
                    // Dòng giữ nguyên (same)
                    var leftRow = createDiffLineElement(oldLineNum++, ' ', lineText, 'same');
                    var rightRow = createDiffLineElement(newLineNum++, ' ', lineText, 'same');
                    leftCol.appendChild(leftRow);
                    rightCol.appendChild(rightRow);
                }
            });
        });

        container.appendChild(leftCol);
        container.appendChild(rightCol);
    }

    function createDiffLineElement(lineNum, prefix, text, className) {
        var row = document.createElement('div');
        row.className = 'diff-line ' + className;

        var numSpan = document.createElement('span');
        numSpan.className = 'line-num';
        numSpan.textContent = lineNum;

        var prefixSpan = document.createElement('span');
        prefixSpan.className = 'line-prefix';
        prefixSpan.textContent = prefix;

        var codeSpan = document.createElement('span');
        codeSpan.className = 'line-code';
        codeSpan.textContent = text; // Dùng textContent để chống XSS 100%

        row.appendChild(numSpan);
        row.appendChild(prefixSpan);
        row.appendChild(codeSpan);
        return row;
    }
</script>

</body>
</html>
