package java.nio.channels;

public interface CompletionHandler<V, A> {
    void completed(V var1, A var2);

    void failed(Throwable var1, A var2);
}