package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;
import org.objectweb.asm.util.Printer;

import java.util.*;

/**
 * The base class for all instructions and pseudo-instructions
 */
public abstract class Instruction {

	public static final String LABEL_PREFIX = "label_";

	protected final int opcode;
	protected final BytecodeMethod method;
	protected List<JavaType> inputs;
	protected List<JavaType> outputs;
	private boolean markedForRemoval;

	public Instruction (BytecodeMethod method, int opcode) {
		this.method = method;
		this.opcode = opcode;
	}

	/**
	 * Get the lowercase name corresponding to the opcode, or null if there isn't one
	 */
	protected final String getOpcodeName() {
		if (opcode == -1)
			return null;
		String name = Printer.OPCODES[opcode];
		return name == null ? null : name.toLowerCase();
	}

	public void processHierarchy(HashMap<String, BytecodeClass> classMap) {
	}

	public void resolveSymbols() {
	}

	/**
	 * Append the raw instruction to the method output
	 */
	public abstract void appendUnoptimized(StringBuilder builder);

	/**
	 * Append optimized instruction (Either as an expression, or full statements, depending on the outputs)
	 */
	public void appendOptimized(StringBuilder builder, List<StackEntry> operands, int temporaries) {
		throw new TranspilerException("Instruction isn't optimizable: " + this);
	}

	/**
	 * Whether the instruction can be inlined into another (In general, no side effects on stack and such)
	 */
	public boolean inlineable() {
		return false;
	}

	/**
	 * Append as an inline expression (Todo: API)
	 */
	public void appendInlined(StringBuilder builder) {

	}

	/**
	 * Append instruction with parameters inlined (Todo: API)
	 */
	public void appendWithInlining(StringBuilder builder) {

	}

	/**
	 * Indicate that this instruction has been optimized out and should be removed after optimization is done
	 */
	public final void markForRemoval() {
		markedForRemoval = true;
	}

	/**
	 * Whether this instruction can be removed due to being optimized out
	 */
	public final boolean isMarkedForRemoval() {
		return markedForRemoval;
	}

	/**
	 * Adjust the operand stack after an instruction, returns the number of new temporaries
	 */
	public int adjustStack(List<StackEntry> operands, int temporaries) {
		operands.clear();
		if (outputs == null)
			return 0;
		for (JavaType output : outputs)
			operands.add(new StackEntry(output, temporaries++));
		return outputs.size();
	}

	/**
	 * Populate inputs and outputs
	 */
	public abstract void resolveIO(List<StackEntry> stack);

	protected void setInputsFromStack(List<StackEntry> stack, int args) {
		if (stack.size() < args)
			return;
		inputs = new ArrayList<>();
		for (int i = stack.size() - args; i < stack.size(); i++)
			inputs.add(stack.get(i).getType());
	}

	protected void setBasicInputs(TypeVariants ... types) {
		inputs = new ArrayList<>();
		for (TypeVariants type : types)
			inputs.add(new JavaType(type));
	}

	protected void setInputs(JavaType ... types) {
		inputs = Arrays.asList(types);
	}

	protected void setBasicOutputs(TypeVariants ... types) {
		outputs = new ArrayList<>();
		for (TypeVariants type : types)
			outputs.add(new JavaType(type));
	}

	protected void setOutputs(JavaType ... types) {
		outputs = Arrays.asList(types);
	}

	/**
	 * Get the input types (Null means unspecified)
	 */
	public final List<JavaType> getInputs() {
		return inputs;
	}

	/**
	 * Get the output types (Null means unspecified)
	 */
	public final List<JavaType> getOutputs() {
		return outputs;
	}

	/**
	 * Get the containing method
	 */
	public final BytecodeMethod getMethod() {
		return method;
	}

	/**
	 * Get the corresponding opcode for an instruction or -1 if it doesn't have one
	 */
	public final int getOpcode () {
		return opcode;
	}

	/**
	 * Collect sanitized dependency class names
	 */
	public void collectDependencies(Set<String> dependencies) {
	}

	/**
	 * Append the default ending for unoptimized instructions
	 */
	protected static void appendStandardInstruction(StringBuilder builder, String name, String ... args) {
		builder.append("\tINST_").append(name.toUpperCase()).append("(");
		boolean first = true;
		for (String arg: args) {
			if (!first)
				builder.append(", ");
			builder.append(arg);
			first = false;
		}
		builder.append(");\n");
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + opcode + ")";
	}
}
