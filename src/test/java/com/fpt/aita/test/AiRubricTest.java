package com.fpt.aita.test;

import com.fpt.aita.ai.*;
import com.fpt.aita.dal.*;
import com.fpt.aita.dal.AiOptimizationDAO.MethodWithIssuesDTO;
import com.fpt.aita.model.AiOptimization;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AiRubricTest {
    private String rubric="NONE";
    private int calls, quality=-1;
    private boolean rejectCacheInsert;
    private final Map<Integer,AiOptimization> stored=new HashMap<Integer,AiOptimization>();
    private final List<MethodWithIssuesDTO> methods=new ArrayList<MethodWithIssuesDTO>();
    private final AiOptimizationDAO dao=new AiOptimizationDAO(){
        @Override public List<MethodWithIssuesDTO> getMethodsWithIssues(int id){return methods;}
        @Override public AiOptimization getByMethodId(int id){return stored.get(id);}
        @Override public AiOptimization findByOriginalCode(String code,String type){
            for(AiOptimization o:stored.values()) if(code.equals(o.getOriginalCode()) && type.equals(o.getRubricType()))return o;
            return null;
        }
        @Override public boolean insertOptimization(AiOptimization o){if(rejectCacheInsert && !stored.isEmpty())return false;stored.put(o.getMethodId(),o);return true;}
        @Override public boolean updateAnalysisReportQualityScore(int id,int score){quality=score;return true;}
    };
    private MethodWithIssuesDTO method(int id,String name,String complexity){
        MethodWithIssuesDTO m=new MethodWithIssuesDTO();m.setMethodId(id);m.setMethodName(name);m.setFileName("Example.java");m.setEstimatedComplexity(complexity);methods.add(m);return m;
    }
    private AiAnalysisService service(){
        AiClient client=req->{calls++;assertEquals(rubric,req.getRubricType());return AiResult.success("Review","code","O(1)",PromptBuilder.getMaxPoints(req.getMethodName(),rubric)*0.6);};
        return new AiAnalysisService(client,new AiConfig(),(id,file,start,end)->"int search() { return 1; }",dao,
                new AnalysisReportDAO(){@Override public String getRubricTypeBySubmissionId(int id){return rubric;}});
    }
    @Test public void neutralAndBstPromptsDoNotImposeLinkedList(){
        for(String type:Arrays.asList("NONE","BST")){
            AiRequest req=new AiRequest();req.setRubricType(type);req.setMethodName("search");req.setCode("int search() { return 1; }");
            String prompt=PromptBuilder.buildSystemInstruction(type)+PromptBuilder.buildUserPrompt(req);
            assertFalse(prompt.toLowerCase(Locale.ROOT).contains("linkedlist"));
            assertFalse(prompt.contains("\"score\": 0.8"));
            assertTrue(prompt.contains("\"score\": 80"));
            assertEquals(100,PromptBuilder.getMaxPoints("search",type),0.001);
            if("BST".equals(type)){assertTrue(prompt.contains("Core Logic (50%)"));assertTrue(prompt.contains("Quality (25%)"));}
        }
        assertTrue(PromptBuilder.buildSystemInstruction("LINKEDLIST").contains("java.util.LinkedList"));
        assertTrue(PromptBuilder.buildSystemInstruction("LINKEDLIST").contains("\"score\": 0.8"));
        assertEquals(1.5,PromptBuilder.getMaxPoints("search","LINKEDLIST"),0.001);
        assertFalse(PromptBuilder.buildSystemInstruction().contains("LinkedList"));
    }
    @Test public void rubricMethodsAreNotAppliedToNone(){
        method(1,"search","O(1)");assertEquals(0,service().analyzeSubmission(7).getTotalMethods());assertEquals(0,calls);
        rubric="LINKEDLIST";assertEquals(1,service().analyzeSubmission(7).getAnalyzed());assertEquals(60,quality);
    }
    @Test public void bstCoreMethodsAreSelected(){rubric="BST";method(1,"insert","O(1)");assertEquals(1,service().analyzeSubmission(7).getAnalyzed());assertEquals(60,quality);}
    @Test public void freshCacheAndExistingResultsKeepSameScore(){
        rubric="LINKEDLIST";method(1,"search","O(1)");method(2,"search","O(1)");
        AiRunSummary first=service().analyzeSubmission(7);assertEquals(1,first.getAnalyzed());assertEquals(1,first.getFromCache());assertEquals(60,quality);assertEquals(1,calls);
        quality=-1;AiRunSummary again=service().analyzeSubmission(7);assertEquals(2,again.getSkipped());assertEquals(60,quality);assertEquals(1,calls);
    }
    @Test public void cachePointsParticipateInMixedWeightedScore() {
        rubric="LINKEDLIST";
        AiOptimization cached=new AiOptimization(90,"int search() { return 1; }","code","cached");
        cached.setRubricType(rubric);cached.setScore(0.3);stored.put(90,cached);
        method(1,"search","O(1)");method(2,"sort","O(1)");
        // Same source but different method maximum: search 0.3/1.5 and sort 0.3/3.
        AiRunSummary result=service().analyzeSubmission(7);
        assertEquals(2,result.getFromCache());assertEquals(0,calls);
        assertEquals(13,quality); // round((0.3 + 0.3) / (1.5 + 3) * 100)
    }
    @Test public void crossRubricCacheCannotBeReused(){
        rubric="LINKEDLIST";method(1,"search","O(N^2)");service().analyzeSubmission(7);
        methods.clear();method(2,"search","O(N^2)");rubric="BST";AiRunSummary result=service().analyzeSubmission(8);
        assertEquals(1,result.getAnalyzed());assertEquals(0,result.getFromCache());assertEquals(2,calls);assertEquals(60,quality);
    }
    @Test public void legacyCacheWithoutScoreIsReanalyzed(){
        method(1,"search","O(N^2)");AiOptimization old=new AiOptimization(1,"int search() { return 1; }","code","old rubric");old.setRubricType("NONE");stored.put(1,old);
        assertEquals(1,service().analyzeSubmission(7).getAnalyzed());assertEquals(60,quality);
    }
    @Test public void cacheInsertFailureDoesNotReportSuccess(){
        method(1,"search","O(N^2)");method(2,"search","O(N^2)");rejectCacheInsert=true;
        AiRunSummary result=service().analyzeSubmission(7);assertEquals(1,result.getFailed());assertEquals(0,result.getFromCache());assertEquals(1,calls);
    }
    @Test public void mockDefaultUsesRequestedRubricAndScoreScale()throws Exception {
        AiRequest req=new AiRequest();req.setRubricType("BST");req.setMethodName("search");req.setCode("int search(){return 1;}");
        AiResult result=new MockAiClient().analyze(req);assertFalse(result.getExplanation().contains("LinkedList"));assertTrue(result.getScore()>0 && result.getScore()<=100);
    }
}
