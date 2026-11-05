package com.fpt.aita.dal;

import com.fpt.aita.model.*;
import com.fpt.aita.service.SubmissionAnalysisService;
import com.fpt.aita.util.PasswordUtil;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

/** Opt-in tests: use a disposable PostgreSQL database whose name contains _test_. */
public class PostgresJdbcIT {
    @Before public void requireDisposableDatabase() throws Exception {
        Assume.assumeTrue("Set AITA_POSTGRES_TESTS=true and a disposable DB_URL", "true".equals(System.getenv("AITA_POSTGRES_TESTS")));
        try(Connection c=DBContext.openConnection()) {
            assertTrue("Refusing non-test database",c.getCatalog().contains("_test_"));
            assertEquals("PostgreSQL",c.getMetaData().getDatabaseProductName());
        }
    }
    @Test public void schemaHasAllFifteenTablesAndNinetyThreeAttributes() throws Exception {
        try(Connection c=DBContext.openConnection(); Statement s=c.createStatement(); ResultSet r=s.executeQuery(
                "SELECT count(DISTINCT table_name),count(*) FROM information_schema.columns WHERE table_schema='public'")) {
            assertTrue(r.next());assertEquals(15,r.getInt(1));assertEquals(93,r.getInt(2));
        }
    }
    @Test public void daoCrudGradingTransactionsAndAstAiRoundTrip() throws Exception {
        int user=0,assignment=0,submission=0;Path source=null;
        try(UserDAO users=new UserDAO(); AssignmentDAO assignments=new AssignmentDAO(); SubmissionDAO submissions=new SubmissionDAO();
                CourseDAO courses=new CourseDAO(); AnalysisReportDAO analysis=new AnalysisReportDAO(); GradingDAO grading=new GradingDAO()) {
            String marker="pg_"+UUID.randomUUID().toString().substring(0,8);
            User u=new User(0,marker,PasswordUtil.sha256Hex("test-password"),"Thử PostgreSQL", "Student",null);
            assertTrue(users.insertUser(u));User saved=users.getUserByUsername(marker.toUpperCase(Locale.ROOT));assertNotNull(saved);user=saved.getUserId();
            assertNotNull(users.login(marker.toUpperCase(Locale.ROOT),"test-password"));assertTrue(users.isUsernameExists(marker.toUpperCase(Locale.ROOT)));
            assertFalse(users.getAllUsers().isEmpty());assertEquals("Thử PostgreSQL",users.getUserById(user).getFullName());
            int course=courses.getAllCourses().get(0).getCourseId();assertNotNull(courses.getCourseById(course));
            assertFalse(courses.searchCourses("prj").isEmpty());assertTrue(users.insertUserClass(user,course));
            assertFalse(courses.getCoursesByUserId(user).isEmpty());assertTrue(grading.isCourseMember(user,course));
            Assignment a=new Assignment();a.setLecturerId(user);a.setTitle(marker);a.setDescription("Unicode tiếng Việt");a.setCourseId(course);
            assertTrue(assignments.insertAssignmentWithCourse(a));
            for(Assignment item:assignments.getAssignmentsByCourse(course))if(marker.equals(item.getTitle()))assignment=item.getAssignmentId();
            assertTrue(assignment>0);a=assignments.getAssignmentById(assignment);assertEquals("Unicode tiếng Việt",a.getDescription());
            a.setTitle(marker+" updated");assertTrue(assignments.updateAssignmentFull(a));assertNotNull(grading.findAssignment(assignment));
            Submission sub=new Submission();sub.setStudentId(user);sub.setAssignmentId(assignment);sub.setFilePath("test/Main.java");sub.setSha256Hash(String.join("", Collections.nCopies(64,"a")));sub.setStatus("Pending");
            assertTrue(submissions.insertSubmission(sub));submission=sub.getSubmissionId();assertTrue(submission>0);
            assertEquals(submission,submissions.getLatestSubmission(user,assignment).getSubmissionId());
            assertFalse(submissions.getSubmissionsByAssignment(assignment).isEmpty());assertFalse(submissions.getStudentSubmissionsForCourse(user,course).isEmpty());
            assertFalse(submissions.getSubmissionsMapByCourse(course).isEmpty());assertNotNull(submissions.getSubmissionById(submission));
            GradingTestCase hidden=new GradingTestCase("Test ẩn tiếng Việt","2 3","5",new BigDecimal("2.50"),true);
            grading.saveConfig(assignment,new GradingConfig("Main",0,Arrays.asList(hidden)));
            assertTrue(grading.loadConfig(assignment).getTests().get(0).isHidden());
            try { grading.saveConfig(assignment,new GradingConfig("Main",0,Arrays.asList(hidden)));fail("stale config accepted"); } catch(IllegalArgumentException expected) { }
            assertEquals(1,grading.loadConfig(assignment).getVersion());
            GradingReport report=new GradingReport("ACCEPTED","Main",1,new BigDecimal("10.00"),"Đúng",Arrays.asList(
                    new GradingCaseResult(1,hidden,GradingCaseResult.Verdict.ACCEPTED,"5","",123)));
            grading.saveReport(submission,report);GradingReport restored=grading.loadReport(submission);
            assertEquals(new BigDecimal("10.00"),restored.getScore());assertEquals("",restored.forStudent().getCases().get(0).getInput());
            grading.saveConfig(assignment,new GradingConfig("Main",1,Arrays.asList(new GradingTestCase("new","","other",BigDecimal.ONE,false))));
            assertEquals("5",grading.loadReport(submission).getCases().get(0).getExpectedOutput());
            grading.saveReport(submission,GradingReport.unavailable("NO_TEST_CASES","Chưa có test"));assertNull(grading.loadReport(submission).getScore());
            source=Files.createTempDirectory("aita-pg-source-").resolve("Main.java");
            Files.write(source,("public class Main { public static int f(int n){ int s=0; for(int i=0;i<n;i++){ for(int j=0;j<n;j++) s++; }return s;} }").getBytes(StandardCharsets.UTF_8));
            assertTrue(new SubmissionAnalysisService().analyze(submission,source));assertTrue(analysis.hasReport(submission));
            assertFalse(analysis.getIssuesBySubmissionId(submission).isEmpty());
            try(AiOptimizationDAO ai=new AiOptimizationDAO()) {
                assertFalse(ai.getBySubmissionId(submission).isEmpty());AiOptimization o=ai.getBySubmissionId(submission).get(0);
                assertNotNull(ai.getByMethodId(o.getMethodId()));assertNotNull(ai.findByOriginalCode(o.getOriginalCode()));
                assertNotNull(o.getComplexityAfter());
            }
            assertNotNull(analysis.getReportsStatusMapByCourse(course));grading.updateStatus(submission,"Completed");
            assertEquals("Completed",grading.findSubmission(submission).getStatus());
        } finally {
            try(Connection c=DBContext.openConnection();Statement s=c.createStatement()) {
                if(submission>0){s.executeUpdate("DELETE FROM \"ISSUES\" WHERE file_id IN (SELECT file_id FROM \"SOURCE_FILES\" WHERE submission_id="+submission+")");s.executeUpdate("DELETE FROM \"SUBMISSIONS\" WHERE submission_id="+submission);}
                if(assignment>0)s.executeUpdate("DELETE FROM \"ASSIGNMENTS\" WHERE assignment_id="+assignment);
                if(user>0)s.executeUpdate("DELETE FROM \"USERS\" WHERE user_id="+user);
            }
            if(source!=null){Files.deleteIfExists(source);Files.deleteIfExists(source.getParent());}
        }
    }
    @Test public void concurrentLoginClosesAllClientConnections() throws Exception {
        try(Connection observer=DBContext.openConnection()) {
            int baseline=connectionCount(observer);UserDAO shared=new UserDAO();
            java.util.concurrent.ExecutorService workers=java.util.concurrent.Executors.newFixedThreadPool(6);
            try {
                List<java.util.concurrent.Future<Boolean>> jobs=new ArrayList<>();
                for(int n=0;n<48;n++)jobs.add(workers.submit(()->shared.login("gv_namhn","123456")!=null));
                for(java.util.concurrent.Future<Boolean> job:jobs)assertTrue(job.get(30,java.util.concurrent.TimeUnit.SECONDS));
            } finally {workers.shutdownNow();}
            for(int n=0;n<30 && connectionCount(observer)>baseline;n++)Thread.sleep(50);
            assertEquals("Login left database clients behind",baseline,connectionCount(observer));
        }
    }
    private int connectionCount(Connection observer)throws Exception {
        try(Statement s=observer.createStatement();ResultSet r=s.executeQuery(
                "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND application_name='AITA'")) {
            r.next();return r.getInt(1);
        }
    }

}
