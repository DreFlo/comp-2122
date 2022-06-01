package pt.up.fe.comp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

public class JmmAnalyser implements JmmAnalysis {
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        var symbolTable = new SymbolTableBuilder();
        SymbolTableFiller symbolTableFiller = new SymbolTableFiller();
        symbolTableFiller.visit(parserResult.getRootNode(), symbolTable);

        //VisitorSemantic visitorSemantic = new VisitorSemantic(symbolTable);
        //visitorSemantic.visit(parserResult.getRootNode(), symbolTable);

        List<Report> reports = new ArrayList<>(symbolTableFiller.getReports());
        //reports.addAll(visitorSemantic.getReports());
        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }
}
