package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An instruction for statically invoking a method
 */
public class MethodInstruction extends Instruction {

    private final String owner;
    private final String qualifiedOwner;
    private final String name;
    private final String originalName;
    private final String desc;
    private final boolean onInterface;
    private final MethodSignature signature;
    private final boolean arrayClone; // Special case for Array#clone
    private final JavaType ownerType;
    private boolean onThis;

    public MethodInstruction(BytecodeMethod method, int opcode, String owner, String name, String desc, boolean onInterface) {
        super(method, opcode);
        this.owner = owner;
        qualifiedOwner = Utils.getQualifiedClassName(owner);
        originalName = name;
        signature = new MethodSignature(name, desc);
        this.name = Utils.sanitizeMethod(name, signature, opcode == Opcodes.INVOKESTATIC, false);
        this.desc = desc;
        this.onInterface = onInterface;
        arrayClone = "clone".equals(name) && owner.contains("[");
        ownerType = new JavaType(owner);
    }

    @Override
    public void appendUnoptimized(StringBuilder builder) {
        if (signature.getParamTypes().length > 0 || opcode != Opcodes.INVOKESTATIC)
            builder.append("\tvm::pop(sp, ").append(signature.getParamTypes().length + (opcode != Opcodes.INVOKESTATIC ? 1 : 0)).append("); // Pop method args\n");
        builder.append("\t");
        if (!signature.getReturnType().isVoid())
            builder.append("vm::push(sp, ");
        switch (opcode) {
            case Opcodes.INVOKEVIRTUAL, Opcodes.INVOKEINTERFACE -> {
                if (arrayClone)
                    builder.append("object_cast<vm::Array>(vm::nullCheck(get<jobject>(sp[0])))->clone");
                else
                    builder.append("object_cast<").append(qualifiedOwner).append(">(vm::nullCheck(get<jobject>(sp[0])))->").append(name);
            }
            case Opcodes.INVOKESPECIAL ->
                    builder.append("object_cast<").append(qualifiedOwner).append(">(vm::nullCheck(get<jobject>(sp[0])))->").append(qualifiedOwner).append("::").append(name);
            case Opcodes.INVOKESTATIC -> builder.append(qualifiedOwner).append("::").append(name);
            default -> throw new TranspilerException("Invalid opcode");
        }
        builder.append("(");
        int paramOffset = opcode == Opcodes.INVOKESTATIC ? 0 : 1;
        boolean first = true;
        for (int i = 0; i < signature.getParamTypes().length; i++) {
            JavaType type = signature.getParamTypes()[i];
            if (!first)
                builder.append(", ");
            builder.append(type.getCppType()).append("(get<").append(type.getArithmeticType()).append(">(sp[").append(paramOffset + i).append("]))");
            first = false;
        }
        builder.append(")");
        if (!signature.getReturnType().isVoid())
            builder.append(")");
        builder.append(";\n");
    }

    private String getOwnerReference(List<StackEntry> operands) {
        if (onThis)
            return "this->";
        if (opcode == Opcodes.INVOKEINTERFACE)
            for (BytecodeMethod m: BytecodeClass.OBJECT_METHODS)
                if (m.getSignature().equals(signature))
                    return "temp" + operands.get(0).getTemporary() + "->";
        return operands.get(0).getTypedTemporary(ownerType) + "->";
    }

    @Override
    public void appendOptimized(StringBuilder builder, List<StackEntry> operands, int temporaries) {
        if (opcode != Opcodes.INVOKESTATIC && !onThis)
            builder.append("\t\tvm::nullCheck(").append(operands.get(0)).append(".get());\n");
        builder.append("\t\t");
        if (!signature.getReturnType().isVoid()) {
            builder.append("auto temp").append(temporaries).append(" = ");
            builder.append(signature.getReturnType().isPrimitive() ? signature.getReturnType().getArithmeticType() : "object_cast<java::lang::Object>").append("(");
        }
        switch (opcode) {
            case Opcodes.INVOKEVIRTUAL, Opcodes.INVOKEINTERFACE -> builder.append(getOwnerReference(operands)).append(name);
            case Opcodes.INVOKESPECIAL ->
                    builder.append(getOwnerReference(operands)).append(qualifiedOwner).append("::").append(name);
            case Opcodes.INVOKESTATIC -> builder.append(qualifiedOwner).append("::").append(name);
            default -> throw new TranspilerException("Invalid opcode");
        }
        builder.append("(");
        int paramOffset = opcode == Opcodes.INVOKESTATIC || onThis ? 0 : 1;
        boolean first = true;
        for (int i = 0; i < signature.getParamTypes().length; i++) {
            JavaType type = signature.getParamTypes()[i];
            if (!first)
                builder.append(", ");
            if (type.isPrimitive())
                builder.append(type.getCppType()).append("(").append(operands.get(paramOffset + i)).append(")");
            else
                builder.append(operands.get(paramOffset + i).getTypedTemporary(type));
            first = false;
        }
        if (!signature.getReturnType().isVoid())
            builder.append(")");
        builder.append(");\n");
    }

    @Override
    public void populateIO(List<StackEntry> stack) {
        inputs = new ArrayList<>();
        typedInputs = new ArrayList<>();
        if (opcode != Opcodes.INVOKESTATIC) {
            inputs.add(TypeVariants.OBJECT);
            typedInputs.add(null);
        }
        for (JavaType paramType: signature.getParamTypes()) {
            inputs.add(paramType.getBasicType());
            typedInputs.add(paramType.isPrimitive() ? null : paramType);
        }
        if (opcode != Opcodes.INVOKESTATIC && stack.size() >= inputs.size() && onThisCheck(stack.get(stack.size() - inputs.size()).getSource(), owner)) {
            onThis = true;
            inputs.remove(0);
            typedInputs.remove(0);
        }
        if (opcode != Opcodes.INVOKESTATIC && !onThis)
            typedInputs.set(0, ownerType);
        outputs = signature.getReturnType().isVoid() ? Collections.emptyList() : Collections.singletonList(signature.getReturnType().getBasicType());
    }

    @Override
    public void collectDependencies(Set<String> dependencies) {
        if (!arrayClone)
            dependencies.add(Utils.sanitizeName(owner));
        for (JavaType type: signature.getParamTypes())
            if (!type.isPrimitive() && type.getArrayDimensions() == 0)
                dependencies.add(type.getRegistryTypeName());
        if (!signature.getReturnType().isPrimitive() && signature.getReturnType().getArrayDimensions() == 0)
            dependencies.add(signature.getReturnType().getReferenceType());
    }

    public String getOwner() {
        return owner;
    }

    public String getQualifiedOwner() {
        return qualifiedOwner;
    }

    public String getName() {
        return name;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isOnInterface() {
        return onInterface;
    }

    public MethodSignature getSignature() {
        return signature;
    }
}
