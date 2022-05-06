package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class OllirExpressionsUtils extends AJmmVisitor<Integer, OllirCode> {
    private static int localVariableCounter = 0;
    private final SymbolTable symbolTable;
    private int indentCounter;

    public OllirExpressionsUtils(SymbolTable symbolTable, int indentCounter){
        this.symbolTable = symbolTable;
        this.indentCounter = indentCounter;

        addVisit("BinOp", this::binOpVisit);
        addVisit("Literal", this::literalVisit);
        addVisit("UnaryOp", this::unaryOpVisit);
        addVisit("Identifier", this::identifierVisit);
        addVisit("NewExp", this::newExpVisit);
    }

    private OllirCode binOpVisit(JmmNode jmmNode, Integer integer) {
        StringBuilder beforeCode = new StringBuilder();
        StringBuilder variable = new StringBuilder();

        JmmNode leftNode = jmmNode.getJmmChild(0);
        JmmNode rightNode = jmmNode.getJmmChild(1);

        OllirCode leftCode = visit(leftNode);
        OllirCode rightCode = visit(rightNode);

        beforeCode.append(leftCode.getBeforeCode()).append(rightCode.getBeforeCode());

        variable.append(OllirUtils.getVariableName(jmmNode));
        switch (jmmNode.get("op")){
            case "lt":
                variable.append(".bool");
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=.bool ").append(leftCode.getVariable()).append(" <.i32 ")
                        .append(rightCode.getVariable()).append(";\n");
                break;
            case "and":
                variable.append(".bool");
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=.bool ").append(leftCode.getVariable()).append(" &&.bool ")
                        .append(rightCode.getVariable()).append(";\n");
                break;
            case "add":
                variable.append(".i32");
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=.i32 ").append(leftCode.getVariable()).append(" +.i32 ")
                        .append(rightCode.getVariable()).append(";\n");
                break;
            case "sub":
                variable.append(".i32");
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=.i32 ").append(leftCode.getVariable()).append(" -.i32 ")
                        .append(rightCode.getVariable()).append(";\n");
                break;
            case "mult":
                variable.append(".int");
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=.i32 ").append(leftCode.getVariable()).append(" *.i32 ")
                        .append(rightCode.getVariable()).append(";\n");
                break;
            case "div":
                variable.append(".i32");
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=.i32 ").append(leftCode.getVariable()).append(" /.i32 ")
                        .append(rightCode.getVariable()).append(";\n");
                break;
        }

        return new OllirCode(beforeCode, variable);
    }

    private OllirCode literalVisit(JmmNode jmmNode, Integer integer) {
        StringBuilder variable = new StringBuilder(jmmNode.get("value"));
        switch (jmmNode.get("type")){
            case "int":
                variable.append(".i32");
                break;
            case "boolean":
                variable.append(".bool");
                break;
        }
        return new OllirCode(new StringBuilder(), variable);
    }

    private OllirCode unaryOpVisit(JmmNode jmmNode, Integer integer) {
        StringBuilder beforeCode = new StringBuilder();
        StringBuilder variable = new StringBuilder();
        OllirCode child = visit(jmmNode.getJmmChild(0));

        switch (jmmNode.get("op")){
            case "not":
                variable.append(OllirUtils.getVariableName(jmmNode)).append(".bool");
                beforeCode.append(child.getBeforeCode());
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=.bool !.bool ").append(child.getVariable()).append(";\n");
                break;
            case "length":
                variable.append(OllirUtils.getVariableName(jmmNode)).append(".i32");
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(" :=.i32 arraylength($1.").append(child.getVariable()).append(").")
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

        return new OllirCode(beforeCode, variable);
    }

    private OllirCode newExpVisit(JmmNode jmmNode, Integer integer) {
        StringBuilder beforeCode = new StringBuilder();
        StringBuilder variable = new StringBuilder();

        OllirCode ollirCode = visit(jmmNode.getJmmChild(0));
        beforeCode.append(ollirCode.getBeforeCode());
        variable.append(OllirUtils.getVariableName(jmmNode));

        String type = "";
        switch (jmmNode.get("type")){
            case "intArray":
                type = ".array.i32";
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(type).append(" :=").append(type).append(" new(array, ").
                        append(ollirCode.getVariable()).append(")").append(type).append(";\n");
                break;
            case "object":
                type = jmmNode.getJmmChild(0).get("name");
                beforeCode.append("\t".repeat(indentCounter)).append(variable).append(".").append(type).append(" :=.").append(type).append(" new(").
                        append(type).append(").").append(type).append(";\n");
                break;
        }


        return new OllirCode(beforeCode, variable);
    }
}
