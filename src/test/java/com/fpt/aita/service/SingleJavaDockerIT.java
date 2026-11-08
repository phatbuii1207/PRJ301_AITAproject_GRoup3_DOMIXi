package com.fpt.aita.service;

import com.fpt.aita.model.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class SingleJavaDockerIT {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private Path student(String text)throws Exception {
        Path p=temp.newFile("123_student_MyBST.java").toPath();Files.write(p,text.getBytes(StandardCharsets.UTF_8));return p;
    }
    private GradingTestCase test(String name,String input,String expected){return new GradingTestCase(name,input,expected,BigDecimal.ONE,true);}
    private GradingConfig config()throws Exception {
        String driver=new String(Files.readAllBytes(Paths.get("docs/grading-driver/Main.java")),StandardCharsets.UTF_8);
        return new GradingConfig("Main",1,Arrays.asList(
            test("empty","height search 7","0\nfalse"),
            test("insert","insert 5 insert 3 insert 7 insert 3 search 3 search 9 inorder height","true\nfalse\n3 5 7\n2"),
            test("delete root","insert 5 insert 3 insert 7 delete 5 inorder search 5 height","3 7\nfalse\n2")),driver);
    }
    @Test public void realBstSingleJavaPassesDriverTests()throws Exception {
        String source=new String(Files.readAllBytes(Paths.get("src/test/resources/grading-driver/MyBST.java")),StandardCharsets.UTF_8);
        GradingReport report=new GradingService().grade(student(source),config());
        assertEquals(report.getCases().isEmpty()?report.getMessage():report.getCases().get(0).getActualOutput(),"ACCEPTED",report.getVerdict());
        assertEquals(new BigDecimal("10.00"),report.getScore());assertEquals(3,report.getPassed());
    }
    @Test public void wrongBstActuallyRunsAndLosesPoints()throws Exception {
        String source="public class MyBST { public void insert(int v){} public void delete(int v){} public boolean search(int v){return false;} public String inorder(){return \"\";} public int height(){return 0;} }";
        GradingReport report=new GradingService().grade(student(source),config());
        assertEquals("PARTIAL",report.getVerdict());assertEquals(new BigDecimal("3.33"),report.getScore());assertEquals(1,report.getPassed());
    }
    @Test public void compilerErrorInStudentSourceIsCompileError()throws Exception {
        GradingReport report=new GradingService().grade(student("public class MyBST { broken Java }"),config());
        assertEquals("COMPILE_ERROR",report.getVerdict());assertEquals(new BigDecimal("0.00"),report.getScore());
    }
    @Test public void runtimeGetsFullFiveSecondsAfterCompilation()throws Exception {
        String source="public class MyBST { public void run() throws Exception { Thread.sleep(4750); System.out.println(\"done\"); } }";
        String driver="public class Main { public static void main(String[] a) throws Exception {new MyBST().run();} }";
        GradingReport report=new GradingService().grade(student(source),new GradingConfig("Main",1,Arrays.asList(test("runtime budget","","done")),driver));
        assertEquals("ACCEPTED",report.getVerdict());assertEquals(new BigDecimal("10.00"),report.getScore());
        assertTrue(report.getCases().get(0).getExecutionTimeMs()>=4750);
    }
    @Test public void infiniteLoopIsRuntimeTimeoutWithDriver()throws Exception {
        String driver="public class Main {public static void main(String[] a){new MyBST().run();}}";
        GradingReport report=new GradingService().grade(student("public class MyBST {public void run(){while(true){}}}"),new GradingConfig("Main",1,Arrays.asList(test("timeout","","")),driver));
        assertEquals("TIME_LIMIT_EXCEEDED",report.getVerdict());assertEquals(new BigDecimal("0.00"),report.getScore());
    }
}
