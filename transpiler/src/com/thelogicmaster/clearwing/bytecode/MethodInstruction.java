package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;
import org.objectweb.asm.Opcodes;

import java.util.*;

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
    private final JavaType ownerType;
    private final boolean isStatic;
    private BytecodeClass ownerClass;
    private BytecodeMethod resolvedMethod;

    public MethodInstruction(BytecodeMethod method, int opcode, String owner, String name, String desc, boolean onInterface) {
        super(method, opcode);
        this.owner = owner;
        ownerType = new JavaType(owner);
        qualifiedOwner = ownerType.getArrayDimensions() > 0 ? "java_lang_Object" : Utils.getQualifiedClassName(owner);
        originalName = name;
        signature = new MethodSignature(name, desc, null);
        isStatic = opcode == Opcodes.INVOKESTATIC;
        this.name = Utils.sanitizeMethod(qualifiedOwner, signature, isStatic);
        this.desc = desc;
        this.onInterface = onInterface;
    }

    @Override
    public void processHierarchy(HashMap<String, BytecodeClass> classMap) {
        ownerClass = classMap.get(owner);
        if (ownerClass == null && !"java/lang/Object".equals(owner) && ownerType.getArrayDimensions() == 0)
            System.err.println("Failed to find owner " + owner + " for " + name);
    }

    @Override
    public void resolveSymbols() {
        if (ownerClass != null)
            resolvedMethod = resolveMethod(ownerClass);
        if (resolvedMethod == null && !isStatic && (!"<init>".equals(originalName) || "java/lang/Object".equals(owner)))
            for (BytecodeMethod m : BytecodeClass.OBJECT_METHODS)
                if (m.getSignature().equals(signature)) {
                    resolvedMethod = m;
                    break;
                }
    }

    // Todo: Move to BytecodeClass
    private BytecodeMethod resolveMethod(BytecodeClass clazz) {
        if (clazz == null)
            return null;
        for (BytecodeMethod m : clazz.getMethods())
            if (m.getSignature().equals(signature) && m.isStatic() == isStatic && (opcode != Opcodes.INVOKESPECIAL || !m.isAbstract()))
                return m;
        for (BytecodeClass c : clazz.getInterfaceClasses()) {
            BytecodeMethod resolved = resolveMethod(c);
            if (resolved != null)
                return resolved;
        }
        if (!clazz.getSuperName().equals("java/lang/Object"))
            return resolveMethod(clazz.getSuperClass());
        return null;
    }

    @Override
    public void appendUnoptimized(StringBuilder builder, TranspilerConfig config) {
        if (resolvedMethod == null)
            throw new TranspilerException("Method not resolved: " + owner + "." + originalName + " " + signature.getDesc() + " for " + method);
        if (signature.getParamTypes().length > 0 || opcode != Opcodes.INVOKESTATIC)
            builder.append("\tPOP_N(").append(signature.getParamTypes().length + (opcode != Opcodes.INVOKESTATIC ? 1 : 0)).append("); // Pop method args\n");
        builder.append("\t");
        if (!signature.getReturnType().isVoid())
            builder.append("sp->").append(signature.getReturnType().getBasicType().getStackName()).append(" = (").append(signature.getReturnType().getBasicType().getArithmeticType()).append(")");

        // Todo: Use invocation macros
        switch (opcode) {
            case Opcodes.INVOKEVIRTUAL ->
                builder.append("((func_").append(name.substring(2)).append(") ((void **) nullCheck(ctx, sp[0].o)->vtable)[VTABLE_").append(name.substring(2)).append("])");
            case Opcodes.INVOKEINTERFACE ->
                builder.append("((func_").append(resolvedMethod.getName().substring(2)).append(") resolveInterfaceMethod(ctx, &class_")
                        .append(resolvedMethod.getOwner().getQualifiedName()).append(", INDEX_").append(resolvedMethod.getName().substring(2)).append(", sp[0].o))");
            case Opcodes.INVOKESPECIAL, Opcodes.INVOKESTATIC -> builder.append(resolvedMethod.getName());
            default -> throw new TranspilerException("Invalid opcode");
        }
        builder.append("(ctx");
        if (opcode != Opcodes.INVOKESTATIC)
            builder.append(", sp[0].o");
        int paramOffset = opcode == Opcodes.INVOKESTATIC ? 0 : 1;
        for (int i = 0; i < signature.getParamTypes().length; i++) {
            JavaType type = signature.getParamTypes()[i];
            builder.append(", ");
            builder.append("sp[").append(paramOffset + i).append("].").append(type.getBasicType().getStackName());
        }
        builder.append(");\n");

        if (!signature.getReturnType().isVoid())
            builder.append("\tsp++;\n");
    }

    @Override
    public void appendOptimized(StringBuilder builder, TranspilerConfig config) {
        if (resolvedMethod == null)
            throw new TranspilerException("Method not resolved: " + owner + "." + originalName + " " + signature.getDesc() + " for " + method);
        
        if (!signature.getReturnType().isVoid())
            outputs.get(0).buildAssignment(builder).append("(")
                    .append(signature.getReturnType().getBasicType().getArithmeticType()).append(")");
        else
            builder.append("\t");

        // Todo: Use invocation macros
        switch (opcode) {
            case Opcodes.INVOKEVIRTUAL ->
                    builder.append("((func_").append(name.substring(2)).append(") ((void **) nullCheck(ctx, ").append(inputs.get(0).arg())
                            .append(")->vtable)[VTABLE_").append(name.substring(2)).append("])");
            case Opcodes.INVOKEINTERFACE ->
                    builder.append("((func_").append(resolvedMethod.getName().substring(2)).append(") resolveInterfaceMethod(ctx, &class_")
                            .append(resolvedMethod.getOwner().getQualifiedName()).append(", INDEX_").append(resolvedMethod.getName().substring(2))
                            .append(", ").append(inputs.get(0).arg()).append("))");
            case Opcodes.INVOKESPECIAL, Opcodes.INVOKESTATIC -> builder.append(resolvedMethod.getName());
            default -> throw new TranspilerException("Invalid opcode");
        }
        builder.append("(ctx");
        if (opcode != Opcodes.INVOKESTATIC)
            builder.append(", ").append(inputs.get(0).arg());
        int paramOffset = opcode == Opcodes.INVOKESTATIC ? 0 : 1;
        for (int i = 0; i < signature.getParamTypes().length; i++) {
            builder.append(", ");
            inputs.get(paramOffset + i).buildArg(builder);
        }
        builder.append(");\n");
    }

    @Override
    public void resolveIO(List<StackEntry> stack) {
        int argCount = 0;
        if (opcode != Opcodes.INVOKESTATIC)
            argCount++;
        argCount += signature.getParamTypes().length;
        setInputsFromStack(stack, argCount);
        if (signature.getReturnType().isVoid())
            setBasicOutputs();
        else
            setBasicOutputs(signature.getReturnType().getBasicType());
    }

    @Override
    public void collectDependencies(Set<String> dependencies) {
        if (ownerType.getArrayDimensions() == 0)
            dependencies.add(Utils.sanitizeName(owner));
        for (JavaType type: signature.getParamTypes())
            if (!type.isPrimitive() && type.getArrayDimensions() == 0)
                dependencies.add(type.getRegistryTypeName());
        if (!signature.getReturnType().isPrimitive() && signature.getReturnType().getArrayDimensions() == 0)
            dependencies.add(signature.getReturnType().getReferenceType());
        if (resolvedMethod != null)
            dependencies.add(resolvedMethod.getOwner().getName());
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
