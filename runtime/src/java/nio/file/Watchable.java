package java.nio.file;

import java.io.IOException;

public interface Watchable {
    WatchKey register(WatchService var1, WatchEvent.Kind<?>[] var2, WatchEvent.Modifier... var3) throws IOException;

    WatchKey register(WatchService var1, WatchEvent.Kind<?>... var2) throws IOException;
}