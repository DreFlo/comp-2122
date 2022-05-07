package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<Integer, Integer> {
    private final StringBuilder code;
    private final SymbolTable symbolTable;
    private int indentCounter;

    /*
    TODO:
      Statements: Assign with Array on left side (Index)
      Expressions: Array
     */


    public OllirGenerator(SymbolTable symbolTable) {
        this.code = new StringBuilder();
        this.symbolTable = symbolTable;
        this.indentCounter = 0;

        //Start
        addVisit("Start", this::startVisit); //Adds imports

        //Class
        addVisit("ClassDeclaration", this::classDeclVisit);

        //Methods
        addVisit("MainMethod", this::mainMethodVisit);
        addVisit("InstanceMethod", this::instanceMethodVisit);

        //Expressions
        addVisit("CallExpression", this::callExpressionVisit);

        //Statements
        addVisit("IfStatement", this::ifStatementVisit);
        addVisit("WhileStatement", this::whileStatementVisit);
        addVisit("AssignmentStatement", this::assignmentStatementVisit);

        addVisit("UnaryOp", this::unaryOpVisit);
    }

    public String getCode() {
        return code.toString();
    }

    private Integer startVisit(JmmNode start, Integer dummy) {
        //Add imports
        for(String importString : symbolTable.getImports()) {
            code.append("import ").append(importString).append(";\n");
        }

        for(JmmNode child : start.getChildren()) {
            visit(child);
        }

        return 0;
    }

    private Integer classDeclVisit(JmmNode classDecl, Integer dummy) {
        code.append("public ").append(symbolTable.getClassName());
        String superClass = symbolTable.getSuper();
        if(superClass != null){
            code.append(" extends ").append(superClass);
        }

        code.append(" {\n");
        this.indentCounter++;

        for(var field : symbolTable.getFields()){
            code.append("\t".repeat(indentCounter)).append(".field private ").append(OllirUtils.getCode(field)).append(";\n");
        }

        code.append("\t".repeat(indentCounter)).append(".construct ").append(symbolTable.getClassName()).append("().V {\n").
                append("\t".repeat(indentCounter+1)).append("invokespecial(this, \"<init>\").V;\n").
                append("\t".repeat(indentCounter)).append("}\n");

        int index = 0;
        for(int i = 0; i < classDecl.getNumChildren(); i++){
            String childKind = classDecl.getJmmChild(i).getKind();
            if(childKind.equals("MainMethod") || childKind.equals("InstanceMethod")){
               index = i;
               break;
            }
        }

        for(JmmNode child : classDecl.getChildren().subList(index, classDecl.getNumChildren())){
            visit(child);
        }
        code.append("}\n");

        return 0;
    }

    private Integer mainMethodVisit(JmmNode mainMethod, Integer dummy) {
        String methodSignature = "main";
        int begIndentCounter = this.indentCounter;
        code.append("\t".repeat(indentCounter)).append(".method public static main(");

        //Add Arguments
        List<Symbol> params = symbolTable.getParameters(methodSignature);
        if(params != null){
            String paramCode = params.stream()
                    .map(symbol -> OllirUtils.getCode(symbol))
                    .collect(Collectors.joining(", "));
            code.append(paramCode);
        }

        code.append(").V {\n");
        this.indentCounter++;

        //Every child except Arguments
        for(int i = 1; i < mainMethod.getNumChildren(); i++){
            visit(mainMethod.getJmmChild(i));
        }

        code.append("\t".repeat(indentCounter)).append("ret.V;\n");

        this.indentCounter = begIndentCounter;
        code.append("\t".repeat(indentCounter)).append("}\n");

        return 0;
    }

    private Integer instanceMethodVisit(JmmNode instanceMethod, Integer dummy) {
        String methodSignature = instanceMethod.getJmmChild(1).get("name");
        int begIndentCounter = this.indentCounter;
        code.append("\t".repeat(indentCounter)).append(".method public ").append(methodSignature).append("(");

        //Add Arguments
        List<Symbol> params = symbolTable.getParameters(methodSignature);
        if(params != null){
            String paramCode = params.stream()
                    .map(symbol -> OllirUtils.getCode(symbol))
                    .collect(Collectors.joining(", "));
            code.append(paramCode);
        }

        code.append(").");

        //Return type
        code.append(OllirUtils.getCode(symbolTable.getReturnType(methodSignature))).append(" {\n");
        this.indentCounter++;

        //Every child except method information and arguments
        for(int i = 3; i < instanceMethod.getNumChildren(); i++){
            visit(instanceMethod.getJmmChild(i));
        }

        this.indentCounter = begIndentCounter;
        code.append("\t".repeat(indentCounter)).append("}\n");

        return 0;
    }

    private Integer callExpressionVisit(JmmNode callExpression, Integer dummy){
        OllirExpressionsUtils ollirExpressionsUtils = new OllirExpressionsUtils(symbolTable, indentCounter);
        OllirCode ollirCode = ollirExpressionsUtils.visit(callExpression);
        code.append(ollirCode.getBeforeCode());
        return 0;
    }

    private Integer ifStatementVisit(JmmNode ifStatement, Integer dummy){
        JmmNode ifCondition = ifStatement.getJmmChild(0);
        JmmNode ifBody = ifStatement.getJmmChild(1);
        JmmNode elseStatement = ifStatement.getJmmChild(2);

        OllirExpressionsUtils ollirExpressionsUtils = new OllirExpressionsUtils(symbolTable, indentCounter);
        OllirCode ollirCode = ollirExpressionsUtils.visit(ifCondition.getJmmChild(0));

        String[] codeLines = ollirCode.getBeforeCode().toString().split("\n");
        if(ifCondition.getJmmChild(0).getKind().equals("CallExpression")){
            code.append(ollirCode.getBeforeCode());
        }
        else{
            if(codeLines.length > 1){
                for(int i = 0; i < codeLines.length - 1; i++){
                    code.append(codeLines[i]).append("\n");
                }
            }
        }

        code.append("\t".repeat(indentCounter)).append("if (");
        switch (ifCondition.getJmmChild(0).getKind()){
            case "BinOp":
                String condition = codeLines[codeLines.length - 1];
                List<String> variables = OllirUtils.getVarNamesFromExpression(condition);
                switch (ifCondition.getJmmChild(0).get("op")){
                    case "lt":
                        code.append(variables.get(0)).append(" >=.bool ").append(variables.get(1));
                        break;
                    case "and":
                        code.append(variables.get(0)).append(" ||.bool ").append(variables.get(1));
                        break;
                }
                break;
            case "UnaryOp":
                switch (ifCondition.getJmmChild(0).get("op")){
                    case "not":
                        String[] parts = codeLines[codeLines.length - 1].split(" ");
                        String variable = parts[parts.length - 1].replace(";", "");
                        code.append(variable);
                        break;
                }
                break;
            case "Literal":
                switch (ifCondition.getJmmChild(0).get("value")){
                    case "true":
                        code.append("0.bool");
                        break;
                    case "false":
                        code.append("1.bool");
                        break;
                }
                break;
            case "Identifier":
            case "CallExpression":
                code.append("!.bool ").append(ollirCode.getVariable());
                break;
            default:
                System.out.println("This should be done in ifCondition: ");
                System.out.println(ifCondition.getJmmChild(0).getKind());
                break;
        }
        code.append(") goto else;\n");

        //Visit body
        this.indentCounter++;
        for(JmmNode child : ifBody.getChildren()){
            visit(child);
        }

        code.append("\t".repeat(indentCounter)).append("goto endif;\n");
        this.indentCounter--;

        //Visit else
        code.append("\t".repeat(indentCounter)).append("else:\n");
        this.indentCounter++;

        for(JmmNode child : elseStatement.getChildren()){
            visit(child);
        }

        this.indentCounter--;
        code.append("\t".repeat(indentCounter)).append("endif:\n");

        this.indentCounter++;
        return 0;
    }

    private Integer whileStatementVisit(JmmNode whileStatement, Integer dummy){
        JmmNode whileCondition = whileStatement.getJmmChild(0);
        JmmNode whileBody = whileStatement.getJmmChild(1);

        code.append("\t".repeat(indentCounter)).append("Loop:\n");
        this.indentCounter++;

        OllirExpressionsUtils ollirExpressionsUtils = new OllirExpressionsUtils(symbolTable, indentCounter);
        OllirCode ollirCode = ollirExpressionsUtils.visit(whileCondition.getJmmChild(0));
        System.out.println("Code:");
        System.out.println(ollirCode.getBeforeCode());
        System.out.println("Var");
        System.out.println(ollirCode.getVariable());
        System.out.println("-");

        String[] codeLines = ollirCode.getBeforeCode().toString().split("\n");
        if(whileCondition.getJmmChild(0).getKind().equals("CallExpression")){
            code.append(ollirCode.getBeforeCode());
        }
        else{
            if(codeLines.length > 1){
                for(int i = 0; i < codeLines.length - 1; i++){
                    code.append(codeLines[i]).append("\n");
                }
            }
        }

        code.append("\t".repeat(indentCounter)).append("if (");
        switch (whileCondition.getJmmChild(0).getKind()){
            case "BinOp":
                String condition = codeLines[codeLines.length - 1];
                List<String> variables = OllirUtils.getVarNamesFromExpression(condition);
                switch (whileCondition.getJmmChild(0).get("op")){
                    case "lt":
                        code.append(variables.get(0)).append(" <.bool ").append(variables.get(1));
                        break;
                    case "and":
                        code.append(variables.get(0)).append(" &&.bool ").append(variables.get(1));
                        break;
                }
                break;
            case "UnaryOp":
                switch (whileCondition.getJmmChild(0).get("op")){
                    case "not":
                        String[] parts = codeLines[codeLines.length - 1].split(" ");
                        code.append(parts[parts.length - 2]).append(" ").append(parts[parts.length - 1].replace(";", ""));
                        break;
                }
                break;
            case "Literal":
            case "Identifier":
            case "CallExpression":
                code.append(ollirCode.getVariable());
                break;
            default:
                System.out.println("This should be done in whileCondition: ");
                System.out.println(whileCondition.getJmmChild(0).getKind());
                break;
        }
        code.append(") goto Body;\n");
        code.append("\t".repeat(indentCounter)).append("goto EndLoop;\n");

        this.indentCounter--;
        code.append("\t".repeat(indentCounter)).append("Body:\n");

        this.indentCounter++;
        for(JmmNode child : whileBody.getChildren()){
            visit(child);
        }
        code.append("\t".repeat(indentCounter)).append("goto Loop;\n");

        this.indentCounter--;
        code.append("\t".repeat(indentCounter)).append("EndLoop:\n");

        this.indentCounter++;

        return 0;
    }

    private Integer assignmentStatementVisit(JmmNode assignmentStatement, Integer dummy){
        JmmNode rightSide = assignmentStatement.getJmmChild(1);

        OllirExpressionsUtils ollirExpressionsUtils = new OllirExpressionsUtils(symbolTable, indentCounter);
        OllirCode ollirCode = ollirExpressionsUtils.visit(rightSide);
        code.append(ollirCode.getBeforeCode());
        switch (rightSide.getKind()){
            case "Literal":
                JmmNode id = assignmentStatement.getJmmChild(0);
                OllirCode leftOllir = ollirExpressionsUtils.visit(id);
                code.append("\t".repeat(indentCounter)).append(leftOllir.getVariable()).append(" :=.").
                        append(OllirUtils.getTypeFromVariableName(leftOllir.getVariable().toString())).append(" ").
                        append(ollirCode.getVariable()).append(";\n");
                break;
            default:
                //code.append(ollirCode.getBeforeCode());
                break;
        }


        return 0;
    }

    //Need to add rest of op's
    private Integer unaryOpVisit(JmmNode unaryOp, Integer dummy){

        switch (unaryOp.get("op")){
            case "not":
                /*System.out.println("NOT unaryOp");
                OllirExpressionsUtils ollirExpressionsUtils = new OllirExpressionsUtils(symbolTable, indentCounter);
                OllirCode ollirCode = ollirExpressionsUtils.visit(unaryOp);
                code.append(ollirCode.getBeforeCode());*/
                break;
            case "length":
                break;
            case "return":
                if(unaryOp.getChildren().isEmpty()){
                    code.append("\t".repeat(indentCounter)).append("ret.V\n");
                }
                else {
                    code.append("\t".repeat(indentCounter)).append("ret.");
                    switch (unaryOp.getJmmChild(0).getKind()){
                        case "Identifier":
                            String methodSignature = OllirUtils.getParentMethodSignature(unaryOp);
                            String return_type = OllirUtils.getCode(symbolTable.getReturnType(methodSignature));
                            code.append(return_type).append(" ");
                            code.append(unaryOp.getJmmChild(0).get("name")).append(".").append(return_type).append(";\n");
                    }
                }
                break;
            default:
                break;
        }
        return 0;
    }
}
