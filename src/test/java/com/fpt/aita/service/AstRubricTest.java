package com.fpt.aita.service;

import com.fpt.aita.dal.AnalysisReportDAO;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AstRubricTest {
    static class Reports extends AnalysisReportDAO {
        String rubric = "NONE";
        boolean missingRules;
        List<Integer> rules = new ArrayList<Integer>();
        int count, score;
        @Override public String getRubricTypeBySubmissionId(int id) { return rubric; }
        @Override public void cleanOldAnalysisData(int id) { }
        @Override public int getRuleIdByCode(String code) {
            if (missingRules) return -1;
            return "LL01".equals(code) ? 101 : "LL02".equals(code) ? 102 : 1;
        }
        @Override public int insertSourceFile(int id, String name, int loc) { return 1; }
        @Override public int insertMethod(int file, String name, int start, int end, int depth, int cc, String complexity, boolean recursive) { return 2; }
        @Override public boolean insertIssue(int file, Integer method, int rule, int line, String severity, String message, String explanation) {
            assertTrue("Invalid rule must never reach DAO", rule > 0);
            rules.add(rule); return true;
        }
        @Override public int insertAnalysisReport(int id, int quality, int issues) { score=quality; count=issues; return 1; }
    }
    private Reports analyze(String rubric, String code, boolean missing) throws Exception {
        Reports dao = new Reports(); dao.rubric=rubric; dao.missingRules=missing;
        File file=File.createTempFile("MyBST", ".java");
        try {
            Files.write(file.toPath(), code.getBytes(StandardCharsets.UTF_8));
            assertTrue(new AstAnalyzerService(dao).analyzeAndSave(7,file));
            assertEquals(dao.rules.size(), dao.count);
            return dao;
        } finally { Files.deleteIfExists(file.toPath()); }
    }
    @Test public void bstAndNoneAllowLibraryImportAndEmptyMethods() throws Exception {
        String code="import java.util.LinkedList; class MyBST { void insert() {} }";
        for (String rubric: Arrays.asList("NONE","BST")) {
            Reports dao=analyze(rubric,code,false);
            assertTrue(dao.rules.isEmpty()); assertEquals(100,dao.score);
        }
    }
    @Test public void linkedListFindsImportAndEmptyBody() throws Exception {
        Reports dao=analyze("LINKEDLIST","import java.util.LinkedList; class MyBST { void insert() {} }",false);
        assertEquals(Arrays.asList(101,102),dao.rules);
    }
    @Test public void commentsAndStringsAreNotUsage() throws Exception {
        String code="// import java.util.LinkedList; new LinkedList()\nclass Example { String text = \"java.util.LinkedList new LinkedList()\"; int size() { return 1; } }";
        assertTrue(analyze("LINKEDLIST",code,false).rules.isEmpty());
    }
    @Test public void creationUsesAstIncludingMultilineAndQualifiedTypes() throws Exception {
        String code="class Example { Object a = new\n java.util.LinkedList<Integer>(); Object b = new LinkedList<String>(); }";
        assertEquals(Arrays.asList(101,101),analyze("LINKEDLIST",code,false).rules);
    }
    @Test public void studentDeclaredOrOtherImportedTypeIsNotJdkLinkedList() throws Exception {
        assertTrue(analyze("LINKEDLIST","class LinkedList {} class Example { Object list = new LinkedList(); }",false).rules.isEmpty());
        assertTrue(analyze("LINKEDLIST","import student.LinkedList; class Example { Object list = new LinkedList(); }",false).rules.isEmpty());
    }
    @Test public void missingRuleIdsDoNotCreateIssuesOrPenalty() throws Exception {
        Reports dao=analyze("LINKEDLIST","import java.util.LinkedList; class Example { void Bad_name() {} }",true);
        assertTrue(dao.rules.isEmpty()); assertEquals(100,dao.score);
        assertTrue(analyze("LINKEDLIST","this is invalid Java",true).rules.isEmpty());
    }
}
