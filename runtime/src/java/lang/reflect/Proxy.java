/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.lang.reflect;

public class Proxy {
    protected InvocationHandler h;
    private Method[] methods;

    protected Proxy(InvocationHandler h) {
        this.h = h;
    }

    public static native Class<?> getProxyClass(ClassLoader loader, Class<?>... interfaces) throws IllegalArgumentException;
    
    public static Object newProxyInstance(ClassLoader loader, Class<?>[] interfaces, InvocationHandler h) throws IllegalArgumentException {
        try {
            return getProxyClass(loader, interfaces).getConstructor(InvocationHandler.class).newInstance(h);
        } catch (ReflectiveOperationException | IllegalAccessException e) {
            return null;
        }
    }

    public static boolean isProxyClass(Class<?> cl) {
        return Proxy.class.isAssignableFrom(cl);
    }

    public static InvocationHandler getInvocationHandler(Object proxy) throws IllegalArgumentException {
        if (!(proxy instanceof Proxy))
            throw new IllegalArgumentException();
        return ((Proxy) proxy).h;
    }
}
