package com.thelogicmaster.clearwing;

import com.thelogicmaster.clearwing.bytecode.Instruction;

import java.util.ArrayList;

public class StackEntry {

    private final JavaType type;
    private final Instruction source;
    private final StackEntry original;
    private final ArrayList<Instruction> consumers = new ArrayList<>();

    private OperandType opType;
    private int index;

    public StackEntry(JavaType type, Instruction source, StackEntry original) {
        this.type = type;
        this.source = source;
        this.original = original;
    }

    public StackEntry(JavaType type, Instruction source) {
        this(type, source, null);
    }
    
    public void makeTemporary(int index) {
        opType = OperandType.Temporary;
        this.index = index;
    }

    public void makeStack(int index) {
        opType = OperandType.Stack;
        this.index = index;
    }

    public void makeInlined() {
        opType = OperandType.Inlined;
    }
    
    public StackEntry copy(Instruction source) {
        return new StackEntry(type, source, this);
    }
    
    public JavaType getType() {
        return type;
    }

    public TypeVariants getBasicType() {
        return type.getBasicType();
    }

    public OperandType getOperandType() {
        return opType;
    }
    
    public Instruction getSource() {
        return source;
    }
    
    public StackEntry getOriginal() {
        return original == null ? this : original.getOriginal();
    }
    
    public void addConsumer(Instruction consumer) {
        consumers.add(consumer);
    }
    
    public ArrayList<Instruction> getConsumers() {
        return consumers;
    }

    public int getIndex() {
        return index;
    }
    
    public StringBuilder buildArg(StringBuilder builder) {
        if (original != null) {
            original.buildArg(builder);
            return builder;
        }
        switch (opType) {
            case Stack -> builder.append("sp[").append(index).append("].").append(type.getBasicType().getStackName());
            case Temporary -> builder.append("temp").append(index);
            case Inlined -> {
                builder.append("(");
                source.appendInlined(builder);
                builder.append(")");
            }
            default -> throw new TranspilerException("Invalid operand type");
        }
        return builder;
    }

    public String arg() {
        return buildArg(new StringBuilder()).toString();
    }

    public StringBuilder buildAssignment(StringBuilder builder) {
        if (original != null) {
            original.buildAssignment(builder);
            return builder;
        }
        builder.append("\t");
        switch (opType) {
            case Stack -> builder.append("sp[").append(index).append("].").append(type.getBasicType().getStackName());
            case Temporary -> builder.append(type.getArithmeticType()).append(" temp").append(index);
            default -> throw new TranspilerException("Invalid operand type");
        }
        return builder.append(" = ");
    }

    @Override
    public String toString() {
        return opType + " " + type.getArithmeticType();
    }
}
