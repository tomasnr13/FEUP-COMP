package pt.up.fe.comp.ollir;

import pt.up.fe.comp.analysis.analysers.ExpressionVisit;
import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<Integer, Integer> {
    private final StringBuilder code;
    private final SymbolTable symbolTable;
    private final List<Report> reports;
    private Integer loopIndex;
    private Integer ifIndex;
    private final ExpressionVisit exprVisit;
    private String currMethod;
    private Integer threeIndex;
    private String currassign;
    private Integer arrayIndex;
    private Integer usedArrayIndex;

    public OllirGenerator(SymbolTable symbolTable) {
        this.code = new StringBuilder();
        this.symbolTable = symbolTable;
        this.reports = new ArrayList<>();
        this.loopIndex = 0;
        this.ifIndex = 0;
        this.currassign = "";
        this.exprVisit = new ExpressionVisit(symbolTable);
        this.usedArrayIndex=0;
        this.arrayIndex=0;

        addVisit(AstNode.START, this::programVisit);
        addVisit(AstNode.CLASS_DECLARATION,this::classDeclarationVisit);
        addVisit(AstNode.METHOD_DEF,this::methodDeclarationVisit);
        addVisit(AstNode.MAIN_METHOD,this::mainDeclarationVisit);
        addVisit(AstNode.STATEMENT,this::statementVisit);
        addVisit(AstNode.WHILE, this::whileVisit);
        addVisit(AstNode.IF, this::ifVisit);
        addVisit(AstNode.RETURN, this::returnVisit);
        addVisit(AstNode.ASSIGNMENT, this::assignmentVisit);
        addVisit(AstNode.EXPRESSION, this::expressionVisit);
    }

    public String getCode() { return code.toString(); }

    private Integer programVisit(JmmNode program, Integer dummy){
        for (var importString : symbolTable.getImports()) {
            code.append("import ").append(importString).append(";\n");
        }
        for (var child : program.getChildren()) visit(child);

        return 0;
    }

    public Integer classDeclarationVisit(JmmNode classDeclaration, Integer dummy){
        threeIndex=0;
        code.append("public ").append(symbolTable.getClassName());
        var superClass = symbolTable.getSuper();
        if(superClass!=null) code.append(" extends ").append(superClass);
        code.append(" {\n");

        // fields
        var fieldCode = symbolTable.getFields().stream().map(symbol -> ".field " + OllirUtils.getCode(symbol)).collect(Collectors.joining(";\n"));
        if(symbolTable.getFields().size() != 0) code.append(fieldCode).append(";\n");

        //constructor
        code.append(".construct ").append(symbolTable.getClassName()).append("().V {\n");
        code.append("invokespecial(this, \"<init>\").V;\n").append("}\n");

        // methods
        for (var child : classDeclaration.getChildren()) visit(child);
        code.append("}\n");

        return 0;
    }

    public Integer methodDeclarationVisit(JmmNode methodDeclaration, Integer dummy){
        // method name
        threeIndex=0;
        var methodSignature = methodDeclaration.getJmmChild(1).get("value");
        currMethod = methodSignature;
        code.append(".method public ").append(methodSignature).append("(");

        // parameters
        var params = symbolTable.getParameters(methodSignature);
        var paramCode = params.stream().map(OllirUtils::getCode).collect(Collectors.joining(", "));
        code.append(paramCode);
        code.append(").").append(OllirUtils.getCode(symbolTable.getReturnType(methodSignature)));
        code.append(" {\n");

        // statements
        var statements = methodDeclaration.getChildren().stream().filter(y->y.getKind().equals("Statement")).collect(Collectors.toList());
        for (var statement : statements) {
            visit(statement);
        }

        // return
        visit(methodDeclaration.getJmmChild(methodDeclaration.getNumChildren()-1)); // last child
        code.append("}\n");
        return 0;
    }

    public Integer mainDeclarationVisit(JmmNode mainDeclaration, Integer dummy){
        threeIndex=0;
        code.append(".method public static main(");
        var params = symbolTable.getParameters("main");
        var paramCode = params.stream().map(symbol -> OllirUtils.getCode(symbol)).collect(Collectors.joining(", "));
        code.append(paramCode);
        code.append(").").append(OllirUtils.getCode(symbolTable.getReturnType("main")));
        code.append(" {\n");
        var statements = mainDeclaration.getChildren().stream().filter(y->y.getKind().equals("Statement")).collect(Collectors.toList());
        for (var statement : statements) {
            visit(statement);
        }
        code.append("ret.V;\n");
        code.append("}\n");

        return 0;
    }

    public Integer statementVisit(JmmNode statementDeclaration, Integer dummy){
        for (var child : statementDeclaration.getChildren()) {
            visit(child);
        }
        /*if(statementDeclaration.getJmmParent().getKind().equals("MethodDef") || statementDeclaration.getJmmParent().getKind().equals("MainMethod")
            && statementDeclaration.getJmmChild(0).getKind().equals("If")) {
            code.append(";\n");
        }*/

        return 0;
    }

    public Integer expressionVisit(JmmNode expressionNode, Integer dummy){
            expressionParser(expressionNode);
        return 0;
    }

    public Integer whileVisit(JmmNode whileDeclaration, Integer dummy) {
        code.append("Loop_").append(loopIndex).append(":\n");

        //condition for loop, convert expression to if
        var conditionExpression = whileDeclaration.getJmmChild(0);
        code.append("if(");
        visit(conditionExpression);
        code.append(") ");

        code.append("goto Body_").append(loopIndex).append(";\n");
        code.append("goto EndLoop_").append(loopIndex).append(";\n");

        //body of the loop
        code.append("Body_").append(loopIndex).append(":\n");
        // in doStatement treat statements
        var doStatement = whileDeclaration.getJmmChild(1);
        visit(doStatement.getJmmChild(0));
        code.append("goto Loop_").append(loopIndex).append(";\n");
        code.append("EndLoop_").append(loopIndex).append(":\n");
        loopIndex++;

        return 0;
    }


    public Integer ifVisit(JmmNode ifDeclaration, Integer dummy) {
        ifIndex++;
        var actualIndex = ifIndex;
        var hasElse = ifDeclaration.get("hasElse").equals("true");

        // if expression true 'goto Then{number};'
        var ifExpr = ifDeclaration.getJmmChild(0);
        code.append("if (");
        visit(ifExpr);
        code.append(" ) goto Then_").append(ifIndex).append(";\n");

        if (hasElse){
            code.append("goto Else_").append(ifIndex).append(";\n");
        }
        else{
            code.append("goto EndIf_").append(ifIndex).append(";\n");
        }

        // code for then
        var thenCode = ifDeclaration.getJmmChild(1);
        code.append("Then_").append(ifIndex).append(":\n");
        visit(thenCode.getJmmChild(0));

        if (hasElse) {
            code.append("Else_").append(ifIndex).append(":\n");
            // rest of else code
            var elseCode = ifDeclaration.getJmmChild(2);
            visit(elseCode.getJmmChild(0));

        }

        //Endif
        code.append("Endif_").append(actualIndex).append(":\n");

        return 0;
    }

    private Integer returnVisit(JmmNode returnDeclaration, Integer dummy) {
        String tempvar = "";
        if(verifyThree(returnDeclaration.getJmmChild(0))){
            if(returnDeclaration.getJmmChild(0).getKind().equals("Expression")){
                threeIndex=0;
                for (var child : returnDeclaration.getJmmChild(0).getChildren()) {
                    if (isOp(child)) {
                        tempvar = threeAddressArithmetic(child);
                    }
                }
            }
            else {
                threeIndex = 0;
                tempvar = threeAddressArithmetic(returnDeclaration.getJmmChild(0));
            }
        }
        code.append("ret.");
        code.append(OllirUtils.getCode(symbolTable.getReturnType(currMethod))).append(" ");

        if(verifyThree(returnDeclaration.getJmmChild(0))){
            code.append(tempvar).append(".i32");
        }
        else{
            visit(returnDeclaration.getJmmChild(0));
        }
        code.append(";\n");
        return 0;
    }

    // TODO: missing array and point operations
    private Integer assignmentVisit(JmmNode assignmentNode, Integer dummy) {
        //check if the second argument is Three
        String tempvar = "";
        threeArrayAcess(assignmentNode);
        if(verifyThree(assignmentNode.getJmmChild(1))){
            if(assignmentNode.getJmmChild(1).getKind().equals("Expression")){
                for (var child : assignmentNode.getJmmChild(1).getChildren()) {
                    if (isOp(child)) {
                        tempvar = threeAddressArithmetic(child);
                    }
                }
            }
            else {
                tempvar = threeAddressArithmetic(assignmentNode.getJmmChild(0));
            }
        }

        // type from the left assignment side
        var left = assignmentNode.getJmmChild(0);
        expressionParser(left);
        Type typeLeft = exprVisit.expressionRec(left, true, false, true, true);
        currassign = typeLeft.toOllir();
        //code.append(left.get("value"));
        //appendType(typeLeft);

        // assignment operation
        code.append(" :=");
        code.append(typeLeft.toOllir());
        code.append(" ");

        if(verifyThree(assignmentNode.getJmmChild(1))){
            code.append(tempvar);
            code.append(typeLeft.toOllir());
        }
        else {
            // type from the right side of the assignment
            var right = assignmentNode.getJmmChild(1);
            expressionParser(right);
        }
        code.append(";\n");

        return 0;
    }

    private void appendType(Type type){
        if(type.getName().equals("int")) code.append(".i32");
        if(type.getName().equals("bool")) code.append(".bool");
        else{
            code.append(".").append(type.getName());
        }
    }

    // TODO: fix the 3 addresses problem -> expressions can only have 2 variables
    private void expressionParser(JmmNode node){

        // base case 1
        if(node.getKind().equals("BooleanLiteral")){
            code.append(node.get("boolean"));
            code.append(".bool");
        }
        // base case 2
        else if(node.getKind().equals("IntegerLiteral")){
            code.append(node.get("int"));
            code.append(".i32");
        }
        // base case 3
        else if(node.getKind().equals("Id")){
            if(!hasPoint(node) && !checkArray(node)){//and id not for array access
                Type type = exprVisit.expressionRec(node, false, false, true, true);
                code.append(node.get("value")).append(type.toOllir());
            }
        }
        // base case 4
        else if(node.getKind().equals("New")){
            newVisit(node);
        }
        else if(node.getKind().equals("Array")){
            arrayVisit(node);
        }
        else if(node.getKind().equals("Point")){
            methodCallVisit(node);
        }
        // recursive steps
        else if(node.getKind().equals("Expression")) {
            for(int i=0; i<node.getNumChildren(); i++){
                if(node.getJmmChild(i).equals("Id")) {
                    if((i+1)<node.getNumChildren() && node.getJmmChild(i+1).equals("Point")) {
                        expressionParser(node.getJmmChild(i + 1));
                    }
                }
                else{
                    expressionParser(node.getJmmChild(i));
                }
            }
        }
        else if (node.getKind().equals("Add") || node.getKind().equals("Or") || node.getKind().equals("Sub") || node.getKind().equals("Mult") || node.getKind().equals("Div")){
            code.append(threeAddressArithmetic(node));
        }
        else if(node.getKind().equals("And")){
            expressionParser(node.getJmmChild(0));
            code.append(" &&");
            code.append(".bool ");
            expressionParser(node.getJmmChild(1));
        }
        else if(node.getKind().equals("Or")){
            expressionParser(node.getJmmChild(0));
            code.append(" ||");
            code.append(".bool ");
            expressionParser(node.getJmmChild(1));
        }
        else if(node.getKind().equals("Less")){
            expressionParser(node.getJmmChild(0));
            code.append(" <");
            code.append(".bool ");
            expressionParser(node.getJmmChild(1));
        }
        else{
            visit(node);
        }
    }

    public void arrayVisit(JmmNode node){
        if (node.getJmmChild(0).getKind().equals("Expression")){
            //code.append("arrayvisit");
            var id = node.getAncestor("Expression").get().getJmmChild(0);
            code.append(id.get("value"));
            code.append("[");
            code.append("at_"+usedArrayIndex+".i32");
            usedArrayIndex++;
            //expressionParser(node.getJmmChild(0));
            code.append("]");
            code.append((exprVisit.expressionRec(id, true, false, true, true)).toOllir());
        }
    }

    public void newVisit(JmmNode node){
        var nId = node.getJmmChild(0);
        if (nId.getKind().equals("Id")){
            var idtype = nId.get("type");
            if (node.getChildren().size() > 1){
                if(idtype.equals("int array")){
                    code.append("new(array, ");
                    visit(node.getJmmChild(1));
                    code.append(").array.i32");
                    return;
                }
                else{
                    code.append("new(").append(idtype).append(", ");
                    visit(node.getJmmChild(1));
                }
            }
            else{
                code.append("new(").append(idtype);
            }
            code.append(").");
            code.append(idtype);
        }
    }

    public void methodCallVisit(JmmNode node){
        var id = node.getJmmParent().getJmmChild(0);
        //verify if args need tree address
        if(node.getNumChildren()==0){
            if(node.get("name").equals("length")){
                code.append("arraylength(").append(id.get("value")).append(".array.i32).i32");
            }
        }
        else {
            var methodCall = node.getJmmChild(0);
            var arguments = node.getJmmChild(0).getChildren();
            //check if method is virual or static
            if (checkMethod(id.get("value"), methodCall.get("name"))) {
                code.append("invokestatic(");
                code.append(id.get("value"));
            }
            else{
                code.append("invokevirtual(");
                //verify type (methods with same name)
                code.append(id.get("value")).append(exprVisit.expressionRec(id,false,false, true, true).toOllir());
            }
            //code.append(id.get("value"))
            code.append(", \"").append(methodCall.get("name")).append("\"");

            for (int i = 0; i < arguments.size(); i++) {
                code.append(", ");
                expressionParser(arguments.get(i));
            }
            code.append(")");
            if(checkMethod(id.get("value"), methodCall.get("name"))){
                code.append(".V");
            }
            else {
                var returncode = symbolTable.getReturnType(methodCall.get("name"));
                if (returncode == null) {
                    if (node.getAncestor("Assignment").isPresent()) {
                        code.append(currassign);
                    } else {
                        code.append(".V");
                    }
                }
                else code.append(".").append(OllirUtils.getCode(returncode));
            }

            var semicolon= true;//verify if methodcall is inside expression (just assignment)
            if(node.getJmmParent().getKind().equals("Expression")){
                if(node.getJmmParent().getJmmParent().getKind().equals("Assignment")){
                    semicolon = false;
                }
            }
            if (semicolon) code.append(";\n");
        }
    }

    public String threeAddressArithmetic(JmmNode node){
        if(isOp(node)){
            //confirm if operands are arrays
            var t0 = "";
            var t1 = "";
            var expr = node.getJmmParent();
            if(expr.getNumChildren()>2) {
                if (expr.getJmmChild(1).getKind().equals("Array")) {
                    var id = expr.getJmmChild(0);
                    t0 += id.get("value") + "[";
                    //t0 += threeAddressArithmetic(expr.getJmmChild(1).getJmmChild(0).getJmmChild(0));
                    t0 += "at_"+usedArrayIndex+".i32";
                    t0 += ".i32]";
                    usedArrayIndex++;
                }
            }
            else if(expr.getNumChildren()>1){
                if(node.getJmmChild(0).getKind().equals("Array")){
                    t0 = expr.getJmmChild(0).get("value")+ "[";
                    //t0+=threeAddressArithmetic(node.getJmmChild(0).getJmmChild(0).getJmmChild(0));
                    t0 += "at_"+usedArrayIndex+".i32";
                    t0+=".i32]";
                    usedArrayIndex++;
                }
                else {
                    t0 = threeAddressArithmetic(expr.getJmmChild(0));
                }
            }
            else{
                t0 = threeAddressArithmetic(node.getJmmChild(0));
            }

            t1 = threeAddressArithmetic(node.getJmmChild(1));
            code.append("t_").append(threeIndex+1).append(".i32 :=.i32 ");
            code.append(t0).append(".i32");
            code.append(" ").append(opEx(node.getKind())).append(".i32").append(" ");
            code.append(t1).append(".i32;\n");
            threeIndex++;
            return "t_"+threeIndex;
        }
        else{
            if(node.getKind().equals("Id")){
                return node.get("value");
            }
            else if(node.getKind().equals("BooleanLiteral")){
                return node.get("bool");
            }
            else if(node.getKind().equals("IntegerLiteral")){
                return node.get("int");
            }
            else if(node.getKind().equals("Array")){
                String str = "";
                if (node.getJmmChild(0).getKind().equals("Expression")) {
                    var id = node.getJmmParent().getJmmChild(0);
                    str += id.get("value") + "[";
                    //str += threeAddressArithmetic(node.getJmmChild(0).getJmmChild(0));
                    str += "at_"+usedArrayIndex+".i32";
                    str += ".i32]";
                    usedArrayIndex++;
                }
                return str;
            }
            return "other";
        }
    }

    public boolean verifyThree(JmmNode node){
        if(isOp(node)){
            return true;
        }
        else{
            if(node.getKind().equals("Expression")) {
                for(int i=0;i<node.getNumChildren();i++) {
                    if (isOp(node.getJmmChild(i))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void threeArrayAcess(JmmNode node){
        if(node.getNumChildren() > 1) {
            for (int i = 0; i < node.getNumChildren(); i++) {
                if (node.getJmmChild(i).getKind().equals("Array")) {
                    code.append("at_").append(arrayIndex).append(".i32 :=.i32 ");
                    expressionParser(node.getJmmChild(i).getJmmChild(0));
                    code.append(";\n");
                    arrayIndex++;
                }
                else{
                    threeArrayAcess(node.getJmmChild(i));
                }
            }
        }
        else{
            if(node.getNumChildren()>0) {
                threeArrayAcess(node.getJmmChild(0));
            }
        }
    }


    private String opEx(String kind){
        if (kind.equals("Add")){
            return "+";
        }
        else if(kind.equals("Sub")){
            return "-";
        }
        else if(kind.equals("Mult")){
            return "*";
        }
        else {
            return "/";
        }
    }

    public boolean isOp(JmmNode node){
        return (node.getKind().equals("Add") || node.getKind().equals("Sub") || node.getKind().equals("Mult") || node.getKind().equals("Div"));
    }

    public boolean hasPoint(JmmNode node){
        var anc = node.getJmmParent();
        for(int i=0;i<anc.getNumChildren(); i++){
            if(anc.getJmmChild(i).getKind().equals("Point")){
                return true;
            }
        }
        return false;
    }

    public boolean checkArray(JmmNode node) {//check if node id is array and distinguish between accesses
        var expr = node.getAncestor("Expression").get();
        if (expr.getNumChildren()>1){
            var secchild = expr.getJmmChild(1);
            if(secchild.getNumChildren()>=1){
                if (secchild.getKind().equals("Array")){
                    return true;
                }
                else if (secchild.getJmmChild(0).getKind().equals("Array")){
                    return true;
                }
            }
            else if (secchild.getKind().equals("Array")){
                return true;
            }

        }
        else{
            var operation = node.getJmmParent();
            if (operation.getNumChildren()>1){
                if(operation.getJmmChild(1).getKind().equals("Array")){
                    return true;
                }
            }
        }
        return false;//return true if access
    }

    public boolean checkMethod(String obj, String call) {//true if is static
        return  symbolTable.lastStringImports().contains(obj);
    }
}
