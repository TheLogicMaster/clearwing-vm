package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.BytecodeMethod;
import com.thelogicmaster.clearwing.StackEntry;
import com.thelogicmaster.clearwing.TranspilerConfig;

import java.util.List;

public class CustomInstruction extends Instruction {

    private final String code;

    public CustomInstruction(BytecodeMethod method, String code) {
        super(method, -1);
        this.code = code;
    }

    @Override
    public void resolveIO(List<StackEntry> stack) {
        setInputs();
        setBasicOutputs();
    }

    @Override
    public void appendUnoptimized(StringBuilder builder, TranspilerConfig config) {
        builder.append(code);
    }
}
