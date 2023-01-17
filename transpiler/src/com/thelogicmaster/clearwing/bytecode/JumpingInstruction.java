package com.thelogicmaster.clearwing.bytecode;

import java.util.List;

import static com.thelogicmaster.clearwing.bytecode.Instruction.LABEL_PREFIX;

public interface JumpingInstruction {

    void setJumpBypass(int bypass, int label, int bypassLabel);

    List<Integer> getJumpLabels();

    default void appendGoto(StringBuilder builder, int bypass, int label, int originalLabel) {
        if (bypass != -1)
            builder.append("{ bypasses[").append(bypass).append("] = true; goto ").append(LABEL_PREFIX).append(label).append("; } // goto ")
                    .append(LABEL_PREFIX).append(originalLabel).append("\n");
        else
            builder.append("goto ").append(LABEL_PREFIX).append(label).append(";\n");
    }
}
