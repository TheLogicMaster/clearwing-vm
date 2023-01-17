package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.BytecodeMethod;
import com.thelogicmaster.clearwing.StackEntry;
import com.thelogicmaster.clearwing.TypeVariants;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
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
	public void appendUnoptimized (StringBuilder builder) {
		if (getMethod().isLocalKnown(local))
			builder.append("\tlocal").append(local).append("++;\n");
		else
			appendStandardInstruction(builder, "iinc", "local" + local, "" + amount);
	}

	@Override
	public void appendOptimized(StringBuilder builder, List<StackEntry> operands, int temporaries) {
		if (getMethod().isLocalKnown(local))
			builder.append("\t\tlocal").append(local).append(" += ").append(amount).append(";\n");
		else
			builder.append("\t\tget<jint>(local").append(local).append(") += ").append(amount).append(";\n");
	}

	@Override
	public void populateIO(List<StackEntry> stack) {
		inputs = Collections.emptyList();
		outputs = Collections.emptyList();
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
