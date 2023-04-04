package pt.up.fe.comp.analysis.analysers;

import pt.up.fe.comp.analysis.SemanticAnalyser;
import pt.up.fe.comp.analysis.SymbolTableBuilder;
import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class ReturnType extends PreorderJmmVisitor<Integer, Integer> implements SemanticAnalyser {

    private final List<Report> reports;
    private final SymbolTableBuilder symbolTable;

    public ReturnType(JmmNode root, SymbolTableBuilder symbolTable) {
        this.reports = new ArrayList<>();
        this.symbolTable = symbolTable;

        addVisit(AstNode.METHOD_DEF, this::methodVisit);
        visit(root, 0);
    }

    private Integer methodVisit(JmmNode methodNode, Integer dummy){
        JmmNode returnNode = null;
        var methodChildren = methodNode.getChildren();
        for(var child : methodChildren) if(child.getKind().equals("Return")) returnNode = child;

        String returnNodeType;
        String methodReturnType = methodNode.getJmmChild(0).get("value");

        ExpressionVisit exprVis = new ExpressionVisit(symbolTable);

        // check return type with actual method return type
        if(returnNode != null){
            var returnExpression = returnNode.getJmmChild(0);

            Type type = exprVis.expressionRec(returnExpression, true, false, false, true);

            if(type.getName().equals("")) {
                reports.addAll(exprVis.getReports());
                return -1;
            }

            returnNodeType = type.getName();

            if(type.isArray()) returnNodeType += " array";

            if(methodReturnType.equals("import") || returnNodeType.equals("import")) return 0;

            if(!methodReturnType.equals(returnNodeType)){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        Integer.parseInt(returnNode.get("line")),
                        Integer.parseInt(returnNode.get("col")),
                        "Return of type " + returnNodeType + " instead of type " + methodReturnType + "."));
                return -1;
            }
        }

        return 0;
    }

    @Override
    public List<Report> getReports() {
        return reports;
    }
}
