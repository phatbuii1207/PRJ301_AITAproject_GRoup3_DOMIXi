package com.fpt.aita.service;

import com.fpt.aita.model.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;
import java.util.zip.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class TrustedDriverTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private final String driver="public class Main { public static void main(String[] a) { System.out.println(new MyBST().height()); } }";
    private GradingConfig config(String main, String source) {
        return new GradingConfig(main,1,Arrays.asList(new GradingTestCase("test","","0",BigDecimal.ONE,false)),source);
    }
    private Path source(String name,String code)throws Exception {
        Path p=temp.newFile(name).toPath();Files.write(p,code.getBytes(StandardCharsets.UTF_8));return p;
    }
    @Test public void packagesServerPrefixedSourceWithProtectedDriverAndDeletesBundle()throws Exception {
        Path student=source("123_student_MyBST.java","public class MyBST { public int height(){return 0;} }");
        final Path[] bundle={null};
        GradingReport r=new GradingService((p,m,i)->{
            bundle[0]=p;
            try(ZipFile z=new ZipFile(p.toFile())){
                assertEquals(2,z.size());assertNotNull(z.getEntry("Main.java"));assertNotNull(z.getEntry("MyBST.java"));
                assertEquals(driver,new String(read(z,z.getEntry("Main.java")),StandardCharsets.UTF_8));
            }catch(Exception ex){throw new AssertionError(ex);}
            return new SandboxResult(SandboxResult.Status.SUCCESS,"0",1);
        }).grade(student,config("Main",driver));
        assertEquals("ACCEPTED",r.getVerdict());assertFalse(Files.exists(bundle[0]));assertTrue(Files.exists(student));
    }
    private byte[] read(ZipFile zip,ZipEntry entry)throws Exception {
        try(java.io.InputStream in=zip.getInputStream(entry);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){
            byte[] b=new byte[1024];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();
        }
    }
    @Test public void rejectsSameClassAndPackageEvenWithDifferentUploadName()throws Exception {
        for(String code:Arrays.asList("class Main {}", "class MyBST {} class Main {}", "class Main$Hack {}")){
            GradingReport r=new GradingService((p,m,i)->{throw new AssertionError("Cannot execute impersonating source");})
                    .grade(source(UUID.randomUUID()+".java",code),config("Main",driver));
            assertEquals("INVALID_SUBMISSION",r.getVerdict());assertNull(r.getScore());
        }
    }
    @Test public void protectsPackagedDriverAndHelperType()throws Exception {
        String d="package exam; public class Main {public static void main(String[] a){}} class Helper {}";
        Path p=source("s.java","package exam; class Helper {}");
        assertEquals("INVALID_SUBMISSION",new GradingService((a,b,c)->{throw new AssertionError();}).grade(p,config("exam.Main",d)).getVerdict());
    }
    @Test public void noDriverMeansNotConfiguredWithoutExecution()throws Exception {
        GradingReport r=new GradingService((a,b,c)->{throw new AssertionError();})
                .grade(Paths.get("upload.java"),config("Main",""));
        assertEquals("NOT_CONFIGURED",r.getVerdict());assertNull(r.getScore());
    }
    @Test public void syntaxErrorsReachCompilerWithoutReplacingDriver()throws Exception {
        Path p=source("Main.java","public class Main { broken Java }");
        GradingReport r=new GradingService((a,b,c)->new SandboxResult(SandboxResult.Status.COMPILE_ERROR,"error",1)).grade(p,config("Main",driver));
        assertEquals("COMPILE_ERROR",r.getVerdict());
    }
    @Test(expected=IllegalArgumentException.class) public void driverMustDeclareConfiguredEntryPoint(){ config("Other",driver); }
    @Test(expected=IllegalArgumentException.class) public void intVarargsIsNotAnEntryPoint(){ config("Main","public class Main {public static void main(int... a){}}"); }
    @Test(expected=IllegalArgumentException.class) public void driverMustBeValidJava(){ config("Main","not Java"); }
    @Test(expected=IllegalArgumentException.class) public void rejectsOversizedDriver(){ config("Main",String.join("",Collections.nCopies(65537,"x"))); }
    @Test public void rejectsSymlinkSubmission()throws Exception {
        Path real=source("MyBST.java","class MyBST{}");Path link=temp.getRoot().toPath().resolve("link.java");Files.createSymbolicLink(link,real);
        assertEquals("INVALID_SUBMISSION",new GradingService((a,b,c)->{throw new AssertionError();}).grade(link,config("Main",driver)).getVerdict());
    }
}
