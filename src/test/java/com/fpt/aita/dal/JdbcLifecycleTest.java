package com.fpt.aita.dal;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import org.junit.*;
import static org.junit.Assert.*;

/** Simulates a dropped connection without touching a real database or credentials. */
public class JdbcLifecycleTest {
    private List<Driver> original;private Driver fake;private int opened,closed,statementsClosed;private boolean outage,queryOutage;
    private Object defaultValue(Class<?> t){if(t==boolean.class)return false;if(t==int.class)return 0;if(t==long.class)return 0L;return null;}
    @SuppressWarnings("unchecked")private <T>T proxy(Class<T> t,InvocationHandler h){return (T)Proxy.newProxyInstance(t.getClassLoader(),new Class<?>[]{t},h);}
    @Before public void useFakeDriver()throws Exception {
        Class.forName("org.postgresql.Driver");original=Collections.list(DriverManager.getDrivers());
        for(Driver d:original)DriverManager.deregisterDriver(d);
        fake=new Driver(){
            public Connection connect(String u,Properties p)throws SQLException {
                if(outage)throw new SQLException("simulated disconnect","08006");opened++;
                return proxy(Connection.class,(c,m,a)->{
                    if(m.getName().equals("close")){closed++;return null;}
                    if(m.getName().equals("prepareStatement"))return proxy(PreparedStatement.class,(s,n,b)->{
                        if(n.getName().equals("close")){statementsClosed++;return null;}
                        if(n.getName().equals("executeQuery")){if(queryOutage)throw new SQLException("simulated query failure","08006");return proxy(ResultSet.class,(r,k,v)->defaultValue(k.getReturnType()));}
                        return defaultValue(n.getReturnType());});
                    return defaultValue(m.getReturnType());
                });
            }
            public boolean acceptsURL(String u){return u.startsWith("jdbc:postgresql:");}
            public DriverPropertyInfo[] getPropertyInfo(String u,Properties p){return new DriverPropertyInfo[0];}
            public int getMajorVersion(){return 1;}public int getMinorVersion(){return 0;}public boolean jdbcCompliant(){return false;}public Logger getParentLogger(){return Logger.getGlobal();}
        };DriverManager.registerDriver(fake);
        System.setProperty("DB_URL","jdbc:postgresql://fake.invalid/test");System.setProperty("DB_PASSWORD","test-only-not-a-real-password");
    }
    @After public void restoreDrivers()throws Exception {
        DriverManager.deregisterDriver(fake);for(Driver d:original)DriverManager.registerDriver(d);
        System.clearProperty("DB_URL");System.clearProperty("DB_PASSWORD");
    }
    @Test public void constructingReusableDaosDoesNotReserveDatabaseSessions(){
        new UserDAO();new CourseDAO();new AssignmentDAO();new SubmissionDAO();new AnalysisReportDAO();new AiOptimizationDAO();assertEquals(0,opened);
    }
    @Test public void repeatedLoginClosesConnectionAndStatementEveryTime(){
        UserDAO users=new UserDAO();for(int i=0;i<50;i++)assertNull(users.login("demo","password"));
        assertEquals(50,opened);assertEquals(opened,closed);assertEquals(opened,statementsClosed);
    }
    @Test public void sameDaoRecoversAfterTransientOutageWithoutRestart(){
        UserDAO users=new UserDAO();outage=true;
        try{users.login("demo","password");fail("outage treated as bad password");}catch(DatabaseUnavailableException expected){}
        outage=false;assertNull(users.login("demo","password"));assertEquals(1,opened);assertEquals(1,closed);
    }
    @Test public void queryFailureStillClosesResourcesAndNextRequestRecovers(){
        UserDAO users=new UserDAO();queryOutage=true;
        try{users.login("demo","password");fail("query failure treated as invalid account");}catch(DatabaseUnavailableException expected){}
        assertEquals(1,closed);assertEquals(1,statementsClosed);
        queryOutage=false;assertNull(users.login("demo","password"));assertEquals(2,closed);assertEquals(2,statementsClosed);
    }

}
