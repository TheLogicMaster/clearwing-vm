package com.thelogicmaster.clearwing;

import java.lang.reflect.Type;

/**
 * A representation and helper for the various Java types
 */
public enum TypeVariants {
	BOOLEAN(Boolean.TYPE, Boolean.class, "jbool", "jint", "class_boolean", "Z", "i"),
	BYTE(Byte.TYPE, Byte.class, "jbyte", "jint", "class_byte", "B", "i"),
	CHAR(Character.TYPE, Character.class, "jchar", "jint", "class_char", "C", "i"),
	SHORT(Short.TYPE, Short.class, "jshort", "jint", "class_short", "S", "i"),
	INT(Integer.TYPE, Integer.class, "jint", "jint", "class_int", "I", "i"),
	LONG(Long.TYPE, Long.class, "jlong", "jlong", "class_long", "J", "l"),
	FLOAT(Float.TYPE, Float.class, "jfloat", "jfloat", "class_float", "F", "f"),
	DOUBLE(Double.TYPE, Double.class, "jdouble", "jdouble", "class_double", "D", "d"),
	OBJECT(Object.class, Object.class, "jobject", "jobject", "class_java_lang_Object", "java/lang/Object", "o"),
	VOID(Void.TYPE, Void.class, "void", "void", "class_java_lang_Void", "V", null);

	private final Class<?> javaType;
	private final Class<?> wrapper;
	private final String cppType;
	private final String arithmeticType;
	private final String cppClass;
	private final String registryName;
	private final String stackName;

	TypeVariants (Class<?> javaType, Class<?> wrapper, String cppType, String arithmeticType, String cppClass, String registryName, String stackName) {
		this.javaType = javaType;
		this.wrapper = wrapper;
		this.cppType = cppType;
		this.arithmeticType = arithmeticType;
		this.cppClass = cppClass;
		this.registryName = registryName;
		this.stackName = stackName;
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

	public String getStackName() {
		return stackName;
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
