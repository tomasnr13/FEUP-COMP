package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.*;
import java.util.stream.Collectors;

public class OllirToJasmin {

    private final ClassUnit classUnit;
    private final FunctionClassMap<Instruction,String> instructionMap;
    private final SymbolTable symbolTable;
    private int comparison_assignment_index = 0;

    public OllirToJasmin(ClassUnit classUnit, SymbolTable symbolTable) {
        this.classUnit = classUnit;
        this.symbolTable = symbolTable;
        this.instructionMap = new FunctionClassMap<>();
//        instructionMap.put(CallInstruction.class,this::getCode);
//        instructionMap.put(AssignInstruction.class,this::getCode);
//        instructionMap.put(PutFieldInstruction.class,this::getCode);
//        instructionMap.put(ReturnInstruction.class,this::getCode);
    }



    public String getFullyQualifiedName(String className) {
        for (var importString : classUnit.getImports()) {
            var splittedImport = importString.split("\\.");
            String lastName;
            if (splittedImport.length == 0) {
                lastName = importString;
            } else {
                lastName = splittedImport[splittedImport.length - 1];
            }
            if (lastName.equals(className)) {
                return importString.replace(".", "/");
            }
        }
        throw new RuntimeException("Could not find import for class '" + className + "'");
    }

    public String getCode() {
        var code = new StringBuilder();
        classUnit.buildVarTables();

        code.append(".class public ").append(classUnit.getClassName()).append("\n");
        String superFullName;
        if (classUnit.getSuperClass()==null) superFullName = "java/lang/Object";
        else superFullName = getFullyQualifiedName(classUnit.getSuperClass());
        code.append(".super ").append(superFullName).append("\n");
        for (var field : classUnit.getFields()) {
            code.append("\t.field ");
            if (!(field.getFieldAccessModifier()==AccessModifiers.DEFAULT)) code.append(field.getFieldAccessModifier().toString().toLowerCase()).append(" ");
            code.append(field.getFieldName()).append(" ").append(getJasminType(field.getFieldType())).append("\n");
        }
        code.append("\n");

        var method_init = classUnit.getMethods().stream().filter(y->y.getMethodName().equals(classUnit.getClassName())).collect(Collectors.toList());
        code.append(".method public <init>()V\n");
        code.append("\taload_0\n\tinvokenonvirtual ").append(superFullName).append("/<init>()V\n\treturn\n.end method\n\n");

        for (var method : classUnit.getMethods()) {
            code.append(getCode(method));
        }

        return code.toString();
    }

    public String getCode(Method method) {
        if (method.getMethodName().equals(classUnit.getClassName())) return "";

        var code = new StringBuilder();


        code.append(".method ");
        var accessModifiers = method.getMethodAccessModifier().name().toLowerCase();
        if (!accessModifiers.equals("default")) code.append(accessModifiers).append(" ");
        if (method.isStaticMethod()) code.append("static ");
        code.append(method.getMethodName()).append("(");
        var methodParamTypes = method.getParams().stream().map(y -> getJasminType(y.getType())).collect(Collectors.joining());
        code.append(methodParamTypes).append(")").append(getJasminType(method.getReturnType())).append("\n");
        code.append(".limit stack 89\n");
//        code.append(".limit locals 89\n");
//        var stack_size = 4;
        var locals_size = method.getVarTable().values().stream().mapToInt(v -> v.getVirtualReg()).max().orElse(88) + 1;
//        code.append(".limit stack ").append(stack_size).append("\n");
        code.append(".limit locals ").append(locals_size).append("\n");


        for (var instruction : method.getInstructions()){
            code.append(getCode(instruction, method));
        }

        code.append(".end method\n\n");
        return code.toString();
    }

    public String getJasminType(Type type) {
        if (type instanceof ArrayType) {
            var typeName = getJasminType(((ArrayType) type).getArrayType());
            if(typeName == null) return "L" + getFullyQualifiedName(((ArrayType) type).getElementClass()) + ";";
            else return "[" + typeName;
        }
        var single_type = getJasminType(type.getTypeOfElement());
        if (single_type == null) {
            if(((ClassType)type).getName().equals(classUnit.getClassName()))return "L"+((ClassType)type).getName()+";";
            else return "L" + getFullyQualifiedName(((ClassType)type).getName()) + ";";
        }
        else return single_type;
    }

    public String getJasminType(ElementType type) {
        switch (type) {
            default:
                return classUnit.getClassName();
            case STRING:
                return "Ljava/lang/String;";
            case VOID:
                return "V";
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z";
            case OBJECTREF:
                return null;
        }
    }

    public String getCode(Instruction instruction, Method method){
        var code = new StringBuilder();
        for (var label : method.getLabels(instruction))
            code.append(label).append(":\n");
        switch (instruction.getInstType()){
            case CALL: code.append(getCode((CallInstruction)instruction,method)); break;
            case ASSIGN: code.append(getCode((AssignInstruction) instruction,method)); break;
            case PUTFIELD: code.append(getCode((PutFieldInstruction) instruction,method)); break;
            case RETURN: code.append(getCode((ReturnInstruction) instruction,method)); break;
            case GETFIELD: code.append(getCode((GetFieldInstruction) instruction,method)); break;
            case NOPER: code.append(getCode((SingleOpInstruction) instruction,method)); break;
            case BINARYOPER: code.append(getCode((BinaryOpInstruction) instruction,method)); break;
            case GOTO: code.append(getCode((GotoInstruction) instruction,method)); break;
            case BRANCH: code.append(getCode((CondBranchInstruction) instruction,method)); break;
            case UNARYOPER: code.append(getCode((UnaryOpInstruction) instruction,method)); break;
            default: break;
        }
        return code.toString();
    }

    public String getCode(AssignInstruction instruction, Method method){
        var code = new StringBuilder();

        String right_hand_instruction = getCode(instruction.getRhs(),method);

        if(instruction.getDest() instanceof ArrayOperand){
            code.append(getStoreIntArray(instruction.getDest(),method));
            code.append(right_hand_instruction);
            code.append("iastore\n");
        }else{
            code.append(right_hand_instruction);
            code.append(getStoreInstruction(instruction.getDest(),method));
        }
        return code.toString();
    }

    public String getCode(CallInstruction instruction, Method method){
        var code = new StringBuilder();
        switch (instruction.getInvocationType()){
            default:
                throw new NotImplementedException(instruction.getInvocationType());
            case invokestatic:
                code.append(getCodeInvokeStatic(instruction, method));
                break;
            case invokespecial:
                code.append(getCodeInvokeSpecial(instruction, method));
                break;
            case NEW:
                code.append(getCodeNew(instruction, method));
                break;
            case invokevirtual:
                var caller = getLoadInstruction(instruction.getFirstArg(),method);
                code.append(caller);
                code.append(getCodeInvokeVirtual(instruction, method));
                if(caller.equals("aload_0\n"))code.append("pop\n");
                break;
            case arraylength:
                code.append(getCodeArrayLength(instruction, method));
        }

        return code.toString();
    }



    public String getCode(GotoInstruction instruction, Method method){
        var code = new StringBuilder();
        code.append("goto ").append(instruction.getLabel()).append("\n");

        return code.toString();
    }
    public String getCode(ReturnInstruction instruction, Method method){
        var code = new StringBuilder();
        if (!instruction.hasReturnValue()) return "return\n";
        code.append(getLoadInstruction(instruction.getOperand(),method));
        code.append("\n");
        switch (instruction.getOperand().getType().getTypeOfElement()){
            case INT32:
            case BOOLEAN:
                code.append("ireturn\n");
                break;
            case VOID:
                return "return\n";
            default:
                code.append("areturn\n");
                break;
        }
        return code.toString();
    }
    public String getCode(PutFieldInstruction instruction, Method method){
        var code = new StringBuilder();
        var first = instruction.getFirstOperand();
        var second = instruction.getSecondOperand();
        var third = instruction.getThirdOperand();
        code.append(getLoadInstruction(first,method));
        code.append(getLoadInstruction(third,method));
        code.append("putfield ").append(getJasminType(first.getType())).append("/");
        code.append(((Operand)second).getName()).append(" ").append(getJasminType(second.getType())).append("\n");

        return code.toString();
    }
    public String getCode(GetFieldInstruction instruction, Method method){
        var code = new StringBuilder();

        var first = instruction.getFirstOperand();
        var second = instruction.getSecondOperand();
        code.append(getLoadInstruction(first,method));
        code.append("getfield ").append(getJasminType(first.getType())).append("/").append(((Operand)second).getName());
        code.append(" ").append(getJasminType(second.getType())).append("\n");

        return code.toString();
    }
    public String getCode(UnaryOpInstruction instruction, Method method){
        var code = new StringBuilder();
        code.append(getLoadInstruction(instruction.getOperand(),method));
        switch (instruction.getOperation().getOpType()){
            case NOT:
            case NOTB:
                code.append("iconst_1\n");
                code.append("ixor\n");
                break;
        }

        return code.toString();
    }
    public String getCode(BinaryOpInstruction instruction, Method method){
        var code = new StringBuilder();
        var first = instruction.getLeftOperand();
        var second = instruction.getRightOperand();
        var op = instruction.getOperation();

        if(op.getOpType() == OperationType.NOT){
            code.append(getLoadInstruction(second, method));
            code.append("iconst_1\n");
            code.append("ixor\n");


            return code.toString();
        }

        code.append(getLoadInstruction(first, method));
        code.append(getLoadInstruction(second, method));

        switch (op.getOpType()){
            case ADD:
                code.append("iadd\n");
                break;
            case SUB:
                code.append("isub\n");
                break;
            case MUL:
                code.append("imul\n");
                break;
            case DIV:
                code.append("idiv\n");
                break;
            case AND:
                code.append("iand\n");
                break;
            case ANDB:
                code.append("iand\n");
                break;
            case LTH:
                var current_index = getComparisonAssignIndex();
                code.append("if_icmplt comparison_assign_begin_").append(current_index).append("\n");
                code.append("iconst_0\n");
                code.append("goto comparison_assign_end_").append(current_index).append("\n");
                code.append("comparison_assign_begin_").append(current_index).append(":\n");
                code.append("iconst_1\n");
                code.append("comparison_assign_end_").append(current_index).append(":\n");
                break;

            default:
                System.out.println("OPERATION MISSED: "+ op.getOpType());
                break;
        }


        return code.toString();
    }
    public String getCode(CondBranchInstruction instruction, Method method){
        var code = new StringBuilder();

        if (instruction instanceof SingleOpCondInstruction){
            var singleOpCond = (SingleOpCondInstruction) instruction;
            code.append(getCode(singleOpCond, method));
        } else {
            code.append(getCode((OpCondInstruction) instruction,method));
        }
        return code.toString();
    }

    public String getCode(OpCondInstruction instruction, Method method){
        var code = new StringBuilder();
        var operation = instruction.getCondition();
        if (operation instanceof UnaryOpInstruction){
            code.append(getCode((UnaryOpInstruction) operation,method));
            code.append("ifne ");
        } else {
            for (var operand: operation.getOperands()){
                code.append(getLoadInstruction(operand,method));
            }
            switch (operation.getOperation().getOpType()){
                case LTH:
                    code.append("if_icmplt ");
                    break;
                default:
                    System.out.println("Unknown operation '"+operation.getOperation()+"' in OpCondInstruction.");
                    break;
            }
        }
        code.append(instruction.getLabel()).append("\n");
        return code.toString();
    }

    public String getCode(SingleOpCondInstruction instruction, Method method){
        var code = new StringBuilder();
        code.append(getLoadInstruction(instruction.getCondition().getSingleOperand(),method));
        code.append("ifne ");
        code.append(instruction.getLabel()).append("\n");
        return code.toString();
    }
    public String getCode(SingleOpInstruction instruction, Method method){
        var code = new StringBuilder();
        if (instruction.getSingleOperand() instanceof ArrayOperand){
            code.append(getStoreIntArray(instruction.getSingleOperand(),method));
            code.append("iaload\n");
        }else{
            code.append(getLoadInstruction(instruction.getSingleOperand(),method));
        }

        return code.toString();
    }

    public String getCodeInvokeStatic(CallInstruction instruction, Method method){
        var code = new StringBuilder();
        for (var arg : instruction.getListOfOperands()){
            code.append(getLoadInstruction(arg, method));
        }


        var methodClass = ((Operand)instruction.getFirstArg()).getName();
        code.append("invokestatic ");
        code.append(getFullyQualifiedName(methodClass)).append("/");
        var calledMethod = ((LiteralElement)instruction.getSecondArg()).getLiteral();
        code.append(calledMethod.substring(1,calledMethod.length()-1));
        code.append("(");


        if (!instruction.getListOfOperands().isEmpty()){
            var operandString = instruction.getListOfOperands().stream().map(y->getJasminType(y.getType())).collect(Collectors.joining(""));
            code.append(operandString);
        }

        code.append(")");
        code.append(getJasminType(instruction.getReturnType())).append("\n");
        return code.toString();
    }

    public String getCodeInvokeSpecial(CallInstruction instruction, Method method){
        var code = new StringBuilder();


        code.append(getLoadInstruction(instruction.getFirstArg(),method));


        var methodClass = ((ClassType)instruction.getFirstArg().getType()).getName();
        code.append("invokespecial ");
        code.append(methodClass).append("/");
        var calledMethod = ((LiteralElement)instruction.getSecondArg()).getLiteral();
        code.append(calledMethod.substring(1,calledMethod.length()-1));
        code.append("(");


        if (!instruction.getListOfOperands().isEmpty()){
            var operandString = instruction.getListOfOperands().stream().map(y->getJasminType(y.getType())).collect(Collectors.joining(""));
            code.append(operandString);
        }

        code.append(")");
        code.append(getJasminType(instruction.getReturnType())).append("\n");
        return code.toString();
    }

    private String getCodeNew(CallInstruction instruction, Method method) {
        var code = new StringBuilder();

        if(instruction.getFirstArg().getType().getTypeOfElement()==ElementType.ARRAYREF){
            for (var op : instruction.getListOfOperands()){
                code.append(getLoadInstruction(op,method));
            }
            code.append("newarray int\n");
        } else {
            var class_name = getJasminType(instruction.getReturnType());
            class_name = class_name.substring(1,class_name.length()-1);
            code.append("new ").append(class_name).append("\n");
            code.append("dup\n");
        }
        return code.toString();
    }

    private Integer getArgCode(Element arg, Method method){
        var name = ((Operand)arg).getName();
        var descriptor = method.getVarTable().get(name);
        return descriptor.getVirtualReg();
    }

    private String getLoadInstruction(ElementType elementType){
        switch (elementType){
            case INT32:
            case BOOLEAN:
                return "iload ";
            default:
                return "aload ";
        }
    }

    private String getCodeInvokeVirtual(CallInstruction instruction, Method method) {
        var code = new StringBuilder();
        for (var arg : instruction.getListOfOperands()){
            code.append(getLoadInstruction(arg, method));
        }

        var methodClass = ((ClassType)instruction.getFirstArg().getType()).getName();
        code.append("invokevirtual ");
        code.append(methodClass).append("/");
        var calledMethod = ((LiteralElement)instruction.getSecondArg()).getLiteral();
        code.append(calledMethod.substring(1,calledMethod.length()-1));
        code.append("(");


        if (!instruction.getListOfOperands().isEmpty()){
            var operandString = instruction.getListOfOperands().stream().map(y->getJasminType(y.getType())).collect(Collectors.joining(""));
            code.append(operandString);
        }

        code.append(")");
        code.append(getJasminType(instruction.getReturnType())).append("\n");

        return code.toString();
    }

    private String getCodeArrayLength(CallInstruction instruction, Method method) {
        var code = new StringBuilder();
        code.append(getLoadInstruction(instruction.getFirstArg(),method));
        code.append("arraylength\n");

        return code.toString();
    }

    private String getLoadInstruction(Element element, Method method){
        var code = new StringBuilder();
        if (element.isLiteral()){
            code.append("ldc ").append(((LiteralElement)element).getLiteral().replaceAll("\"","")).append("\n");
            return code.toString();
        }
        var name = ((Operand)element).getName();

        if(name.equals("false"))code.append("iconst_0\n");
        else if(name.equals("true"))code.append("iconst_1\n");
        else{
            if (element.getType() instanceof ArrayType) code.append("aload");
            else switch (element.getType().getTypeOfElement()){
                default:
                    code.append("aload");
                    break;
                case INT32:
                case BOOLEAN:
                    code.append("iload");
                    break;
            }
            var index = method.getVarTable().get(name).getVirtualReg();
            if (index < 0 || index > 3) code.append(" ");
            else code.append("_");
            code.append(index).append("\n");

        }

        return code.toString();
    }


    private String getStoreInstruction(Element element, Method method){
        var code = new StringBuilder();
        switch (element.getType().getTypeOfElement()){
            case INT32:
            case BOOLEAN:
                code.append("istore");
                break;
            default:
                code.append("astore");
                break;
        }
        var name = ((Operand)element).getName();
        var index = method.getVarTable().get(name).getVirtualReg();
        if (index < 0 || index > 3) code.append(" ");
        else code.append("_");
        code.append(index).append("\n");
        return code.toString();
    }

    private String getStoreIntArray(Element element,Method method){
        var code = new StringBuilder();
        code.append("aload");
        var index = method.getVarTable().get(((Operand)element).getName()).getVirtualReg();
        if (index < 0 || index > 3) code.append(" ");
        else code.append("_");
        code.append(index).append("\n");

        for(var operand: ((ArrayOperand)element).getIndexOperands()){
            code.append(getLoadInstruction(operand,method));
        }

        return code.toString();
    }

    private int getComparisonAssignIndex(){
        return comparison_assignment_index++;
    }
}
