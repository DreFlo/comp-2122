package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import javax.swing.text.StyledEditorKit;
import java.util.ArrayList;
import java.util.List;

public class VisitorSemantic extends PostorderJmmVisitor<Object, Integer> {
    final SymbolTable symbolTable;
    final List<Report> reports;

    public List<Report> getReports() {
        return reports;
    }

    public VisitorSemantic (SymbolTable symbolTable){
        this.symbolTable = symbolTable;
        this.reports = new ArrayList<>();

        addVisit("UnaryOp", this::unaryOpVisit);
        addVisit("BinOp", this::binOpVisit);
        addVisit("CallExpression", this::callExpressionVisit);
        addVisit("AssignmentStatement", this::callAssignmentStatement);
        addVisit("IfCondition", this::ifConditionVisit);
    }
    private Integer ifConditionVisit(JmmNode node, Object dummy) {
        switch(node.getJmmChild(0).getKind()) {
            case "Literal":
                if(!node.get("type").equals("boolean")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                            Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                            "If Condition should be 'boolean'"));
                    return null;
                }
                break;
            case "Identifier":
                var typeI = getType(node);
                if(typeI == null) {
                    System.out.println("Error: ifConditionVisit - " + node.getJmmChild(0).getKind());
                    return null;
                }
                if(!typeI.equals("boolean")){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                            Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                            "If Condition should be 'boolean'"));
                    return null;
                }
                break;
            case "BinOp":
                var typeB = getType(node);
                if(typeB == null) {
                    System.out.println("Error: ifConditionVisit - " + node.getJmmChild(0).getKind());
                    return null;
                }
                if(!typeB.equals("bool")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                            Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                            "If Condition should be 'boolean'"));
                    return null;
                }
                break;
            default:
                System.out.println("Not Implemented Yet: ifConditionVisit - " + node.getJmmChild(0).getKind());
                return null;
        }
        return 0;
    }
    private Integer binOpVisit(JmmNode node, Object dummy) {

        /* Get importants node items */
        var op = node.get("op");
        var child0 = node.getJmmChild(0);
        if (!checkVariableDeclaration(child0)) return null;
        var child1 = node.getJmmChild(1);
        if (!checkVariableDeclaration(child1)) return null;

        /* Get Method from BinOp */
        var parentNode = getParentMethodSignature(node);
        if (parentNode == null) return null;

        /* Check op and types */
        var child0Type = getType(child0);
        var child1Type = getType(child1);

        if(child0Type == null) return null;
        if(child1Type == null) return null;

        if (!child0Type.equals(child1Type)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("col")), "Operation with different arguments:'" + child0Type + "' and '" + child1Type + "'"));
            return -1;
        }

        /* Check operators and types */
        switch (op) {
            case "add":
            case "sub":
            case "mult":
            case "div":
            case "lt":
                if(!child0Type.equals("int")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("col")), "Operation '" + op + "' not allowed for type '" + child0Type + "'"));
                    return null;
                }
                break;
            case "or":
            case "and":
                if(!child0Type.equals("bool")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("col")), "Operation '" + op + "' not allowed for type '" + child0Type + "'"));
                    return null;
                }
                break;
            default:
                System.out.println("Not Implemented yet: binOpVisit - " + op);
                return -1;
        }

        return 0;
    }
    private Integer unaryOpVisit (JmmNode node, Object dummy) {
        var op = node.get("op");
        switch(op) {
            case "return":
                var child = node.getJmmChild(0);
                var returnType = this.symbolTable.getReturnType(getParentMethodSignature(child));

                switch (child.getKind()) {
                    case "Literal":
                        if(!returnType.getName().equals(child.get("type"))) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                                    Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                                    "Invalid return type. Expected: '"+returnType.getName()+"'"));
                            return null;
                        }
                        break;
                    case "Identifier":
                        if(!checkVariableDeclaration(child)) return -1;
                        var typeI = getType(child);
                        if (typeI == null) {
                            System.out.println("Error: unaryOpVisit - " + child.getKind());
                            return null;
                        }
                        if(!typeI.equals(returnType.getName())){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                                    Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                                    "Invalid return type. Expected: '"+returnType.getName()+"'"));
                            return null;
                        }
                        break;
                    case "CallExpression":
                    case "BinOp":
                        var typeB = getType(child);
                        if (typeB == null) {
                            System.out.println("Error: unaryOpVisit - " + child.getKind());
                            return null;
                        }
                        if(!typeB.equals(returnType.getName())) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                                    Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                                    "Invalid return type. Expected: '"+returnType.getName()+"'"));
                            return -1;
                        }
                        break;
                    case "Array":
                        if(!checkVariableDeclaration(child.getJmmChild(0))){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                                    Integer.parseInt(child.getJmmChild(0).get("line")), Integer.parseInt(child.getJmmChild(0).get("col")),
                                    child.getJmmChild(0).get("name") + " not declared"));
                            return null;
                        }
                        Boolean array = isArray(child.getJmmChild(0));
                        if(array == null) {
                            System.out.println("Array null somehow");
                            return null;
                        }
                        if(!array) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                                    Integer.parseInt(child.getJmmChild(0).get("line")), Integer.parseInt(child.getJmmChild(0).get("col")),
                                    child.getJmmChild(0).get("name") + " is not an array"));
                            return null;
                        }
                        break;

                    default:
                        System.out.println("Not implemented yet: return - unaryOpVisit - " + child.getKind());
                        break;
                }
                break;
            case "not":
            case "length":
            default:
                System.out.println("Not implemented yet: unaryOpVisit - " + node.getKind());
                return null;
        }

        return 0;
    }
    private Integer callExpressionVisit(JmmNode node, Object dummy) {

        /* Verify parameters */
        var methodArguments = node.getJmmChild(2).getChildren();
        if(methodArguments == null) return null;
        var methodParameters = this.symbolTable.getParameters(node.getJmmChild(1).get("name"));
        if(methodParameters == null) return null;

        /* Verify size of arguments */
        if (methodArguments.size() == methodParameters.size()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("col")), "Invalid number of Arguments"));
            return -1;
        }

        /* Get types of arguments */
        List<String> argumentType = new ArrayList<>();
        for(var argument: methodArguments) {
            argumentType.add(getType(argument));
        }

        for(var i = 0; i < argumentType.size(); i++) {
            if(!argumentType.get(i).equals(methodParameters.get(i).getType().getName())) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("col")),
                        "Invalid argument type. Expected:'" + methodParameters.get(i).getType().getName() + "'. Get:'" + argumentType.get(i) + "'"));
                return null;
            }
        }

        return 0;
    }

    private Integer callAssignmentStatement(JmmNode node, Object dummy) {

        var leftChild = node.getJmmChild(0);
        if(!checkVariableDeclaration(leftChild)) return null;
        var rightChild = node.getJmmChild(1);
        if(!checkVariableDeclaration(rightChild)) return null;

        var leftChildType = getType(leftChild);
        var rightChildType = getType(rightChild);
        if(leftChildType == null) {
            System.out.println("leftChildType with error");
            return null;
        }
        if(rightChildType == null) {
            System.out.println("rightChildType with error");
            return null;
        }


        switch (leftChild.getKind()){
            case "Literal":
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        Integer.parseInt(leftChild.get("line")), Integer.parseInt(leftChild.get("col")),
                        "Literal cannot be assigned"));
                return -1;
            case "Identifier":
                switch(rightChild.getKind()) {
                    case "Literal":
                    case "Identifier":
                        if(!leftChildType.equals(rightChildType)) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                                    Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                                    "Invalid assignment type"));
                            return -1;
                        }
                        break;
                    case "Index":
                        System.out.println("Not implemented: Index - callAssignmentStatement");
                        break;
                    case "NewExp":
                        if(!leftChildType.equals(getType(rightChild.getJmmChild(0)))) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                                    Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                                    "Invalid assignment type"));
                            return -1;
                        }
                        break;
                    default:
                        System.out.println("Not Implemented Yet: default - callAssignmentStatement");
                        return -1;
                }
                break;
            default:
                System.out.print("Add new options: callAssignmentStatement");
                return -1;
        }

        return 0;
    }


    /**
     * Get the type of any node kind
     * @param node
     * @return node type, "null" otherwise
     */
    private String getType(JmmNode node) {
        switch (node.getKind()) {
            case "Literal":
                return node.get("type");
            case "Identifier":
                var variables = getVariablesInScope(getParentMethodSignature(node));
                for(var variable: variables) {
                    if(node.get("name").equals(variable.getName())) {
                        if(variable.getType().isArray()) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                                    Integer.parseInt(node.get("col")), ""));
                            return null;
                        }
                        return variable.getType().getName();
                    }
                }
                break;
            case "BinOp":
            case "UnaryOp":
                return getTypeOfOp(node);
            case "CallExpression":
                return getTypeOfCall(node);
            case "NewExp":
                return getType(node.getJmmChild(0));
            case "IfCondition":
                return "boolean";
            default:
                System.out.println("Add new option: getType - " + node.getKind());
                return null;
        }
        return null;
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
     * Get all variables in scope on node
     * @param nodeSignature
     * @return All variables available in scope
     */
    private List<Symbol> getVariablesInScope(String nodeSignature) {
        var variables = this.symbolTable.getLocalVariables(nodeSignature);
        variables.addAll(this.symbolTable.getParameters(nodeSignature));
        variables.addAll(this.symbolTable.getFields());
        return variables;
    }

    /**
     * Verify if variable is in Symbol Table
     * @param variable
     * @return true if it is, false otherwise
     */
    private Boolean checkVariableOnSymbolTable(JmmNode variable) {
        var variables = getVariablesInScope(getParentMethodSignature(variable));
        for (var variableTemp: variables) {
            if (variable.get("name").equals(variableTemp.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verify if variable is a import
     * @param variable
     * @return
     */
    private Boolean checkImport(JmmNode variable) {
        var imports = this.symbolTable.getImports();
        for(var import_: imports) {
            if (variable.get("name").equals(import_)) {
                return true;
            }
        }
        return false;
    }
    private String getTypeOfOp(JmmNode node) {

        switch(node.getKind()) {
            case "UnaryOp":
                switch(node.get("op")) {
                    case "return":
                        return this.symbolTable.getReturnType(getParentMethodSignature(node)).getName();
                    case "length":
                        return "int";
                    case "not":
                        return "boolean";
                    default:
                        System.out.println("Not implemented yet: UnaryOp - getTypeOfOp");
                        return null;
                }

            case "BinOp":
                switch(node.getJmmParent().getKind()) {
                    case "UnaryOp":
                        return getTypeOfOp(node.getJmmParent());
                    case "IfCondition":
                        return "boolean";
                    default:
                        System.out.println("Not Implemented Yet: BinOp - getTypeOfOp");
                        return null;
                }
            default:
                System.out.println("Not supposed to be here: default - getTypeOfOp");
                return null;
        }
    }

    private Boolean isArray(JmmNode node) {
        var variables = getVariablesInScope(getParentMethodSignature(node));
        for(var variable: variables) {
            if (variable.getName().equals(node.get("name"))) {
                return variable.getType().isArray();
            }
        }
        return null;
    }
    private String getTypeOfCall(JmmNode node) {

        JmmNode lastIdentifier = null;

        for(var i = 0; i < node.getNumChildren(); i++ ) {
            lastIdentifier = node.getJmmChild(i);
            if (!checkVariableDeclaration(lastIdentifier)) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                        lastIdentifier.get("name") + " not declared in scope"));
                return null;
            }

            if(node.getJmmChild(i+1).getKind().equals("Arguments"))
                break;
        }
        if(lastIdentifier == null) {
            System.out.println("Some error: getTypeofCall");
            return null;
        }

        return getType(lastIdentifier);
    }
    private Boolean checkVariableDeclaration(JmmNode variable) {
        switch(variable.getKind()) {
            case "NewExp":
                variable = variable.getJmmChild(0);
                if(variable.getKind().equals("Literal")) return true;
                break;
            case "Literal":
                return true;
            case "Index":
                JmmNode index = variable.getJmmChild(1);
                variable = variable.getJmmChild(0);
                if(checkVariableOnSymbolTable(variable))
                    return true;
                else {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                            Integer.parseInt(variable.get("line")), Integer.parseInt(variable.get("col")),
                            "Variable '" + variable.get("name") + "' not declared."));
                }
                if(checkVariableOnSymbolTable(index))
                    return true;
                else{
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                            Integer.parseInt(variable.get("line")), Integer.parseInt(variable.get("col")),
                            "Variable '" + index.get("name") + "' not declared."));
                }
                return false;


        }

        if(checkVariableOnSymbolTable(variable) || checkImport(variable))
            return true;

        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                Integer.parseInt(variable.get("line")), Integer.parseInt(variable.get("col")),
                "Variable '" + variable.get("name") + "' not declared."));
        return false;

    }
}

