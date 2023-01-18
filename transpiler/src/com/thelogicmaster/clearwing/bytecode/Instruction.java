package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.*;
import org.objectweb.asm.util.Printer;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * The base class for all instructions and pseudo-instructions
 */
public abstract class Instruction {

	public static final String LABEL_PREFIX = "label_";

	protected final int opcode;
	protected final BytecodeMethod method;
	protected List<TypeVariants> inputs;
	protected List<JavaType> typedInputs;
	protected List<TypeVariants> outputs;
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
	 * A simple check for the last instruction to push the field/method's owner to see if it's `this`. Doesn't work for DUP and such.
	 * Marks the source instruction for removal if successful
	 */
	protected boolean onThisCheck(Instruction source, String owner) {
		if (getMethod().isStatic() || !(source instanceof VariableInstruction) || !Utils.sanitizeName(owner).equals(getMethod().getOwner().getName()))
			return false;
		VariableInstruction instruction = ((VariableInstruction) source);
		if (instruction.getLocal() != 0)
			return false;
		instruction.markForRemoval();
		return true;
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
		for (TypeVariants output : outputs)
			operands.add(new StackEntry(output, temporaries++));
		return outputs.size();
	}

	/**
	 * Populate inputs and outputs
	 */
	public void populateIO(List<StackEntry> stack) {
	}

	/**
	 * Get the input types (Null means unspecified)
	 */
	public final List<TypeVariants> getInputs() {
		return inputs;
	}

	/**
	 * Get the reference types of the inputs, if known (Null means unspecified, null values mean unknown)
	 */
	public final List<JavaType> getTypedInputs() {
		return typedInputs;
	}

	/**
	 * Get the output types (Null means unspecified)
	 */
	public final List<TypeVariants> getOutputs() {
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
		builder.append("\tinst::").append(name).append("(sp");
		for (String arg: args)
			builder.append(", ").append(arg);
		builder.append(");\n");
	}
}
