package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<Integer, Integer> {
    private final StringBuilder code;
    private final SymbolTable symbolTable;
    private int indentCounter;
    private int labelCounter;

    /*
    TODO:
      Expressions: Array
     */


    public OllirGenerator(SymbolTable symbolTable) {
        this.code = new StringBuilder();
        this.symbolTable = symbolTable;
        this.indentCounter = 0;
        this.labelCounter = 0;

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
        code.append(") goto else").append(this.labelCounter).append(";\n");

        //Visit body
        this.indentCounter++;
        for(JmmNode child : ifBody.getChildren()){
            visit(child);
        }

        code.append("\t".repeat(indentCounter)).append("goto endif").append(this.labelCounter).append(";\n");
        this.indentCounter--;

        //Visit else
        code.append("\t".repeat(indentCounter)).append("else").append(this.labelCounter).append(":\n");
        this.indentCounter++;

        for(JmmNode child : elseStatement.getChildren()){
            visit(child);
        }

        this.indentCounter--;
        code.append("\t".repeat(indentCounter)).append("endif").append(this.labelCounter).append(":\n");

        this.indentCounter++;
        this.labelCounter++;
        return 0;
    }

    private Integer whileStatementVisit(JmmNode whileStatement, Integer dummy){
        JmmNode whileCondition = whileStatement.getJmmChild(0);
        JmmNode whileBody = whileStatement.getJmmChild(1);

        code.append("\t".repeat(indentCounter)).append("Loop").append(this.labelCounter).append(":\n");
        this.indentCounter++;

        OllirExpressionsUtils ollirExpressionsUtils = new OllirExpressionsUtils(symbolTable, indentCounter);
        OllirCode ollirCode = ollirExpressionsUtils.visit(whileCondition.getJmmChild(0));

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
        code.append(") goto Body").append(this.labelCounter).append(";\n");
        code.append("\t".repeat(indentCounter)).append("goto EndLoop").append(this.labelCounter).append(";\n");

        this.indentCounter--;
        code.append("\t".repeat(indentCounter)).append("Body").append(this.labelCounter).append(":\n");

        this.indentCounter++;
        for(JmmNode child : whileBody.getChildren()){
            visit(child);
        }
        code.append("\t".repeat(indentCounter)).append("goto Loop").append(this.labelCounter).append(";\n");

        this.indentCounter--;
        code.append("\t".repeat(indentCounter)).append("EndLoop").append(this.labelCounter).append(":\n");

        this.indentCounter++;
        this.labelCounter++;

        return 0;
    }

    private Integer assignmentStatementVisit(JmmNode assignmentStatement, Integer dummy){
        JmmNode leftSide = assignmentStatement.getJmmChild(0);
        JmmNode rightSide = assignmentStatement.getJmmChild(1);

        OllirExpressionsUtils ollirExpressionsUtils = new OllirExpressionsUtils(symbolTable, indentCounter);
        OllirCode ollirCode = ollirExpressionsUtils.visit(rightSide);

        switch (leftSide.getKind()){
            case "Identifier":
                OllirCode leftOllir = ollirExpressionsUtils.visit(leftSide);
                if(rightSide.getKind().equals("Literal")) {
                    code.append(ollirCode.getBeforeCode());
                    code.append("\t".repeat(indentCounter));
                    if (leftSide.getOptional("field").isPresent()) {
                        code.append("putfield(this, ").append(leftOllir.getVariable()).append(", ").
                                append(ollirCode.getVariable()).append(").").
                                append(OllirUtils.getTypeFromVariableName(leftOllir.getVariable().toString()))
                                .append(";\n");
                    } else {
                        code.append(leftOllir.getVariable()).append(" :=.").
                                append(OllirUtils.getTypeFromVariableName(leftOllir.getVariable().toString())).append(" ").
                                append(ollirCode.getVariable()).append(";\n");
                    }
                    break;
                }
                else{
                    if (leftSide.getOptional("field").isPresent()) {
                        StringBuilder rightExp = new StringBuilder();
                        if(rightSide.getKind().equals("Identifier")) {
                            rightExp.append(".").
                                    append(OllirUtils.getTypeFromVariableName(ollirCode.getVariable().toString())).
                                    append(" ").append(ollirCode.getVariable()).append(";");
                        }
                        else{
                            String[] codeLines = ollirCode.getBeforeCode().toString().split("\n");
                            if(codeLines.length > 1){
                                for(int i = 0; i < codeLines.length - 1; i++){
                                    code.append(codeLines[i]).append("\n");
                                }
                            }
                            String[] assign = codeLines[codeLines.length - 1].split(":=");
                            rightExp.append(assign[1]);
                        }

                        String type = OllirUtils.getTypeFromVariableName(ollirCode.getVariable().toString());
                        StringBuilder newVar = new StringBuilder(OllirUtils.getNewVariableName()).append(".").
                                append(type);
                        code.append("\t".repeat(indentCounter)).append(newVar).append(" :=").append(rightExp).
                                append("\n");
                        code.append("\t".repeat(indentCounter)).append("putfield(this, ").
                                append(leftOllir.getVariable()).append(", ").append(newVar).append(").").append(type).
                                append(";\n");
                    }
                    else{
                        code.append(ollirCode.getBeforeCode());
                        if(rightSide.getKind().equals("Identifier") || rightSide.getKind().equals("Array")){
                            code.append("\t".repeat(indentCounter)).append(leftOllir.getVariable()).append(" :=.").
                                    append(OllirUtils.getTypeFromVariableName(leftOllir.getVariable().toString())).
                                    append(" ").append(ollirCode.getVariable()).append(";\n");
                        }
                    }
                }
                break;
            case "Index":
                JmmNode identifier = leftSide.getJmmChild(0);
                JmmNode expression = leftSide.getJmmChild(1);

                OllirCode identifierCode = ollirExpressionsUtils.visit(identifier);
                OllirCode expressionCode = ollirExpressionsUtils.visit(expression);

                code.append(identifierCode.getBeforeCode());
                code.append(expressionCode.getBeforeCode());

                String type = OllirUtils.getTypeFromVariableName(identifierCode.getVariable().toString());
                if(type.contains("array")){
                    type = type.split("[.]")[1];
                }

                StringBuilder variable = new StringBuilder();
                if(expression.getKind().equals("Identifier")){
                    variable.append(identifier.get("name")).append("[").append(expressionCode.getVariable()).
                            append("].").append(type);

                    code.append(ollirCode.getBeforeCode());
                }
                else{
                    StringBuilder tempVar = new StringBuilder(OllirUtils.getNewVariableName()).append(".").append(type);

                    variable.append(identifier.get("name")).append("[").append(tempVar).append("].").append(type);

                    code.append(ollirCode.getBeforeCode());

                    code.append("\t".repeat(indentCounter)).append(tempVar).append(" :=.").append(type).append(" ").
                            append(expressionCode.getVariable()).append(";\n");
                }

                List<Symbol> parameters = symbolTable.getParameters(OllirUtils.getParentMethodSignature(assignmentStatement));
                for (int i = 0; i < parameters.size(); i++) {
                    if (parameters.get(i).getName().equals(identifier.get("name"))) {
                        StringBuilder newVariable = new StringBuilder("$").append(i + 1).append(".").append(variable);
                        variable = new StringBuilder(newVariable);
                    }
                }

                code.append("\t".repeat(indentCounter)).append(variable).append(" :=.").append(type).append(" ").
                        append(ollirCode.getVariable()).append(";\n");

                break;
        }

        return 0;
    }

    //Need to add rest of op's
    private Integer unaryOpVisit(JmmNode unaryOp, Integer dummy){

        switch (unaryOp.get("op")){
            case "not":
                break;
            case "length":
                break;
            case "return":
                if(unaryOp.getChildren().isEmpty()){
                    code.append("\t".repeat(indentCounter)).append("ret.V\n");
                }
                else {
                    code.append("\t".repeat(indentCounter)).append("ret.");
                    JmmNode child = unaryOp.getJmmChild(0);
                    switch (child.getKind()){
                        case "Identifier":
                            String methodSignature = OllirUtils.getParentMethodSignature(unaryOp);
                            String return_type = OllirUtils.getCode(symbolTable.getReturnType(methodSignature));
                            code.append(return_type).append(" ");
                            code.append(child.get("name")).append(".").append(return_type).append(";\n");
                            break;
                        case "Literal":
                            switch (child.get("type")){
                                case "int":
                                    code.append("i32 ").append(child.get("value")).append(".i32").append(";\n");
                                    break;
                                case "boolean":
                                    code.append("bool ");
                                    switch (child.get("value")){
                                        case "true":
                                            code.append("1.bool");
                                            break;
                                        case "false":
                                            code.append("0.bool");
                                            break;
                                    }
                                    code.append(";\n");
                                    break;
                            }
                            break;
                    }
                }
                break;
            default:
                break;
        }
        return 0;
    }
}
