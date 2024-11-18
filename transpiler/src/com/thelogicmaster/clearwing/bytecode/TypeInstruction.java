package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.Set;

/**
 * An instruction that takes a type as a parameter (NEW, ANEWARRAY, CHECKCAST, INSTANCEOF)
 */
public class TypeInstruction extends Instruction {

    private final String type;
    private final String qualifiedType;
    private final JavaType javaType;

    public TypeInstruction(BytecodeMethod method, int opcode, String type) {
        super(method, opcode);
        this.type = type;
        this.qualifiedType = Utils.getQualifiedClassName(type);
        javaType = new JavaType(type);
    }

    @Override
    public void appendUnoptimized(StringBuilder builder, TranspilerConfig config) {
        switch (opcode) {
            case Opcodes.NEW -> {
                builder.append("\tclinit_").append(qualifiedType).append("(ctx);\n");
                builder.append("\tPUSH_OBJECT(gcAlloc(ctx, &class_").append(qualifiedType).append("));\n");
            }
            case Opcodes.ANEWARRAY -> appendStandardInstruction(builder, "anewarray", javaType.generateClassFetch());
            case Opcodes.CHECKCAST -> appendStandardInstruction(builder, "checkcast", javaType.generateClassFetch());
            case Opcodes.INSTANCEOF -> appendStandardInstruction(builder, "instanceof", javaType.generateClassFetch());
            default -> throw new TranspilerException("Invalid opcode: " + opcode);
        }
    }

    @Override
    public void appendOptimized(StringBuilder builder, TranspilerConfig config) {
        switch (opcode) {
            case Opcodes.NEW -> {
                builder.append("\tclinit_").append(qualifiedType).append("(ctx);\n");
                outputs.get(0).buildAssignment(builder).append("gcAlloc(ctx, &class_").append(qualifiedType).append(");\n");;
            }
            case Opcodes.ANEWARRAY -> outputs.get(0).buildAssignment(builder).append("(jobject)createArray(ctx, ")
                    .append(javaType.generateClassFetch()).append(", ").append(inputs.get(0).arg()).append(");\n");
            case Opcodes.CHECKCAST -> outputs.get(0).buildAssignment(builder).append("checkCast(ctx, ")
                    .append(javaType.generateClassFetch()).append(", ").append(inputs.get(0).arg()).append(");\n");
            case Opcodes.INSTANCEOF -> outputs.get(0).buildAssignment(builder).append("isInstance(ctx, ")
                    .append(inputs.get(0).arg()).append(", ").append(javaType.generateClassFetch()).append(");\n");
            default -> throw new TranspilerException("Invalid opcode: " + opcode);
        }
    }

    @Override
    public boolean inlineable() {
        return opcode == Opcodes.CHECKCAST || opcode == Opcodes.INSTANCEOF;
    }

    @Override
    public void appendInlined(StringBuilder builder) {
        switch (opcode) {
            case Opcodes.CHECKCAST -> builder.append("checkCast(ctx, ")
                    .append(javaType.generateClassFetch()).append(", ").append(inputs.get(0).arg()).append(")");
            case Opcodes.INSTANCEOF -> builder.append("isInstance(ctx, ")
                    .append(inputs.get(0).arg()).append(", ").append(javaType.generateClassFetch()).append(")");
            default -> throw new TranspilerException("Not inlinable: " + opcode);
        }
    }

    @Override
    public void resolveIO(List<StackEntry> stack) {
        switch (opcode) {
            case Opcodes.NEW -> {
                setInputs();
                setBasicOutputs(TypeVariants.OBJECT);
            }
            case Opcodes.ANEWARRAY -> {
                setInputsFromStack(stack, 1);
                setBasicOutputs(TypeVariants.OBJECT);
            }
            case Opcodes.CHECKCAST -> {
                setInputsFromStack(stack, 1);
                setOutputs(inputs.get(0).copyOriginal(this));
            }
            case Opcodes.INSTANCEOF -> {
                setInputsFromStack(stack, 1);
                setBasicOutputs(TypeVariants.BOOLEAN);
            }
            default -> throw new TranspilerException("Invalid opcode: " + opcode);
        }
    }

    @Override
    public void collectDependencies(Set<String> dependencies) {
        if (javaType.getComponentType() == TypeVariants.OBJECT)
            dependencies.add(javaType.getRegistryTypeName());
    }

    public String getType() {
        return type;
    }

    public String getQualifiedType() {
        return qualifiedType;
    }
}
