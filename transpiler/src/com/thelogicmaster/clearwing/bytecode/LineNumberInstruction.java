package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.BytecodeMethod;
import com.thelogicmaster.clearwing.StackEntry;
import org.objectweb.asm.Label;

import java.util.Collections;
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
	public void appendUnoptimized (StringBuilder builder) {
		builder.append("\tframeRef->lineNumber = ").append(line).append(";\n");
	}

	@Override
	public void appendOptimized(StringBuilder builder, List<StackEntry> operands, int temporaries) {
		builder.append("\t\tvm::setLineNumber(").append(line).append(");\n");
	}

	@Override
	public void resolveIO(List<StackEntry> stack) {
		setBasicInputs();
		setBasicOutputs();
	}

	public int getLine () {
		return line;
	}

	public Label getStart () {
		return start;
	}
}
