package com.fpt.aita.test;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.ForStmt;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class AstAnalyzerTest {

    @Test
    public void testParseAstAndExtractMethods() {
        String code = "public class SampleTest {\n"
                + "    public void bubbleSort(int[] a) {\n"
                + "        for (int i = 0; i < a.length; i++) {\n"
                + "            for (int j = 0; j < a.length - 1; j++) {\n"
                + "                if (a[j] > a[j+1]) {\n"
                + "                    int temp = a[j];\n"
                + "                    a[j] = a[j+1];\n"
                + "                    a[j+1] = temp;\n"
                + "                }\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
        Assert.assertEquals(1, methods.size());

        MethodDeclaration md = methods.get(0);
        Assert.assertEquals("bubbleSort", md.getNameAsString());

        List<ForStmt> forLoops = md.findAll(ForStmt.class);
        Assert.assertEquals(2, forLoops.size());
    }
}

