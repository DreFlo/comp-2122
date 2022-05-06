package pt.up.fe.comp.ollir;

public class OllirCode {
    private final StringBuilder beforeCode;
    private final StringBuilder variable;

    public OllirCode(StringBuilder beforeCode, StringBuilder variable){
        this.beforeCode = beforeCode;
        this.variable = variable;
    }

    public StringBuilder getBeforeCode() {
        return beforeCode;
    }

    public StringBuilder getVariable() {
        return variable;
    }
}
