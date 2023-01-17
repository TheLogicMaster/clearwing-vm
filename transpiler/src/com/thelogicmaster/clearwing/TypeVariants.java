package com.thelogicmaster.clearwing;

import java.lang.reflect.Type;

/**
 * A representation and helper for the various Java types
 */
public enum TypeVariants {
	BOOLEAN(Boolean.TYPE, Boolean.class, "jbool", "jint", "vm::classBoolean", "Z"),
	BYTE(Byte.TYPE, Byte.class, "jbyte", "jint", "vm::classByte", "B"),
	CHAR(Character.TYPE, Character.class, "jchar", "jint", "vm::classChar", "C"),
	SHORT(Short.TYPE, Short.class, "jshort", "jint", "vm::classShort", "S"),
	INT(Integer.TYPE, Integer.class, "jint", "jint", "vm::classInt", "I"),
	LONG(Long.TYPE, Long.class, "jlong", "jlong", "vm::classLong", "L"),
	FLOAT(Float.TYPE, Float.class, "jfloat", "jfloat", "vm::classFloat", "F"),
	DOUBLE(Double.TYPE, Double.class, "jdouble", "jdouble", "vm::classDouble", "D"),
	OBJECT(Object.class, Object.class, "jobject", "jobject", "java::lang::Object", "java/lang/Object"),
	VOID(Void.TYPE, Void.class, "void", "void", null, "V");

	private final Class<?> javaType;
	private final Class<?> wrapper;
	private final String cppType;
	private final String arithmeticType;
	private final String cppClass;
	private final String registryName;

	TypeVariants (Class<?> javaType, Class<?> wrapper, String cppType, String arithmeticType, String cppClass, String registryName) {
		this.javaType = javaType;
		this.wrapper = wrapper;
		this.cppType = cppType;
		this.arithmeticType = arithmeticType;
		this.cppClass = cppClass;
		this.registryName = registryName;
	}

	/**
	 * Get the corresponding Java type
	 */
	public Class<?> getJavaType () {
		return javaType;
	}

	/**
	 * Get the wrapper type
	 */
	public Class<?> getWrapper() {
		return wrapper;
	}

	/**
	 * Get the type as stored in an array or as function parameters
	 */
	public String getCppType () {
		return cppType;
	}

	/**
	 * Get the type as stored on the stack frame
	 */
	public String getArithmeticType () {
		return arithmeticType;
	}

	/**
	 * Get the code for referencing the type's class
	 */
	public String getCppClass() {
		return cppClass;
	}

	/**
	 * Get name of the type as it shows in the registry
	 */
	public String getRegistryName() {
		return registryName;
	}

	public boolean isWide() {
		return this == DOUBLE || this == LONG;
	}

	public TypeVariants getArithmeticVariant() {
		if (this == BOOLEAN || this == BYTE || this == CHAR || this == SHORT)
			return INT;
		return this;
	}

	/**
	 * Get the variant corresponding to a particular Java Type (For example, Integer.TYPE)
	 * Returns null if not found
	 */
	public static TypeVariants fromJavaType(Type javaType) {
		for (TypeVariants type: values())
			if (type.javaType == javaType)
				return type;
		return null;
	}

	/**
	 * Get a primitive type from a type symbol
	 */
	public static TypeVariants fromSymbol(char symbol) {
		return switch (Character.toUpperCase(symbol)) {
			case 'I' -> TypeVariants.INT;
			case 'V' -> TypeVariants.VOID;
			case 'Z' -> TypeVariants.BOOLEAN;
			case 'B' -> TypeVariants.BYTE;
			case 'C' -> TypeVariants.CHAR;
			case 'S' -> TypeVariants.SHORT;
			case 'D' -> TypeVariants.DOUBLE;
			case 'F' -> TypeVariants.FLOAT;
			case 'J' -> TypeVariants.LONG;
			default -> throw new TranspilerException("Invalid JavaType");
		};
	}
}
