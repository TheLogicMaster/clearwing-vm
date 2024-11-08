package java.nio.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
//import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.NoSuchElementException;

public interface Path extends Comparable<Path>, Iterable<Path>, Watchable {
    static Path of(String first, String... more) {
        throw new UnsupportedOperationException();
//        return FileSystems.getDefault().getPath(first, more);
    }

    static Path of(URI uri) {
        throw new UnsupportedOperationException();
//        String scheme = uri.getScheme();
//        if (scheme == null) {
//            throw new IllegalArgumentException("Missing scheme");
//        } else if (scheme.equalsIgnoreCase("file")) {
//            return FileSystems.getDefault().provider().getPath(uri);
//        } else {
//            Iterator var2 = FileSystemProvider.installedProviders().iterator();
//
//            FileSystemProvider provider;
//            do {
//                if (!var2.hasNext()) {
//                    throw new FileSystemNotFoundException("Provider \"" + scheme + "\" not installed");
//                }
//
//                provider = (FileSystemProvider)var2.next();
//            } while(!provider.getScheme().equalsIgnoreCase(scheme));
//
//            return provider.getPath(uri);
//        }
    }

    FileSystem getFileSystem();

    boolean isAbsolute();

    Path getRoot();

    Path getFileName();

    Path getParent();

    int getNameCount();

    Path getName(int var1);

    Path subpath(int var1, int var2);

    boolean startsWith(Path var1);

    default boolean startsWith(String other) {
        return this.startsWith(this.getFileSystem().getPath(other));
    }

    boolean endsWith(Path var1);

    default boolean endsWith(String other) {
        return this.endsWith(this.getFileSystem().getPath(other));
    }

    Path normalize();

    Path resolve(Path var1);

    default Path resolve(String other) {
        return this.resolve(this.getFileSystem().getPath(other));
    }

    default Path resolveSibling(Path other) {
        if (other == null) {
            throw new NullPointerException();
        } else {
            Path parent = this.getParent();
            return parent == null ? other : parent.resolve(other);
        }
    }

    default Path resolveSibling(String other) {
        return this.resolveSibling(this.getFileSystem().getPath(other));
    }

    Path relativize(Path var1);

    URI toUri();

    Path toAbsolutePath();

    Path toRealPath(LinkOption... var1) throws IOException;

    default File toFile() {
        return new File(toString());
//        if (this.getFileSystem() == FileSystems.getDefault()) {
//            return new File(this.toString());
//        } else {
//            throw new UnsupportedOperationException("Path not associated with default file system.");
//        }
    }

    WatchKey register(WatchService var1, WatchEvent.Kind<?>[] var2, WatchEvent.Modifier... var3) throws IOException;

    default WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        return this.register(watcher, events);
    }

    default Iterator<Path> iterator() {
        return new Iterator<Path>() {
            private int i = 0;

            public boolean hasNext() {
                return this.i < Path.this.getNameCount();
            }

            public Path next() {
                if (this.i < Path.this.getNameCount()) {
                    Path result = Path.this.getName(this.i);
                    ++this.i;
                    return result;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }

    int compareTo(Path var1);

    boolean equals(Object var1);

    int hashCode();

    String toString();
}