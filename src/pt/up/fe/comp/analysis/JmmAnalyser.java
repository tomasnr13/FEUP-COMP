package pt.up.fe.comp.analysis;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.comp.analysis.analysers.*;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

public class JmmAnalyser implements JmmAnalysis{
    
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult){
        var symbolTable = new SymbolTableBuilder();
        var symbolTableFiller = new SymbolTableFillerVisitor();
        symbolTableFiller.visit(parserResult.getRootNode(), symbolTable);
        List<Report> reports = new ArrayList<>(symbolTableFiller.getReports());

        // List of semantic analysers
        List<SemanticAnalyser> analysers = new ArrayList<>();
        analysers.add(new ExtendedClassImported(parserResult.getRootNode(), symbolTable));
        analysers.add(new MethodAndArgumentsExist(parserResult.getRootNode(), symbolTable));
        analysers.add(new ReturnType(parserResult.getRootNode(), symbolTable));
        analysers.add(new ArrayIndex(parserResult.getRootNode(), symbolTable));
        analysers.add(new BooleanClauses(parserResult.getRootNode(), symbolTable));
        analysers.add(new AssignmentType(parserResult.getRootNode(), symbolTable));
        analysers.add(new ThisInWrongPlaces(parserResult.getRootNode(), symbolTable));

        for (var analyser : analysers){
            reports.addAll(analyser.getReports());
        }

        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }
}
