package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Set;

/**
 * An LDC instruction for loading a constant pool value
 */
public class LoadConstantInstruction extends Instruction {

    private final Object value;
    private final TypeVariants type;

    public LoadConstantInstruction(BytecodeMethod method, Object value) {
        super(method, Opcodes.LDC);
        this.value = value;
        if (value instanceof Integer)
            type = TypeVariants.INT;
        else if (value instanceof Long)
            type = TypeVariants.LONG;
        else if (value instanceof Float)
            type = TypeVariants.FLOAT;
        else if (value instanceof Double)
            type = TypeVariants.DOUBLE;
        else
            type = TypeVariants.OBJECT;
    }

    private String getExpression() {
        if (value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Double || value instanceof Type)
            return Utils.getObjectValue(value);
        else if (value instanceof String)
            return "((jobject) createStringLiteral(ctx, " + Utils.encodeStringLiteral((String)value) + "))";
        else if (value instanceof Handle) {
            throw new TranspilerException("Unsupported value: " + value); // Todo
        }
        throw new TranspilerException("Invalid value: " + value);
    }

    @Override
    public void appendUnoptimized(StringBuilder builder, TranspilerConfig config) {
        builder.append("\t(sp++)->").append(type.getStackName()).append(" = ").append(getExpression()).append(";\n");
    }

    @Override
    public void appendOptimized(StringBuilder builder, TranspilerConfig config) {
        outputs.get(0).buildAssignment(builder).append(getExpression()).append(";\n");
    }

    @Override
    public void resolveIO(List<StackEntry> stack) {
        setInputs();
        setBasicOutputs(type);
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
