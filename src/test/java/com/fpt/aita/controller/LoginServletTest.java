package com.fpt.aita.controller;

import com.fpt.aita.dal.*;
import com.fpt.aita.model.User;
import java.lang.reflect.*;
import java.sql.SQLException;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class LoginServletTest {
    private final Map<String,Object> attributes=new HashMap<>();
    private int status=200; private String forwarded,redirect;private boolean sessionCreated;
    private Object defaultValue(Class<?> type) {
        if(type==boolean.class)return false;if(type==int.class)return 0;if(type==long.class)return 0L;return null;
    }
    @SuppressWarnings("unchecked") private <T>T proxy(Class<T> type,InvocationHandler h) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},h);
    }
    private HttpServletRequest request() {
        return proxy(HttpServletRequest.class,(p,m,a)->{
            switch(m.getName()) {
                case "getParameter":return "username".equals(a[0])?"demo":"password".equals(a[0])?"test-password":null;
                case "setAttribute":attributes.put((String)a[0],a[1]);return null;
                case "getContextPath":return "/AITA";
                case "getSession":sessionCreated=true;return proxy(HttpSession.class,(s,n,b)->defaultValue(n.getReturnType()));
                case "getRequestDispatcher":return proxy(RequestDispatcher.class,(d,n,b)->{if(n.getName().equals("forward"))forwarded=(String)a[0];return null;});
                default:return defaultValue(m.getReturnType());
            }
        });
    }
    private HttpServletResponse response() {
        return proxy(HttpServletResponse.class,(p,m,a)->{
            if(m.getName().equals("setStatus"))status=(Integer)a[0];
            if(m.getName().equals("sendRedirect"))redirect=(String)a[0];
            return defaultValue(m.getReturnType());
        });
    }
    private void submit(UserDAO dao)throws Exception {
        new LoginServlet(){@Override protected UserDAO createUserDAO(){return dao;}}.doPost(request(),response());
    }
    @Test public void databaseOutageIs503AndDoesNotClaimPasswordIsWrong()throws Exception {
        submit(new UserDAO(){@Override public User login(String u,String p){throw new DatabaseUnavailableException(new SQLException("fake outage","08006"));}});
        assertEquals(503,status);assertEquals("login.jsp",forwarded);assertFalse(sessionCreated);assertNull(redirect);
        assertTrue(attributes.get("error").toString().contains("cơ sở dữ liệu"));assertFalse(attributes.get("error").toString().contains("Sai tên"));
    }
    @Test public void badCredentialsRemainAValidationError()throws Exception {
        submit(new UserDAO(){@Override public User login(String u,String p){return null;}});
        assertEquals(200,status);assertEquals("login.jsp",forwarded);assertFalse(sessionCreated);assertTrue(attributes.get("error").toString().contains("Sai tên"));
    }
    @Test public void successfulLoginCreatesSessionAndRedirects()throws Exception {
        submit(new UserDAO(){@Override public User login(String u,String p){return new User(1,u,"hash","Demo","Student",null);}});
        assertEquals(200,status);assertEquals("home",redirect);assertTrue(sessionCreated);assertNull(attributes.get("error"));
    }
}
