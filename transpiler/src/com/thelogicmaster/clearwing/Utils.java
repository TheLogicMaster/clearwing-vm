package com.thelogicmaster.clearwing;

import org.objectweb.asm.Type;

import java.nio.charset.StandardCharsets;

public class Utils {

	public static final String[] CPP_KEYWORDS = {
			"alignas", "alignof", "and", "and_eq", "asm", "atomic_cancel", "atomic_commit", "atomic_noexcept", "auto", "bitand", "bitor", "bool", "break", "case", "catch", "char",
			"char8_t", "char16_t", "char32_t", "class", "compl", "concept", "const", "consteval", "constexpr", "constinit", "const_cast", "continue", "co_await", "co_return",
			"co_yield", "decltype", "default", "delete", "do", "double", "dynamic_cast", "else", "enum", "explicit", "export", "extern", "false", "float", "for", "friend", "goto",
			"if", "inline", "int", "long", "mutable", "namespace", "new", "noexcept", "not", "not_eq", "nullptr", "operator", "or", "or_eq", "private", "protected", "public",
			"reflexpr", "register", "reinterpret_cast", "requires", "return", "short", "signed", "sizeof", "static", "static_assert", "static_cast", "struct", "switch",
			"synchronized", "template", "this", "thread_local", "throw", "true", "try", "typedef", "typeid", "typename", "union", "unsigned", "using", "virtual", "void",
			"volatile", "wchar_t", "while", "xor", "xor_eq", "final", "override", "transaction_safe", "transaction_safe_dynamic", "import", "module"
	};

	/**
	 * Replaces invalid characters found in Java symbols for C++ symbols
	 */
	public static String sanitizeName (String name) {
		String sanitized = name.replaceAll("\\.", "/").replace("-", "_");

		// Replace keywords in package names
//		if (sanitized.contains("/")) {
//			String[] sections = sanitized.split("/");
//			StringBuilder builder = new StringBuilder();
//			boolean first = true;
//			for (String section: sections) {
//				if (!first)
//					builder.append("/");
//				for (String keyword: CPP_KEYWORDS)
//					if (keyword.equals(section)) {
//						builder.append("_");
//						break;
//					}
//				builder.append(section);
//				first = false;
//			}
//			sanitized = builder.toString();
//		}

		return sanitized;
	}

	/**
	 * Get a sanitized simple class name from a Java class name
	 */
	public static String getSimpleClassName(String name) {
		int index = name.lastIndexOf('/');
		if (index < 0)
			index = 0;
		return sanitizeName(name.substring(index + 1));
	}

	/**
	 * Get a fully-qualified C++ class name from a Java class name
	 */
	public static String getQualifiedClassName(String name) {
		return sanitizeName(name).replace("/", "_");
	}

	/**
	 * Get a Makefile-safe filename from a Java class or simple name
	 */
	public static String getClassFilename(String name) {
		return sanitizeName(name).replace("$", "_");
	}

	/**
	 * Parse a class description and return a sanitized class name
	 */
	public static String parseClassDescription(String desc) {
		return sanitizeName(desc.substring(1, desc.length() - 1));
	}

	/**
	 * Sanitize method name
	 */
	public static String sanitizeMethod(String className, MethodSignature signature, boolean isStatic) {
		className = getQualifiedClassName(className);
		StringBuilder nameBuilder = new StringBuilder();
		if ("<init>".equals(signature.getName()))
			nameBuilder.append("init_").append(className);
		else if ("<clinit>".equals(signature.getName()))
			nameBuilder.append("clinit_").append(className);
		else {
			if (isStatic)
				nameBuilder.append("S");
			nameBuilder.append("M_").append(className).append("_").append(signature.getName());
		}

		for (JavaType type: signature.getParamTypes())
			nameBuilder.append("_").append(type.getMethodSuffixName());

		if (!signature.getReturnType().isVoid())
			nameBuilder.append("_R_").append(signature.getReturnType().getMethodSuffixName());

		return sanitizeName(nameBuilder.toString());
	}

	/**
	 * Sanitize a field name
	 */
	public static String sanitizeField(String className, String name, boolean isStatic) {
		className = getQualifiedClassName(className);
		return (isStatic ? "SF_" + className + "_" : "F_") + sanitizeName(name);
	}

	/**
	 * Get the qualified package name from a qualified/class name
	 */
	public static String getQualifiedPackage(String name) {
		name = getQualifiedClassName(name);
		int index = name.lastIndexOf('_');
		return index < 0 ? "" : name.substring(0, index - 1);
	}

	/**
	 * Encodes a UTF-8 C++ String and returns the full literal
	 */
	public static String encodeStringLiteral(String string) {
		if (string == null)
			string = "";
		string = string.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
				.replace("\r", "\\r").replace("\t", "\\t");
		StringBuilder builder = new StringBuilder();
		builder.append("u8\"");
		char[] chars = string.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if (c >= 32 && c <= 127)
				builder.append(c);
			else if (Character.isHighSurrogate(c)) {
				byte[] bytes = string.substring(i, i + 2).getBytes(StandardCharsets.UTF_8);
				builder.append(String.format("\\x%02x\\x%02x\\x%02x\\x%02x", bytes[0], bytes[1], bytes[2], bytes[3]));
				i++;
			} else
				builder.append("\\u").append(String.format("%04x", (int)c));
		}
		builder.append("\"_j");
		return builder.toString();
	}

	/**
	 * Get a value representation of a Number, String, or Type
	 */
	public static String getObjectValue(Object o) {
		if (o instanceof Boolean)
			return (Boolean)o ? "1" : "0";
		else if (o instanceof Character)
			return "" + (int)(Character)o;
		else if (o instanceof Byte)
			return "" + (int)(Byte)o;
		else if (o instanceof Short)
			return "" + (int)(Short)o;
		else if (o instanceof Integer)
			return (Integer)o == Integer.MIN_VALUE ? "bit_cast<jint>(0x80000000)" : "jint(" + o + ")";
		else if (o instanceof Long)
			return (Long)o == Long.MIN_VALUE ? "bit_cast<jlong>(0x8000000000000000ll)" : "jlong(" + o + "ll)";
		else if (o instanceof Float) {
			float f = (Float) o;
			if (f == Float.POSITIVE_INFINITY)
				return "jfloat(std::numeric_limits<float>::infinity())";
			else if (f == Float.NEGATIVE_INFINITY)
				return "jfloat(-std::numeric_limits<float>::infinity())";
			else if (Float.isNaN(f))
				return "jfloat(std::numeric_limits<float>::quiet_NaN())";
			else
				return "jfloat(" + f + "f)";
		} else if (o instanceof Double) {
			double d = (Double) o;
			if (d == Double.POSITIVE_INFINITY)
				return "jdouble(std::numeric_limits<double>::infinity())";
			else if (d == Double.NEGATIVE_INFINITY)
				return "jdouble(-std::numeric_limits<double>::infinity())";
			else if (Double.isNaN(d))
				return "jdouble(std::numeric_limits<double>::quiet_NaN())";
			else
				return "jdouble(" + d + ")";
		} else if (o instanceof String)
			return "((jobject) createStringLiteral(ctx, " + Utils.encodeStringLiteral((String)o) + "))";
		else if (o instanceof Type)
			switch (((Type) o).getSort()) {
				case Type.OBJECT, Type.ARRAY -> {
					return "((jobject) " + new JavaType(((Type) o).getDescriptor()).generateClassFetch() + ")";
				}
				default -> throw new TranspilerException("Invalid Type: " + o);
			}
		else
			throw new TranspilerException("Invalid annotation parameter object of type: " + o.getClass());
	}
}
