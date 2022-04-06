package pt.up.fe.comp;

import pt.up.fe.specs.util.SpecsEnums;

public enum SimpleCalculatorOps {

    ADD("+"),
    SUB("-"),
    MUL("*"),
    DIV("/"),
    NEG("-");

    private final String code;

    SimpleCalculatorOps(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }

    static SimpleCalculatorOps fromName(String name) {
        return SpecsEnums.fromName(SimpleCalculatorOps.class, name);
    }
}
