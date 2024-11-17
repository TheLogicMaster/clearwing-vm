package com.thelogicmaster.clearwing;

import com.thelogicmaster.clearwing.bytecode.Instruction;
import com.thelogicmaster.clearwing.bytecode.JumpingInstruction;

import java.util.*;

public class InstructionGroup extends Instruction {

    private final List<Instruction> instructions;

    public InstructionGroup(BytecodeMethod method, List<Instruction> instructions) {
        super(method, -1);
        this.instructions = instructions;

        inputs = new ArrayList<>();
        outputs = new ArrayList<>();
        
        for (int i = instructions.size() - 1; i >= 0; i--){
             Instruction instruction = instructions.get(i);
             for (StackEntry input : instruction.getInputs()) {
                 boolean found = false;
                 for (int j = i - 1; j >= 0; j--) {
                     if (!instructions.get(j).getOutputs().contains(input))
                         continue;
                     found = true;
                     break;
                 }
                 if (found)
                     continue;
                 inputs.add(input);
             }
        }
        
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            if (instruction.getOutputs() == null)
                continue;
            for (StackEntry output : instruction.getOutputs()) {
                boolean found = false;
                for (int j = i + 1; j < instructions.size(); j++) {
                    if (!instructions.get(j).getInputs().contains(output))
                        continue;
                    found = true;
                    break;
                }
                if (found)
                    continue;
                outputs.add(output);
            }
        }
    }

    @Override
    public void processHierarchy(HashMap<String, BytecodeClass> classMap) {
        for (Instruction instruction: instructions)
            instruction.processHierarchy(classMap);
    }

    private int inlineCheck(int index) {
        Instruction candidate = instructions.get(index);
        if (!candidate.inlineable() || candidate.getOutputs().size() != 1) 
            return -1;
        StackEntry output = candidate.getOutputs().get(0);
        if (output.getConsumers().size() != 1)
            return -1;
        Instruction consumer = output.getConsumers().get(0);
        if (!instructions.contains(consumer))
            return -1;
        for (int i = index; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            int result = inlineCheck(i);
            if (instruction == consumer)
                return result < 0 ? i : result;
            else if (result < 0)
                return -1;
        }
        return -1;
    }
    
    @Override
    public void appendUnoptimized(StringBuilder builder, TranspilerConfig config) {
        // Todo: Before adding inlining, remove all NULL_CHECK, CHECK_CAST, and ARRAY_ACCESS to prevent evaluating twice
        int tempCount = 0;
        builder.append("\t{\n");
        if (!inputs.isEmpty())
            builder.append("\tsp -= ").append(inputs.size()).append(";\n");
        
        ArrayList<StackEntry> stack = new ArrayList<>();
        while (stack.size() < method.getStackSize() - instructions.get(0).getStackDepth() + inputs.size())
            stack.add(null);
        int[] stackUsages = new int[stack.size()];
        for (int i = 0; i < inputs.size(); i++) {
            StackEntry input = inputs.get(i).getOriginal();
            int index = stack.indexOf(input);
            if (index >= 0) {
                stackUsages[index] += 1;
                continue;
            }
            stack.set(i, input);
            stackUsages[i] = 1;
            input.makeStack(i);
        }
        
        Instruction lastInstruction = instructions.get(instructions.size() - 1);
        boolean specialLast = lastInstruction.getOutputs() == null || lastInstruction instanceof JumpingInstruction;
        
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            if (specialLast && instruction == lastInstruction)
                break;

            for (StackEntry input : instruction.getInputs()) {
                StackEntry originalInput = input.getOriginal();
                if (originalInput.getOperandType() != OperandType.Stack)
                    continue;
                int index = stack.indexOf(originalInput);
                if (index >= 0 && stackUsages[index] > 0)
                    stackUsages[index]--;
                else
                    throw new TranspilerException("Failed to decrement stack slot");
            }
            
            for (StackEntry output : instruction.getOutputs()) {
                StackEntry originalOutput = output.getOriginal();
                int index = stack.indexOf(originalOutput);
                if (index >= 0) {
                    stackUsages[index]++;
                } else if (originalOutput.getType().isPrimitive()) {
                    output.makeTemporary(tempCount++);
                } else {
                    boolean placed = false;
                    for (int j = 0; j < stack.size(); j++) {
                        if (stackUsages[j] <= 0) {
                            stackUsages[j] = 1;
                            stack.set(j, originalOutput);
                            originalOutput.makeStack(j);
                            placed = true;
                            break;
                        }
                    }
                    if (!placed)
                        throw new TranspilerException("Failed to allocate stack slot");
                }
            }
            
            if (!instruction.isRoutingInstruction())
                instruction.appendOptimized(builder, config);
            
            for (StackEntry input : instruction.getInputs()) {
                StackEntry originalInput = input.getOriginal();
                if (originalInput.getOperandType() != OperandType.Stack)
                    continue;
                int index = stack.indexOf(originalInput);
                if (index >= 0 && stackUsages[index] <= 0)
                    stack.set(index, null);
            }
        }
        
        for (int i = 0; i < outputs.size(); i++) {
            StackEntry output = outputs.get(i).getOriginal();
            if (specialLast && output.getSource() == lastInstruction)
                continue;
            boolean isTemp = output.getOperandType() == OperandType.Temporary;
            int index = stack.indexOf(output); // Todo: No need to search once it is assured that entry indices are all correct
            if (!isTemp && index < 0)
                throw new TranspilerException("Missing instruction group output");
            if (!isTemp && index == i)
                continue;
            if (stackUsages[i] > 0) {
                // Storing an object in a temp variable is safe here because the last instruction must be a jump or return
                StackEntry temp = stack.get(i).getOriginal();
                builder.append("\t").append(temp.getType().getArithmeticType()).append(" temp").append(tempCount++).append(" = ");
                temp.buildArg(builder);
                builder.append(";\n");
                temp.makeTemporary(tempCount - 1);
            }
            builder.append("\tsp[").append(i).append("].").append(output.getBasicType().getStackName()).append(" = ");
            output.buildArg(builder);
            builder.append(";\n");
            stack.set(i, output);
            stackUsages[i] = 1;
        }
        
        if (!outputs.isEmpty())
            builder.append("\tsp += ").append(outputs.size()).append(";\n");
        
        if (specialLast) {
            for (StackEntry input : lastInstruction.getInputs()) {
                if (input.getOperandType() != OperandType.Stack)
                    continue;
                input.makeStack(input.getIndex() - outputs.size());
            }
            
            if (lastInstruction.getOutputs() != null) {
                for (int i = 0; i < lastInstruction.getOutputs().size(); i++) {
                    StackEntry output = lastInstruction.getOutputs().get(i);
                    output.makeStack(i - outputs.size());
                }
            }
            
            lastInstruction.appendOptimized(builder, config);
        }
        
        builder.append("\t}\n");
    }

//            int inlineOffset = inlineCheck(i);
//            if (inlineOffset >= 0) {
//                Instruction container = instructions.get(inlineOffset);
//                StackEntry dest = null;
//                if (!container.getOutputs().isEmpty()) {
//                    StackEntry output = container.getOutputs().get(0);
//                    if (output.getBasicType() == TypeVariants.OBJECT) {
//                        dest = new StackEntry(output.getType(), );
//                    } else {
//                        dest = new StackEntry(output.getType(), );
//                    }
//                }
//                container.appendOptimized(builder, config, container.getInputs(), dest);
//                i = inlineOffset;
//            }
    
    @Override
    public void resolveIO(List<StackEntry> stack) {
    }

    @Override
    public void resolveSymbols() {
        for (Instruction instruction: instructions)
            instruction.resolveSymbols();
    }

    @Override
    public void collectDependencies(Set<String> dependencies) {
        for (Instruction instruction: instructions)
            instruction.collectDependencies(dependencies);
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }
}
