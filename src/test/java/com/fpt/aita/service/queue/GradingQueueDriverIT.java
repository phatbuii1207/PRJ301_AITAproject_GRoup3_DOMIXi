package com.fpt.aita.service.queue;

import com.fpt.aita.dal.*;
import com.fpt.aita.model.*;
import com.fpt.aita.util.UploadStorage;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

/** Full queue + file resolver + PostgreSQL + Docker + AST/mock AI; disposable DB only. */
public class GradingQueueDriverIT {
    @Test public void singleJavaQueueGradesAndPreservesAstOnMissingDriverAndGradingFailure()throws Exception {
        Assume.assumeTrue("Explicit disposable database required","true".equals(System.getenv("AITA_POSTGRES_TESTS")));
        try(Connection c=DBContext.openConnection()){assertTrue(c.getCatalog().contains("_test_"));}
        Path root=Files.createTempDirectory("aita-queue-driver-");
        UploadStorage.init(root,null);GradingQueueManager.initialize(root);
        GradingQueueManager queue=GradingQueueManager.getInstance();
        List<Integer> submissions=new ArrayList<Integer>();int assignment=0;
        try(GradingDAO grading=new GradingDAO();AssignmentDAO assignments=new AssignmentDAO();SubmissionDAO files=new SubmissionDAO();AnalysisReportDAO reports=new AnalysisReportDAO()) {
            Assignment a=new Assignment();a.setLecturerId(1);a.setCourseId(1);a.setTitle("queue_driver_"+UUID.randomUUID());
            assertTrue(assignments.insertAssignmentWithCourse(a));assignment=a.getAssignmentId();
            GradingTestCase test=new GradingTestCase("driver run","","7",BigDecimal.ONE,false);
            String driver="public class Main {public static void main(String[] a){System.out.println(new MyBST().height());}}";
            grading.saveConfig(assignment,new GradingConfig("Main",0,Arrays.asList(test),driver));
            int good=submit(files,root,assignment,"public class MyBST {public int height(){return 7;}}",submissions);
            await(queue,good);assertEquals(new BigDecimal("10.00"),grading.loadReport(good).getScore());assertTrue(reports.hasReport(good));
            // No driver: no functional score, but AST is retained.
            grading.saveConfig(assignment,new GradingConfig("Main",1,Arrays.asList(test)));
            int missing=submit(files,root,assignment,"public class MyBST {public int height(){return 0;}}",submissions);
            await(queue,missing);assertEquals("NOT_CONFIGURED",grading.loadReport(missing).getVerdict());assertNull(grading.loadReport(missing).getScore());assertTrue(reports.hasReport(missing));
            // Teacher driver is protected; even a rejected override still gets separate AST analysis.
            grading.saveConfig(assignment,new GradingConfig("Main",2,Arrays.asList(test),driver));
            int override=submit(files,root,assignment,"public class Main {public static void main(String[] a){System.out.println(7);}}",submissions);
            await(queue,override);assertEquals("INVALID_SUBMISSION",grading.loadReport(override).getVerdict());assertTrue(reports.hasReport(override));
        } finally {
            queue.shutdown();
            try(Connection c=DBContext.openConnection();Statement s=c.createStatement()){
                for(int id:submissions){s.executeUpdate("DELETE FROM \"ISSUES\" WHERE file_id IN (SELECT file_id FROM \"SOURCE_FILES\" WHERE submission_id="+id+")");s.executeUpdate("DELETE FROM \"SUBMISSIONS\" WHERE submission_id="+id);}
                if(assignment>0)s.executeUpdate("DELETE FROM \"ASSIGNMENTS\" WHERE assignment_id="+assignment);
            }
            try(java.util.stream.Stream<Path> paths=Files.walk(root)){for(Path p:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator)Files.deleteIfExists(p);}
        }
    }
    private int submit(SubmissionDAO dao,Path root,int assignment,String code,List<Integer> ids)throws Exception {
        byte[] bytes=code.getBytes(StandardCharsets.UTF_8);String name="upload_"+UUID.randomUUID()+".java";Files.write(root.resolve(name),bytes);
        StringBuilder hex=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))hex.append(String.format("%02x",b));
        Submission s=new Submission();s.setStudentId(4);s.setAssignmentId(assignment);s.setFilePath("/uploads/submissions/"+name);s.setSha256Hash(hex.toString());s.setStatus("Pending");
        assertTrue(dao.insertSubmission(s));ids.add(s.getSubmissionId());return s.getSubmissionId();
    }
    private void await(GradingQueueManager queue,int id)throws Exception {
        assertTrue(queue.enqueue(id));long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(35);
        while(queue.isActive(id) && System.nanoTime()<deadline)Thread.sleep(50);
        assertFalse("Queue did not finish: "+queue.getStatusDetail(id),queue.isActive(id));
    }
}
