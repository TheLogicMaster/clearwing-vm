package java.util;

public class ResourceBundle {

	public static ResourceBundle getBundle(String baseName, Locale locale, ClassLoader loader) {
		return new ResourceBundle();
	}

	public Enumeration<String> getKeys() {
		return null;
	}

	public String getString(String key) {
		throw new UnsupportedOperationException();
	}
}
