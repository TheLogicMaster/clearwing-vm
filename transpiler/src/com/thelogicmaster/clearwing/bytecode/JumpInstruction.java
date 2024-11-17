package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.List;

/**
 * A jump instruction, conditional or not
 */
public class JumpInstruction extends Instruction implements JumpingInstruction {

    private final int originalLabel;

    private int bypass = -1;
    private int label;
    public int exceptionPops = 0;

    public JumpInstruction(BytecodeMethod method, int opcode, Label label) {
        super(method, opcode);
        originalLabel = method.getLabelId(label);
        this.label = originalLabel;
    }

    private void appendCompare(StringBuilder builder, TypeVariants type, String operation) {
        builder.append("\tsp -= 2; ");
        builder.append("if (sp[0].").append(type.getStackName()).append(" ").append(operation).append(" sp[1].").append(type.getStackName()).append(") ");
        appendGoto(builder, bypass, label, originalLabel, exceptionPops);
    }

    private void appendCompareZero(StringBuilder builder, TypeVariants type, String operation) {
        builder.append("\tif ((--sp)->").append(type.getStackName()).append(" ").append(operation).append(" ").append(type == TypeVariants.OBJECT ? "nullptr" : "0").append(") ");
        appendGoto(builder, bypass, label, originalLabel, exceptionPops);
    }

    // Todo: Ensure future stack optimizations don't break object comparisons (Only compare jobject base types)
    private void appendCompareOptimized(StringBuilder builder, TypeVariants type, String operation) {
        builder.append("\tif (").append(inputs.get(0).arg()).append(" ").append(operation);
        builder.append(" ").append(inputs.get(1).arg()).append(") ");
        appendGoto(builder, bypass, label, originalLabel, exceptionPops);
    }

    private void appendCompareZeroOptimized(StringBuilder builder, TypeVariants type, String operation) {
        builder.append("\tif (").append(inputs.get(0).arg()).append(" ").append(operation);
        builder.append(type == TypeVariants.OBJECT ? " nullptr" : " 0").append(") ");
        appendGoto(builder, bypass, label, originalLabel, exceptionPops);
    }

    @Override
    public void appendUnoptimized(StringBuilder builder, TranspilerConfig config) {
        switch (opcode) {
            case Opcodes.IFEQ -> appendCompareZero(builder, TypeVariants.INT, "==");
            case Opcodes.IFNE -> appendCompareZero(builder, TypeVariants.INT, "!=");
            case Opcodes.IFLT -> appendCompareZero(builder, TypeVariants.INT, "<");
            case Opcodes.IFGE -> appendCompareZero(builder, TypeVariants.INT, ">=");
            case Opcodes.IFGT -> appendCompareZero(builder, TypeVariants.INT, ">");
            case Opcodes.IFLE -> appendCompareZero(builder, TypeVariants.INT, "<=");
            case Opcodes.IFNULL -> appendCompareZero(builder, TypeVariants.OBJECT, "==");
            case Opcodes.IFNONNULL -> appendCompareZero(builder, TypeVariants.OBJECT, "!=");
            case Opcodes.IF_ICMPEQ -> appendCompare(builder, TypeVariants.INT, "==");
            case Opcodes.IF_ICMPNE -> appendCompare(builder, TypeVariants.INT, "!=");
            case Opcodes.IF_ICMPLT -> appendCompare(builder, TypeVariants.INT, "<");
            case Opcodes.IF_ICMPGE -> appendCompare(builder, TypeVariants.INT, ">=");
            case Opcodes.IF_ICMPGT -> appendCompare(builder, TypeVariants.INT, ">");
            case Opcodes.IF_ICMPLE -> appendCompare(builder, TypeVariants.INT, "<=");
            case Opcodes.IF_ACMPEQ -> appendCompare(builder, TypeVariants.OBJECT, "==");
            case Opcodes.IF_ACMPNE -> appendCompare(builder, TypeVariants.OBJECT, "!=");
            case Opcodes.GOTO -> {
                builder.append("\t");
                appendGoto(builder, bypass, label, originalLabel, exceptionPops);
            }
            default -> throw new TranspilerException("Invalid opcode");
        }
        builder.append("\n");
    }

    @Override
    public void appendOptimized(StringBuilder builder, TranspilerConfig config) {
        switch (opcode) {
            case Opcodes.IFEQ -> appendCompareZeroOptimized(builder, TypeVariants.INT, "==");
            case Opcodes.IFNE -> appendCompareZeroOptimized(builder, TypeVariants.INT, "!=");
            case Opcodes.IFLT -> appendCompareZeroOptimized(builder, TypeVariants.INT, "<");
            case Opcodes.IFGE -> appendCompareZeroOptimized(builder, TypeVariants.INT, ">=");
            case Opcodes.IFGT -> appendCompareZeroOptimized(builder, TypeVariants.INT, ">");
            case Opcodes.IFLE -> appendCompareZeroOptimized(builder, TypeVariants.INT, "<=");
            case Opcodes.IFNULL -> appendCompareZeroOptimized(builder, TypeVariants.OBJECT, "==");
            case Opcodes.IFNONNULL -> appendCompareZeroOptimized(builder, TypeVariants.OBJECT, "!=");
            case Opcodes.IF_ICMPEQ -> appendCompareOptimized(builder, TypeVariants.INT, "==");
            case Opcodes.IF_ICMPNE -> appendCompareOptimized(builder, TypeVariants.INT, "!=");
            case Opcodes.IF_ICMPLT -> appendCompareOptimized(builder, TypeVariants.INT, "<");
            case Opcodes.IF_ICMPGE -> appendCompareOptimized(builder, TypeVariants.INT, ">=");
            case Opcodes.IF_ICMPGT -> appendCompareOptimized(builder, TypeVariants.INT, ">");
            case Opcodes.IF_ICMPLE -> appendCompareOptimized(builder, TypeVariants.INT, "<=");
            case Opcodes.IF_ACMPEQ -> appendCompareOptimized(builder, TypeVariants.OBJECT, "==");
            case Opcodes.IF_ACMPNE -> appendCompareOptimized(builder, TypeVariants.OBJECT, "!=");
            case Opcodes.GOTO -> {
                builder.append("\t");
                appendGoto(builder, bypass, label, originalLabel, exceptionPops);
            }
            default -> throw new TranspilerException("Invalid opcode");
        }
        builder.append("\n");
    }

    @Override
    public void resolveIO(List<StackEntry> stack) {
        switch (opcode) {
            case Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE, Opcodes.IFNULL, Opcodes.IFNONNULL -> {
                setInputsFromStack(stack, 1);
            }
            case Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE, Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE, Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE -> {
                setInputsFromStack(stack, 2);
            }
            case Opcodes.GOTO -> setInputs();
            default -> throw new TranspilerException("Invalid opcode");
        }
        if (opcode != Opcodes.GOTO)
            setBasicOutputs();
    }

    @Override
    public void setJumpBypass(int bypass, int label, int bypassLabel) {
        this.bypass = bypass;
        this.label = bypassLabel;
    }

    @Override
    public void setJumpExceptionPops(int label, int pops) {
        exceptionPops = pops;
    }

    public int getBypass() {
        return bypass;
    }

    @Override
    public List<Integer> getJumpLabels() {
        return Collections.singletonList(label);
    }

    public int getOriginalLabel() {
        return originalLabel;
    }
}
