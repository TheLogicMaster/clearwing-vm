package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.BytecodeMethod;
import com.thelogicmaster.clearwing.StackEntry;
import com.thelogicmaster.clearwing.TranspilerException;
import com.thelogicmaster.clearwing.TypeVariants;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.List;

/**
 * An instruction that takes a single integer as an operand
 */
public class IntegerInstruction extends Instruction {

    private final int operand;

    public IntegerInstruction(BytecodeMethod method, int opcode, int operand) {
        super(method, opcode);
        this.operand = operand;
    }

    @Override
    public void appendUnoptimized(StringBuilder builder) {
        switch (opcode) {
            case Opcodes.BIPUSH -> appendStandardInstruction(builder, "bipush", "" + operand);
            case Opcodes.SIPUSH -> appendStandardInstruction(builder, "sipush", "" + operand);
            case Opcodes.NEWARRAY -> appendStandardInstruction(builder, "newarray", getArrayType());
            default -> throw new TranspilerException("Invalid opcode");
        }
    }

    @Override
    public void appendOptimized(StringBuilder builder, List<StackEntry> operands, int temporaries) {
        builder.append("\t\tauto temp").append(temporaries).append(" = ");
        if (opcode == Opcodes.NEWARRAY)
            builder.append("vm::newArray(").append(getArrayType()).append(", ").append(operands.get(0)).append(");\n");
        else
            builder.append("jint(").append(operand).append(");\n");
    }

    @Override
    public void resolveIO(List<StackEntry> stack) {
        if (opcode == Opcodes.NEWARRAY)
            setBasicInputs(TypeVariants.INT);
        else
            setBasicInputs();
        setBasicOutputs(opcode == Opcodes.NEWARRAY ? TypeVariants.OBJECT : TypeVariants.INT);
    }

    private String getArrayType() {
        return switch (operand) {
            case Opcodes.T_BOOLEAN -> "&class_boolean";
            case Opcodes.T_CHAR -> "&class_char";
            case Opcodes.T_FLOAT -> "&class_float";
            case Opcodes.T_DOUBLE -> "&class_double";
            case Opcodes.T_BYTE -> "&class_byte";
            case Opcodes.T_SHORT -> "&class_short";
            case Opcodes.T_INT -> "&class_int";
            case Opcodes.T_LONG -> "&class_long";
            default -> throw new TranspilerException("Invalid array type");
        };
    }

    public int getOperand() {
        return operand;
    }
}
