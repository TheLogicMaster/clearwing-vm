package java.util;

public class ResourceBundle {

	public static ResourceBundle getBundle(String baseName, Locale locale, ClassLoader loader) {
		return new ResourceBundle();
	}

	public static ResourceBundle getBundle(String bundleName, Locale locale) {
		return new ResourceBundle();
	}

	public static ResourceBundle getBundle(String bundleName) {
		return new ResourceBundle();
	}

	public Enumeration<String> getKeys() {
		return null;
	}

	public String getString(String key) {
		return key;
	}
}
