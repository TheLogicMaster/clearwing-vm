package com.thelogicmaster.clearwing;

import com.thelogicmaster.clearwing.bytecode.Instruction;
import com.thelogicmaster.clearwing.bytecode.SuperCallInstruction;
import org.objectweb.asm.Opcodes;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BytecodeClass {

	private final static BytecodeClass OBJECT_CLASS = new BytecodeClass("java/lang/Object", null, null, Opcodes.ACC_PUBLIC);
	private final static BytecodeMethod[] OBJECT_METHODS = {
			new BytecodeMethod(OBJECT_CLASS, "hashCode", Opcodes.ACC_PUBLIC, "()I", null, null),
			new BytecodeMethod(OBJECT_CLASS, "equals", Opcodes.ACC_PUBLIC, "(Ljava/lang/Object;)Z", null, null),
			new BytecodeMethod(OBJECT_CLASS, "clone", Opcodes.ACC_PUBLIC, "()Ljava/lang/Object;", null, null),
			new BytecodeMethod(OBJECT_CLASS, "getClass", Opcodes.ACC_PUBLIC, "()Ljava/lang/Class;", null, null),
			new BytecodeMethod(OBJECT_CLASS, "toString", Opcodes.ACC_PUBLIC, "()Ljava/lang/String;", null, null),
			new BytecodeMethod(OBJECT_CLASS, "finalize", Opcodes.ACC_PROTECTED, "()V", null, null),
			new BytecodeMethod(OBJECT_CLASS, "notify", Opcodes.ACC_PUBLIC, "()V", null, null),
			new BytecodeMethod(OBJECT_CLASS, "notifyAll", Opcodes.ACC_PUBLIC, "()V", null, null),
			new BytecodeMethod(OBJECT_CLASS, "wait", Opcodes.ACC_PUBLIC, "()V", null, null),
			new BytecodeMethod(OBJECT_CLASS, "wait", Opcodes.ACC_PUBLIC, "(J)V", null, null),
			new BytecodeMethod(OBJECT_CLASS, "wait", Opcodes.ACC_PUBLIC, "(JI)V", null, null),
	};

	private final String originalName;
	private final String originalSuperName;
	private final String superName;
	private final String qualifiedSuperName;
	private final String[] interfaces;
	private final String[] originalInterfaces;
	private int access;
	private final String simpleName;
	private final String qualifiedName;
	private final String name;

	private String signature;
	private boolean anonymous;
	private boolean nested;

	private boolean reflective;
	private final ArrayList<BytecodeMethod> methods = new ArrayList<>();
	private final ArrayList<BytecodeField> fields = new ArrayList<>();
	private final HashSet<String> dependencies = new HashSet<>();
	private final ArrayList<BytecodeAnnotation> annotations = new ArrayList<>();
	private final BytecodeAnnotation defaultAnnotation;
	private boolean hierarchyProcessed;

	public BytecodeClass (String name, String superName, String[] interfaces, int access) {
		this.originalName = name;
		this.originalSuperName = superName;
		this.originalInterfaces = interfaces == null ? new String[0] : interfaces;
		this.access = access;

		this.qualifiedName = Utils.getQualifiedClassName(name);
		this.superName = superName == null ? "java/lang/Object" : Utils.sanitizeName(superName);
		this.qualifiedSuperName = Utils.getQualifiedClassName(this.superName);
		simpleName = Utils.getSimpleClassName(name);
		this.name = Utils.sanitizeName(name);
		this.interfaces = new String[originalInterfaces.length];
		for (int i = 0; i < originalInterfaces.length; i++)
			this.interfaces[i] = Utils.sanitizeName(originalInterfaces[i]);
		defaultAnnotation = isAnnotation() ? new BytecodeAnnotation(this.name) : null;

		if (isEnum())
			reflective = true;
	}

	public void addMethod(BytecodeMethod method) {
		methods.add(method);
	}

	public void addField(BytecodeField field) {
		fields.add(field);
	}

	public ArrayList<BytecodeMethod> getMethods () {
		return methods;
	}

	public ArrayList<BytecodeField> getFields () {
		return fields;
	}

	public void setSignature (String signature) {
		this.signature = signature;
	}

	public void setAnonymous(boolean anonymous) {
		this.anonymous = anonymous;
	}

	public boolean isAnonymous() {
		return anonymous;
	}

	public void markNested() {
		this.nested = true;

		// Not sure why flag has to be added here
		if (isInterface() || isAnnotation() || isEnum())
			access |= Opcodes.ACC_STATIC;
	}

	public boolean isNested() {
		return nested;
	}

	public boolean isReflective() {
		return reflective;
	}

	public void setReflective(boolean reflective) {
		this.reflective = reflective;
	}

	public boolean hasFinalizer() {
		for (BytecodeMethod method: methods)
			if (method.isFinalizer())
				return true;
		return false;
	}

	public boolean hasStaticInitializer() {
		for (BytecodeMethod method: methods)
			if (method.isStaticInitializer())
				return true;
		return false;
	}

	/**
	 * Returns if any annotation data is present
	 */
	public boolean hasAnnotations() {
		if (!annotations.isEmpty())
			return true;
		for (BytecodeMethod method: methods)
			if (!method.getAnnotations().isEmpty())
				return true;
		for (BytecodeField field: fields)
			if (!field.getAnnotations().isEmpty())
				return true;
		return false;
	}

	public void addAnnotation(BytecodeAnnotation annotation) {
		annotations.add(annotation);
	}

	public ArrayList<BytecodeAnnotation> getAnnotations() {
		return annotations;
	}

	/**
	 * Get default annotation if this class is an annotation, otherwise null
	 */
	public BytecodeAnnotation getDefaultAnnotation() {
		return defaultAnnotation;
	}

	/**
	 * Collects all class dependencies (Sanitized class names)
	 */
	public void collectDependencies(Map<String, BytecodeClass> classMap) {
		dependencies.clear();
		dependencies.add(superName);
		dependencies.addAll(Arrays.asList(interfaces));
		for (BytecodeMethod method: methods)
			method.collectDependencies(dependencies, classMap);
		for (BytecodeField field: fields)
			field.collectDependencies(dependencies, classMap);
		if (defaultAnnotation != null)
			defaultAnnotation.collectDependencies(dependencies, classMap);
		for (BytecodeAnnotation annotation: annotations)
			annotation.collectDependencies(dependencies, classMap);
	}

	/**
	 * Returns a set of cached dependencies (Does not perform collection)
	 */
	public Set<String> getDependencies() {
		return dependencies;
	}

	private void collectHierarchyMethod(BytecodeMethod method, Map<MethodSignature, Set<BytecodeMethod>> methodSignatures) {
		MethodSignature type = method.getSignature();
		if (!methodSignatures.containsKey(type))
			methodSignatures.put(type, new HashSet<>());
		methodSignatures.get(type).add(method);
	}

	private void collectHierarchyMethods(String clazz, Map<String, BytecodeClass> classMap, Map<MethodSignature, Set<BytecodeMethod>> methodSignatures) {
		if (!classMap.containsKey(clazz))
			return;
		BytecodeClass c = classMap.get(clazz);

		if (c.superName.equals("java/lang/Object") && !isInterface()) {
			for (BytecodeMethod method: OBJECT_METHODS)
				collectHierarchyMethod(method, methodSignatures);
		} else if (!isInterface())
			collectHierarchyMethods(c.superName, classMap, methodSignatures);
		for (String inter: c.interfaces)
			collectHierarchyMethods(inter, classMap, methodSignatures);

		for (BytecodeMethod method: c.methods) {
			if (method.isConstructor() || method.isStaticInitializer() || method.isStatic())
				continue;
			collectHierarchyMethod(method, methodSignatures);
		}
	}

	private void processHierarchyOrWarn(HashMap<String, BytecodeClass> classMap, String className) {
		BytecodeClass clazz = classMap.get(className);
		if (clazz == null)
			System.out.println("Warning: Failed to find dependent class: " + className);
		else
			clazz.processHierarchy(classMap);
	}

	/**
	 * Insert virtual functions where needed to clear up ambiguity with C++ dual inheritance
	 */
	public void processHierarchy(HashMap<String, BytecodeClass> classMap) {
		if (hierarchyProcessed)
			return;
		hierarchyProcessed = true;
		if ("java/lang/Object".equals(name))
			return;
		if (!superName.equals("java/lang/Object"))
			processHierarchyOrWarn(classMap, superName);
		for (String inter: interfaces)
			processHierarchyOrWarn(classMap, inter);

		for (BytecodeMethod method: methods)
			method.processHierarchy(classMap);

		Map<MethodSignature, Set<BytecodeMethod>> methodSignatures = new HashMap<>();
		collectHierarchyMethods(name, classMap, methodSignatures);

		for (Map.Entry<MethodSignature, Set<BytecodeMethod>> entry: methodSignatures.entrySet()) {
			if (entry.getValue().size() == 1)
				continue;
			boolean exists = false;
			for (BytecodeMethod method: methods)
				if (method.getSignature().equals(entry.getKey())) {
					exists = true;
					break;
				}
			if (exists)
				continue;

			BytecodeMethod m = entry.getValue().iterator().next();
			boolean implemented = false;
			for (BytecodeMethod method: entry.getValue())
				if (!method.isAbstract()) {
					implemented = true;
					m = method;
					break;
				}

			// Todo: Use labels for loop breaks
			BytecodeMethod newMethod;
			if (implemented) {
				BytecodeClass implementationClass = null;
				for (BytecodeClass clazz = classMap.get(superName); clazz != null; clazz = classMap.get(clazz.superName)) {
					for (BytecodeMethod method : clazz.methods)
						if (method.getSignature().equals(m.getSignature()) && !method.isAbstract()) {
							implementationClass = clazz;
							break;
						}
					if (implementationClass != null)
						break;
				}
				if (implementationClass == null)
					for (BytecodeClass clazz = classMap.get(superName); clazz != null; clazz = classMap.get(clazz.superName)) {
						for (String inter : clazz.interfaces) {
							if (!classMap.containsKey(inter))
								continue;
							for (BytecodeMethod method : classMap.get(inter).methods)
								if (method.getSignature().equals(m.getSignature()) && !method.isAbstract()) {
									implementationClass = clazz;
									break;
								}
							if (implementationClass != null)
								break;
						}
						if (implementationClass != null)
							break;
					}
				if (implementationClass == null && !isInterface() && !isEnum())
					for (BytecodeMethod method: OBJECT_METHODS)
						if (method.getSignature().equals(m.getSignature())) {
							implementationClass = OBJECT_CLASS;
							break;
						}
				if (implementationClass == null)
					throw new TranspilerException("Failed to find super implementation for: " + m);

				newMethod = new BytecodeMethod(this, m.getOriginalName(), Opcodes.ACC_SYNTHETIC, m.getDesc(), null, null);
				newMethod.addInstruction(new SuperCallInstruction(newMethod, implementationClass.name, m.getName(), m.getSignature()));
				newMethod.markGenerated();
			} else
				newMethod = new BytecodeMethod(this, m.getOriginalName(), Opcodes.ACC_ABSTRACT | Opcodes.ACC_SYNTHETIC, m.getDesc(), null, null);
			methods.add(newMethod);
		}
	}

	// Todo: Move reflection methods into static per-class functions to reduce linkage
	public void generateHeader(StringBuilder builder, TranspilerConfig config, HashMap<String, BytecodeClass> classMap) {
		builder.append("#pragma once\n\n");

		builder.append("#include <").append(Utils.getClassFilename(superName)).append(".hpp>\n");
		if (isAnnotation())
			builder.append("#include <java/lang/Class.hpp>\n");
		for (String clazz: interfaces)
			builder.append("#include <").append(Utils.getClassFilename(clazz)).append(".hpp>\n");
		builder.append("\n");

		// Todo: Ignore Object/Class/String
		var forwardedClasses = new HashMap<String, Set<String>>();
		Consumer<JavaType> addForwards = type -> {
			if (type.getArrayDimensions() == 0 && !type.isPrimitive()) {
				String packageName = Utils.getQualifiedPackage(Utils.getQualifiedClassName(type.getReferenceType()));
				if (!forwardedClasses.containsKey(packageName))
					forwardedClasses.put(packageName, new HashSet<>());
				forwardedClasses.get(packageName).add(Utils.getSimpleClassName(type.getReferenceType()));
			}
		};
		for (var method: methods) {
			addForwards.accept(method.getSignature().getReturnType());
			for (JavaType type: method.getSignature().getParamTypes())
				addForwards.accept(type);
		}
		for (var field: fields)
			addForwards.accept(field.getType());
		for (var entry: forwardedClasses.entrySet()) {
			builder.append("namespace ").append(entry.getKey()).append(" {\n");
			for (String name : entry.getValue())
				builder.append("\tclass ").append(name).append(";\n");
			builder.append("}\n");
		}

		builder.append("namespace ").append(Utils.getQualifiedPackage(qualifiedName)).append(" {\n\n");
		builder.append("\tclass ").append(simpleName);

		builder.append(" : ");
		if (isInterface())
			builder.append("public virtual std::enable_shared_from_this<java::lang::Object>");
		else
			builder.append("public virtual ").append(qualifiedSuperName);
		for (String clazz : interfaces)
			builder.append(", public virtual ").append(Utils.getQualifiedClassName(clazz));

		builder.append(" {\n");
		builder.append("\tpublic:\n");

		// Todo: Break loop labels for efficiency
		HashSet<String> usingMethods = new HashSet<>();
		BiConsumer<BytecodeMethod, BytecodeMethod> processUsingMethods = (method, m) -> {
			String name = m.getOwner().qualifiedName + "::" + m.getName();
			if (method.getName().equals(m.getName()) && !m.getSignature().equals(method.getSignature()) && !usingMethods.contains(name)) {
				usingMethods.add(name);
				builder.append("\tusing ").append(name).append(";\n");
			}
		};
		for (var method: methods) {
			if (method.isConstructor() || method.isStaticInitializer())
				continue;
			for (String interfaceName: interfaces)
				for (BytecodeMethod m: Objects.requireNonNull(classMap.get(interfaceName)).methods)
					processUsingMethods.accept(method, m);
			if (superName.equals("java/lang/Object"))
				continue;
			BytecodeClass parent = classMap.get(superName);
			while (parent != null) {
				for (BytecodeMethod m: parent.methods)
					processUsingMethods.accept(method, m);
				for (String interfaceName: parent.interfaces)
					for (BytecodeMethod m: Objects.requireNonNull(classMap.get(interfaceName)).methods)
						processUsingMethods.accept(method, m);
				if (parent.superName.equals("java/lang/Object"))
					break;
				if (classMap.get(parent.superName) == null)
					throw new TranspilerException("Failed to find super class for: " + parent.superName);
				parent = classMap.get(parent.superName);
			}
		}

		builder.append("\t\texplicit ").append(simpleName).append("();\n");
		builder.append("\t\t~").append(simpleName).append(!isInterface() && !isEnum() && !isAnnotation() ? "();\n" : "() = default;\n");
		if (isInterface())
			builder.append("\t\tvirtual jobject get_this() = 0;\n");
		else
			builder.append("\t\tinline jobject get_this() override { return java::lang::Object::get_this(); }\n");
		if (reflective && isInstantiatable())
			builder.append("\t\tinline static jobject newObject() { clinit(); return make_shared<").append(simpleName).append(">(); }\n");
		builder.append("\t\tstatic const std::string NAME;\n");
		builder.append("\t\tstatic const jclass CLASS;\n");
		builder.append("\t\tstatic void clinit();\n");
		if (!isAbstract() && !isInterface() && !isEnum() && !isAnnotation())
			builder.append("\t\tvirtual jobject clone();\n\n");

		for (var method: methods) {
			if (method.isStaticInitializer())
				continue;
			builder.append(method.isStatic() ? "\t\tstatic " : "\t\tvirtual ");
			builder.append(method.getSignature().getReturnType().getCppMemberType(false)).append(" ").append(method.getName()).append("(");
			method.getSignature().appendMethodArgs(builder);
			builder.append(")");
			if (method.isAbstract())
				builder.append(" = 0");
			builder.append(";\n");
		}
		builder.append("\n");

		for (var field: fields) {
			builder.append("\t\t");
			if (field.isStatic())
				builder.append("static ");
			if (field.isVolatile() && field.getType().isPrimitive())
				builder.append("volatile ");
			builder.append(field.getType().getCppMemberType(field.isWeak())).append(" ").append(field.getName()).append(field.isStatic() ? "" : "{}").append(";\n");
		}
		builder.append("\t};\n\n");

		if (isAnnotation()) {
			builder.append("\tclass ").append(simpleName).append("Impl : public virtual java::lang::Object, public virtual ").append(simpleName).append(" {\n");
			builder.append("\tpublic:\n");
			builder.append("\t\t").append(simpleName).append("Impl();\n");
			builder.append("\t\tinline jobject get_this() override { return java::lang::Object::get_this(); }\n");
			builder.append("\t\tinline jint M_hashCode_R_int() override { return Object::M_hashCode_R_int();}\n");
			builder.append("\t\tinline jbool M_equals_R_boolean(const jobject &other) override { return Object::M_equals_R_boolean(other); }\n");
			builder.append("\t\tinline jobject M_clone_R_java_lang_Object() override { return Object::M_clone_R_java_lang_Object(); }\n");
			builder.append("\t\tinline jclass M_getClass_R_java_lang_Class() override { return Object::M_getClass_R_java_lang_Class(); }\n");
			builder.append("\t\tinline jstring M_toString_R_java_lang_String() override { return Object::M_toString_R_java_lang_String(); }\n");
			builder.append("\t\tinline jclass M_annotationType_R_java_lang_Class() override { return ").append(simpleName).append("::CLASS; }\n");
			for (BytecodeMethod method: methods) {
				if (method.isStatic())
					continue;
				JavaType type = method.getSignature().getReturnType();
				String fieldName = Utils.sanitizeField(method.getOriginalName(), false);
				builder.append("\t\tinline virtual ").append(type.getCppMemberType(false)).append(" ").append(method.getName()).append("() {return ").append(fieldName).append(";}\n");
				builder.append("\t\t").append(type.getCppMemberType(false)).append(" ").append(fieldName).append(";\n");
			}
			builder.append("\t};\n");
		}

		builder.append("}\n\n");
	}

	public void generateCpp(StringBuilder builder, TranspilerConfig config, HashMap<String, BytecodeClass> classMap) {
		builder.append("#include <").append(Utils.getClassFilename(name)).append(".hpp>\n");
		for (String clazz: dependencies)
			builder.append("#include <").append(Utils.getClassFilename(clazz)).append(".hpp>\n");
		builder.append("#include \"Instructions.hpp\"\n");
		builder.append("#include \"Utils.hpp\"\n\n");

		builder.append("using ").append(qualifiedName).append(";\n\n");

		if (reflective) {
			for (var method : methods)
				builder.append("static jobject reflect_").append(Utils.sanitizeMethod(method.getOriginalName(), method.getSignature(), method.isStatic(), true))
						.append("(const jobject &object, const jarray &args);\n");
			for (var field: fields) {
				builder.append("static jobject reflect_get_").append(field.getName()).append("(const jobject &object);\n");
				builder.append("static void reflect_set_").append(field.getName()).append("(const jobject &object, const jobject &value);\n");
			}
			builder.append("\n");
		}

		builder.append("const std::string ").append(simpleName).append("::NAME = \"").append(name).append("\";\n");

		// Reflection info
		builder.append("static const ClassData classData {\n\t").append(simpleName).append("::NAME, ");
		builder.append(access).append(", ");
		builder.append("\"").append(superName).append("\", \n");

		// Interface info
		builder.append("\t{");
		for (String inter: interfaces)
			builder.append("\"").append(inter).append("\",");
		builder.append("}, \n");

		// Field info
		builder.append("\t{");
		if (reflective) {
			builder.append("\n");
			for (BytecodeField field : fields) {
				builder.append("\t\t{\"").append(field.getOriginalName()).append("\", ").append(field.getAccess()).append(", \"");
				builder.append(field.getType().getRegistryFullName()).append("\", \"").append(field.getSignature() == null ? "" : field.getSignature()).append("\", ");
				builder.append("reflect_get_").append(field.getName()).append(", ");
				builder.append("reflect_set_").append(field.getName());
				builder.append("},\n");
			}
			builder.append("\t");
		}
		builder.append("}, \n");

		// Method info
		builder.append("\t{");
		if (reflective) {
			builder.append("\n");
			for (BytecodeMethod method : methods) {
				if (method.isStaticInitializer())
					continue;
				builder.append("\t\t{\"").append(method.getOriginalName()).append("\", ").append(method.getAccess()).append(", {");
				for (JavaType type : method.getSignature().getParamTypes())
					builder.append("\"").append(type.getRegistryFullName()).append("\", ");
				String returnType = method.getSignature().getReturnType().getRegistryFullName();
				builder.append("}, \"").append(returnType == null ? "" : returnType).append("\", ");
				builder.append("reflect_").append(Utils.sanitizeMethod(method.getOriginalName(), method.getSignature(), method.isStatic(), true));
				builder.append("},\n");
			}
			builder.append("\t");
		}
		builder.append("}, \n\t");

		// New object function pointer
		if (isInstantiatable() && reflective)
			builder.append(simpleName).append("::newObject, ");
		else
			builder.append("nullptr, ");

		builder.append(simpleName).append("::clinit, "); // Static initializer pointer
		builder.append(anonymous).append(", "); // Anonymous
		builder.append(isSynthetic()).append(", "); // Synthetic
		builder.append("false, sizeof(jobject), 0, \"\""); // Array data

		builder.append("\n};\n");

		// Register class
		builder.append("const jclass ").append(simpleName).append("::CLASS = vm::registerClass(&classData);\n");
		if (hasStaticInitializer())
			builder.append("static bool initialized;\n");
		builder.append("\n");

		// Constructor
		builder.append(simpleName).append("::").append(simpleName);
		if (isInterface())
			builder.append("()");
		else
			builder.append("() : java::lang::Object(NAME)");
		builder.append(" {}\n\n");

		// Destructor
		if (!isInterface() && !isEnum() && !isAnnotation()) {
			builder.append(simpleName).append("::~").append(simpleName).append("() {\n");
			builder.append("\tif (finalized) return;\n");
			builder.append("\tfinalized = true;\n");
			builder.append("\ttry {\n");
			builder.append("\t\nM_finalize();\n");
			builder.append("\t} catch (jobject &ignored){}\n");
			builder.append("}\n\n");
		}

		// Clone function
		// Todo: Only add for Cloneable classes
		if (!isAbstract() && !isInterface() && !isEnum() && !isAnnotation()) {
			builder.append("jobject ").append(simpleName).append("::clone() {\n");
			builder.append("\treturn make_shared<").append(simpleName).append(">(*this);\n");
			builder.append("}\n\n");
		}

		boolean generateAnnotations = hasAnnotations() && reflective;
		if (generateAnnotations)
			builder.append("static void generateAnnotations();\n\n");

		// Default static initializer
		if (!hasStaticInitializer()) {
			builder.append("void ").append(simpleName).append("::clinit() {");
			if (!"java/lang/Object".equals(superName))
				builder.append("\n\t").append(qualifiedSuperName).append("::clinit();\n");
			if (generateAnnotations) {
				builder.append("\ttry {\n");
				builder.append("\n\tgenerateAnnotations();\n");
				builder.append("\t} catch(jobject &ex) {\n");
				builder.append("\t\tvm::throwInitializerException(ex);\n");
				builder.append("\t}\n");
			}
			appendDefaultFieldValues(builder);
			builder.append("}\n\n");
		}

		// Static fields
		for (BytecodeField field: fields)
			if (field.isStatic()) {
				if (field.isVolatile() && field.getType().isPrimitive())
					builder.append("volatile ");
				builder.append(field.getType().getCppMemberType(field.isWeak())).append(" ").append(simpleName).append("::").append(field.getName()).append(";\n");
			}
		builder.append("\n");

		// Methods
		for (BytecodeMethod method: methods) {
			if (!method.hasBody() || method.isIntrinsic())
				continue;

			builder.append(method.getSignature().getReturnType().getCppMemberType(false)).append(" ");
			builder.append(simpleName).append("::");
			builder.append(method.getName()).append("(");
			method.getSignature().appendMethodArgs(builder);
			builder.append(") {\n");

			// Static initializer calls
			if (method.isStaticInitializer()) {
				builder.append("\tif (initialized) return;\n");
				builder.append("\tinitialized = true;\n");
				builder.append("\ttry {\n");
				if (!"java/lang/Object".equals(superName))
					builder.append("\t").append(qualifiedSuperName).append("::clinit();\n");
				if (generateAnnotations)
					builder.append("\tgenerateAnnotations();\n");
				appendDefaultFieldValues(builder);
				builder.append("\n");
			} else if (method.isStatic())
				builder.append("\tclinit();\n");

			if (!method.isGenerated()) {
				// Method stack
				if (method.getStackSize() > 0) {
					builder.append("\tDataVariant stack[").append(method.getStackSize() + (config.hasStackCookies() ? 1 : 0)).append("];\n");
					builder.append("\tDataVariant *sp = stack;\n");
				}

				// Callstack observer
				if (config.hasStackTraces()) {
					builder.append("\tCallStackObserver stackObserver {").append(simpleName).append("::NAME, \"").append(method.getOriginalName()).append("\"");
					if (config.hasStackCookies() && method.getStackSize() > 0)
						builder.append(", &stack[").append(method.getStackSize()).append("]");
					builder.append("};\n");
				}

				// Synchronized method observer
				if (method.isSynchronized())
					builder.append("\tSynchronizationObserver synchronizationObserver {").append(method.isStatic() ? "CLASS" : "get_this()").append("};\n");

				// Try-catch jump bypass flags
				if (method.getTryCatchBypasses() > 0)
					builder.append("\tbool bypasses[").append(method.getTryCatchBypasses()).append("]{};\n");
				builder.append("\n");

				// Method locals
				for (int i = 0; i < method.getLocalCount(); i++) {
					if (!method.getKnownLocals().containsKey(i))
						continue;
					TypeVariants local = method.getKnownLocals().get(i);
					if (local == null)
						builder.append("\tDataVariant local").append(i).append(";\n");
					else {
						builder.append("\t").append(local.getArithmeticType()).append(" local").append(i).append(";\n");
						if (local.isWide())
							i++;
					}
				}
				builder.append("\n");

				// Initialize locals from method parameters and `this`
				if (method.getLocalCount() > 0) {
					if (!method.isStatic() && method.getKnownLocals().containsKey(0))
						builder.append("\tlocal0 = this->get_this();\n");
					for (int i = 0, j = 0; i < method.getSignature().getParamTypes().length; i++, j++) {
						TypeVariants paramType = method.getSignature().getParamTypes()[i].getBasicType();
						int local = j + (method.isStatic() ? 0 : 1);
						if (!method.getKnownLocals().containsKey(local)) {
							if (paramType.isWide())
								j++;
							continue;
						}
						if (paramType == TypeVariants.OBJECT)
							builder.append("\tlocal").append(local).append(" = object_cast<java::lang::Object>(param").append(i).append(");\n");
						else
							builder.append("\tlocal").append(local).append(" = param").append(i).append(";\n");
						if (paramType.isWide())
							j++;
					}
					builder.append("\n");
				}
			}

			// Append instructions
			for (Instruction instruction: method.getInstructions())
				instruction.appendUnoptimized(builder);

			if (method.isStaticInitializer())
				builder.append("\t} catch(jobject &ex) {\n")
						.append("\t\tvm::throwInitializerException(ex);\n")
						.append("\t}\n");

			builder.append("}\n\n");
		}

		if (reflective) {
			// Reflection proxy methods
			for (BytecodeMethod method : methods) {
				if (method.isStaticInitializer())
					continue;
				MethodSignature sig = method.getSignature();
				builder.append("jobject reflect_").append(Utils.sanitizeMethod(method.getOriginalName(), method.getSignature(), method.isStatic(), true));
				builder.append("(const jobject &object, const jarray &args) {\n");
				builder.append("\t");
				if (!sig.getReturnType().isVoid()) {
					builder.append("return object_cast<java::lang::Object>(");
					if (sig.getReturnType().isPrimitive())
						builder.append("vm::wrap<java::lang::").append(sig.getReturnType().getBasicType().getWrapper().getSimpleName()).append(">(");
				}
				if (!method.isStatic())
					builder.append("vm::checkedCast<").append(simpleName).append(">(object)->");
				else
					builder.append(simpleName).append("::");
				builder.append(method.getName()).append("(");

				boolean firstArg = true;
				for (int i = 0; i < sig.getParamTypes().length; i++) {
					JavaType type = sig.getParamTypes()[i];
					if (!firstArg)
						builder.append(", ");
					if (type.isPrimitive())
						builder.append("vm::unwrap<").append(type.getCppType()).append(">(");
					else if (type.getArrayDimensions() > 0)
						builder.append("object_cast<vm::Array>(");
					else
						builder.append("object_cast<").append(Utils.getQualifiedClassName(type.getReferenceType())).append(">(");
					builder.append("args->get<jobject>(").append(i).append(")");
					builder.append(")");
					firstArg = false;
				}
				if (sig.getReturnType().isPrimitive() && !sig.getReturnType().isVoid())
					builder.append(")");
				if (!sig.getReturnType().isVoid())
					builder.append(")");
				builder.append(");\n");
				if (sig.getReturnType().isVoid())
					builder.append("\treturn nullptr;\n");
				builder.append("}\n\n");
			}

			// Reflection field accessors
			for (var field: fields) {
				builder.append("jobject reflect_get_").append(field.getName()).append("(const jobject &object) {\n");
				builder.append("\treturn ");
				if (field.getType().isPrimitive())
					builder.append("vm::wrap<java::lang::").append(field.getType().getBasicType().getWrapper().getSimpleName()).append(">(");
				else if (field.getType().getArrayDimensions() > 0)
					builder.append("object_cast<vm::Array>(");
				else
					builder.append("object_cast<java::lang::Object>(");
				if (field.isStatic())
					builder.append(simpleName).append("::").append(field.getName());
				else
					builder.append("object_cast<").append(simpleName).append(">(object)->").append(field.getName());
				if (field.isWeak())
					builder.append(".lock()");
				builder.append(")");
				builder.append(";\n");
				builder.append("}\n\n");

				builder.append("void reflect_set_").append(field.getName()).append("(const jobject &object, const jobject &value) {\n");
				if (field.isStatic())
					builder.append("\t").append(simpleName).append("::").append(field.getName());
				else
					builder.append("\tobject_cast<").append(simpleName).append(">(object)->").append(field.getName());
				builder.append(" = ");
				if (field.getType().isPrimitive())
					builder.append("vm::unwrap<").append(field.getType().getCppType()).append(">(value);\n");
				else if (field.getType().getArrayDimensions() > 0)
					builder.append("object_cast<vm::Array>(value);\n");
				else
					builder.append("object_cast<").append(Utils.getQualifiedClassName(field.getType().getReferenceType())).append(">(value);\n");
				builder.append("}\n\n");
			}
		}

		// Annotation implementation class constructor with default values
		if (isAnnotation()) {
			builder.append(qualifiedName).append("Impl::").append(simpleName).append("Impl() : java::lang::Object(").append(simpleName).append("::NAME) {\n");
			for (AnnotationValue value: defaultAnnotation.getValues())
				value.append(builder, Utils.sanitizeField(value.getName(), false), classMap);
			builder.append("}\n\n");
		}

		// Annotation generator function
		if (generateAnnotations) {
			builder.append("static void generateAnnotations() {\n");
			builder.append("\tauto &clazz = ").append(qualifiedName).append("::CLASS;\n");
			builder.append("\tclazz->M_ensureInitialized();\n");
			builder.append("\tauto fields = clazz->F_fields->getData<jobject>();\n");
			builder.append("\tauto methods = clazz->F_methods->getData<jobject>();\n");
			builder.append("\tauto constructors = clazz->F_constructors->getData<jobject>();\n\n");

			if (!annotations.isEmpty()) {
				builder.append("\t{ // Class\n");
				builder.append("\tauto annotationArray = vm::newArray(java::lang::annotation::Annotation::CLASS, ").append(annotations.size()).append(");\n");
				builder.append("\tclazz->F_annotations = annotationArray;\n");
				builder.append("\tauto annotations = object_cast<vm::Array>(annotationArray)->getData<jobject>();\n");
				for (int i = 0; i < annotations.size(); i++)
					annotations.get(i).append(builder, "annotations[" + i + "]" , classMap);
				builder.append("\t}\n\n");
			}

			for (int i = 0; i < fields.size(); i++) {
				BytecodeField field = fields.get(i);
				if (field.getAnnotations().isEmpty())
					continue;
				builder.append("\t{ // Field ").append(field.getOriginalName()).append("\n");
				builder.append("\tauto annotationArray = vm::newArray(java::lang::annotation::Annotation::CLASS, ").append(field.getAnnotations().size()).append(");\n");
				builder.append("\tobject_cast<java::lang::reflect::Field>(fields[").append(i).append("])->F_annotations = annotationArray;\n");
				builder.append("\tauto annotations = object_cast<vm::Array>(annotationArray)->getData<jobject>();\n");
				for (int j = 0; j < field.getAnnotations().size(); j++)
					field.getAnnotations().get(j).append(builder, "annotations[" + j + "]", classMap);
				builder.append("\t}\n\n");
			}

			for (int i = 0, methodCount = 0, constructorCount = 0; i < methods.size(); i++) {
				BytecodeMethod method = methods.get(i);
				int index = method.isConstructor() ? constructorCount++ : methodCount++;
				if (method.getAnnotations().isEmpty())
					continue;
				builder.append("\t{ // Method ").append(method.getOriginalName()).append("\n");
				builder.append("\tauto annotationArray = vm::newArray(java::lang::annotation::Annotation::CLASS, ").append(method.getAnnotations().size()).append(");\n");
				if (method.isConstructor())
					builder.append("\tobject_cast<java::lang::reflect::Method>(object_cast<java::lang::reflect::Constructor>(constructors[").append(index)
							.append("])->F_method)->F_annotations = annotationArray;\n");
				else
					builder.append("\tobject_cast<java::lang::reflect::Method>(methods[").append(index).append("])->F_annotations = annotationArray;\n");
				builder.append("\tauto annotations = object_cast<vm::Array>(annotationArray)->getData<jobject>();\n");
				for (int j = 0; j < method.getAnnotations().size(); j++)
					method.getAnnotations().get(j).append(builder, "annotations[" + j + "]", classMap);
				builder.append("\t}\n\n");
			}

			builder.append("}\n");
		}
	}

	private void appendDefaultFieldValues(StringBuilder builder) {
		for (BytecodeField field: fields)
			if (field.isStatic() && field.isFinal() && field.getInitialValue() != null)
				builder.append("\t").append(field.getName()).append(" = ").append(Utils.getObjectValue(field.getInitialValue())).append(";\n");
	}

	/**
	 * Get the original class name
	 */
	public String getOriginalName() {
		return originalName;
	}

	/**
	 * Get the original super class name (May be null)
	 */
	public String getOriginalSuperName() {
		return originalSuperName;
	}

	public String getSuperName() {
		return superName;
	}

	public String getQualifiedSuperName() {
		return qualifiedSuperName;
	}

	public String[] getInterfaces () {
		return interfaces;
	}

	public String[] getOriginalInterfaces() {
		return originalInterfaces;
	}

	public int getAccess () {
		return access;
	}

	public boolean isAbstract() {
		return (access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT;
	}

	public boolean isInterface() {
		return (access & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE;
	}

	public boolean isAnnotation() {
		return (access & Opcodes.ACC_ANNOTATION) == Opcodes.ACC_ANNOTATION;
	}

	public boolean isSynthetic() {
		return (access & Opcodes.ACC_SYNTHETIC) == Opcodes.ACC_SYNTHETIC;
	}

	public boolean isFinal() {
		return (access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL;
	}

	public boolean isEnum() {
		return (access & Opcodes.ACC_ENUM) == Opcodes.ACC_ENUM;
	}

	public boolean isInstantiatable() {
		return !isAbstract() && !isInterface() && !isAnnotation();
	}

	public String getSignature () {
		return signature;
	}

	/**
	 * Get the sanitized simple name
	 */
	public String getSimpleName () {
		return simpleName;
	}

	/**
	 * Get the fully qualified C++ name
	 */
	public String getQualifiedName() {
		return qualifiedName;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "class " + name;
	}
}
