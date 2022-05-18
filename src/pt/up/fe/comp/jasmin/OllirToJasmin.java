package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.Array;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class OllirToJasmin {
    private final ClassUnit classUnit;

    private final SymbolTable symbolTable;

    private final Map<String, String> fullyQualifiedClassNames;

    private final FunctionClassMap<Instruction, String> instructionMap;

    private final Map<String, String> variableClass;

    private Method currentMethod = null;

    private final static String DEFAULT_ACCESS = "public";

    private int labelNumber = 0;

    OllirToJasmin(OllirResult ollirResult) throws OllirErrorException {
        this.classUnit = ollirResult.getOllirClass();
        this.symbolTable = ollirResult.getSymbolTable();
        this.fullyQualifiedClassNames = new HashMap<>();
        this.instructionMap = new FunctionClassMap<>();
        this.variableClass = new HashMap<>();
        registerFullyQualifiedClassNames();
        registerInstructionFunctions();
        this.classUnit.checkMethodLabels(); // check the use of labels in the OLLIR loaded
        this.classUnit.buildCFGs(); // build the CFG of each method
        this.classUnit.buildVarTables();
    }

    private void registerFullyQualifiedClassNames() {
        fullyQualifiedClassNames.put("this", classUnit.getClassName());
        fullyQualifiedClassNames.put(classUnit.getClassName(), classUnit.getClassName());
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
        instructionMap.put(BinaryOpInstruction.class, this::getCode);
        instructionMap.put(UnaryOpInstruction.class, this::getCode);
        instructionMap.put(ReturnInstruction.class, this::getCode);
        instructionMap.put(SingleOpInstruction.class, this::getCode);
        instructionMap.put(GotoInstruction.class, this::getCode);
        instructionMap.put(GetFieldInstruction.class, this::getCode);
        instructionMap.put(PutFieldInstruction.class, this::getCode);
        instructionMap.put(OpCondInstruction.class, this::getCode);
    }

    private String getFullyQualifiedClassName(String name) throws RuntimeException {
        String fullyQualifiedName = fullyQualifiedClassNames.get(name);
        if (fullyQualifiedName != null) return fullyQualifiedName;
        else if (symbolTable != null) {
            for (Symbol symbol : symbolTable.getLocalVariables(currentMethod.getMethodName())) {
                if (symbol.getName().equals(name)) {
                    return fullyQualifiedClassNames.get(symbol.getType().getName());
                }
            }
        }
        else if (variableClass.containsKey(name)) {
            return variableClass.get(name);
        }
        throw new RuntimeException("Could not find fully qualified class name for class: " + name);
    }

    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append(".class public ").append(classUnit.getClassName()).append("\n");

        String qualifiedSuperClassName = "java/lang/Object";

        if (classUnit.getSuperClass() != null) {
            qualifiedSuperClassName = getFullyQualifiedClassName(classUnit.getSuperClass());
        }

        code.append(".super ").append(qualifiedSuperClassName);

        code.append("\n\n");

        code.append(classUnit.getFields().stream().map(this::getCode).collect(Collectors.joining()));

        code.append("\n");

        code.append(SpecsIo.getResource("resources/jasminConstructor.template").replace("${SUPER_F_Q_CLASS_NAME}", qualifiedSuperClassName)).append("\n\n");

        for (Method method : classUnit.getMethods()) {
            if (method.isConstructMethod()) continue;
            code.append(getCode(method));
        }

        return code.toString();
    }

    private String getCode(Field field) {
        StringBuilder code = new StringBuilder();
        code.append(".field ").append(getAccessModifierString(field.getFieldAccessModifier())).append(" ");
        if (field.isStaticField()) {
            code.append("static ");
        }
        if (field.isFinalField()) {
            code.append("final ");
        }
        code.append(field.getFieldName()).append(" ");
        code.append(getJasminType(field.getFieldType())).append(" ");
        if (field.isInitialized()) {
            code.append("= ").append(field.getInitialValue());
        }
        code.append("\n");
        return code.toString();
    }

    private String getCode(Method method) {
        currentMethod = method;

        StringBuilder code = new StringBuilder();

        code.append(".method ").append(getAccessModifierString(method.getMethodAccessModifier())).append(" ");

        if (method.isStaticMethod()) {
            code.append("static ");
        }

        code.append(method.getMethodName()).append("(");

        String methodParamTypes = method.getParams().stream().map(this::getJasminType).collect(Collectors.joining());

        code.append(methodParamTypes).append(")").append(getJasminType(method.getReturnType())).append("\n");

        // TODO CP3 - Calculate values
        code.append(".limit stack 99\n");
        code.append(".limit locals 99\n");

        for (Instruction instruction : method.getInstructions()) {
            code.append(getLabels(method.getLabels(instruction)));
            code.append(getCode(instruction));
        }

        code.append(".end method\n\n");

        return code.toString();
    }

    private String getLabels(List<String> labels) {
        StringBuilder code = new StringBuilder();
        for (String label : labels) {
            code.append(label).append(":\n");
        }
        return code.toString();
    }

    private String getCode(Instruction instruction) {
        Optional<String> code = instructionMap.applyTry(instruction);
        if (code.isPresent()) {
            return code.get();
        } else {
            throw new NotImplementedException("Not implemented for " + instruction.getClass() + " instructions");
        }
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
                return pushElementToStack(instruction.getFirstArg()) + "arraylength\n";
            }

            default -> throw new NotImplementedException("Not implemented for invocation type: " + instruction.getInvocationType());
        }
    }

    public String getCodeInvoke(CallInstruction instruction) {
        StringBuilder code = new StringBuilder();
        code.append(pushArgumentsToStack(instruction));
        code.append(instruction.getInvocationType()).append(" ");
        code.append(getFullyQualifiedClassName(((Operand) instruction.getFirstArg()).getName()));
        code.append("/");
        code.append(((LiteralElement )instruction.getSecondArg()).getLiteral().replace("\"", ""));
        code.append("(");
        code.append(instruction.getListOfOperands().stream().map(this::getJasminType).collect(Collectors.joining()));
        code.append(")");
        if (instruction.getInvocationType() == CallType.invokeinterface) {
            code.append(" ").append(instruction.getNumOperands());
        }
        code.append(getJasminType(instruction.getReturnType())).append("\n");
        return code.toString();
    }

    private String getCodeNew(CallInstruction instruction) {
        StringBuilder code = new StringBuilder();
        if (!instruction.getFirstArg().isLiteral()) {
            Operand operand = (Operand) instruction.getFirstArg();
            if (operand.getType().getTypeOfElement() == ElementType.ARRAYREF) {
                Element element = instruction.getListOfOperands().get(0);
                code.append(pushElementToStack(element));
                code.append("newarray int\n");
                return code.toString();
            }
        }
        return "new " + getFullyQualifiedClassName(((Operand) instruction.getFirstArg()).getName()) + "\n";
    }

    private String getCodeLDC(CallInstruction instruction) {
        return "ldc " + ((Operand) instruction.getFirstArg()).getName() + "\n";
    }

    private String getCode(AssignInstruction instruction) {
        StringBuilder code = new StringBuilder();
        if (instruction.getDest() instanceof ArrayOperand) {
            code.append(storeInArray((ArrayOperand) instruction.getDest(), getCode(instruction.getRhs())));
        }
        else {
            code.append(getCode(instruction.getRhs()));
            switch (instruction.getTypeOfAssign().getTypeOfElement()) {
                case INT32, BOOLEAN -> code.append("istore ").append(getCurrentMethodVarVirtualRegisterFromElement(instruction.getDest()));
                case OBJECTREF, ARRAYREF -> code.append("astore ").append(getCurrentMethodVarVirtualRegisterFromElement(instruction.getDest()));
                default -> throw new NotImplementedException("Not implemented for type: " + instruction.getTypeOfAssign().getTypeOfElement());
            }
            code.append("\n");
            switch (instruction.getTypeOfAssign().getTypeOfElement()) {
                case INT32 -> variableClass.put(((Operand) instruction.getDest()).getName(), "int");
                case BOOLEAN -> variableClass.put(((Operand) instruction.getDest()).getName(), "boolean");
                case OBJECTREF -> {
                    if (instruction.getRhs().getClass() == SingleOpInstruction.class) {
                        variableClass.put(((Operand) instruction.getDest()).getName(), getFullyQualifiedClassName(((Operand) ((SingleOpInstruction) instruction.getRhs()).getSingleOperand()).getName()));
                    } else if (instruction.getRhs().getClass() == CallInstruction.class) {
                        variableClass.put(((Operand) instruction.getDest()).getName(), getFullyQualifiedClassName(((Operand) ((CallInstruction) instruction.getRhs()).getFirstArg()).getName()));
                    } else {
                        throw new NotImplementedException("Not implemented for : " + instruction.getRhs().getClass());
                    }
                }
                case ARRAYREF -> {
                    if (instruction.getRhs().getClass() == CallInstruction.class) {
                        variableClass.put(((Operand) instruction.getDest()).getName(), "[I" );
                    }
                }
                default -> throw new NotImplementedException("Not implemented for type: " + instruction.getTypeOfAssign().getTypeOfElement());
            }
        }
        return code.toString();
    }

    private String getCode(BinaryOpInstruction instruction) {
        StringBuilder code = new StringBuilder();
        code.append(pushElementToStack(instruction.getLeftOperand()));
        code.append(pushElementToStack(instruction.getRightOperand()));
        switch (instruction.getOperation().getOpType()) {
            case ADDI32, ADD -> code.append("iadd\n");
            case SUBI32, SUB -> code.append("isub\n");
            case MULI32, MUL -> code.append("imul\n");
            case DIVI32, DIV -> code.append("idiv\n");
            case ANDB, AND -> code.append("iand\n");
            case LTHI32, LTH -> {
                code.append("if_icmplt L").append(labelNumber).append("\n");
                code.append(pushComparisonResultToStack());
            }
            case GTHI32, GTH -> {
                code.append("if_icmpgt L").append(labelNumber).append("\n");
                code.append(pushComparisonResultToStack());
            }
            case LTEI32, LTE -> {
                code.append("if_icmple L").append(labelNumber).append("\n");
                code.append(pushComparisonResultToStack());
            }
            case GTEI32, GTE -> {
                code.append("if_icmpge L").append(labelNumber).append("\n");
                code.append(pushComparisonResultToStack());
            }
            case EQ -> {
                code.append("if_icmpeq L").append(labelNumber).append("\n");
                code.append(pushComparisonResultToStack());
            }
            default -> throw new NotImplementedException("Not implemented for type: " + instruction.getOperation().getOpType());
        }
        return code.toString();
    }

    private String getCode(UnaryOpInstruction instruction) {
        StringBuilder code = new StringBuilder();
        code.append(pushElementToStack(instruction.getOperand()));
        if (instruction.getOperation().getOpType() == OperationType.NOT || instruction.getOperation().getOpType() == OperationType.NOTB) {
            code.append("ifeq L").append(labelNumber).append("\n");
            code.append(pushComparisonResultToStack());
        } else {
            throw new NotImplementedException("Not implemented for type: " + instruction.getOperation().getOpType());
        }
        return code.toString();
    }

    private String pushComparisonResultToStack() {
        String code = "bipush 0\n" +
                "goto LE" + labelNumber + "\n" +
                "L" + labelNumber + ":\n" +
                "bipush 1\n" +
                "LE" + labelNumber + ":\n";
        labelNumber++;
        return code;
    }

    private String getCode(ReturnInstruction instruction) {
        StringBuilder code = new StringBuilder();
        if (instruction.hasReturnValue()) {
            code.append(pushElementToStack(instruction.getOperand()));
            switch (instruction.getOperand().getType().getTypeOfElement()) {
                case OBJECTREF, STRING -> code.append("areturn\n");
                case INT32, BOOLEAN -> code.append("ireturn\n");
            }
        } else {
            code.append("return\n");
        }
        return code.toString();
    }

    public String getCode(SingleOpInstruction instruction) {
        return pushElementToStack(instruction.getSingleOperand());
    }

    public String getCode(GotoInstruction instruction) {
        return "goto " + instruction.getLabel() + "\n";
    }

    public String getCode(GetFieldInstruction instruction) {
        return pushElementToStack(instruction.getFirstOperand()) +
                "getfield " + getFullyQualifiedClassName(((Operand) instruction.getFirstOperand()).getName()) + "/" + ((Operand) instruction.getSecondOperand()).getName() + " " + getJasminType((instruction.getSecondOperand())) + "\n";
    }

    public String getCode(PutFieldInstruction instruction) {
        return pushElementToStack(instruction.getFirstOperand()) +
                pushElementToStack(instruction.getThirdOperand()) +
                "putfield " +
                getFullyQualifiedClassName(((Operand) instruction.getFirstOperand()).getName()) + "/" +
                ((Operand) instruction.getSecondOperand()).getName() + " " +
                getJasminType((instruction.getSecondOperand())) + " " +
                "\n";
    }

    public String getCode(OpCondInstruction instruction) {
        return getCode(instruction.getCondition()) +
                "ifne " + instruction.getLabel() + "\n";
    }

    private String pushElementToStack(Element element) {
        StringBuilder code = new StringBuilder();
        if (element instanceof ArrayOperand) {
            ArrayOperand arrayOperand = (ArrayOperand) element;
            code.append("aload ").append(getCurrentMethodVarVirtualRegisterFromElement(arrayOperand)).append("\n");
            for (Element index : arrayOperand.getIndexOperands()) {
                code.append(pushElementToStack(index));
            }
            code.append("iaload");
        }
        else if (!element.isLiteral()) {
            if (((Operand) element).getName().equals("this")) {
                code.append("aload_0");
            }
            else {
                switch (element.getType().getTypeOfElement()) {
                    case INT32, BOOLEAN -> code.append("iload ").append(getCurrentMethodVarVirtualRegisterFromElement(element));
                    case OBJECTREF, ARRAYREF -> code.append("aload ").append(getCurrentMethodVarVirtualRegisterFromElement(element));
                    default -> throw new NotImplementedException("Not implemented for type: " + element.getType().getTypeOfElement() + " with no name.");
                }
            }
        } else {
            switch (element.getType().getTypeOfElement()) {
                case STRING -> code.append("ldc ").append(((LiteralElement) element).getLiteral());
                case INT32, BOOLEAN -> code.append("bipush ").append(((LiteralElement) element).getLiteral());
                default -> throw new NotImplementedException("Not implemented for type: " + element.getType().getTypeOfElement() + " with no name.");
            }
        }
        code.append("\n");
        return code.toString();
    }

    private String storeInArray(ArrayOperand arrayOperand, String toStore) {
        StringBuilder code = new StringBuilder();
        code.append("aload ").append(getCurrentMethodVarVirtualRegisterFromElement(arrayOperand)).append("\n");
        for (Element index : arrayOperand.getIndexOperands()) {
            code.append(pushElementToStack(index));
        }
        code.append(toStore);
        code.append("iastore\n");
        return code.toString();
    }

    private String pushArgumentsToStack(CallInstruction instruction) {
        StringBuilder code = new StringBuilder();
        if (instruction.getInvocationType() == CallType.invokestatic) {
            for (Element element : instruction.getListOfOperands()) {
                code.append(pushElementToStack(element));
            }
        }
        else if (instruction.getInvocationType() == CallType.invokespecial || instruction.getInvocationType() == CallType.invokevirtual) {
            code.append(pushElementToStack(instruction.getFirstArg()));
            for (Element element : instruction.getListOfOperands()) {
                code.append(pushElementToStack(element));
            }
        }
        else {
            instruction.show();
            throw new NotImplementedException("Not implemented for " + instruction.getInvocationType());
        }
        return code.toString();
    }

    private String getJasminType(Element element) {
        if (element.getType() instanceof ArrayType) {
            return "[" + getJasminType(((ArrayType) element.getType()).getArrayType());
        }
        else {
            switch (element.getType().getTypeOfElement()) {
                case INT32 -> {
                    return "I";
                }
                case BOOLEAN -> {
                    return "Z";
                }
                case OBJECTREF -> {
                    return "L" + getFullyQualifiedClassName(((Operand) element).getName()) + ";";
                }
                case STRING -> {
                    return "Ljava/lang/String;";
                }
                case VOID -> {
                    return "V";
                }
                default -> throw new NotImplementedException(element.getType().getTypeOfElement());
            }
        }
    }

    private String getJasminType(Type type) {
        if (type instanceof ArrayType) {
            return "[" + getJasminType(((ArrayType) type).getArrayType());
        }
        switch (type.getTypeOfElement()) {
            case INT32 -> {
                return "I";
            }
            case BOOLEAN -> {
                return "Z";
            }
            case STRING, OBJECTREF -> {
                return "Ljava/lang/String;";
            }
            case VOID -> {
                return "V";
            }
            default -> throw new NotImplementedException(type.getTypeOfElement());
        }
    }

    private String getJasminType(ElementType type) {
        switch (type) {
            case INT32 -> {
                return "I";
            }
            case BOOLEAN -> {
                return "Z";
            }
            case STRING, OBJECTREF -> {
                return "Ljava/lang/String;";
            }
            case VOID -> {
                return "V";
            }
            default -> throw new NotImplementedException(type);
        }
    }

    private Integer getCurrentMethodVarVirtualRegisterFromElement(Element element) {
        Operand operand = (Operand) element;
        return currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
    }

    private String getAccessModifierString(AccessModifiers modifier) {
        return modifier == AccessModifiers.DEFAULT ? DEFAULT_ACCESS : modifier.name().toLowerCase();
    }
}
