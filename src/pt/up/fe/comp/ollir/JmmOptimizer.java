package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

import java.util.Collections;

public class JmmOptimizer implements JmmOptimization {
    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        var ollirGenerator = new OllirGenerator(semanticsResult.getSymbolTable());
        ollirGenerator.visit(semanticsResult.getRootNode());
        var ollirCode = ollirGenerator.getCode();    //this is commented for jasmin testing
        //var ollirCode = SpecsIo.getResource("myTests/DummyOllirCode.txt");
        System.out.println("Parse Tree:\n\n" + semanticsResult.getRootNode().toTree());
        System.out.println("Ollir Code:\n\n"+ollirCode);
        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    //optimize
}
