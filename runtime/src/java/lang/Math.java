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

import java.math.DoubleConsts;
import java.math.FloatConsts;
import java.util.Random;

/**
 * The class Math contains methods for performing basic numeric operations.
 * Since: JDK1.0, CLDC 1.0
 */
public final class Math {
	/**
	 * The double value that is closer than any other to e, the base of the natural logarithms.
	 * Since: CLDC 1.1 See Also:Constant Field Values
	 */
	public static final double E = 2.718281828459045d;

	/**
	 * The double value that is closer than any other to
	 * , the ratio of the circumference of a circle to its diameter.
	 * Since: CLDC 1.1 See Also:Constant Field Values
	 */
	public static final double PI = 3.141592653589793d;

	/**
	 * Returns the absolute value of a double value. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned. Special cases: If the argument is positive zero or negative zero, the result is positive zero. If the argument is infinite, the result is positive infinity. If the argument is NaN, the result is NaN. In other words, the result is equal to the value of the expression:
	 * Double.longBitsToDouble((Double.doubleToLongBits(a)<<1)>>>1)
	 */
	public static native double abs (double a);

	/**
	 * Returns the absolute value of a float value. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned. Special cases: If the argument is positive zero or negative zero, the result is positive zero. If the argument is infinite, the result is positive infinity. If the argument is NaN, the result is NaN. In other words, the result is equal to the value of the expression:
	 * Float.intBitsToFloat(0x7fffffff & Float.floatToIntBits(a))
	 */
	public static native float abs (float a);

	/**
	 * Returns the absolute value of an int value. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned.
	 * Note that if the argument is equal to the value of Integer.MIN_VALUE, the most negative representable int value, the result is that same value, which is negative.
	 */
	public static native int abs (int a);

	/**
	 * Returns the absolute value of a long value. If the argument is not negative, the argument is returned. If the argument is negative, the negation of the argument is returned.
	 * Note that if the argument is equal to the value of Long.MIN_VALUE, the most negative representable long value, the result is that same value, which is negative.
	 */
	public static native long abs (long a);

	/**
	 * Returns the smallest (closest to negative infinity) double value that is not less than the argument and is equal to a mathematical integer. Special cases: If the argument value is already equal to a mathematical integer, then the result is the same as the argument. If the argument is NaN or an infinity or positive zero or negative zero, then the result is the same as the argument. If the argument value is less than zero but greater than -1.0, then the result is negative zero. Note that the value of Math.ceil(x) is exactly the value of -Math.floor(-x).
	 */
	public static native double ceil (double a);

	/**
	 * Returns the largest (closest to positive infinity) double value that is not greater than the argument and is equal to a mathematical integer. Special cases: If the argument value is already equal to a mathematical integer, then the result is the same as the argument. If the argument is NaN or an infinity or positive zero or negative zero, then the result is the same as the argument.
	 */
	public static native double floor (double a);

	public static int floorDiv(int x, int y) {
		int r = x / y;
		if ((x ^ y) < 0 && r * y != x) {
			--r;
		}

		return r;
	}

	public static long floorDiv(long x, int y) {
		return floorDiv(x, (long)y);
	}

	public static long floorDiv(long x, long y) {
		long r = x / y;
		if ((x ^ y) < 0L && r * y != x) {
			--r;
		}

		return r;
	}

	public static int floorMod(int x, int y) {
		return x - floorDiv(x, y) * y;
	}

	public static int floorMod(long x, int y) {
		return (int)(x - floorDiv(x, y) * (long)y);
	}

	public static long floorMod(long x, long y) {
		return x - floorDiv(x, y) * y;
	}

	/**
	 * Returns the greater of two double values. That is, the result is the argument closer to positive infinity. If the arguments have the same value, the result is that same value. If either value is NaN, then the result is NaN. Unlike the the numerical comparison operators, this method considers negative zero to be strictly smaller than positive zero. If one argument is positive zero and the other negative zero, the result is positive zero.
	 */
	public static native double max (double a, double b);

	public static native double pow (double a, double b);

	/**
	 * Returns the greater of two float values. That is, the result is the argument closer to positive infinity. If the arguments have the same value, the result is that same value. If either value is NaN, then the result is NaN. Unlike the the numerical comparison operators, this method considers negative zero to be strictly smaller than positive zero. If one argument is positive zero and the other negative zero, the result is positive zero.
	 */
	public static native float max (float a, float b);

	/**
	 * Returns the greater of two int values. That is, the result is the argument closer to the value of Integer.MAX_VALUE. If the arguments have the same value, the result is that same value.
	 */
	public static native int max (int a, int b);

	/**
	 * Returns the greater of two long values. That is, the result is the argument closer to the value of Long.MAX_VALUE. If the arguments have the same value, the result is that same value.
	 */
	public static native long max (long a, long b);

	/**
	 * Returns the smaller of two double values. That is, the result is the value closer to negative infinity. If the arguments have the same value, the result is that same value. If either value is NaN, then the result is NaN. Unlike the the numerical comparison operators, this method considers negative zero to be strictly smaller than positive zero. If one argument is positive zero and the other is negative zero, the result is negative zero.
	 */
	public static native double min (double a, double b);

	/**
	 * Returns the smaller of two float values. That is, the result is the value closer to negative infinity. If the arguments have the same value, the result is that same value. If either value is NaN, then the result is NaN. Unlike the the numerical comparison operators, this method considers negative zero to be strictly smaller than positive zero. If one argument is positive zero and the other is negative zero, the result is negative zero.
	 */
	public static native float min (float a, float b);

	/**
	 * Returns the smaller of two int values. That is, the result the argument closer to the value of Integer.MIN_VALUE. If the arguments have the same value, the result is that same value.
	 */
	public static native int min (int a, int b);

	/**
	 * Returns the smaller of two long values. That is, the result is the argument closer to the value of Long.MIN_VALUE. If the arguments have the same value, the result is that same value.
	 */
	public static native long min (long a, long b);

	/**
	 * Returns the trigonometric cosine of an angle. Special case: If the argument is NaN or an infinity, then the result is NaN.
	 */
	public native static double cos (double a);

	/**
	 * Returns the trigonometric sine of an angle. Special cases: If the argument is NaN or an infinity, then the result is NaN. If the argument is positive zero, then the result is positive zero; if the argument is negative zero, then the result is negative zero.
	 */
	public native static double sin (double a);

	/**
	 * Returns the correctly rounded positive square root of a double value. Special cases: If the argument is NaN or less than zero, then the result is NaN. If the argument is positive infinity, then the result is positive infinity. If the argument is positive zero or negative zero, then the result is the same as the argument.
	 */
	public native static double sqrt (double a);

	public native static double cbrt (double a);

	/**
	 * Returns the trigonometric tangent of an angle. Special cases: If the argument is NaN or an infinity, then the result is NaN. If the argument is positive zero, then the result is positive zero; if the argument is negative zero, then the result is negative zero
	 */
	public native static double tan (double a);

	public native static double atan (double d);

	public native static double asin (double a);

	public native static double acos (double a);

	public native static double atan2 (double a, double b);

	public native static double sinh (double a);

	public native static double cosh (double a);

	public native static double tanh (double a);

	public native static double exp (double a);

	public native static double expm1 (double a);

	public native static double log10 (double a);

	public native static double log1p (double a);

	public native static double rint (double a);

	public static native double IEEEremainder(double f1, double f2);

	public native static double log(double a);

	public native static double hypot (double a, double b);

	private static Random randomInst;
	public static double random() {
		if (randomInst == null)
			randomInst = new Random();
		return randomInst.nextDouble();
	}

	/**
	 * Returns the floating-point value adjacent to {@code d} in
	 * the direction of positive infinity.  This method is
	 * semantically equivalent to {@code nextAfter(d,
	 * Double.POSITIVE_INFINITY)}; however, a {@code nextUp}
	 * implementation may run faster than its equivalent
	 * {@code nextAfter} call.
	 *
	 * <p>Special Cases:
	 * <ul>
	 * <li> If the argument is NaN, the result is NaN.
	 *
	 * <li> If the argument is positive infinity, the result is
	 * positive infinity.
	 *
	 * <li> If the argument is zero, the result is
	 * {@link Double#MIN_VALUE}
	 *
	 * </ul>
	 *
	 * @param d starting floating-point value
	 * @return The adjacent floating-point value closer to positive
	 * infinity.
	 * @since 1.6
	 */
	public static double nextUp(double d) {
		if( Double.isNaN(d) || d == Double.POSITIVE_INFINITY)
			return d;
		else {
			d += 0.0d;
			return Double.longBitsToDouble(Double.doubleToRawLongBits(d) +
				((d >= 0.0d)?+1L:-1L));
		}
	}

	/**
	 * Returns the floating-point value adjacent to {@code f} in
	 * the direction of positive infinity.  This method is
	 * semantically equivalent to {@code nextAfter(f,
	 * Float.POSITIVE_INFINITY)}; however, a {@code nextUp}
	 * implementation may run faster than its equivalent
	 * {@code nextAfter} call.
	 *
	 * <p>Special Cases:
	 * <ul>
	 * <li> If the argument is NaN, the result is NaN.
	 *
	 * <li> If the argument is positive infinity, the result is
	 * positive infinity.
	 *
	 * <li> If the argument is zero, the result is
	 * {@link Float#MIN_VALUE}
	 *
	 * </ul>
	 *
	 * @param f starting floating-point value
	 * @return The adjacent floating-point value closer to positive
	 * infinity.
	 * @since 1.6
	 */
	public static float nextUp(float f) {
		if( Float.isNaN(f) || f == FloatConsts.POSITIVE_INFINITY)
			return f;
		else {
			f += 0.0f;
			return Float.intBitsToFloat(Float.floatToRawIntBits(f) +
				((f >= 0.0f)?+1:-1));
		}
	}

	/**
	 * Returns the size of an ulp of the argument.  An ulp, unit in
	 * the last place, of a {@code double} value is the positive
	 * distance between this floating-point value and the {@code
	 * double} value next larger in magnitude.  Note that for non-NaN
	 * <i>x</i>, <code>ulp(-<i>x</i>) == ulp(<i>x</i>)</code>.
	 *
	 * <p>Special Cases:
	 * <ul>
	 * <li> If the argument is NaN, then the result is NaN.
	 * <li> If the argument is positive or negative infinity, then the
	 * result is positive infinity.
	 * <li> If the argument is positive or negative zero, then the result is
	 * {@code Double.MIN_VALUE}.
	 * <li> If the argument is &plusmn;{@code Double.MAX_VALUE}, then
	 * the result is equal to 2<sup>971</sup>.
	 * </ul>
	 *
	 * @param d the floating-point value whose ulp is to be returned
	 * @return the size of an ulp of the argument
	 * @author Joseph D. Darcy
	 * @since 1.5
	 */
	public static double ulp(double d) {
		int exp = getExponent(d);

		switch(exp) {
		case DoubleConsts.MAX_EXPONENT+1:       // NaN or infinity
			return Math.abs(d);

		case DoubleConsts.MIN_EXPONENT-1:       // zero or subnormal
			return Double.MIN_VALUE;

		default:
			assert exp <= DoubleConsts.MAX_EXPONENT && exp >= DoubleConsts.MIN_EXPONENT;

			// ulp(x) is usually 2^(SIGNIFICAND_WIDTH-1)*(2^ilogb(x))
			exp = exp - (DoubleConsts.SIGNIFICAND_WIDTH-1);
			if (exp >= DoubleConsts.MIN_EXPONENT) {
				return powerOfTwoD(exp);
			}
			else {
				// return a subnormal result; left shift integer
				// representation of Double.MIN_VALUE appropriate
				// number of positions
				return Double.longBitsToDouble(1L <<
					(exp - (DoubleConsts.MIN_EXPONENT - (DoubleConsts.SIGNIFICAND_WIDTH-1)) ));
			}
		}
	}

	/**
	 * Returns the size of an ulp of the argument.  An ulp, unit in
	 * the last place, of a {@code float} value is the positive
	 * distance between this floating-point value and the {@code
	 * float} value next larger in magnitude.  Note that for non-NaN
	 * <i>x</i>, <code>ulp(-<i>x</i>) == ulp(<i>x</i>)</code>.
	 *
	 * <p>Special Cases:
	 * <ul>
	 * <li> If the argument is NaN, then the result is NaN.
	 * <li> If the argument is positive or negative infinity, then the
	 * result is positive infinity.
	 * <li> If the argument is positive or negative zero, then the result is
	 * {@code Float.MIN_VALUE}.
	 * <li> If the argument is &plusmn;{@code Float.MAX_VALUE}, then
	 * the result is equal to 2<sup>104</sup>.
	 * </ul>
	 *
	 * @param f the floating-point value whose ulp is to be returned
	 * @return the size of an ulp of the argument
	 * @author Joseph D. Darcy
	 * @since 1.5
	 */
	public static float ulp(float f) {
		int exp = getExponent(f);

		switch(exp) {
		case FloatConsts.MAX_EXPONENT+1:        // NaN or infinity
			return Math.abs(f);

		case FloatConsts.MIN_EXPONENT-1:        // zero or subnormal
			return FloatConsts.MIN_VALUE;

		default:
			assert exp <= FloatConsts.MAX_EXPONENT && exp >= FloatConsts.MIN_EXPONENT;

			// ulp(x) is usually 2^(SIGNIFICAND_WIDTH-1)*(2^ilogb(x))
			exp = exp - (FloatConsts.SIGNIFICAND_WIDTH-1);
			if (exp >= FloatConsts.MIN_EXPONENT) {
				return powerOfTwoF(exp);
			}
			else {
				// return a subnormal result; left shift integer
				// representation of FloatConsts.MIN_VALUE appropriate
				// number of positions
				return Float.intBitsToFloat(1 <<
					(exp - (FloatConsts.MIN_EXPONENT - (FloatConsts.SIGNIFICAND_WIDTH-1)) ));
			}
		}
	}

	/**
	 * Returns a floating-point power of two in the normal range.
	 */
	static double powerOfTwoD(int n) {
		assert(n >= DoubleConsts.MIN_EXPONENT && n <= DoubleConsts.MAX_EXPONENT);
		return Double.longBitsToDouble((((long)n + (long)DoubleConsts.EXP_BIAS) <<
			(DoubleConsts.SIGNIFICAND_WIDTH-1))
			& DoubleConsts.EXP_BIT_MASK);
	}

	/**
	 * Returns a floating-point power of two in the normal range.
	 */
	static float powerOfTwoF(int n) {
		assert(n >= FloatConsts.MIN_EXPONENT && n <= FloatConsts.MAX_EXPONENT);
		return Float.intBitsToFloat(((n + FloatConsts.EXP_BIAS) <<
			(FloatConsts.SIGNIFICAND_WIDTH-1))
			& FloatConsts.EXP_BIT_MASK);
	}

	/**
	 * Returns the unbiased exponent used in the representation of a
	 * {@code float}.  Special cases:
	 *
	 * <ul>
	 * <li>If the argument is NaN or infinite, then the result is
	 * {@link Float#MAX_EXPONENT} + 1.
	 * <li>If the argument is zero or subnormal, then the result is
	 * {@link Float#MIN_EXPONENT} -1.
	 * </ul>
	 * @param f a {@code float} value
	 * @return the unbiased exponent of the argument
	 * @since 1.6
	 */
	public static int getExponent(float f) {
		/*
		 * Bitwise convert f to integer, mask out exponent bits, shift
		 * to the right and then subtract out float's bias adjust to
		 * get true exponent value
		 */
		return ((Float.floatToRawIntBits(f) & FloatConsts.EXP_BIT_MASK) >>
			(FloatConsts.SIGNIFICAND_WIDTH - 1)) - FloatConsts.EXP_BIAS;
	}

	/**
	 * Returns the unbiased exponent used in the representation of a
	 * {@code double}.  Special cases:
	 *
	 * <ul>
	 * <li>If the argument is NaN or infinite, then the result is
	 * {@link Double#MAX_EXPONENT} + 1.
	 * <li>If the argument is zero or subnormal, then the result is
	 * {@link Double#MIN_EXPONENT} -1.
	 * </ul>
	 * @param d a {@code double} value
	 * @return the unbiased exponent of the argument
	 * @since 1.6
	 */
	public static int getExponent(double d) {
		/*
		 * Bitwise convert d to long, mask out exponent bits, shift
		 * to the right and then subtract out double's bias adjust to
		 * get true exponent value.
		 */
		return (int)(((Double.doubleToRawLongBits(d) & DoubleConsts.EXP_BIT_MASK) >>
			(DoubleConsts.SIGNIFICAND_WIDTH - 1)) - DoubleConsts.EXP_BIAS);
	}

	/**
	 * Converts an angle measured in radians to the equivalent angle measured in degrees.
	 */
	public static double toDegrees (double angrad) {
		return angrad * 57.2957795;
	}

	/**
	 * Converts an angle measured in degrees to the equivalent angle measured in radians.
	 */
	public static double toRadians (double angdeg) {
		return angdeg / 57.2957795;
	}

	/**
	 * Returns the first floating-point argument with the sign of the
	 * second floating-point argument.  Note that unlike the {@link
	 * StrictMath#copySign(double, double) StrictMath.copySign}
	 * method, this method does not require NaN {@code sign}
	 * arguments to be treated as positive values; implementations are
	 * permitted to treat some NaN arguments as positive and other NaN
	 * arguments as negative to allow greater performance.
	 *
	 * @param magnitude the parameter providing the magnitude of the result
	 * @param sign      the parameter providing the sign of the result
	 * @return a value with the magnitude of {@code magnitude}
	 * and the sign of {@code sign}.
	 * @since 1.6
	 */
	public static double copySign (double magnitude, double sign) {
		return Double.longBitsToDouble((Double.doubleToRawLongBits(sign) & (DoubleConsts.SIGN_BIT_MASK)) | (Double.doubleToRawLongBits(magnitude) & (DoubleConsts.EXP_BIT_MASK | DoubleConsts.SIGNIF_BIT_MASK)));
	}

	/**
	 * Returns the first floating-point argument with the sign of the
	 * second floating-point argument.  Note that unlike the {@link
	 * StrictMath#copySign(float, float) StrictMath.copySign}
	 * method, this method does not require NaN {@code sign}
	 * arguments to be treated as positive values; implementations are
	 * permitted to treat some NaN arguments as positive and other NaN
	 * arguments as negative to allow greater performance.
	 *
	 * @param magnitude the parameter providing the magnitude of the result
	 * @param sign      the parameter providing the sign of the result
	 * @return a value with the magnitude of {@code magnitude}
	 * and the sign of {@code sign}.
	 * @since 1.6
	 */
	public static float copySign (float magnitude, float sign) {
		return Float.intBitsToFloat((Float.floatToRawIntBits(sign) & (FloatConsts.SIGN_BIT_MASK)) | (Float.floatToRawIntBits(magnitude) & (FloatConsts.EXP_BIT_MASK | FloatConsts.SIGNIF_BIT_MASK)));
	}

	/**
	 * Returns the signum function of the argument; zero if the argument
	 * is zero, 1.0 if the argument is greater than zero, -1.0 if the
	 * argument is less than zero.
	 *
	 * <p>Special Cases:
	 * <ul>
	 * <li> If the argument is NaN, then the result is NaN.
	 * <li> If the argument is positive zero or negative zero, then the
	 *      result is the same as the argument.
	 * </ul>
	 *
	 * @param d the floating-point value whose signum is to be returned
	 * @return the signum function of the argument
	 * @author Joseph D. Darcy
	 * @since 1.5
	 */
	public static double signum (double d) {
		return (d == 0.0 || Double.isNaN(d)) ? d : copySign(1.0, d);
	}

	/**
	 * Returns the signum function of the argument; zero if the argument
	 * is zero, 1.0f if the argument is greater than zero, -1.0f if the
	 * argument is less than zero.
	 *
	 * <p>Special Cases:
	 * <ul>
	 * <li> If the argument is NaN, then the result is NaN.
	 * <li> If the argument is positive zero or negative zero, then the
	 *      result is the same as the argument.
	 * </ul>
	 *
	 * @param f the floating-point value whose signum is to be returned
	 * @return the signum function of the argument
	 * @author Joseph D. Darcy
	 * @since 1.5
	 */
	public static float signum (float f) {
		return (f == 0.0f || Float.isNaN(f)) ? f : copySign(1.0f, f);
	}

	/**
	 * Returns the result of rounding the argument to an integer. The result is
	 * equivalent to {@code (long) Math.floor(d+0.5)}.
	 * <p>
	 * Special cases:
	 * <ul>
	 * <li>{@code round(+0.0) = +0.0}</li>
	 * <li>{@code round(-0.0) = +0.0}</li>
	 * <li>{@code round((anything > Long.MAX_VALUE) = Long.MAX_VALUE}</li>
	 * <li>{@code round((anything < Long.MIN_VALUE) = Long.MIN_VALUE}</li>
	 * <li>{@code round(+infintiy) = Long.MAX_VALUE}</li>
	 * <li>{@code round(-infintiy) = Long.MIN_VALUE}</li>
	 * <li>{@code round(NaN) = +0.0}</li>
	 * </ul>
	 *
	 * @param d the value to be rounded.
	 * @return the closest integer to the argument.
	 */
	public static long round (double d) {
		// check for NaN
		if (d != d) {
			return 0L;
		}
		return (long)floor(d + 0.5d);
	}

	/**
	 * Returns the result of rounding the argument to an integer. The result is
	 * equivalent to {@code (int) Math.floor(f+0.5)}.
	 * <p>
	 * Special cases:
	 * <ul>
	 * <li>{@code round(+0.0) = +0.0}</li>
	 * <li>{@code round(-0.0) = +0.0}</li>
	 * <li>{@code round((anything > Integer.MAX_VALUE) = Integer.MAX_VALUE}</li>
	 * <li>{@code round((anything < Integer.MIN_VALUE) = Integer.MIN_VALUE}</li>
	 * <li>{@code round(+infintiy) = Integer.MAX_VALUE}</li>
	 * <li>{@code round(-infintiy) = Integer.MIN_VALUE}</li>
	 * <li>{@code round(NaN) = +0.0}</li>
	 * </ul>
	 *
	 * @param f the value to be rounded.
	 * @return the closest integer to the argument.
	 */
	public static int round (float f) {
		// check for NaN
		if (f != f) {
			return 0;
		}
		return (int)floor(f + 0.5f);
	}

	public static double nextDown (double d) {
		if (Double.isNaN(d)) {
			return d;
		}
		if (d == Double.NEGATIVE_INFINITY) {
			return d;
		}
		long bits = Double.doubleToLongBits(d);
		boolean negative = (bits & (1L << 63)) != 0;
		if (negative) {
			bits++;
		} else {
			bits--;
		}
		return Double.longBitsToDouble(bits);
	}

	public static double nextAfter (double start, double direction) {
		throw new RuntimeException("Math.nextAfter() not supported.  Use MathUtil.nextAfter()");
	}

	public static float nextAfter (float start, double direction) {
		throw new RuntimeException("Math.nextAfter() not supported.  Use MathUtil.nextAfter()");
	}
}
