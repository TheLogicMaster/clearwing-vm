package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface SeekableByteChannel extends ByteChannel {
    int read(ByteBuffer var1) throws IOException;

    int write(ByteBuffer var1) throws IOException;

    long position() throws IOException;

    SeekableByteChannel position(long var1) throws IOException;

    long size() throws IOException;

    SeekableByteChannel truncate(long var1) throws IOException;
}