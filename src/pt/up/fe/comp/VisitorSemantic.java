package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

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
        addVisit("VarDeclaration", this::varDeclarationVisit);
        addVisit("Index", this::indexVisit);
        setDefaultVisit(this::defaultVisit);
    }
    private Integer indexVisit(JmmNode node, Object dummy){
        if(!getType(node.getJmmChild(1)).equals("int")){
            addReport(node.getJmmChild(1), "Index should be an int");
            return null;
        }
        return 0;
    }
    private Integer defaultVisit(JmmNode node, Object dummy) {

        for(var child: node.getChildren())
            visit(child, dummy);
        return 0;
    }
    private Integer varDeclarationVisit(JmmNode node, Object dummy) {
        JmmNode type = node.getJmmChild(0);
        JmmNode identifier = node.getJmmChild(1);
        visit(identifier, dummy);
        switch(type.get("type")) {
            case "int":
            case "boolean":
            case "String":
                break;
            default:
                if(!(checkImport(identifier) || checkClass(identifier) || checkSuperclass(identifier))) {
                    addReport(type, type.get("type") + " not declared.");
                    return null;
                }
        }
        return 0;
    }
    private Integer whileConditionStatement(JmmNode node, Object dummy) {
        var child = node.getJmmChild(0);
        visit(child, dummy);
        var childType = getType(child);
        if(childType == null) return null;
        if(!childType.equals("boolean")) {
            addReport(node.getJmmChild(0), "While condition should be a boolean");
            return null;
        }
        return 0;
    }
    private Integer ifConditionVisit(JmmNode node, Object dummy) {
        JmmNode child = node.getJmmChild(0);
        visit(child, dummy);
        switch(child.getKind()) {
            case "Literal":
                if(!child.get("type").equals("boolean")) {
                    addReport(node, "If Statement should be 'boolean'");
                    return null;
                }
                break;
            case "Identifier":
            case "BinOp":
                var typeI = getType(node.getJmmChild(0));
                if(typeI == null) {
                    return null;
                }
                if(!typeI.equals("boolean")){
                    addReport(node, "If Statement should be 'boolean'");
                    return null;
                }
                break;
            default:
                return null;
        }
        return 0;
    }
    private Integer binOpVisit(JmmNode node, Object dummy) {
        /* Get important node items */
        var child0 = node.getJmmChild(0);
        var child1 = node.getJmmChild(1);
        visit(child0, dummy);
        visit(child1, dummy);

        if (!checkVariableDeclaration(child0)) {
            addReport(child0, "Variable not declared");
            return null;
        }
        if (!checkVariableDeclaration(child1)) {
            addReport(child1,  "Variable not declared");
            return null;
        }
        /* Get Method from BinOp */
        var parentNode = getParentMethodSignature(node);
        if (parentNode == null) return null;

        /* Check op and types */
        var op = node.get("op");
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
                return false;
        }
        return true;
    }
    private Integer unaryOpVisit (JmmNode node, Object dummy) {
        var op = node.get("op");
        JmmNode child = node.getJmmChild(0);
        visit(child, dummy);
        switch (op) {
            case "return" -> {
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
                            return null;
                        }
                        if (!typeI.equals(returnType.getName())) {
                            addReport(node, "Invalid return type. Expected: '" + returnType.getName() + "'");
                            return null;
                        }
                        break;
                    case "CallExpression":
                        if(checkClass(child.getJmmChild(0))) {
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
                            return null;
                        }
                        if (!typeB.equals(returnType.getName())) {
                            addReport(node, "Invalid return type. Expected: '" + returnType.getName() + "'");
                            return null;
                        }
                        break;
                    case "Array":
                        if(!checkVariableDeclaration(child)) return null;
                        var childA = child.getJmmChild(0);
                        if (!checkVariableDeclaration(childA)) {
                            addReport(childA, "Array not declared");
                            return null;
                        }
                        if (!isArray(child.getJmmChild(0))) {
                            addReport(child,  "Not an array");
                            return null;
                        }
                        break;

                    default:
                        break;
                }
            }
            case "not" -> {
                var typeN = getType(child);
                if (typeN == null) return null;
                if (!typeN.equals("boolean")) {
                    addReport(child, "Expression not a boolean");
                    return null;
                }
            }
            case "length" -> {
                if(child.getKind().equals("Literal")) {
                    addReport(child, "Should be a variable");
                    return null;
                }
                if (!isArray(child)) {
                    addReport(child,  "Not an array");
                    return null;
                }
            }
            default -> {
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
                visit(child, dummy);
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
                /* Check this. in static method aka MainMethod */
                if(identifiers.get(0).getKind().equals("ThisT")) {
                    var parentMethod = getParentNode(node);
                    if (parentMethod.getKind().equals("MainMethod")) {
                        addReport(identifiers.get(0), "this is used on static method");
                        return null;
                    }
                }

                if(!checkClass(getType(identifiers.get(0)))) {
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
                        if(!checkArguments(argument, identifiers)) {
                            return null;
                        }
                    }


                }
                break;
            default:
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
        if (methodArguments.size() != methodParameters.size()) {
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
        var rightChild = node.getJmmChild(1);
        visit(leftChild, dummy);
        visit(rightChild, dummy);
        if(!checkVariableDeclaration(leftChild)) {
            addReport(leftChild, "Variable not declared");
            return null;
        }

        if(!checkVariableInitialization(rightChild)) {
            addReport(rightChild, "Variable not initialized");
            return null;
        }

        var leftChildType = getType(leftChild);
        var rightChildType = getType(rightChild);



        if(leftChildType == null) return null;
        if(rightChildType == null) return null;

        switch (leftChild.getKind()){
            case "Literal":
                addReport(leftChild, "Literal cannot be assigned");
                return null;
            case "Identifier":
            case "Index":
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
                        if(!leftChildType.equals(rightChildType)){
                            addReport(rightChild, "Invalid type");
                            return null;
                        }
                        break;
                    case "NewExp":
                        if(!leftChildType.equals(getType(rightChild.getJmmChild(0)))) {
                            addReport(node, "Invalid assignment type");
                            return null;
                        }
                        break;
                    default:
                        return null;
                }
                break;
            default:
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
                var methodNode = getParentNode(node);
                List<Symbol> variables = new ArrayList<>();
                if(methodNode == null) variables = getVariablesInScope(getParentMethodSignature(node), false);
                else variables = getVariablesInScope(getParentMethodSignature(node), methodNode.getKind().equals("MainMethod"));
                for(var variable: variables) {
                    if (node.get("name").equals(variable.getName())) {
                        return variable.getType().getName();
                    }
                }
                for(var import_: this.symbolTable.getImports()) {
                    if(import_.equals(node.get("name"))) return import_;
                }
                if(this.symbolTable.getSuper() == null) return null;
                if(this.symbolTable.getSuper().equals(node.get("name"))) return this.symbolTable.getSuper();
                return null;
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
            case "Index":
                return "int";
            default:
                return null;
        }
    }
    private String getType(JmmNode node, String typeOfDot) {
        switch(node.getKind()) {
            case "Identifier":
                if(checkClass(typeOfDot)) {
                    for(var method: this.symbolTable.getMethods()) {
                        if (node.get("name").equals(method)) {
                            return this.symbolTable.getReturnType(method).getName();
                        }
                    }
                    if(this.symbolTable.getSuper() != null) return this.symbolTable.getSuper();
                    addReport(node, "Method not exist");
                    return null;
                }
                if(checkImport(typeOfDot) || checkSuperclass(typeOfDot)) {
                    return typeOfDot;
                }
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
    private JmmNode getParentNode(JmmNode node) {
        while(true) {
            if(node.getKind().equals("InstanceMethod") || node.getKind().equals("MainMethod")) {
                return node;
            } else {
                node = node.getJmmParent();
                if(node == null) return null;
            }
        }
    }

    private JmmNode getClassNode(JmmNode node) {
        while(true) {
            if(node.getKind().equals("ClassDeclaration")) {
                return node;
            } else {
                node = node.getJmmParent();
                if(node == null) return null;
            }
        }
    }
    /**
     * Get all variables in scope on node
     * @param nodeSignature
     * @return All variables available in scope
     */
    private List<Symbol> getVariablesInScope(String nodeSignature, Boolean static_) {
        List<Symbol> variables = new ArrayList<>();
        var localVariables = this.symbolTable.getLocalVariables(nodeSignature);
        if(localVariables != null) variables.addAll(localVariables);
        var parameters = this.symbolTable.getParameters(nodeSignature);
        if(localVariables != null) variables.addAll(parameters);
        var fields = this.symbolTable.getFields();
        if(fields != null && !static_) variables.addAll(fields);
        return variables;
    }

    /**
     * Verify if variable is in Symbol Table
     * @param variable
     * @return true if it is, false otherwise
     */
    private Boolean checkIdentifierOnSymbolTable(JmmNode variable) {
        if(!variable.getKind().equals("Identifier")) return true;
        var methodNode = getParentNode(variable);
        List<Symbol> variables = new ArrayList<>();
        if(methodNode == null) variables = getVariablesInScope(getParentMethodSignature(variable), false);
        else variables = getVariablesInScope(getParentMethodSignature(variable), methodNode.getKind().equals("MainMethod"));
        for (var variableTemp: variables) {
            if (variable.get("name").equals(variableTemp.getName())) {
                return true;
            }
        }
        return false;
    }

    private Boolean checkSuperclass(String node){
        if(node == null) return false;
        return node.equals(this.symbolTable.getSuper());
    }
    private Boolean checkSuperclass(JmmNode node) {
        var export = this.symbolTable.getSuper();
        if(export == null) return false;
        return export.equals(getType(node));
    }

    private Boolean checkClass (String className) {
        return this.symbolTable.getClassName().equals(className);
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

    private Boolean checkFields(JmmNode node) {
        for(var field: this.symbolTable.getFields()){
            if(node.get("name").equals(field.getName())) return true;
        }
        return false;
    }

    private Boolean checkImport(String variable) {
        if(variable == null) return false;
        var imports = this.symbolTable.getImports();
        for(var import_: imports) {
            if(variable.equals(import_)) return true;
        }
        return false;
    }
    private Boolean checkImport(JmmNode variable) {
        if(!variable.getAttributes().contains("name")) return false;
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
                        return null;
                }

            case "BinOp":
                var op = node.get("op");
                switch(op) {
                    case "lt":
                    case "and":
                    case "or":
                        return "boolean";
                    case "add":
                    case "sub":
                    case "mult":
                    case "div":
                        return "int";
                    default:
                        return null;
                }
            default:
               return null;
        }
    }

    private Boolean isArray(JmmNode node) {
        if(!node.getAttributes().contains("name")) return false;

        var methodNode = getParentNode(node);
        List<Symbol> variables = new ArrayList<>();
        if(methodNode == null) variables = getVariablesInScope(getParentMethodSignature(node), false);
        else variables = getVariablesInScope(getParentMethodSignature(node), methodNode.getKind().equals("MainMethod"));

        for(var variable: variables) {
            if (variable.getName().equals(node.get("name"))) {
                return variable.getType().isArray();
            }
        }
        return false;
    }
    private String getTypeOfCall(JmmNode node) {

        JmmNode lastIdentifier = null;
        JmmNode firstIdentifier = node.getJmmChild(0);
        if(firstIdentifier.getKind().equals("NewExp")) {
            firstIdentifier = firstIdentifier.getJmmChild(0);
        }
        for(var child: node.getChildren()) {
            if(child.getKind().equals("Arguments")) {
                break;
            } else {
                lastIdentifier = child;
            }
        }

        if(lastIdentifier == null) return null;
        if (!checkVariableDeclaration(lastIdentifier)) {
            addReport(node, lastIdentifier.get("name") + " not declared in scope");
            return null;
        }
        if(firstIdentifier.getKind().equals("ThisT")) {
            return getType(lastIdentifier, this.symbolTable.getClassName());
        }
        if(firstIdentifier.get("name").equals(lastIdentifier.get("name"))) {
            return getType(lastIdentifier);
        }
        return getType(lastIdentifier, getType(firstIdentifier));
    }


    private String getTypeOfAssignment(JmmNode node) {
        var leftChild = node.getJmmChild(0);
        if(!checkVariableDeclaration(leftChild)) return null;

        switch(leftChild.getKind()) {
            case "Literal":
            case "Identifier":
                return getType(leftChild);
            default:
                return null;
        }
    }
    private Boolean checkArgument(JmmNode node) {
        if(!node.getAttributes().contains("name")) return false;
        var parentMethod = getParentMethodSignature(node);
        var parameters = this.symbolTable.getParameters(parentMethod);
        for(var parameter: parameters) {
            if(node.get("name").equals(parameter.getName())) {
                return true;
            }
        }
        return false;
    }

    private Boolean checkVariableInitialization(JmmNode node) {
        if(node.getKind().equals("Literal")) return true;
        if(!checkVariableDeclaration(node)) {
            addReport(node, "Variable should be declared");
            return false;
        }

        if(checkArgument(node)) return true;
        if(checkImport(node)) return true;
        if(checkClass(node)) return true;
        if(checkSuperclass(node)) return true;

        switch(node.getKind()) {
            case "Literal":
            case "CallExpression":
                return true;
            case "Identifier":

                if(checkFields(node)) return true;
                JmmNode parentNode = getParentNode(node);
                List<JmmNode> assigns = getAllAssignment(parentNode);

                for (JmmNode assign : assigns) {
                    JmmNode identifier = assign.getJmmChild(0);
                    if(identifier.getKind().equals("Index")) {
                        identifier = identifier.getJmmChild(0);
                    }
                    if (identifier.get("name").equals(node.get("name"))) {
                        if (Integer.parseInt(identifier.get("line")) < Integer.parseInt(node.get("line"))) {
                            return true;
                        }
                    }
                }

                break;
            case "UnaryOp":
                return checkVariableInitialization(node.getJmmChild(0));
            case "BinOp":

                return checkVariableInitialization(node.getJmmChild(0)) && checkVariableInitialization(node.getJmmChild(1));
            default:
                return checkVariableDeclaration(node);
        }

        return false;
    }

    private List<JmmNode> getAllAssignment(JmmNode parent) {
        List<JmmNode> result = new ArrayList<>();
        Queue<JmmNode> queue = new LinkedList<>();
        queue.add(parent);
        while(!queue.isEmpty()) {
            JmmNode temp = queue.peek();
            queue.remove();

            if(temp.getKind().equals("AssignmentStatement")) {
                result.add(temp);
            }
            queue.addAll(temp.getChildren());
        }

        return result;
    }
    private Boolean checkVariableDeclaration(JmmNode variable) {

        switch(variable.getKind()) {
            case "Identifier":
                if(checkIdentifierOnSymbolTable(variable) || checkImport(variable) || checkSuperclass(variable) || checkClass(variable) || checkArgument(variable))
                    return true;
                else {
                    var classNode = getClassNode(variable);

                    /* Ver se método pertence á class */
                    if(classNode.getJmmChild(0).get("name").equals(this.symbolTable.getClassName())) {
                        if(this.symbolTable.getMethods().contains(variable.get("name"))) {
                            return true;
                        }
                        if(this.symbolTable.getSuper() != null) return true;
                    }

                    if(variable.getJmmParent().getKind().equals("CallExpression")) {
                        JmmNode child0 = variable.getJmmParent().getJmmChild(0);
                        if(checkImport(child0) || checkSuperclass(child0))
                            return true;
                    }


                    addReport(variable, variable.get("name") + " should be declared");
                    return false;
                }
            case "NewExp":
                variable = variable.getJmmChild(0);
                if(variable.getKind().equals("Literal")) return true;
                break;
            case "Literal":
            case "ThisT":
            case "CallExpression":
                return true;
            case "BinOp":
                return checkVariableDeclaration(variable.getJmmChild(0)) && checkVariableDeclaration(variable.getJmmChild(1));
            case "UnaryOp":
                return checkVariableDeclaration(variable.getJmmChild(0));
            case "Array":
                var arrayId = variable.getJmmChild(0);
                if(!arrayId.getKind().equals("Identifier")) {
                    addReport(arrayId, "Array should be a variable");
                    return false;
                }
                if(checkIdentifierOnSymbolTable(arrayId)) {
                    var indexId = variable.getJmmChild(1);
                    switch (indexId.getKind()) {
                        case "Literal":
                            if(!"int".equals(getType(indexId))) {
                                addReport(indexId, "Index should be an integer");
                                return false;
                            }
                            return true;
                        case "Identifier":
                            if(checkIdentifierOnSymbolTable(indexId)) {
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
                            return false;
                    }
                }
                else {
                    addReport(arrayId, "array not declared.");
                    return false;
                }
            case "Index":
                var array = variable.getJmmChild(0);
                if(checkIdentifierOnSymbolTable(array)) {
                    JmmNode index = variable.getJmmChild(1);
                    if(checkIdentifierOnSymbolTable(index))
                        return true;
                    else{
                        addReport(index, "Variable not declared.");
                        return false;
                    }
                }
                else {
                    addReport(array, "Variable not declared.");
                    return false;
                }
            default:
                return false;
        }
        if(variable.getKind().equals("Identifier") && checkIdentifierOnSymbolTable(variable)) {
            return true;
        }
        if(checkImport(variable) || checkSuperclass(variable) || checkClass(variable) || checkArgument(variable))
            return true;
        if(variable.getKind().equals("UnaryOp")) {
            return checkVariableDeclaration(variable.getJmmChild(0));
        }
        addReport(variable, "Variable not declared.");
        return false;

    }
}

