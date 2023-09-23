package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.util.*;

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
        proxyMethodSignature = new MethodSignature("", proxyMethodDesc, null);
    }

    @Override
    public void appendUnoptimized(StringBuilder builder) {
        builder.append("\t{ /* InvokeDynamic */\n");
        builder.append("\t\tclinit_").append(qualifiedProxyClassName).append("(ctx);\n");
        builder.append("\t\tauto proxy").append(" = (").append(qualifiedProxyClassName).append(" *) gcAlloc(ctx, &class_").append(qualifiedProxyClassName).append(");\n");
        for (int i = proxyFields.length - 1; i >= 0; i--)
            builder.append("\t\tproxy->F_field").append(i).append(" = ").append(proxyFields[i].isPrimitive() ? "" : "(jref) ").append("(--sp)->").append(proxyFields[i].getBasicType().getStackName()).append(";\n");
        builder.append("\t\tPUSH_OBJECT((jobject) proxy);\n");
        builder.append("\t}\n");
    }

    @Override
    public void appendOptimized(StringBuilder builder, List<StackEntry> operands, int temporaries) {
//        builder.append("\t\t").append(qualifiedProxyClassName).append("::clinit();\n");
//        builder.append("\t\tauto temp").append(temporaries).append(" = make_shared<").append(qualifiedProxyClassName).append(">();\n");
//        for (int i = 0; i < proxyFields.length; i++)
//            builder.append("\t\ttemp").append(temporaries).append("->F_field").append(i).append(" = ")
//                    .append(proxyFields[i].isPrimitive() ? operands.get(i) : operands.get(i).getTypedTemporary(proxyFields[i])).append(";\n");
    }

    @Override
    public void resolveIO(List<StackEntry> stack) {
        setInputs(proxyFields);
        setBasicOutputs(TypeVariants.OBJECT);
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
            targetSignature = new MethodSignature(handle.getName(), handle.getDesc(), null);
            targetMethod = Utils.sanitizeMethod(handle.getOwner(), targetSignature, isStatic);
        }

        @Override
        public void resolveSymbols() {
            // Todo: Resolve methods the same way as MethodInstruction, if needed
        }

        @Override
        public void appendUnoptimized(StringBuilder builder) {
            boolean isConstructor = handle.getName().equals("<init>");
            builder.append("auto proxy = (").append(qualifiedProxyClassName).append(" *) self;\n");

            builder.append("\t");
            boolean returnWrapped = false;
            if (!isConstructor && !targetSignature.getReturnType().isVoid()) {
                builder.append("return ");
                returnWrapped = targetSignature.getReturnType().appendWrapperPrefix(proxyMethodSignature.getReturnType(), builder);
            }

            if (isStatic)
                builder.append(targetMethod).append("(ctx");
            else if (isInterface) {
                builder.append("((func_").append(targetMethod.substring(2)).append(") resolveInterfaceMethod(ctx, &class_").append(target)
                        .append(", INDEX_").append(targetMethod.substring(2)).append(", (jobject) proxy->F_field0))(ctx, (jobject) proxy->F_field0");
            } else if (isConstructor) {
                builder.append("auto object = gcAlloc(ctx, &class_").append(qualifiedTarget).append(");\n");
                builder.append("\t").append(targetMethod).append("(ctx, object");
            } else
                builder.append("((func_").append(targetMethod.substring(2)).append(") ((void **) NULL_CHECK((jobject) proxy->F_field0)->vtable)[VTABLE_").append(targetMethod.substring(2)).append("])(ctx, (jobject) proxy->F_field0");

            int paramOffset = isStatic || isConstructor ? 0 : 1;
            for (int i = paramOffset; i < proxyFields.length; i++) {
                builder.append(", ");
                boolean wrapped = proxyFields[i].appendWrapperPrefix(targetSignature.getParamTypes()[i - paramOffset], builder);
                builder.append(proxyFields[i].isPrimitive() ? "" : "(jobject) ").append("proxy->F_field").append(i);
                if (wrapped)
                    builder.append(")");
            }
            for (int i = 0; i < proxyMethodSignature.getParamTypes().length; i++) {
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
            }

            if (returnWrapped)
                builder.append(")");
            builder.append(");\n");

            if (isConstructor)
                builder.append("\treturn object;\n");
        }

        @Override
        public void resolveIO(List<StackEntry> stack) {
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
