package com.fpt.aita.controller;

import com.fpt.aita.model.User;
import java.lang.reflect.Proxy;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.Test;
import static org.junit.Assert.*;

public class AnalysisReportAuthorizationTest {
    @Test public void studentCannotReadEvenOwnReportOrMockReport() throws Exception {
        assertRejected("Student");
    }

    @Test public void unknownRoleCannotReadReports() throws Exception {
        assertRejected("Other");
    }

    private void assertRejected(String role) throws Exception {
        User user = new User(1, "demo", "hash", "Demo", role, null);
        HttpSession session = (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(), new Class<?>[]{HttpSession.class},
                (p, m, a) -> "getAttribute".equals(m.getName()) ? user : null);
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                (p, m, a) -> {
                    if ("getSession".equals(m.getName())) return session;
                    if ("getParameter".equals(m.getName())) {
                        fail("Unauthorized access must stop before resolving any report or mock data");
                    }
                    return null;
                });
        int[] status = {0};
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class},
                (p, m, a) -> {
                    if ("sendError".equals(m.getName())) status[0] = (Integer) a[0];
                    return null;
                });
        new AnalysisReportServlet().doGet(request, response);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, status[0]);
    }
}
