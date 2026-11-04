package com.fpt.aita.service.queue;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * THÀNH VIÊN 3: Listener vòng đời ứng dụng web cho Grading Queue.
 * - Khởi động GradingQueueManager khi Tomcat deploy ứng dụng.
 * - Truyền đường dẫn thực (realPath) của thư mục uploads cho queue manager.
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

        // Lấy đường dẫn tuyệt đối thư mục gốc webapp trên đĩa
        String webAppRoot = ctx.getRealPath("/");
        if (webAppRoot == null) {
            webAppRoot = System.getProperty("user.dir");
            System.err.println("[GradingQueueListener] Không lấy được realPath, dùng working dir: " + webAppRoot);
        }

        // Khởi tạo GradingQueueManager với đường dẫn thực
        String uploadRoot = ctx.getRealPath("/uploads/submissions");
        if (uploadRoot == null) uploadRoot = new java.io.File(webAppRoot, "uploads/submissions").getPath();
        GradingQueueManager.initialize(java.nio.file.Paths.get(uploadRoot));

        System.out.println("[GradingQueueListener] Grading Queue đã khởi động. webAppRoot=" + webAppRoot);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        GradingQueueManager.getInstance().shutdown();
        System.out.println("[GradingQueueListener] Grading Queue đã dừng gracefully.");
    }
}
