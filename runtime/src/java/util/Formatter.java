// Adapted from: https://github.com/tommyettinger/RegExodus/blob/b5d665c3154a7ad7b9b4f14e07f86ca90dded1bb/src/test/java/regexodus/Doormat.java

package java.util;

import regexodus.MatchResult;
import regexodus.Pattern;
import regexodus.Replacer;
import regexodus.Substitution;
import regexodus.TextBuffer;

public class Formatter {

	private static final char[] DIGITS_LOWER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
	private static final char[] DIGITS_UPPER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

	private static final Pattern pattern = Pattern.compile(
		"%(?:({=esc}%)|({=str}s)" +
			"|({=int}({=flags}[ 0-]+)?({=width}[1-9][0-9]*)?d)" +
			"|({=hex}({=flags}[0-]+)?({=width}[1-9][0-9]*)?({=case}[xX]))" +
			"|({=float}({=flags}[ 0-]+)?({=width}[1-9][0-9]*)?({=pre}\\.({=precise}[0-9]+))?f)" +
			")");

	private String formatted;

	public Formatter() {

	}

	public Formatter format(String format, Object ... args) {
		Doormat doormat = new Doormat(args);
		Replacer replacer = new Replacer(pattern, doormat);
		formatted = replacer.replace(format);
		return this;
	}

	@Override
	public String toString () {
		return formatted;
	}

	public static class Doormat implements Substitution {

		private Doormat (Object... args) {
			arguments = args;
			index = 0;
		}

		private final Object[] arguments;
		private int index = 0;

		@Override
		public void appendSubstitution (MatchResult match, TextBuffer dest) {
			if (match.isCaptured("str")) {
				dest.append(arguments[index++].toString());
			} else if (match.isCaptured("int")) {
				long n = ((Number)arguments[index++]).longValue();
				StringBuilder isb = new StringBuilder(Long.toString(n));
				int width = -1;
				if (match.isCaptured("width"))
					width = Integer.parseInt(match.group("width"));
				char pad = ' ';
				if (match.isCaptured("flags")) {
					if (match.group("flags").contains("0"))
						pad = '0';
					if (n >= 0L && match.group("flags").contains(" "))
						isb.insert(0, ' ');

					if (match.group("flags").contains("-"))
						while (isb.length() < width)
							isb.append(pad);
					else
						while (isb.length() < width)
							isb.insert(0, pad);
				} else {
					while (isb.length() < width)
						isb.insert(0, pad);
				}
				dest.append(isb.toString());
			} else if (match.isCaptured("hex")) {
				Object arg = arguments[index++];
				long n = ((Number)arg).longValue();
				int width = -1;
				if (match.isCaptured("width"))
					width = Integer.parseInt(match.group("width"));
				char[] digits = match.group("case").equals("x") ? DIGITS_LOWER : DIGITS_UPPER;
				StringBuilder isb;
				if (arg instanceof Integer)
					isb = new StringBuilder(intToHexString((int)n, Math.min(width, 8), digits));
				else if (arg instanceof Short)
					isb = new StringBuilder(shortToHexString((short)n, Math.min(width, 4), digits));
				else if (arg instanceof Byte)
					isb = new StringBuilder(byteToHexString((byte)n, Math.min(width, 2), digits));
				else
					isb = new StringBuilder(longToHexString(n, Math.min(width, 16), digits));
				char pad = ' ';
				if (match.isCaptured("flags")) {
					if (match.group("flags").contains("0"))
						pad = '0';

					if (match.group("flags").contains("-"))
						while (isb.length() < width)
							isb.append(' ');
					else
						while (isb.length() < width)
							isb.insert(0, pad);
				} else {
					while (isb.length() < width)
						isb.insert(0, pad);
				}
				dest.append(isb.toString());
			} else if (match.isCaptured("float")) {
				double f = ((Number)arguments[index++]).doubleValue();
				String is;
				if (match.isCaptured("pre")) {
					int p = Integer.parseInt(match.group("precise"));
					f += Math.copySign(0.5 * Math.pow(10.0, -p), f);
					is = Double.toString(f);
					int dot = is.lastIndexOf('.') + 1;
					String post = is.substring(dot, Math.min(is.length(), dot + p));
					is = is.substring(0, dot) + post;
				} else
					is = Double.toString(f);
				StringBuilder isb = new StringBuilder(is);
				int precision = -1;
				if (match.isCaptured("width"))
					precision = Integer.parseInt(match.group("width"));
				char pad = ' ';
				if (match.isCaptured("flags")) {
					if (match.group("flags").contains("0"))
						pad = '0';
					// ugh, this copySign is needed because of -0.0
					if (Math.copySign(1.0, f) == 1.0 && match.group("flags").contains(" "))
						isb.insert(0, ' ');

					if (match.group("flags").contains("-"))
						while (isb.length() < precision)
							isb.append(pad);
					else
						while (isb.length() < precision)
							isb.insert(0, pad);
				} else {
					while (isb.length() < precision)
						isb.insert(0, pad);
				}
				dest.append(isb.toString());
			} else if (match.isCaptured("esc"))
				dest.append('%');
		}

		private static final int bufLen = 16;  // Max number of hex digits in a long
		private final char[] workBuffer = new char[bufLen];

		public String byteToHexString (byte i, int minWidth, char[] digits) {
			final int bufLen = 2;
			int cursor = bufLen;

			do {
				workBuffer[--cursor] = digits[i & 0xf];
			} while ((i = (byte) ((i & 0xFF) >>> 4)) != 0 || (bufLen - cursor < minWidth));
			return String.valueOf(workBuffer, cursor, bufLen - cursor);
		}

		public String shortToHexString (short i, int minWidth, char[] digits) {
			final int bufLen = 4;
			int cursor = bufLen;

			do {
				workBuffer[--cursor] = digits[i & 0xf];
			} while ((i = (short) ((i & 0xFFFF) >>> 4)) != 0 || (bufLen - cursor < minWidth));
			return String.valueOf(workBuffer, cursor, bufLen - cursor);
		}

		public String intToHexString (int i, int minWidth, char[] digits) {
			final int bufLen = 8;
			int cursor = bufLen;

			do {
				workBuffer[--cursor] = digits[i & 0xf];
			} while ((i >>>= 4) != 0 || (bufLen - cursor < minWidth));
			return String.valueOf(workBuffer, cursor, bufLen - cursor);
		}

		public String longToHexString (long i, int minWidth, char[] digits) {
			final int bufLen = 16;
			int cursor = bufLen;

			do {
				workBuffer[--cursor] = digits[(int)(i & 0xfL)];
			} while ((i >>>= 4) != 0 || (bufLen - cursor < minWidth));
			return String.valueOf(workBuffer, cursor, bufLen - cursor);
		}
	}
}
