package com.thelogicmaster.clearwing;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.Type;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Transpiler {

	private final static String[] NATIVE_DEPENDENCIES = {
			"java/lang/ArithmeticException",
			"java/lang/reflect/Array",
			"java/lang/Boolean",
			"java/nio/Buffer",
			"java/lang/Byte",
			"java/lang/Character",
			"java/lang/Class",
			"java/lang/ClassNotFoundException",
			"java/lang/reflect/Constructor",
			"java/lang/reflect/InvocationTargetException",
			"java/lang/reflect/Proxy",
			"java/text/DateFormat",
			"java/lang/Double",
			"java/lang/Enum",
			"java/lang/ExceptionInInitializerError",
			"java/lang/reflect/Field",
			"java/io/File",
			"java/io/FileInputStream",
			"java/io/FileOutputStream",
			"java/lang/Float",
			"java/util/HashMap",
			"java/lang/Integer",
			"java/lang/IllegalMonitorStateException",
			"java/lang/InterruptedException",
			"java/util/Locale",
			"java/lang/Long",
			"java/lang/Math",
			"java/lang/NoSuchMethodError",
			"java/lang/OutOfMemoryError",
			"java/lang/reflect/Method",
			"java/io/NativeOutputStream",
			"java/nio/NativeUtils",
			"java/lang/Runtime",
			"java/lang/Short",
			"java/lang/String",
			"java/lang/StringBuilder",
			"java/lang/StringToReal",
			"java/lang/StackOverflowError",
			"java/lang/System",
			"java/lang/Thread",
			"java/lang/Thread$UncaughtExceptionHandler",
			"java/lang/Throwable",
			"java/util/zip/CRC32",
			"java/util/zip/Deflater",
			"java/util/zip/Inflater",
			"java/lang/ref/WeakReference",
			"java/util/zip/ZipFile",
	};

	private static void collect(BytecodeClass clazz, Set<BytecodeClass> collected, HashMap<String, BytecodeClass> classMap) {
		collected.add(clazz);
		clazz.collectDependencies(classMap);
		Set<String> dependencies = clazz.getDependencies();
		for (String dependency: dependencies) {
			if (dependency.equals("java/lang/Object"))
				continue;
			BytecodeClass depClass = classMap.get(dependency);
			if (depClass == null) {
				System.err.println("Failed to find class dependency: " + dependency);
				continue;
			}
			if (!collected.contains(depClass))
				collect(depClass, collected, classMap);
		}
	}

	private static void copyResources(String source, String prefix, File outputDir) throws IOException {
		try (ScanResult scanResult = new ClassGraph().acceptPaths(source).scan()) {
			for (Resource resource: scanResult.getAllResources()) {
				Path path = Paths.get(outputDir.getPath(), resource.getPath().substring(prefix.length()));
				path.getParent().toFile().mkdirs();
				try (InputStream input = resource.open()) {
					Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}

	private static void addScanResultSuppliers(ScanResult scanResult, ArrayList<Supplier<InputStream>> sources) {
		for (ClassInfo info: scanResult.getAllClasses())
			sources.add(() -> {
				try {
					return info.getResource().open();
				} catch (IOException e) {
					throw new TranspilerException("Failed to open file", e);
				}
			});
	}

	private static <T> Collection<Map.Entry<String, T>> filterByPattern(String expression, Collection<Map.Entry<String, T>> entries) {
		expression = expression.replace('.', '/').replace("$", "\\$").replace("**", "@").replace("*", "\\w*").replace("@", ".*");
		Pattern pattern = Pattern.compile(expression);
		return entries.stream().filter(entry -> pattern.matcher(entry.getKey()).matches()).collect(Collectors.toList());
	}

	/**
	 * Generates Cpp files from inlined jnigen style native methods
	 */
	private static void processSources(List<File> sourceDirs, File outputDir, List<String> ignorePatterns) throws IOException {
		HashMap<String, Path> sourceMap = new HashMap<>();
		for (File source: sourceDirs)
			try (Stream<Path> stream = Files.find(source.toPath(), Integer.MAX_VALUE, (path, attr) -> attr.isRegularFile() && path.toString().endsWith(".java"))) {
				stream.forEach(path -> sourceMap.put(source.toPath().relativize(path).toString().replace('\\', '/'), path));
			}
		Collection<Map.Entry<String, Path>> sources = sourceMap.entrySet();
		for (String pattern: ignorePatterns)
			sources = filterByPattern(pattern, sources);

		Function<Type, String> typeToCpp = type -> {
			if (type.isVoidType())
				return "void";
			if (type.isPrimitiveType())
				return "j" + type.asPrimitiveType().getType().name().toLowerCase().replaceAll("ean", "");
			return "jobject";
		};

		JavaMethodParser parser = new RobustJavaMethodParser();
		for (Map.Entry<String, Path> entry: sources) {
			String name = entry.getKey().substring(0, entry.getKey().length() - 5);

			String code = Files.readString(entry.getValue());
			if (!code.contains("native"))
				continue;
			ArrayList<JavaMethodParser.JavaSegment> segments = parser.parse(code);
			if (segments.isEmpty())
				continue;

			StringBuilder builder = new StringBuilder();
			builder.append("#include \"Clearwing.h\"\n");
			builder.append("#include \"java/nio/Buffer.h\"\n");

			HashSet<String> includes = new HashSet<>();
			includes.add(name);
			for (JavaMethodParser.JavaSegment segment: segments)
				if (segment instanceof JavaMethodParser.JavaMethod)
					includes.add(((JavaMethodParser.JavaMethod) segment).getSanitizedName(name));
			for (String include: includes)
				builder.append("#include <").append(Utils.getClassFilename(include)).append(".h>\n");
			builder.append("\n");

			for (JavaMethodParser.JavaSegment segment: segments)
				if (segment instanceof JavaMethodParser.JniSection) {
					JavaMethodParser.JniSection section = (JavaMethodParser.JniSection)segment;
					builder.append(section.getNativeCode().replace("\r", "")).append('\n');
				} else if (segment instanceof JavaMethodParser.JavaMethod) {
					JavaMethodParser.JavaMethod method = (JavaMethodParser.JavaMethod)segment;
					if (method.getNativeCode() == null) {
						System.err.println("Warning: No native method body for: " + method.getName());
						continue;
					}
					MethodSignature signature = new MethodSignature(method.getName(), method.getDescriptor(), null);
					String methodName = Utils.sanitizeMethod(method.getSanitizedName(name), signature, method.isStatic());

					builder.append(typeToCpp.apply(method.getReturnType())).append(" ").append(methodName);
					builder.append("(jcontext ctx");
					if (!method.isStatic())
						builder.append(", jobject self");
					for (int i = 0; i < method.getArguments().size(); i++) {
						Parameter arg = method.getArguments().get(i);
						JavaMethodParser.ArgumentType type = method.getArgumentTypes().get(i);
						builder.append(", ").append(typeToCpp.apply(arg.getType())).append(" ").append(arg.getName());
						if (type.isPrimitiveArray() || type.isBuffer() ||type.isString())
							builder.append("_object");
					}
					builder.append(") {\n");
					for (int i = 0; i < method.getArguments().size(); i++) {
						Parameter arg = method.getArguments().get(i);
						JavaMethodParser.ArgumentType type = method.getArgumentTypes().get(i);
						if (!type.isBuffer() && !type.isPrimitiveArray() && !type.isString())
							continue;
						builder.append("\tauto ").append(arg.getName()).append(" = ");
						if (type.isBuffer())
							builder.append("(").append(type.getPointerType()).append(")((java_nio_Buffer *)").append(arg.getName()).append("_object)->F_address;\n");
						else if (type.isString())
							builder.append("stringToNative(ctx, (jstring) ").append(arg.getName()).append("_object);\n");
						else if (type.isPrimitiveArray())
							builder.append("(").append(type.getPointerType()).append(")((jarray)").append(arg.getName()).append("_object)->data;\n");
					}
					builder.append("\n");

					String methodCode = method.getNativeCode().replace("\r", "");
					while (methodCode.startsWith("\n"))
						methodCode = methodCode.substring(1);
					while (methodCode.endsWith("\t"))
						methodCode = methodCode.substring(0, methodCode.length() - 1);
					builder.append(methodCode);

					builder.append("}\n\n");
				}

			File file = new File(outputDir, name + "_native.cpp");
			file.getParentFile().mkdirs();
			try (Writer writer = new BufferedWriter(new FileWriter(file))) {
				writer.write(builder.toString());
			}
		}
	}

	public static void transpile(List<File> inputs, List<File> sourceDirs, File outputDir, TranspilerConfig config) throws IOException {
		// Parse input class files
		Parser parser = new Parser(config);
		ArrayList<Supplier<InputStream>> sources = new ArrayList<>();
		List<BytecodeClass> classes;
		ArrayList<Closeable> parserCloseables = new ArrayList<>();
		ScanResult scanResult = new ClassGraph().acceptPaths("regexodus/", "java/").enableClassInfo().ignoreClassVisibility().scan();
		parserCloseables.add(scanResult);
		addScanResultSuppliers(scanResult, sources);

		for (File input: inputs) {
			if (input.isDirectory())
				try (Stream<Path> stream = Files.find(input.toPath(), Integer.MAX_VALUE, (path, attr) -> attr.isRegularFile() && path.toString().endsWith(".class"))) {
					sources.addAll(stream.map(path -> (Supplier<InputStream>) () -> {
						try {
							return Files.newInputStream(path);
						} catch (IOException e) {
							throw new TranspilerException("Failed to open file", e);
						}
					}).collect(Collectors.toList()));
				}
			else {
				if (!input.getName().endsWith(".jar"))
					continue;
				ZipFile zipFile = new ZipFile(input);
				parserCloseables.add(zipFile);
				Enumeration<? extends ZipEntry> entries = zipFile.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					if (entry.isDirectory())
						continue;
					if (entry.getName().endsWith("clearwing.json")) {
						config.merge(new TranspilerConfig(new String(zipFile.getInputStream(entry).readAllBytes())));
						continue;
					}
					if (!entry.getName().endsWith(".class"))
						continue;
					sources.add(() -> {
						try {
							return zipFile.getInputStream(entry);
						} catch (IOException e) {
							throw new TranspilerException("Failed to open file", e);
						}
					});
				}
			}
		}

		classes = parser.parse(sources);

		for (Closeable result: parserCloseables)
			result.close();

		HashSet<BytecodeClass> required = new HashSet<>();

		// Collect classes into map
		HashMap<String, BytecodeClass> classMap = new HashMap<>();
		for (BytecodeClass clazz: classes)
			classMap.put(clazz.getName(), clazz);

		// Process hierarchy to insert proxy methods and such
		for (BytecodeClass clazz: classes)
			clazz.processHierarchy(classMap);

		// Resolve symbols like methods once the entire hierarchy is generated
		for (BytecodeClass clazz : classes)
			clazz.resolveSymbols();

		// Collect dependencies for class trimming
		for (BytecodeClass clazz: classes)
			clazz.collectDependencies(classMap);

		// Mark intrinsic methods
		for (String intrinsic: config.getIntrinsics()) {
			int separator = intrinsic.lastIndexOf('.');
			int descIndex = intrinsic.indexOf('(');
			if (separator < 0 || descIndex < 0) {
				System.out.println("Warning: Invalid intrinsic format: '" + intrinsic + "'");
				continue;
			}
			BytecodeClass clazz = classMap.get(Utils.sanitizeName(intrinsic.substring(0, separator)));
			if (clazz == null) {
				System.out.println("Warning: Failed to find class for intrinsic: '" + intrinsic + "'");
				continue;
			}
			String name = intrinsic.substring(separator + 1, descIndex);
			MethodSignature signature = new MethodSignature(name, intrinsic.substring(descIndex), null);
			boolean found = false;
			for (BytecodeMethod method: clazz.getMethods())
				if (signature.equals(method.getSignature())) {
					method.markIntrinsic();
					found = true;
					break;
				}
			if (!found)
				System.out.println("Warning: Failed to mark method as intrinsic for: '" + intrinsic + "'");
			collect(clazz, required, classMap);
		}

		// Find main class
		BytecodeClass mainClass = null;
		String main = config.getMainClass() == null ? null : config.getMainClass().replace('.', '/');
		for (BytecodeClass c: classes) {
			if (main != null && !c.getOriginalName().equals(main))
				continue;
			for (BytecodeMethod method: c.getMethods())
				if (method.isMain()) {
					if (mainClass != null)
						throw new TranspilerException("Multiple main classes found");
					mainClass = c;
					break;
				}
		}
		if (mainClass == null && inputs.size() > 0)
			throw new TranspilerException("Failed to find main class");

		// Collect required classes
		for (String dep: NATIVE_DEPENDENCIES)
			collect(classMap.get(dep), required, classMap);
		ArrayList<String> requiredPatterns = new ArrayList<>(config.getNonOptimized());
		for (String pattern: requiredPatterns)
			for (Map.Entry<String, BytecodeClass> entry: filterByPattern(pattern, classMap.entrySet()))
				collect(entry.getValue(), required, classMap);
		if (mainClass != null)
			collect(mainClass, required, classMap);

		// Todo: Trim unused methods

		// Write transpiled output
		File srcDir = new File(outputDir, "src");
		File includeDir = srcDir;//new File(outputDir, "include");
		boolean failed = false;
		for (BytecodeClass clazz: required) {
			File header = new File(includeDir, Utils.getClassFilename(clazz.getName()) + ".h");
			Paths.get(header.getAbsolutePath()).getParent().toFile().mkdirs();
			FileWriter writer = new FileWriter(header);
			StringBuilder builder = new StringBuilder();
			clazz.generateHeader(builder, config, classMap);
			writer.write(builder.toString());
			writer.close();

			File cpp = new File(srcDir, Utils.getClassFilename(clazz.getName()) + ".cpp");
			Paths.get(cpp.getAbsolutePath()).getParent().toFile().mkdirs();
			writer = new FileWriter(cpp);
			builder = new StringBuilder();
			try {
				clazz.generateCpp(builder, config, classMap);
			} catch (Exception e) {
				System.err.println("ERR: " + e.getMessage());
				failed = true;
			}
			writer.write(builder.toString());
			writer.close();
		}
		if (failed)
			throw new TranspilerException("Failed to transpile sources");

		// Write main.cpp
		if (mainClass != null)
			try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(new File(outputDir.getPath(), "src/main.cpp")))) {
				writer.write("" +
						"#include \"" + Utils.getClassFilename(mainClass.getName()) + ".h\"\n" +
						"#include \"Clearwing.h\"\n" +
						"\n" +
						"int main() {\n" +
						"\trunVM(SM_" + mainClass.getQualifiedName() + "_main_Array1_java_lang_String);\n" +
						"}\n"
				);
			}

		// Write config header
		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(new File(outputDir.getPath(), "src/Config.h")))) {
			writer.write("#pragma once\n\n" +
					"#ifndef USE_LINE_NUMBERS\n#define USE_LINE_NUMBERS " + config.hasLineNumbers() + "\n#endif\n\n" +
					"#ifndef USE_VALUE_CHECKS\n#define USE_VALUE_CHECKS " + config.hasValueChecks() + "\n#endif\n\n" +
					"#ifndef USE_PLATFORM_OVERRIDE\n#define USE_PLATFORM_OVERRIDE " + config.hasPlatformOverride() + "\n#endif\n\n"
			);
		}

		// Generate natives from jnigen style comments
		processSources(sourceDirs, new File(outputDir, "src"), config.getSourceIgnores());

		// Copy resources to output
		copyResources("clearwing/src/", "clearwing/", outputDir);
		copyResources("clearwing/include/", "clearwing/", outputDir);
		if (config.isWritingProjectFiles())
			copyResources("clearwing/project/", "clearwing/project/", outputDir);
	}

	private static List<File> getFileArgs(Namespace namespace, String name) {
		List<String> paths = namespace.getList(name);
		ArrayList<File> files = new ArrayList<>();
		if (paths != null)
			for (String path: paths)
				files.add(new File(path));
		return files;
	}

	// Todo: Logging
	// Todo: Configurable definitions for Config.hpp (For selective native patches, for example)
	public static void main (String[] args) throws IOException, ArgumentParserException {
		ArgumentParser parser = ArgumentParsers.newFor("Transpiler").build()
				.defaultHelp(true)
				.description("Run the transpiler");
		parser.addArgument("-i", "--input")
				.required(false)
				.nargs("*")
				.help("Input class directories and JARs (Later ones have higher priority)");
		parser.addArgument("-s", "--source")
				.required(false)
				.nargs("*")
				.help("Source code root directories (src/main/java)");
		parser.addArgument("-o", "--output")
				.required(true)
				.help("The output directory");
		parser.addArgument("-m", "--main")
				.required(false)
				.help("The main/entrypoint class");
		parser.addArgument("-c", "--config")
				.required(false)
				.help("An optional config file");
		parser.addArgument("-p", "--project")
				.type(Boolean.class)
				.required(false)
				.setDefault(false)
				.help("Copy default project files");
		Namespace namespace = parser.parseArgs(args);

		List<File> inputs = getFileArgs(namespace, "input");
		List<File> sourceDirs = getFileArgs(namespace, "source");

		File outputDir = new File(namespace.getString("output"));

		String configPath = namespace.getString("config");
		TranspilerConfig config = configPath == null ? new TranspilerConfig() : new TranspilerConfig(Files.readString(Path.of(configPath)));
		if (namespace.getString("main") != null)
			config.setMainClass(namespace.getString("main"));
		if (namespace.getBoolean("project") != null)
			config.setWritingProjectFiles(namespace.getBoolean("project"));
		transpile(inputs, sourceDirs, outputDir, config);
	}
}
