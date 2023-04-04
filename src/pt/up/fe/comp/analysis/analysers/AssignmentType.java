package pt.up.fe.comp.analysis.analysers;

import pt.up.fe.comp.analysis.SemanticAnalyser;
import pt.up.fe.comp.analysis.SymbolTableBuilder;
import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import java.util.ArrayList;
import java.util.List;

public class AssignmentType extends PreorderJmmVisitor<Integer, Integer> implements SemanticAnalyser {

    private final List<Report> reports;
    private final SymbolTableBuilder symbolTable;

    public AssignmentType(JmmNode root, SymbolTableBuilder symbolTable){
        this.reports = new ArrayList<>();
        this.symbolTable = symbolTable;

        addVisit(AstNode.ASSIGNMENT, this::assignmentVisit);
        visit(root,0);
    }

    private Integer assignmentVisit(JmmNode assignmentNode, Integer dummy){

        // left side of the assignment
        JmmNode left = assignmentNode.getJmmChild(0);

        // right side of the assignment
        JmmNode right;
        if(assignmentNode.getJmmChild(1).getKind().equals("Array")) right = assignmentNode.getJmmChild(2);
        else right = assignmentNode.getJmmChild(1);

        ExpressionVisit exprVis = new ExpressionVisit(symbolTable);

        Type leftType = exprVis.expressionRec(left, true, false, false, true);

        //System.out.println("TYPE ON THE LEFT: "+ leftType.getName());

        // wildcard "import" type
        if(leftType.getName().equals("import")) return 0;

        if(leftType.getName().equals("")) {
            reports.addAll(exprVis.getReports());
            return -1;
        }

        Type rightType = exprVis.expressionRec(right, true, false, false, true);

        //System.out.println("TYPE ON THE RIGHT: "+ rightType.getName());

        // little hack to check for object extends in te left side, and still have wildcard import in the right
        if(rightType.getName().equals("import") && !leftType.getName().equals(symbolTable.getClassName())) return 0;

        if(rightType.getName().equals("")) {
            reports.addAll(exprVis.getReports());
            return -1;
        }

        if(!rightType.equals(leftType)){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                Integer.parseInt(assignmentNode.get("line")),
                Integer.parseInt(assignmentNode.get("col")),
                errorMsgBuilder(leftType, rightType)));
            return -1;
        }

        return 0;
    }

    @Override
    public List<Report> getReports() {
        return reports;
    }

    private String errorMsgBuilder(Type left, Type right){
        String msg = "Assigned " + right.getName();

        if(right.isArray()) msg += " array";
        msg += " to ";
        msg +=  left.getName();

        if(left.isArray()) msg += " array";
        msg += " type.";

        return msg.replaceAll("import", "Imported Class type");
    }
}
