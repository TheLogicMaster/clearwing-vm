/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
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
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */

package java.lang;

import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Instances of the class Class represent classes and interfaces in a running
 * Java application. Every array also belongs to a class that is reflected as a
 * Class object that is shared by all arrays with the same element type and
 * number of dimensions. Class has no public constructor. Instead Class objects
 * are constructed automatically by the Java Virtual Machine as classes are
 * loaded. The following example uses a Class object to print the class name of
 * an object: Since: JDK1.0, CLDC 1.0
 */
public final class Class<T> implements java.lang.reflect.Type {
    
    
    public ClassLoader getClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }

    /**
     * Returns the Class object associated with the class with the given string
     * name. Given the fully-qualified name for a class or interface, this
     * method attempts to locate, load and link the class. For example, the
     * following code fragment returns the runtime Class descriptor for the
     * class named java.lang.Thread: Classt= Class.forName("java.lang.Thread")
     */
    public static java.lang.Class forName(java.lang.String className) throws java.lang.ClassNotFoundException {
        className = className.replace('$', '.');
        Class c = forNameImpl(className);
        if(c == null) {
            throw new ClassNotFoundException(className);
        }
        return c;
    }

    private native static java.lang.Class forNameImpl(java.lang.String className) throws java.lang.ClassNotFoundException;
    
    /**
     * Returns the fully-qualified name of the entity (class, interface, array
     * class, primitive type, or void) represented by this Class object, as a
     * String. If this Class object represents a class of arrays, then the
     * internal form of the name consists of the name of the element type in
     * Java signature format, preceded by one or more "[" characters
     * representing the depth of array nesting. Thus: (new
     * Object[3]).getClass().getName() returns "[Ljava.lang.Object;" and: (new
     * int[3][4][5][6][7][8][9]).getClass().getName() returns "[[[[[[[I". The
     * encoding of element type names is as follows: B byte C char D double F
     * float I int J long L class or interface S short Z boolean The class or
     * interface name is given in fully qualified form as shown in the example
     * above.
     */
    public native java.lang.String getName();/* {
        if (this.name == null) {
            String name = getNameImpl();
            if (name.endsWith("[]")) {
                String componentType = name.substring(name.indexOf("["));
                int dimension = (name.length() - componentType.length())/2;
                String type = null;
                StringBuilder sb = new StringBuilder();
                while (dimension-- > 0) {
                    sb.append("[");
                }
                if (componentType.indexOf(".") != -1) {
                    sb.append("L").append(componentType).append(";");
                    
                } else if ("int".equals(componentType)) {
                    sb.append("I");
                    
                } else if ("float".equals(componentType)) {
                    sb.append("F");
                } else if ("boolean".equals(componentType)) {
                    sb.append("Z");
                } else if ("byte".equals(componentType)) {
                    sb.append("B");
                } else if ("char".equals(componentType)) {
                    sb.append("C");
                } else if ("short".equals(componentType)) {
                    sb.append("S");
                } else if ("long".equals(componentType)) {
                    sb.append("J");
                } else if ("double".equals(componentType)) {
                    sb.append("D");
                } else {
                    sb.append(name);
                }
                this.name = sb.toString();
            } else {
                this.name = name;
            }
            
        }
        return this.name;
    }
    
    native java.lang.String getNameImpl();
    */

    /**
     * Finds a resource with a given name in the application's JAR file. This
     * method returns null if no resource with this name is found in the
     * application's JAR file. The resource names can be represented in two
     * different formats: absolute or relative. Absolute format:
     * /packagePathName/resourceName Relative format: resourceName In the
     * absolute format, the programmer provides a fully qualified name that
     * includes both the full path and the name of the resource inside the JAR
     * file. In the path names, the character "/" is used as the separator. In
     * the relative format, the programmer provides only the name of the actual
     * resource. Relative names are converted to absolute names by the system by
     * prepending the resource name with the fully qualified package name of
     * class upon which the getResourceAsStream method was called.
     */
    public java.io.InputStream getResourceAsStream(java.lang.String name){
         return null; 
    }

    /**
     * Finds a resource with a given name.  The rules for searching resources
     * associated with a given class are implemented by the defining
     * {@linkplain ClassLoader class loader} of the class.  This method
     * delegates to this object's class loader.  If this object was loaded by
     * the bootstrap class loader, the method delegates to {@link
     * ClassLoader#getSystemResource}.
     *
     * <p> Before delegation, an absolute resource name is constructed from the
     * given resource name using this algorithm:
     *
     * <ul>
     *
     * <li> If the {@code name} begins with a {@code '/'}
     * (<tt>'&#92;u002f'</tt>), then the absolute name of the resource is the
     * portion of the {@code name} following the {@code '/'}.
     *
     * <li> Otherwise, the absolute name is of the following form:
     *
     * <blockquote>
     *   {@code modified_package_name/name}
     * </blockquote>
     *
     * <p> Where the {@code modified_package_name} is the package name of this
     * object with {@code '/'} substituted for {@code '.'}
     * (<tt>'&#92;u002e'</tt>).
     *
     * </ul>
     *
     * @param  name name of the desired resource
     * @return      A  {@link java.net.URL} object or {@code null} if no
     *              resource with this name is found
     * @since  JDK1.1
     */
    public java.net.URL getResource(String name) {
        return null;
    }
    
    /**
     * Determines if this Class object represents an array class.
     */
    public native boolean isArray();

    /**
     * Determines if the class or interface represented by this Class object is
     * either the same as, or is a superclass or superinterface of, the class or
     * interface represented by the specified Class parameter. It returns true
     * if so; otherwise it returns false. If this Class object represents a
     * primitive type, this method returns true if the specified Class parameter
     * is exactly this Class object; otherwise it returns false. Specifically,
     * this method tests whether the type represented by the specified Class
     * parameter can be converted to the type represented by this Class object
     * via an identity conversion or via a widening reference conversion. See
     * The Java Language Specification, sections 5.1.1 and 5.1.4 , for details.
     */
    public native boolean isAssignableFrom(java.lang.Class cls);

    /**
     * Determines if the specified Object is assignment-compatible with the
     * object represented by this Class. This method is the dynamic equivalent
     * of the Java language instanceof operator. The method returns true if the
     * specified Object argument is non-null and can be cast to the reference
     * type represented by this Class object without raising a
     * ClassCastException. It returns false otherwise. Specifically, if this
     * Class object represents a declared class, this method returns true if the
     * specified Object argument is an instance of the represented class (or of
     * any of its subclasses); it returns false otherwise. If this Class object
     * represents an array class, this method returns true if the specified
     * Object argument can be converted to an object of the array class by an
     * identity conversion or by a widening reference conversion; it returns
     * false otherwise. If this Class object represents an interface, this
     * method returns true if the class or any superclass of the specified
     * Object argument implements this interface; it returns false otherwise. If
     * this Class object represents a primitive type, this method returns false.
     */
    public native boolean isInstance(java.lang.Object obj);

    /**
     * Determines if the specified Class object represents an interface type.
     */
    public native boolean isInterface();

    /**
     * Creates a new instance of a class.
     */
    public java.lang.Object newInstance() throws java.lang.InstantiationException, java.lang.IllegalAccessException {
        Object o = newInstanceImpl();
        if(o == null) {
            throw new InstantiationException();
        }
        return o; 
    }

    private native java.lang.Object newInstanceImpl();
    
    /**
     * Converts the object to a string. The string representation is the string
     * "class" or "interface", followed by a space, and then by the fully
     * qualified name of the class in the format returned by getName. If this
     * Class object represents a primitive type, this method returns the name of
     * the primitive type. If this Class object represents void this method
     * returns "void".
     */
    public java.lang.String toString() {
        return getName() + " class";
    }

    public native boolean isAnnotation();

    /**
     * Returns this element's annotation for the specified type if such an
     * annotation is present, else null.
     *
     */
    public <A extends Annotation> A getAnnotation(Class<?> annotationType) {
        for (Annotation annotation: getAnnotations())
            if (annotationType == annotation.annotationType())
                return (A)annotation;
        return null;
    }

    /**
     * Returns all annotations present on this element.
     */
    public Annotation[] getAnnotations() {
        ArrayList<Annotation> annotations = new ArrayList<>(Arrays.asList(getDeclaredAnnotations()));
        if (getSuperclass() != null)
            annotations.addAll(Arrays.asList(getSuperclass().getAnnotations()));
        return annotations.toArray(new Annotation[0]);
    }

    /**
     * Returns all annotations that are directly present on this element.
     */
    public native Annotation[] getDeclaredAnnotations();

    /**
     * Returns true if an annotation for the specified type is present on this
     * element, else false.
     */
    public boolean isAnnotationPresent(Class<?> annotationType) {
        for (Annotation annotation: getAnnotations())
            if (annotationType == annotation.annotationType())
                return true;
        return false;
    }

    public Field getDeclaredField(String name) throws NoSuchFieldException  {
        for (Field field: getDeclaredFields())
            if (field.getName().equals(name))
                return field;
        throw new NoSuchFieldException(name);
    }

    public Field getField(String name) throws NoSuchFieldException {
        for (Field field: getFields())
            if (field.getName().equals(name))
                return field;
        throw new NoSuchFieldException(name);
    }

    /**
     * Replacement for Class.asSubclass(Class).
     *
     * @param superclass another Class which must be a superclass of <i>c</i>
     * @return <i>c</i>
     * @throws java.lang.ClassCastException if <i>c</i> is
     */
    public Class asSubclass(Class superclass) {
        return null;
    }

    /**
     * Replacement for Class.cast(Object). Throws a ClassCastException if
     * <i>obj</i>
     * is not an instance of class <var>c</var>, or a subtype of <var>c</var>.
     *
     * @param object object we want to cast
     * @return The object, or <code>null</code> if the object is
     * <code>null</code>.
     * @throws java.lang.ClassCastException if <var>obj</var> is not
     * <code>null</code> or an instance of <var>c</var>
     */
    public Object cast(Object object) {
        if (object == null) {
            return null;
        }
        if (!isAssignableFrom(object.getClass())) {
            throw new java.lang.ClassCastException("Cannot cast "+object.getClass()+" to "+this);
        }
        return object;
    }

    /**
     * Replacement for Class.isEnum().
     *
     * @return true if the class was declared as an Enum.
     */
    public native boolean isEnum();

    public native Object[] getEnumConstants();

    /**
     * replacement for Class.isAnonymousClass()
     */
    public native boolean isAnonymousClass();    
    
    public native Field[] getDeclaredFields();

    public Field[] getFields() {
        ArrayList<Field> fields = new ArrayList<>();
        for (Field field: getDeclaredFields())
            if ((field.getModifiers() & Modifier.PUBLIC) != 0)
                fields.add(field);
        if (getSuperclass() != null)
            fields.addAll(Arrays.asList(getSuperclass().getFields()));
        return fields.toArray(new Field[0]);
    }

    public Constructor getDeclaredConstructor(Class<?> ... types) throws NoSuchMethodException {
        for (Constructor constructor: getDeclaredConstructors())
            if (Arrays.equals(constructor.getParameterTypes(), types))
                return constructor;
        throw new NoSuchMethodException();
    }

    public Constructor getConstructor(Class<?> ... types) throws NoSuchMethodException {
        for (Constructor constructor: getConstructors())
            if (Arrays.equals(constructor.getParameterTypes(), types))
                return constructor;
        throw new NoSuchMethodException();
    }

    public Constructor[] getDeclaredConstructors() {
        ArrayList<Constructor> constructors = new ArrayList<>();
        for (Method method: getNativeMethods())
            if (method.getName().equals("__INIT__"))
                constructors.add(new Constructor(method));
        return constructors.toArray(new Constructor[0]);
    }

    public Constructor[] getConstructors() {
        ArrayList<Constructor> constructors = new ArrayList<>();
        for (Constructor constructor: getDeclaredConstructors())
            if ((constructor.getModifiers() & Modifier.PUBLIC) != 0)
                constructors.add(constructor);
        if (getSuperclass() != null)
            constructors.addAll(Arrays.asList(getSuperclass().getConstructors()));
        return constructors.toArray(new Constructor[0]);
    }

    public Method getDeclaredMethod(String name, Class<?> ... types) throws NoSuchMethodException {
        for (Method method: getDeclaredMethods())
            if (method.getName().equals(name) && Arrays.equals(method.getParameterTypes(), types))
                return method;
        throw new NoSuchMethodException();
    }

    public Method getMethod(String name, Class<?> ... types) throws NoSuchMethodException {
        for (Method method: getMethods())
            if (method.getName().equals(name) && Arrays.equals(method.getParameterTypes(), types))
                return method;
        throw new NoSuchMethodException();
    }

    public Method[] getDeclaredMethods() {
        ArrayList<Method> methods = new ArrayList<>();
        for (Method method: getNativeMethods())
            if (!method.getName().equals("__INIT__") && !method.getName().equals("__CLINIT__"))
                methods.add(method);
        return methods.toArray(new Method[0]);
    }

    public Method[] getMethods() {
        ArrayList<Method> methods = new ArrayList<>();
        for (Method method: getDeclaredMethods())
            if ((method.getModifiers() & Modifier.PUBLIC) != 0)
                methods.add(method);
        if (getSuperclass() != null)
            methods.addAll(Arrays.asList(getSuperclass().getMethods()));
        return methods.toArray(new Method[0]);
    }

    public native Method[] getNativeMethods();

    public native Class<?> getSuperclass();

    public int getModifiers() {
        return 0;
    }

    public boolean isMemberClass() {
        return false;
    }

    /**
     * replacement for Class.getSimpleName()
     */
    public String getSimpleName() {
        String n = getName();
        return n.substring(n.lastIndexOf('.') + 1);
    }

    /**
     * replacement for Class.isSynthetic()
     */
    public native boolean isSynthetic();

    public String getCanonicalName() {
        return getName();
    }

    @Override
    public native int hashCode();

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    public boolean desiredAssertionStatus() {
        return false;
    }
    
    public native Class getComponentType();

    public Class[] getInterfaces() {
        return new Class[0];
    }

    public java.lang.reflect.Type[] getGenericInterfaces() {
        throw new UnsupportedOperationException("Class.getGenericInterfaces() not supported on this platform");
    }
    
    public native boolean isPrimitive();
    
    public Method getEnclosingMethod() {
        return null;
    }
    
    public Constructor getEnclosingConstructor() {
        return null;
    }
    
    public boolean isLocalClass() {
        return false;
    }

    public Package getPackage() {
        return null;
    }
}
