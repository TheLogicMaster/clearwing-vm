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
        private final boolean isSpecial;
        private final String target;
        private final String qualifiedTarget;
        private final String targetMethod;
        private final MethodSignature targetSignature;

        public Proxy(BytecodeMethod method) {
            super(method, -1);
            isStatic = handle.getTag() == Opcodes.H_INVOKESTATIC;
            isInterface = handle.getTag() == Opcodes.H_INVOKEINTERFACE;
            isSpecial = handle.getTag() == Opcodes.H_INVOKESPECIAL;
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
            boolean thisParam = !isConstructor && !isStatic && !isSpecial && targetSignature.getParamTypes().length < proxyMethodSignature.getParamTypes().length;
            
            builder.append("auto proxy = (").append(qualifiedProxyClassName).append(" *) self;\n");

            builder.append("\t");
            boolean returnWrapped = false;
            if (!isConstructor && !proxyMethodSignature.getReturnType().isVoid()) {
                builder.append("auto object = ");
                returnWrapped = targetSignature.getReturnType().appendWrapperPrefix(proxyMethodSignature.getReturnType(), builder);
            }

            String thisStr = thisParam ? "param0" : "(jobject)proxy->F_field0";
            if (isStatic)
                builder.append(targetMethod).append("(ctx");
            else if (isInterface) {
                builder.append("((func_").append(targetMethod.substring(2)).append(") resolveInterfaceMethod(ctx, &class_").append(qualifiedTarget)
                        .append(", INDEX_").append(targetMethod.substring(2)).append(", ").append(thisStr).append("))(ctx");
            } else if (isConstructor) {
                builder.append("auto object = gcAlloc(ctx, &class_").append(qualifiedTarget).append(");\n");
                builder.append("\t").append(targetMethod).append("(ctx, object");
            } else if (isSpecial) {
                builder.append(targetMethod).append("(ctx");
            } else
                builder.append("((func_").append(targetMethod.substring(2)).append(") ((void **) NULL_CHECK(").append(thisStr).append(")->vtable)[VTABLE_").append(targetMethod.substring(2)).append("])(ctx");

            if (thisParam)
                builder.append(", param0");
            
            int paramOffset = thisParam ? 1 : 0;
            int fieldOffset = !isStatic && !isConstructor && !thisParam ? 1 : 0;
            for (int i = 0; i < proxyFields.length; i++) {
                builder.append(", ");
                int targetIndex = i - fieldOffset;
                boolean wrapped = targetIndex >= 0 && proxyFields[i].appendWrapperPrefix(targetSignature.getParamTypes()[targetIndex], builder);
                builder.append(proxyFields[i].isPrimitive() ? "" : "(jobject) ").append("proxy->F_field").append(i);
                if (wrapped)
                    builder.append(")");
            }
            for (int i = paramOffset; i < proxyMethodSignature.getParamTypes().length; i++) {
                builder.append(", ");
                JavaType targetType = targetSignature.getParamTypes()[i + proxyFields.length - paramOffset - fieldOffset];
                JavaType paramType = proxyMethodSignature.getParamTypes()[i];
                boolean wrapped = paramType.appendWrapperPrefix(targetType, builder);
                builder.append("param").append(i);
                if (wrapped)
                    builder.append(")");
            }

            if (returnWrapped)
                builder.append(")");
            builder.append(");\n");

            builder.append("\tpopStackFrame(ctx);\n");
            if (isConstructor || !proxyMethodSignature.getReturnType().isVoid())
                builder.append("\treturn object;\n");
            else
                builder.append("\treturn;\n");
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
