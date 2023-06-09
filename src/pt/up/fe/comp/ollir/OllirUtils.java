package pt.up.fe.comp.ollir;

import pt.up.fe.comp.Array;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
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

    /**
     * Get the method signature of the method the node belongs to
     * @param jmmNode
     * @return Method signature
     */
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

    /**
     * Gets a needed variable name for assignment
     * @param jmmNode
     * @return Variable name
     */
    public static String getVariableName(JmmNode jmmNode){
        switch (jmmNode.getJmmParent().getKind()) {
            case "AssignmentStatement":
                switch (jmmNode.getJmmParent().getJmmChild(0).getKind()) {
                    case "Identifier":
                        return jmmNode.getJmmParent().getJmmChild(0).get("name");
                }
            default:
                return getNewVariableName();
        }
    }

    /**
     * @return New temporary variable
     */
    public static String getNewVariableName(){
        return "t" + localVariableCount++;
    }

    /**
     * @param identifier
     * @param symbolTable
     * @return Identifier code
     */
    public static String getIdentifierCode(JmmNode identifier, SymbolTable symbolTable){
        String methodSignature = OllirUtils.getParentMethodSignature(identifier);
        if(methodSignature != null){
            for(var variable : symbolTable.getLocalVariables(methodSignature)) {
                if(variable.getName().equals(identifier.get("name"))){
                    return OllirUtils.getCode(variable);
                }
            }
            for(var variable : symbolTable.getParameters(methodSignature)) {
                if(variable.getName().equals(identifier.get("name"))){
                    return OllirUtils.getCode(variable);
                }
            }
        }

        for(var variable : symbolTable.getFields()){
            if(variable.getName().equals(identifier.get("name"))){
                identifier.put("field", "true");
                return OllirUtils.getCode(variable);
            }
        }

        return "V";
    }

    /**
     * Gets a variable type from the full code
     * @param name Variable full code in string
     * @return Variable type
     */
    public static String getTypeFromVariableName(String name){
        String[] separate = name.split("[.]");

        if(name.contains("array")){
            return "array." + separate[separate.length - 1];
        }
        else{
            return separate[separate.length - 1];
        }
    }

    /**
     * Gets the two variable names of an expression
     * @param expression
     * @return List with the two variable names as strings
     */
    public static List<String> getVarNamesFromExpression(String expression){
        List<String> list = new ArrayList<>();

        String[] parts = expression.split(" ");
        list.add(parts[2]);
        list.add(parts[4].replaceFirst(";", ""));

        return list;
    }

    /**
     * Gets the type for a needed expression
     * @param jmmNode Node that belongs to the expression
     * @param symbolTable
     * @return Type needed
     */
    public static String getTypeFromUnknown(JmmNode jmmNode, SymbolTable symbolTable) {
        JmmNode parent = jmmNode.getJmmParent();

        switch (parent.getKind()) {
            case "AssignmentStatement":
                switch (parent.getJmmChild(0).getKind()){
                    case "Identifier":
                        return OllirUtils.getTypeFromVariableName(OllirUtils.getIdentifierCode(parent.getJmmChild(0), symbolTable));
                    case "Index":
                        return OllirUtils.getTypeFromVariableName(OllirUtils.
                                getIdentifierCode(parent.getJmmChild(0).getJmmChild(0), symbolTable)).
                                toString().split("[.]")[1];
                }

            case "BinOp":
                switch (parent.get("op")) {
                    case "lt":
                    case "and":
                        return "bool";
                    default:
                        return "i32";
                }
            case "UnaryOp":
                return "bool";
            case "Identifier":
                return OllirUtils.getTypeFromVariableName(OllirUtils.getIdentifierCode(parent, symbolTable));
            default:
                return "V";
        }
    }

    /**
     * Gets the code for retrieval of a class field variable
     * @param field
     * @param indentCounter
     * @return OllirCode with getfield code in the 'beforeCode' and variable where it's stored in the 'variable'
     */
    public static OllirCode getField(String field, int indentCounter){
        StringBuilder beforeCode = new StringBuilder();
        StringBuilder variable = new StringBuilder();

        String type = OllirUtils.getTypeFromVariableName(field);

        variable.append(OllirUtils.getNewVariableName()).append(".").append(type);
        beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=.").append(type).
                append(" getfield(this, ").append(field).append(").").append(type).append(";\n");

        return new OllirCode(beforeCode, variable);
    }
}
