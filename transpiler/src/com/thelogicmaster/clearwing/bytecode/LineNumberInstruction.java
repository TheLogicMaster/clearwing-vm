package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.BytecodeMethod;
import com.thelogicmaster.clearwing.StackEntry;
import com.thelogicmaster.clearwing.TranspilerConfig;
import org.objectweb.asm.Label;

import java.util.List;

/**
 * A pseudo-instruction representing a line number
 */
public class LineNumberInstruction extends Instruction {

	private final int line;
	private final Label start;

	public LineNumberInstruction (BytecodeMethod method, int line, Label start) {
		super(method, -1);
		this.line = line;
		this.start = start;
	}

	@Override
	public void appendUnoptimized (StringBuilder builder, TranspilerConfig config) {
		builder.append("\tLINE_NUMBER(").append(line).append(");\n");
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

	public int getLine () {
		return line;
	}

	public Label getStart () {
		return start;
	}
}
