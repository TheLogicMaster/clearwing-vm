/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.nio.NativeUtils;
import java.util.ArrayList;

/**
 *
 * @author shannah
 */
public class Method {
    private final long offset;
    private final Class<?> declaringClass;
    private final String desc;
    private final String name;
    private final int modifiers;
    private Annotation[] annotations = new Annotation[0];
    private Class<?>[] parameterTypes;
    private Class<?> returnType;

    private Method (long offset, Class<?> declaringClass, String desc, String name, int modifiers) {
        this.offset = offset;
        this.declaringClass = declaringClass;
        this.desc = desc;
        this.name = name;
        this.modifiers = modifiers;
    }

    private void ensureSignatureInitialized() {
        if (parameterTypes != null && returnType != null)
            return;

        int offset = desc.indexOf(')');
        if (offset == -1)
            throw new ClassFormatError("Invalid method signature");

        try {
            returnType = Class.forName(desc.charAt(offset + 1) == 'L' ? desc.substring(offset + 2, desc.length() - 1) : desc.substring(offset + 1));
        } catch (ClassNotFoundException e) {
            throw new ClassFormatError("Invalid method signature");
        }

        String paramsDesc = desc.substring(1, offset);
        ArrayList<Class<?>> paramTypes = new ArrayList<>();
        int paramOffset = 0;
        while (paramOffset < paramsDesc.length()) {
            int arrayDimensions = 0;
            while (paramsDesc.charAt(paramOffset) == '[') {
                arrayDimensions++;
                paramOffset++;
            }
            Class<?> type;
            switch (paramsDesc.charAt(paramOffset++)) {
                case 'L': {
                    int end = paramsDesc.indexOf(';', paramOffset);
                    if (end == -1)
                        throw new ClassFormatError("Invalid method signature");
                    try {
                        type = Class.forName(paramsDesc.substring(paramOffset, end));
                    } catch (ClassNotFoundException e) {
                        throw new ClassFormatError("Invalid method signature");
                    }
                    paramOffset = end + 1;
                    break;
                }
                case 'B':
                    type = byte.class;
                    break;
                case 'C':
                    type = char.class;
                    break;
                case 'S':
                    type = short.class;
                    break;
                case 'I':
                    type = int.class;
                    break;
                case 'J':
                    type = long.class;
                    break;
                case 'F':
                    type = float.class;
                    break;
                case 'D':
                    type = double.class;
                    break;
                case 'Z':
                    type = boolean.class;
                    break;
                case 'V':
                    type = void.class;
                    break;
                default:
                    throw new ClassFormatError("Invalid method signature");
            }
            paramTypes.add(arrayDimensions == 0 ? type : NativeUtils.getArrayClass(type, arrayDimensions));
        }
        parameterTypes = paramTypes.toArray(new Class<?>[0]);
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
        ensureSignatureInitialized();
        return parameterTypes;
    }

    public Class<?> getReturnType() {
        ensureSignatureInitialized();
        return returnType;
    }
}
