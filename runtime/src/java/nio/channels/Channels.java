package java.nio.channels;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedOperationException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public final class Channels {
    private Channels() {
        throw new Error("no instances");
    }

    private static void writeFullyImpl(WritableByteChannel ch, ByteBuffer bb) throws IOException {
        while(true) {
            if (bb.remaining() > 0) {
                int n = ch.write(bb);
                if (n > 0) {
                    continue;
                }

                throw new RuntimeException("no bytes written");
            }

            return;
        }
    }

    private static void writeFully(WritableByteChannel ch, ByteBuffer bb) throws IOException {
        if (ch instanceof SelectableChannel) {
            SelectableChannel sc = (SelectableChannel)ch;
            synchronized(sc.blockingLock()) {
                if (!sc.isBlocking()) {
                    throw new IllegalBlockingModeException();
                }

                writeFullyImpl(ch, bb);
            }
        } else {
            writeFullyImpl(ch, bb);
        }

    }

    public static InputStream newInputStream(ReadableByteChannel ch) {
        Objects.requireNonNull(ch, "ch");
        return new ChannelInputStream(ch);
    }

    public static OutputStream newOutputStream(final WritableByteChannel ch) {
        Objects.requireNonNull(ch, "ch");
        return new OutputStream() {
            private ByteBuffer bb;
            private byte[] bs;
            private byte[] b1;

            public synchronized void write(int b) throws IOException {
                if (this.b1 == null) {
                    this.b1 = new byte[1];
                }

                this.b1[0] = (byte)b;
                this.write(this.b1);
            }

            public synchronized void write(byte[] bs, int off, int len) throws IOException {
                if (off >= 0 && off <= bs.length && len >= 0 && off + len <= bs.length && off + len >= 0) {
                    if (len != 0) {
                        ByteBuffer bb = this.bs == bs ? this.bb : ByteBuffer.wrap(bs);
                        bb.limit(Math.min(off + len, bb.capacity()));
                        bb.position(off);
                        this.bb = bb;
                        this.bs = bs;
                        Channels.writeFully(ch, bb);
                    }
                } else {
                    throw new IndexOutOfBoundsException();
                }
            }

            public void close() throws IOException {
                ch.close();
            }
        };
    }

    public static InputStream newInputStream(final AsynchronousByteChannel ch) {
        Objects.requireNonNull(ch, "ch");
        return new InputStream() {
            private ByteBuffer bb;
            private byte[] bs;
            private byte[] b1;

            public synchronized int read() throws IOException {
                if (this.b1 == null) {
                    this.b1 = new byte[1];
                }

                int n = this.read(this.b1);
                return n == 1 ? this.b1[0] & 255 : -1;
            }

            public synchronized int read(byte[] bs, int off, int len) throws IOException {
                if (off >= 0 && off <= bs.length && len >= 0 && off + len <= bs.length && off + len >= 0) {
                    if (len == 0) {
                        return 0;
                    } else {
                        ByteBuffer bb = this.bs == bs ? this.bb : ByteBuffer.wrap(bs);
                        bb.position(off);
                        bb.limit(Math.min(off + len, bb.capacity()));
                        this.bb = bb;
                        this.bs = bs;
                        boolean interrupted = false;

                        try {
                            while(true) {
                                try {
                                    int var14 = (Integer)ch.read(bb).get();
                                    return var14;
                                } catch (ExecutionException var11) {
                                    ExecutionException ee = var11;
                                    throw new IOException(ee.getCause());
                                } catch (InterruptedException var12) {
                                    interrupted = true;
                                }
                            }
                        } finally {
                            if (interrupted) {
                                Thread.currentThread().interrupt();
                            }

                        }
                    }
                } else {
                    throw new IndexOutOfBoundsException();
                }
            }

            public void close() throws IOException {
                ch.close();
            }
        };
    }

    public static OutputStream newOutputStream(final AsynchronousByteChannel ch) {
        Objects.requireNonNull(ch, "ch");
        return new OutputStream() {
            private ByteBuffer bb;
            private byte[] bs;
            private byte[] b1;

            public synchronized void write(int b) throws IOException {
                if (this.b1 == null) {
                    this.b1 = new byte[1];
                }

                this.b1[0] = (byte)b;
                this.write(this.b1);
            }

            public synchronized void write(byte[] bs, int off, int len) throws IOException {
                if (off >= 0 && off <= bs.length && len >= 0 && off + len <= bs.length && off + len >= 0) {
                    if (len != 0) {
                        ByteBuffer bb = this.bs == bs ? this.bb : ByteBuffer.wrap(bs);
                        bb.limit(Math.min(off + len, bb.capacity()));
                        bb.position(off);
                        this.bb = bb;
                        this.bs = bs;
                        boolean interrupted = false;

                        try {
                            while(bb.remaining() > 0) {
                                try {
                                    ch.write(bb).get();
                                } catch (ExecutionException var11) {
                                    ExecutionException ee = var11;
                                    throw new IOException(ee.getCause());
                                } catch (InterruptedException var12) {
                                    interrupted = true;
                                }
                            }
                        } finally {
                            if (interrupted) {
                                Thread.currentThread().interrupt();
                            }

                        }

                    }
                } else {
                    throw new IndexOutOfBoundsException();
                }
            }

            public void close() throws IOException {
                ch.close();
            }
        };
    }

    public static ReadableByteChannel newChannel(InputStream in) {
        Objects.requireNonNull(in, "in");
        return (ReadableByteChannel)(in.getClass() == FileInputStream.class ? ((FileInputStream)in).getChannel() : new ReadableByteChannelImpl(in));
    }

    public static WritableByteChannel newChannel(OutputStream out) {
        Objects.requireNonNull(out, "out");
        return (WritableByteChannel)(out.getClass() == FileOutputStream.class ? ((FileOutputStream)out).getChannel() : new WritableByteChannelImpl(out));
    }

    public static Reader newReader(ReadableByteChannel ch, CharsetDecoder dec, int minBufferCap) {
        Objects.requireNonNull(ch, "ch");
        throw new UnsupportedOperationException();
//        return StreamDecoder.forDecoder(ch, dec.reset(), minBufferCap);
    }

    public static Reader newReader(ReadableByteChannel ch, String csName) {
        Objects.requireNonNull(csName, "csName");
        return newReader(ch, Charset.forName(csName).newDecoder(), -1);
    }

    public static Reader newReader(ReadableByteChannel ch, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        return newReader(ch, charset.newDecoder(), -1);
    }

    public static Writer newWriter(WritableByteChannel ch, CharsetEncoder enc, int minBufferCap) {
        Objects.requireNonNull(ch, "ch");
        throw new UnsupportedOperationException();
//        return StreamEncoder.forEncoder(ch, enc.reset(), minBufferCap);
    }

    public static Writer newWriter(WritableByteChannel ch, String csName) {
        Objects.requireNonNull(csName, "csName");
        return newWriter(ch, Charset.forName(csName).newEncoder(), -1);
    }

    public static Writer newWriter(WritableByteChannel ch, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        return newWriter(ch, charset.newEncoder(), -1);
    }

    private static class WritableByteChannelImpl extends AbstractInterruptibleChannel implements WritableByteChannel {
        private final OutputStream out;
        private static final int TRANSFER_SIZE = 8192;
        private byte[] buf = new byte[0];
        private final Object writeLock = new Object();

        WritableByteChannelImpl(OutputStream out) {
            this.out = out;
        }

        public int write(ByteBuffer src) throws IOException {
            if (!this.isOpen()) {
                throw new ClosedChannelException();
            } else {
                int len = src.remaining();
                int totalWritten = 0;
                synchronized(this.writeLock) {
                    int bytesToWrite;
                    for(; totalWritten < len; totalWritten += bytesToWrite) {
                        bytesToWrite = Math.min(len - totalWritten, 8192);
                        if (this.buf.length < bytesToWrite) {
                            this.buf = new byte[bytesToWrite];
                        }

                        src.get(this.buf, 0, bytesToWrite);

                        try {
                            this.begin();
                            this.out.write(this.buf, 0, bytesToWrite);
                        } finally {
                            this.end(bytesToWrite > 0);
                        }
                    }

                    return totalWritten;
                }
            }
        }

        protected void implCloseChannel() throws IOException {
            this.out.close();
        }
    }

    private static class ReadableByteChannelImpl extends AbstractInterruptibleChannel implements ReadableByteChannel {
        private final InputStream in;
        private static final int TRANSFER_SIZE = 8192;
        private byte[] buf = new byte[0];
        private final Object readLock = new Object();

        ReadableByteChannelImpl(InputStream in) {
            this.in = in;
        }

        public int read(ByteBuffer dst) throws IOException {
            if (!this.isOpen()) {
                throw new ClosedChannelException();
            } else {
                int len = dst.remaining();
                int totalRead = 0;
                int bytesRead = 0;
                synchronized(this.readLock) {
                    while(totalRead < len) {
                        int bytesToRead = Math.min(len - totalRead, 8192);
                        if (this.buf.length < bytesToRead) {
                            this.buf = new byte[bytesToRead];
                        }

                        if (totalRead > 0 && this.in.available() <= 0) {
                            break;
                        }

                        try {
                            this.begin();
                            bytesRead = this.in.read(this.buf, 0, bytesToRead);
                        } finally {
                            this.end(bytesRead > 0);
                        }

                        if (bytesRead < 0) {
                            break;
                        }

                        totalRead += bytesRead;
                        dst.put(this.buf, 0, bytesRead);
                    }

                    return bytesRead < 0 && totalRead == 0 ? -1 : totalRead;
                }
            }
        }

        protected void implCloseChannel() throws IOException {
            this.in.close();
        }
    }
}