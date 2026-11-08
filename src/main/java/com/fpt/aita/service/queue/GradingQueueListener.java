package com.fpt.aita.service.queue;

import com.fpt.aita.util.UploadStorage;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * THÀNH VIÊN 3: Listener vòng đời ứng dụng web cho Grading Queue.
 * - Khởi động GradingQueueManager khi Tomcat deploy ứng dụng.
 * - Truyền đường dẫn lưu trữ ngoài webroot và thiết lập fallback cho queue manager.
 * - Dừng worker thread gracefully khi Tomcat undeploy / tắt.
 *
 * Đăng ký qua annotation @WebListener (Servlet 3.0+),
 * không cần thêm vào web.xml.
 */
@WebListener
public class GradingQueueListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();

        // Lấy đường dẫn tuyệt đối thư mục gốc webapp trên đĩa để cấu hình fallback cho bài nộp cũ
        String webAppRoot = ctx.getRealPath("/");
        if (webAppRoot == null) {
            webAppRoot = System.getProperty("user.dir");
            System.err.println("[GradingQueueListener] Không lấy được realPath, dùng working dir: " + webAppRoot);
        }

        String webAppUploadRoot = ctx.getRealPath("/uploads/submissions");
        if (webAppUploadRoot == null) webAppUploadRoot = new java.io.File(webAppRoot, "uploads/submissions").getPath();
        UploadStorage.getInstance().setFallbackDir(Paths.get(webAppUploadRoot));

        // Khởi tạo GradingQueueManager với đường dẫn thực ngoài webroot
        Path uploadRoot = UploadStorage.getInstance().getUploadDir();
        GradingQueueManager.initialize(uploadRoot);

        System.out.println("[GradingQueueListener] Grading Queue đã khởi động. uploadRoot=" + uploadRoot + ", fallback=" + webAppUploadRoot);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        GradingQueueManager.getInstance().shutdown();
        System.out.println("[GradingQueueListener] Grading Queue đã dừng gracefully.");
    }
}
