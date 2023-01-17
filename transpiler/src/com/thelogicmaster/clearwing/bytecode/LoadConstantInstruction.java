package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An LDC instruction for loading a constant pool value
 */
public class LoadConstantInstruction extends Instruction {

    private final Object value;

    public LoadConstantInstruction(BytecodeMethod method, Object value) {
        super(method, Opcodes.LDC);
        this.value = value;
    }

    private String getExpression() {
        if (value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Double || value instanceof Type)
            return Utils.getObjectValue(value);
        else if (value instanceof String)
            return Utils.encodeStringLiteral((String)value);
        else if (value instanceof Handle) {
            throw new TranspilerException("Unsupported value: " + value); // Todo
        }
        throw new TranspilerException("Invalid value: " + value);
    }

    @Override
    public void appendUnoptimized(StringBuilder builder) {
        builder.append("\tvm::push(sp, ").append(getExpression()).append(");\n");
    }

    @Override
    public void appendOptimized(StringBuilder builder, List<StackEntry> operands, int temporaries) {
        builder.append("\t\tauto temp").append(temporaries).append(" = ").append(getExpression()).append(";\n");
    }

    @Override
    public void populateIO(List<StackEntry> stack) {
        inputs = Collections.emptyList();
        if (value instanceof Integer)
            outputs = Collections.singletonList(TypeVariants.INT);
        else if (value instanceof Float)
            outputs = Collections.singletonList(TypeVariants.FLOAT);
        else if (value instanceof Long)
            outputs = Collections.singletonList(TypeVariants.LONG);
        else if (value instanceof Double)
            outputs = Collections.singletonList(TypeVariants.DOUBLE);
        else
            outputs = Collections.singletonList(TypeVariants.OBJECT);
    }

    @Override
    public void collectDependencies(Set<String> dependencies) {
        if (value instanceof Type && ((Type) value).getSort() == Type.OBJECT)
            dependencies.add(Utils.sanitizeName(((Type) value).getInternalName()));
    }

    public Object getValue() {
        return value;
    }
}
