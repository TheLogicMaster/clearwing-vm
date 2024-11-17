package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.BytecodeMethod;
import com.thelogicmaster.clearwing.StackEntry;
import com.thelogicmaster.clearwing.TranspilerConfig;
import com.thelogicmaster.clearwing.TypeVariants;
import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * The IINC instruction, which adds a constant amount to a local
 */
public class IncrementInstruction extends Instruction implements LocalInstruction {

	private final int local;
	private final int amount;

	public IncrementInstruction (BytecodeMethod method, int local, int amount) {
		super(method, Opcodes.IINC);
		this.local = local;
		this.amount = amount;
	}

	@Override
	public void appendUnoptimized (StringBuilder builder, TranspilerConfig config) {
		appendStandardInstruction(builder, "iinc", "" + local, "" + amount);
	}

	@Override
	public void appendOptimized(StringBuilder builder, TranspilerConfig config) {
		appendUnoptimized(builder, config);
	}

	@Override
	public void resolveIO(List<StackEntry> stack) {
		setInputs();
		setBasicOutputs();
	}

	@Override
	public TypeVariants getLocalType() {
		return TypeVariants.INT;
	}

	@Override
	public int getLocal () {
		return local;
	}

	public int getAmount () {
		return amount;
	}
}
