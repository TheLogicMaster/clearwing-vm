package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;
import javassist.bytecode.Opcode;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
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
    public void appendUnoptimized(StringBuilder builder) {
        switch (opcode) {
            case Opcodes.NEW -> {
                builder.append("\t").append(qualifiedType).append("::").append("clinit();\n");
                builder.append("\tvm::push(sp, make_shared<").append(qualifiedType).append(">());\n");
            }
            case Opcodes.ANEWARRAY -> appendStandardInstruction(builder, "anewarray", javaType.generateClassFetch());
            case Opcodes.CHECKCAST -> appendStandardInstruction(builder, "checkcast", javaType.generateClassFetch());
            case Opcodes.INSTANCEOF -> appendStandardInstruction(builder, "instanceof", javaType.generateClassFetch());
            default -> throw new TranspilerException("Invalid opcode: " + opcode);
        }
    }

    @Override
    public void appendOptimized(StringBuilder builder, List<StackEntry> operands, int temporaries) {
        switch (opcode) {
            case Opcodes.NEW -> {
                builder.append("\t\t").append(qualifiedType).append("::clinit();\n");
                builder.append("\t\tauto temp").append(temporaries).append(" = make_shared<").append(qualifiedType).append(">();\n");
            }
            case Opcodes.ANEWARRAY ->
                builder.append("\t\tauto temp").append(temporaries).append(" = vm::newArray(").append(javaType.generateClassFetch())
                    .append(", ").append(operands.get(0)).append(");\n");
            case Opcodes.CHECKCAST ->
                builder.append("\t\tif (").append(operands.get(0)).append(" and !")
                    .append(javaType.generateClassFetch()).append("->M_isInstance_R_boolean(").append(operands.get(0))
                    .append(")) vm::throwNew<java::lang::ClassCastException>();\n");
            case Opcodes.INSTANCEOF ->
                builder.append("\t\tauto temp").append(temporaries).append(" = ")
                        .append(javaType.generateClassFetch()).append("->M_isInstance_R_boolean(").append(operands.get(0)).append(");\n");
            default -> throw new TranspilerException("Invalid opcode: " + opcode);
        }
    }

    @Override
    public void populateIO(List<StackEntry> stack) {
        switch (opcode) {
            case Opcodes.NEW -> {
                inputs = Collections.emptyList();
                outputs = Collections.singletonList(TypeVariants.OBJECT);
            }
            case Opcodes.ANEWARRAY -> {
                inputs = Collections.singletonList(TypeVariants.INT);
                outputs = Collections.singletonList(TypeVariants.OBJECT);
            }
            case Opcodes.CHECKCAST -> {
                inputs = Collections.singletonList(TypeVariants.OBJECT);
                outputs = Collections.singletonList(TypeVariants.OBJECT);
            }
            case Opcodes.INSTANCEOF -> {
                inputs = Collections.singletonList(TypeVariants.OBJECT);
                outputs = Collections.singletonList(TypeVariants.BOOLEAN);
            }
            default -> throw new TranspilerException("Invalid opcode: " + opcode);
        }
    }

    @Override
    public int adjustStack(List<StackEntry> operands, int temporaries) {
        if (opcode == Opcode.CHECKCAST)
            return 0;
        return super.adjustStack(operands, temporaries);
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
