package com.fpt.aita.controller;

import com.fpt.aita.dal.*;
import com.fpt.aita.model.*;
import java.lang.reflect.*;
import java.sql.Timestamp;
import java.util.*;
import javax.servlet.http.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CourseDetailAuthorizationTest {
    private int status=200, writes=0;
    private String redirect;
    private List<Integer> allowed=Collections.singletonList(1);
    private String role="Lecturer";
    private String savedRubric;
    private Object empty(Class<?> t) { if(t==boolean.class)return false;if(t==int.class)return 0;if(t==long.class)return 0L;return null; }
    @SuppressWarnings("unchecked") private <T>T proxy(Class<T> type, InvocationHandler h) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},h);
    }
    private CourseDetailServlet servlet() {
        AssignmentDAO assignments=new AssignmentDAO(){
            @Override public Assignment getAssignmentById(int id) { Assignment a=new Assignment();a.setAssignmentId(id);a.setCourseId(2);return a; }
            @Override public boolean deleteAssignment(int id){writes++;return true;}
            @Override public boolean updateDeadline(int id,Timestamp d){writes++;return true;}
            @Override public boolean updateAssignmentFull(Assignment a){writes++;savedRubric=a.getRubricType();return true;}
            @Override public boolean insertAssignmentWithCourse(Assignment a){writes++;a.setAssignmentId(12);savedRubric=a.getRubricType();return true;}
            @Override public boolean updateRubricType(int id,String rubric){assertEquals(12,id);savedRubric=rubric;return true;}
        };
        SubmissionDAO submissions=new SubmissionDAO(){
            @Override public Submission getSubmissionById(int id){Submission s=new Submission();s.setAssignmentId(12);s.setStudentId(99);s.setFilePath("/uploads/private.java");return s;}
        };
        UserDAO users=new UserDAO(){@Override public List<Integer> getClassIdsByUserId(int id){return allowed;}};
        return new CourseDetailServlet(assignments,submissions,users);
    }
    private HttpServletRequest request(Map<String,String> params) {
        User user=new User(5,"teacher","hash","Teacher",role,null);
        HttpSession session=proxy(HttpSession.class,(p,m,a)->"getAttribute".equals(m.getName())?user:empty(m.getReturnType()));
        return proxy(HttpServletRequest.class,(p,m,a)->{
            if("getParameter".equals(m.getName()))return params.get(a[0]);
            if("getSession".equals(m.getName()))return session;
            if("getServletContext".equals(m.getName()))throw new AssertionError("Denied download must not touch storage");
            return empty(m.getReturnType());
        });
    }
    private HttpServletResponse response(){return proxy(HttpServletResponse.class,(p,m,a)->{
        if("sendError".equals(m.getName()))status=(Integer)a[0];
        if("sendRedirect".equals(m.getName()))redirect=(String)a[0];
        return empty(m.getReturnType());
    });}
    private Map<String,String> params(String action){Map<String,String> p=new HashMap<String,String>();p.put("action",action);p.put("assignmentId","12");p.put("courseId","1");p.put("title","Example");p.put("deadline","2030-01-01T10:00");p.put("rubric_type","BST");return p;}
    @Test public void forgedCourseCannotAuthorizeAnyAssignmentMutation()throws Exception {
        for(String action:Arrays.asList("update_deadline","delete_assignment","edit_assignment")){
            status=200;servlet().doPost(request(params(action)),response());assertEquals(action,403,status);assertEquals(0,writes);
        }
    }
    @Test public void assignmentOnlyRequestUsesPersistedCourse()throws Exception {
        Map<String,String> p=params("delete_assignment");p.remove("courseId");
        servlet().doPost(request(p),response());assertEquals(403,status);assertEquals(0,writes);
    }
    @Test public void noAssignedCoursesDeniesCreate()throws Exception {
        allowed=Collections.emptyList();servlet().doPost(request(params("create_assignment")),response());assertEquals(403,status);assertEquals(0,writes);
    }
    @Test public void studentCannotManageAssignments()throws Exception {
        role="Student";servlet().doPost(request(params("delete_assignment")),response());assertEquals(403,status);assertEquals(0,writes);
    }
    @Test public void unrelatedLecturerCannotDownload()throws Exception {
        Map<String,String> p=params("download_submission");p.put("submissionId","20");servlet().doGet(request(p),response());assertEquals(403,status);
    }
    @Test public void assignedLecturerCanCreateAndEditRubric()throws Exception {
        servlet().doPost(request(params("create_assignment")),response());assertEquals(200,status);assertEquals("BST",savedRubric);assertEquals(1,writes);
        allowed=Collections.singletonList(2);servlet().doPost(request(params("edit_assignment")),response());assertEquals(200,status);assertEquals(2,writes);assertEquals("course?id=2",redirect);
    }
    @Test public void unknownRubricRejectedBeforeWrite()throws Exception {
        Map<String,String> p=params("create_assignment");p.put("rubric_type","unexpected");servlet().doPost(request(p),response());assertEquals(400,status);assertEquals(0,writes);
    }
    @Test public void downloadSymlinkSubmissionReturns403Forbidden() throws Exception {
        allowed = Collections.singletonList(2);
        role = "Lecturer";
        java.nio.file.Path tempUpload = java.nio.file.Files.createTempDirectory("aita-symlink-test-");
        java.nio.file.Path realFile = java.nio.file.Files.createTempFile("aita-real-", ".java");
        java.nio.file.Files.write(realFile, "public class SymlinkTest {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String symlinkName = "123_student_symlink.java";
        java.nio.file.Path symlink = tempUpload.resolve(symlinkName);
        try {
            java.nio.file.Files.createSymbolicLink(symlink, realFile);
            com.fpt.aita.util.UploadStorage.init(tempUpload, null);
            AssignmentDAO assignments = new AssignmentDAO() {
                @Override public Assignment getAssignmentById(int id) { Assignment a = new Assignment(); a.setAssignmentId(id); a.setCourseId(2); return a; }
            };
            SubmissionDAO submissions = new SubmissionDAO() {
                @Override public Submission getSubmissionById(int id) {
                    Submission s = new Submission();
                    s.setAssignmentId(12);
                    s.setStudentId(99);
                    s.setFilePath("/uploads/submissions/" + symlinkName);
                    return s;
                }
            };
            UserDAO users = new UserDAO() { @Override public List<Integer> getClassIdsByUserId(int id) { return allowed; } };
            CourseDetailServlet s = new CourseDetailServlet(assignments, submissions, users);
            Map<String, String> p = params("download_submission");
            p.put("submissionId", "99");
            status = 200;
            s.doGet(request(p), response());
            assertEquals(403, status);
        } catch (UnsupportedOperationException ignored) {
        } finally {
            java.nio.file.Files.deleteIfExists(symlink);
            java.nio.file.Files.deleteIfExists(realFile);
            java.nio.file.Files.deleteIfExists(tempUpload);
            com.fpt.aita.util.UploadStorage.init(com.fpt.aita.util.UploadStorage.resolveDefaultUploadDir(), null);
        }
    }
}
