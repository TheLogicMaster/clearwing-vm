/*
 * Copyright (c) 2000, 2007, Oracle and/or its affiliates. All rights reserved.
 *
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
 *
 */

package java.nio;

public final class ByteOrder {

	private String name;

	private ByteOrder(String name) {
		this.name = name;
	}

	/**
	 * Constant denoting big-endian byte order.  In this order, the bytes of a
	 * multibyte value are ordered from most significant to least significant.
	 */
	public static final ByteOrder BIG_ENDIAN
		= new ByteOrder("BIG_ENDIAN");

	/**
	 * Constant denoting little-endian byte order.  In this order, the bytes of
	 * a multibyte value are ordered from least significant to most
	 * significant.
	 */
	public static final ByteOrder LITTLE_ENDIAN
		= new ByteOrder("LITTLE_ENDIAN");

	/**
	 * Retrieves the native byte order of the underlying platform.
	 *
	 * <p> This method is defined so that performance-sensitive Java code can
	 * allocate direct buffers with the same byte order as the hardware.
	 * Native code libraries are often more efficient when such buffers are
	 * used.  </p>
	 *
	 * @return  The native byte order of the hardware upon which this Java
	 *          virtual machine is running
	 */
	public static ByteOrder nativeOrder() {
		return isLittleEndian() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
	}

	private static native boolean isLittleEndian();

	/**
	 * Constructs a string describing this object.
	 *
	 * <p> This method returns the string <tt>"BIG_ENDIAN"</tt> for {@link
	 * #BIG_ENDIAN} and <tt>"LITTLE_ENDIAN"</tt> for {@link #LITTLE_ENDIAN}.
	 * </p>
	 *
	 * @return  The specified string
	 */
	public String toString() {
		return name;
	}

}
