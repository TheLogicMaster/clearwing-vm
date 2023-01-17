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

package com.thelogicmaster.clearwing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.Type;

import java.util.ArrayList;

public interface JavaMethodParser {
	ArrayList<JavaSegment> parse (String classFile);

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
		Boolean(""), Byte(""), Char(""), Short(""), Integer(""),
		Long(""), Float(""), Double(""),
		Buffer("jbyte *"), ByteBuffer("jbyte *"), CharBuffer("jchar *"), ShortBuffer("jshort *"), IntBuffer("jint *"),
		LongBuffer("jlong *"), FloatBuffer("jfloat *"), DoubleBuffer("jdouble *"),
		BooleanArray("jbool *"), ByteArray("jbyte *"), CharArray("jchar *"), ShortArray("jshort *"), IntegerArray("jint *"),
		LongArray("jlong *"), FloatArray("jfloat *"), DoubleArray("jdouble *"),
		String(""),
		Class(""),
		Throwable(""),
		Object(""),
		ObjectArray("");

		private final String pointerType;

		ArgumentType (String pointerType) {
			this.pointerType = pointerType;
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

		public String getPointerType () {
			return pointerType;
		}

		public boolean isReference() {
			return isBuffer() || isObject() || isPrimitiveArray() || isString();
		}
	}

	/** @author mzechner */
	class JavaMethod implements JavaSegment {
		private final String className;
		private final String simpleName;
		private final boolean nested;
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
		private final String descriptor;

		public JavaMethod (String className, String simpleName, boolean nested, String name, boolean isStatic, Type returnType, String nativeCode, ArrayList<Parameter> arguments, ArrayList<ArgumentType> argumentTypes, int startIndex, int endIndex, CompilationUnit compilationUnit, String descriptor) {
			this.className = className;
			this.simpleName = simpleName;
			this.nested = nested;
			this.name = name;
			this.isStatic = isStatic;
			this.returnType = returnType;
			this.nativeCode = nativeCode;
			this.arguments = arguments;
			this.argumentTypes = argumentTypes;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.compilationUnit = compilationUnit;
			this.descriptor = descriptor;
		}

		public String getSanitizedName(String outerClass) {
			return Utils.sanitizeName(nested ? outerClass + "$" + simpleName : className);
		}

		public String getName () {
			return name;
		}

		public String getSimpleName() {
			return simpleName;
		}

		public boolean isNested() {
			return nested;
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

		public String getClassName () {
			return className;
		}

		public String getDescriptor() {
			return descriptor;
		}

		@Override
		public String toString () {
			return "JavaMethod [className=" + className + ", name=" + name + ", isStatic=" + isStatic + ", returnType=" + returnType
				+ ", nativeCode=" + nativeCode + ", arguments=" + arguments
				+ ", startIndex=" + startIndex + ", endIndex=" + endIndex + "]";
		}
	}
}
