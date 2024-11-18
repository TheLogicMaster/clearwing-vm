package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;
import org.objectweb.asm.Opcodes;

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
    public void appendUnoptimized(StringBuilder builder, TranspilerConfig config) {
        switch (opcode) {
            case Opcodes.BIPUSH -> appendStandardInstruction(builder, "bipush", "" + operand);
            case Opcodes.SIPUSH -> appendStandardInstruction(builder, "sipush", "" + operand);
            case Opcodes.NEWARRAY -> appendStandardInstruction(builder, "newarray", getArrayType());
            default -> throw new TranspilerException("Invalid opcode");
        }
    }

    @Override
    public void appendOptimized(StringBuilder builder, TranspilerConfig config) {
        switch (opcode) {
            case Opcodes.BIPUSH, Opcodes.SIPUSH -> outputs.get(0).buildAssignment(builder).append(operand).append(";\n");
            case Opcodes.NEWARRAY -> outputs.get(0).buildAssignment(builder).append("(jobject) createArray(ctx, ")
                    .append(getArrayType()).append(", ").append(inputs.get(0).arg()).append(");\n");
            default -> throw new TranspilerException("Invalid opcode");
        }
    }

    @Override
    public void appendInlined(StringBuilder builder) {
        switch (opcode) {
            case Opcodes.BIPUSH, Opcodes.SIPUSH -> builder.append(operand);
            default -> throw new TranspilerException("Not inlinable");
        }
    }

    @Override
    public boolean inlineable() {
        return opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH;
    }

    @Override
    public void resolveIO(List<StackEntry> stack) {
        if (opcode == Opcodes.NEWARRAY)
            setInputsFromStack(stack, 1);
        else
            setInputs();
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
