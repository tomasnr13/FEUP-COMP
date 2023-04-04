package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTableBuilder implements SymbolTable {

    private final List<String> imports;
    private String className;
    private String superName;
    private List<Symbol> fields;
    private final List<String> methods;
    private final Map<String,Type> methodReturnType;
    private final Map<String,List<Symbol>> methodParameters;
    private final Map<String,List<Symbol>> methodLocalVar;

    public SymbolTableBuilder() {
        this.imports = new ArrayList<>();
        this.className = null;
        this.superName = null;
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.methodReturnType = new HashMap<>();
        this.methodParameters = new HashMap<>();
        this.methodLocalVar = new HashMap<>();
    }


    @Override
    public List<String> getImports() {
        return imports;
    }

    public void addImport(String importString) {
        this.imports.add(importString);
    }

    @Override
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String getSuper() {
        return superName;
    }

    public void setSuperName(String superName) {
        this.superName = superName;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    public void addField(Symbol field) {
        this.fields.add(field);
    }

    @Override
    public List<String> getMethods() {
        return methods;
    }

    public void addMethod(String methodSignature, Type returnType, List<Symbol> parameters, List<Symbol> localVar){
        this.methods.add(methodSignature);
        this.methodReturnType.put(methodSignature, returnType);
        this.methodParameters.put(methodSignature, parameters);
        this.methodLocalVar.put(methodSignature, localVar);
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return methodReturnType.get(methodSignature);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return methodParameters.get(methodSignature);
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return methodLocalVar.get(methodSignature);
    }

    public Boolean hasMethod(String methodSignature){
        return this.methods.contains(methodSignature);
    }

    public boolean containsName(final List<Symbol> list, final String name){
        if(list != null) return list.stream().anyMatch(o -> o.getName().equals(name));
        return false;
    }

    public List<String> lastStringImports(){
        List<String> lastStrings = new ArrayList<>();

        for(var imp : imports){
            String[] impSplitted = imp.split("\\.");
            String last;

            if(impSplitted.length==0) last = imp;
            else last = impSplitted[impSplitted.length-1];

            lastStrings.add(last);
        }
        return lastStrings;
    }

    public boolean isImportedMethod(String methodSignature){
        for(var m : methods){
            if(m.equals(methodSignature)) return true;
        }
        return false;
    }
}
