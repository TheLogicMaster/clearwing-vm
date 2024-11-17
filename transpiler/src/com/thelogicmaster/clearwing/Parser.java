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
            currentClass.markInner(outerName);
        }
        if (currentClass.getOriginalName().equals(outerName))
            currentClass.addInnerClass(name);
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
            final int FLAG_SERIALIZABLE = 0x1;
            final int FLAG_MARKERS = 0x2;
            final int FLAG_BRIDGES = 0x4;
            boolean isAlt = "altMetafactory".equals(bsm.getName());
            if (!isAlt && !"metafactory".equals(bsm.getName())) 
                throw new TranspilerException("Unsupported LambdaMetafactory: " + bsm.getName());
            int flags = isAlt ? (int)bsmArgs[3] : 0;
            Handle handle = (Handle)bsmArgs[1];
            if (handle.getTag() < Opcodes.H_INVOKEVIRTUAL)
                throw new TranspilerException("Unsupported InvokeDynamic handle type: " + handle.getTag());
            Type proxyMethodType = (Type)bsmArgs[0];
            JavaType[] proxyFields = new MethodSignature("", desc, null).getParamTypes();
            String interfaceClass = Utils.sanitizeName(Type.getMethodType(desc).getReturnType().getClassName());

            String proxyPrefix = currentClass.getName() + "_invoke_" + Utils.getQualifiedClassName(interfaceClass) + "_";
            if (!invokeDynamicCounts.containsKey(currentClass.getName()))
                invokeDynamicCounts.put(currentClass.getName(), new HashMap<>());
            HashMap<String, Integer> counts = invokeDynamicCounts.get(currentClass.getName());
            if (!counts.containsKey(proxyPrefix))
                counts.put(proxyPrefix, 0);
            int count = counts.get(proxyPrefix);
            counts.put(proxyPrefix, count + 1);
            String className = proxyPrefix + count;

            ArrayList<String> interfaces = new ArrayList<>();
            interfaces.add(interfaceClass);
            if ((flags & FLAG_SERIALIZABLE) != 0) {
                interfaces.add("java/io/Serializable");
                throw new TranspilerException("InvokeDynamic altMetafactory Serializable lambda not supported");
            }
            int index = 4;
            if ((flags & FLAG_MARKERS) != 0) {
                int markerCount = (int)bsmArgs[index++];
                for (int i = 0; i < markerCount; i++)
                    interfaces.add(((Type)bsmArgs[index++]).getDescriptor());
            }
            if ((flags & FLAG_BRIDGES) != 0) {
                int bridgeCount = (int)bsmArgs[index];
                if (bridgeCount > 0)
                    throw new TranspilerException("InvokeDynamic altMetafactory bridges not supported");
            }

            BytecodeClass proxyClass = new BytecodeClass(className, "java/lang/Object", interfaces.toArray(new String[0]), 0);
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

            insertTryCatchBlocks();
            resolveInstructionIO();
            trimLabels();
            handleTryCatchOuterJumps();
            insertTryCatchBypasses();
            
            if (config.useOptimizations())
                groupInstructions();
        }

        public void resolveInstructionIO() {
            resolveInstructionIO(0, new ArrayList<>());
//            for (Instruction instruction : method.getInstructions())
//                if (instruction.getInputs() == null && !(instruction instanceof LabelInstruction))
//                    throw new TranspilerException("Failed to resolve instruction I/O: " + instruction);
            for (int i = 0; i < method.getInstructions().size(); i++)
                if (method.getInstructions().get(i).getInputs() == null && !(method.getInstructions().get(i) instanceof LabelInstruction))
                    throw new TranspilerException("Failed to resolve instruction I/O: " + method.getInstructions().get(i));
        }

        private void resolveInstructionIO(int offset, List<StackEntry> stack) {
            List<Instruction> instructions = method.getInstructions();

            while (offset < instructions.size()) {
                Instruction instruction = instructions.get(offset);
                if (instruction.getInputs() != null)
                    return;
                instruction.setStackDepth(stack.size());
                instruction.setInstructionIndex(offset);
                instruction.resolveIO(stack);
                for (StackEntry input : instruction.getInputs())
                    input.addConsumer(instruction);
                
                if (instruction.getInputs() != null && !stack.isEmpty())
                    stack.subList(Math.max(0, stack.size() - instruction.getInputs().size()), stack.size()).clear();

                if (instruction.getOutputs() != null)
                    stack.addAll(instruction.getOutputs());

                if (instruction instanceof TryInstruction) {
                    resolveInstructionIO(method.findLabelInstruction(((TryInstruction) instruction).getHandler()),
                            new ArrayList<>(List.of(new StackEntry(new JavaType(TypeVariants.OBJECT), instruction))));
                } else if (instruction instanceof JumpingInstruction) {
                    for (int label : ((JumpingInstruction) instruction).getJumpLabels())
                        resolveInstructionIO(method.findLabelInstruction(label), new ArrayList<>(stack));
                    if (instruction.getOutputs() == null)
                        return;
                }
                
                if (instruction.getOutputs() == null) 
                    return;

                offset++;
            }
        }

        /**
         * Group optimizable instructions into InstructionGroup objects to convert stack accesses into local variables
         */
        private void groupInstructions() {
            List<Instruction> instructions = method.getInstructions();

            for (int i = 0; i < instructions.size(); i++) {
                Instruction first = instructions.get(i);
                if (first.getOutputs() == null || first instanceof LabelInstruction || first instanceof JumpingInstruction 
                        || first instanceof TryInstruction.CatchInstruction)
                    continue;

                int last = i;
                while (last + 1 < instructions.size()) {
                    Instruction instruction = instructions.get(last + 1);
                    if (instruction instanceof LabelInstruction || instruction instanceof TryInstruction || instruction instanceof TryInstruction.CatchInstruction) break;
                    last++;
                    if (instruction.getOutputs() == null || instruction instanceof JumpingInstruction)
                        break;
                }
                if (last < i + 2) // Todo: Minimum number in group? Ignore labels...
                    continue;
                
                List<Instruction> groupInstructions = instructions.subList(i, last + 1);
                InstructionGroup group = new InstructionGroup(method, new ArrayList<>(groupInstructions));
                groupInstructions.clear();
                groupInstructions.add(group);
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
        
        private void insertTryCatchBypasses() {
            List<Instruction> instructions = method.getInstructions();
            for (int i = 0; i < instructions.size(); i++) {
                if (!(instructions.get(i) instanceof JumpingInstruction jump))
                    continue;
                ArrayList<Integer> jumpStack = getTryStack(i);
                for (int label : jump.getJumpLabels()) {
                    int target = method.findLabelInstruction(label);
                    ArrayList<Integer> targetStack = getTryStack(target);
                    for (int j = 0; j < targetStack.size(); j++) {
                        if (j < jumpStack.size() && jumpStack.get(j).equals(targetStack.get(j)))
                            continue;
                        int currentLabel = label;
                        int bypassIndex = method.allocateTryCatchBypass();
                        for (int k = targetStack.size() - 1; k >= j; k--) {
                            int tryLabel = targetStack.get(k);
                            TryInstruction tryCatch = (TryInstruction) instructions.get(method.findInstruction(0, instructions.size(), true, inst -> inst instanceof TryInstruction t && t.getLabel() == tryLabel));
                            tryCatch.createBypass(currentLabel, label, bypassIndex);
                            currentLabel = tryCatch.getLabel();
                        }
                        jump.setJumpBypass(bypassIndex, label, currentLabel);
                        break;
                    }
                }
            }
        }
        
        private ArrayList<Integer> getTryStack(int index) {
            ArrayList<Integer> stack = new ArrayList<>();
            List<Instruction> instructions = method.getInstructions();
            for (int i = 0; i <= index; i++) {
                Instruction instruction = instructions.get(i);
                if (instruction instanceof TryInstruction tryInstruction)
                    stack.add(tryInstruction.getLabel());
                if (index == i)
                    break;
                if (instruction instanceof TryInstruction.CatchInstruction)
                    stack.remove(stack.size() - 1);
            }
            return stack;
        }
        
        private void handleTryCatchOuterJumps() {
            List<Instruction> instructions = method.getInstructions();
            for (int i = 0; i < instructions.size(); i++) {
                if (!(instructions.get(i) instanceof JumpingInstruction jump))
                    continue;
                ArrayList<Integer> jumpStack = getTryStack(i);
                for (int label : jump.getJumpLabels()) {
                    int target = method.findLabelInstruction(label);
                    ArrayList<Integer> targetStack = getTryStack(target);
                    for (int j = 0; j < jumpStack.size(); j++) {
                        if (j < targetStack.size() && targetStack.get(j).equals(jumpStack.get(j)))
                            continue;
                        jump.setJumpExceptionPops(label, jumpStack.size() - j);
                        break;
                    }
                }
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
