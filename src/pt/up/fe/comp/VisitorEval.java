package pt.up.fe.comp;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;

public class VisitorEval extends AJmmVisitor<Object, Integer> {

    public VisitorEval() {

        addVisit("Literal", this::LiteralVisit);
        addVisit("UnaryOp", this::unaryOpVisit);
        addVisit("BinOp", this::binOpVisit);
        addVisit("Identifier", this::identifierVisist);
        addVisit("Inheritance", this::inheritanceVisit);
        addVisit("VarDeclaration", this::varDeclarationVisit);
        addVisit("Type", this::varTypeVisit);
        addVisit("CallExpression", this::callExpressionVisit);
        addVisit("Index", this::indexVisit);

        setDefaultVisit(this::defaultVisit);
    }

    private Integer LiteralVisit(JmmNode node, Object dummy) {

        String typeString = node.get("type");
        if (node.getNumChildren() == 0) {
            switch (typeString){
                case "int":
                    return Integer.parseInt(node.get("image"));
                case "boolean":
                    return 0;
                default:
                    throw new RuntimeException("Illegal type '" + typeString + "' in " + node.getKind() + ".");
            }

        }

        throw new RuntimeException("Illegal number of children in node " + node.getKind() + ".");
    }

    private Integer unaryOpVisit(JmmNode node, Object dummy) {

        String opString = node.get("op");
        if (opString != null) {

            if (node.getNumChildren() != 1) {
                throw new RuntimeException("Illegal number of children in node " + node.getKind() + ".");
            }

            /*
            SimpleCalculatorOps op = SimpleCalculatorOps.fromName(opString);
            switch (op) {
            case NEG:
                return -1 * visit(node.getJmmChild(0));

            default:
                throw new RuntimeException("Illegal operation '" + op + "' in " + node.getKind() + ".");
            }
            */
        }

        if (node.getNumChildren() != 1) {
            throw new RuntimeException("Illegal number of children in node " + node.getKind() + ".");
        }

        return visit(node.getJmmChild(0));
    }

    private Integer binOpVisit(JmmNode node, Object dummy) {

        String opString = node.get("op");
        if (opString != null) {

            if (node.getNumChildren() != 2) {
                throw new RuntimeException("Illegal number of children in node " + node.getKind() + ".");
            }

            /*
            SimpleCalculatorOps op = SimpleCalculatorOps.fromName(opString);
            switch (op) {
            case MUL:
                return visit(node.getJmmChild(0)) * visit(node.getJmmChild(1));

            case DIV:
                return visit(node.getJmmChild(0)) / visit(node.getJmmChild(1));

            case ADD:
                return visit(node.getJmmChild(0)) + visit(node.getJmmChild(1));

            case SUB:
                return visit(node.getJmmChild(0)) - visit(node.getJmmChild(1));

            case AND:
            case LT:
                return 0;

            default:
                throw new RuntimeException("Illegal operation '" + op + "' in " + node.getKind() + ".");
            }*/
        }

        if (node.getNumChildren() != 1) {
            throw new RuntimeException("Illegal number of children in node " + node.getKind() + ".");
        }

        return visit(node.getJmmChild(0));
    }

    private Integer identifierVisist(JmmNode node, Object dummy){
        if (node.getNumChildren() == 0) {
            return 0;
        }

        throw new RuntimeException("Illegal number of children in node " + node.getKind() + ".");
    }

    private Integer inheritanceVisit(JmmNode node, Object dummy){
        if (node.getNumChildren() == 0) {
            return 0;
        }

        throw new RuntimeException("Illegal number of children in node " + node.getKind() + ".");
    }

    private Integer varDeclarationVisit(JmmNode node, Object dummy){
        if (node.getNumChildren() == 2) {
            return 0;
        }

        throw new RuntimeException("Illegal number of children in node " + node.getKind() + ".");
    }

    private Integer varTypeVisit(JmmNode node, Object dummy){
        /*if (node.getNumChildren() == 2) {
            return 0;
        }

        throw new RuntimeException("Illegal number of children in node " + node.getKind() + ".");*/
        return  0;
    }

    private Integer callExpressionVisit(JmmNode node, Object dummy){
        /*if (node.getNumChildren() == 2) {
            return 0;
        }

        throw new RuntimeException("Illegal number of children in node " + node.getKind() + ".");*/
        return  0;
    }

    private Integer indexVisit(JmmNode node, Object dummy){
        /*if (node.getNumChildren() == 2) {
            return 0;
        }

        throw new RuntimeException("Illegal number of children in node " + node.getKind() + ".");*/
        return  0;
    }

    private Integer defaultVisit(JmmNode node, Object dummy) {

        if (node.getNumChildren() != 1) {
            throw new RuntimeException("Illegal number of children in node " + node.getKind() + ".");
        }

        return visit(node.getJmmChild(0));
    }
}
