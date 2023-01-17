package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
    private boolean onThis;

    public FieldInstruction(BytecodeMethod method, int opcode, String owner, String name, String desc) {
        super(method, opcode);
        this.owner = owner;
        this.qualifiedOwner = Utils.getQualifiedClassName(owner);
        originalName = name;
        this.name = Utils.sanitizeField(name, opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC);
        this.desc = desc;
        type = new JavaType(desc);
        ownerType = new JavaType(owner);
    }

    @Override
    public void appendUnoptimized(StringBuilder builder) {
        if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC)
            builder.append("\t").append(qualifiedOwner).append("::clinit();\n");

        switch (opcode) {
            case Opcodes.GETSTATIC ->
                builder.append("\tvm::push(sp, ").append(type.getArithmeticType()).append("(").append(qualifiedOwner).append("::").append(name).append("));\n");
            case Opcodes.PUTSTATIC -> {
                builder.append("\t").append(qualifiedOwner).append("::").append(name).append(" = ").append(type.getCppType()).append("(");
                builder.append("vm::pop<").append(type.getArithmeticType()).append(">(sp));\n");
            }
            case Opcodes.GETFIELD -> {
                builder.append("\tvm::push(sp, ").append(type.getArithmeticType()).append("(object_cast<").append(qualifiedOwner);
                builder.append(">(vm::nullCheck(vm::pop<jobject>(sp)))->").append(name).append("));\n");
            }
            case Opcodes.PUTFIELD ->
                builder.append("\t{\n" + "\tauto &temp = vm::pop<").append(type.getArithmeticType()).append(">(sp);\n")
                    .append("\tobject_cast<").append(qualifiedOwner).append(">(vm::nullCheck(vm::pop<jobject>(sp)))->").append(name).append(" = temp;\n")
                    .append("\t}\n");
            default -> throw new TranspilerException("Invalid opcode");
        }
    }

    @Override
    public void populateIO(List<StackEntry> stack) {
        if (opcode == Opcodes.GETFIELD) {
            inputs = Collections.singletonList(TypeVariants.OBJECT);
            if (!stack.isEmpty() && onThisCheck(stack.get(stack.size() - 1).getSource(), owner)) {
                onThis = true;
                inputs = Collections.emptyList();
            } else
                typedInputs = Collections.singletonList(ownerType);
        } else if (opcode == Opcodes.PUTFIELD) {
            inputs = Arrays.asList(TypeVariants.OBJECT, type.getBasicType());
            if (stack.size() >= 2 && onThisCheck(stack.get(stack.size() - 2).getSource(), owner)) {
                onThis = true;
                inputs = Collections.singletonList(type.getBasicType());
                typedInputs = Collections.singletonList(type.isPrimitive() ? null : type);
            } else
                typedInputs = Arrays.asList(ownerType, type.isPrimitive() ? null : type);
        } else if (opcode == Opcodes.PUTSTATIC) {
            inputs = Collections.singletonList(type.getBasicType());
            typedInputs = Collections.singletonList(type.isPrimitive() ? null : type);
        } else
            inputs = Collections.emptyList();

        if (opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC)
            outputs = Collections.singletonList(type.getBasicType());
        else
            outputs = Collections.emptyList();
    }

    private String getFieldReference(List<StackEntry> operands) {
        return onThis ? "this->" + name : operands.get(0).getTypedTemporary(ownerType) + "->" + name;
    }

    private String getValueOperand(StackEntry entry) {
        return type.isPrimitive() ? entry.toString() : entry.getTypedTemporary(type);
    }

    @Override
    public void appendOptimized(StringBuilder builder, List<StackEntry> operands, int temporaries) {
        if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC)
            builder.append("\t\t").append(qualifiedOwner).append("::clinit();\n");

        if ((opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD) && !onThis)
            builder.append("\t\tvm::nullCheck(").append(operands.get(0)).append(".get());\n");

        switch (opcode) {
            case Opcodes.GETSTATIC ->
                builder.append("\t\tauto temp").append(temporaries).append(" = ").append(type.isPrimitive() ? type.getArithmeticType() : "object_cast<java::lang::Object>")
                        .append("(").append(qualifiedOwner).append("::").append(name).append(");\n");
            case Opcodes.PUTSTATIC ->
                builder.append("\t\t").append(qualifiedOwner).append("::").append(name).append(" = ").append(getValueOperand(operands.get(0))).append(";\n");
            case Opcodes.GETFIELD ->
                    builder.append("\t\tauto temp").append(temporaries).append(" = ").append(type.isPrimitive() ? type.getArithmeticType() : "object_cast<java::lang::Object>")
                            .append("(").append(getFieldReference(operands)).append(");\n");
            case Opcodes.PUTFIELD ->
                builder.append("\t\t").append(getFieldReference(operands)).append(" = ").append(getValueOperand(operands.get(onThis ? 0 : 1))).append(";\n");
            default -> throw new TranspilerException("Invalid opcode");
        }
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
