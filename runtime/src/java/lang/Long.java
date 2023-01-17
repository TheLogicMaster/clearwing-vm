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

import java.nio.NativeUtils;

/**
 * The Long class wraps a value of the primitive type long in an object. An object of type Long contains a single field whose type is long.
 * In addition, this class provides several methods for converting a long to a String and a String to a long, as well as other constants and methods useful when dealing with a long.
 * Since: JDK1.0, CLDC 1.0
 */
public final class Long extends Number implements Comparable<Long> {

	public static final Class<Long> TYPE = (Class<Long>)NativeUtils.getPrimitive("L");

	/**
	 * The largest value of type long.
	 * See Also:Constant Field Values
	 */
	public static final long MAX_VALUE = 9223372036854775807l;

	/**
	 * The smallest value of type long.
	 * See Also:Constant Field Values
	 */
	public static final long MIN_VALUE = -9223372036854775807l;

	public static final int SIZE = 64;

	private long value;

	/**
	 * Constructs a newly allocated Long object that represents the primitive long argument.
	 * value - the value to be represented by the Long object.
	 */
	public Long (long value) {
		this.value = value;
	}

	/**
	 * Constructs a newly allocated {@code Long} object that
	 * represents the {@code long} value indicated by the
	 * {@code String} parameter. The string is converted to a
	 * {@code long} value in exactly the manner used by the
	 * {@code parseLong} method for radix 10.
	 *
	 * @param      s   the {@code String} to be converted to a
	 *             {@code Long}.
	 * @throws     NumberFormatException  if the {@code String} does not
	 *             contain a parsable {@code long}.
	 * @see        java.lang.Long#parseLong(java.lang.String, int)
	 */
	public Long(String s) throws NumberFormatException {
		this.value = parseLong(s, 10);
	}

	/**
	 * Returns the value of this Long as a double.
	 */
	public double doubleValue () {
		return (double)value;
	}

	/**
	 * Compares this object against the specified object. The result is true if and only if the argument is not null and is a Long object that contains the same long value as this object.
	 */
	public boolean equals (java.lang.Object obj) {
		return obj != null && obj.getClass() == getClass() && ((Long)obj).value == value;
	}

	/**
	 * Returns the value of this Long as a float.
	 */
	public float floatValue () {
		return (float)value;
	}

	public static int signum(long i) {
		return (int) ((i >> 63) | (-i >>> 63));
	}

	/**
	 * Computes a hashcode for this Long. The result is the exclusive OR of the two halves of the primitive long value represented by this Long object. That is, the hashcode is the value of the expression: (int)(this.longValue()^(this.longValue()>>>32))
	 */
	public int hashCode () {
		return (int)(this.value ^ (this.value >>> 32));
	}

	/**
	 * Returns the value of this Long as a long value.
	 */
	public long longValue () {
		return value;
	}

	/**
	 * Returns the value of this Long as an int value.
	 */
	public int intValue () {
		return (int)value;
	}

	/**
	 * Returns the value of this Long as a byte value.
	 */
	public byte byteValue () {
		return (byte)value;
	}

	public static long rotateLeft(long i, int distance) {
		return i << distance | i >>> -distance;
	}

	public static long rotateRight(long i, int distance) {
		return i >>> distance | i << -distance;
	}

	public static long reverse(long i) {
		i = (i & 6148914691236517205L) << 1 | i >>> 1 & 6148914691236517205L;
		i = (i & 3689348814741910323L) << 2 | i >>> 2 & 3689348814741910323L;
		i = (i & 1085102592571150095L) << 4 | i >>> 4 & 1085102592571150095L;
		return reverseBytes(i);
	}

	public static long reverseBytes (long i) {
		i = (i & 0x00ff00ff00ff00ffL) << 8 | (i >>> 8) & 0x00ff00ff00ff00ffL;
		return (i << 48) | ((i & 0xffff0000L) << 16) | ((i >>> 16) & 0xffff0000L) | (i >>> 48);
	}

	/**
	 * Parses the string argument as a signed decimal long. The characters in the string must all be decimal digits, except that the first character may be an ASCII minus sign '-' (
	 * u002d') to indicate a negative value. The resulting long value is returned, exactly as if the argument and the radix 10 were given as arguments to the
	 * method that takes two arguments.
	 * Note that neither L nor l is permitted to appear at the end of the string as a type indicator, as would be permitted in Java programming language source code.
	 */
	public static long parseLong (java.lang.String s) throws java.lang.NumberFormatException {
		return parseLong(s, 10);
	}

	/**
	 * Parses the string argument as a signed long in the radix specified by the second argument. The characters in the string must all be digits of the specified radix (as determined by whether Character.digit returns a nonnegative value), except that the first character may be an ASCII minus sign '-' ('
	 * u002d' to indicate a negative value. The resulting long value is returned.
	 * Note that neither L nor l is permitted to appear at the end of the string as a type indicator, as would be permitted in Java programming language source code - except that either L or l may appear as a digit for a radix greater than 22.
	 * An exception of type NumberFormatException is thrown if any of the following situations occurs: The first argument is null or is a string of length zero. The radix is either smaller than Character.MIN_RADIX or larger than Character.MAX_RADIX. The first character of the string is not a digit of the specified radix and is not a minus sign '-' ('u002d'). The first character of the string is a minus sign and the string is of length 1. Any character of the string after the first is not a digit of the specified radix. The integer value represented by the string cannot be represented as a value of type long.
	 * Examples:
	 * parseLong("0", 10) returns 0L parseLong("473", 10) returns 473L parseLong("-0", 10) returns 0L parseLong("-FF", 16) returns -255L parseLong("1100110", 2) returns 102L parseLong("99", 8) throws a NumberFormatException parseLong("Hazelnut", 10) throws a NumberFormatException parseLong("Hazelnut", 36) returns 1356099454469L
	 */
	public static long parseLong (String string, int radix) throws NumberFormatException {
		if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
			throw new NumberFormatException("Invalid radix: " + radix);
		}
		if (string == null) {
			throw invalidLong(string);
		}
		int length = string.length(), i = 0;
		if (length == 0) {
			throw invalidLong(string);
		}
		boolean negative = string.charAt(i) == '-';
		if (negative && ++i == length) {
			throw invalidLong(string);
		}

		return parse(string, i, radix, negative);
	}

	private static long parse (String string, int offset, int radix, boolean negative) {
		long max = Long.MIN_VALUE / radix;
		long result = 0, length = string.length();
		while (offset < length) {
			int digit = Character.digit(string.charAt(offset++), radix);
			if (digit == -1) {
				throw invalidLong(string);
			}
			if (max > result) {
				throw invalidLong(string);
			}
			long next = result * radix - digit;
			if (next > result) {
				throw invalidLong(string);
			}
			result = next;
		}
		if (!negative) {
			result = -result;
			if (result < 0) {
				throw invalidLong(string);
			}
		}
		return result;
	}

	private static NumberFormatException invalidLong (String s) {
		throw new NumberFormatException("Invalid long: \"" + s + "\"");
	}

	/**
	 * Returns a String object representing this Long's value. The long integer value represented by this Long object is converted to signed decimal representation and returned as a string, exactly as if the long value were given as an argument to the
	 * method that takes one argument.
	 */
	public java.lang.String toString () {
		return toString(value);
	}

	/**
	 * Returns a new String object representing the specified integer. The argument is converted to signed decimal representation and returned as a string, exactly as if the argument and the radix 10 were given as arguments to the
	 * method that takes two arguments.
	 */
	public static java.lang.String toString (long i) {
		return toString(i, 10);
	}

	/**
	 * Creates a string representation of the first argument in the radix specified by the second argument.
	 * If the radix is smaller than Character.MIN_RADIX or larger than Character.MAX_RADIX, then the radix 10 is used instead.
	 * If the first argument is negative, the first element of the result is the ASCII minus sign '-' ('u002d'. If the first argument is not negative, no sign character appears in the result.
	 * The remaining characters of the result represent the magnitude of the first argument. If the magnitude is zero, it is represented by a single zero character '0' ('u0030'); otherwise, the first character of the representation of the magnitude will not be the zero character. The following ASCII characters are used as digits:
	 * 0123456789abcdefghijklmnopqrstuvwxyz These are '
	 * u0030' through '
	 * u0039' and '
	 * u0061' through '
	 * u007a'. If the radix is N, then the first N of these characters are used as radix-N digits in the order shown. Thus, the digits for hexadecimal (radix 16) are 0123456789abcdef.
	 */
	public native static java.lang.String toString (long i, int radix);

	/**
	 * Returns a string representation of the {@code long}
	 * argument as an unsigned integer in base&nbsp;16.
	 *
	 * <p>The unsigned {@code long} value is the argument plus
	 * 2<sup>64</sup> if the argument is negative; otherwise, it is
	 * equal to the argument.  This value is converted to a string of
	 * ASCII digits in hexadecimal (base&nbsp;16) with no extra
	 * leading {@code 0}s.
	 *
	 * <p>The value of the argument can be recovered from the returned
	 * string {@code s} by calling {@link
	 * Long#parseUnsignedLong(String, int) Long.parseUnsignedLong(s,
	 * 16)}.
	 *
	 * <p>If the unsigned magnitude is zero, it is represented by a
	 * single zero character {@code '0'} ({@code '\u005Cu0030'});
	 * otherwise, the first character of the representation of the
	 * unsigned magnitude will not be the zero character. The
	 * following characters are used as hexadecimal digits:
	 *
	 * <blockquote>
	 *  {@code 0123456789abcdef}
	 * </blockquote>
	 *
	 * These are the characters {@code '\u005Cu0030'} through
	 * {@code '\u005Cu0039'} and  {@code '\u005Cu0061'} through
	 * {@code '\u005Cu0066'}.  If uppercase letters are desired,
	 * the {@link java.lang.String#toUpperCase()} method may be called
	 * on the result:
	 *
	 * <blockquote>
	 *  {@code Long.toHexString(n).toUpperCase()}
	 * </blockquote>
	 *
	 * @param   i   a {@code long} to be converted to a string.
	 * @return  the string representation of the unsigned {@code long}
	 *          value represented by the argument in hexadecimal
	 *          (base&nbsp;16).
	 * @see #parseUnsignedLong(String, int)
	 * @see #toUnsignedString(long, int)
	 * @since   JDK 1.0.2
	 */
	public static String toHexString(long i) {
		return toUnsignedString0(i, 4);
	}

	public static String toBinaryString(long i) {
		return toUnsignedString0(i, 1);
	}

	/**
	 * Format a long (treated as unsigned) into a String.
	 * @param val the value to format
	 * @param shift the log2 of the base to format in (4 for hex, 3 for octal, 1 for binary)
	 */
	static String toUnsignedString0(long val, int shift) {
		// assert shift > 0 && shift <=5 : "Illegal shift value";
		int mag = Long.SIZE - Long.numberOfLeadingZeros(val);
		int chars = Math.max(((mag + (shift - 1)) / shift), 1);
		char[] buf = new char[chars];

		formatUnsignedLong(val, shift, buf, 0, chars);
		return new String(buf);
	}

	/**
	 * Returns the number of one-bits in the two's complement binary
	 * representation of the specified {@code long} value.  This function is
	 * sometimes referred to as the <i>population count</i>.
	 *
	 * @param i the value whose bits are to be counted
	 * @return the number of one-bits in the two's complement binary
	 *     representation of the specified {@code long} value.
	 * @since 1.5
	 */
	public static int bitCount(long i) {
		// HD, Figure 5-14
		i = i - ((i >>> 1) & 0x5555555555555555L);
		i = (i & 0x3333333333333333L) + ((i >>> 2) & 0x3333333333333333L);
		i = (i + (i >>> 4)) & 0x0f0f0f0f0f0f0f0fL;
		i = i + (i >>> 8);
		i = i + (i >>> 16);
		i = i + (i >>> 32);
		return (int)i & 0x7f;
	}

	/**
	 * Format a long (treated as unsigned) into a character buffer.
	 * @param val the unsigned long to format
	 * @param shift the log2 of the base to format in (4 for hex, 3 for octal, 1 for binary)
	 * @param buf the character buffer to write to
	 * @param offset the offset in the destination buffer to start at
	 * @param len the number of characters to write
	 * @return the lowest character location used
	 */
	static int formatUnsignedLong(long val, int shift, char[] buf, int offset, int len) {
		int charPos = len;
		int radix = 1 << shift;
		int mask = radix - 1;
		do {
			buf[offset + --charPos] = Integer.DIGITS[((int) val) & mask];
			val >>>= shift;
		} while (val != 0 && charPos > 0);

		return charPos;
	}

	/**
	 * Returns the object instance of i
	 *
	 * @param i the primitive
	 * @return object instance
	 */
	public static Long valueOf (long i) {
		return new Long(i);
	}

	public static Long valueOf(String s) {
		return null;
	}

	public static int compare (long f1, long f2) {
		if (f1 > f2)
			return 1;
		else if (f1 < f2)
			return -1;
		return 0;
	}

	public int compareTo (Long another) {
		return value < another.value ? -1 : value > another.value ? 1 : 0;
	}

	public static int numberOfLeadingZeros (long i) {
		// HD, Figure 5-6
		if (i == 0)
			return 64;
		int n = 1;
		int x = (int)(i >>> 32);
		if (x == 0) {
			n += 32;
			x = (int)i;
		}
		if (x >>> 16 == 0) {
			n += 16;
			x <<= 16;
		}
		if (x >>> 24 == 0) {
			n += 8;
			x <<= 8;
		}
		if (x >>> 28 == 0) {
			n += 4;
			x <<= 4;
		}
		if (x >>> 30 == 0) {
			n += 2;
			x <<= 2;
		}
		n -= x >>> 31;
		return n;
	}

	public static int numberOfTrailingZeros (long i) {
		int x, y;
		if (i == 0)
			return 64;
		int n = 63;
		y = (int)i;
		if (y != 0) {
			n = n - 32;
			x = y;
		} else
			x = (int)(i >>> 32);
		y = x << 16;
		if (y != 0) {
			n = n - 16;
			x = y;
		}
		y = x << 8;
		if (y != 0) {
			n = n - 8;
			x = y;
		}
		y = x << 4;
		if (y != 0) {
			n = n - 4;
			x = y;
		}
		y = x << 2;
		if (y != 0) {
			n = n - 2;
			x = y;
		}
		return n - ((x << 1) >>> 31);
	}
}
