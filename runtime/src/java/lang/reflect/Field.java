/*
 * Copyright (c) 1996, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A {@code Field} provides information about, and dynamic access to, a
 * single field of a class or an interface.  The reflected field may
 * be a class (static) field or an instance field.
 *
 * <p>A {@code Field} permits widening conversions to occur during a get or
 * set access operation, but throws an {@code IllegalArgumentException} if a
 * narrowing conversion would occur.
 *
 * @see Member
 * @see Class
 * @see Class#getFields()
 * @see Class#getField(String)
 * @see Class#getDeclaredFields()
 * @see Class#getDeclaredField(String)
 *
 * @author Kenneth Russell
 * @author Nakul Saraiya
 */
public final class Field {

    private final long offset;
    private final Class<?> declaringClass;
    private final Class<?> type;
    private String signature;
    private Type genericType;
    private final String name;
    private final int modifiers;
    private Annotation[] annotations = new Annotation[0];

    private Field(long offset, Class<?> declaringClass, Class<?> type, String signature, String name, int modifiers) {
        this.offset = offset;
        this.declaringClass = declaringClass;
        this.type = type;
        this.signature = signature;
        this.name = name;
        this.modifiers = modifiers;
    }

    private Type parseSignature(String signature) throws ClassNotFoundException {
        signature = signature.replace('.', '$');
        int index = 0;
        int dimensions = 0;
        int end = signature.endsWith(";") ? signature.length() - 2 : signature.length() - 1;
        if (signature.charAt(0) == '+' || signature.charAt(0) == '-')
            index++;
        while (signature.charAt(index) == '[') {
            dimensions++;
            index++;
        }
        if (signature.charAt(index) == '*' || signature.charAt(index) == 'T')
            return dimensions > 0 ? new GenericArrayTypeImpl(Object.class) : Object.class;
        int start = ++index;
        int genericStart = -1;
        int depth = 0;
        Type type = null;
        StringBuilder builder = new StringBuilder();
        for (; index <= end; index++) {
            if (signature.charAt(index) == '<') {
                if (depth++ == 0)
                    genericStart = index;
            } else if (signature.charAt(index) == '>') {
                if (--depth == 0) {
                    builder.append(signature, start, genericStart);
                    start = index + 1;
                    ArrayList<String> args = new ArrayList<>();
                    for (int i = genericStart + 1, argDepth = 0, argOffset = i; i < index; i++) {
                        if (signature.charAt(i) == '<')
                            argDepth++;
                        else if (signature.charAt(i) == '>')
                            argDepth--;
                        else if (signature.charAt(i) == ';' && argDepth == 0) {
                            args.add(signature.substring(argOffset, i + 1));
                            argOffset = i + 1;
                        }
                    }
                    String[] argStrings = args.toArray(new String[0]);
                    Type[] argTypes = new Type[argStrings.length];
                    for (int i = 0; i < argStrings.length; i++)
                        argTypes[i] = parseSignature(argStrings[i]);
                    type = new ParameterizedTypeImpl(argTypes, Class.forName(builder.toString()), type);
                }
            }
        }
        if (start <= end) {
            builder.append(signature, start, end + 1);
            type = Class.forName(builder.toString());
        }
        return dimensions > 0 ? new GenericArrayTypeImpl(type) : type;
    }

    public Class<?> getDeclaringClass() {
        return declaringClass;
    }

    public String getName() {
        return name;
    }

    public boolean isAccessible() {
        return true;
    }

    public void setAccessible(boolean accessible) {

    }

    public int getModifiers() {
        return modifiers;
    }

    public boolean isEnumConstant() {
        return (modifiers & Modifier.ENUM) != 0;
    }

    public boolean isStatic() {
        return (modifiers & Modifier.STATIC) != 0;
    }

    public boolean isSynthetic() {
        return Modifier.isSynthetic(modifiers);
    }

    public Class<?> getType() {
        return type;
    }

    public Type getGenericType() {
        if (genericType == null)
            try {
                genericType = signature == null || signature.isEmpty() ? type : parseSignature(signature);
            } catch (ClassNotFoundException e) {
                throw new MalformedParameterizedTypeException("Failed to find class: " + e.getMessage());
            }
        return genericType;
    }

    public native Object get(Object obj) throws IllegalArgumentException, IllegalAccessException;

    public native void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException;

    public byte getByte(Object obj) throws IllegalAccessException {
        return (byte)get(obj);
    }

    public short getShort(Object obj) throws IllegalAccessException {
        return (short)get(obj);
    }

    public char getChar(Object obj) throws IllegalAccessException {
        return (char)get(obj);
    }

    public int getInt(Object obj) throws IllegalAccessException {
        return (int)get(obj);
    }

    public long getLong(Object obj) throws IllegalAccessException {
        return (long)get(obj);
    }

    public float getFloat(Object obj) throws IllegalAccessException {
        return (float)get(obj);
    }

    public double getDouble(Object obj) throws IllegalAccessException {
        return (double)get(obj);
    }

    public boolean getBoolean(Object obj) throws IllegalAccessException {
        return (boolean)get(obj);
    }

    public void setByte(Object obj, byte value) throws IllegalAccessException {
        set(obj, value);
    }

    public void setShort(Object obj, short value) throws IllegalAccessException {
        set(obj, value);
    }

    public void setChar(Object obj, char value) throws IllegalAccessException {
        set(obj, value);
    }

    public void setInt(Object obj, int value) throws IllegalAccessException {
        set(obj, value);
    }

    public void setLong(Object obj, long value) throws IllegalAccessException {
        set(obj, value);
    }

    public void setFloat(Object obj, float value) throws IllegalAccessException {
        set(obj, value);
    }

    public void setDouble(Object obj, double value) throws IllegalAccessException {
        set(obj, value);
    }

    public void setBoolean(Object obj, boolean value) throws IllegalAccessException {
        set(obj, value);
    }

    public Annotation[] getDeclaredAnnotations() {
        return annotations;
    }

    public boolean isAnnotationPresent(Class<?> annotation) {
        for (Annotation a: annotations)
            if (annotation == a.getClass())
                return true;
        return false;
    }

    public Annotation[] getAnnotations() {
        return annotations.clone();
    }

    public <A extends Annotation> A getAnnotation(Class<?> annotationType) {
        for (Annotation annotation: annotations)
            if (annotationType == annotation.annotationType())
                return (A)annotation;
        return null;
    }

    private static class GenericArrayTypeImpl implements GenericArrayType {

        private Type componentType;

        public GenericArrayTypeImpl(Type componentType) {
            this.componentType = componentType;
        }

        @Override
        public Type getGenericComponentType() {
            return componentType;
        }
    }

    private static class ParameterizedTypeImpl implements ParameterizedType {

        private Type[] args;
        private Type raw;
        private Type owner;

        public ParameterizedTypeImpl(Type[] args, Type raw, Type owner) {
            this.args = args;
            this.raw = raw;
            this.owner = owner;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return args;
        }

        @Override
        public Type getRawType() {
            return raw;
        }

        @Override
        public Type getOwnerType() {
            return owner;
        }
    }
}
