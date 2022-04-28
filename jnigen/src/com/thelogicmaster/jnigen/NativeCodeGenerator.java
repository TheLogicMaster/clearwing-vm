package com.thelogicmaster.jnigen;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import java.io.File;
import java.util.ArrayList;
import java.util.Optional;

public class NativeCodeGenerator {
	private final AntPathMatcher matcher = new AntPathMatcher();
	private final JavaMethodParser javaMethodParser = new RobustJavaMethodParser();

	private FileDescriptor source;
	private FileDescriptor dest;
	private String[] includes;
	private String[] excludes;

	public void generate(String source, String dest) throws Exception {
		generate(source, dest, new String[0], new String[0]);
	}

	public void generate (String source, String dest, String[] includes, String[] excludes) throws Exception {
		this.source = new FileDescriptor(source);
		this.dest = new FileDescriptor(dest);
		this.includes = includes;
		this.excludes = excludes;

		if (!this.source.exists())
			throw new RuntimeException("Java source directory does not exist: " + source);

		if (!this.dest.exists() && !this.dest.mkdirs())
			throw new RuntimeException("Failed to create destination directory: " + dest);

		processDirectory(this.source);
	}

	private void processDirectory(FileDescriptor dir) throws Exception {
		for (FileDescriptor file: dir.list()) {
			if (!file.isDirectory()) {
				if (!file.extension().equals("java") || (excludes.length > 0 && matcher.match(file.path(), excludes)) || !matcher.match(file.path(), includes))
					continue;

				String className = file.path().replace(source.path(), "").replace('\\', '_').replace('/', '_').replace(".java", "");
				if (className.startsWith("_"))
					className = className.substring(1);

				String java = file.readString();
				if (!java.contains("native"))
					continue;

				ArrayList<JavaMethodParser.JavaSegment> segments = javaMethodParser.parse(java);

				if (segments.size() == 0) {
					System.out.println("No JNI code found, skipping: " + file);
					continue;
				}
				System.out.println("Generating C++ for: " + file);

				generateCppFile(segments, className);
			} else if (excludes.length == 0 || !matcher.match(file.path(), excludes))
				processDirectory(file);
		}
	}

	private void generateCppFile(ArrayList<JavaMethodParser.JavaSegment> segments, String className) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("// Auto-generated C++ for ClearwingVM\n\n");

		buffer.append("extern \"C\" {\n");
		buffer.append("#include \"cn1_globals.h\"\n");
		buffer.append("#include \"java_nio_ByteBuffer.h\"\n");
		buffer.append("}\n");

		for (JavaMethodParser.JavaSegment segment: segments) {
			if (segment instanceof JavaMethodParser.JniSection) {
				JavaMethodParser.JniSection section = (JavaMethodParser.JniSection)segment;
				buffer.append("\n// @Line: ").append(section.getStartIndex()).append('\n');
				buffer.append(section.getNativeCode().replace("\r", "")).append('\n');
			} else if (segment instanceof JavaMethodParser.JavaMethod) {
				JavaMethodParser.JavaMethod method = (JavaMethodParser.JavaMethod)segment;
				if (method.getNativeCode() == null) {
					System.out.println("Warning: No native method body for: " + method.getName());
					continue;
				}

				buffer.append("extern \"C\" ");
				buffer.append(getVMType(method.getReturnType())).append(' ');
				buffer.append(className);
				if (!method.getClassName().equals(className.substring(className.lastIndexOf('_') + 1)))
					buffer.append('_').append(method.getClassName());
				buffer.append('_').append(method.getName()).append("__");

				for (Parameter arg: method.getArguments())
					buffer.append('_').append(getSignatureType(arg.getType(), method.getCompilationUnit()));

				if (!method.getReturnType().isVoidType())
					buffer.append("_R_").append(getSignatureType(method.getReturnType(), method.getCompilationUnit()));

				buffer.append("(CODENAME_ONE_THREAD_STATE");
				if (!method.isStatic())
					buffer.append(", JAVA_OBJECT object");
				for (int i = 0; i < method.getArguments().size(); i++) {
					Parameter arg = method.getArguments().get(i);
					JavaMethodParser.ArgumentType type = method.getArgumentTypes().get(i);
					buffer.append(", ").append(getVMType(arg.getType())).append(' ').append(arg.getName());
					if (type.isPrimitiveArray() || type.isBuffer() || type.isString())
						buffer.append("__object");
				}
				buffer.append(") {\n");

				for (int i = 0; i < method.getArguments().size(); i++) {
					Parameter arg = method.getArguments().get(i);
					JavaMethodParser.ArgumentType type = method.getArgumentTypes().get(i);
					if (!type.isBuffer() && !type.isPrimitiveArray() && !type.isString())
						continue;
					buffer.append("\tauto ").append(arg.getName()).append(" = ");
					if (type.isBuffer()) {
						buffer.append('(').append(type.getBufferCType()).append(") ");
						buffer.append("((obj__java_nio_ByteBuffer *) ").append(arg.getName()).append("__object)->java_nio_Buffer_address;\n");
					} else if (type.isString())
						buffer.append("toNativeString(threadStateData, ").append(arg.getName()).append("__object").append(");\n");
					else if (type.isPrimitiveArray())
						buffer.append('(').append(type.getArrayCType()).append(") ((JAVA_ARRAY) ").append(arg.getName()).append("__object)->data;\n");
				}

				String code = method.getNativeCode().replace("\r", "");
				while (code.startsWith("\n"))
					code = code.substring(1);
				while (code.endsWith("\t"))
					code = code.substring(0, code.length() - 1);
				buffer.append(code);

				buffer.append("}\n\n");
			}
		}

		new FileDescriptor(dest + File.separator + className + "_natives.cpp").writeString(buffer.toString(), false);
	}

	private String getSignatureType(Type type, CompilationUnit compilationUnit) {
		int array = 0;
		if (type.isArrayType()) {
			array = type.asArrayType().getArrayLevel();
			type = type.asArrayType().getComponentType();
		}

		String name;
		if (type.isPrimitiveType())
			name = type.asPrimitiveType().getType().name().toLowerCase();
		else {
			Optional<ClassOrInterfaceType> classType = type.asClassOrInterfaceType().getScope();
			name = type.asClassOrInterfaceType().getName().getIdentifier();
			while (classType.isPresent()) {
				name = classType.get().getName().getIdentifier() + '_' + name;
				classType = classType.get().getScope();
			}

			for (ImportDeclaration declaration: compilationUnit.getImports()) {
				String text = declaration.getNameAsString().replace('.', '_');
				if (!text.equals(name) && text.endsWith(name))
					name = text;
			}
		}

		if (array != 0)
			return name + '_' + array + "ARRAY";

		return name;
	}

	private String getVMType(Type type) {
		if (type.isVoidType())
			return "JAVA_VOID";
		if (type.isPrimitiveType())
			return "JAVA_" + type.asPrimitiveType().getType().name().toUpperCase();
		return "JAVA_OBJECT";
	}

	public static void main (String[] args) throws Exception {
		new NativeCodeGenerator().generate(args[0], args[1]);
	}
}
