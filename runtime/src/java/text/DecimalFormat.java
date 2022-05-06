package java.text;

/**
 * Not properly implemented
 */
public class DecimalFormat {

	private final String pattern;

	public DecimalFormat (String pattern) {
		this.pattern = pattern;
	}

	public String format(double number) {
		return Double.toString(number);
	}
}
