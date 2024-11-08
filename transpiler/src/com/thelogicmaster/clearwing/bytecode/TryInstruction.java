package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.BytecodeMethod;
import com.thelogicmaster.clearwing.StackEntry;
import com.thelogicmaster.clearwing.Utils;
import org.objectweb.asm.Label;

import java.util.*;

/**
 * A pseudo-instruction try-catch that gets inserted in parts after the start and end labels
 */
public class TryInstruction extends Instruction implements JumpingInstruction {

    private final int start;
    private final int end;
    private int handler;
    private final int originalHandler;
    private final String type;
    private final String qualifiedType;
    private final int label;
    private final CatchInstruction catchInstruction;

    private final ArrayList<Bypass> bypasses = new ArrayList<>();
    private int handlerBypass = -1;
    private int exceptionPops = 0;

    public TryInstruction(BytecodeMethod method, Label start, Label end, Label handler, String type) {
        super(method, -1);
        this.start = method.getLabelId(start);
        this.end = method.getLabelId(end);
        originalHandler = this.handler = method.getLabelId(handler);
        this.type = type;
        label = method.getLabelId(new Label());
        qualifiedType = type == null ? null : Utils.getQualifiedClassName(type);
        catchInstruction = new CatchInstruction();
    }

    @Override
    public void appendUnoptimized(StringBuilder builder) {
        builder.append(LABEL_PREFIX).append(label).append(": if (setjmp(*pushExceptionFrame(frameRef, &class_")
                .append(qualifiedType == null ? "java_lang_Throwable" : qualifiedType).append("))) {\n\t\tsp = stack; PUSH_OBJECT(popExceptionFrame(frameRef)); ");
        appendGoto(builder, handlerBypass, handler, originalHandler, exceptionPops);
        builder.append("\n\t}\n");
        
        for (Bypass bypass: bypasses) {
            builder.append("\tif (bypasses[").append(bypass.index).append("]) { ");
            if (bypass.isLast())
                builder.append("bypasses[").append(bypass.index).append("] = false; ");
            builder.append("goto ").append(LABEL_PREFIX).append(bypass.target).append("; }\n");
        }
    }

    @Override
    public void collectDependencies(Set<String> dependencies) {
        if (type != null)
            dependencies.add(Utils.sanitizeName(type));
        else
            dependencies.add("java/lang/Throwable");
    }

    public void createBypass(int target, int originalTarget, int bypassIndex) {
        bypasses.add(new Bypass(bypassIndex, target, originalTarget));
    }

    @Override
    public void setJumpExceptionPops(int label, int pops) {
        exceptionPops = pops - 1;
    }

    @Override
    public List<Integer> getJumpLabels() {
        return List.of(handler);
    }

    @Override
    public void setJumpBypass(int bypass, int label, int bypassLabel) {
        handler = bypassLabel;
        handlerBypass = bypass;
    }

    @Override
    public void resolveIO(List<StackEntry> stack) {
        setBasicInputs();
        setBasicOutputs();
    }

    /**
     * Get a label uniquely identifying this try-instruction
     */
    public int getLabel() {
        return label;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getHandler() {
        return handler;
    }

    public String getType() {
        return type;
    }

    public String getQualifiedType() {
        return qualifiedType;
    }

    public CatchInstruction getCatchInstruction() {
        return catchInstruction;
    }

    public class CatchInstruction extends Instruction {
        
        private CatchInstruction() {
            super(TryInstruction.this.getMethod(), -1);
        }

        @Override
        public void appendUnoptimized(StringBuilder builder) {
            builder.append("\tpopExceptionFrame(frameRef);\n");
        }
        
        public int getLabel() {
            return handler;
        }

        @Override
        public void resolveIO(List<StackEntry> stack) {
            setBasicInputs();
            setBasicOutputs();
        }
    }

    public static class Bypass {
        private final int index;
        private final int target;
        private final int originalTarget;

        public Bypass(int index, int target, int originalTarget) {
            this.index = index;
            this.target = target;
            this.originalTarget = originalTarget;
        }

        public int getIndex() {
            return index;
        }

        public int getTarget() {
            return target;
        }

        public int getOriginalTarget() {
            return originalTarget;
        }
        
        public boolean isLast() {
            return target == originalTarget;
        }
    }
}
