package java.nio.file;

public interface WatchEvent<T> {
    Kind<T> kind();

    int count();

    T context();

    public interface Modifier {
        String name();
    }

    public interface Kind<T> {
        String name();

        Class<T> type();
    }
}