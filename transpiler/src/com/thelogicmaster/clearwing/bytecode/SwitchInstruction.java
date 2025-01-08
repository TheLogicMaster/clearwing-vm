package com.thelogicmaster.clearwing.bytecode;

import com.thelogicmaster.clearwing.BytecodeMethod;
import com.thelogicmaster.clearwing.StackEntry;
import com.thelogicmaster.clearwing.TranspilerConfig;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An instruction for either type of switch tables
 */
public class SwitchInstruction extends Instruction implements JumpingInstruction {

	private final int[] keys;
	private final int[] originalLabels;
	private final int[] labels;
	private final int originalDefaultLabel;
	private int defaultLabel;

	private final int[] labelBypasses;
	private int defaultLabelBypass = -1;
	private final int[] exceptionPops;
	private int defaultExceptionPops = 0;

	public SwitchInstruction (BytecodeMethod method, int[] keys, Label[] labels, Label defaultLabel) {
		super(method, Opcodes.LOOKUPSWITCH);
		this.keys = keys;
		this.labels = new int[labels.length];
		this.originalLabels = new int[labels.length];
		for (int i = 0; i < labels.length; i++) {
			this.labels[i] = method.getLabelId(labels[i]);
			this.originalLabels[i] = this.labels[i];
		}
		this.defaultLabel = method.getLabelId(defaultLabel);
		this.originalDefaultLabel = this.defaultLabel;
		labelBypasses = new int[labels.length];
		Arrays.fill(labelBypasses, -1);
		exceptionPops = new int[labels.length];
	}

	private void appendSwitch(StringBuilder builder, String value) {
		builder.append("\t").append("switch(").append(value).append(") {\n");
		for (int i = 0; i < keys.length; i++) {
			builder.append("\t").append("\tcase ").append(keys[i]).append(": ");
			appendGoto(builder, labelBypasses[i], labels[i], originalLabels[i], exceptionPops[i]);
			builder.append("\n");
		}
		if (defaultLabel != -1) {
			builder.append("\t").append("\tdefault: ");
			appendGoto(builder, defaultLabelBypass, defaultLabel, originalDefaultLabel, defaultExceptionPops);
			builder.append("\n");
		}
		builder.append("\t").append("}\n");
	}

	@Override
	public void appendUnoptimized (StringBuilder builder, TranspilerConfig config) {
		appendSwitch(builder, "(--sp)->i");
	}

	@Override
	public void appendOptimized(StringBuilder builder, TranspilerConfig config) {
		appendSwitch(builder, inputs.get(0).arg());
	}

	@Override
	public void resolveIO(List<StackEntry> stack) {
		setInputsFromStack(stack, 1);
	}

	public int[] getKeys() {
		return keys;
	}

	public int[] getLabels() {
		return labels;
	}

	public int getDefaultLabel() {
		return defaultLabel;
	}

	public int[] getOriginalLabels() {
		return originalLabels;
	}

	public int getOriginalDefaultLabel() {
		return originalDefaultLabel;
	}

	@Override
	public void setJumpBypass(int bypass, int label, int bypassLabel) {
		if (label == defaultLabel) {
			defaultLabel = bypassLabel;
			defaultLabelBypass = bypass;
			return;
		}
		for (int i = 0; i < labels.length; i++)
			if (label == labels[i]) {
				labels[i] = bypassLabel;
				labelBypasses[i] = bypass;
				break;
			}
	}

	@Override
	public void setJumpExceptionPops(int index, int pops) {
		if (index == labels.length)
			defaultLabelBypass = pops;
		else
			exceptionPops[index] = pops;
	}

	@Override
	public List<Integer> getJumpLabels() {
		ArrayList<Integer> jumpLabels = new ArrayList<>();
		for (int label: labels)
			jumpLabels.add(label);
		if (defaultLabel != -1)
			jumpLabels.add(defaultLabel);
		return jumpLabels;
	}
}
