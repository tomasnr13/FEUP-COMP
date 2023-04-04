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

public class MethodAndArgumentsExist extends PreorderJmmVisitor<Integer, Integer> implements SemanticAnalyser {

    private final List<Report> reports;
    private final SymbolTableBuilder symbolTable;

    public MethodAndArgumentsExist(JmmNode root, SymbolTableBuilder symbolTable) {
        this.reports = new ArrayList<>();
        this.symbolTable = symbolTable;

        addVisit(AstNode.METHOD_CALL, this::methodCallVisit);
        visit(root, 0);
    }

    private Integer methodCallVisit(JmmNode methodCallNode, Integer dummy){
        ExpressionVisit exprVis = new ExpressionVisit(symbolTable);

        // check if method exists
        var methodName = methodCallNode.get("name");

        var methods = symbolTable.getMethods();
        List<String> imports = symbolTable.lastStringImports();

        // check imports for method name
        // when imported, is assumed that the method exists and their arguments are always right
        if(imports.contains(methodName)) return 0;

        // check imports for possible class or variable that is calling the method
        var expression = methodCallNode.getAncestor("Expression");
        JmmNode variable = null;

        if(expression.isPresent()){
            variable = expression.get().getJmmChild(0);

            // in the case of "this" ignore, because method definition in the class is checked later
            if(!variable.getKind().equals("This")) {

                // when it is a class, it can be checked directly in imports names
                if(variable.getAttributes().contains("value")) if (imports.contains(variable.get("value"))) return 0;

                Type varType = exprVis.expressionRec(variable, true, false, false, true);
                if (imports.contains(varType.getName()) || varType.getName().equals("import")) return 0;
            }
        }

        // check arguments types
        if(methods.contains(methodName)){

            // get method definition parameters and arguments that were used
            var methodParams = symbolTable.getParameters(methodName);
            var methodArgs = methodCallNode.getChildren();

            // check if it is being call from imported class and return if it is
            var expression2 = methodCallNode.getAncestor("Expression");

            if(expression2.get().getJmmChild(0).getKind().equals("Id")) {
                var type = exprVis.expressionRec(expression2.get().getJmmChild(0), true, true, false, true);

                if (type.getName().equals("import")) return 0;
            }

            // check if it was called with all the parameters and ignore if it is constructor
            if(methodParams.size() != methodArgs.size() /*&& !methodName.equals(symbolTable.getClassName())*/){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        Integer.parseInt(methodCallNode.get("line")),
                        Integer.parseInt(methodCallNode.get("col")),
                        "Missing arguments in method call of method " + methodName + "."));
                return -1;
            }

            // check if arguments types and method call parameters types are equal
            for(var i = 0; i < methodParams.size(); i++){
                Type paramType = methodParams.get(i).getType();
                Type argType = exprVis.expressionRec(methodArgs.get(i), true, false, false, true);

                // wildcard import -> Warning won't work if two different imported classes are used in function arguments
                if(argType.getName().equals("import")) return 0;

                if(!paramType.equals(argType)){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                            Integer.parseInt(methodCallNode.get("line")),
                            Integer.parseInt(methodCallNode.get("col")),
                            "Argument of type " + argType.getName() + " instead of type " + paramType.getName() + "."));
                    return -1;
                }
            }
        }
        else if(symbolTable.getSuper() != null){  // NEW
            Type methodType = exprVis.expressionRec(methodCallNode.getAncestor("Expression").get(), true, false, false, true);

            if(methodType.getName().equals(symbolTable.getClassName())){
                return 0;
            }
        }
        else{
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(methodCallNode.get("line")),
                    Integer.parseInt(methodCallNode.get("col")),
                    "Method is not defined."));
            return -1;
        }

        return 0;
    }

    @Override
    public List<Report> getReports() { return reports; }
}
