package pt.up.fe.comp.analysis.analysers;

import pt.up.fe.comp.analysis.SemanticAnalyser;
import pt.up.fe.comp.analysis.SymbolTableBuilder;
import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class MainMethod extends PreorderJmmVisitor<Integer, Integer> implements SemanticAnalyser {

    private final List<Report> reports;
    private final SymbolTableBuilder symbolTable;

    public MainMethod(JmmNode root, SymbolTableBuilder symbolTable) {
        this.reports = new ArrayList<>();
        this.symbolTable = symbolTable;

        addVisit(AstNode.CLASS_DECLARATION, this::classVisit);
        visit(root, 0);
    }

    private Integer classVisit(JmmNode classNode, Integer dummy){
        // check if main method exists
        if(!symbolTable.getMethods().contains("main")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(classNode.get("line")),
                    Integer.parseInt(classNode.get("col")),
                    "Class " + classNode.get("name") + " does not have a main method."));
            return -1;
        }

        return 0;
    }

    @Override
    public List<Report> getReports() { return reports; }
}
