package java.net;

import java.io.Closeable;
import java.io.IOException;
import java.security.SecureClassLoader;

public class URLClassLoader extends SecureClassLoader implements Closeable {
    public URLClassLoader(URL[] urls, ClassLoader parent) {
        super(parent);
    }

    public URLClassLoader(URL[] urls) {
    }

    public URLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(parent);
    }
    
    @Override
    public void close() throws IOException {
    }
}
