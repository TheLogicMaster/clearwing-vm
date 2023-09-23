package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.BytecodeMethod;
import com.thelogicmaster.clearwing.StackEntry;
import com.thelogicmaster.clearwing.Utils;
import org.objectweb.asm.Label;

import java.util.*;

/**
 * A pseudo-instruction try-catch that gets inserted in parts after the start and end labels
 */
public class TryInstruction extends Instruction {

    private final int start;
    private final int end;
    private final int handler;
    private final String type;
    private final String qualifiedType;
    private final int label;
    private final CatchInstruction catchInstruction;

    private final HashMap<Integer, Bypass> bypasses = new HashMap<>();

    public TryInstruction(BytecodeMethod method, Label start, Label end, Label handler, String type) {
        super(method, -1);
        this.start = method.getLabelId(start);
        this.end = method.getLabelId(end);
        this.handler = method.getLabelId(handler);
        this.type = type;
        label = method.getLabelId(new Label());
        qualifiedType = type == null ? null : Utils.getQualifiedClassName(type);
        catchInstruction = new CatchInstruction();
    }

    // Todo: Switch from TRY macro, it leaks exceptions and makes it impossible to debug
    @Override
    public void appendUnoptimized(StringBuilder builder) {
        builder.append(LABEL_PREFIX).append(label).append(": if (setjmp(*pushExceptionFrame(frameRef, &class_")
                .append(qualifiedType == null ? "java_lang_Throwable" : qualifiedType).append("))) { sp = stack; PUSH_OBJECT(popExceptionFrame(frameRef)); goto ").append(LABEL_PREFIX).append(handler).append("; }\n");

//        builder.append(LABEL_PREFIX).append(label).append(":\n\tTRY (\n");

//        for (Map.Entry<Integer, Bypass> bypass: bypasses.entrySet()) {
//            builder.append("\tif (bypasses[").append(bypass.getValue().index).append("]) { bypasses[").append(bypass.getValue().index).append("] = false; ");
//            if (bypass.getValue().bypassed())
//                builder.append("bypasses[").append(bypass.getValue().index - 1).append("] = true; ");
//            builder.append("goto ").append(LABEL_PREFIX).append(bypass.getKey()).append("; }\n");
//        }
    }

    @Override
    public void collectDependencies(Set<String> dependencies) {
        if (type != null)
            dependencies.add(Utils.sanitizeName(type));
        else
            dependencies.add("java/lang/Throwable");
    }

    public Bypass getBypass(int target, int originalTarget) {
        if (!bypasses.containsKey(target))
            bypasses.put(target, new Bypass(getMethod().allocateTryCatchBypass(), target, originalTarget));
        return bypasses.get(target);
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

    public class CatchInstruction extends Instruction implements JumpingInstruction {

        private int bypass = -1;
        private int label;

        private CatchInstruction() {
            super(TryInstruction.this.getMethod(), -1);
            label = handler;
        }

        @Override
        public void appendUnoptimized(StringBuilder builder) {
//            builder.append("\t} CATCH (").append(qualifiedType == null ? "java_lang_Throwable" : qualifiedType).append(" ex) {\n");
//            builder.append("\tsp = stack;\n");
//            builder.append("\tPUSH_OBJECT(ex);\n");
//            builder.append("\t");
//            appendGoto(builder, bypass, label, handler);
//            builder.append("\t)\n");

            builder.append("\tpopExceptionFrame(frameRef);\n");
        }

        @Override
        public void setJumpBypass(int bypass, int label, int bypassLabel) {
            this.bypass = bypass;
            this.label = bypassLabel;
        }

        public int getBypass() {
            return bypass;
        }

        @Override
        public List<Integer> getJumpLabels() {
            return Collections.singletonList(label);
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

        public boolean bypassed() {
            return target != originalTarget;
        }
    }
}
