/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.lang.reflect;

import java.lang.annotation.Annotation;

/**
 *
 * @author shannah
 */
public class Method {
    private final long ptr;
    private final Class<?> declaringClass;
    private final Class<?>[] parameterTypes;
    private final Class<?> returnType;
    private final String name;
    private final int modifiers;
    private Annotation[] annotations = new Annotation[0];

    private Method (long ptr, Class<?> declaringClass, Class<?>[] parameterTypes, Class<?> returnType, String name, int modifiers) {
        this.ptr = ptr;
        this.declaringClass = declaringClass;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.name = name;
        this.modifiers = modifiers;
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

    public boolean isVarArgs() {
        return (Modifier.VARARGS & modifiers) != 0;
    }

    public int getModifiers() {
        return modifiers;
    }

    public native Object invoke(Object obj, Object ... args);

    public Annotation[] getDeclaredAnnotations() {
        return annotations;
    }

    public boolean isAnnotationPresent(Class<?> annotation) {
        for (Annotation a: annotations)
            if (annotation == a.getClass())
                return true;
        return false;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public Class<?> getReturnType() {
        return returnType;
    }
}
