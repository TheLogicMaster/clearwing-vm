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

/**
 * Added this for Kotlin
 *
 * @author shannah
 */
public class Array {
	public static Object newInstance (Class<?> componentType, int[] dimensions) {
		if (dimensions.length != 1) {
			throw new IllegalArgumentException("Only 1-dimensional arrays currently supported by Array.newInstance()");
		}
		return newInstanceImpl(componentType, dimensions[0]);

	}

	public static Object newInstance (Class<?> componentType, int length) {
		return newInstanceImpl(componentType, length);
	}

	native static Object newInstanceImpl (Class<?> componentType, int length);

	public native static void set (Object array, int index, Object value);

	public native static Object get (Object array, int index);

	public native static int getLength (Object array);
}
