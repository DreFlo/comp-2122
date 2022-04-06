package pt.up.fe.comp;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;

public class VisitorEval extends AJmmVisitor<Object, Integer> {

    public VisitorEval() {

        addVisit("IntegerLiteral", this::integerVisit);
        addVisit("UnaryOp", this::unaryOpVisit);
        addVisit("BinOp", this::binOpVisit);

        setDefaultVisit(this::defaultVisit);
    }

    private Integer integerVisit(JmmNode node, Object dummy) {

        if (node.getNumChildren() == 0) {

            return Integer.parseInt(node.get("image"));
        }

        throw new RuntimeException("Illegal number of children in node " + node.getKind() + ".");
    }

    private Integer unaryOpVisit(JmmNode node, Object dummy) {

        String opString = node.get("op");
        if (opString != null) {

            if (node.getNumChildren() != 1) {
                throw new RuntimeException("Illegal number of children in node " + node.getKind() + ".");
            }

            SimpleCalculatorOps op = SimpleCalculatorOps.fromName(opString);
            switch (op) {
            case NEG:
                return -1 * visit(node.getJmmChild(0));

            default:
                throw new RuntimeException("Illegal operation '" + op + "' in " + node.getKind() + ".");
            }
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

            default:
                throw new RuntimeException("Illegal operation '" + op + "' in " + node.getKind() + ".");
            }
        }

        if (node.getNumChildren() != 1) {
            throw new RuntimeException("Illegal number of children in node " + node.getKind() + ".");
        }

        return visit(node.getJmmChild(0));
    }

    private Integer defaultVisit(JmmNode node, Object dummy) {

        if (node.getNumChildren() != 1) {
            throw new RuntimeException("Illegal number of children in node " + node.getKind() + ".");
        }

        return visit(node.getJmmChild(0));
    }
}
