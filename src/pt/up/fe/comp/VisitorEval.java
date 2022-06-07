package pt.up.fe.comp;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class VisitorEval extends AJmmVisitor<Object, Integer> {
    List<Report> reports;

    public VisitorEval() {

        addVisit("Literal", this::LiteralVisit);
        addVisit("UnaryOp", this::unaryOpVisit);
        addVisit("BinOp", this::binOpVisit);
        addVisit("Identifier", this::identifierVisit);
        addVisit("Inheritance", this::inheritanceVisit);
        addVisit("VarDeclaration", this::varDeclarationVisit);
        addVisit("Type", this::varTypeVisit);
        addVisit("CallExpression", this::callExpressionVisit);
        addVisit("Index", this::indexVisit);

        setDefaultVisit(this::defaultVisit);

        reports = new ArrayList<>();
    }

    private void addReport(JmmNode node, String message) {
        reports.add(new Report(ReportType.ERROR, Stage.SYNTATIC,
                Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                message));
    }

    private Integer LiteralVisit(JmmNode node, Object dummy) {

        String typeString = node.get("type");
        if (node.getNumChildren() == 0) {
            switch (typeString){
                case "int":
                case "boolean":
                    return 0;
                default:
                    addReport(node, "Illegal type '" + typeString + "' in " + node.getKind() + ".");
                    return 1;
            }
        }
        addReport(node, "Illegal number of children in node");
        return 1;
    }

    private Integer unaryOpVisit(JmmNode node, Object dummy) {

        if (node.getNumChildren() != 1) {
            addReport(node, "Illegal number of children in node " + node.getKind() + ".");
            return 1;
        }

        return visit(node.getJmmChild(0));
    }

    private Integer binOpVisit(JmmNode node, Object dummy) {

        if (node.getNumChildren() != 2) {
            addReport(node, "Illegal number of children in node " + node.getKind() + ".");
            return 1;
        }

        for(var child: node.getChildren())
            if (visit(child, dummy) != 0) return 1;

        return 0;
    }

    private Integer identifierVisit(JmmNode node, Object dummy){
        if (node.getNumChildren() == 0) {
            return 0;
        }

        addReport(node, "Illegal number of children in node " + node.getKind() + ".");
        return 1;
    }

    private Integer inheritanceVisit(JmmNode node, Object dummy){
        System.out.println(node.toTree());
        if (node.getNumChildren() == 1) {
            return visit(node.getJmmChild(0), dummy);
        }
        addReport(node, "Illegal number of children in node " + node.getKind() + ".");
        return 1;
    }

    private Integer varDeclarationVisit(JmmNode node, Object dummy){
        if (node.getNumChildren() == 2) {
            for(var child: node.getChildren())
                if (visit(child, dummy) != 0) return 1;

            return 0;
        }

        addReport(node, "Illegal number of children in node " + node.getKind() + ".");
        return 1;
    }

    private Integer varTypeVisit(JmmNode node, Object dummy){
        if (node.getNumChildren() == 0) {
            return 0;
        }

        addReport(node, "Illegal number of children in node " + node.getKind() + ".");
        return 1;
    }

    private Integer callExpressionVisit(JmmNode node, Object dummy){
        if (node.getNumChildren() == 3) {
            for(var child: node.getChildren())
                if (visit(child, dummy) != 0) return 1;

            return 0;
        }

        addReport(node, "Illegal number of children in node " + node.getKind() + ".");
        return  1;
    }

    private Integer indexVisit(JmmNode node, Object dummy){
        if (node.getNumChildren() == 2) {
            for(var child: node.getChildren())
                if (visit(child, dummy) != 0) return 1;
            return 0;
        }

        addReport(node, "Illegal number of children in node " + node.getKind() + ".");
        return  0;
    }

    private Integer defaultVisit(JmmNode node, Object dummy) {
        for(var child: node.getChildren())
            if (visit(child, dummy) != 0) return 1;
        return 0;
    }

    public List<Report> getReports() {
        return reports;
    }
}
