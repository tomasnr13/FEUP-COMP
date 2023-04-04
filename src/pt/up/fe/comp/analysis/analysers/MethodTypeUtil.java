package pt.up.fe.comp.analysis.analysers;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;

public class MethodTypeUtil {

    // returns the method type, but this function assumes the methodSignature passed actual exists
    public static Type getMethodType(JmmNode node, final String methodSignature){
        Type type = new Type("", false);

        var classNode = node.getAncestor("ClassDeclaration");

        List<JmmNode> methodDefinitions = new ArrayList<>();

        if(classNode.isPresent()){
            var classChildren = classNode.get().getChildren();

            for(var child : classChildren){
                if(child.getKind().equals("MethodDef")){
                    methodDefinitions.add(child);
                }
            }

            for(var methodDef : methodDefinitions){
                if(methodDef.getJmmChild(1).get("value").equals(methodSignature)){
                    String typeName = methodDef.getJmmChild(0).get("value");

                    if(typeName.contains("array")) return new Type("int", true);
                    else return new Type(typeName, false);
                }
            }
        }

        return type;
    }
}
