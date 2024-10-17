package com.thelogicmaster.clearwing;

import com.thelogicmaster.clearwing.bytecode.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.JSRInlinerAdapter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;

public class Parser extends ClassVisitor {

    private final TranspilerConfig config;
    private BytecodeClass currentClass;
    private ArrayList<BytecodeClass> classes;
    private final HashMap<String, HashMap<String, Integer>> invokeDynamicCounts = new HashMap<>();

    public Parser(TranspilerConfig config) {
        super(Opcodes.ASM7);
        this.config = config;
    }

    public List<BytecodeClass> parse(List<Supplier<InputStream>> sources) throws IOException {
        classes = new ArrayList<>();

        for (Supplier<InputStream> input: sources)
            try (InputStream inputStream = input.get()) {
                ClassReader reader = new ClassReader(inputStream);
                if ("java/lang/Object".equals(reader.getClassName()))
                    continue;
                currentClass = new BytecodeClass(reader.getClassName(), reader.getSuperName(), reader.getInterfaces(), reader.getAccess());
                reader.accept(this, ClassReader.EXPAND_FRAMES);
                classes.add(currentClass);
            }

        return classes;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        currentClass.setSignature(signature);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (!visible)
            return null;
        BytecodeAnnotation annotation = new BytecodeAnnotation(Utils.parseClassDescription(desc), null);
        currentClass.addAnnotation(annotation);
        return new AnnotationParser(annotation, null);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        return null;
    }

    @Override
    public void visitAttribute(Attribute attr) {

    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if (currentClass.getOriginalName().equals(name)) {
            currentClass.setAnonymous(innerName == null);
            currentClass.markNested();
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        BytecodeField field = new BytecodeField(currentClass, name, access, desc, signature, value);
        currentClass.addField(field);
        return new FieldVisitor(Opcodes.ASM5) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
//                if (Utils.parseClassDescription(descriptor).equals("com/thelogicmaster/clearwing/Weak"))
//                    field.markWeak();
                if (!visible)
                    return null;
                BytecodeAnnotation annotation = new BytecodeAnnotation(Utils.parseClassDescription(descriptor));
                field.addAnnotation(annotation);
                return new AnnotationParser(annotation, null);
            }
        };
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        BytecodeMethod method = new BytecodeMethod(currentClass, name, access, desc, signature, exceptions);
        currentClass.addMethod(method);
        return new JSRInlinerAdapter(new MethodParser(method), access, name, desc, signature, exceptions);
    }

    @Override
    public void visitSource(String source, String debug) {
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
    }

    @Override
    public void visitEnd() {
    }

    private class MethodParser extends MethodVisitor {

        private final BytecodeMethod method;

        public MethodParser(BytecodeMethod method) {
            super(Opcodes.ASM6);
            this.method = method;
        }

        @Override
        public void visitParameter(String name, int access) {
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return new AnnotationParser(currentClass.getDefaultAnnotation(), method.getOriginalName());
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (!visible)
                return null;
            BytecodeAnnotation annotation = new BytecodeAnnotation(Utils.parseClassDescription(desc));
            method.addAnnotation(annotation);
            return new AnnotationParser(annotation, null);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            return null;
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
            return null;
        }

        @Override
        public void visitAttribute(Attribute attr) {

        }

        @Override
        public void visitCode() {
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        }

        @Override
        public void visitInsn(int opcode) {
            method.addInstruction(new ZeroOperandInstruction(method, opcode));
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            method.addInstruction(new IntegerInstruction(method, opcode, operand));
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            method.addInstruction(new VariableInstruction(method, opcode, var));
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            method.addInstruction(new TypeInstruction(method, opcode, type));
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            method.addInstruction(new FieldInstruction(method, opcode, owner, name, desc));
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            method.addInstruction(new MethodInstruction(method, opcode, owner, name, desc, itf));
        }

        @Override
        public MethodVisitor getDelegate() {
            return super.getDelegate();
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            switch (bsm.getOwner()) {
                case "java/lang/invoke/LambdaMetafactory":
                    visitInvokeLambda(name, desc, bsm, bsmArgs);
                    break;
                case "java/lang/invoke/StringConcatFactory":
                    visitInvokeStringConcat(name, desc, bsm, bsmArgs);
                    break;
                default:
                    throw new TranspilerException("Unsupported InvokeDynamic call: " + bsm.getOwner());
            }
        }
        
        private void visitInvokeLambda(String name, String desc, Handle bsm, Object... bsmArgs) {
            if (!"metafactory".equals(bsm.getName())) throw new TranspilerException("Unsupported LambdaMetafactory: " + bsm.getName());
            Handle handle = (Handle)bsmArgs[1];
            if (handle.getTag() < Opcodes.H_INVOKEVIRTUAL)
                throw new TranspilerException("Unsupported InvokeDynamic handle type: " + handle.getTag());
            Type proxyMethodType = (Type)bsmArgs[0];
            JavaType[] proxyFields = new MethodSignature("", desc, null).getParamTypes();
            String interfaceClass = Utils.sanitizeName(Type.getMethodType(desc).getReturnType().getClassName());

            String handleName = Utils.sanitizeName(handle.getName().replace("<init>", "init"));
            String handlePrefix = Utils.sanitizeName(handle.getOwner()) + "_invoke_" + handleName + "_";
            if (!invokeDynamicCounts.containsKey(currentClass.getName()))
                invokeDynamicCounts.put(currentClass.getName(), new HashMap<>());
            HashMap<String, Integer> counts = invokeDynamicCounts.get(currentClass.getName());
            if (!counts.containsKey(handlePrefix))
                counts.put(handlePrefix, 0);
            int count = counts.get(handlePrefix);
            counts.put(handlePrefix, count + 1);
            String className = handlePrefix + count;

            BytecodeClass proxyClass = new BytecodeClass(className, "java/lang/Object", new String[]{interfaceClass}, 0);
            InvokeDynamicInstruction invokeDynamic = new InvokeDynamicInstruction(method, handle, className, proxyFields, proxyMethodType.getDescriptor());
            BytecodeMethod delegate = new BytecodeMethod(proxyClass, name, 0, proxyMethodType.getDescriptor(), null, null);
            delegate.addInstruction(invokeDynamic.getProxy(delegate));
            delegate.markGenerated();
            proxyClass.addMethod(delegate);

            for (int i = 0; i < proxyFields.length; i++) {
                BytecodeField field = new BytecodeField(proxyClass, "field" + i, 0, proxyFields[i].getDesc(), null, null);
                proxyClass.addField(field);
            }

            method.addInstruction(invokeDynamic);
            classes.add(proxyClass);
        }

        private void visitInvokeStringConcat(String name, String desc, Handle bsm, Object... bsmArgs) {
            if (!"makeConcatWithConstants".equals(name)) throw new TranspilerException("Unsupported InvokeStringConcat: " + name);
            method.addInstruction(new InvokeStringConcatInstruction(method, desc, (String) bsmArgs[0], Arrays.stream(bsmArgs).skip(1).toArray()));
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            method.addInstruction(new JumpInstruction(method, opcode, label));
        }

        @Override
        public void visitLabel(Label label) {
            method.addInstruction(new LabelInstruction(method, label));
        }

        @Override
        public void visitLdcInsn(Object cst) {
            method.addInstruction(new LoadConstantInstruction(method, cst));
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            method.addInstruction(new IncrementInstruction(method, var, increment));
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label defaultLabel, Label... labels) {
            int[] keys = new int[labels.length];
            for (int i = 0; i < labels.length; i++)
                keys[i] = min + i;
            method.addInstruction(new SwitchInstruction(method, keys, labels, defaultLabel));
        }

        @Override
        public void visitLookupSwitchInsn(Label defaultLabel, int[] keys, Label[] labels) {
            method.addInstruction(new SwitchInstruction(method, keys, labels, defaultLabel));
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            method.addInstruction(new MultiArrayInstruction(method, desc, dims));
        }

        @Override
        public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            return null;
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            method.addTryCatch(new TryInstruction(method, start, end, handler, type));
        }

        @Override
        public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            return null;
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        }

        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
            return null;
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            method.addInstruction(new LineNumberInstruction(method, line, start));
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            method.setStackSize(maxStack, maxLocals);
        }

        @Override
        public void visitEnd() {
            // Todo: Move to BytecodeMethod

            insertTryCatchBlocks(); // Todo: Maybe insert exception push/pop instructions directly instead
            resolveInstructionIO();
            // Todo: Inline instructions from bottom to top
            trimLabels();
//            insertTryCatchBypasses();
//            optimizeInstructions();
//            discoverLocals();
//            groupInstructions();

//            trimStack();
        }

        /**
         * Determine the type for locals where possible
         */
        private void discoverLocals() {
            List<Instruction> instructions = method.getInstructions();
            Map<Integer, TypeVariants> knownLocals = method.getKnownLocals();

            for (Instruction instruction: instructions) {
                if (!(instruction instanceof LocalInstruction))
                    continue;
                LocalInstruction local = (LocalInstruction)instruction;
                int index = local.getLocal();
                TypeVariants type = local.getLocalType().getArithmeticVariant();
                if ((knownLocals.containsKey(index) && type != knownLocals.get(index)) || (type.isWide() && knownLocals.containsKey(index + 1))) {
                    if (index > 0 && knownLocals.get(index - 1) != null && knownLocals.get(index - 1).isWide())
                        knownLocals.put(index - 1, null);
                    knownLocals.put(index, null);
                    if (type.isWide())
                        knownLocals.put(index + 1, null);
                    continue;
                }
                knownLocals.put(index, type);
            }

            JavaType[] params = method.getSignature().getParamTypes();
            for (int i = 0; i < params.length; i++) {
                int index = i + (method.isStatic() ? 0 : 1);
                if (knownLocals.get(index) != null && knownLocals.get(index) != params[i].getBasicType()) {
                    if (index > 0 && knownLocals.get(index - 1) != null && knownLocals.get(index - 1).isWide())
                        knownLocals.put(index - 1, null);
                    knownLocals.put(index, null);
                    if (params[i].getBasicType().isWide())
                        knownLocals.put(index + 1, null);
                }
            }
        }

        public void resolveInstructionIO() {
            resolveInstructionIO(0, new ArrayList<>());
            for (int i = 0; i < method.getInstructions().size(); i++)
                if (method.getInstructions().get(i).getInputs() == null && !(method.getInstructions().get(i) instanceof LabelInstruction))
                    throw new TranspilerException("Failed to resolve instruction I/O: " + method.getInstructions().get(i));
//            for (Instruction instruction : method.getInstructions())
//                if (instruction.getInputs() == null && !(instruction instanceof LabelInstruction))
//                    throw new TranspilerException("Failed to resolve instruction I/O: " + instruction);
        }

        private void resolveInstructionIO(int offset, List<StackEntry> stack) {
            List<Instruction> instructions = method.getInstructions();

            // Todo: Ensure types are using arithmetic variants with pushed onto the stack, if it becomes an issue
            while (offset < instructions.size()) {
                Instruction instruction = instructions.get(offset);
                if (instruction.getInputs() != null)
                    return;
                instruction.resolveIO(stack);

                // Todo: Store instruction dependencies for use when determining inline-ability

                if (instruction.getInputs() != null && !stack.isEmpty())
                    stack.subList(Math.max(0, stack.size() - instruction.getInputs().size()), stack.size()).clear();

                if (instruction.getOutputs() != null)
                    for (JavaType type : instruction.getOutputs())
                        stack.add(new StackEntry(type, instruction));

                if (instruction instanceof TryInstruction)
                    resolveInstructionIO(method.findLabelInstruction(((TryInstruction) instruction).getEnd()),
                            new ArrayList<>(List.of(new StackEntry(new JavaType(TypeVariants.OBJECT), instruction))));

                if (instruction instanceof JumpingInstruction) {
                    for (int label : ((JumpingInstruction) instruction).getJumpLabels())
                        resolveInstructionIO(method.findLabelInstruction(label), new ArrayList<>(stack));
                    if (instruction.getOutputs() == null)
                        return;
                }

                offset++;
            }
        }

//        /**
//         * Calculate instruction I/O where known and perform optimizations
//         */
//        private void optimizeInstructions() {
//            List<Instruction> instructions = method.getInstructions();
//
//            // Calculate instruction I/O where types are known
//            ArrayList<StackEntry> stack = new ArrayList<>();
//            for (Instruction instruction : instructions) {
//                instruction.resolveIO(stack);
//                if (instruction.getInputs() != null && !stack.isEmpty())
//                    stack.subList(Math.max(0, stack.size() - instruction.getInputs().size()), stack.size()).clear();
//                if (instruction.getOutputs() == null)
//                    stack.clear();
//                else {
//                    for (int i = 0; i < stack.size(); i++)
//                        if (stack.get(i).getSource().isMarkedForRemoval())
//                            stack.remove(i--);
//                    for (TypeVariants type : instruction.getOutputs())
//                        stack.add(new StackEntry(type, instruction));
//                }
//            }
//
//            // Remove optimized out instructions and cast checks if configured
//            for (int i = 0; i < instructions.size(); i++)
//                if (instructions.get(i).isMarkedForRemoval() || (!config.hasValueChecks() && instructions.get(i).getOpcode() == Opcodes.CHECKCAST))
//                    instructions.remove(i--);
//        }

        /**
         * Group optimizable instructions into InstructionGroup objects to convert stack accesses into local variables
         */
        private void groupInstructions() {
            List<Instruction> instructions = method.getInstructions();

            // Extract instructions into groups
            for (int i = 0; i < instructions.size(); i++) {
                if (instructions.get(i).getInputs() == null || instructions.get(i).getOutputs() == null)
                    continue;

                int last = i;
                while (last + 1 < instructions.size() && instructions.get(last + 1).getInputs() != null) {
                    last++;
                    if (instructions.get(last).getOutputs() == null)
                        break;
                }
                List<Instruction> groupInstructions = instructions.subList(i, last + 1);
                InstructionGroup group = new InstructionGroup(method, new ArrayList<>(groupInstructions));
                groupInstructions.clear();
                groupInstructions.add(group);
            }
        }

        private int findStackMaxDepthJump(int label, List<StackEntry> stack, Set<Integer> branches, Map<Integer, Integer> labels) {
            int branch = labels.get(label);
            if (branches.contains(branch))
                return 0;
            branches.add(branch);
            int depth = findStackMaxDepth(branch, new ArrayList<>(stack), branches, labels);
            if (depth < 0)
                return -1;
            return depth;
        }

        private int findStackMaxDepth(int index, List<StackEntry> stack, Set<Integer> branches, Map<Integer, Integer> labels) {
            List<Instruction> instructions = method.getInstructions();
            int max = stack.size();
            for (int i = index; i < instructions.size(); i++) {
                Instruction instruction = instructions.get(i);
                if (instruction instanceof TryInstruction.CatchInstruction) {
                    ArrayList<StackEntry> newStack = new ArrayList<>();
                    newStack.add(new StackEntry(new JavaType(TypeVariants.OBJECT), null));
                    int branch = labels.get(((TryInstruction.CatchInstruction) instruction).getJumpLabels().get(0));
                    if (branches.contains(branch))
                        return max;
                    branches.add(branch);
                    int depth = findStackMaxDepth(branch, newStack, branches, labels);
                    max = Math.max(max, depth);
                    if (depth < 0)
                        return -1;
                } else if (instruction instanceof JumpingInstruction) {
                    for (int label : ((JumpingInstruction) instruction).getJumpLabels()) {
                        if (stack.size() < instruction.getInputs().size())
                            return -1;
                        if (instruction.getInputs().size() > 0)
                            stack.subList(stack.size() - instruction.getInputs().size(), stack.size()).clear();
                        int depth = findStackMaxDepthJump(label, stack, branches, labels);
                        if (depth < 0)
                            return -1;
                        max = Math.max(max, depth);
                    }
                    if (!(instruction instanceof JumpInstruction) || instruction.getOpcode() == Opcodes.GOTO)
                        return max;
                } else if (instruction instanceof SuperCallInstruction || instruction instanceof InvokeDynamicInstruction.Proxy)
                    return 0;
                else if (instruction instanceof LineNumberInstruction || instruction instanceof LabelInstruction || instruction instanceof TryInstruction)
                    continue;
                else if (instruction instanceof ZeroOperandInstruction) {
                    switch (instruction.getOpcode()) {
                        case Opcodes.DUP, Opcodes.DUP_X1, Opcodes.DUP_X2, Opcodes.DUP2, Opcodes.DUP2_X1, Opcodes.DUP2_X2, Opcodes.SWAP -> {
                            instruction.adjustStack(stack.subList(stack.size() - instruction.getInputs().size(), stack.size()), 0);
                            max = Math.max(max, stack.size());
                        }
                        default -> {
                            return -1;
                        }
                    }
                } else if (instruction.getInputs() != null && instruction.getOutputs() != null) {
                    if (stack.size() < instruction.getInputs().size())
                        return -1;
                    instruction.adjustStack(stack.subList(stack.size() - instruction.getInputs().size(), stack.size()), 0);
                    max = Math.max(max, stack.size());
                    if (instruction instanceof InstructionGroup) {
                        InstructionGroup group = (InstructionGroup) instruction;
                        Instruction lastInstruction = group.getLastInstruction();
                        switch (lastInstruction.getOpcode()) {
                            case Opcodes.IRETURN, Opcodes.LRETURN, Opcodes.FRETURN, Opcodes.DRETURN, Opcodes.ARETURN, Opcodes.RETURN, Opcodes.ATHROW -> {
                                return max;
                            }
                        }
                        if (!(lastInstruction instanceof JumpingInstruction))
                            continue;
                        for (int label: ((JumpingInstruction) lastInstruction).getJumpLabels()) {
                            int depth = findStackMaxDepthJump(label, stack, branches, labels);
                            if (depth < 0)
                                return -1;
                            max = Math.max(max, depth);
                        }
                        if (!(lastInstruction instanceof JumpInstruction) || lastInstruction.getOpcode() == Opcodes.GOTO)
                            return max;
                    }
                } else {
                    System.out.println("Warning: Failed to trim stack for instruction: " + instruction);
                    return -1;
                }
            }
            return max;
        }

        /**
         * Trim max stack size after optimizations
         */
        private void trimStack() {
            try {
                HashMap<Integer, Integer> labels = new HashMap<>();
                for (int i = 0; i < method.getInstructions().size(); i++)
                    if (method.getInstructions().get(i) instanceof LabelInstruction)
                        labels.put(((LabelInstruction) method.getInstructions().get(i)).getLabel(), i);
                int depth = findStackMaxDepth(0, new ArrayList<>(), new HashSet<>(), labels);
                if (depth < 0) {
                    System.out.println("Warning: Failed to trim stack for: " + method);
                    return;
                }
                if (depth > method.getStackSize())
                    System.out.println("Somehow increased stack size for: " + method);
                else
                    System.out.println("Trimmed " + (method.getStackSize() - depth) + " from " + method);
                method.setStackSize(depth, method.getLocalCount());
            } catch (Exception e) {
                System.err.println("Caught exception while trimming stack for " + method + ": " + e);
            }
        }

        /**
         * Remove unneeded labels to not block optimizations
         */
        private void trimLabels() {
            HashSet<Integer> used = new HashSet<>();
            List<Instruction> instructions = method.getInstructions();
            for (Instruction instruction: instructions)
                if (instruction instanceof JumpingInstruction)
                    used.addAll(((JumpingInstruction) instruction).getJumpLabels());
            for (int i = 0; i < instructions.size(); i++)
                if (instructions.get(i) instanceof LabelInstruction && !used.contains(((LabelInstruction) instructions.get(i)).getLabel()))
                    instructions.remove(i--);
        }

        /**
         * Insert helper instructions for try-catch blocks
         */
        private void insertTryCatchBlocks() {
            List<Instruction> instructions = method.getInstructions();
            List<TryInstruction> tryCatchBlocks = new ArrayList<>(method.getTryCatchBlocks());

            // Invert order for nested try-catch with same start label
            for (int i = 0; i < tryCatchBlocks.size(); i++) {
                int first = i;
                while (i + 1 < tryCatchBlocks.size() && tryCatchBlocks.get(first).getStart() == tryCatchBlocks.get(i + 1).getStart())
                    i++;
                int last = i;
                if (first == last)
                    continue;
                for (int j = 0; j < (last - first + 1) / 2; j++) {
                    TryInstruction temp = tryCatchBlocks.get(first + j);
                    tryCatchBlocks.set(first + j, tryCatchBlocks.get(last - j));
                    tryCatchBlocks.set(last - j, temp);
                }
            }

            // Insert "Catch" block instructions
            for (TryInstruction tryCatch : tryCatchBlocks) {
                int index = method.findLabelInstruction(tryCatch.getStart());
                instructions.add(index + 1, tryCatch);
                index = method.findInstruction(index, instructions.size(), true,
                        instr -> instr instanceof LabelInstruction && ((LabelInstruction) instr).getLabel() == tryCatch.getEnd());
                instructions.add(index + 1, tryCatch.getCatchInstruction());
            }
        }

        /**
         * Bypasses a try-catch jump if needed and returns the new jump label
         */
        private int bypassTryCatchJump(int jumpIndex, int jumpLabel) {
            List<Instruction> instructions = method.getInstructions();

            int jumpLabelIndex = method.findLabelInstruction(jumpLabel);

            for (int i = 0; i < instructions.size(); i++) {
                if (!(instructions.get(i) instanceof TryInstruction))
                    continue;
                TryInstruction tryInstruction = (TryInstruction) instructions.get(i);

                int scopeEnd = method.findInstruction(i, instructions.size(), true, instr -> tryInstruction.getCatchInstruction() == instr);

                if (jumpLabelIndex < i || jumpLabelIndex > scopeEnd || (jumpIndex >= i && jumpIndex < scopeEnd))
                    continue;

                int target = bypassTryCatchJump(i, jumpLabel);
                TryInstruction.Bypass bypass = tryInstruction.getBypass(target, jumpLabel);
                if (instructions.get(jumpIndex) instanceof JumpingInstruction)
                    ((JumpingInstruction) instructions.get(jumpIndex)).setJumpBypass(bypass.getIndex(), jumpLabel, tryInstruction.getLabel());

                return tryInstruction.getLabel();
            }

            return jumpLabel;
        }

        private void insertTryCatchBypasses() {
            List<Instruction> instructions = method.getInstructions();
            for (int i = 0; i < instructions.size(); i++) {
                if (!(instructions.get(i) instanceof JumpingInstruction))
                    continue;
                JumpingInstruction jump = (JumpingInstruction) instructions.get(i);
                for (int label: jump.getJumpLabels())
                    bypassTryCatchJump(i, label);
            }
        }
    }

    private static class AnnotationParser extends AnnotationVisitor {

        private final BytecodeAnnotation annotation;
        private final String valueName;

        public AnnotationParser (BytecodeAnnotation annotation, @Nullable String valueName) {
            super(Opcodes.ASM6);
            this.annotation = annotation;
            this.valueName = valueName;
        }

        @Override
        public void visit(String name, Object value) {
            annotation.addValue(new AnnotationObjectValue(annotation, valueName == null ? name : valueName, value));
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            annotation.addValue(new AnnotationEnumValue(valueName == null ? name : valueName, Utils.parseClassDescription(descriptor), value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            BytecodeAnnotation nested = new BytecodeAnnotation(Utils.parseClassDescription(descriptor), valueName == null ? name : valueName);
            annotation.addValue(nested);
            return new AnnotationParser(nested, null);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            return new AnnotationVisitor(Opcodes.ASM6) {
                private final ArrayList<Object> array = new ArrayList<>();

                @Override
                public void visit (String name, Object value) {
                    array.add(value);
                }

                @Override
                public void visitEnum (String name, String desc, String value) {
                    array.add(new AnnotationEnumValue(name, Utils.parseClassDescription(desc), value));
                }

                @Override
                public AnnotationVisitor visitAnnotation (String name, String desc) {
                    BytecodeAnnotation nested = new BytecodeAnnotation(Utils.parseClassDescription(desc), name);
                    array.add(nested);
                    return new AnnotationParser(nested, null);
                }

                @Override
                public void visitEnd () {
                    annotation.addValue(new AnnotationObjectValue(annotation, valueName == null ? name : valueName, array.toArray()));
                }
            };
        }
    }
}
