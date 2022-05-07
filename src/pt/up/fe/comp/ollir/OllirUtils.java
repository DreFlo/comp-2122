package pt.up.fe.comp.ollir;

import pt.up.fe.comp.Array;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            case "boolean":
                return "bool";
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
        return "V";
    }

    public static String getTypeFromVariableName(String name){
        String[] separate = name.split("[.]");
        return separate[separate.length - 1];
    }

    public static List<String> getVarNamesFromExpression(String expression){
        List<String> list = new ArrayList<>();

        String[] parts = expression.split(" ");
        list.add(parts[2]);
        list.add(parts[4].replaceFirst(";", ""));

        return list;
    }

    public static String getTypeFromUnknown(JmmNode jmmNode, SymbolTable symbolTable){
        JmmNode parent = jmmNode.getJmmParent();

        switch (parent.getKind()){
            case "AssignmentStatement":
                return OllirUtils.getIdentifierCode(parent.getJmmChild(0), symbolTable);
            case "BinOp":
                switch (parent.get("op")){
                    case "lt":
                    case "and":
                        return "bool";
                    default:
                        return "i32";
                }
            case "UnaryOp":
                return "bool";
            case "Identifier":
                return OllirUtils.getIdentifierCode(parent, symbolTable);
            default:
                return "V";
        }
    }
}
