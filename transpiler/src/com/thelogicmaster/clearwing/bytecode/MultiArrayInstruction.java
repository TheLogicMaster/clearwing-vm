package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.Set;

public class MultiArrayInstruction extends Instruction {

    private final String desc;
    private final int dimensions;
    private final JavaType type;

    public MultiArrayInstruction(BytecodeMethod method, String desc, int dimensions) {
        super(method, Opcodes.MULTIANEWARRAY);
        this.desc = desc;
        this.dimensions = dimensions;
        type = new JavaType(desc);
    }

    @Override
    public void appendUnoptimized(StringBuilder builder, TranspilerConfig config) {
        appendStandardInstruction(builder, "multianewarray", type.generateComponentClassFetch(), "" + dimensions);
    }

    @Override
    public void appendOptimized(StringBuilder builder, TranspilerConfig config) {
        outputs.get(0).buildAssignment(builder).append("(jobject)createMultiArray(ctx, ").append(type.generateComponentClassFetch()).append(", {");
        boolean first = true;
        for (int i = 0; i < dimensions; i++) {
            if (!first)
                builder.append(", ");
            first = false;
            builder.append(inputs.get(i).arg());
        }
        builder.append("});\n");
    }

    @Override
    public void resolveIO(List<StackEntry> stack) {
        setInputsFromStack(stack, dimensions);
        setBasicOutputs(TypeVariants.OBJECT);
    }

    @Override
    public void collectDependencies(Set<String> dependencies) {
        if (type.getComponentType() == TypeVariants.OBJECT)
            dependencies.add(type.getReferenceType());
    }

    public String getDesc() {
        return desc;
    }

    public int getDimensions() {
        return dimensions;
    }

    public JavaType getType() {
        return type;
    }
}
