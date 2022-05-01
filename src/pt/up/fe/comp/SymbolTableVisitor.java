package pt.up.fe.comp;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.Objects;

class SymbolTableVisitor extends AJmmVisitor<Object, Integer> {
    SymbolTable table;
    private boolean inClassDeclaration = false;
    private SymbolTableMethod currentMethod = new NullSymbolTableMethod();

    public SymbolTableVisitor(SymbolTable table) {
        this.table = table;

        addVisit("Start", this::visitStart);
        addVisit("ImportDec", this::visitImport);
        addVisit("ClassDeclaration", this::visitClass);
        addVisit("Inheritance", this::visitInheritance);
        addVisit("VarDeclaration", this::visitVarDeclaration);
        addVisit("MainMethod", this::visitMainMethod);
        addVisit("MainArguments", this::visitMainArguments);
        addVisit("InstanceMethod", this::visitInstanceMethod);
        addVisit("Arguments", this::visitArguments);

        setDefaultVisit(this::defaultVisit);
    }

    private Integer visitStart(JmmNode node, Object dummy) {
        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) {
            visit(child);
        }

        return 0;
    }

    private Integer visitImport(JmmNode node, Object dummy) {
        List<JmmNode> children = node.getChildren();
        String _package = "";

        if (children.size() != 0) {
            for (int i = 1; i < children.size(); i++) {
                _package = _package.concat(".".concat(children.get(i).get("name")));
            }
        }

        table.imports.put(children.get(0).get("name"), _package);

        return 0;
    }

    private Integer visitClass(JmmNode node, Object dummy) {
        List<JmmNode> children = node.getChildren();
        inClassDeclaration = true;
        for (int i = 0; i < children.size(); i++) {
            if (i == 0) {
                table.className = children.get(i).get("name");
            } else {
                visit(children.get(i), dummy);
            }
        }
        inClassDeclaration = false;
        return 0;
    }

    private Integer visitInheritance(JmmNode node, Object dummy) {
        table.inheritance = node.getJmmChild(0).get("name");
        return 0;
    }

    private Integer visitVarDeclaration(JmmNode node, Object dummy) {
        if (inClassDeclaration) {
            table.fields.put(node.getJmmChild(1).get("name"), new Tuple<>(node.getJmmChild(0).get("type"), ""));
        } else {
            currentMethod.localVariables.put(node.getJmmChild(1).get("name"), new Tuple<>(node.getJmmChild(0).get("type"), ""));
        }
        return 0;
    }

    private Integer visitMainMethod(JmmNode node, Object dummy) {
        currentMethod = new SymbolTableMethod();
        for (JmmNode child : node.getChildren()) {
            visit(child, dummy);
        }
        table.methods.put("main", currentMethod);
        return 0;
    }

    private Integer visitMainArguments(JmmNode node, Object dummy) {
        currentMethod.parameters.put(node.getJmmChild(1).get("name"), new Tuple<>(node.getJmmChild(0).get("type"), ""));
        return 0;
    }

    private Integer visitInstanceMethod(JmmNode node, Object dummy) {
        currentMethod = new SymbolTableMethod();
        currentMethod.returnType = node.getJmmChild(0).get("type");
        for (int i = 2; i < node.getNumChildren(); i++) {
            visit(node.getJmmChild(i), dummy);
        }
        table.methods.put(node.getJmmChild(1).get("name"), currentMethod);
        return 0;
    }

    private Integer visitArguments(JmmNode node, Object dummy) {
        for (JmmNode child : node.getChildren()) {
            currentMethod.parameters.put(child.getJmmChild(1).get("name"), new Tuple<>(child.getJmmChild(0).get("type"), ""));
        }
        return 0;
    }

    private Integer defaultVisit(JmmNode node, Object dummy) {
        return 0;
    }
}
