package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;
import javassist.bytecode.Opcode;
import org.objectweb.asm.Opcodes;

import java.util.*;

/**
 * An instruction that takes zero operands
 */
public class ZeroOperandInstruction extends Instruction {

    public ZeroOperandInstruction(BytecodeMethod method, int opcode) {
        super(method, opcode);
    }

    private String getOpcodeConst(int zeroOpcode) {
        return Integer.toString(opcode - zeroOpcode);
    }

    private void appendConst(StringBuilder builder, String name, int zeroOpcode) {
        appendStandardInstruction(builder, name, getOpcodeConst(zeroOpcode));
    }

    // Todo: Is this still needed with noreturn annotations?
    private void appendThrowReturn(StringBuilder builder) {
        builder.append("return");
        if (method.getSignature().getReturnType().getBasicType() == TypeVariants.OBJECT)
            builder.append(" nullptr");
        else if (!method.getSignature().getReturnType().isVoid())
            builder.append(" 0");
        builder.append(";\n");
    }

    @Override
    public void appendUnoptimized(StringBuilder builder, TranspilerConfig config) {
        switch (opcode) {
            case Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5:
                appendConst(builder, "iconst", Opcodes.ICONST_0);
                break;
            case Opcodes.LCONST_0, Opcodes.LCONST_1:
                appendConst(builder, "lconst", Opcodes.LCONST_0);
                break;
            case Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2:
                appendConst(builder, "fconst", Opcodes.FCONST_0);
                break;
            case Opcodes.DCONST_0, Opcodes.DCONST_1:
                appendConst(builder, "dconst", Opcodes.DCONST_0);
                break;
            case Opcodes.NOP:
            case Opcodes.ACONST_NULL:
            case Opcodes.IALOAD, Opcodes.LALOAD, Opcodes.FALOAD, Opcodes.DALOAD, Opcodes.AALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD:
            case Opcodes.IASTORE, Opcodes.LASTORE, Opcodes.FASTORE, Opcodes.DASTORE, Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE:
            case Opcodes.POP, Opcodes.DUP, Opcodes.DUP_X1, Opcodes.SWAP:
            case Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD:
            case Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB:
            case Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL:
            case Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV:
            case Opcodes.IREM, Opcodes.LREM, Opcodes.FREM, Opcodes.DREM:
            case Opcodes.INEG, Opcodes.LNEG, Opcodes.FNEG, Opcodes.DNEG:
            case Opcodes.ISHL, Opcodes.LSHL, Opcodes.ISHR, Opcodes.LSHR, Opcodes.IUSHR, Opcodes.LUSHR:
            case Opcodes.IAND, Opcodes.LAND:
            case Opcodes.IOR, Opcodes.LOR:
            case Opcodes.IXOR, Opcodes.LXOR:
            case Opcodes.I2L, Opcodes.I2F, Opcodes.I2D:
            case Opcodes.L2I, Opcodes.L2F, Opcodes.L2D:
            case Opcodes.F2I, Opcodes.F2L, Opcodes.F2D:
            case Opcodes.D2I, Opcodes.D2L, Opcodes.D2F:
            case Opcodes.I2B, Opcodes.I2C, Opcodes.I2S:
            case Opcodes.LCMP, Opcodes.FCMPL, Opcodes.FCMPG, Opcodes.DCMPL, Opcodes.DCMPG:
            case Opcodes.ARRAYLENGTH:
                appendStandardInstruction(builder, Objects.requireNonNull(getOpcodeName()));
                break;
            case Opcodes.IRETURN, Opcodes.LRETURN, Opcodes.FRETURN, Opcodes.DRETURN, Opcodes.ARETURN, Opcodes.RETURN:
                builder.append("\tpopStackFrame(ctx);\n");
                appendStandardInstruction(builder, Objects.requireNonNull(getOpcodeName()));
                break;
            case Opcodes.ATHROW:
                appendStandardInstruction(builder, "athrow");
                builder.append("\t");
                appendThrowReturn(builder);
                break;
            case Opcodes.DUP_X2, Opcodes.DUP2_X1:
                if (inputs.size() == 3)
                    appendStandardInstruction(builder, getOpcodeName() + "_1");
                else
                    appendStandardInstruction(builder, getOpcodeName() + "_2");
                break;
            case Opcodes.DUP2, Opcodes.POP2:
                if (inputs.size() == 2)
                    appendStandardInstruction(builder, getOpcodeName() + "_1");
                else
                    appendStandardInstruction(builder, getOpcodeName() + "_2");
                break;
            case Opcodes.DUP2_X2:
                appendStandardInstruction(builder, getOpcodeName() + "_" + switch (outputs.size()){ case 6 -> 1; case 5 -> 3; case 4 -> 2; case 3 -> 4; default -> 0; });
                break;
            case Opcodes.MONITORENTER:
                appendStandardInstruction(builder, "monitorenter");
                break;
            case Opcodes.MONITOREXIT:
                appendStandardInstruction(builder, "monitorexit");
                break;
            default:
                throw new TranspilerException("Invalid opcode");
        }
    }

    @Override
    public void appendOptimized(StringBuilder builder, TranspilerConfig config) {
        TypeVariants type;
        switch (opcode) {
            case Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5 ->
                outputs.get(0).buildAssignment(builder).append("jint(").append(getOpcodeConst(Opcodes.ICONST_0)).append(");\n");
            case Opcodes.LCONST_0, Opcodes.LCONST_1 ->
                outputs.get(0).buildAssignment(builder).append("jlong(").append(getOpcodeConst(Opcodes.LCONST_0)).append(");\n");
            case Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2 ->
                outputs.get(0).buildAssignment(builder).append("jfloat(").append(getOpcodeConst(Opcodes.FCONST_0)).append(");\n");
            case Opcodes.DCONST_0, Opcodes.DCONST_1 ->
                outputs.get(0).buildAssignment(builder).append("jdouble(").append(getOpcodeConst(Opcodes.DCONST_0)).append(");\n");
            case Opcodes.NOP -> {}
            case Opcodes.ACONST_NULL -> outputs.get(0).buildAssignment(builder).append("jobject(nullptr);\n");
            case Opcodes.IALOAD, Opcodes.LALOAD, Opcodes.FALOAD, Opcodes.DALOAD, Opcodes.AALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD -> {
                type = opcodeType(Opcodes.IALOAD);
                outputs.get(0).buildAssignment(builder).append("(").append(type.getArithmeticType()).append(")ARRAY_ACCESS(")
                        .append(type.getCppType()).append(", ").append(inputs.get(0).arg()).append(", ")
                        .append(inputs.get(1).arg()).append(");\n");
            }
            case Opcodes.IASTORE, Opcodes.LASTORE, Opcodes.FASTORE, Opcodes.DASTORE, Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE -> {
                type = opcodeType(Opcodes.IASTORE);
                builder.append("\tARRAY_ACCESS(").append(type.getCppType()).append(", ").append(inputs.get(0).arg()).append(", ")
                        .append(inputs.get(1).arg()).append(") = ").append(inputs.get(2).arg()).append(";\n");
            }
            case Opcodes.POP, Opcodes.POP2, Opcodes.DUP, Opcodes.DUP_X1, Opcodes.DUP_X2, Opcodes.DUP2, Opcodes.DUP2_X1, Opcodes.DUP2_X2, Opcodes.SWAP ->
                throw new TranspilerException("Routing instruction not optimizable");
            case Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD ->
                outputs.get(0).buildAssignment(builder).append(inputs.get(0).arg()).append(" + ").append(inputs.get(1).arg()).append(";\n");
            case Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB ->
                outputs.get(0).buildAssignment(builder).append(inputs.get(0).arg()).append(" - ").append(inputs.get(1).arg()).append(";\n");
            case Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL ->
                outputs.get(0).buildAssignment(builder).append(inputs.get(0).arg()).append(" * ").append(inputs.get(1).arg()).append(";\n");
            case Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV ->
                outputs.get(0).buildAssignment(builder).append(inputs.get(0).arg()).append(" / ").append(inputs.get(1).arg()).append(";\n");
            case Opcodes.IREM, Opcodes.LREM ->
                outputs.get(0).buildAssignment(builder).append(inputs.get(0).arg()).append(" % ").append(inputs.get(1).arg()).append(";\n");
            case Opcodes.FREM ->
                outputs.get(0).buildAssignment(builder).append("fmodf(").append(inputs.get(0).arg()).append(", ").append(inputs.get(1).arg()).append(");\n");
            case Opcodes.DREM ->
                outputs.get(0).buildAssignment(builder).append("fmod(").append(inputs.get(0).arg()).append(", ").append(inputs.get(1).arg()).append(");\n");
            case Opcodes.INEG, Opcodes.LNEG, Opcodes.FNEG, Opcodes.DNEG ->
                outputs.get(0).buildAssignment(builder).append("-").append(inputs.get(0).arg()).append(";\n");
            case Opcodes.ISHL, Opcodes.LSHL ->
                outputs.get(0).buildAssignment(builder).append(inputs.get(0).arg()).append(" << ").append(inputs.get(1).arg()).append(";\n");
            case Opcodes.ISHR, Opcodes.LSHR ->
                outputs.get(0).buildAssignment(builder).append(inputs.get(0).arg()).append(" >> ").append(inputs.get(1).arg()).append(";\n");
            case Opcodes.IUSHR ->
                outputs.get(0).buildAssignment(builder).append("bit_cast<jint>(bit_cast<uint32_t>(").append(inputs.get(0).arg())
                        .append(") >> ").append(inputs.get(1).arg()).append(");\n");
            case Opcodes.LUSHR ->
                outputs.get(0).buildAssignment(builder).append("bit_cast<jlong>(bit_cast<uint64_t>(").append(inputs.get(0).arg())
                            .append(") >> ").append(inputs.get(1).arg()).append(");\n");
            case Opcodes.IAND, Opcodes.LAND ->
                outputs.get(0).buildAssignment(builder).append(inputs.get(0).arg()).append(" & ").append(inputs.get(1).arg()).append(";\n");
            case Opcodes.IOR, Opcodes.LOR ->
                outputs.get(0).buildAssignment(builder).append(inputs.get(0).arg()).append(" | ").append(inputs.get(1).arg()).append(";\n");
            case Opcodes.IXOR, Opcodes.LXOR ->
                outputs.get(0).buildAssignment(builder).append(inputs.get(0).arg()).append(" ^ ").append(inputs.get(1).arg()).append(";\n");
            case Opcodes.I2L, Opcodes.F2L, Opcodes.D2L ->
                outputs.get(0).buildAssignment(builder).append("jlong(").append(inputs.get(0).arg()).append(");\n");
            case Opcodes.I2F, Opcodes.L2F, Opcodes.D2F ->
                outputs.get(0).buildAssignment(builder).append("jfloat(").append(inputs.get(0).arg()).append(");\n");
            case Opcodes.I2D, Opcodes.L2D, Opcodes.F2D ->
                outputs.get(0).buildAssignment(builder).append("jdouble(").append(inputs.get(0).arg()).append(");\n");
            case Opcodes.L2I, Opcodes.F2I, Opcodes.D2I ->
                outputs.get(0).buildAssignment(builder).append("jint(").append(inputs.get(0).arg()).append(");\n");
            case Opcodes.I2B -> outputs.get(0).buildAssignment(builder).append("jbyte(").append(inputs.get(0).arg()).append(");\n");
            case Opcodes.I2C -> outputs.get(0).buildAssignment(builder).append("jchar(").append(inputs.get(0).arg()).append(");\n");
            case Opcodes.I2S -> outputs.get(0).buildAssignment(builder).append("jshort(").append(inputs.get(0).arg()).append(");\n");
            case Opcodes.LCMP ->
                outputs.get(0).buildAssignment(builder).append("longCompare(").append(inputs.get(0).arg()).append(", ").append(inputs.get(1).arg()).append(");\n");
            case Opcodes.FCMPL ->
                outputs.get(0).buildAssignment(builder).append("floatCompare(").append(inputs.get(0).arg()).append(", ").append(inputs.get(1).arg()).append(", -1);\n");
            case Opcodes.FCMPG ->
                outputs.get(0).buildAssignment(builder).append("floatCompare(").append(inputs.get(0).arg()).append(", ").append(inputs.get(1).arg()).append(", 1);\n");
            case Opcodes.DCMPL ->
                outputs.get(0).buildAssignment(builder).append("doubleCompare(").append(inputs.get(0).arg()).append(", ").append(inputs.get(1).arg()).append(", -1);\n");
            case Opcodes.DCMPG ->
                outputs.get(0).buildAssignment(builder).append("doubleCompare(").append(inputs.get(0).arg()).append(", ").append(inputs.get(1).arg()).append(", 1);\n");
            case Opcodes.ARRAYLENGTH ->
                outputs.get(0).buildAssignment(builder).append("((jarray) nullCheck(ctx, ").append(inputs.get(0).arg()).append("))->length;\n");
            case Opcodes.ATHROW -> {
                builder.append("\tthrowException(ctx, ").append(inputs.get(0).arg()).append(");");
                appendThrowReturn(builder);
            }
            case Opcodes.IRETURN, Opcodes.LRETURN, Opcodes.FRETURN, Opcodes.DRETURN, Opcodes.ARETURN -> {
                builder.append("\tpopStackFrame(ctx);\n");
                builder.append("\treturn ").append(inputs.get(0).arg()).append(";\n");
            }
            case Opcodes.RETURN -> {
                builder.append("\tpopStackFrame(ctx);\n");
                builder.append("\treturn;\n");
            }
            case Opcodes.MONITORENTER -> builder.append("\tmonitorEnter(ctx, ").append(inputs.get(0).arg()).append(");\n");
            case Opcodes.MONITOREXIT -> builder.append("\tmonitorExit(ctx, ").append(inputs.get(0).arg()).append(");\n");
            default -> throw new TranspilerException("Invalid opcode");
        }
    }

    private TypeVariants opcodeType(int baseOpcode) {
        return switch (opcode - baseOpcode) {
            case 0 -> TypeVariants.INT;
            case 1 -> TypeVariants.LONG;
            case 2 -> TypeVariants.FLOAT;
            case 3 -> TypeVariants.DOUBLE;
            case 4 -> TypeVariants.OBJECT;
            case 5 -> TypeVariants.BYTE;
            case 6 -> TypeVariants.CHAR;
            case 7 -> TypeVariants.SHORT;
            default -> throw new TranspilerException("Invalid opcode for type: " + opcode);
        };
    }

    @Override
    public void resolveIO(List<StackEntry> stack) {
        TypeVariants type, type2, type3;
        switch (opcode) {
            case Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5:
                setInputs();
                setBasicOutputs(TypeVariants.INT);
                break;
            case Opcodes.LCONST_0, Opcodes.LCONST_1:
                setInputs();
                setBasicOutputs(TypeVariants.LONG);
                break;
            case Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2:
                setInputs();
                setBasicOutputs(TypeVariants.FLOAT);
                break;
            case Opcodes.DCONST_0, Opcodes.DCONST_1:
                setInputs();
                setBasicOutputs(TypeVariants.DOUBLE);
                break;
            case Opcodes.NOP:
                setInputs();
                setBasicOutputs();
                break;
            case Opcodes.ACONST_NULL:
                setInputs();
                setBasicOutputs(TypeVariants.OBJECT);
                break;
            case Opcodes.IALOAD, Opcodes.LALOAD, Opcodes.FALOAD, Opcodes.DALOAD, Opcodes.AALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD:
                setInputsFromStack(stack, 2);
                setBasicOutputs(opcodeType(Opcode.IALOAD));
                break;
            case Opcodes.IASTORE, Opcodes.LASTORE, Opcodes.FASTORE, Opcodes.DASTORE, Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE:
                setInputsFromStack(stack, 3);
                setBasicOutputs();
                break;
            case Opcodes.POP:
                setInputsFromStack(stack, 1);
                setBasicOutputs();
                break;
            case Opcodes.POP2:
                type = stack.get(stack.size() - 1).getBasicType();
                if (!type.isWide() && stack.size() < 2)
                    break;
                setInputsFromStack(stack, type.isWide() ? 1 : 2);
                setBasicOutputs();
                break;
            case Opcodes.DUP:
                if (setInputsFromStack(stack, 1))
                    setOutputs(inputs.get(0).copy(this), inputs.get(0).copy(this));
                break;
            case Opcodes.DUP_X1:
                if (setInputsFromStack(stack, 2))
                    setOutputs(inputs.get(1).copy(this), inputs.get(0).copy(this), inputs.get(1).copy(this));
                break;
            case Opcodes.DUP_X2:
                if (stack.size() < 2)
                    break;
                type2 = stack.get(stack.size() - 2).getBasicType();
                if (type2.isWide()) { // Form 2
                    setInputsFromStack(stack, 2);
                    setOutputs(inputs.get(1).copy(this), inputs.get(0).copy(this), inputs.get(1).copy(this));
                } else { // Form 1
                    if (stack.size() < 3)
                        break;
                    setInputsFromStack(stack, 3);
                    setOutputs(inputs.get(2).copy(this), inputs.get(0).copy(this), inputs.get(1).copy(this), inputs.get(2).copy(this));
                }
                break;
            case Opcodes.DUP2:
                if (stack.isEmpty())
                    break;
                type = stack.get(stack.size() - 1).getBasicType();
                if (type.isWide()) { // Form 2
                    setInputsFromStack(stack, 1);
                    setOutputs(inputs.get(0).copy(this), inputs.get(0).copy(this));
                } else { // Form 1
                    if (stack.size() < 2)
                        break;
                    setInputsFromStack(stack, 2);
                    setOutputs(inputs.get(0).copy(this), inputs.get(1).copy(this), inputs.get(0).copy(this), inputs.get(1).copy(this));
                }
                break;
            case Opcodes.DUP2_X1:
                if (stack.size() < 2)
                    break;
                type = stack.get(stack.size() - 1).getBasicType();
                if (type.isWide()) { // Form 2
                    setInputsFromStack(stack, 2);
                    setOutputs(inputs.get(1).copy(this), inputs.get(0).copy(this), inputs.get(1).copy(this));
                } else { // Form 1
                    if (stack.size() < 3)
                        break;
                    setInputsFromStack(stack, 3);
                    setOutputs(inputs.get(1).copy(this), inputs.get(2).copy(this), inputs.get(0).copy(this), inputs.get(1).copy(this), inputs.get(2).copy(this));
                }
                break;
            case Opcodes.DUP2_X2:
                if (stack.size() < 2)
                    break;
                type = stack.get(stack.size() - 1).getBasicType();
                type2 = stack.get(stack.size() - 2).getBasicType();
                if (type.isWide() && type2.isWide()) { // Form 4
                    setInputsFromStack(stack, 2);
                    setOutputs(inputs.get(1).copy(this), inputs.get(0).copy(this), inputs.get(1).copy(this));
                } else {
                    if (stack.size() < 3)
                        break;
                    type3 = stack.get(stack.size() - 3).getBasicType();
                    if (type.isWide()) { // Form 2
                        setInputsFromStack(stack, 3);
                        setOutputs(inputs.get(2).copy(this), inputs.get(0).copy(this), inputs.get(1).copy(this), inputs.get(2).copy(this));
                    } else if (type3.isWide()) { // Form 3
                        setInputsFromStack(stack, 3);
                        setOutputs(inputs.get(1).copy(this), inputs.get(2).copy(this), inputs.get(0).copy(this), inputs.get(1).copy(this), inputs.get(2).copy(this));
                    } else { // Form 1
                        if (stack.size() < 4)
                            break;
                        setInputsFromStack(stack, 4);
                        setOutputs(inputs.get(2).copy(this), inputs.get(3).copy(this), inputs.get(0).copy(this), inputs.get(1).copy(this), inputs.get(2).copy(this), inputs.get(3).copy(this));
                    }
                }
                break;
            case Opcodes.SWAP:
                if (setInputsFromStack(stack, 2))
                    setOutputs(inputs.get(1).copy(this), inputs.get(0).copy(this));
                break;
            case Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD:
                type = opcodeType(Opcodes.IADD);
                setInputsFromStack(stack, 2);
                setBasicOutputs(type);
                break;
            case Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB:
                type = opcodeType(Opcodes.ISUB);
                setInputsFromStack(stack, 2);
                setBasicOutputs(type);
                break;
            case Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL:
                type = opcodeType(Opcodes.IMUL);
                setInputsFromStack(stack, 2);
                setBasicOutputs(type);
                break;
            case Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV:
                type = opcodeType(Opcodes.IDIV);
                setInputsFromStack(stack, 2);
                setBasicOutputs(type);
                break;
            case Opcodes.IREM, Opcodes.LREM, Opcodes.FREM, Opcodes.DREM:
                type = opcodeType(Opcodes.IREM);
                setInputsFromStack(stack, 2);
                setBasicOutputs(type);
                break;
            case Opcodes.INEG, Opcodes.LNEG, Opcodes.FNEG, Opcodes.DNEG:
                type = opcodeType(Opcodes.INEG);
                setInputsFromStack(stack, 1);
                setBasicOutputs(type);
                break;
            case Opcodes.ISHL, Opcodes.LSHL, Opcodes.ISHR, Opcodes.LSHR, Opcodes.IUSHR, Opcodes.LUSHR:
                type = (opcode - Opcodes.ISHL) % 2 == 0 ? TypeVariants.INT : TypeVariants.LONG;
                setInputsFromStack(stack, 2);
                setBasicOutputs(type);
                break;
            case Opcodes.IAND, Opcodes.LAND:
            case Opcodes.IOR, Opcodes.LOR:
            case Opcodes.IXOR, Opcodes.LXOR:
                type = (opcode - Opcodes.IAND) % 2 == 0 ? TypeVariants.INT : TypeVariants.LONG;
                setInputsFromStack(stack, 2);
                setBasicOutputs(type);
                break;
            case Opcodes.I2L:
                setInputsFromStack(stack, 1);
                setBasicOutputs(TypeVariants.LONG);
                break;
            case Opcodes.I2F:
                setInputsFromStack(stack, 1);
                setBasicOutputs(TypeVariants.FLOAT);
                break;
            case Opcodes.I2D:
                setInputsFromStack(stack, 1);
                setBasicOutputs(TypeVariants.DOUBLE);
                break;
            case Opcodes.L2I:
                setInputsFromStack(stack, 1);
                setBasicOutputs(TypeVariants.INT);
                break;
            case Opcodes.L2F:
                setInputsFromStack(stack, 1);
                setBasicOutputs(TypeVariants.FLOAT);
                break;
            case Opcodes.L2D:
                setInputsFromStack(stack, 1);
                setBasicOutputs(TypeVariants.DOUBLE);
                break;
            case Opcodes.F2I:
                setInputsFromStack(stack, 1);
                setBasicOutputs(TypeVariants.INT);
                break;
            case Opcodes.F2L:
                setInputsFromStack(stack, 1);
                setBasicOutputs(TypeVariants.LONG);
                break;
            case Opcodes.F2D:
                setInputsFromStack(stack, 1);
                setBasicOutputs(TypeVariants.DOUBLE);
                break;
            case Opcodes.D2I:
                setInputsFromStack(stack, 1);
                setBasicOutputs(TypeVariants.INT);
                break;
            case Opcodes.D2L:
                setInputsFromStack(stack, 1);
                setBasicOutputs(TypeVariants.LONG);
                break;
            case Opcodes.D2F:
                setInputsFromStack(stack, 1);
                setBasicOutputs(TypeVariants.FLOAT);
                break;
            case Opcodes.I2B:
                setInputsFromStack(stack, 1);
                setBasicOutputs(TypeVariants.BOOLEAN);
                break;
            case Opcodes.I2C:
                setInputsFromStack(stack, 1);
                setBasicOutputs(TypeVariants.CHAR);
                break;
            case Opcodes.I2S:
                setInputsFromStack(stack, 1);
                setBasicOutputs(TypeVariants.SHORT);
                break;
            case Opcodes.LCMP:
                setInputsFromStack(stack, 2);
                setBasicOutputs(TypeVariants.INT);
                break;
            case Opcodes.FCMPL, Opcodes.FCMPG:
                setInputsFromStack(stack, 2);
                setBasicOutputs(TypeVariants.INT);
                break;
            case Opcodes.DCMPL, Opcodes.DCMPG:
                setInputsFromStack(stack, 2);
                setBasicOutputs(TypeVariants.INT);
                break;
            case Opcodes.ARRAYLENGTH:
                setInputsFromStack(stack, 1);
                setBasicOutputs(TypeVariants.INT);
                break;
            case Opcodes.ATHROW:
                setInputsFromStack(stack, 1);
                break;
            case Opcodes.IRETURN, Opcodes.LRETURN, Opcodes.FRETURN, Opcodes.DRETURN, Opcodes.ARETURN:
                setInputsFromStack(stack, 1);
            break;
            case Opcodes.RETURN:
                setInputs();
                break;
            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT:
                setInputsFromStack(stack, 1);
                setBasicOutputs();
                break;
            default:
                throw new TranspilerException("Invalid opcode");
        }
    }

    @Override
    public boolean isRoutingInstruction() {
        return switch (opcode) {
            case Opcodes.DUP, Opcodes.DUP_X1, Opcodes.DUP_X2, Opcodes.DUP2, 
                 Opcodes.DUP2_X1, Opcodes.DUP2_X2, Opcodes.SWAP, Opcodes.POP, Opcodes.POP2 -> true;
            default -> false;
        };
    }
}
