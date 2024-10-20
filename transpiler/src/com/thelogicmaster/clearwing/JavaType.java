package com.thelogicmaster.clearwing;

/**
 * Represents a Java type with object and array info
 */
public class JavaType {

	private final String desc;
	private final TypeVariants type;
	private final String referenceType;
	private final String registryTypeName;
	private final String registryFullName;
	private final int arrayDimensions;

	public JavaType (String desc) {
		this.desc = desc;

		int lastBracket = desc.lastIndexOf('[');
		if (lastBracket >= 0) {
			desc = desc.substring(lastBracket + 1);
			arrayDimensions = lastBracket + 1;
		} else
			arrayDimensions = 0;

		if (desc.startsWith("L")) {
			referenceType = Utils.sanitizeName(desc.substring(1, desc.length() - 1));
			type = TypeVariants.OBJECT;
			registryTypeName = referenceType;
		} else if (Character.isLowerCase(desc.charAt(0))) {
			referenceType = Utils.sanitizeName(desc);
			type = TypeVariants.OBJECT;
			registryTypeName = referenceType;
		} else if (desc.length() == 1) {
			referenceType = null;
			type = TypeVariants.fromSymbol(desc.charAt(0));
			registryTypeName = type.getRegistryName();
		} else {
			referenceType = Utils.sanitizeName(desc);
			type = TypeVariants.OBJECT;
			registryTypeName = referenceType;	
		}
		registryFullName = "[".repeat(arrayDimensions) + registryTypeName;
	}

	public JavaType(TypeVariants type) {
		this.type = type;
		registryTypeName = type.getRegistryName();
		registryFullName = registryTypeName;
		desc = type == TypeVariants.OBJECT ? "L" + registryTypeName + ";" : registryTypeName;
		referenceType = type == TypeVariants.OBJECT ? registryTypeName : null;
		arrayDimensions = 0;
	}

	/**
	 * Get the Java description for the type
	 */
	public String getDesc () {
		return desc;
	}

	/**
	 * Get the basic enumerated type
	 */
	public TypeVariants getBasicType() {
		return arrayDimensions > 0 ? TypeVariants.OBJECT : type;
	}

	/**
	 * Get the type, or the component type if this is an array type
	 */
	public TypeVariants getComponentType() {
		return type;
	}

	/**
	 * Get the internal type of the object reference being held
	 */
	public String getReferenceType () {
		return referenceType;
	}

	/**
	 * Gets the C++ type, taking array dimensions into account
	 */
	public String getCppType() {
		if (arrayDimensions > 0)
			return TypeVariants.OBJECT.getCppType();
		return type.getCppType();
	}

	/**
	 * Get the C++ type name for use as object fields
	 */
	public String getCppMemberType() {
		return isPrimitive() ? getCppType() : "jref";
	}

	/**
	 * Gets the arithmetic type, taking array dimensions into account
	 */
	public String getArithmeticType() {
		if (arrayDimensions > 0)
			return TypeVariants.OBJECT.getArithmeticType();
		return type.getArithmeticType();
	}

	/**
	 * Gets whether this type can be represented as a Java primitive
	 */
	public boolean isPrimitive() {
		return arrayDimensions == 0 && type != TypeVariants.OBJECT;
	}

	public boolean isVoid() {
		return type == TypeVariants.VOID;
	}

	/**
	 * Get the number of array dimensions, or zero if not an array
	 */
	public int getArrayDimensions() {
		return arrayDimensions;
	}

	/**
	 * Get the full type name, including array dimensions, as can be used in C++ names
	 */
	public String getSafeName() {
		return (arrayDimensions > 0 ? "Array" + arrayDimensions + "_" : "") + registryTypeName.replace("/", "_");
	}

	/**
	 * Get the class name as it will be registered at runtime (Without array brackets)
	 */
	public String getRegistryTypeName() {
		return registryTypeName;
	}

	/**
	 * Get the qualified class name with underscores (Without array brackets)
	 */
	public String getQualifiedTypeName() {
		return registryTypeName == null ? null : Utils.getQualifiedClassName(registryTypeName);
	}

	/**
	 * Get the class name as it will be registered at runtime (With array brackets)
	 */
	public String getRegistryFullName() {
		return registryFullName;
	}

	/**
	 * Get a type name as can be appended as part of a method suffix
	 */
	public String getMethodSuffixName() {
		String component = type == TypeVariants.OBJECT ? getRegistryTypeName().replace('/', '_') : type.getJavaType().getSimpleName();
		return arrayDimensions > 0 ? "Array" + arrayDimensions + "_" + component : component;
	}

	/**
	 * Appends a wrapper prefix if needed to convert from this type to the specified target type.
	 * Returns true if appended.
	 * Prefix is of the form `{@code vm::wrap<type>(}`
	 */
	public boolean appendWrapperPrefix(JavaType target, StringBuilder builder) {
		if (isPrimitive() && !target.isPrimitive())
			builder.append("box").append(getBasicType().getWrapper().getSimpleName()).append("(ctx, ");
		else if (!isPrimitive() && target.isPrimitive())
			builder.append("unbox").append(target.getBasicType().getWrapper().getSimpleName()).append("(");
		else
			return false;
		return true;
	}

	/**
	 * Generate an expression that resolves to a class of the specified type
	 */
	public String generateClassFetch() {
		String classFetch = type == TypeVariants.OBJECT ? "&class_" + Utils.getQualifiedClassName(referenceType) : "&" + type.getCppClass();
		return arrayDimensions > 0 ? "getArrayClass(" + classFetch + ", " + arrayDimensions + ")" : classFetch;
	}

	/**
	 * Generate an expression that resolves to a class of the specified type (The component type for arrays)
	 */
	public String generateComponentClassFetch() {
		return type == TypeVariants.OBJECT ? "&class_" + Utils.getQualifiedClassName(referenceType) : "&" + type.getCppClass();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof JavaType))
			return false;
		JavaType that = (JavaType) o;
		return desc.equals(that.desc);
	}

	@Override
	public int hashCode() {
		return desc.hashCode();
	}

	@Override
	public String toString() {
		return desc;
	}
}
