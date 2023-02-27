package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class InvokeDynamicInstruction extends Instruction {

    private final Handle handle;
    private final String className;
    private final String qualifiedProxyClassName;
    private final JavaType[] proxyFields;
    private final MethodSignature proxyMethodSignature;

    public InvokeDynamicInstruction(BytecodeMethod method, Handle handle, String proxyClassName, JavaType[] proxyFields, String proxyMethodDesc) {
        super(method, Opcodes.INVOKEDYNAMIC);
        this.handle = handle;
        this.className = proxyClassName;
        this.proxyFields = proxyFields;
        qualifiedProxyClassName = Utils.getQualifiedClassName(proxyClassName);
        proxyMethodSignature = new MethodSignature("", proxyMethodDesc);
    }

    @Override
    public void appendUnoptimized(StringBuilder builder) {
        builder.append("\t{ /* InvokeDynamic */\n");
        builder.append("\t\t").append(qualifiedProxyClassName).append("::clinit();\n");
        builder.append("\t\tauto proxy").append(" = make_shared<").append(qualifiedProxyClassName).append(">();\n");
        for (int i = proxyFields.length - 1; i >= 0; i--)
            builder.append("\t\tproxy->F_field").append(i).append(" = vm::pop<").append(proxyFields[i].getCppType()).append(">(sp);\n");
        builder.append("\t\tvm::push(sp, proxy);\n");
        builder.append("\t}\n");
    }

    @Override
    public void appendOptimized(StringBuilder builder, List<StackEntry> operands, int temporaries) {
        builder.append("\t\t").append(qualifiedProxyClassName).append("::clinit();\n");
        builder.append("\t\tauto temp").append(temporaries).append(" = make_shared<").append(qualifiedProxyClassName).append(">();\n");
        for (int i = 0; i < proxyFields.length; i++)
            builder.append("\t\ttemp").append(temporaries).append("->F_field").append(i).append(" = ")
                    .append(proxyFields[i].isPrimitive() ? operands.get(i) : operands.get(i).getTypedTemporary(proxyFields[i])).append(";\n");
    }

    @Override
    public void populateIO(List<StackEntry> stack) {
        inputs = new ArrayList<>();
        typedInputs = new ArrayList<>();
        for (JavaType type: proxyFields) {
            inputs.add(type.getBasicType().getArithmeticVariant());
            typedInputs.add(type.isPrimitive() ? null : type);
        }
        outputs = Collections.singletonList(TypeVariants.OBJECT);
    }

    @Override
    public void collectDependencies(Set<String> dependencies) {
        dependencies.add(Utils.sanitizeName(className));
    }

    public Handle getHandle() {
        return handle;
    }

    public String getClassName() {
        return className;
    }

    public String getQualifiedProxyClassName() {
        return qualifiedProxyClassName;
    }

    public JavaType[] getProxyFields() {
        return proxyFields;
    }

    public MethodSignature getProxyMethodSignature() {
        return proxyMethodSignature;
    }

    public Proxy getProxy(BytecodeMethod method) {
        return new Proxy(method);
    }

    public class Proxy extends Instruction {

        private final boolean isStatic;
        private final boolean isInterface;
        private final String target;
        private final String qualifiedTarget;
        private final String targetMethod;
        private final MethodSignature targetSignature;

        public Proxy(BytecodeMethod method) {
            super(method, -1);
            isStatic = handle.getTag() == Opcodes.H_INVOKESTATIC;
            isInterface = handle.getTag() == Opcodes.H_INVOKEINTERFACE;
            target = Utils.sanitizeName(handle.getOwner());
            qualifiedTarget = Utils.getQualifiedClassName(target);
            targetSignature = new MethodSignature(handle.getName(), handle.getDesc());
            targetMethod = Utils.sanitizeMethod(handle.getName(), targetSignature, isStatic, false);
        }

        @Override
        public void appendUnoptimized(StringBuilder builder) {
            boolean isConstructor = handle.getName().equals("<init>");

            builder.append("\t");
            boolean returnWrapped = false;
            if (!isConstructor && !targetSignature.getReturnType().isVoid()) {
                builder.append("return ");
                returnWrapped = targetSignature.getReturnType().appendWrapperPrefix(proxyMethodSignature.getReturnType(), builder);
            }

            if (isStatic)
                builder.append(qualifiedTarget).append("::");
            else if (isConstructor) {
                builder.append("\t").append(qualifiedTarget).append("::clinit();\n");
                builder.append("auto object = make_shared<").append(qualifiedTarget).append(">();\n");
                builder.append("\tobject->");
            } else {
                boolean objectMethodOnInterface = false;
                if (isInterface)
                    for (BytecodeMethod m: BytecodeClass.OBJECT_METHODS)
                        if (m.getSignature().equals(targetSignature)) {
                            objectMethodOnInterface = true;
                            builder.append("jobject(F_field0)->");
                        }
                if (!objectMethodOnInterface)
                    builder.append("object_cast<").append(qualifiedTarget).append(">(F_field0)->");
            }

            builder.append(targetMethod).append("(");

            boolean firstArg = true;
            int paramOffset = isStatic || isConstructor ? 0 : 1;
            for (int i = paramOffset; i < proxyFields.length; i++) {
                if (!firstArg)
                    builder.append(", ");
                boolean wrapped = proxyFields[i].appendWrapperPrefix(targetSignature.getParamTypes()[i - paramOffset], builder);
                builder.append("F_field").append(i);
                if (wrapped)
                    builder.append(")");
                firstArg = false;
            }
            for (int i = 0; i < proxyMethodSignature.getParamTypes().length; i++) {
                if (!firstArg)
                    builder.append(", ");
                JavaType targetType = targetSignature.getParamTypes()[i + proxyFields.length - paramOffset];
                JavaType paramType = proxyMethodSignature.getParamTypes()[i];
                boolean wrapped = paramType.appendWrapperPrefix(targetType, builder);
                boolean casted = !wrapped && !targetType.isPrimitive() && !targetType.getRegistryTypeName().equals(paramType.getRegistryTypeName());
                if (casted)
                    builder.append("object_cast<").append(Utils.getQualifiedClassName(targetType.getRegistryTypeName())).append(">(");
                builder.append("param").append(i);
                if (wrapped || casted)
                    builder.append(")");
                firstArg = false;
            }

            if (returnWrapped)
                builder.append(")");
            builder.append(");\n");

            if (isConstructor)
                builder.append("\treturn object;\n");
        }

        @Override
        public void collectDependencies(Set<String> dependencies) {
            dependencies.add(target);
            for (JavaType type: targetSignature.getParamTypes())
                if (!type.isPrimitive() && type.getArrayDimensions() == 0)
                    dependencies.add(type.getRegistryTypeName());
        }
    }
}
