package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OllirExpressionsUtils extends AJmmVisitor<Integer, OllirCode> {
    private static int localVariableCounter = 0;
    private final SymbolTable symbolTable;
    private int indentCounter;

    public OllirExpressionsUtils(SymbolTable symbolTable, int indentCounter) {
        this.symbolTable = symbolTable;
        this.indentCounter = indentCounter;

        addVisit("BinOp", this::binOpVisit);
        addVisit("Literal", this::literalVisit);
        addVisit("UnaryOp", this::unaryOpVisit);
        addVisit("Identifier", this::identifierVisit);
        addVisit("NewExp", this::newExpVisit);
        addVisit("CallExpression", this::callExpressionVisit);
        addVisit("Array", this::arrayOrIndexVisit);
        addVisit("Index", this::arrayOrIndexVisit);
    }

    private OllirCode binOpVisit(JmmNode jmmNode, Integer integer) {
        StringBuilder beforeCode = new StringBuilder();
        StringBuilder variable = new StringBuilder();

        JmmNode leftNode = jmmNode.getJmmChild(0);
        JmmNode rightNode = jmmNode.getJmmChild(1);

        OllirCode leftCode = visit(leftNode);
        OllirCode rightCode = visit(rightNode);

        StringBuilder leftVariable = leftCode.getVariable();
        StringBuilder rightVariable = rightCode.getVariable();

        beforeCode.append(leftCode.getBeforeCode()).append(rightCode.getBeforeCode());

        variable.append(OllirUtils.getVariableName(jmmNode));

        switch (jmmNode.get("op")) {
            case "lt":
                variable.append(".bool");
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=.bool ").append(leftVariable).append(" <.i32 ")
                        .append(rightVariable).append(";\n");
                break;
            case "and":
                variable.append(".bool");
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=.bool ").append(leftVariable).append(" &&.bool ")
                        .append(rightVariable).append(";\n");
                break;
            case "add":
                variable.append(".i32");
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=.i32 ").append(leftVariable).append(" +.i32 ")
                        .append(rightVariable).append(";\n");
                break;
            case "sub":
                variable.append(".i32");
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=.i32 ").append(leftVariable).append(" -.i32 ")
                        .append(rightVariable).append(";\n");
                break;
            case "mult":
                variable.append(".i32");
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=.i32 ").append(leftVariable).append(" *.i32 ")
                        .append(rightVariable).append(";\n");
                break;
            case "div":
                variable.append(".i32");
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=.i32 ").append(leftVariable).append(" /.i32 ")
                        .append(rightVariable).append(";\n");
                break;
        }

        return new OllirCode(beforeCode, variable);
    }

    private OllirCode literalVisit(JmmNode jmmNode, Integer integer) {
        StringBuilder variable = new StringBuilder();
        switch (jmmNode.get("type")) {
            case "int":
                variable.append(jmmNode.get("value")).append(".i32");
                break;
            case "boolean":
                switch (jmmNode.get("value")) {
                    case "true":
                        variable.append("1.bool");
                        break;
                    case "false":
                        variable.append("0.bool");
                        break;
                }
                break;
        }
        return new OllirCode(new StringBuilder(), variable);
    }

    private OllirCode unaryOpVisit(JmmNode jmmNode, Integer integer) {
        StringBuilder beforeCode = new StringBuilder();
        StringBuilder variable = new StringBuilder();
        OllirCode child = visit(jmmNode.getJmmChild(0));

        switch (jmmNode.get("op")) {
            case "not":
                variable.append(OllirUtils.getVariableName(jmmNode)).append(".bool");
                beforeCode.append(child.getBeforeCode());
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=.bool !.bool ").append(child.getVariable()).append(";\n");
                break;
            case "length":
                variable.append(OllirUtils.getVariableName(jmmNode)).append(".i32");
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=.i32 arraylength(").append(child.getVariable()).append(").")
                        .append(OllirUtils.getTypeFromVariableName(child.getVariable().toString())).append(";\n");
                break;
            default:
                System.out.println("Not supposed to reach here");
        }

        return new OllirCode(beforeCode, variable);
    }

    private OllirCode identifierVisit(JmmNode jmmNode, Integer integer) {
        StringBuilder beforeCode = new StringBuilder();
        StringBuilder variable = new StringBuilder();

        variable.append(OllirUtils.getIdentifierCode(jmmNode, symbolTable));

        List<Symbol> parameters = symbolTable.getParameters(OllirUtils.getParentMethodSignature(jmmNode));
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i).getName().equals(jmmNode.get("name"))) {
                StringBuilder newVariable = new StringBuilder("$").append(i + 1).append(".").append(variable);
                variable = new StringBuilder(newVariable);
            }
        }

        if (jmmNode.getOptional("field").isPresent()) {
            if (!(jmmNode.getJmmParent().getKind().equals("AssignmentStatement")
                    && jmmNode.getJmmParent().getJmmChild(0).equals(jmmNode))) {
                return OllirUtils.getField(variable.toString(), this.indentCounter);
            }
        }

        return new OllirCode(beforeCode, variable);
    }

    private OllirCode arrayOrIndexVisit(JmmNode jmmNode, Integer integer) {
        StringBuilder beforeCode = new StringBuilder();
        StringBuilder variable = new StringBuilder();

        OllirCode arrayCode = OllirUtils.getArrayOrIndexVar(jmmNode, this.symbolTable, this.indentCounter);
        beforeCode.append(arrayCode.getBeforeCode());
        variable.append(arrayCode.getVariable());

        return new OllirCode(beforeCode, variable);
    }

    private OllirCode newExpVisit(JmmNode jmmNode, Integer integer) {
        StringBuilder beforeCode = new StringBuilder();
        StringBuilder variable = new StringBuilder();

        OllirCode ollirCode = visit(jmmNode.getJmmChild(0));
        beforeCode.append(ollirCode.getBeforeCode());
        variable.append(OllirUtils.getVariableName(jmmNode));

        String type = "";
        switch (jmmNode.get("type")) {
            case "intArray":
                type = ".array.i32";
                variable.append(type);
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=").append(type).
                        append(" new(array, ").append(ollirCode.getVariable()).append(")").append(type).append(";\n");
                break;
            case "object":
                type = jmmNode.getJmmChild(0).get("name");
                variable.append(".").append(type);
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=.").append(type).
                        append(" new(").append(type).append(").").append(type).append(";\n");
                break;
        }


        return new OllirCode(beforeCode, variable);
    }

    private OllirCode callExpressionVisit(JmmNode jmmNode, Integer integer) {
        StringBuilder beforeCode = new StringBuilder();
        StringBuilder variable = new StringBuilder();

        JmmNode variableId = jmmNode.getJmmChild(0);
        JmmNode functionId = jmmNode.getJmmChild(1);
        JmmNode arguments = jmmNode.getJmmChild(2);

        String functionName = functionId.get("name");

        List<StringBuilder> argumentsList = new ArrayList<>();
        for (var argument : arguments.getChildren()) {
            OllirCode childOllir = visit(argument);
            beforeCode.append(childOllir.getBeforeCode());

            if(argument.getKind().equals("Array")){
                String type = OllirUtils.getTypeFromVariableName(childOllir.getVariable().toString());
                StringBuilder newVar = new StringBuilder(OllirUtils.getNewVariableName()).append(".").append(type);
                beforeCode.append("\t".repeat(indentCounter)).append(newVar).append(" :=.").append(type).append(" ").
                        append(childOllir.getVariable()).append(";\n");
                argumentsList.add(newVar);
            }
            else{
                argumentsList.add(childOllir.getVariable());
            }


        }
        String args = argumentsList.stream()
                .collect(Collectors.joining(", "));

        String retType;
        switch (variableId.getKind()) {
            case "ThisT":
                beforeCode.append("\t".repeat(indentCounter));
                if (symbolTable.getMethods().contains(functionName)) {
                    retType = OllirUtils.getCode(symbolTable.getReturnType(functionName));
                    variable.append(OllirUtils.getVariableName(jmmNode)).append(".").append(retType);
                    beforeCode.append(variable).append(" :=.").append(retType).append(" ");
                } else {
                    retType = OllirUtils.getTypeFromUnknown(jmmNode, symbolTable);
                    if (!retType.equals("V")) {
                        variable.append(OllirUtils.getVariableName(jmmNode)).append(".").append(retType);
                        beforeCode.append(variable).append(" :=.").append(retType).append(" ");
                    }
                }
                beforeCode.append("invokevirtual(this, \"").append(functionName).append("\"").
                        append(args.isEmpty() ? "" : ", ").append(args).append(").").append(retType).append(";\n");

                break;
            default:
                if (variableId.getOptional("name").isPresent()) {
                    beforeCode.append("\t".repeat(indentCounter));
                    String name = variableId.get("name");
                    if (symbolTable.getImports().contains(name)) {
                        retType = OllirUtils.getTypeFromUnknown(jmmNode, symbolTable);
                        if (!retType.equals("V")) {
                            variable.append(OllirUtils.getVariableName(jmmNode)).append(".").append(retType);
                            beforeCode.append(variable).append(" :=.").append(retType).append(" ");
                        }
                        beforeCode.append("invokestatic(").append(name).append(", \"").append(functionName).append("\"").
                                append(args.isEmpty() ? "" : ", ").append(args).append(").").
                                append(retType).append(";\n");
                    } else if (symbolTable.getMethods().contains(functionName)) {
                        retType = OllirUtils.getCode(symbolTable.getReturnType(functionName));
                        variable.append(OllirUtils.getVariableName(jmmNode)).append(".").append(retType);
                        beforeCode.append(variable).append(" :=.").append(retType).append(" ").
                                append("invokevirtual(").
                                append(OllirUtils.getIdentifierCode(variableId, symbolTable)).append(", \"").
                                append(functionName).append("\"").append(args.isEmpty() ? "" : ", ").append(args).
                                append(").").append(retType).append(";\n");
                    } else {
                        retType = OllirUtils.getTypeFromUnknown(jmmNode, symbolTable);
                        if (!retType.equals("V")) {
                            variable.append(OllirUtils.getVariableName(jmmNode)).append(".").append(retType);
                            beforeCode.append(variable).append(" :=.").append(retType).append(" ");
                        }
                        beforeCode.append("invokevirtual(").
                                append(OllirUtils.getIdentifierCode(variableId, symbolTable)).
                                append(", \"").append(functionName).append("\"").
                                append(args.isEmpty() ? "" : ", ").append(args).append(").").
                                append(retType).append(";\n");
                    }
                } else {
                    OllirCode ollirCode = visit(variableId);
                    beforeCode.append(ollirCode.getBeforeCode());
                    retType = OllirUtils.getTypeFromUnknown(jmmNode, symbolTable);
                    beforeCode.append("\t".repeat(indentCounter));
                    if (!retType.equals("V")) {
                        variable.append(OllirUtils.getVariableName(jmmNode)).append(".").append(retType);
                        beforeCode.append(variable).append(" :=.").append(retType).append(" ");
                    }
                    beforeCode.append("invokevirtual(").append(ollirCode.getVariable()).
                            append(", \"").append(functionName).append("\"").
                            append(args.isEmpty() ? "" : ", ").append(args).append(").").
                            append(retType).append(";\n");
                }
                break;
        }

        return new OllirCode(beforeCode, variable);
    }
}
