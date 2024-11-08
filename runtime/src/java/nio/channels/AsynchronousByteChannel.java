package java.nio.channels;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;

public interface AsynchronousByteChannel extends AsynchronousChannel {
    <A> void read(ByteBuffer var1, A var2, CompletionHandler<Integer, ? super A> var3);

    Future<Integer> read(ByteBuffer var1);

    <A> void write(ByteBuffer var1, A var2, CompletionHandler<Integer, ? super A> var3);

    Future<Integer> write(ByteBuffer var1);
}