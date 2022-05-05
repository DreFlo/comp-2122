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
    private int localVariableCounter;

    public OllirGenerator(SymbolTable symbolTable) {
        this.code = new StringBuilder();
        this.symbolTable = symbolTable;
        this.localVariableCounter = 0;

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
        for(JmmNode child : classDecl.getChildren()){
            visit(child);
        }
        code.append("}\n");

        return 0;
    }

    private Integer mainMethodVisit(JmmNode mainMethod, Integer dummy) {
        String methodSignature = "main";
        code.append(".method public static main(");

        //Add Arguments
        List<Symbol> params = symbolTable.getParameters(methodSignature);
        if(params != null){
            String paramCode = params.stream()
                    .map(symbol -> OllirUtils.getCode(symbol))
                    .collect(Collectors.joining(", "));
            code.append(paramCode);
        }

        code.append(").V {\n");

        //Every child except Arguments
        for(int i = 1; i < mainMethod.getNumChildren(); i++){
            visit(mainMethod.getJmmChild(i));
        }

        code.append("}\n");
        return 0;
    }

    private Integer instanceMethodVisit(JmmNode instanceMethod, Integer dummy) {
        String methodSignature = instanceMethod.getJmmChild(1).get("name");
        code.append(".method public ").append(methodSignature).append("(");

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

        //Every child except method information and arguments
        for(int i = 3; i < instanceMethod.getNumChildren(); i++){
            visit(instanceMethod.getJmmChild(i));
        }

        code.append("}\n");

        return 0;
    }

    private Integer callExpressionVisit(JmmNode callExpression, Integer dummy){
        if(symbolTable.getImports().contains(callExpression.getJmmChild(0).get("name"))){
            code.append("invokestatic(");
            code.append(callExpression.getJmmChild(0).get("name"));
        }
        else { //this,
            code.append("invokevirtual(");
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

    //Need to add rest of op's
    private Integer unaryOpVisit(JmmNode unaryOp, Integer dummy){

        switch (unaryOp.get("op")){
            case "return":

                break;
            default:
                break;
        }
        return 0;
    }
}
