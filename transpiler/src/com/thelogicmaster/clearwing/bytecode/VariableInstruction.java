package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;
import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * An instruction that stores or loads a local (XLOAD, XSTORE)
 */
public class VariableInstruction extends Instruction implements LocalInstruction {

    private final int local;

    public VariableInstruction(BytecodeMethod method, int opcode, int local) {
        super(method, opcode);
        this.local = local;
    }

    private void appendLoadStore(StringBuilder builder, int baseOpcode, String suffix) {
        String name = (TypeVariants.values()[TypeVariants.INT.ordinal() + opcode - baseOpcode].name().toLowerCase().charAt(0) + "").replace("o", "a") + suffix;
        appendStandardInstruction(builder, name, "" + local);
    }

    @Override
    public void appendUnoptimized(StringBuilder builder, TranspilerConfig config) {
        switch (opcode) {
            case Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD -> appendLoadStore(builder, Opcodes.ILOAD, "load");
            case Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE -> appendLoadStore(builder, Opcodes.ISTORE, "store");
            default -> throw new TranspilerException("Invalid opcode");
        }
    }

    @Override
    public void appendOptimized(StringBuilder builder, TranspilerConfig config) {
        switch (opcode) {
            case Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD ->
                    outputs.get(0).buildAssignment(builder).append("frame[").append(local).append("].")
                            .append(getLocalType().getStackName()).append(";\n");
            case Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE ->
                    builder.append("\tframe[").append(local).append("].").append(getLocalType().getStackName())
                            .append(" = ").append(inputs.get(0).arg()).append(";\n");
            default -> throw new TranspilerException("Invalid opcode");
        }
    }

    @Override
    public void appendInlined(StringBuilder builder) {
        switch (opcode) {
            case Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD ->
                    builder.append("frame[").append(local).append("].").append(getLocalType().getStackName());
            default -> throw new TranspilerException("Not inlinable");
        }
    }

    @Override
    public boolean inlineable() {
        return switch (opcode) {
            case Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD -> true;
            default -> false;
        };
    }

    @Override
    public void resolveIO(List<StackEntry> stack) {
        switch (opcode) {
            case Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE -> setInputsFromStack(stack, 1);
            default -> setInputs();
        }
        switch (opcode) {
            case Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD -> setBasicOutputs(getLocalType());
            default -> setOutputs();
        }
    }

    @Override
    public TypeVariants getLocalType() {
        return switch (opcode) {
            case Opcodes.ILOAD, Opcodes.ISTORE -> TypeVariants.INT;
            case Opcodes.LLOAD, Opcodes.LSTORE -> TypeVariants.LONG;
            case Opcodes.FLOAD, Opcodes.FSTORE -> TypeVariants.FLOAT;
            case Opcodes.DLOAD, Opcodes.DSTORE -> TypeVariants.DOUBLE;
            case Opcodes.ALOAD, Opcodes.ASTORE -> TypeVariants.OBJECT;
            default -> throw new TranspilerException("Invalid opcode: " + opcode);
        };
    }

    @Override
    public int getLocal() {
        return local;
    }
}
