package com.thelogicmaster.clearwing;

import com.thelogicmaster.clearwing.bytecode.Instruction;

import java.util.*;

public class InstructionGroup extends Instruction {

    private final List<Instruction> instructions;
    private final Map<Integer, Set<JavaType>> temporaryTypes = new HashMap<>();

    public InstructionGroup(BytecodeMethod method, List<Instruction> instructions) {
        super(method, -1);
        this.instructions = instructions;

        inputs = new ArrayList<>();
        outputs = new ArrayList<>();
        var stack = new ArrayList<StackEntry>();
        int temporaries = 0;
        for (Instruction instruction : instructions) {
            while (stack.size() < instruction.getInputs().size()) {
                var type = instruction.getInputs().get(instruction.getInputs().size() - 1 - stack.size()).getArithmeticVariant();
                stack.add(0, new StackEntry(type, temporaries++));
                inputs.add(0, type);
            }
            var operands = stack.subList(stack.size() - instruction.getInputs().size(), stack.size());
            var typedInputs = instruction.getTypedInputs();
            if (typedInputs != null)
                for (int i = 0; i < operands.size(); i++)
                    if (typedInputs.get(i) != null) {
                        int temp = operands.get(i).getTemporary();
                        if (!temporaryTypes.containsKey(temp))
                            temporaryTypes.put(temp, new HashSet<>());
                        temporaryTypes.get(temp).add(typedInputs.get(i));
                    }
            temporaries += instruction.adjustStack(operands, temporaries);

        }
        while (!stack.isEmpty())
            outputs.add(stack.remove(0).getType());
    }

    private static void appendTypedTemporary(StringBuilder builder, StackEntry entry, JavaType type, Map<Integer, Set<String>> addedTypes) {
        if (type.getArrayDimensions() == 0 && type.getReferenceType().equals("java/lang/Object"))
            return;
        String typeName = type.getArrayDimensions() > 0 ? "array" : type.getReferenceType();
        if (!addedTypes.containsKey(entry.getTemporary()))
            addedTypes.put(entry.getTemporary(), new HashSet<>());
        var types = addedTypes.get(entry.getTemporary());
        if (types.contains(typeName))
            return;
        types.add(typeName);
        builder.append("\t\tauto ").append(entry.getTypedTemporary(type)).append(" = object_cast<");
        builder.append(type.getArrayDimensions() > 0 ? "vm::Array" : Utils.getQualifiedClassName(typeName)).append(">(").append(entry).append(");\n");
    }

    @Override
    public void processHierarchy(HashMap<String, BytecodeClass> classMap) {
        for (Instruction instruction: instructions)
            instruction.processHierarchy(classMap);
    }

    @Override
    public void appendUnoptimized(StringBuilder builder) {
        builder.append("\t{\n");

        int temporaries = 0;
        var stack = new ArrayList<StackEntry>();
        var addedTypes = new HashMap<Integer, Set<String>>();

        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);

            while (stack.size() < instruction.getInputs().size()) {
                int temp = temporaries++;
                int paramIndex = instruction.getInputs().size() - 1 - stack.size();
                builder.append("\t\tauto temp").append(temp).append(" = vm::pop<").append(instruction.getInputs().get(paramIndex).getArithmeticType()).append(">(sp);\n");
                var entry = new StackEntry(null, temp);
                stack.add(0, entry);
                if (temporaryTypes.containsKey(temp))
                    for (JavaType type: temporaryTypes.get(temp))
                        appendTypedTemporary(builder, entry, type, addedTypes);
            }

            if (i == instructions.size() - 1)
                while (stack.size() > instruction.getInputs().size())
                    builder.append("\t\tvm::push(sp, ").append(stack.remove(0)).append(");\n");

            var operands = stack.subList(stack.size() - instruction.getInputs().size(), stack.size());
            instruction.appendOptimized(builder, operands, temporaries);
            temporaries += instruction.adjustStack(operands, temporaries);
            for (StackEntry operand: operands)
                if (temporaryTypes.containsKey(operand.getTemporary()))
                    for (JavaType type: temporaryTypes.get(operand.getTemporary()))
                        appendTypedTemporary(builder, operand, type, addedTypes);
        }

        while (!stack.isEmpty())
            builder.append("\t\tvm::push(sp, ").append(stack.remove(0)).append(");\n");

        builder.append("\t}\n");
    }

    @Override
    public void collectDependencies(Set<String> dependencies) {
        for (Instruction instruction: instructions)
            instruction.collectDependencies(dependencies);
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public Instruction getLastInstruction() {
        return instructions.get(instructions.size() - 1);
    }
}
