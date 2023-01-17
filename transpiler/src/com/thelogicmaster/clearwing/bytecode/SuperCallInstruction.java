package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;

public class SuperCallInstruction extends Instruction {

    private final String superClass;
    private final String name;
    private final MethodSignature signature;

    public SuperCallInstruction(BytecodeMethod method, String superClass, String name, MethodSignature signature) {
        super(method, -1);
        this.superClass = superClass;
        this.name = name;
        this.signature = signature;
    }

    @Override
    public void appendUnoptimized(StringBuilder builder) {
        builder.append("\t");
        if (!signature.getReturnType().isVoid())
            builder.append("return ");
        builder.append(Utils.getQualifiedClassName(superClass)).append("::").append(name).append("(");
        JavaType[] paramTypes = signature.getParamTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append(" param").append(i);
        }
        builder.append(");\n");
    }
}
