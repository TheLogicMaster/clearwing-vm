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

package java.lang.reflect;

import java.lang.annotation.Annotation;

/**
 *
 * @author shannah
 */
public class Constructor<T> {

    private Method method;

    public Constructor (Method method) {
        this.method = method;
    }

    public String getName() {
        return getDeclaringClass().getName();
    }

    public boolean isAccessible() {
        return true;
    }

    public void setAccessible(boolean accessible) {

    }

    public int getModifiers() {
        return method.getModifiers();
    }

    public boolean isVarArgs() {
        return method.isVarArgs();
    }

    public Class<?> getDeclaringClass() {
        return method.getDeclaringClass();
    }

    public Annotation[] getDeclaredAnnotations() {
        return method.getDeclaredAnnotations();
    }

    public boolean isAnnotationPresent(Class<?> annotation) {
        return method.isAnnotationPresent(annotation);
    }

    public Object newInstance(Object ... initargs) throws IllegalAccessException {
        Object o = nativeCreate(getDeclaringClass());
        method.invoke(o, initargs);
        return o;
    }

    private native static Object nativeCreate(Class clazz);

    public Class<?>[] getParameterTypes() {
        return method.getParameterTypes();
    }
}
