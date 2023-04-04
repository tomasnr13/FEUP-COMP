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

public class ThisInWrongPlaces extends PreorderJmmVisitor<Integer, Integer> implements SemanticAnalyser {
    private final List<Report> reports;
    private final SymbolTableBuilder symbolTable;

    public ThisInWrongPlaces(JmmNode root, SymbolTableBuilder symbolTable) {
        this.reports = new ArrayList<>();
        this.symbolTable = symbolTable;

        addVisit(AstNode.THIS, this::thisVisit);
        visit(root, 0);
    }

    private Integer thisVisit(JmmNode node, Integer dummy){

        // when there is a 'this' in main method
        if(node.getAncestor("MainMethod").isPresent()){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("col")),
                    "'This' in main method is not allowed!"));
            return -1;
        }

        return 0;
    }

    @Override
    public List<Report> getReports() {
        return reports;
    }
}
