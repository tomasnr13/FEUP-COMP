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

public class ArrayIndex extends PreorderJmmVisitor<Integer, Integer> implements SemanticAnalyser {

    private final List<Report> reports;
    private final SymbolTableBuilder symbolTable;

    public ArrayIndex(JmmNode root, SymbolTableBuilder symbolTable) {
        this.reports = new ArrayList<>();
        this.symbolTable = symbolTable;

        addVisit(AstNode.ARRAY, this::arrayVisit);
        visit(root, 0);
    }

    private Integer arrayVisit(JmmNode node, Integer dummy) {
        ExpressionVisit exprVis = new ExpressionVisit(symbolTable);

        Type type = exprVis.expressionRec(node.getJmmChild(0), false, false, false, true);

        if (type.getName().equals("")) {
            reports.addAll(exprVis.getReports());
            return -1;
        }
        // check array index type
        else if (!type.getName().equals("int")) {
            reports.addAll(exprVis.getReports());
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("col")),
                    "Array index is not of type int."));
            return -1;
        }

        // check array access on non array variables
        var parent = node.getJmmParent();
        Type varType = null;
        JmmNode variable = null;

        if(parent.getJmmChild(0).getKind().equals("Id")) {
            variable = parent.getJmmChild(0);
        }
        else {
            var expression = node.getAncestor("Expression");
            if(expression.isPresent()){
                variable = expression.get().getJmmChild(0);
            }
        }

        assert variable != null;
        varType = exprVis.expressionRec(variable, false, false, false, false);

        System.out.println("HERE: " + varType.isArray());
        if (varType != null) {
            if (varType.getName().equals("")) {
                reports.addAll(exprVis.getReports());
                return -1;
            }
        }

        assert varType != null;
        if (!varType.isArray()) {
            reports.addAll(exprVis.getReports());
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("col")),
                    "Array access on " + varType.getName() + " type variable."));
            return -1;
        }

        return 0;
    }

    @Override
    public List<Report> getReports() {
        return reports;
    }
}
