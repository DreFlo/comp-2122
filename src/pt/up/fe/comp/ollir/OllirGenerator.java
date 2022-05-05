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

    public OllirGenerator(SymbolTable symbolTable) {
        this.code = new StringBuilder();
        this.symbolTable = symbolTable;

        addVisit("Start", this::startVisit);
        addVisit("ClassDeclaration", this::classDeclVisit);
        addVisit("MainMethod", this::mainMethodVisit);
        addVisit("InstanceMethod", this::instanceMethodVisit);
    }

    public String getCode() {
        return code.toString();
    }

    private Integer startVisit(JmmNode start, Integer dummy) {
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

        System.out.println(methodSignature);
        List<Symbol> params = symbolTable.getParameters(methodSignature);
        if(params != null){
            String paramCode = params.stream()
                    .map(symbol -> OllirUtils.getCode(symbol))
                    .collect(Collectors.joining(", "));
            code.append(paramCode);
        }

        code.append(").V {\n");

        //Expressions
        for(int i = 1; i < mainMethod.getNumChildren(); i++){
            visit(mainMethod.getJmmChild(i));
        }

        code.append("}\n");
        return 0;
    }

    private Integer instanceMethodVisit(JmmNode instanceMethod, Integer dummy) {

        return 0;
    }

    private Integer callExpressionVisit(JmmNode callExpression, Integer dummy){
        StringBuilder stringBuilder = new StringBuilder();

        var children = callExpression.getChildren();
        if(symbolTable.getImports().contains(children.get(0).get("name"))){
            stringBuilder.append("invokestatic(");
            stringBuilder.append(children.get(0).get("name"));
        }
        else { //this,
            stringBuilder.append("invokevirtual(");
            switch (children.get(0).getKind()){
                case "ThisT":
                    stringBuilder.append("this");
                    break;
                case "Literal":
                    break;
                case "Identifier":
                    break;
                default:
                    break;
            }
        }

        stringBuilder.append(", \"").append(callExpression.getJmmChild(0).get("name")).append("\"");

        var argumentes = callExpression.getJmmChild(1);
        for(var argument : argumentes.getChildren()){

        }

        stringBuilder.append(")");

        return 0;
    }
}
