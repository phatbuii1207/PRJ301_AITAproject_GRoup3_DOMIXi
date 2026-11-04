package com.fpt.aita.listener;

import com.fpt.aita.service.queue.GradingQueueManager;
import java.nio.file.Paths;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public final class GradingLifecycleListener implements ServletContextListener {
    @Override public void contextInitialized(ServletContextEvent event) {
        String root = event.getServletContext().getRealPath("/uploads/submissions");
        if (root == null) root = Paths.get(System.getProperty("user.dir"), "uploads", "submissions").toString();
        GradingQueueManager.initialize(Paths.get(root));
    }
    @Override public void contextDestroyed(ServletContextEvent event) { GradingQueueManager.getInstance().shutdown(); }
}
