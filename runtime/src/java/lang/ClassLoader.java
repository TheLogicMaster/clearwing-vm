/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package java.lang;

import jdk.internal.loader.BootLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Enumeration;


public abstract class ClassLoader extends Object {
    private ClassLoader parent;
    private static ClassLoader systemClassLoader = new ClassLoader() {
        
    };
    
    protected ClassLoader() {
        this(null);
    }

    protected ClassLoader(ClassLoader parent) {
        this.parent = parent;
    }

    public ClassLoader getParent() {
        return parent;
    }

    public static ClassLoader getSystemClassLoader() {
        return systemClassLoader;
    }

    public InputStream getResourceAsStream(String name) {
        throw new UnsupportedOperationException();
    }

    public static URL getSystemResource(String name) {
        return getSystemClassLoader().getResource(name);
    }

    public static Enumeration<URL> getSystemResources(String name) throws IOException {
        return getSystemClassLoader().getResources(name);
    }

    public static InputStream getSystemResourceAsStream(String name) {
        throw new UnsupportedOperationException();
    }

    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        throw new UnsupportedOperationException();
    }
    
    public Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public URL getResource(String name) {
        throw new UnsupportedOperationException();
    }

    public Enumeration<URL> getResources(String name) throws IOException {
        throw new UnsupportedOperationException();
    }

    protected final Class<?> findLoadedClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    protected Class<?> findClass(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }

    protected final Class<?> findSystemClass(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }

    protected final Class<?> defineClass(String className, byte[] classRep, int offset, int length) throws ClassFormatError {
        throw new UnsupportedOperationException();
    }

    protected final void resolveClass(Class<?> c) {
        if (c == null) {
            throw new NullPointerException();
        }
    }
}