package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTableBuilder implements pt.up.fe.comp.jmm.analysis.table.SymbolTable {
    private String className;
    private String superClass;
    private final List<String> imports;
    private final List<String> methods;
    private final Map<String, Type> methodReturnTypes;
    private final Map<String, List<Symbol>> methodParams;
    private final Map<String, List<Symbol>> methodFields;
    private final List<Symbol> fields;

    public SymbolTableBuilder() {
        this.imports = new ArrayList<>();
        this.className = null;
        this.superClass = null;
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.methodReturnTypes = new HashMap<>();
        this.methodParams = new HashMap<>();
        this.methodFields = new HashMap<>();
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superClass;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    @Override
    public List<String> getMethods() {
        return methods;
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return methodReturnTypes.get(methodSignature);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return methodParams.get(methodSignature);
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return methodFields.get(methodSignature);
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }

    public void addImport(String importString) {
        this.imports.add(importString);
    }

    public void addField(Symbol field) {
        this.fields.add(field);
    }

    public void addMethodField(String methodSignature, Symbol field) {
        methodFields.get(methodSignature).add(field);
    }

    public void addMethod(String methodSignature) {
        methods.add(methodSignature);
        methodFields.put(methodSignature, new ArrayList<>());
        methodParams.put(methodSignature, new ArrayList<>());
    }

    public void setMethodReturnType(String methodSignature, Type type) {
        methodReturnTypes.put(methodSignature, type);
    }

    public void addMethodParam(String methodSignature, Symbol param) {
        methodParams.get(methodSignature).add(param);
    }

    public boolean containsMethod(String methodSignature) {
        return methods.contains(methodSignature);
    }

    public boolean containsParameter(String methodSignature, String paramName) {
        for (Symbol symbol : getParameters(methodSignature)) {
            if (symbol.getName().equals(paramName)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsField(String name) {
        for (Symbol symbol : getFields()) {
            if (symbol.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsField(String methodSignature, String name) {
        for (Symbol symbol : getLocalVariables(methodSignature)) {
            if (symbol.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "SymbolTableBuilder{" +
                "className='" + className + '\'' +
                ", superClass='" + superClass + '\'' +
                ", imports=" + imports +
                ", methods=" + methods +
                ", methodReturnTypes=" + methodReturnTypes +
                ", methodParams=" + methodParams +
                ", methodFields=" + methodFields +
                ", fields=" + fields +
                '}';
    }
}
