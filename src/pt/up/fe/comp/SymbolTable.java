package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable implements pt.up.fe.comp.jmm.analysis.table.SymbolTable {
    Map<String, String> imports;
    String className;

    String inheritance;
    Map<String, Tuple<String, String>> fields;
    Map<String, SymbolTableMethod> methods;

    public SymbolTable() {
        this.imports = new HashMap<>();
        this.className = "";
        this.inheritance = "";
        this.fields = new HashMap<>();
        this.methods = new HashMap<>();
    }

    @Override
    public List<String> getImports() {
        return null;
    }

    @Override
    public String getClassName() {
        return null;
    }

    @Override
    public String getSuper() {
        return null;
    }

    @Override
    public List<Symbol> getFields() {
        return null;
    }

    @Override
    public List<String> getMethods() {
        return null;
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return null;
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return null;
    }

    @Override
    public String toString() {
        return "SymbolTable{" +
                "imports=" + imports +
                ", className='" + className + '\'' +
                ", inheritance='" + inheritance + '\'' +
                ", fields=" + fields +
                ", methods=" + methods +
                '}';
    }
}
