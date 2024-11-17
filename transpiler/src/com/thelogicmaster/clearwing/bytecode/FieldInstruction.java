package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;
import org.objectweb.asm.Opcodes;

import java.util.*;

/**
 * An instruction that gets or sets a field
 */
public class FieldInstruction extends Instruction {

    private final String owner;
    private final String qualifiedOwner;
    private final String name;
    private final String originalName;
    private final String desc;
    private final JavaType type;
    private final JavaType ownerType;
    private final boolean isStatic;
    private boolean weak;
    private BytecodeClass ownerClass;
    private BytecodeClass realOwnerClass;
    private String realName;

    public FieldInstruction(BytecodeMethod method, int opcode, String owner, String name, String desc) {
        super(method, opcode);
        this.owner = Utils.sanitizeName(owner);
        qualifiedOwner = Utils.getQualifiedClassName(owner);
        originalName = name;
        isStatic = opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC;
        this.name = Utils.sanitizeField(qualifiedOwner, name, isStatic);
        this.desc = desc;
        type = new JavaType(desc);
        ownerType = new JavaType(owner);
    }

    private BytecodeClass findRealOwner(HashMap<String, BytecodeClass> classMap, BytecodeClass clazz) {
        for (BytecodeField field : clazz.getFields())
            if (field.getOriginalName().equals(originalName) && field.isStatic() == isStatic && field.getDesc().equals(desc))
                return clazz;
        
        for (String inter : clazz.getInterfaces()) {
            BytecodeClass iClazz = classMap.get(inter);
            if (iClazz == null)
                continue;
            for (BytecodeField field : iClazz.getFields())
                if (field.getOriginalName().equals(originalName) && field.isStatic() == isStatic && field.getDesc().equals(desc))
                    return iClazz;
            iClazz = findRealOwner(classMap, iClazz);
            if (iClazz != null)
                return iClazz;
        }
        
        BytecodeClass parent = classMap.get(clazz.getSuperName());
        if (parent == null)
            return null;
        return findRealOwner(classMap, parent);
    }
    
    @Override
    public void processHierarchy(HashMap<String, BytecodeClass> classMap) {
        ownerClass = classMap.get(owner);
        if (ownerClass == null)
            System.err.println("Failed to find owner class for: " + owner);

        // Real owner could be a super class
        realOwnerClass = ownerClass == null ? null : findRealOwner(classMap, ownerClass);
        
        if (realOwnerClass == null)
            System.err.println("Failed to locate field owner for: " + owner + "." + originalName);
        else
            realName = Utils.sanitizeField(realOwnerClass.getQualifiedName(), originalName, isStatic);

//        if (!classMap.containsKey(owner))
//            return;
//        BytecodeClass clazz = classMap.get(owner);
//        while (clazz != null && !clazz.getName().equals("java/lang/Object")) {
//            for (BytecodeField field: clazz.getFields())
//                if (field.getOriginalName().equals(originalName)) {
//                    weak = field.isWeak();
//                    return;
//                }
//            clazz = classMap.get(clazz.getSuperName());
//        }
    }

//    private String locateRealOwner() {
//        BytecodeClass clazz = ownerClass;
//        while (clazz != null) {
//            for (BytecodeField field : clazz.getFields())
//                if (field.getOriginalName().equals(originalName))
//                    return clazz.getQualifiedName();
//            clazz = clazz.getSuperClass();
//        }
//        throw new TranspilerException("Failed to locate field owner");
//    }

    @Override
    public void appendUnoptimized(StringBuilder builder, TranspilerConfig config) {
        if (realOwnerClass == null)
            throw new TranspilerException("Failed to find owner class for: " + name + " needed for " + method.getName());
        
        if (isStatic) // Todo: Skip on same class for clinit
            builder.append("\tclinit_").append(qualifiedOwner).append("(ctx);\n");
        
        switch (opcode) {
            case Opcodes.GETSTATIC ->
                    builder.append("\t(sp++)->").append(type.getBasicType().getStackName()).append(" = ").append(realName).append(";\n");
            case Opcodes.PUTSTATIC ->
                builder.append("\t").append(realName).append(" = (--sp)->").append(type.getBasicType().getStackName()).append(";\n");
            case Opcodes.GETFIELD -> {
                builder.append("\tsp[-1].").append(type.getBasicType().getStackName()).append(" = ").append(type.isPrimitive() ? "" : "(jobject) ")
                        .append("((").append(realOwnerClass.getQualifiedName()).append(" *) NULL_CHECK(sp[-1].o))->").append(name).append(";\n");
            }
            case Opcodes.PUTFIELD -> {
                builder.append("\tsp -= 2;\n");
                builder.append("\t((").append(realOwnerClass.getQualifiedName()).append(" *) NULL_CHECK(sp[0].o))->").append(name).append(" = ").append(type.isPrimitive() ? "" : "(jref) ")
                        .append("sp[1].").append(type.getBasicType().getStackName()).append(";\n");
            }
            default -> throw new TranspilerException("Invalid opcode");
        }
    }

    @Override
    public void appendOptimized(StringBuilder builder, TranspilerConfig config) {
        if (realOwnerClass == null)
            throw new TranspilerException("Failed to find owner class for: " + name + " needed for " + method.getName());
        
        if (isStatic) // Todo: Skip on same class for clinit
            builder.append("\tclinit_").append(qualifiedOwner).append("(ctx);\n");

        switch (opcode) {
            case Opcodes.GETSTATIC -> outputs.get(0).buildAssignment(builder).append("(")
                    .append(type.getBasicType().getArithmeticType()).append(")").append(realName).append(";\n");
            case Opcodes.PUTSTATIC ->
                    builder.append("\t").append(realName).append(" = ").append(inputs.get(0).arg()).append(";\n");
            case Opcodes.GETFIELD -> outputs.get(0).buildAssignment(builder).append("(")
                    .append(type.getBasicType().getArithmeticType()).append(")").append("((")
                    .append(realOwnerClass.getQualifiedName()).append(" *) NULL_CHECK(")
                    .append(inputs.get(0).arg()).append("))->").append(name).append(";\n");
            case Opcodes.PUTFIELD -> builder.append("\t((").append(realOwnerClass.getQualifiedName())
                    .append(" *) NULL_CHECK(").append(inputs.get(0).arg()).append("))->").append(name)
                    .append(" = ").append(type.isPrimitive() ? "" : "(jref) ").append(inputs.get(1).arg()).append(";\n");
            default -> throw new TranspilerException("Invalid opcode");
        }
    }

    @Override
    public void resolveIO(List<StackEntry> stack) {
        if (opcode == Opcodes.GETFIELD)
            setInputsFromStack(stack, 1);
        else if (opcode == Opcodes.PUTFIELD)
            setInputsFromStack(stack, 2);
        else if (opcode == Opcodes.PUTSTATIC)
            setInputsFromStack(stack, 1);
        else
            setInputs();

        if (opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC)
            setBasicOutputs(type.getBasicType());
        else
            setBasicOutputs();
    }

    @Override
    public void collectDependencies(Set<String> dependencies) {
        dependencies.add(Utils.sanitizeName(owner));
        if (!type.isPrimitive() && type.getArrayDimensions() == 0)
            dependencies.add(type.getRegistryTypeName());
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

    public JavaType getType() {
        return type;
    }
}
