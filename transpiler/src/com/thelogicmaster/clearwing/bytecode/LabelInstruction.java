package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.BytecodeMethod;
import com.thelogicmaster.clearwing.StackEntry;
import org.objectweb.asm.Label;

import java.util.List;

/**
 * A pseudo-instruction representing a label
 */
public class LabelInstruction extends Instruction {

	private final int label;

	public LabelInstruction (BytecodeMethod method, Label label) {
		super(method, -1);
		this.label = method.getLabelId(label);
	}

	@Override
	public void appendUnoptimized (StringBuilder builder) {
		builder.append(LABEL_PREFIX).append(label).append(":;\n");
	}

	public int getLabel () {
		return label;
	}

	@Override
	public void resolveIO(List<StackEntry> stack) {
		setBasicInputs();
		setBasicOutputs();
	}
}
