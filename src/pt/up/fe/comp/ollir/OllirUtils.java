package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.Arrays;

public class OllirUtils {
    private static int localVariableCount = 0;
    public static String getCode(Symbol symbol){
        return symbol.getName() + "." + getCode(symbol.getType());
    }

    public static String getCode(Type type){
        StringBuilder code = new StringBuilder();

        if(type.isArray()){
            code.append("array.");
        }
        code.append(getOllirType(type.getName()));
        return code.toString();
    }

    public static String getOllirType(String jmmType){
        switch (jmmType){
            case "void":
                return "V";
            case "int":
                return "i32";
            default:
                return jmmType;
        }
    }

    public static String getParentMethodSignature(JmmNode jmmNode){
        while (true){
            if(jmmNode.getKind().equals("InstanceMethod")){
                return jmmNode.getJmmChild(1).get("name");
            }
            else if(jmmNode.getKind().equals("MainMethod")){
                return "main";
            }
            else{
                jmmNode = jmmNode.getJmmParent();
                if(jmmNode == null) return null;
            }
        }
    }

    public static String getVariableName(JmmNode jmmNode){
        switch (jmmNode.getJmmParent().getKind()){
            case "AssignmentStatement":
                return jmmNode.getJmmParent().getJmmChild(0).get("name");
            default:
                return getNewVariableName();
        }
    }

    public static String getNewVariableName(){
        return "t" + localVariableCount++;
    }

    public static String getIdentifierCode(JmmNode identifier, SymbolTable symbolTable){
        String code = "";
        String methodSignature = OllirUtils.getParentMethodSignature(identifier);
        if(methodSignature != null){
            for(var variable : symbolTable.getLocalVariables(methodSignature)) {
                if(variable.getName().equals(identifier.get("name"))){
                    return OllirUtils.getCode(variable);
                }
            }
        }
        else{
            for(var variable : symbolTable.getFields()){
                if(variable.getName().equals(identifier.get("name"))){
                    return OllirUtils.getCode(variable);
                }
            }
        }
        return code;
    }

    public static String getTypeFromVariableName(String name){
        String[] separate = name.split("[.]");
        return separate[separate.length - 1];
    }
}
