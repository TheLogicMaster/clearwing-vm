package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.BytecodeMethod;
import com.thelogicmaster.clearwing.StackEntry;

import java.util.List;

public class CustomInstruction extends Instruction {

    private final String code;

    public CustomInstruction(BytecodeMethod method, String code) {
        super(method, -1);
        this.code = code;
    }

    @Override
    public void resolveIO(List<StackEntry> stack) {
        setBasicInputs();
        setBasicOutputs();
    }

    @Override
    public void appendUnoptimized(StringBuilder builder) {
        builder.append(code);
    }
}
