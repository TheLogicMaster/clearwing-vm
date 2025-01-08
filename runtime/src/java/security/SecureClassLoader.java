package java.security;

public class SecureClassLoader extends ClassLoader {
    protected SecureClassLoader(ClassLoader parent) {
        super(parent);
    }

    protected SecureClassLoader() {
    }
}
