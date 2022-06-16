package com.thelogicmaster.clearwing.translator;

import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ByteCodeAnnotation {

	private final ArrayList<Value> values = new ArrayList<>();
	private final String annotation;

	public ByteCodeAnnotation (String annotation) {
		this.annotation = annotation;
	}

	public void addValue (Value value) {
		values.add(value);
	}

	public String getAnnotation () {
		return annotation;
	}

	public List<String> getDependencies () {
		HashSet<String> dependencies = new HashSet<>();
		dependencies.add(annotation);
		dependencies.add("java_lang_Class");
		dependencies.add("java_lang_String");
		for (Value value : values) {
			for (ByteCodeClass cls: Parser.getClasses())
				if (cls.getClsName().equals(annotation)) {
					for (BytecodeMethod method : cls.getMethods())
						if (method.getMethodName().equals(value.name) && method.getReturnType().getPrimitiveType() == null)
							dependencies.add(method.getReturnType().getType());
					break;
				}
			if (value instanceof AnnotationValue) {
				dependencies.addAll(((AnnotationValue)value).annotation.getDependencies());
				dependencies.add(((AnnotationValue)value).annotation.annotation);
			} else if (value instanceof ObjectValue) {
				Object object = ((ObjectValue)value).object;
				if (object instanceof Type)
					dependencies.add(Util.sanitizeClassName(((Type)object).getClassName()));
				else if (object instanceof Object[]) {
					for (Object o: (Object[])object) {
						if (o instanceof Type)
							dependencies.add(Util.sanitizeClassName(((Type)o).getClassName()));
						else if (o instanceof EnumValue)
							dependencies.add(((EnumValue)o).clazz);
						else if (o instanceof AnnotationValue) {
							dependencies.addAll(((AnnotationValue)o).annotation.getDependencies());
							dependencies.add(((AnnotationValue)o).annotation.annotation);
						}
					}
					if (object.getClass().getComponentType().isEnum())
						dependencies.add(Util.sanitizeClassName(object.getClass().getComponentType().getName()));
					else if (object instanceof Type[])
						for (Type type: ((Type[])object))
							dependencies.add(Util.sanitizeClassName(type.getClassName()));
				}
			} else if (value instanceof EnumValue)
				dependencies.add(((EnumValue)value).clazz);
		}
		return new ArrayList<>(dependencies);
	}

	public void merge (ByteCodeAnnotation defaults) {
		for (Value value: defaults.values) {
			Value found = null;
			for (Value v: values)
				if (value.name.equals(v.name)) {
					found = v;
					break;
				}
			if (found == null)
				values.add(value);
			else if (found instanceof AnnotationValue)
				((AnnotationValue)found).annotation.merge(((AnnotationValue)value).annotation);
		}
	}

	public void append (StringBuilder buffer, String variable) {
		buffer.append("\t").append(variable).append(" = __NEW_INSTANCE_").append(annotation).append("(threadStateData);\n");
		buffer.append("\t((struct obj__java_lang_annotation_Annotation *)").append(variable).append(")->__isAnnotation = JAVA_TRUE;\n");
		for (Value value : values)
			value.append(buffer, "((struct obj__" + annotation + " *)" + variable + ")->field__" + value.name);
	}

	private static abstract class Value {

		protected final String name;

		public Value (String name) {
			this.name = name;
		}

		public abstract void append (StringBuilder buffer, String variable);
	}

	public static class ObjectValue extends Value {

		private final Object object;
		private ByteCodeAnnotation annotation;

		public ObjectValue (ByteCodeAnnotation annotation, String name, Object object) {
			super(name);
			this.annotation = annotation;
			this.object = object;
		}

		private void appendObject(StringBuilder buffer, Object o) {
			if (o instanceof Boolean)
				buffer.append((Boolean)o ? 1 : 0);
			else if (o instanceof Character)
				buffer.append((int)(Character)o);
			else if (o instanceof Byte)
				buffer.append((int)(Byte)o);
			else if (o instanceof Short)
				buffer.append((int)(Short)o);
			else if (o instanceof Integer)
				buffer.append((int)(Integer)o);
			else if (o instanceof Long)
				buffer.append((long)(Long)o);
			else if (o instanceof Float)
				buffer.append("(JAVA_FLOAT)").append((float)(Float)o);
			else if (o instanceof Double)
				buffer.append((double)(Double)o);
			else if (o instanceof String)
				buffer.append("fromNativeString(threadStateData, \"").append(Parser.encodeString((String)o)).append("\")");
			else if (o instanceof Type)
				buffer.append("(JAVA_OBJECT)&class__").append(Util.sanitizeClassName(((Type)o).getClassName()));
			else
				throw new RuntimeException("Invalid annotation parameter object of type: " + o.getClass());
		}

		@Override
		public void append (StringBuilder buffer, String variable) {
			ByteCodeClass cls = null;
			for (ByteCodeClass c: Parser.getClasses())
				if (c.getClsName().equals(annotation.annotation)) {
					cls = c;
					break;
				}
			if (cls == null)
				throw new RuntimeException("Unable to find class for annotation: " + annotation.annotation);
			BytecodeMethod method = null;
			for (BytecodeMethod m: cls.getMethods())
				if (m.getMethodName().equals(name)) {
					method = m;
					break;
				}
			if (method == null)
				throw new RuntimeException("Unable to find method for annotation: " + annotation.annotation + '.' + name);

			if (object.getClass().isArray()) {
				if (object.getClass().getComponentType().isPrimitive()) {
					buffer.append('\t').append(variable).append(" = __NEW_ARRAY_JAVA_");
					if (object instanceof boolean[]) {
						boolean[] array = (boolean[])object;
						buffer.append("BOOLEAN(threadStateData, ").append(array.length).append(");\n");
						for (int i = 0; i < array.length; i++)
							buffer.append("\t((JAVA_BOOLEAN *)((JAVA_ARRAY)").append(variable).append(")->data)[").append(i).append("] = ").append(array[i] ? 1 : 0).append(";\n");
					} else if (object instanceof char[]) {
						char[] array = (char[])object;
						buffer.append("CHAR(threadStateData, ").append(array.length).append(");\n");
						for (int i = 0; i < array.length; i++)
							buffer.append("\t((JAVA_CHAR *)((JAVA_ARRAY)").append(variable).append(")->data)[").append(i).append("] = ").append((int)array[i]).append(";\n");
					} else if (object instanceof byte[]) {
						byte[] array = (byte[])object;
						buffer.append("BYTE(threadStateData, ").append(array.length).append(");\n");
						for (int i = 0; i < array.length; i++)
							buffer.append("\t((JAVA_BYTE *)((JAVA_ARRAY)").append(variable).append(")->data)[").append(i).append("] = ").append(array[i]).append(";\n");
					} else if (object instanceof short[]) {
						short[] array = (short[])object;
						buffer.append("SHORT(threadStateData, ").append(array.length).append(");\n");
						for (int i = 0; i < array.length; i++)
							buffer.append("\t((JAVA_SHORT *)((JAVA_ARRAY)").append(variable).append(")->data)[").append(i).append("] = ").append(array[i]).append(";\n");
					} else if (object instanceof int[]) {
						int[] array = (int[])object;
						buffer.append("INT(threadStateData, ").append(array.length).append(");\n");
						for (int i = 0; i < array.length; i++)
							buffer.append("\t((JAVA_INT *)((JAVA_ARRAY)").append(variable).append(")->data)[").append(i).append("] = ").append(array[i]).append(";\n");
					} else if (object instanceof long[]) {
						long[] array = (long[])object;
						buffer.append("LONG(threadStateData, ").append(array.length).append(");\n");
						for (int i = 0; i < array.length; i++)
							buffer.append("\t((JAVA_LONG *)((JAVA_ARRAY)").append(variable).append(")->data)[").append(i).append("] = ").append(array[i]).append(";\n");
					} else if (object instanceof float[]) {
						float[] array = (float[])object;
						buffer.append("FLOAT(threadStateData, ").append(array.length).append(");\n");
						for (int i = 0; i < array.length; i++)
							buffer.append("\t((JAVA_FLOAT *)((JAVA_ARRAY)").append(variable).append(")->data)[").append(i).append("] = (JAVA_FLOAT)").append(array[i]).append(";\n");
					} else if (object instanceof double[]) {
						double[] array = (double[])object;
						buffer.append("DOUBLE(threadStateData, ").append(array.length).append(");\n");
						for (int i = 0; i < array.length; i++)
							buffer.append("\t((JAVA_DOUBLE *)((JAVA_ARRAY)").append(variable).append(")->data)[").append(i).append("] = ").append(array[i]).append(";\n");
					}
				} else {
					Object[] array = (Object[])object;
					String type = method.getReturnType().getPrimitiveType() == null ? method.getReturnType().getType() : Util.getCType(method.getReturnType().getPrimitiveType());
					buffer.append('\t').append(variable).append(" = __NEW_ARRAY_").append(type).append("(threadStateData, ").append(array.length).append(");\n");
					for (int i = 0; i < array.length; i++) {
						if (array[i] instanceof EnumValue)
							((EnumValue)array[i]).append(buffer, "((JAVA_OBJECT *)((JAVA_ARRAY)" + variable + ")->data)[" + i + ']');
						else if (array[i] instanceof AnnotationValue)
							((AnnotationValue)array[i]).append(buffer, "((JAVA_OBJECT *)((JAVA_ARRAY)" + variable + ")->data)[" + i + ']');
						else {
							if (array[i] instanceof String || array[i] instanceof Type) {
								buffer.append("\t((JAVA_OBJECT *)((JAVA_ARRAY)").append(variable).append(")->data)[").append(i).append("] = ");
								if (array[i] instanceof String)
									buffer.append("fromNativeString(threadStateData, \"").append(Parser.encodeString((String)array[i])).append("\")");
								else
									buffer.append("(JAVA_OBJECT)&class__").append(Util.sanitizeClassName(((Type)array[i]).getClassName()));
							} else {
								buffer.append("\t((").append(Util.getCType(method.getReturnType().getPrimitiveType())).append(" *)((JAVA_ARRAY)")
									.append(variable).append(")->data)[").append(i).append("] = ");
								appendObject(buffer, array[i]);
							}
							buffer.append(";\n");
						}
					}
				}
			} else {
				buffer.append('\t').append(variable).append(" = ");
				appendObject(buffer, object);
				buffer.append(";\n");
			}
		}
	}

	public static class EnumValue extends Value {

		private final String clazz;
		private final String value;

		public EnumValue (String name, String clazz, String value) {
			super(name);
			this.clazz = clazz;
			this.value = value;
		}

		@Override
		public void append (StringBuilder buffer, String variable) {
			buffer.append('\t').append(variable).append(" = get_static_").append(clazz).append('_').append(value).append("(threadStateData);\n");
		}
	}

	public static class AnnotationValue extends Value {

		private final ByteCodeAnnotation annotation;

		public AnnotationValue (String name, ByteCodeAnnotation annotation) {
			super(name);
			this.annotation = annotation;
		}

		@Override
		public void append (StringBuilder buffer, String variable) {
			annotation.append(buffer, variable);
		}
	}
}
