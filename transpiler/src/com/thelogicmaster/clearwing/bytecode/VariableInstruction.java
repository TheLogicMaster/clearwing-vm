package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.BytecodeMethod;
import com.thelogicmaster.clearwing.StackEntry;
import com.thelogicmaster.clearwing.TranspilerException;
import com.thelogicmaster.clearwing.TypeVariants;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
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
        if (getMethod().isLocalKnown(local)) {
            if (opcode <= Opcodes.ALOAD)
                builder.append("\tvm::push(sp, ").append("local").append(local).append(");\n");
            else
                builder.append("\tlocal").append(local).append(" = vm::pop<").append(getLocalType().getArithmeticType()).append(">(sp);\n");
        } else
            appendStandardInstruction(builder, name, "" + local);
    }

    @Override
    public void appendUnoptimized(StringBuilder builder) {
        switch (opcode) {
            case Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD -> appendLoadStore(builder, Opcodes.ILOAD, "load");
            case Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE -> appendLoadStore(builder, Opcodes.ISTORE, "store");
            default -> throw new TranspilerException("Invalid opcode");
        }
    }

    private void appendOptimizedLoad(StringBuilder builder, String type, int temporaries) {
        builder.append("\t\tauto temp").append(temporaries);
        if (getMethod().isLocalKnown(local))
            builder.append(" = ").append("local").append(local).append(";\n");
        else
          builder.append(" = get<").append(type).append(">(local").append(local).append(");\n");
    }

    @Override
    public void appendOptimized(StringBuilder builder, List<StackEntry> operands, int temporaries) {
        switch (opcode) {
            case Opcodes.ILOAD -> appendOptimizedLoad(builder, "jint", temporaries);
            case Opcodes.LLOAD -> appendOptimizedLoad(builder, "jlong", temporaries);
            case Opcodes.FLOAD -> appendOptimizedLoad(builder, "jfloat", temporaries);
            case Opcodes.DLOAD -> appendOptimizedLoad(builder, "jdouble", temporaries);
            case Opcodes.ALOAD -> appendOptimizedLoad(builder, "jobject", temporaries);
            case Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE ->
                builder.append("\t\tlocal").append(local).append(" = ").append(operands.get(0)).append(";\n");
            default -> throw new TranspilerException("Invalid opcode");
        }
    }

    @Override
    public void resolveIO(List<StackEntry> stack) {
        switch (opcode) {
            case Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE -> setBasicInputs(getLocalType());
            default -> setBasicInputs();
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
