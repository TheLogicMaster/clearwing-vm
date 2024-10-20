package java.text;

import java.util.Locale;

public class MessageFormat extends Format {

	private String pattern;

	public MessageFormat(String pattern) {
		this(pattern, null);
	}

	public MessageFormat(String pattern, Locale locale) {
		this.pattern = pattern;
	}

	public void setLocale(Locale locale) {

	}

	public Locale getLocale() {
		return null;
	}

	public void applyPattern(String pattern) {
		this.pattern = pattern;
	}

	public String toPattern() {
		return pattern;
	}

	public void setFormatsByArgumentIndex(Format[] newFormats) {

	}

	public void setFormats(Format[] newFormats) {

	}

	public void setFormatByArgumentIndex(int argumentIndex, Format newFormat) {

	}

	public void setFormat(int formatElementIndex, Format newFormat) {

	}

	public Format[] getFormatsByArgumentIndex() {
		return null;
	}

	public Format[] getFormats() {
		return null;
	}

	public final StringBuffer format(Object[] arguments, StringBuffer result, FieldPosition pos) {
		// Todo: Only supports the bare minimum
		int groupStart = -1;
		for (int i = 0; i < pattern.length(); i++) {
			char c = pattern.charAt(i);
			if (c == '{') {
				groupStart = i + 1;
			} else if (groupStart < 0) {
				result.append(c);
			} else if (c == '}') {
				try {
					int index = Integer.parseInt(pattern.substring(groupStart, i));
					result.append(arguments[index]);
				} catch (NumberFormatException ignored) { }
				groupStart = -1;
			}
		}
		return result;
	}

	public static String format(String pattern, Object ... arguments) {
		MessageFormat temp = new MessageFormat(pattern);
		return temp.format(arguments);
	}

	public String format(Object obj) {
		return this.format(obj, new StringBuffer(), new FieldPosition(0)).toString();
	}

	public final StringBuffer format(Object arguments, StringBuffer result, FieldPosition pos) {
		return format((Object[]) arguments, result, pos);
	}

	public AttributedCharacterIterator formatToCharacterIterator(Object arguments) {
		return null;
	}

	public Object[] parse(String source, ParsePosition pos) {
		return null;
	}

	public Object[] parse(String source) throws ParseException {
		return null;
	}

	public Object parseObject(String source) {
		return null;
	}

	public Object parseObject(String source, ParsePosition pos) {
		return null;
	}
}
