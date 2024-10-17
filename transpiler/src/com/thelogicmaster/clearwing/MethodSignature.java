package com.thelogicmaster.clearwing;

import java.util.ArrayList;

public class MethodSignature {

	private final String name;
	private final String desc;
	private final JavaType returnType;
	private final JavaType[] paramTypes;
	private final BytecodeClass owner;

	public MethodSignature (String name, String desc, BytecodeClass owner) {
		this.name = name;
		this.desc = desc;
		this.owner = owner;
		int returnIndex = desc.indexOf(')');
		String paramDesc = desc.substring(1, returnIndex);
		ArrayList<JavaType> params = new ArrayList<>();
		while (!paramDesc.isEmpty()) {
			int start = 0;
			if (paramDesc.charAt(0) == '[')
				for (start = 1; start < paramDesc.length(); start++)
					if (paramDesc.charAt(start) != '[')
						break;
			int index = paramDesc.charAt(start) == 'L' ? paramDesc.indexOf(';') + 1 : start + 1;
			params.add(new JavaType(paramDesc.substring(0, index)));
			paramDesc = paramDesc.substring(index);
		}
		paramTypes = params.toArray(new JavaType[0]);
		returnType = new JavaType(desc.substring(returnIndex + 1));
	}

	/**
	 * Append the argument types and names for a function definition
	 */
	public void appendMethodArgs(StringBuilder builder) {
		for (int i = 0; i < paramTypes.length; i++) {
			if (i > 0)
				builder.append(", ");
			builder.append(paramTypes[i].getCppType()).append(" ");
			builder.append("param").append(i);
		}
	}

	/**
	 * Get the unsanitized name
	 */
	public String getName() {
		return name;
	}

	public String getDesc () {
		return desc;
	}

	public BytecodeClass getOwner() {
		return owner;
	}

	public JavaType getReturnType () {
		return returnType;
	}

	public JavaType[] getParamTypes () {
		return paramTypes;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof MethodSignature))
			return false;
		MethodSignature that = (MethodSignature) o;
		return desc.equals(that.desc) && name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return 31 * name.hashCode() + desc.hashCode();
	}

	@Override
	public String toString() {
		return "MethodSignature{" +
				"name='" + name + '\'' +
				", desc='" + desc + '\'' +
				'}';
	}
}
