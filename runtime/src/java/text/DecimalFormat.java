package java.text;

/**
 * Not properly implemented
 */
public class DecimalFormat extends NumberFormat {

	private String pattern;

	public DecimalFormat() {

	}

	public DecimalFormat (String pattern) {
		this.pattern = pattern;
	}

	public String format(double number) {
		String string = Double.toString(number);
		if (pattern == null)
			return string;
		int patternIndex = pattern.lastIndexOf('.');
		if (patternIndex < 0 || patternIndex >= pattern.length() - 1)
			return string;
		int decimalPlaces = pattern.length() - 1 - pattern.lastIndexOf('.');
		double factor = Math.pow(10, decimalPlaces);
		return Double.toString(Math.round(number * factor) / factor);
	}
}
