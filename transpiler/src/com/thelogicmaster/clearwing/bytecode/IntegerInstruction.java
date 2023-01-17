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
    public void populateIO(List<StackEntry> stack) {
        outputs = Collections.singletonList(opcode == Opcodes.NEWARRAY ? TypeVariants.OBJECT : TypeVariants.INT);
        inputs = opcode == Opcodes.NEWARRAY ? Collections.singletonList(TypeVariants.INT) : Collections.emptyList();
    }

    private String getArrayType() {
        return switch (operand) {
            case Opcodes.T_BOOLEAN -> "vm::classBoolean";
            case Opcodes.T_CHAR -> "vm::classChar";
            case Opcodes.T_FLOAT -> "vm::classFloat";
            case Opcodes.T_DOUBLE -> "vm::classDouble";
            case Opcodes.T_BYTE -> "vm::classByte";
            case Opcodes.T_SHORT -> "vm::classShort";
            case Opcodes.T_INT -> "vm::classInt";
            case Opcodes.T_LONG -> "vm::classLong";
            default -> throw new TranspilerException("Invalid array type");
        };
    }

    public int getOperand() {
        return operand;
    }
}
