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

    private void appendThrowReturn(StringBuilder builder) {
        builder.append("return");
        if (method.getSignature().getReturnType().getBasicType() == TypeVariants.OBJECT)
            builder.append(" nullptr");
        else if (!method.getSignature().getReturnType().isVoid())
            builder.append(" 0");
        builder.append(";\n");
    }

    @Override
    public void appendUnoptimized(StringBuilder builder) {
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
            case Opcodes.POP, Opcodes.POP2, Opcodes.DUP, Opcodes.DUP_X1, Opcodes.DUP_X2, Opcodes.DUP2, Opcodes.DUP2_X1, Opcodes.DUP2_X2, Opcodes.SWAP:
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
            case Opcodes.ATHROW:
                appendStandardInstruction(builder, getOpcodeName());
                builder.append("\t");
                appendThrowReturn(builder);
                break;
            case Opcodes.IRETURN, Opcodes.LRETURN, Opcodes.FRETURN, Opcodes.DRETURN: {
                TypeVariants type = TypeVariants.values()[opcode - Opcodes.IRETURN + TypeVariants.INT.ordinal()];
                builder.append("\treturn ").append(type.getCppType()).append("(vm::pop<").append(type.getArithmeticType()).append(">(sp));\n");
            }
            break;
            case Opcodes.ARETURN: {
                JavaType returnType = getMethod().getSignature().getReturnType();
                builder.append("\t\treturn object_cast<").append(returnType.getArrayDimensions() > 0 ? "vm::Array" : Utils.getQualifiedClassName(returnType.getReferenceType()));
                builder.append(">(vm::pop<jobject>(sp));\n");
            }
            break;
            case Opcodes.RETURN:
                builder.append("\treturn;\n");
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
    public void appendOptimized(StringBuilder builder, List<StackEntry> operands, int temporaries) {
        TypeVariants type;
        switch (opcode) {
            case Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5 ->
                builder.append("\t\tauto temp").append(temporaries).append(" = jint(").append(getOpcodeConst(Opcodes.ICONST_0)).append(");\n");
            case Opcodes.LCONST_0, Opcodes.LCONST_1 ->
                    builder.append("\t\tauto temp").append(temporaries).append(" = jlong(").append(getOpcodeConst(Opcodes.LCONST_0)).append(");\n");
            case Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2 ->
                    builder.append("\t\tauto temp").append(temporaries).append(" = jfloat(").append(getOpcodeConst(Opcodes.FCONST_0)).append(");\n");
            case Opcodes.DCONST_0, Opcodes.DCONST_1 ->
                    builder.append("\t\tauto temp").append(temporaries).append(" = jdouble(").append(getOpcodeConst(Opcodes.DCONST_0)).append(");\n");
            case Opcodes.NOP -> {}
            case Opcodes.ACONST_NULL -> builder.append("\t\tauto temp").append(temporaries).append(" = jobject(nullptr);\n");
            case Opcodes.IALOAD, Opcodes.LALOAD, Opcodes.FALOAD, Opcodes.DALOAD, Opcodes.AALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD -> {
                type = opcodeType(Opcodes.IALOAD);
                builder.append("\t\tauto temp").append(temporaries).append(" = ").append(type.getArithmeticType()).append("(vm::checkedCast<vm::Array>(")
                        .append(operands.get(0)).append(")->get<").append(type.getCppType()).append(">(").append(operands.get(1)).append("));\n");
            }
            case Opcodes.IASTORE, Opcodes.LASTORE, Opcodes.FASTORE, Opcodes.DASTORE, Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE -> {
                type = opcodeType(Opcodes.IASTORE);
                builder.append("\t\tvm::checkedCast<vm::Array>(").append(operands.get(0)).append(")->get<").append(type.getCppType())
                        .append(">(").append(operands.get(1)).append(") = ").append(operands.get(2)).append(";\n");
            }
            case Opcodes.POP, Opcodes.POP2, Opcodes.DUP, Opcodes.DUP_X1, Opcodes.DUP_X2, Opcodes.DUP2, Opcodes.DUP2_X1, Opcodes.DUP2_X2, Opcodes.SWAP ->
                builder.append("\t\t// ").append(Objects.requireNonNull(getOpcodeName()).toUpperCase()).append("\n");
            case Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD ->
                builder.append("\t\tauto temp").append(temporaries).append(" = ").append(operands.get(0)).append(" + ").append(operands.get(1)).append(";\n");
            case Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB ->
                builder.append("\t\tauto temp").append(temporaries).append(" = ").append(operands.get(0)).append(" - ").append(operands.get(1)).append(";\n");
            case Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL ->
                builder.append("\t\tauto temp").append(temporaries).append(" = ").append(operands.get(0)).append(" * ").append(operands.get(1)).append(";\n");
            case Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV ->
                builder.append("\t\tauto temp").append(temporaries).append(" = ").append(operands.get(0)).append(" / ").append(operands.get(1)).append(";\n");
            case Opcodes.IREM, Opcodes.LREM ->
                builder.append("\t\tauto temp").append(temporaries).append(" = ").append(operands.get(0)).append(" % ").append(operands.get(1)).append(";\n");
            case Opcodes.FREM ->
                builder.append("\t\tauto temp").append(temporaries).append(" = fmodf(").append(operands.get(0)).append(", ").append(operands.get(1)).append(");\n");
            case Opcodes.DREM ->
                builder.append("\t\tauto temp").append(temporaries).append(" = fmod(").append(operands.get(0)).append(", ").append(operands.get(1)).append(");\n");
            case Opcodes.INEG, Opcodes.LNEG, Opcodes.FNEG, Opcodes.DNEG ->
                builder.append("\t\tauto temp").append(temporaries).append(" = -").append(operands.get(0)).append(";\n");
            case Opcodes.ISHL, Opcodes.LSHL ->
                builder.append("\t\tauto temp").append(temporaries).append(" = ").append(operands.get(0)).append(" << ").append(operands.get(1)).append(";\n");
            case Opcodes.ISHR, Opcodes.LSHR ->
                builder.append("\t\tauto temp").append(temporaries).append(" = ").append(operands.get(0)).append(" >> ").append(operands.get(1)).append(";\n");
            case Opcodes.IUSHR ->
                builder.append("\t\tauto temp").append(temporaries).append(" = bit_cast<jint>(bit_cast<uint32_t>(").append(operands.get(0))
                        .append(") >> ").append(operands.get(1)).append(");\n");
            case Opcodes.LUSHR ->
                    builder.append("\t\tauto temp").append(temporaries).append(" = bit_cast<jlong>(bit_cast<uint64_t>(").append(operands.get(0))
                            .append(") >> ").append(operands.get(1)).append(");\n");
            case Opcodes.IAND, Opcodes.LAND ->
                    builder.append("\t\tauto temp").append(temporaries).append(" = ").append(operands.get(0)).append(" & ").append(operands.get(1)).append(";\n");
            case Opcodes.IOR, Opcodes.LOR ->
                    builder.append("\t\tauto temp").append(temporaries).append(" = ").append(operands.get(0)).append(" | ").append(operands.get(1)).append(";\n");
            case Opcodes.IXOR, Opcodes.LXOR ->
                    builder.append("\t\tauto temp").append(temporaries).append(" = ").append(operands.get(0)).append(" ^ ").append(operands.get(1)).append(";\n");
            case Opcodes.I2L, Opcodes.F2L, Opcodes.D2L -> builder.append("\t\tauto temp").append(temporaries).append(" = jlong(").append(operands.get(0)).append(");\n");
            case Opcodes.I2F, Opcodes.L2F, Opcodes.D2F -> builder.append("\t\tauto temp").append(temporaries).append(" = jfloat(").append(operands.get(0)).append(");\n");
            case Opcodes.I2D, Opcodes.L2D, Opcodes.F2D -> builder.append("\t\tauto temp").append(temporaries).append(" = jdouble(").append(operands.get(0)).append(");\n");
            case Opcodes.L2I, Opcodes.F2I, Opcodes.D2I -> builder.append("\t\tauto temp").append(temporaries).append(" = jint(").append(operands.get(0)).append(");\n");
            case Opcodes.I2B -> builder.append("\t\tauto temp").append(temporaries).append(" = jbyte(").append(operands.get(0)).append(");\n");
            case Opcodes.I2C -> builder.append("\t\tauto temp").append(temporaries).append(" = jchar(").append(operands.get(0)).append(");\n");
            case Opcodes.I2S -> builder.append("\t\tauto temp").append(temporaries).append(" = jshort(").append(operands.get(0)).append(");\n");
            case Opcodes.LCMP ->
                builder.append("\t\tauto temp").append(temporaries).append(" = vm::longCompare(").append(operands.get(0)).append(", ").append(operands.get(1)).append(");\n");
            case Opcodes.FCMPL, Opcodes.DCMPL ->
                builder.append("\t\tauto temp").append(temporaries).append(" = vm::floatCompare(").append(operands.get(0)).append(", ").append(operands.get(1)).append(", -1);\n");
            case Opcodes.FCMPG, Opcodes.DCMPG ->
                builder.append("\t\tauto temp").append(temporaries).append(" = vm::floatCompare(").append(operands.get(0)).append(", ").append(operands.get(1)).append(", 1);\n");
            case Opcodes.ARRAYLENGTH ->
                builder.append("\t\tauto temp").append(temporaries).append(" = vm::checkedCast<vm::Array>(").append(operands.get(0)).append(")->length;\n");
            case Opcodes.ATHROW -> {
                builder.append("\t\tvm::throwEx(").append(operands.get(0)).append("); ");
                appendThrowReturn(builder);
            }
            case Opcodes.IRETURN, Opcodes.LRETURN, Opcodes.FRETURN, Opcodes.DRETURN -> builder.append("\t\treturn ").append(operands.get(0)).append(";\n");
            case Opcodes.ARETURN -> {
                JavaType returnType = getMethod().getSignature().getReturnType();
                builder.append("\t\treturn object_cast<").append(returnType.getArrayDimensions() > 0 ? "vm::Array" : Utils.getQualifiedClassName(returnType.getReferenceType()));
                builder.append(">(").append(operands.get(0)).append(");\n");
            }
            case Opcodes.RETURN -> builder.append("\t\treturn;\n");
            case Opcodes.MONITORENTER -> builder.append("\t\t").append(operands.get(0)).append("->acquireMonitor();\n");
            case Opcodes.MONITOREXIT -> builder.append("\t\t").append(operands.get(0)).append("->releaseMonitor();\n");
            default -> throw new TranspilerException("Invalid opcode");
        }
    }

    @Override
    public void populateIO(List<StackEntry> stack) {
        TypeVariants type, type2, type3, type4;
        switch (opcode) {
            case Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5:
                inputs = Collections.emptyList();
                outputs = Collections.singletonList(TypeVariants.INT);
                break;
            case Opcodes.LCONST_0, Opcodes.LCONST_1:
                inputs = Collections.emptyList();
                outputs = Collections.singletonList(TypeVariants.LONG);
                break;
            case Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2:
                inputs = Collections.emptyList();
                outputs = Collections.singletonList(TypeVariants.FLOAT);
                break;
            case Opcodes.DCONST_0, Opcodes.DCONST_1:
                inputs = Collections.emptyList();
                outputs = Collections.singletonList(TypeVariants.DOUBLE);
                break;
            case Opcodes.NOP:
                inputs = Collections.emptyList();
                outputs = Collections.emptyList();
                break;
            case Opcodes.ACONST_NULL:
                inputs = Collections.emptyList();
                outputs = Collections.singletonList(TypeVariants.OBJECT);
                break;
            case Opcodes.IALOAD, Opcodes.LALOAD, Opcodes.FALOAD, Opcodes.DALOAD, Opcodes.AALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD:
                inputs = Arrays.asList(TypeVariants.OBJECT, TypeVariants.INT);
                outputs = Collections.singletonList(opcodeType(Opcode.IALOAD));
                break;
            case Opcodes.IASTORE, Opcodes.LASTORE, Opcodes.FASTORE, Opcodes.DASTORE, Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE:
                inputs = Arrays.asList(TypeVariants.OBJECT, TypeVariants.INT, opcodeType(Opcode.IASTORE));
                outputs = Collections.emptyList();
                break;
            case Opcodes.POP:
                if (stack.isEmpty())
                    break;
                inputs = Collections.singletonList(stack.get(stack.size() - 1).getType());
                outputs = Collections.emptyList();
                break;
            case Opcodes.POP2:
                if (stack.isEmpty())
                    break;
                type = stack.get(stack.size() - 1).getType();
                if (!type.isWide() && stack.size() == 1)
                    break;
                if (type.isWide())
                    inputs = Collections.singletonList(type);
                else
                    inputs = Arrays.asList(stack.get(stack.size() - 2).getType(), type);
                outputs = Collections.emptyList();
                break;
            case Opcodes.DUP:
                if (stack.isEmpty())
                    break;
                type = stack.get(stack.size() - 1).getType();
                inputs = Collections.singletonList(type);
                outputs = Arrays.asList(type, type);
                break;
            case Opcodes.DUP_X1:
                if (stack.size() < 2)
                    break;
                type = stack.get(stack.size() - 1).getType();
                type2 = stack.get(stack.size() - 2).getType();
                inputs = Arrays.asList(type2, type);
                outputs = Arrays.asList(type, type2, type);
                break;
            case Opcodes.DUP_X2:
                if (stack.size() < 2)
                    break;
                type = stack.get(stack.size() - 1).getType();
                type2 = stack.get(stack.size() - 2).getType();
                if (type2.isWide()) { // Form 2
                    inputs = Arrays.asList(type2, type);
                    outputs = Arrays.asList(type, type2, type);
                } else { // Form 1
                    if (stack.size() < 3)
                        break;
                    type3 = stack.get(stack.size() - 3).getType();
                    inputs = Arrays.asList(type3, type2, type);
                    outputs = Arrays.asList(type, type3, type2, type);
                }
                break;
            case Opcodes.DUP2:
                if (stack.isEmpty())
                    break;
                type = stack.get(stack.size() - 1).getType();
                if (type.isWide()) { // Form 2
                    inputs = Collections.singletonList(type);
                    outputs = Arrays.asList(type, type);
                } else { // Form 1
                    if (stack.size() < 2)
                        break;
                    type2 = stack.get(stack.size() - 2).getType();
                    inputs = Arrays.asList(type2, type);
                    outputs = Arrays.asList(type2, type, type2, type);
                }
                break;
            case Opcodes.DUP2_X1:
                if (stack.size() < 2)
                    break;
                type = stack.get(stack.size() - 1).getType();
                type2 = stack.get(stack.size() - 2).getType();
                if (type.isWide()) { // Form 2
                    inputs = Arrays.asList(type2, type);
                    outputs = Arrays.asList(type, type2, type);
                } else { // Form 1
                    if (stack.size() < 3)
                        break;
                    type3 = stack.get(stack.size() - 3).getType();
                    inputs = Arrays.asList(type3, type2, type);
                    outputs = Arrays.asList(type2, type, type3, type2, type);
                }
                break;
            case Opcodes.DUP2_X2:
                if (stack.size() < 2)
                    break;
                type = stack.get(stack.size() - 1).getType();
                type2 = stack.get(stack.size() - 2).getType();
                if (type.isWide() && type2.isWide()) { // Form 4
                    inputs = Arrays.asList(type2, type);
                    outputs = Arrays.asList(type, type2, type);
                } else {
                    if (stack.size() < 3)
                        break;
                    type3 = stack.get(stack.size() - 3).getType();
                    if (type.isWide()) { // Form 2
                        inputs = Arrays.asList(type3, type2, type);
                        outputs = Arrays.asList(type, type3, type2, type);
                    } else if (type3.isWide()) { // Form 3
                        inputs = Arrays.asList(type3, type2, type);
                        outputs = Arrays.asList(type2, type, type3, type2, type);
                    } else { // Form 1
                        if (stack.size() < 4)
                            break;
                        type4 = stack.get(stack.size() - 4).getType();
                        inputs = Arrays.asList(type4, type3, type2, type);
                        outputs = Arrays.asList(type2, type, type4, type3, type2, type);
                    }
                }
                break;
            case Opcodes.SWAP:
                if (stack.size() < 2)
                    break;
                type = stack.get(stack.size() - 1).getType();
                type2 = stack.get(stack.size() - 2).getType();
                inputs = Arrays.asList(type2, type);
                outputs = Arrays.asList(type, type2);
                break;
            case Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD:
                type = opcodeType(Opcodes.IADD);
                inputs = Arrays.asList(type, type);
                outputs = Collections.singletonList(type);
                break;
            case Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB:
                type = opcodeType(Opcodes.ISUB);
                inputs = Arrays.asList(type, type);
                outputs = Collections.singletonList(type);
                break;
            case Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL:
                type = opcodeType(Opcodes.IMUL);
                inputs = Arrays.asList(type, type);
                outputs = Collections.singletonList(type);
                break;
            case Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV:
                type = opcodeType(Opcodes.IDIV);
                inputs = Arrays.asList(type, type);
                outputs = Collections.singletonList(type);
                break;
            case Opcodes.IREM, Opcodes.LREM, Opcodes.FREM, Opcodes.DREM:
                type = opcodeType(Opcodes.IREM);
                inputs = Arrays.asList(type, type);
                outputs = Collections.singletonList(type);
                break;
            case Opcodes.INEG, Opcodes.LNEG, Opcodes.FNEG, Opcodes.DNEG:
                type = opcodeType(Opcodes.INEG);
                inputs = Collections.singletonList(type);
                outputs = Collections.singletonList(type);
                break;
            case Opcodes.ISHL, Opcodes.LSHL, Opcodes.ISHR, Opcodes.LSHR, Opcodes.IUSHR, Opcodes.LUSHR:
                type = (opcode - Opcodes.ISHL) % 2 == 0 ? TypeVariants.INT : TypeVariants.LONG;
                inputs = Arrays.asList(type, TypeVariants.INT);
                outputs = Collections.singletonList(type);
                break;
            case Opcodes.IAND, Opcodes.LAND:
            case Opcodes.IOR, Opcodes.LOR:
            case Opcodes.IXOR, Opcodes.LXOR:
                type = (opcode - Opcodes.ISHL) % 2 == 0 ? TypeVariants.INT : TypeVariants.LONG;
                inputs = Arrays.asList(type, type);
                outputs = Collections.singletonList(type);
                break;
            case Opcodes.I2L:
                inputs = Collections.singletonList(TypeVariants.INT);
                outputs = Collections.singletonList(TypeVariants.LONG);
                break;
            case Opcodes.I2F:
                inputs = Collections.singletonList(TypeVariants.INT);
                outputs = Collections.singletonList(TypeVariants.FLOAT);
                break;
            case Opcodes.I2D:
                inputs = Collections.singletonList(TypeVariants.INT);
                outputs = Collections.singletonList(TypeVariants.DOUBLE);
                break;
            case Opcodes.L2I:
                inputs = Collections.singletonList(TypeVariants.LONG);
                outputs = Collections.singletonList(TypeVariants.INT);
                break;
            case Opcodes.L2F:
                inputs = Collections.singletonList(TypeVariants.LONG);
                outputs = Collections.singletonList(TypeVariants.FLOAT);
                break;
            case Opcodes.L2D:
                inputs = Collections.singletonList(TypeVariants.LONG);
                outputs = Collections.singletonList(TypeVariants.DOUBLE);
                break;
            case Opcodes.F2I:
                inputs = Collections.singletonList(TypeVariants.FLOAT);
                outputs = Collections.singletonList(TypeVariants.INT);
                break;
            case Opcodes.F2L:
                inputs = Collections.singletonList(TypeVariants.FLOAT);
                outputs = Collections.singletonList(TypeVariants.LONG);
                break;
            case Opcodes.F2D:
                inputs = Collections.singletonList(TypeVariants.FLOAT);
                outputs = Collections.singletonList(TypeVariants.DOUBLE);
                break;
            case Opcodes.D2I:
                inputs = Collections.singletonList(TypeVariants.DOUBLE);
                outputs = Collections.singletonList(TypeVariants.INT);
                break;
            case Opcodes.D2L:
                inputs = Collections.singletonList(TypeVariants.DOUBLE);
                outputs = Collections.singletonList(TypeVariants.LONG);
                break;
            case Opcodes.D2F:
                inputs = Collections.singletonList(TypeVariants.DOUBLE);
                outputs = Collections.singletonList(TypeVariants.FLOAT);
                break;
            case Opcodes.I2B:
                inputs = Collections.singletonList(TypeVariants.INT);
                outputs = Collections.singletonList(TypeVariants.BOOLEAN);
                break;
            case Opcodes.I2C:
                inputs = Collections.singletonList(TypeVariants.INT);
                outputs = Collections.singletonList(TypeVariants.CHAR);
                break;
            case Opcodes.I2S:
                inputs = Collections.singletonList(TypeVariants.INT);
                outputs = Collections.singletonList(TypeVariants.SHORT);
                break;
            case Opcodes.LCMP:
                inputs = Arrays.asList(TypeVariants.LONG, TypeVariants.LONG);
                outputs = Collections.singletonList(TypeVariants.INT);
                break;
            case Opcodes.FCMPL, Opcodes.FCMPG:
                inputs = Arrays.asList(TypeVariants.FLOAT, TypeVariants.FLOAT);
                outputs = Collections.singletonList(TypeVariants.INT);
                break;
            case Opcodes.DCMPL, Opcodes.DCMPG:
                inputs = Arrays.asList(TypeVariants.DOUBLE, TypeVariants.DOUBLE);
                outputs = Collections.singletonList(TypeVariants.INT);
                break;
            case Opcodes.ARRAYLENGTH:
                inputs = Collections.singletonList(TypeVariants.OBJECT);
                outputs = Collections.singletonList(TypeVariants.INT);
                break;
            case Opcodes.ATHROW:
                inputs = Collections.singletonList(TypeVariants.OBJECT);
                break;
            case Opcodes.IRETURN, Opcodes.LRETURN, Opcodes.FRETURN, Opcodes.DRETURN, Opcodes.ARETURN:
                inputs = Collections.singletonList(opcodeType(Opcodes.IRETURN));
            break;
            case Opcodes.RETURN:
                break;
            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT:
                inputs = Collections.singletonList(TypeVariants.OBJECT);
                outputs = Collections.emptyList();
                break;
            default:
                throw new TranspilerException("Invalid opcode");
        }
    }

    @Override
    public int adjustStack(List<StackEntry> operands, int temporaries) {
        return switch (opcode) {
            case Opcodes.DUP -> {
                operands.add(operands.get(0));
                yield 0;
            }
            case Opcodes.DUP_X1 -> {
                operands.add(0, operands.get(1));
                yield 0;
            }
            case Opcodes.DUP_X2 -> {
                if (inputs.size() == 2) // Form 2
                    operands.add(0, operands.get(1));
                else // Form 1
                    operands.add(0, operands.get(2));
                yield 0;
            }
            case Opcodes.DUP2 -> {
                if (inputs.size() == 1) // Form 2
                    operands.add(0, operands.get(0));
                else { // Form 1
                    operands.add(0, operands.get(1));
                    operands.add(0, operands.get(1));
                }
                yield 0;
            }
            case Opcodes.DUP2_X1 -> {
                if (inputs.size() == 2) // Form 2
                    operands.add(0, operands.get(1));
                else { // Form 1
                    operands.add(0, operands.get(2));
                    operands.add(0, operands.get(2));
                }
                yield 0;
            }
            case Opcodes.DUP2_X2 -> {
                if (inputs.size() == 2) // Form 4
                    operands.add(0, operands.get(1));
                else if (inputs.size() == 4) { // Form 1
                    operands.add(0, operands.get(3));
                    operands.add(0, operands.get(3));
                } else if (outputs.size() == 4) // Form 2
                    operands.add(0, operands.get(2));
                else { // Form 3
                    operands.add(0, operands.get(2));
                    operands.add(0, operands.get(2));
                }
                yield 0;
            }
            case Opcodes.SWAP -> {
                operands.add(0, operands.remove(1));
                yield 0;
            }
            default -> super.adjustStack(operands, temporaries);
        };
    }
}
