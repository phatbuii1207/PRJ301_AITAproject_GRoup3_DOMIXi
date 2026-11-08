package com.fpt.aita.filter;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class AuthFilterUploadsTest {
    private AuthFilter filter;
    private int responseStatus;
    private String responseMessage;
    private boolean chainCalled;

    @Before
    public void setUp() {
        filter = new AuthFilter();
        responseStatus = 200;
        responseMessage = null;
        chainCalled = false;
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private HttpServletRequest createRequest(String uri, String contextPath, String servletPath, String pathInfo) {
        return proxy(HttpServletRequest.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("getRequestURI".equals(name)) return uri;
            if ("getContextPath".equals(name)) return contextPath;
            if ("getServletPath".equals(name)) return servletPath;
            if ("getPathInfo".equals(name)) return pathInfo;
            if ("getCharacterEncoding".equals(name)) return "UTF-8";
            if ("getSession".equals(name)) return null;
            return null;
        });
    }

    private HttpServletResponse createResponse() {
        return proxy(HttpServletResponse.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("sendError".equals(name)) {
                responseStatus = (Integer) args[0];
                if (args.length > 1) {
                    responseMessage = (String) args[1];
                }
                return null;
            }
            return null;
        });
    }

    private FilterChain createFilterChain() {
        return new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) {
                chainCalled = true;
            }
        };
    }

    @Test
    public void testDirectUploadsFolderBlocked() throws Exception {
        HttpServletRequest req = createRequest("/app/uploads", "/app", "/uploads", null);
        HttpServletResponse resp = createResponse();
        filter.doFilter(req, resp, createFilterChain());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, responseStatus);
        assertFalse(chainCalled);
    }

    @Test
    public void testDirectUploadSubmissionsJavaFileBlocked() throws Exception {
        HttpServletRequest req = createRequest("/app/uploads/submissions/1234_student_Main.java", "/app", "/uploads/submissions/1234_student_Main.java", null);
        HttpServletResponse resp = createResponse();
        filter.doFilter(req, resp, createFilterChain());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, responseStatus);
        assertFalse(chainCalled);
    }

    @Test
    public void testDirectUploadSubmissionsZipFileBlocked() throws Exception {
        HttpServletRequest req = createRequest("/app/uploads/submissions/project.zip", "/app", "/uploads/submissions/project.zip", null);
        HttpServletResponse resp = createResponse();
        filter.doFilter(req, resp, createFilterChain());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, responseStatus);
        assertFalse(chainCalled);
    }

    @Test
    public void testMatrixParametersBlocked() throws Exception {
        // Ví dụ: /uploads;jsessionid=xyz/submissions/test.java
        HttpServletRequest req = createRequest("/app/uploads;jsessionid=123/submissions/test.java", "/app", "/uploads", "/submissions/test.java");
        HttpServletResponse resp = createResponse();
        filter.doFilter(req, resp, createFilterChain());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, responseStatus);
        assertFalse(chainCalled);
    }

    @Test
    public void testClaudeRequestedMatrixParamAndUppercaseBlocked() throws Exception {
        // Yêu cầu Claude review (2): /app/uploads;x=1/submissions/test.java và /app/UPLOADS/submissions/test.java
        responseStatus = 200;
        chainCalled = false;
        HttpServletRequest req1 = createRequest("/app/uploads;x=1/submissions/test.java", "/app", "/uploads", "/submissions/test.java");
        HttpServletResponse resp1 = createResponse();
        filter.doFilter(req1, resp1, createFilterChain());
        assertEquals("Matrix param x=1 must be 403", HttpServletResponse.SC_FORBIDDEN, responseStatus);
        assertFalse(chainCalled);

        responseStatus = 200;
        chainCalled = false;
        HttpServletRequest req2 = createRequest("/app/UPLOADS/submissions/test.java", "/app", "/UPLOADS/submissions/test.java", null);
        HttpServletResponse resp2 = createResponse();
        filter.doFilter(req2, resp2, createFilterChain());
        assertEquals("Uppercase UPLOADS must be 403", HttpServletResponse.SC_FORBIDDEN, responseStatus);
        assertFalse(chainCalled);
    }

    @Test
    public void testUrlEncodedPathsBlocked() throws Exception {
        String[] encodedUris = {
            "/app/upl%6fads/submissions/test.java",
            "/app/%75%70%6c%6f%61%64%73/submissions/test.java",
            "/app/uploads/%73%75%62%6d%69%73%73%69%6f%6e%73/test.zip"
        };
        for (String uri : encodedUris) {
            responseStatus = 200;
            chainCalled = false;
            HttpServletRequest req = createRequest(uri, "/app", "/uploads/submissions/test.java", null);
            HttpServletResponse resp = createResponse();
            filter.doFilter(req, resp, createFilterChain());

            assertEquals("Should block encoded URI: " + uri, HttpServletResponse.SC_FORBIDDEN, responseStatus);
            assertFalse(chainCalled);
        }
    }

    @Test
    public void testDotSegmentTraversalBlocked() throws Exception {
        // Ví dụ: /assets/../uploads/submissions/test.java
        HttpServletRequest req = createRequest("/app/assets/../uploads/submissions/test.java", "/app", "/uploads/submissions/test.java", null);
        HttpServletResponse resp = createResponse();
        filter.doFilter(req, resp, createFilterChain());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, responseStatus);
        assertFalse(chainCalled);
    }

    @Test
    public void testCaseInsensitiveAndDoubleSlashUploadsBlocked() throws Exception {
        String[] attacks = {
            "/app/UPLOADS/secret.java",
            "/app/Uploads/submissions/Main.java",
            "/app//uploads/submissions/Main.java",
            "/app///uploads/submissions/Main.zip"
        };
        for (String uri : attacks) {
            responseStatus = 200;
            chainCalled = false;
            HttpServletRequest req = createRequest(uri, "/app", "/uploads/submissions/Main.java", null);
            HttpServletResponse resp = createResponse();
            filter.doFilter(req, resp, createFilterChain());

            assertEquals("Should block URI: " + uri, HttpServletResponse.SC_FORBIDDEN, responseStatus);
            assertFalse("Chain should not be called for: " + uri, chainCalled);
        }
    }

    @Test
    public void testNormalStaticAssetsAllowed() throws Exception {
        HttpServletRequest req = createRequest("/app/css/style.css", "/app", "/css/style.css", null);
        HttpServletResponse resp = createResponse();
        filter.doFilter(req, resp, createFilterChain());

        assertEquals(200, responseStatus);
        assertTrue("Normal static asset should pass through", chainCalled);
    }
}
