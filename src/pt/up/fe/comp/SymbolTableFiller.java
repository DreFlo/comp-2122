package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import javax.sound.midi.Soundbank;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class SymbolTableFiller extends PreorderJmmVisitor<SymbolTableBuilder, Integer> {
    private boolean inClassDeclaration = true;
    private String currentMethodSignature = null;

    private final List<Report> reports;

    public SymbolTableFiller() {
        reports = new ArrayList<>();

        addVisit("ImportDec", this::visitImport);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("Inheritance", this::visitInheritance);
        addVisit("VarDeclaration", this::visitVarDeclaration);
        addVisit("MainMethod", this::visitMainMethod);
        addVisit("MainArguments", this::visitMainArguments);
        addVisit("InstanceMethod", this::visitInstanceMethod);
        addVisit("Argument", this::visitArgument);

        setDefaultVisit(this::defaultVisit);
    }

    public List<Report> getReports() {
        return reports;
    }

    private Integer visitImport(JmmNode node, SymbolTableBuilder symbolTableBuilder) {
        List<JmmNode> children = node.getChildren();
        String importString = children.get(0).get("name");

        for (int i = 1; i < children.size(); i++) {
            importString = importString.concat(".".concat(children.get(i).getJmmChild(0).get("name")));
        }

        symbolTableBuilder.addImport(importString);

        return 0;
    }

    private Integer visitClassDeclaration(JmmNode node, SymbolTableBuilder symbolTableBuilder) {
        symbolTableBuilder.setClassName(node.getJmmChild(0).get("name"));
        return 0;
    }

    private Integer visitInheritance(JmmNode node, SymbolTableBuilder symbolTableBuilder) {
        symbolTableBuilder.setSuperClass(node.getJmmChild(0).get("name"));
        inClassDeclaration = true;
        return 0;
    }

    private Integer visitVarDeclaration(JmmNode node, SymbolTableBuilder symbolTableBuilder) {
        Symbol symbol = new Symbol(new Type(node.getJmmChild(0).get("type"), Objects.equals(node.getJmmChild(0).get("array"), "true")), node.getJmmChild(1).get("name"));
        if (inClassDeclaration) {
            if (symbolTableBuilder.containsField(symbol.getName())) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Variable of same name declared twice"));
                return -1;
            }
            symbolTableBuilder.addField(symbol);
        } else {
            if (symbolTableBuilder.containsField(currentMethodSignature, symbol.getName())) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Variable of same name declared twice"));
                return -1;
            }
            symbolTableBuilder.addMethodField(currentMethodSignature, symbol);
        }
        return 0;
    }

    private Integer visitMainMethod(JmmNode node, SymbolTableBuilder symbolTableBuilder) {
        inClassDeclaration = false;
        currentMethodSignature = "main";
        if (symbolTableBuilder.containsMethod(currentMethodSignature)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Same method declared twice"));
            return -1;
        }
        symbolTableBuilder.addMethod("main");
        symbolTableBuilder.setMethodReturnType("main", new Type("void", false));
        return 0;
    }

    private Integer visitMainArguments(JmmNode node, SymbolTableBuilder symbolTableBuilder) {
        symbolTableBuilder.addMethodParam(currentMethodSignature, new Symbol(new Type("String", true), node.getJmmChild(1).get("name")));
        return 0;
    }

    private Integer visitInstanceMethod(JmmNode node, SymbolTableBuilder symbolTableBuilder) {
        inClassDeclaration = false;
        currentMethodSignature = node.getJmmChild(1).get("name");
        if (symbolTableBuilder.containsMethod(currentMethodSignature)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Same method declared twice"));
            return -1;
        }
        symbolTableBuilder.addMethod(currentMethodSignature);
        symbolTableBuilder.setMethodReturnType(currentMethodSignature, new Type(node.getJmmChild(0).get("type"), Objects.equals(node.getJmmChild(0).get("array"), "true")));
        return 0;
    }

    private Integer visitArgument(JmmNode node, SymbolTableBuilder symbolTableBuilder) {
        Symbol symbol = new Symbol(new Type(node.getJmmChild(0).get("type"), Objects.equals(node.getJmmChild(0).get("array"), "true")), node.getJmmChild(1).get("name"));
        if (symbolTableBuilder.containsParameter(currentMethodSignature, symbol.getName())) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Variable of same name declared twice"));
            return -1;
        }
        symbolTableBuilder.addMethodParam(currentMethodSignature, symbol);
        return 0;
    }

    private Integer defaultVisit(JmmNode node, SymbolTableBuilder symbolTableBuilder) {
        return 0;
    }
}
