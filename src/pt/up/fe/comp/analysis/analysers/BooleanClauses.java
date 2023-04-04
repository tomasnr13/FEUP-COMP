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
import java.util.Objects;

public class BooleanClauses extends PreorderJmmVisitor<Integer, Integer> implements SemanticAnalyser {

    private final List<Report> reports;
    private final SymbolTableBuilder symbolTable;

    public BooleanClauses(JmmNode root, SymbolTableBuilder symbolTable) {
        this.reports = new ArrayList<>();
        this.symbolTable = symbolTable;

        addVisit(AstNode.IF, this::booleanClausesVisit);
        addVisit(AstNode.WHILE, this::booleanClausesVisit);
        visit(root, 0);
    }

    private Integer booleanClausesVisit(JmmNode node, Integer dummy){
        String nodeKind = "While and If";

        if(node.getKind().equals("If")) nodeKind = "If";
        if(node.getKind().equals("While")) nodeKind = "While";

        ExpressionVisit exprVis = new ExpressionVisit(symbolTable);

        Type type = null;
        boolean lessWithPoint = false;

        var expr = node.getJmmChild(0);

        Type type1, type2;

        switch (expr.getChildren().size()){
            // normal case (both operands below less)
            case 1:
                type = exprVis.expressionRec(node.getJmmChild(0), true, false, false, true);
                if(!type.getName().equals("boolean")){
                    reports.addAll(exprVis.getReports());
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                            Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("col")),
                            (nodeKind + " clauses should evaluate to a boolean type, but evaluated to " + type.getName() + ".")));
                    return -1;
                }
                break;
            // case where one of the operands has 'This' or 'Point'
            case 2:
                type1 = exprVis.expressionRec(expr.getJmmChild(0), true, false, false, true);

                // cases where the first child of less or and is Point node
                if(expr.getJmmChild(1).getJmmChild(0).getKind().equals("Point")) {
                    type2 = exprVis.expressionRec(expr.getJmmChild(1).getJmmChild(1), true, false, false, true);
                }
                else{
                    type2 = exprVis.expressionRec(expr.getJmmChild(1).getJmmChild(0), true, false, false, true);

                }

                if(type1.getName().equals("boolean") || type2.getName().equals("boolean")){
                    reports.addAll(exprVis.getReports());
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                            Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("col")),
                            ("Boolean clauses should not have boolean type operands 1")));
                    return -1;
                }
                if(!Objects.equals(type1.getName(), type2.getName())){
                    reports.addAll(exprVis.getReports());
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                            Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("col")),
                            ("Boolean clauses operands should have the same type")));
                    return -1;
                }

                break;
            // case where both operands have 'This' or 'Point'
            case 3:
                type1 = exprVis.expressionRec(expr.getJmmChild(0), true, false, false, true);
                type2 = exprVis.expressionRec(expr.getJmmChild(2).getJmmChild(0), true, false, false, true);

                if(type1.getName().equals("boolean") || type2.getName().equals("boolean")){
                    reports.addAll(exprVis.getReports());
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                            Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("col")),
                            ("Boolean clauses should not have boolean type operands")));
                    return -1;
                }

                if(!Objects.equals(type1.getName(), type2.getName())){
                    reports.addAll(exprVis.getReports());
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                            Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("col")),
                            ("Boolean clauses operands should have the same type")));
                    return -1;
                }

                break;
            default:
                System.out.println("Unknown case in boolean clause semantic check");
        }

        return 0;
    }

    @Override
    public List<Report> getReports() {
        return reports;
    }
}
