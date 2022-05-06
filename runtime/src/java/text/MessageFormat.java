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
		return null;
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
		return null;
	}

	public static String format(String pattern, Object ... arguments) {
		return pattern; // Todo: Actually format...
	}

	public String format(Object o) {
		return null;
	}

	public final StringBuffer format(Object arguments, StringBuffer result, FieldPosition pos) {
		return null;
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
