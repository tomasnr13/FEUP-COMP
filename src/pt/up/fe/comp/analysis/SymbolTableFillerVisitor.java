package pt.up.fe.comp.analysis;

import pt.up.fe.comp.VariableDeclaration;
import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SymbolTableFillerVisitor extends PreorderJmmVisitor<SymbolTableBuilder, Integer> {

    private final List<Report> reports;

    public SymbolTableFillerVisitor(){
        this.reports = new ArrayList<>();

        addVisit(AstNode.IMPORT_DECLARATION, this::importDeclarationVisit);
        addVisit(AstNode.CLASS_DECLARATION,this::classDeclarationVisit);
        addVisit(AstNode.METHOD_DEF, this::methodDeclarationVisit);
        addVisit(AstNode.MAIN_METHOD, this::mainMethodVisit);
    }

    public List<Report> getReports() {return reports;}

    private Integer importDeclarationVisit(JmmNode importDeclaration, SymbolTableBuilder symbolTable) {
        String importString = importDeclaration.getChildren().stream().map(y->y.get("value")).collect(Collectors.joining("."));

        // Single import declaration check
        if(symbolTable.getImports().contains(importString)){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                            Integer.parseInt(importDeclaration.get("line")),
                            Integer.parseInt(importDeclaration.get("col")),
                    "Found duplicate import '" + importString + "'."));
            return -1;
        }

        symbolTable.addImport(importString);
        return 0;
    }

    private Integer classDeclarationVisit(JmmNode classDeclaration, SymbolTableBuilder symbolTable) {
        symbolTable.setClassName(classDeclaration.get("name"));
        classDeclaration.getOptional("extends").ifPresent(superName->symbolTable.setSuperName(superName));
        var fields = classDeclaration.getChildren().stream().filter(y->y.getKind().equals("VariableDeclaration")).collect(Collectors.toList());
        for (var field : fields){
            var fieldType = field.getJmmChild(0).get("value");
            var fieldName = field.getJmmChild(1).get("value");

            var fieldTypeIsArray = fieldType.contains("array");
            if(fieldTypeIsArray) fieldType = "int";

            // Single field declaration check
            if(symbolTable.containsName(symbolTable.getFields(), fieldName)){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        Integer.parseInt(field.get("line")),
                        Integer.parseInt(field.get("col")),
                        "Found duplicate field '" + fieldName + "'."));
                return -1;
            }

            symbolTable.addField(new Symbol(new Type(fieldType, fieldTypeIsArray), fieldName));
        }
        return 0;
    }

    private Integer methodDeclarationVisit(JmmNode methodDeclaration, SymbolTableBuilder symbolTable) {
        String methodSignature = methodDeclaration.getJmmChild(1).get("value");

        // Single method declaration check
        if (symbolTable.hasMethod(methodSignature)){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(methodDeclaration.get("line")),
                    Integer.parseInt(methodDeclaration.get("col")),
                    "Found duplicate method '" + methodSignature + "'."));
            return -1;
        }
        var isArray = methodDeclaration.getJmmChild(0).get("value").contains("array");
        var typeName = methodDeclaration.getJmmChild(0).get("value");

        if(isArray) typeName = "int";

        var type = new Type(typeName, isArray);

        var parameters = methodDeclaration.getChildren().stream().filter(y->y.getKind().equals("Parameter")).collect(Collectors.toList());
        List<Symbol> paramList = new ArrayList<>();
        for (var param : parameters){
            var paramType = param.getJmmChild(0).get("value");
            var paramName = param.getJmmChild(1).get("value");

            isArray = param.getJmmChild(0).get("value").contains("array");

            if(isArray) paramType = "int";

            // Single parameter declaration check
            if(symbolTable.containsName(paramList, paramName)){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        Integer.parseInt(param.get("line")),
                        Integer.parseInt(param.get("col")),
                        "Found duplicate parameter '" + paramName + "'."));
                return -1;
            }

            paramList.add(new Symbol(new Type(paramType, isArray), paramName));
        }

        var localVars = methodDeclaration.getChildren().stream().filter(y->y.getKind().equals("VariableDeclaration")).collect(Collectors.toList());
        List<Symbol> localVarList = new ArrayList<>();
        for (var localVar : localVars){
            var varType = localVar.getJmmChild(0).get("value");
            var varName = localVar.getJmmChild(1).get("value");

            isArray = localVar.getJmmChild(0).get("value").contains("array");

            if(isArray) varType = "int";

            // Single local variable declaration check (against fields)
            if(symbolTable.containsName(symbolTable.getFields(), varName)){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        Integer.parseInt(localVar.get("line")),
                        Integer.parseInt(localVar.get("col")),
                        "Field already declared with the name '" + varName + "'."));
                return -1;
            }

            // Single local variable declaration check (against other local variables)
            if(symbolTable.containsName(localVarList, varName)){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        Integer.parseInt(localVar.get("line")),
                        Integer.parseInt(localVar.get("col")),
                        "Found duplicate local variable '" + varName + "'."));
                return -1;
            }

            localVarList.add(new Symbol(new Type(varType, isArray), varName));
        }

        symbolTable.addMethod(methodSignature, type, paramList, localVarList);
        return 0;
    }

    private Integer mainMethodVisit(JmmNode mainMethod, SymbolTableBuilder symbolTable) {
        String methodSignature = "main";
        if (symbolTable.hasMethod(methodSignature)){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(mainMethod.get("line")),
                    Integer.parseInt(mainMethod.get("col")),
                    "Found duplicate method in main '" + methodSignature + "'."));
            return -1;
        }
        var type = new Type("void", false);

        List<Symbol> paramList = new ArrayList<>();
        paramList.add(new Symbol(new Type("String",true), mainMethod.get("param")));

        var localVars = mainMethod.getChildren().stream().filter(y->y.getKind().equals("VariableDeclaration")).collect(Collectors.toList());
        List<Symbol> localVarList = new ArrayList<>();
        for (var localVar : localVars){
            var varType = localVar.getJmmChild(0).get("value");
            var varName = localVar.getJmmChild(1).get("value");

            var isArray = localVar.getJmmChild(0).get("value").contains("array");

            if(isArray) varType = "int";

            // Single local variable in main declaration check
            if(symbolTable.containsName(localVarList, varName)){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        Integer.parseInt(localVar.get("line")),
                        Integer.parseInt(localVar.get("col")),
                        "Found duplicate local variable in main '" + varName + "'."));
                return -1;
            }

            localVarList.add(new Symbol(new Type(varType, isArray), varName));
        }

        symbolTable.addMethod(methodSignature, type, paramList, localVarList);

        return 0;
    }
}
