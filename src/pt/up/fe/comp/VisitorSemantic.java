package pt.up.fe.comp;

import jdk.swing.interop.SwingInterOpUtils;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import javax.swing.text.StyledEditorKit;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;

public class VisitorSemantic extends AJmmVisitor<Object, Integer> {
    final SymbolTable symbolTable;
    final List<Report> reports;

    public List<Report> getReports() {
        return reports;
    }
    private void addReport(JmmNode node, String message) {
        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                message));
    }

    public VisitorSemantic (SymbolTable symbolTable){
        this.symbolTable = symbolTable;
        this.reports = new ArrayList<>();

        addVisit("UnaryOp", this::unaryOpVisit);
        addVisit("BinOp", this::binOpVisit);
        addVisit("CallExpression", this::callExpressionVisit);
        addVisit("AssignmentStatement", this::callAssignmentStatement);
        addVisit("IfCondition", this::ifConditionVisit);
        addVisit("WhileCondition", this::whileConditionStatement);

        setDefaultVisit(this::defaultVisit);
    }

    private Integer defaultVisit(JmmNode node, Object dummy) {

        for(var child: node.getChildren())
            visit(child, dummy);
        return 0;
    }

    private Integer whileConditionStatement(JmmNode node, Object dummy) {
        var child = node.getJmmChild(0);
        var childType = getType(child);
        if(childType == null) return null;
        if(!childType.equals("boolean")) {
            addReport(node.getJmmChild(0), "While condition should be a boolean");
            return null;
        }
        return 0;
    }
    private Integer ifConditionVisit(JmmNode node, Object dummy) {
        switch(node.getJmmChild(0).getKind()) {
            case "Literal":
                if(!node.get("type").equals("boolean")) {
                    addReport(node, "If Statement should be 'boolean'");
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
                    addReport(node, "If Statement should be 'boolean'");
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
                    addReport(node, "If Statement should be 'boolean'");
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
        /* Get important node items */

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
            addReport(node, "Operation with different arguments:'" + child0Type + "' and '" + child1Type + "'");
            return null;
        }

        switch(child0.getKind()) {
            case "Literal":
                if(!checkOperation(node.get("op"), child0)) return null;
                break;
            case "Identifier":
                if(isArray(child0)) {
                    addReport(child0, "Not supported operation for array");
                    return null;
                }
                if(!child1.getKind().equals("Literal") && isArray(child1)) {
                    addReport(child1, "Not supported operation for array");
                    return null;
                }
                if(!checkOperation(node.get("op"), child0)) return null;
                break;
            default:
                System.out.println("Add new options: binOpVisit - " + child0.getKind());
        }

        return 0;
    }

    private Boolean checkOperation(String op, JmmNode node) {
        var nodeType = getType(node);
        switch (op) {
            case "add":
            case "sub":
            case "mult":
            case "div":
            case "lt":
                if(!nodeType.equals("int")) {
                    addReport(node, "Operation '" + op + "' not allowed for type '" + nodeType + "'");
                    return false;
                }
                break;
            case "or":
            case "and":
                if(!nodeType.equals("boolean")) {
                    addReport(node, "Operation '" + op + "' not allowed for type '" + nodeType + "'");
                    return false;
                }
                break;
            default:
                System.out.println("Not Implemented yet: binOpVisit - " + op);
                return false;
        }
        return true;
    }
    private Integer unaryOpVisit (JmmNode node, Object dummy) {
        var op = node.get("op");
        switch (op) {
            case "return" -> {
                var child = node.getJmmChild(0);

                var returnType = this.symbolTable.getReturnType(getParentMethodSignature(child));
                switch (child.getKind()) {
                    case "Literal":
                        if (!returnType.getName().equals(child.get("type"))) {
                            addReport(node, "Invalid return type. Expected: '" + returnType.getName() + "'");
                            return null;
                        }
                        break;
                    case "Identifier":
                        if (!checkVariableDeclaration(child)) return null;
                        var typeI = getType(child);
                        if (typeI == null) {
                            System.out.println("Error: unaryOpVisit - " + child.getKind());
                            return null;
                        }
                        if (!typeI.equals(returnType.getName())) {
                            addReport(node, "Invalid return type. Expected: '" + returnType.getName() + "'");
                            return null;
                        }
                        break;
                    case "CallExpression":
                        if(checkClass(child)) {
                            var ceType = getType(child);
                            if(ceType == null) return null;
                            if(!ceType.equals(returnType.getName())){
                                addReport(child, "Invalid Return Type");
                            }
                            break;
                        }
                        break;
                    case "BinOp":
                        var typeB = getType(child);
                        if (typeB == null) {
                            System.out.println("Error: unaryOpVisit - " + child.getKind());
                            return null;
                        }
                        if (!typeB.equals(returnType.getName())) {
                            addReport(node, "Invalid return type. Expected: '" + returnType.getName() + "'");
                            return -1;
                        }
                        break;
                    case "Array":
                        if(!checkVariableDeclaration(child)) return null;
                        var childA = child.getJmmChild(0);
                        if (!checkVariableDeclaration(childA)) {
                            addReport(childA, childA.get("name") + " not declared");
                            return null;
                        }
                        Boolean array = isArray(child.getJmmChild(0));
                        if (array == null) {
                            System.out.println("Array null somehow");
                            return null;
                        }
                        if (!array) {
                            addReport(child, child.getJmmChild(0).get("name") + " is not an array");
                            return null;
                        }
                        break;

                    default:
                        System.out.println("Not implemented yet: return - unaryOpVisit - " + child.getKind());
                        break;
                }
                visit(child, dummy);
            }
            case "not" -> {
                var childN = node.getJmmChild(0);
                var typeN = getType(childN);
                if (typeN == null) return null;
                if (!typeN.equals("boolean")) {
                    addReport(childN, "Expression not a boolean");
                    return null;
                }
                visit(childN, dummy);
            }
            case "length" -> {
                var childL = node.getJmmChild(0);
                if (!isArray(childL)) {
                    addReport(childL, childL.get("name") + " is not an array");
                    return null;
                }
                break;
            }
            default -> {
                System.out.println("Not implemented yet: unaryOpVisit - " + node.get("op"));
                return null;
            }
        }

        return 0;
    }
    private Integer callExpressionVisit(JmmNode node, Object dummy) {
        List<JmmNode> identifiers = new ArrayList<>();
        JmmNode argument = null;

        for(var child: node.getChildren()) {
            if(child.getKind().equals("Arguments")) {
                argument = child;
                break;
            } else {
                identifiers.add(child);
            }
        }

        if(argument == null) return null;

        switch(identifiers.size()) {
            case 1:
                if(!checkVariableDeclaration(identifiers.get(0))) {
                    addReport(identifiers.get(0), "Function not declared");
                    return null;
                }
                if(!checkArguments(argument, identifiers)) return null;
                break;
            case 2:
                if(!checkClass(identifiers.get(0))) {
                    if(!checkVariableDeclaration(identifiers.get(0))) {
                        addReport(identifiers.get(0), "Not accessible");
                        return null;
                    }
                } else {
                    if(this.symbolTable.getSuper() == null) {
                        if(!checkVariableDeclaration(identifiers.get(1))){
                            addReport(identifiers.get(1), "Not declared");
                            return null;
                        }
                    }
                }
                break;
            default:
                System.out.println("callExpressionVisit(identifiers.size()) - " + identifiers.size());
                return null;
        }



        return 0;
    }

    private Boolean checkArguments(JmmNode argument, List<JmmNode> identifiers) {
        /* Verify parameters */
        var methodArguments = argument.getChildren();
        if(methodArguments == null) return false;
        var methodParameters = this.symbolTable.getParameters(identifiers.get(identifiers.size()-1).get("name"));
        if(methodParameters == null) return false;



        /* Verify size of arguments */
        if (methodArguments.size() == methodParameters.size()) {
            addReport(argument, "Invalid number of Arguments");
            return false;
        }

        /* Get types of arguments */
        List<String> argumentType = new ArrayList<>();
        for(var argument_: methodArguments) {
            argumentType.add(getType(argument_));
        }

        for(var i = 0; i < argumentType.size(); i++) {
            if(!argumentType.get(i).equals(methodParameters.get(i).getType().getName())) {
                addReport(argument, "Invalid argument type. Expected:'" + methodParameters.get(i).getType().getName() + "'. Get:'" + argumentType.get(i) + "'");
                return false;
            }
        }
        return true;
    }

    private Integer callAssignmentStatement(JmmNode node, Object dummy) {

        var leftChild = node.getJmmChild(0);
        if(!checkVariableDeclaration(leftChild)) return null;
        var rightChild = node.getJmmChild(1);
        if(!checkVariableDeclaration(rightChild)) return null;

        var leftChildType = getType(leftChild);
        var rightChildType = getType(rightChild);
        if(leftChildType == null) {
            return null;
        }
        if(rightChildType == null) {
            return null;
        }


        switch (leftChild.getKind()){
            case "Literal":
                addReport(leftChild, "Literal cannot be assigned");
                return null;
            case "Identifier":
                switch(rightChild.getKind()) {
                    case "BinOp":
                    case "UnaryOp":
                    case "Literal":
                    case "Identifier":
                        if(!leftChildType.equals(rightChildType)) {
                            if(checkImport(leftChild) && checkImport(rightChild)) break;
                            if((checkSuperclass(leftChild) || checkClass(leftChild)) &&
                                    (checkSuperclass(rightChild)) || checkClass(rightChild)) {
                                break;
                            }
                            addReport(node, "Invalid assignment type");
                            return null;
                        }
                        break;
                    case "Index":
                        System.out.println("Not implemented: Index - callAssignmentStatement");
                        break;
                    case "NewExp":
                        if(!leftChildType.equals(getType(rightChild.getJmmChild(0)))) {
                            System.out.println(leftChildType + "\t" + getType(rightChild.getJmmChild(0)));
                            addReport(node, "Invalid assignment type");
                            return null;
                        }
                        break;
                    default:
                        System.out.println("Not Implemented Yet: default - callAssignmentStatement - " + rightChild.getKind());
                        return null;
                }
                break;
            default:
                System.out.print("Add new options: callAssignmentStatement");
                return null;
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
                        return variable.getType().getName();
                    }
                }
                break;
            case "BinOp":
            case "UnaryOp":
                return getTypeOfOp(node);
            case "CallExpression":
                return getTypeOfCall(node);
            case "AssignmentStatement":
                return getTypeOfAssignment(node);
            case "NewExp":
            case "Array":
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

    private Boolean checkSuperclass(JmmNode node) {
        var export = this.symbolTable.getSuper();;
        if(export == null) return false;
        return export.equals(getType(node));
    }

    private Boolean checkClass (JmmNode node) {
        var class_ = this.symbolTable.getClassName();
        if(class_ == null) return false;
        for(var atr: node.getAttributes()) {
            if(atr.equals("name")) {
                return class_.equals(getType(node)) || class_.equals(node.get("name"));
            }
        }
        return class_.equals(getType(node));
    }
    /**
     * Verify if variable is a import
     * @param variable
     * @return
     */
    private Boolean checkImport(JmmNode variable) {
        var imports = this.symbolTable.getImports();
        for(var import_: imports) {
            var type = getType(variable);
            if(type == null) {
                if(variable.get("name").equals(import_))
                    return true;
            }
            else if (type.equals(import_)) {
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
                    case "AssignmentStatement":
                        return getType(node.getJmmParent());
                    case "IfCondition":
                        return "boolean";
                    default:
                        System.out.println("Not Implemented Yet: getTypeOfOp/BinOp - " + node.getJmmParent().getKind());
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

        lastIdentifier = node.getJmmChild(0);
        if (!checkVariableDeclaration(lastIdentifier)) {
            addReport(node, lastIdentifier.get("name") + " not declared in scope");
            return null;
        }
        if(lastIdentifier == null) {
            System.out.println("Some error: getTypeofCall");
            return null;
        }

        return getType(lastIdentifier);
    }

    private String getTypeOfAssignment(JmmNode node) {
        var leftChild = node.getJmmChild(0);
        if(!checkVariableDeclaration(leftChild)) return null;

        switch(leftChild.getKind()) {
            case "Literal":
            case "Identifier":
                return getType(leftChild);
            default:
                System.out.print("Add new options: callAssignmentStatement");
                return null;
        }
    }
    private Boolean checkVariableDeclaration(JmmNode variable) {
        switch(variable.getKind()) {
            case "Identifier":
                if(checkVariableOnSymbolTable(variable) || checkImport(variable) || checkSuperclass(variable) || checkClass(variable))
                    return true;
                else {
                    addReport(variable, variable.get("name") + " should be declared");
                    return false;
                }
            case "NewExp":
                variable = variable.getJmmChild(0);
                if(variable.getKind().equals("Literal")) return true;
                break;
            case "Literal":
                return true;
            case "BinOp":
            case "UnaryOp":
                var child = variable.getJmmChild(0);
                switch(child.getKind()) {
                    case "UnaryOp":
                    case "Identifier":
                    case "Literal":
                        return checkVariableDeclaration(child);
                    default:
                        System.out.println("Add new options: checkVariableDeclaration('UnaryOp/BinOp') - " + child.getKind());
                        return false;
                }
            case "Array":
                var arrayId = variable.getJmmChild(0);
                if(!arrayId.getKind().equals("Identifier")) {
                    addReport(arrayId, "Array should be a variable");
                    return false;
                }
                if(checkVariableOnSymbolTable(arrayId)) {
                    var indexId = variable.getJmmChild(1);
                    switch (indexId.getKind()) {
                        case "Literal":
                            return true;
                        case "Identifier":
                            if(checkVariableOnSymbolTable(indexId)) {
                                if("int".equals(getType(indexId)))
                                    return true;
                                else {
                                    addReport(indexId, indexId.get("name") + " should be an int");
                                    return false;
                                }
                            }
                            else {
                                addReport(indexId, indexId.get("name") + " not declared");
                                return false;
                            }
                        default:
                            System.out.println("Add new options: checkVariableDeclaration('Array') - " + indexId.getKind());
                            return false;
                    }
                }
                else {
                    addReport(arrayId, arrayId.get("name") + " not declared.");
                    return false;
                }
            case "Index":
                var array = variable.getJmmChild(0);
                if(checkVariableOnSymbolTable(array)) {
                    JmmNode index = variable.getJmmChild(1);
                    if(checkVariableOnSymbolTable(index))
                        return true;
                    else{
                        addReport(index, "Variable '" + index.get("name") + "' not declared.");
                        return false;
                    }
                }
                else {
                    addReport(array, "Variable '" + array.get("name") + "' not declared.");
                    return false;
                }
            default:
                System.out.println("Add new options: checkVariableDeclaration - " + variable.getKind());
                return false;
        }

        if(checkVariableOnSymbolTable(variable) || checkImport(variable) || checkSuperclass(variable) || checkClass(variable))
            return true;

        addReport(variable, "Variable '" + variable.get("name") + "' not declared.");
        return false;

    }
}

