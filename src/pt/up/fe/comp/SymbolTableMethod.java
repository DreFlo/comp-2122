package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SymbolTableMethod {
    String returnType;
    Map<String, Tuple<String, String>> parameters;
    Map<String, Tuple<String, String>> localVariables;

    SymbolTableMethod() {
        returnType = "";
        parameters = new HashMap<>();
        localVariables = new HashMap<>();
    }
}

class NullSymbolTableMethod extends SymbolTableMethod {
}
