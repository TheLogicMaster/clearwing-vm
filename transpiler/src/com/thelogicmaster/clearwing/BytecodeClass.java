package com.thelogicmaster.clearwing;

import com.thelogicmaster.clearwing.bytecode.Instruction;
import org.objectweb.asm.Opcodes;

import java.util.*;
import java.util.stream.Collectors;

public class BytecodeClass {

	public final static BytecodeClass OBJECT_CLASS = new BytecodeClass("java/lang/Object", null, null, Opcodes.ACC_PUBLIC);
	public final static BytecodeMethod[] OBJECT_METHODS = {
			new BytecodeMethod(OBJECT_CLASS, "<init>", Opcodes.ACC_PUBLIC, "()V", null, null),
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
	private boolean hierarchyError;
	private final ArrayList<BytecodeMethod> vtable = new ArrayList<>();
	private BytecodeClass superClass;
	private BytecodeClass[] interfaceClasses;

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

		if (isAnnotationImpl())
			methods.add(new BytecodeMethod(this, "annotationType", Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "()Ljava/lang/Class;", null, null));
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
		for (BytecodeMethod method : vtable)
			dependencies.add(method.getOwner().name);
	}

	/**
	 * Returns a set of cached dependencies (Does not perform collection)
	 */
	public Set<String> getDependencies() {
		return dependencies;
	}

	/**
	 *
	 */
	public void processHierarchy(HashMap<String, BytecodeClass> classMap) {
		if (hierarchyProcessed)
			return;
		hierarchyProcessed = true;
		if ("java/lang/Object".equals(name))
			return;

		superClass = classMap.get(superName);
		if (superClass == null && !"java/lang/Object".equals(superName)) {
			hierarchyError = true;
			System.err.println("Failed to find parent class: " + superName + " for " + name);
		}

		interfaceClasses = new BytecodeClass[interfaces.length];
		for (int i = 0; i < interfaces.length; i++) {
			BytecodeClass clazz = classMap.get(interfaces[i]);
			if (clazz == null) {
				hierarchyError = true;
				System.err.println("Failed to find interface class: " + interfaces[i] + " for " + name);
			}
			interfaceClasses[i] = clazz;
		}

		if (hierarchyError)
			return;

		// Calculate vtable
		if ("java/lang/Object".equals(superName))
			vtable.addAll(Arrays.stream(OBJECT_METHODS).filter(m -> !m.isConstructor()).collect(Collectors.toList()));
		else {
			superClass.processHierarchy(classMap);
			vtable.addAll(superClass.vtable);
		}
		for (String interfaceName : interfaces) {
			BytecodeClass interfaceClass = classMap.get(interfaceName);
			if (interfaceClass == null)
				throw new TranspilerException("Failed to find interface class: " + interfaceName);
			interfaceClass.processHierarchy(classMap);
			for (BytecodeMethod method : interfaceClass.vtable) {
				if (!vtable.contains(method))
					vtable.add(method);
				else if (vtable.get(vtable.indexOf(method)).isAbstract() && (!method.isAbstract()))
					vtable.set(vtable.indexOf(method), method);
			}
		}
		for (BytecodeMethod method : methods)
			if (!method.isStatic() && !method.isStaticInitializer() && !method.isConstructor()) {
				if (!vtable.contains(method))
					vtable.add(method);
				else if (!method.isAbstract() || method.isAnnotationType())
					vtable.set(vtable.indexOf(method), method);
			}

		for (BytecodeField field: fields)
			field.processHierarchy(classMap);

		for (BytecodeMethod method: methods)
			method.processHierarchy(classMap);

		for (BytecodeAnnotation annotation : annotations)
			annotation.mergeDefaults(classMap);
	}

	public void resolveSymbols() {
		if ("java/lang/Object".equals(name) || hierarchyError)
			return;

		for (BytecodeMethod method : methods)
			method.resolveSymbols();
	}

	public void generateHeader(StringBuilder builder, TranspilerConfig config, HashMap<String, BytecodeClass> classMap) {
		builder.append("#ifndef HEADER_").append(qualifiedName).append("\n");
		builder.append("#define HEADER_").append(qualifiedName).append("\n\n");

		builder.append("#include \"Clearwing.h\"\n");
		builder.append("#include \"").append(Utils.getClassFilename(superName)).append(".h\"\n\n");

		builder.append("#ifdef __cplusplus\n");
		builder.append("extern \"C\" {\n");
		builder.append("#endif\n\n");

		builder.append("typedef struct ").append(qualifiedName).append(" {\n");
		builder.append("\t").append(qualifiedSuperName).append(" parent;\n");

		for (BytecodeField field : fields) {
			if (field.isStatic())
				continue;
			builder.append("\t");
			if (field.isVolatile())
				builder.append("volatile ");
			builder.append(field.getType().getCppMemberType()).append(" ").append(field.getName()).append(";\n");
		}

		if (isAnnotationImpl())
			for (BytecodeMethod method : methods) {
				if (method.isStatic() || method.getOriginalName().equals("annotationType"))
					continue;
				builder.append("\t").append(method.getSignature().getReturnType().getCppMemberType()).append(" F_").append(method.getOriginalName()).append(";\n");
			}

		builder.append("} ").append(qualifiedName).append(";\n\n");

		builder.append("extern Class class_").append(qualifiedName).append(";\n\n");

		// Static fields
		for (BytecodeField field : fields) {
			if (!field.isStatic())
				continue;
			builder.append("extern ");
			if (field.isVolatile())
				builder.append("volatile ");
			builder.append(field.getType().getCppType()).append(" ").append(field.getName()).append(";\n");
		}
		builder.append("\n");

		if (isInterface())
			for (int i = 0; i < methods.size(); i++) {
				BytecodeMethod method = methods.get(i);
				if (method.isStatic())
					continue;
				builder.append("#define INDEX_").append(method.getName().substring(2)).append(" ").append(i).append("\n");
				appendFunctionPointerTypedef(builder, method.getSignature());
			}
		if (!isInterface() || isAnnotationImpl())
			for (int i = 0; i < vtable.size(); i++) {
				BytecodeMethod method = vtable.get(i);
				builder.append("#define VTABLE_").append(Utils.sanitizeMethod(qualifiedName, method.getSignature(), false).substring(2)).append(" ").append(i).append("\n");
				appendFunctionPointerTypedef(builder, method.getSignature());
			}
		builder.append("\n");

		builder.append("void mark_").append(qualifiedName).append("(jobject object, jint mark, jint depth);\n");
		builder.append("void clinit_").append(qualifiedName).append("(jcontext ctx);\n");

		for (BytecodeMethod method : methods)
			if (!method.isStaticInitializer() && (!method.isAbstract() || isAnnotationImpl())) {
				appendMethodDeclaration(builder, method);
				builder.append(";\n");
			}
		builder.append("\n");

		builder.append("#ifdef __cplusplus\n");
		builder.append("}\n");
		builder.append("#endif\n\n");

		builder.append("#endif\n");
	}

	public void generateCpp(StringBuilder builder, TranspilerConfig config, HashMap<String, BytecodeClass> classMap) {
		builder.append("#include \"").append(Utils.getClassFilename(name)).append(".h\"\n");
		for (String clazz : dependencies)
			builder.append("#include \"").append(Utils.getClassFilename(clazz)).append(".h\"\n");
		builder.append("\n");

		builder.append("extern \"C\" {\n\n");

		// Static fields
		for (BytecodeField field : fields) {
			if (!field.isStatic())
				continue;
			if (field.isVolatile())
				builder.append("volatile ");
			builder.append(field.getType().getCppType()).append(" ").append(field.getName()).append(";\n");
		}
		builder.append("\n");

		// Mark function
		builder.append("void mark_").append(qualifiedName).append("(jobject object, jint mark, jint depth) {\n");
		builder.append("\tif (!object) {\n");
		for (BytecodeField field : fields)
			if (field.isStatic() && !field.getType().isPrimitive())
				builder.append("\t\tif (").append(field.getName()).append(")\n")
						.append("\t\t\t((gc_mark_ptr) ((jclass) ((jobject) ").append(field.getName()).append(")->clazz)->markFunction)((jobject) ").append(field.getName()).append(", mark, depth + 1);\n");
		builder.append("\t\treturn;\n");
		builder.append("\t}\n");
		builder.append("\tif (depth > GC_DEPTH_ALWAYS && (object->gcMark < GC_MARK_START || object->gcMark == mark))\n");
		builder.append("\t\treturn;\n");
		builder.append("\tif (depth > MAX_GC_MARK_DEPTH) {\n");
		builder.append("\t\tmarkDeepObject(object);\n");
		builder.append("\t\treturn;\n");
		builder.append("\t}\n");
		if (superClass != null && superClass != OBJECT_CLASS)
			builder.append("\tmark_").append(qualifiedSuperName).append("(object, mark, depth);\n");
		builder.append("\tif (object->gcMark >= GC_MARK_START)\n");
		builder.append("\t\tobject->gcMark = mark;\n");
		builder.append("\tauto self = (").append(qualifiedName).append(" *) object;\n");
		for (BytecodeField field : fields)
			if (!field.isStatic() && !field.getType().isPrimitive())
				builder.append("\tif (self->").append(field.getName()).append(")\n")
						.append("\t\t((gc_mark_ptr) ((jclass) ((jobject) self->").append(field.getName()).append(")->clazz)->markFunction)((jobject) self->").append(field.getName()).append(", mark, depth + 1);\n");
		builder.append("}\n\n");

		// Default static initializer
		if (!hasStaticInitializer()) {
			builder.append("void clinit_").append(qualifiedName).append("(jcontext ctx) {\n");
			appendStaticInitializerCode(builder);
			builder.append("}\n\n");
		}

		// Methods
		for (BytecodeMethod method : methods) {
			if (method.isNative() || method.isAbstract())
				continue;

			appendMethodDeclaration(builder, method);
			builder.append(" {\n");

			if (method.isStaticInitializer())
				appendStaticInitializerCode(builder);
			else if (!method.isStatic())
				builder.append("\tNULL_CHECK(self);\n");

			// Todo: Omit frame for `generated` InvokeDynamic methods, potentially
			builder.append("\tjtype frame[").append(method.getStackSize() + method.getLocalCount()).append("];\n"); // Todo: Does this have to be volatile, or is `sp` sufficient
			builder.append("\tauto stack = &frame[").append(method.getLocalCount()).append("];\n");
			builder.append("\tvolatile jtype * volatile sp = stack;\n"); // Todo: Volatile because of exceptions, possibly remove when no setjmp are used
			builder.append("\tauto frameRef = pushStackFrame(ctx, ").append(method.getStackSize() + method.getLocalCount())
					.append(", frame, \"").append(name).append(":").append(method.getOriginalName()).append("\", ");
			if (!method.isSynchronized())
				builder.append("nullptr");
			else if (method.isStatic())
				builder.append("(jobject) &class_").append(qualifiedName);
			else
				builder.append("self");
			builder.append(");\n\n");

			if (method.isStatic() || method.isConstructor())
				builder.append("\tclinit_").append(qualifiedName).append("(ctx);\n");

			// Set locals from parameters
			if (method.getLocalCount() > 0) {
				if (!method.isStatic())
					builder.append("\tframe[0].o = self;\n");
				for (int i = 0, j = method.isStatic() ? 0 : 1; i < method.getSignature().getParamTypes().length; i++, j++) {
					TypeVariants paramType = method.getSignature().getParamTypes()[i].getBasicType();
					builder.append("\tframe[").append(j).append("].").append(paramType.getStackName()).append(" = param").append(i).append(";\n");
					if (paramType.isWide())
						j++;
				}
			}

			for (Instruction instruction : method.getInstructions())
				instruction.appendUnoptimized(builder);

			builder.append("\n\tpopStackFrame(ctx);\n"); // Todo: This can probably be removed
			builder.append("}\n\n");
		}

		// Annotation methods
		if (isAnnotationImpl()) {
			for (BytecodeMethod method : methods) {
				if (method.isStatic())
					continue;

				appendMethodDeclaration(builder, method);
				builder.append(" {\n");
				if (method.getOriginalName().equals("annotationType"))
					builder.append("\treturn (jobject) &class_").append(qualifiedName).append(";\n");
				else
					builder.append("\treturn (").append(method.getSignature().getReturnType().getCppType()).append(") ((").append(qualifiedName).append(" *) self)->F_").append(method.getOriginalName()).append(";\n");
				builder.append("}\n\n");
			}
		}

		if (hasAnnotations()) {
			builder.append("static void initAnnotations(jcontext ctx) {\n");
			builder.append("\tauto &clazz = class_").append(qualifiedName).append(";\n");
			builder.append("\tauto fields = (java_lang_reflect_Field **) ((jarray) clazz.fields)->data;\n");
			builder.append("\tauto methods = (java_lang_reflect_Method **) ((jarray) clazz.methods)->data;\n");
			builder.append("\tauto constructors = (java_lang_reflect_Constructor **) ((jarray) clazz.constructors)->data;\n");

			if (!annotations.isEmpty()) {
				builder.append("\n\t{ // Class\n");
				builder.append("\t\tauto annotationArray = (jarray) createArray(ctx, &class_java_lang_annotation_Annotation, ").append(annotations.size()).append(");\n");
				builder.append("\t\tclazz.annotations = (jref) annotationArray;\n");
				builder.append("\t\tauto annotations = (jobject *) annotationArray->data;\n");
				for (int i = 0; i < annotations.size(); i++)
					annotations.get(i).append(builder, "annotations[" + i + "]", false, classMap);
				builder.append("\t}\n");
			}

			for (int i = 0; i < fields.size(); i++) {
				BytecodeField field = fields.get(i);
				ArrayList<BytecodeAnnotation> fieldAnnotations = field.getAnnotations();
				if (fieldAnnotations.isEmpty()) {
					builder.append("\n\tfields[").append(i).append("]->F_annotations = (intptr_t) createArray(ctx, &class_java_lang_annotation_Annotation, 0);\n");
					continue;
				}
				builder.append("\n\t{ // Field ").append(field.getOriginalName()).append("\n");
				builder.append("\t\tauto annotationArray = (jarray) createArray(ctx, &class_java_lang_annotation_Annotation, ").append(fieldAnnotations.size()).append(");\n");
				builder.append("\t\tfields[").append(i).append("]->F_annotations = (intptr_t) annotationArray;\n");
				builder.append("\t\tauto annotations = (jobject *) annotationArray->data;\n");
				for (int j = 0; j < fieldAnnotations.size(); j++)
					fieldAnnotations.get(j).append(builder, "annotations[" + j + "]", false, classMap);
				builder.append("\t}\n");
			}

			for (int i = 0, methodCount = 0, constructorCount = 0; i < methods.size(); i++) {
				BytecodeMethod method = methods.get(i);
				ArrayList<BytecodeAnnotation> methodAnnotations = method.getAnnotations();
				int index = method.isConstructor() ? constructorCount++ : methodCount++;
				if (methodAnnotations.isEmpty()) {
					if (method.isConstructor())
						builder.append("\n\t((java_lang_reflect_Method *) ((java_lang_reflect_Constructor *) constructors[").append(index)
								.append("])->F_method)->F_annotations = (intptr_t) createArray(ctx, &class_java_lang_annotation_Annotation, 0);\n");
					else
						builder.append("\n\tmethods[").append(index).append("]->F_annotations = (intptr_t) createArray(ctx, &class_java_lang_annotation_Annotation, 0);\n");
					continue;
				}
				builder.append("\n\t{ // Method ").append(method.getOriginalName()).append("\n");
				builder.append("\t\tauto annotationArray = (jarray) createArray(ctx, &class_java_lang_annotation_Annotation, ").append(methodAnnotations.size()).append(");\n");
				if (method.isConstructor())
					builder.append("((java_lang_reflect_Method *) ((java_lang_reflect_Constructor *) constructors[").append(index).append("])->F_method)->F_annotations = (intptr_t) annotationArray;\n");
				else
					builder.append("\t\tmethods[").append(index).append("]->F_annotations = (intptr_t) annotationArray;\n");
				builder.append("\t\tauto annotations = (jobject *) annotationArray->data;\n");
				for (int j = 0; j < methodAnnotations.size(); j++)
					methodAnnotations.get(j).append(builder, "annotations[" + j + "]", false, classMap);
				builder.append("\t}\n");
			}

			builder.append("}\n\n");
		}

		// Todo: Not needed for non-annotation interfaces
		// Vtable
		builder.append("void *vtable_").append(qualifiedName).append("[] {\n");
		for (BytecodeMethod method : vtable) {
			boolean isNull = method.isAbstract() && !method.getOwner().isAnnotationImpl();
			builder.append("\t(void *) ").append(isNull ? "nullptr" : Utils.sanitizeMethod(method.getOwner().qualifiedName, method.getSignature(), false)).append(",\n");
		}
		builder.append("};\n\n");

		builder.append("static VtableEntry vtableEntries[] {\n");
		for (BytecodeMethod method : vtable)
			builder.append("\t{ \"").append(method.getOriginalName()).append("\", \"").append(method.getDesc()).append("\" },\n");
		builder.append("};\n\n");

		// Interfaces list
		builder.append("static jclass interfaces").append("[] { ");
		for (String interfaceName : interfaces)
			builder.append("&class_").append(Utils.getQualifiedClassName(interfaceName)).append(", ");
		builder.append("};\n\n");

		// Field metadata
		builder.append("static FieldMetadata fields").append("[] {\n");
		for (BytecodeField field : fields) {
			builder.append("\t{ \"").append(field.getOriginalName()).append("\", ").append(field.getType().generateClassFetch());
			if (field.isStatic())
				builder.append(", (intptr_t) &").append(field.getName());
			else
				builder.append(", offsetof(").append(qualifiedName).append(", ").append(field.getName()).append(")");
			builder.append(", \"").append(field.getSignature() == null ? "" : field.getSignature()).append("\", ").append(field.getAccess()).append(" },\n");
		}
		builder.append("};\n\n");

		// Method metadata
		builder.append("static MethodMetadata methods").append("[] {\n");
		for (BytecodeMethod method : methods) {
			builder.append("\t{ \"").append(method.getOriginalName()).append("\"");
			if (method.isStatic() || method.isConstructor())
				builder.append(", (intptr_t) ").append(method.getName());
			else
				builder.append(isInterface() ? ", INDEX_" : ", VTABLE_").append(method.getName().substring(2));
			builder.append(", \"").append(method.getDesc()).append("\", ").append(method.getAccess()).append(" },\n");
		}
		builder.append("};\n\n");

		// Class
		builder.append("Class class_").append(qualifiedName).append("{\n");
		builder.append("\t\t.nativeName = (intptr_t) \"").append(name).append("\",\n");
		builder.append("\t\t.parentClass = (intptr_t) &class_").append(qualifiedSuperName).append(",\n");
		builder.append("\t\t.size = sizeof(").append(qualifiedName).append("),\n");
		builder.append("\t\t.classVtable = (intptr_t) vtable_").append(qualifiedName).append(",\n");
		builder.append("\t\t.staticInitializer = (intptr_t) clinit_").append(qualifiedName).append(",\n");
		builder.append("\t\t.annotationInitializer = (intptr_t) ").append(hasAnnotations() ? "initAnnotations" : "nullptr").append(",\n");
		builder.append("\t\t.markFunction = (intptr_t) mark_").append(qualifiedName).append(",\n");
		builder.append("\t\t.primitive = false,\n");
		builder.append("\t\t.arrayDimensions = 0,\n");
		builder.append("\t\t.componentClass = (intptr_t) nullptr,\n");
		builder.append("\t\t.access = ").append(access).append(",\n");
		builder.append("\t\t.interfaceCount = ").append(interfaces.length).append(",\n");
		builder.append("\t\t.nativeInterfaces = (intptr_t) interfaces").append(",\n");
		builder.append("\t\t.fieldCount = ").append(fields.size()).append(",\n");
		builder.append("\t\t.nativeFields = (intptr_t) fields").append(",\n");
		builder.append("\t\t.methodCount = ").append(methods.size()).append(",\n");
		builder.append("\t\t.nativeMethods = (intptr_t) methods").append(",\n");
		builder.append("\t\t.vtableSize = ").append(vtable.size()).append(",\n");
		builder.append("\t\t.vtableEntries = (intptr_t) vtableEntries").append(",\n");
		builder.append("\t\t.anonymous = ").append(isAnonymous()).append(",\n");
		builder.append("\t\t.synthetic = ").append(isSynthetic()).append(",\n");
		builder.append("};\n");
		builder.append("static bool registered_").append(qualifiedName).append(" = registerClass(&class_").append(qualifiedName).append(");\n\n");

		builder.append("}\n\n");
	}

	private void appendFunctionPointerTypedef(StringBuilder builder, MethodSignature signature) {
		builder.append("typedef ").append(signature.getReturnType().getBasicType().getCppType()).append(" (*func_").append(Utils.sanitizeMethod(qualifiedName, signature, false).substring(2));
		builder.append(")(jcontext ctx, jobject self");
		if (signature.getParamTypes().length > 0) {
			builder.append(", ");
			signature.appendMethodArgs(builder);
		}
		builder.append(");\n");
	}

	private void appendMethodDeclaration(StringBuilder builder, BytecodeMethod method) {
		builder.append(method.getSignature().getReturnType().getCppType()).append(" ").append(method.getName()).append("(jcontext ctx");
		if (!method.isStatic())
			builder.append(", jobject self");
		if (method.getSignature().getParamTypes().length > 0) {
			builder.append(", ");
			method.getSignature().appendMethodArgs(builder);
		}
		builder.append(")");
	}

	private void appendStaticInitializerCode(StringBuilder builder) {
		// Todo: Some form of locking to prevent race condition, not sure about partial initialization while avoiding recursive stack overflows
		builder.append("\tstatic bool initialized;\n");
		builder.append("\tif (initialized) return;\n");
		builder.append("\tinitialized = true;\n");
		if (superClass != null)
			builder.append("\tclinit_").append(superClass.qualifiedName).append("(ctx);\n");
		// Todo: Try-catch to rethrow with initializer exception?
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

	public boolean isAnnotationImpl() {
		return isAnnotation() && !"java/lang/annotation/Annotation".equals(originalName);
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

	public ArrayList<BytecodeMethod> getVtable() {
		return vtable;
	}

	public String getName() {
		return name;
	}

	public BytecodeClass getSuperClass() {
		return superClass;
	}

	public BytecodeClass[] getInterfaceClasses() {
		return interfaceClasses;
	}

	@Override
	public String toString() {
		return "class " + name;
	}
}
