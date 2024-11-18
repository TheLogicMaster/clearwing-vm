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
	protected List<StackEntry> inputs;
	protected List<StackEntry> outputs;
	protected int stackDepth = -1;
	protected int instructionIndex = -1;
	private boolean inlined;

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
	public abstract void appendUnoptimized(StringBuilder builder, TranspilerConfig config);

	/**
	 * Append optimized instruction (Either as an expression, or full statements, depending on the outputs)
	 */
	public void appendOptimized(StringBuilder builder, TranspilerConfig config) {
		throw new TranspilerException("Instruction isn't optimizable: " + this);
	}

	/**
	 * Whether the instruction purely exists to route stack values
	 */
	public boolean isRoutingInstruction() {
		return false;
	}
	
	/**
	 * Whether the instruction can be inlined into another (In general, no side effects on stack and such)
	 */
	public boolean inlineable() {
		return false;
	}

	public void inline() {
		if (!inlineable() || outputs.size() != 1)
			throw new TranspilerException("Instruction isn't inlineable: " + this);
		inlined = true;
		outputs.get(0).makeInlined();
	}
	
	public boolean isInlined() {
		return inlined;
	}
	
	/**
	 * Append as an inline expression
	 */
	public void appendInlined(StringBuilder builder) {
		throw new TranspilerException("Instruction isn't inlinable: " + this);
	}

	public void setStackDepth(int depth) {
		if (stackDepth >= 0 && stackDepth != depth)
			throw new TranspilerException("Inconsistent stack depth");
		stackDepth = depth;
	}
	
	public int getStackDepth() {
		return stackDepth;
	}
	
	public void setInstructionIndex(int index) {
		instructionIndex = index;
	}
	
	/**
	 * Populate inputs and outputs
	 */
	public abstract void resolveIO(List<StackEntry> stack);

	protected boolean setInputsFromStack(List<StackEntry> stack, int args) {
		if (stack.size() < args)
			return false;
		inputs = new ArrayList<>();
		for (int i = stack.size() - args; i < stack.size(); i++)
			inputs.add(stack.get(i));
		return true;
	}
	
	protected void setInputs(StackEntry ... types) {
		inputs = Arrays.asList(types);
	}

	protected void setBasicOutputs(TypeVariants ... types) {
		outputs = new ArrayList<>();
		for (TypeVariants type : types)
			outputs.add(new StackEntry(new JavaType(type), this));
	}

	protected void setOutputs(StackEntry ... types) {
		outputs = Arrays.asList(types);
	}

	/**
	 * Get the input types (Null means unspecified)
	 */
	public final List<StackEntry> getInputs() {
		return inputs;
	}

	/**
	 * Get the output types (Null means unspecified)
	 */
	public final List<StackEntry> getOutputs() {
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
		return getClass().getSimpleName() + (opcode < 0 ? "" : "(" + Printer.OPCODES[opcode] + ")");
	}
}
