package pt.up.fe.comp.analysis.analysers;

import pt.up.fe.comp.analysis.SemanticAnalyser;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class ExpressionVisit extends PreorderJmmVisitor<Integer, Integer> implements SemanticAnalyser {

    private final List<Report> reports;
    private final SymbolTable symbolTable;
    private final Type errorType = new Type("", false);

    public ExpressionVisit(SymbolTable symbolTable) {
        this.reports = new ArrayList<>();
        this.symbolTable = symbolTable;
    }


    // recursive function that returns the type of expression
    public Type expressionRec(JmmNode node, boolean noArrayTypeOnAccess, boolean noMethodCallVerification, boolean noWildcardImport, boolean arrayCheck){

        // base case 1 (integer)
        if(node.getKind().equals("IntegerLiteral")){
            return new Type("int", false);
        }
        // base case 2 (boolean)
        else if(node.getKind().equals("BooleanLiteral")){
            return new Type("boolean", false);
        }
        // base case 3 (Id's)
        else if(node.getKind().equals("Id")){
            // search the symbol table and get the corresponding type
            // if not found, report a declaration error
            return declarationCheck(node, noArrayTypeOnAccess, noMethodCallVerification, noWildcardImport, arrayCheck);
        }
        // base case 4 (Array)
        else if(node.getKind().equals("Array")){
            return new Type("int", false);
        }
        // base case 5 (This)
        else if(node.getKind().equals("This")){
            // check previously if 'this' has point operation (represents field) or is alone (represents object)
            var express = node.getAncestor("Expression");
            if(express.isPresent()){
                if(express.get().getChildren().size() > 1){
                    var point = express.get().getJmmChild(1);

                    if(point.getKind().equals("Point")){

                        if(point.getChildren().size() >= 1) {
                            if (point.getJmmChild(0).getKind().equals("MethodCall")) {
                                var meth = point.getJmmChild(0);
                                if(symbolTable.getSuper() != null){
                                    if(noWildcardImport) return new Type(symbolTable.getSuper(), false);
                                    return new Type("import", false);
                                }
                                else if (symbolTable.getMethods().contains(meth.get("name")))
                                    return MethodTypeUtil.getMethodType(node, meth.get("name"));
                            }
                        }
                    }
                }
            }
            return new Type(symbolTable.getClassName(), false);
        }
        // base case 6 (New operation)
        else if(node.getKind().equals("New")){
            // check for import or class definition
            var typeId = node.getJmmChild(0).get("type");

            List<String> imports = symbolTable.lastStringImports();

            if(imports.contains(typeId) || symbolTable.getClassName().equals(typeId)){
                return new Type(typeId, false);
            }
            else if(typeId.equals("int array")){
                return new Type("int", true);
            }
            else{
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("col")),
                        "Undefined Class name. TYPE: " + typeId));
                return errorType;
            }
        }
        // if is node is equal to Expression call function recursively on each child to get expression type
        else if(node.getKind().equals("Expression")){
            var expressionChild = node.getJmmChild(0);

            return expressionRec(expressionChild,true, false, false, true);
        }
        // if node is not an Expression
        else{
            String nodeKind = node.getKind();
            var nodeChildren = node.getChildren();

            List<Type> typesFound = new ArrayList<>();
            Type finalType = errorType;

            boolean isLessWithPoint = false;

            // boolean operation 'And' and comparison Less
            if(nodeKind.equals("And") || nodeKind.equals("Less")){

                finalType = new Type("boolean", false);
            }
            // Arithmetic and Comparison operations
            else if(nodeKind.equals("Add") || nodeKind.equals("Sub") || nodeKind.equals("Mult") || nodeKind.equals("Div")){
                finalType = new Type("int", false);
            }

            if(!isLessWithPoint) {
                for (var child : nodeChildren) {
                    String childKind = child.getKind();

                    if (childKind.equals("Add") || childKind.equals("Sub") || childKind.equals("Mult") || childKind.equals("Div") || childKind.equals("IntegerLiteral") || childKind.equals("BooleanLiteral") || childKind.equals("New") || childKind.equals("Id") || childKind.equals("This")) {
                        // call recursively to get type
                        typesFound.add(expressionRec(child, noArrayTypeOnAccess, false, false, true));
                    }
                }
            }

            // check that all types are equal
            if(typesFound.size() == 0){  // this error should never appear, as it will always exist at least, one literal -> one type found
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("col")),
                        "Expression does not have a type."));
                return errorType;
            }
            else {
                Type prevType = typesFound.get(0);

                // check if types found are equal to the final pre-calculated type of expression
                for (var type : typesFound) {
                    boolean matches = finalType.equals(type);

                    // in case of less comparison operation types found have to be none boolean
                    if(node.getKind().equals("Less")){
                        matches = !finalType.equals(type) && type.equals(prevType);
                    }

                    if (!matches){
                        String errorMessage = "Mismatching expression types, " + finalType.getName();
                        if(finalType.isArray()) errorMessage += " array";
                        errorMessage += " and " + type.getName();
                        if(type.isArray()) errorMessage += " array";
                        errorMessage += ".";
                        errorMessage = errorMessage.replaceAll("import", "Imported Class");

                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                                Integer.parseInt(node.get("line")),
                                Integer.parseInt(node.get("col")),
                                errorMessage));
                        return errorType;
                    }

                    prevType = type;
                }

                return finalType;
            }
        }
    }

    public Type declarationCheck(JmmNode node, boolean noArrayTypeOnAccess, boolean noMethodCallVerification, boolean noWildcardImport, boolean arrayCheck){
        String value = node.get("value");
        Type type = new Type("", false);

        var method = node.getAncestor("MethodDef");
        if(method.isEmpty()) method = node.getAncestor("MainMethod");

        // if outside method, id can only be a field
        if(method.isEmpty()){
            List<Symbol> fields = symbolTable.getFields();

            if (symbolTable.containsName(fields, value)){
                for(var localVar : symbolTable.getFields())
                    if(localVar.getName().equals(value)) return localVar.getType();
            }

            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("col")),
                    "Field not declared."));
            return errorType;
        }

        String methodName = "";
        boolean isMain = false;

        if(method.get().getKind().equals("MainMethod")){
            methodName = "main";
            isMain = true;
        }
        else if(method.get().getKind().equals(("MethodDef"))){
            methodName = method.get().getJmmChild(1).get("value");
        }

        List<Symbol> localVars = symbolTable.getLocalVariables(methodName);
        List<Symbol> params = symbolTable.getParameters(methodName);
        List<Symbol> fields = symbolTable.getFields();

        if(symbolTable.containsName(localVars, value)){
            for(var localVar : localVars)
                if (localVar.getName().equals(node.get("value"))) type = localVar.getType();
        }
        else if (symbolTable.containsName(params, value)){
            for(var localVar : params)
                if (localVar.getName().equals(node.get("value"))) type = localVar.getType();
        }
        else if(symbolTable.containsName(fields, value)) {
            if (!isMain) {
                for (var localVar : fields)
                    if (localVar.getName().equals(node.get("value"))) type = localVar.getType();
            }
        }
        else {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("col")),
                    "Variable has not been declared."));
            return errorType;
        }

        if(isMain && symbolTable.containsName(fields, value) && !symbolTable.containsName(params, value) && !symbolTable.containsName(localVars, value)){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("col")),
                    "Variable has not been declared."));
            return errorType;
        }

        var topExpression = node.getAncestor("Expression");
        var parent = node.getJmmParent();

        // check if there is a point operation to get length from an array
        if(parent.getChildren().size() > 1){
            if(parent.getJmmChild(1).getKind().equals("Point")){
                var attributes = parent.getJmmChild(1).getAttributes();
                if(attributes.contains("name") && type.isArray()){
                    return new Type("int", false);
                }
            }
        }

        if(!noMethodCallVerification) {
            // check if there is a point operation to call a method and if it does return method type
            if (topExpression.isPresent()) {
                if (topExpression.get().getChildren().size() > 1) {
                    if (topExpression.get().getJmmChild(1).getKind().equals("Point")) {
                        var methodCall = topExpression.get().getJmmChild(1).getJmmChild(0);
                        var methods = symbolTable.getMethods();

                        if (methods.contains(methodCall.get("name")))
                            return MethodTypeUtil.getMethodType(node, methodCall.get("name"));
                        else if (symbolTable.getSuper() != null /*&& topExpression.get().getJmmChild(0).get("value").equals(symbolTable.getClassName())*/) {
                            if(noWildcardImport) type = new Type(symbolTable.getSuper(), false);
                            else type = new Type("import", false);  // NEW
                        }
                    }
                }
            }
        }

        // check to see the existence of an array access and if it exists return the type with isArray set to false
        if(topExpression.isPresent() && arrayCheck) {
            if (topExpression.get().getChildren().size() > 1)
                if (topExpression.get().getJmmChild(1).getKind().equals("Array")) {
                    return new Type(type.getName(), !noArrayTypeOnAccess);
                }
                else if (topExpression.get().getJmmChild(1).getNumChildren()>=1){
                    if(topExpression.get().getJmmChild(1).getJmmChild(0).getKind().equals("Array")){
                        return new Type(type.getName(), !noArrayTypeOnAccess);
                    }
                }
        }

        // check if type is imported, if it is, the type is changed to "import"
        List<String> imports = symbolTable.lastStringImports();

        if(imports.contains(type.getName())) {
            if(noWildcardImport) return new Type(type.getName(), false);
            return new Type("import", false);
        }

        // check if the type is equal to the actual class name
        String className = symbolTable.getClassName();

        if(type.getName().equals(className)){
            //if(symbolTable.getSuper() != null) type = new Type("import", false);
            return new Type(className, false); //NEW
        }

        // TODO: the grammar is not putting Array Id node as expected in assignment operations that
        //  have an array in the right side, but it works, because Id stays in the same relative position

        return type;
    }

    @Override
    public List<Report> getReports() {
        return reports;
    }
}
