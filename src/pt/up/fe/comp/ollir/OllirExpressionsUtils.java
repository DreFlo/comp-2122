package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;


public class OllirExpressionsUtils extends AJmmVisitor<Integer, String> {
    private static int localVariableCounter = 0;
    final SymbolTable symbolTable;

    public OllirExpressionsUtils(SymbolTable symbolTable){
        this.symbolTable = symbolTable;

        addVisit("BinOp", this::binOpVisit);
    }

    
    private String binOpVisit(JmmNode jmmNode, Integer integer) {
        return "";
    }


}
