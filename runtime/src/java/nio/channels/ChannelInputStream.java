package java.nio.channels;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.SelectableChannel;
import java.util.Objects;

public class ChannelInputStream extends InputStream {
    protected final ReadableByteChannel ch;
    private ByteBuffer bb = null;
    private byte[] bs = null;
    private byte[] b1 = null;

    public static int read(ReadableByteChannel ch, ByteBuffer bb, boolean block) throws IOException {
        if (ch instanceof SelectableChannel) {
            SelectableChannel sc = (SelectableChannel)ch;
            synchronized(sc.blockingLock()) {
                boolean bm = sc.isBlocking();
                if (!bm) {
                    throw new IllegalBlockingModeException();
                } else {
                    if (bm != block) {
                        sc.configureBlocking(block);
                    }

                    int n = ch.read(bb);
                    if (bm != block) {
                        sc.configureBlocking(bm);
                    }

                    return n;
                }
            }
        } else {
            return ch.read(bb);
        }
    }

    public ChannelInputStream(ReadableByteChannel ch) {
        this.ch = ch;
    }

    public synchronized int read() throws IOException {
        if (this.b1 == null) {
            this.b1 = new byte[1];
        }

        int n = this.read(this.b1);
        return n == 1 ? this.b1[0] & 255 : -1;
    }

    public synchronized int read(byte[] bs, int off, int len) throws IOException {
//        Objects.checkFromIndexSize(off, len, bs.length);
        if (len == 0) {
            return 0;
        } else {
            ByteBuffer bb = this.bs == bs ? this.bb : ByteBuffer.wrap(bs);
            bb.limit(Math.min(off + len, bb.capacity()));
            bb.position(off);
            this.bb = bb;
            this.bs = bs;
            return this.read(bb);
        }
    }

    protected int read(ByteBuffer bb) throws IOException {
        return read(this.ch, bb, true);
    }

    public int available() throws IOException {
        if (this.ch instanceof SeekableByteChannel) {
            SeekableByteChannel sbc = (SeekableByteChannel)this.ch;
            long rem = Math.max(0L, sbc.size() - sbc.position());
            return rem > 2147483647L ? Integer.MAX_VALUE : (int)rem;
        } else {
            return 0;
        }
    }

    public synchronized long skip(long n) throws IOException {
        if (!(this.ch instanceof SeekableByteChannel)) {
            return super.skip(n);
        } else {
            SeekableByteChannel sbc = (SeekableByteChannel)this.ch;
            long pos = sbc.position();
            long newPos;
            if (n > 0L) {
                newPos = pos + n;
                long size = sbc.size();
                if (newPos < 0L || newPos > size) {
                    newPos = size;
                }
            } else {
                newPos = Long.max(pos + n, 0L);
            }

            sbc.position(newPos);
            return newPos - pos;
        }
    }

    public void close() throws IOException {
        this.ch.close();
    }
}
