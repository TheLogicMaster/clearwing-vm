package com.thelogicmaster.clearwing;

import org.objectweb.asm.Type;

import java.util.*;

public class BytecodeAnnotation extends AnnotationValue {

	private final ArrayList<AnnotationValue> values = new ArrayList<>();
	private final String annotationName;
	private final String qualifiedName;

	public BytecodeAnnotation(String annotationName) {
		this(annotationName, null);
	}

	public BytecodeAnnotation(String annotationName, String name) {
		super(name);
		this.annotationName = annotationName;
		this.qualifiedName = Utils.getQualifiedClassName(annotationName);
	}

	public void addValue (AnnotationValue value) {
		values.add(value);
	}

	public List<AnnotationValue> getValues() {
		return values;
	}

	public String getAnnotationName() {
		return annotationName;
	}

	public String getQualifiedName() {
		return qualifiedName;
	}

	public void collectDependencies(Set<String> dependencies, Map<String, BytecodeClass> classMap) {
		dependencies.add(annotationName);
		dependencies.add("java/lang/annotation/Annotation");
		dependencies.add("java/lang/reflect/Field");
		dependencies.add("java/lang/reflect/Method");
		dependencies.add("java/lang/reflect/Constructor");

		for (AnnotationValue value: values) {
			if (!classMap.containsKey(annotationName)) {
				System.out.println("Warning: Failed to find dependant annotation: " + annotationName);
				continue;
			}
			for (BytecodeMethod method: classMap.get(annotationName).getMethods())
				if (method.getOriginalName().equals(value.name) && method.getSignature().getReturnType().getComponentType() == TypeVariants.OBJECT) {
					dependencies.add(method.getSignature().getReturnType().getRegistryTypeName());
					break;
				}

			if (value instanceof BytecodeAnnotation)
				((BytecodeAnnotation) value).collectDependencies(dependencies, classMap);
			else if (value instanceof AnnotationObjectValue) {
				Object object = ((AnnotationObjectValue)value).getObject();
				if (object instanceof Type)
					dependencies.add(Utils.sanitizeName(((Type)object).getClassName()));
				else if (object instanceof Object[]) {
					for (Object o: (Object[]) object) {
						if (o instanceof Type)
							dependencies.add(Utils.sanitizeName(((Type)o).getClassName()));
						else if (o instanceof AnnotationEnumValue)
							dependencies.add(((AnnotationEnumValue) o).getClazz());
						else if (o instanceof BytecodeAnnotation)
							((BytecodeAnnotation) o).collectDependencies(dependencies, classMap);
					}

					if (object.getClass().getComponentType().isEnum())
						dependencies.add(Utils.sanitizeName(object.getClass().getComponentType().getName()));
					else if (object instanceof Type[])
						for (Type type: (Type[]) object)
							dependencies.add(Utils.sanitizeName(type.getClassName()));
				}
			} else if (value instanceof AnnotationEnumValue)
				dependencies.add(((AnnotationEnumValue)value).getClazz());
		}
	}

	public void append (StringBuilder builder, String target, HashMap<String, BytecodeClass> classMap) {
		builder.append("\t").append(target).append(" = make_shared<").append(qualifiedName).append("Impl>();\n");
		for (AnnotationValue value: values)
			value.append(builder, "object_cast<" + qualifiedName + "Impl" + ">(" + target + ")->" + Utils.sanitizeField(qualifiedName, value.name, false), classMap);
	}
}

abstract class AnnotationValue {

	protected final String name;

	public AnnotationValue(String name) {
		this.name = name;
	}

	public abstract void append (StringBuilder builder, String target, HashMap<String, BytecodeClass> classMap);

	public String getName() {
		return name;
	}
}

class AnnotationObjectValue extends AnnotationValue {

	private final Object object;
	private final BytecodeAnnotation annotation;

	public AnnotationObjectValue(BytecodeAnnotation annotation, String name, Object object) {
		super(name);
		this.annotation = annotation;
		this.object = object;
	}

	@Override
	public void append (StringBuilder builder, String target, HashMap<String, BytecodeClass> classMap) {
		BytecodeClass cls = Objects.requireNonNull(classMap.get(getAnnotation().getAnnotationName()), "Failed to find class: " + getAnnotation().getAnnotationName());
		BytecodeMethod method = null;
		for (BytecodeMethod m: cls.getMethods())
			if (m.getOriginalName().equals(name)) {
				method = m;
				break;
			}
		Objects.requireNonNull(method, "Failed to find annotation method for: " + name);

		if (object.getClass().isArray()) {
			if (object.getClass().getComponentType().isPrimitive()) {
				builder.append('\t').append(target).append(" = vm::newArray(vm::class");
				if (object instanceof boolean[]) {
					boolean[] array = (boolean[])object;
					builder.append("Boolean, ").append(array.length).append(");\n");
					for (int i = 0; i < array.length; i++)
						builder.append("\tobject_cast<vm::Array>(").append(target).append(")->get<jbool>(").append(i).append(") = ").append(array[i]).append(";\n");
				} else if (object instanceof char[]) {
					char[] array = (char[])object;
					builder.append("Char, ").append(array.length).append(");\n");
					for (int i = 0; i < array.length; i++)
						builder.append("\tobject_cast<vm::Array>(").append(target).append(")->get<jchar>(").append(i).append(") = ").append((int)array[i]).append(";\n");
				} else if (object instanceof byte[]) {
					byte[] array = (byte[])object;
					builder.append("Byte, ").append(array.length).append(");\n");
					for (int i = 0; i < array.length; i++)
						builder.append("\tobject_cast<vm::Array>(").append(target).append(")->get<jbyte>(").append(i).append(") = ").append(array[i]).append(";\n");
				} else if (object instanceof short[]) {
					short[] array = (short[])object;
					builder.append("Short, ").append(array.length).append(");\n");
					for (int i = 0; i < array.length; i++)
						builder.append("\tobject_cast<vm::Array>(").append(target).append(")->get<jshort>(").append(i).append(") = ").append(array[i]).append(";\n");
				} else if (object instanceof int[]) {
					int[] array = (int[])object;
					builder.append("Int, ").append(array.length).append(");\n");
					for (int i = 0; i < array.length; i++)
						builder.append("\tobject_cast<vm::Array>(").append(target).append(")->get<jint>(").append(i).append(") = ").append(array[i]).append(";\n");
				} else if (object instanceof long[]) {
					long[] array = (long[])object;
					builder.append("Long, ").append(array.length).append(");\n");
					for (int i = 0; i < array.length; i++)
						builder.append("\tobject_cast<vm::Array>(").append(target).append(")->get<jlong>(").append(i).append(") = ").append(array[i]).append("ll;\n");
				} else if (object instanceof float[]) {
					float[] array = (float[])object;
					builder.append("Float, ").append(array.length).append(");\n");
					for (int i = 0; i < array.length; i++)
						builder.append("\tobject_cast<vm::Array>(").append(target).append(")->get<jfloat>(").append(i).append(") = ").append(array[i]).append("f;\n");
				} else if (object instanceof double[]) {
					double[] array = (double[])object;
					builder.append("Double, ").append(array.length).append(");\n");
					for (int i = 0; i < array.length; i++)
						builder.append("\tobject_cast<vm::Array>(").append(target).append(")->get<jdouble>(").append(i).append(") = ").append(array[i]).append(";\n");
				} else
					throw new TranspilerException("Unsupported array type: " + object);
			} else {
				Object[] array = (Object[])object;
				JavaType type = method.getSignature().getReturnType();
				builder.append('\t').append(target).append(" = vm::newArray(").append(type.generateComponentClassFetch()).append(", ").append(array.length).append(");\n");
				for (int i = 0; i < array.length; i++) {
					if (array[i] instanceof AnnotationEnumValue)
						((AnnotationEnumValue)array[i]).append(builder, "object_cast<vm::Array>(" + target + ")->get<jobject>(" + i + ')', classMap);
					else if (array[i] instanceof BytecodeAnnotation)
						((BytecodeAnnotation)array[i]).append(builder, "object_cast<vm::Array>(" + target + ")->get<jobject>(" + i + ')', classMap);
					else {
						if (array[i] instanceof String || array[i] instanceof Type) {
							builder.append("\tobject_cast<vm::Array>(").append(target).append(")->get<jobject>(").append(i).append(") = ");
							if (array[i] instanceof String)
								builder.append(Utils.encodeStringLiteral((String)array[i]));
							else
								builder.append(Utils.getQualifiedClassName(((Type)array[i]).getClassName())).append("::CLASS");
						} else {
							builder.append("\tobject_cast<vm::Array>(").append(target).append(")->get<").append(type.getComponentType().getCppType()).append(">(").append(i).append(") = ");
							builder.append(Utils.getObjectValue(array[i]));
						}
						builder.append(";\n");
					}
				}
			}
		} else
			builder.append('\t').append(target).append(" = ").append(Utils.getObjectValue(object)).append(";\n");
	}

	public Object getObject() {
		return object;
	}

	public BytecodeAnnotation getAnnotation() {
		return annotation;
	}
}

class AnnotationEnumValue extends AnnotationValue {

	private final String clazz;
	private final String value;

	public AnnotationEnumValue(String name, String clazz, String value) {
		super(name);
		this.clazz = Utils.sanitizeName(clazz);
		this.value = value;
	}

	@Override
	public void append (StringBuilder builder, String target, HashMap<String, BytecodeClass> classMap) {
		String qualifiedName = Utils.getQualifiedClassName(clazz);
		builder.append("\t").append(qualifiedName).append("::clinit();\n");
		builder.append('\t').append(target).append(" = ").append(qualifiedName).append("::").append(Utils.sanitizeField(qualifiedName, value, true)).append(";\n");
	}

	public String getClazz() {
		return clazz;
	}

	public String getValue() {
		return value;
	}
}
