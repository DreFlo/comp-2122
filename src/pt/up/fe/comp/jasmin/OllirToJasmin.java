package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class OllirToJasmin {
    private final ClassUnit classUnit;
    private final Map<String, String> fullyQualifiedClassNames;
    private FunctionClassMap<Instruction, String> instructionMap;

    private Map<String, Integer> variableNameNumber;

    OllirToJasmin(ClassUnit classUnit) {
        this.classUnit = classUnit;
        this.fullyQualifiedClassNames = new HashMap<>();
        this.instructionMap = new FunctionClassMap<>();
        this.variableNameNumber = new HashMap<>();
        registerFullyQualifiedClassNames();
        registerInstructionFunctions();
    }

    private void registerFullyQualifiedClassNames() {
        for (String importString : classUnit.getImports()) {
            String[] split =  importString.split("\\.");
            String lastName;

            if (split.length == 0) {
                lastName = importString;
            } else {
                lastName = split[split.length - 1];
            }

            fullyQualifiedClassNames.put(lastName, importString.replace('.', '/'));
        }
    }

    private void registerInstructionFunctions() {
        instructionMap.put(CallInstruction.class, this::getCode);
        instructionMap.put(AssignInstruction.class, this::getCode);
    }

    public String getFullyQualifiedClassName(String className) throws RuntimeException {
        String fullyQualifiedName = fullyQualifiedClassNames.get(className);
        if (fullyQualifiedName != null) return fullyQualifiedName;
        throw new RuntimeException("Could not find fully qualified class name for class: " + className);
    }
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append(".class public ").append(classUnit.getClassName()).append("\n");

        String qualifiedSuperClassName = getFullyQualifiedClassName(classUnit.getSuperClass());
        code.append(".super ").append(qualifiedSuperClassName).append("\n\n");

        code.append(SpecsIo.getResource("resources/jasminConstructor.template").replace("${SUPER_F_Q_CLASS_NAME}", qualifiedSuperClassName)).append("\n");

        for (Method method : classUnit.getMethods()) {
            code.append(getCode(method));
        }


        return code.toString();
    }

    private String getCode(Method method) {
        StringBuilder code = new StringBuilder();

        AccessModifiers modifier = method.getMethodAccessModifier();

        code.append(".method ").append(modifier == AccessModifiers.DEFAULT ? "public " : modifier.name() + " ");

        if (method.isStaticMethod()) {
            code.append("static ");
        }

        code.append(method.getMethodName()).append("(");

        String methodParamTypes = method.getParams().stream().map(element -> getJasminType(element.getType())).collect(Collectors.joining());

        code.append(methodParamTypes).append(")").append(getJasminType(method.getReturnType())).append("\n");

        // TODO CP3 - Calculate values
        code.append(".limit stack 99\n");
        code.append(".limit locals 99\n");

        for (Instruction instruction : method.getInstructions()) {
            code.append(getCode(instruction));
        }

        code.append(".end method\n\n");

        return code.toString();
    }

    private String getCode(Instruction instruction) {
        return instructionMap.apply(instruction);
    }

    private String getCode(CallInstruction instruction) {
        switch (instruction.getInvocationType()) {
            case invokevirtual, invokestatic, invokeinterface, invokespecial -> {
                return getCodeInvoke(instruction);
            }
            case NEW -> {
                return getCodeNew(instruction);
            }
            case ldc -> {
                return getCodeLDC(instruction);
            }
            case arraylength -> {
                return "arraylength\n";
            }
            default -> throw new NotImplementedException("Not implemented for invocation type: " + instruction.getInvocationType());
        }
    }

    public String getCodeInvoke(CallInstruction instruction) {
        StringBuilder code = new StringBuilder();
        code.append(" ").append(instruction.getInvocationType());
        code.append(getFullyQualifiedClassName(((Operand) instruction.getFirstArg()).getName()));
        code.append("/");
        code.append(((LiteralElement )instruction.getSecondArg()).getLiteral().replace("\"", ""));
        code.append("(");
        code.append(instruction.getListOfOperands().stream().map(operand -> getJasminType(operand.getType())).collect(Collectors.joining()));
        code.append(")");
        if (instruction.getInvocationType() == CallType.invokeinterface) {
            code.append(" ").append(instruction.getNumOperands());
        }
        code.append(getJasminType(instruction.getReturnType())).append("\n");
        return code.toString();
    }

    private String getCodeNew(CallInstruction instruction) {
        StringBuilder code = new StringBuilder();
        code.append("new ").append(getFullyQualifiedClassName(((Operand) instruction.getFirstArg()).getName())).append("\n");
        return code.toString();
    }

    private String getCodeLDC(CallInstruction instruction) {
        StringBuilder code = new StringBuilder();
        code.append("ldc ").append(((Operand) instruction.getFirstArg()).getName()).append("\n");
        return code.toString();
    }

    private String getCode(AssignInstruction instruction) {
        StringBuilder code = new StringBuilder();
        switch (instruction.getTypeOfAssign().getTypeOfElement()) {
            case INT32, BOOLEAN -> {
                code.append("istore_");
            }
            default -> {throw new NotImplementedException("Not implemented for type: " + instruction.getTypeOfAssign().getTypeOfElement());}
        }
        return code.toString();
    }

    private String getJasminType(Type type) {
        if (type instanceof ArrayType) {
            return "[" + getJasminType(((ArrayType) type).getTypeOfElements());
        }

        return getJasminType(type.getTypeOfElement());
    }

    private String getJasminType(ElementType type) {
        switch (type) {
            case INT32 -> {
                return "I";
            }
            case BOOLEAN -> {
                return "Z";
            }
            case OBJECTREF -> {
                return "L" + getFullyQualifiedClassName(type.name()) + ";";
            }
            case CLASS -> {
            }
            case THIS -> {
            }
            case STRING -> {
                return "Ljava/lang/String;";
            }
            case VOID -> {
                return "V";
            }
            default -> throw new NotImplementedException(type);
        }
        return "";
    }

    private Integer getVariableNumberFromName(String name) {
        return variableNameNumber.get(name);
    }
}
