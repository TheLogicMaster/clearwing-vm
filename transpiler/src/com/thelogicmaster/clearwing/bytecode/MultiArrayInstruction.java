package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.BytecodeMethod;
import com.thelogicmaster.clearwing.JavaType;
import com.thelogicmaster.clearwing.StackEntry;
import com.thelogicmaster.clearwing.TypeVariants;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
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
    public void appendUnoptimized(StringBuilder builder) {
        appendStandardInstruction(builder, "multianewarray", type.generateComponentClassFetch(), "" + dimensions);
    }

    @Override
    public void appendOptimized(StringBuilder builder, List<StackEntry> operands, int temporaries) {
        builder.append("\t\tauto temp").append(temporaries).append(" = vm::newMultiArray(").append(type.generateComponentClassFetch()).append(", {");
        boolean first = true;
        for (int i = 0; i < dimensions; i++) {
            if (!first)
                builder.append(", ");
            first = false;
            builder.append(operands.get(i));
        }
        builder.append("});\n");
    }

    @Override
    public void populateIO(List<StackEntry> stack) {
        inputs = new ArrayList<>();
        for (int i = 0; i < dimensions; i++)
            inputs.add(TypeVariants.INT);
        outputs = Collections.singletonList(TypeVariants.OBJECT);
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
