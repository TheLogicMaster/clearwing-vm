/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.thelogicmaster.jnigen;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.Type;

import java.util.ArrayList;

public interface JavaMethodParser {
	ArrayList<JavaSegment> parse (String classFile) throws Exception;

	interface JavaSegment {
		int getStartIndex ();

		int getEndIndex ();
	}

	class JniSection implements JavaSegment {
		private String nativeCode;
		private final int startIndex;
		private final int endIndex;

		public JniSection (String nativeCode, int startIndex, int endIndex) {
			this.nativeCode = nativeCode;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
		}

		public String getNativeCode () {
			return nativeCode;
		}

		public void setNativeCode (String nativeCode) {
			this.nativeCode = nativeCode;
		}

		public int getStartIndex () {
			return startIndex;
		}

		public int getEndIndex () {
			return endIndex;
		}

		@Override
		public String toString () {
			return "JniSection [nativeCode=" + nativeCode + ", startIndex=" + startIndex + ", endIndex=" + endIndex + "]";
		}
	}

	enum ArgumentType {
		Boolean("jboolean"), Byte("jbyte"), Char("jchar"), Short("jshort"), Integer("jint"), Long("jlong"), Float("jfloat"), Double(
			"jdouble"), Buffer("jobject"), ByteBuffer("jobject"), CharBuffer("jobject"), ShortBuffer("jobject"), IntBuffer("jobject"), LongBuffer(
			"jobject"), FloatBuffer("jobject"), DoubleBuffer("jobject"), BooleanArray("jbooleanArray"), ByteArray("jbyteArray"), CharArray(
			"jcharArray"), ShortArray("jshortArray"), IntegerArray("jintArray"), LongArray("jlongArray"), FloatArray("jfloatArray"), DoubleArray(
			"jdoubleArray"), String("jstring"), Class("jclass"), Throwable("jthrowable"), Object("jobject"), ObjectArray("jobjectArray");

		private final String jniType;

		ArgumentType (String jniType) {
			this.jniType = jniType;
		}

		public boolean isPrimitiveArray () {
			return toString().endsWith("Array") && this != ObjectArray;
		}

		public boolean isBuffer () {
			return toString().endsWith("Buffer");
		}

		public boolean isObject () {
			return toString().equals("Object") || this == ObjectArray;
		}

		public boolean isString () {
			return toString().equals("String");
		}

		public boolean isPlainOldDataType () {
			return !isString() && !isPrimitiveArray() && !isBuffer() && !isObject();
		}

		public String getBufferCType () {
			if (!this.isBuffer()) throw new RuntimeException("ArgumentType " + this + " is not a Buffer!");
			if (this == Buffer) return "JAVA_ARRAY_BYTE*";
			if (this == ByteBuffer) return "JAVA_ARRAY_BYTE*";
			if (this == CharBuffer) return "JAVA_ARRAY_CHAR*";
			if (this == ShortBuffer) return "JAVA_ARRAY_SHORT*";
			if (this == IntBuffer) return "JAVA_ARRAY_INT*";
			if (this == LongBuffer) return "JAVA_ARRAY_LONG*";
			if (this == FloatBuffer) return "JAVA_ARRAY_FLOAT*";
			if (this == DoubleBuffer) return "JAVA_ARRAY_DOUBLE*";
			throw new RuntimeException("Unknown Buffer type " + this);
		}

		public String getArrayCType () {
			if (!this.isPrimitiveArray()) throw new RuntimeException("ArgumentType " + this + " is not an Array!");
			if (this == BooleanArray) return "JAVA_ARRAY_BOOLEAN*";
			if (this == ByteArray) return "JAVA_ARRAY_BYTE*";
			if (this == CharArray) return "JAVA_ARRAY_CHAR*";
			if (this == ShortArray) return "JAVA_ARRAY_SHORT*";
			if (this == IntegerArray) return "JAVA_ARRAY_INT*";
			if (this == LongArray) return "JAVA_ARRAY_LONG*";
			if (this == FloatArray) return "JAVA_ARRAY_FLOAT*";
			if (this == DoubleArray) return "JAVA_ARRAY_DOUBLE*";
			throw new RuntimeException("Unknown Array type " + this);
		}

		public String getJniType () {
			return jniType;
		}
	}

	/** @author mzechner */
	class JavaMethod implements JavaSegment {
		private final String className;
		private final String name;
		private final boolean isStatic;
		private boolean isManual;
		private final Type returnType;
		private String nativeCode;
		private final ArrayList<Parameter> arguments;
		private final int startIndex;
		private final int endIndex;
		private final CompilationUnit compilationUnit;
		private final ArrayList<ArgumentType> argumentTypes;

		public JavaMethod (String className, String name, boolean isStatic, Type returnType, String nativeCode, ArrayList<Parameter> arguments, ArrayList<ArgumentType> argumentTypes, int startIndex, int endIndex, CompilationUnit compilationUnit) {
			this.className = className;
			this.name = name;
			this.isStatic = isStatic;
			this.returnType = returnType;
			this.nativeCode = nativeCode;
			this.arguments = arguments;
			this.argumentTypes = argumentTypes;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.compilationUnit = compilationUnit;
		}

		public String getName () {
			return name;
		}

		public CompilationUnit getCompilationUnit () {
			return compilationUnit;
		}

		public ArrayList<ArgumentType> getArgumentTypes () {
			return argumentTypes;
		}

		public boolean isStatic () {
			return isStatic;
		}

		public void setManual (boolean isManual) {
			this.isManual = isManual;
		}

		public boolean isManual () {
			return this.isManual;
		}

		public Type getReturnType () {
			return returnType;
		}

		public String getNativeCode () {
			return nativeCode;
		}

		public void setNativeCode (String nativeCode) {
			this.nativeCode = nativeCode;
		}

		public ArrayList<Parameter> getArguments () {
			return arguments;
		}

		@Override
		public int getStartIndex () {
			return startIndex;
		}

		@Override
		public int getEndIndex () {
			return endIndex;
		}

		public CharSequence getClassName () {
			return className;
		}

		@Override
		public String toString () {
			return "JavaMethod [className=" + className + ", name=" + name + ", isStatic=" + isStatic + ", returnType=" + returnType
				+ ", nativeCode=" + nativeCode + ", arguments=" + arguments
				+ ", startIndex=" + startIndex + ", endIndex=" + endIndex + "]";
		}
	}
}
