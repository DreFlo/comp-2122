package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SymbolTableMethod {
    String name;
    Type returnType;
    List<Symbol> parameters;
    List<Symbol> localVariables;
}

public class SymbolTable implements pt.up.fe.comp.jmm.analysis.table.SymbolTable {
    HashMap<String, String> imports;
    String className;
    HashMap<String, Tuple<String, String>> Fields;
    //HashMap<String, HashMap<>> methods;

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
}
