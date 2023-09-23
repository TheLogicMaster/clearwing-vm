package com.thelogicmaster.clearwing;

import com.thelogicmaster.clearwing.bytecode.Instruction;

public class StackEntry {

    private final JavaType type;
    private final Instruction source;
    private final int temporary;

    public StackEntry(JavaType type, Instruction source) {
        this.type = type;
        this.source = source;
        temporary = -1;
    }

    public StackEntry(JavaType type, int temporary) {
        this.type = type;
        source = null;
        this.temporary = temporary;
    }

    public JavaType getType() {
        return type;
    }

    public TypeVariants getBasicType() {
        return type.getBasicType();
    }

    public Instruction getSource() {
        return source;
    }

    public int getTemporary() {
        return temporary;
    }

    public String getTypedTemporary(JavaType type) {
        if (type.getArrayDimensions() > 0)
            return "temp" + temporary + "_array";
        if (type.getReferenceType().equals("java/lang/Object"))
            return "temp" + temporary;
        return "temp" + temporary + "_" + type.getSafeName();
    }

    @Override
    public String toString() {
        return temporary >= 0 ? "temp" + temporary : "StackEntry";
    }
}
