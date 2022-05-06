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

    /*
    TODO:
      Statements: If, While, Assign with Array on left side
      Expressions: Array, Finish CallExpression
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
        addVisit("IfCondition", this::ifConditionVisit);
        addVisit("IfBody", this::ifBodyVisit);
        addVisit("ElseStatement", this::elseStatementVisit);
        addVisit("AssignmentStatement", this::assignmentStatementVisit);

        addVisit("UnaryOp", this::unaryOpVisit);

        addVisit("VarDeclaration", this::varDeclarationVisit);
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

        code.append("\t".repeat(indentCounter)).append("}\n");
        this.indentCounter--;
        return 0;
    }

    private Integer instanceMethodVisit(JmmNode instanceMethod, Integer dummy) {
        String methodSignature = instanceMethod.getJmmChild(1).get("name");
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

        code.append("\t".repeat(indentCounter)).append("\n}\n");
        this.indentCounter--;

        return 0;
    }

    private Integer callExpressionVisit(JmmNode callExpression, Integer dummy){
        if(symbolTable.getImports().contains(callExpression.getJmmChild(0).get("name"))){
            code.append("\t".repeat(indentCounter)).append("invokestatic(");
            code.append(callExpression.getJmmChild(0).get("name"));
        }
        else { //this,
            code.append("\t".repeat(indentCounter)).append("invokevirtual(");
            switch (callExpression.getJmmChild(0).getKind()){
                case "ThisT":
                    code.append("this");
                    break;
                case "Literal":
                    break;
                case "Identifier":
                    break;
                default:
                    break;
            }
        }

        code.append(", \"").append(callExpression.getJmmChild(1).get("name")).append("\"");

        var arguments = callExpression.getJmmChild(2);
        for(var argument : arguments.getChildren()){

        }

        code.append(").");

        //Type
        if(symbolTable.getMethods().contains(callExpression.getJmmChild(1).get("name"))){ //Defined function -> Check symbol table
            code.append(OllirUtils.getCode(symbolTable.getReturnType(callExpression.getJmmChild(1).get("name"))));
        }
        else { //Unknown function -> void
            code.append("V");
        }

        code.append(";\n");
        return 0;
    }

    private Integer ifStatementVisit(JmmNode ifStatement, Integer dummy){
        return 0;
    }

    private Integer ifConditionVisit(JmmNode ifCondition, Integer dummy){
        return 0;
    }

    private Integer ifBodyVisit(JmmNode ifBody, Integer dummy){
        return 0;
    }

    private Integer elseStatementVisit(JmmNode elseStatement, Integer dummy){
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
                    code.append("\t".repeat(indentCounter)).append("ret.V");
                }
                else {
                    code.append("\t".repeat(indentCounter)).append("ret.");
                    switch (unaryOp.getJmmChild(0).getKind()){
                        case "Identifier":
                            String methodSignature = OllirUtils.getParentMethodSignature(unaryOp);
                            String return_type = OllirUtils.getCode(symbolTable.getReturnType(methodSignature));
                            code.append(return_type).append(" ");
                            code.append(unaryOp.getJmmChild(0).get("name")).append(".").append(return_type).append(";");
                    }
                }
                break;
            default:
                break;
        }
        return 0;
    }

    private Integer varDeclarationVisit(JmmNode varDeclaration, Integer dummy){
        /*
        String type = "";
        if(varDeclaration.getJmmChild(0).get("array") == "true"){
            type += ".array";
        }
        type += ("." + varDeclaration.getJmmChild(0).get("type"));
        String name = varDeclaration.getJmmChild(1).get("name");

        code.append(name).append(type).append(" :=").append(type).append(" new(").append(type, 1, type.length()).append(")").append(type).append(";\n");
        */
        return 0;
    }
}
