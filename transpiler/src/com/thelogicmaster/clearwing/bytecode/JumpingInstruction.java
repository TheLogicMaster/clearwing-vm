package com.thelogicmaster.clearwing.bytecode;

import java.util.List;

import static com.thelogicmaster.clearwing.bytecode.Instruction.LABEL_PREFIX;

public interface JumpingInstruction {

    void setJumpBypass(int bypass, int label, int bypassLabel);

    void setJumpExceptionPops(int label, int pops);
    
    List<Integer> getJumpLabels();

    default void appendGoto(StringBuilder builder, int bypass, int label, int originalLabel, int pops) {
        if (bypass != -1 || pops > 0)
            builder.append("{ ");
        if (pops > 0)
            builder.append("popExceptionFrames(frameRef, ").append(pops).append("); ");
        if (bypass != -1)
            builder.append("bypasses[").append(bypass).append("] = true; ");
        builder.append("goto ").append(LABEL_PREFIX).append(label).append(";");
        if (bypass != -1 || pops > 0)
            builder.append(" }");
        if (bypass != -1)
            builder.append(" // goto ").append(LABEL_PREFIX).append(originalLabel);
    }
}
