/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.util.zip;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ZipInputStream extends InflaterInputStream implements ZipConstants {
    private ZipEntry entry;
    private int flag;
    private CRC32 crc;
    private long remaining;
    private byte[] tmpbuf;
    private static final int STORED = 0;
    private static final int DEFLATED = 8;
    private boolean closed;
    private boolean entryEOF;
    private byte[] b;

    private void ensureOpen() throws IOException {
        if (this.closed) {
            throw new IOException("Stream closed");
        }
    }

    public ZipInputStream(InputStream in) {
        this(in, StandardCharsets.UTF_8);
    }

    public ZipInputStream(InputStream in, Charset charset) {
        super(new PushbackInputStream(in, 512), new Inflater(true), 512);
        this.crc = new CRC32();
        this.tmpbuf = new byte[512];
        this.closed = false;
        this.entryEOF = false;
        this.b = new byte[256];
        this.usesDefaultInflater = true;
        if (in == null) {
            throw new NullPointerException("in is null");
        }
    }

    public ZipEntry getNextEntry() throws IOException {
        this.ensureOpen();
        if (this.entry != null) {
            this.closeEntry();
        }

        this.crc.reset();
        this.inf.reset();
        if ((this.entry = this.readLOC()) == null) {
            return null;
        } else {
            if (this.entry.method == 0) {
                this.remaining = this.entry.size;
            }

            this.entryEOF = false;
            return this.entry;
        }
    }

    public void closeEntry() throws IOException {
        this.ensureOpen();

        while(this.read(this.tmpbuf, 0, this.tmpbuf.length) != -1) {
        }

        this.entryEOF = true;
    }

    public int available() throws IOException {
        this.ensureOpen();
        return this.entryEOF ? 0 : 1;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        this.ensureOpen();
        if (off >= 0 && len >= 0 && off <= b.length - len) {
            if (len == 0) {
                return 0;
            } else if (this.entry == null) {
                return -1;
            } else {
                switch (this.entry.method) {
                    case 0:
                        if (this.remaining <= 0L) {
                            this.entryEOF = true;
                            this.entry = null;
                            return -1;
                        } else {
                            if ((long)len > this.remaining) {
                                len = (int)this.remaining;
                            }

                            len = this.in.read(b, off, len);
                            if (len == -1) {
                                throw new ZipException("unexpected EOF");
                            } else {
                                this.crc.update(b, off, len);
                                this.remaining -= (long)len;
                                if (this.remaining == 0L && this.entry.crc != this.crc.getValue()) {
                                    throw new ZipException("invalid entry CRC (expected 0x" + Long.toHexString(this.entry.crc) + " but got 0x" + Long.toHexString(this.crc.getValue()) + ")");
                                }

                                return len;
                            }
                        }
                    case 8:
                        len = super.read(b, off, len);
                        if (len == -1) {
                            this.readEnd(this.entry);
                            this.entryEOF = true;
                            this.entry = null;
                        } else {
                            this.crc.update(b, off, len);
                        }

                        return len;
                    default:
                        throw new ZipException("invalid compression method");
                }
            }
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public long skip(long n) throws IOException {
        if (n < 0L) {
            throw new IllegalArgumentException("negative skip length");
        } else {
            this.ensureOpen();
            int max = (int)Math.min(n, 2147483647L);

            int total;
            int len;
            for(total = 0; total < max; total += len) {
                len = max - total;
                if (len > this.tmpbuf.length) {
                    len = this.tmpbuf.length;
                }

                len = this.read(this.tmpbuf, 0, len);
                if (len == -1) {
                    this.entryEOF = true;
                    break;
                }
            }

            return (long)total;
        }
    }

    public void close() throws IOException {
        if (!this.closed) {
            super.close();
            this.closed = true;
        }

    }

    private ZipEntry readLOC() throws IOException {
        try {
            this.readFully(this.tmpbuf, 0, 30);
        } catch (EOFException var5) {
            return null;
        }

        if (ZipUtils.get32(this.tmpbuf, 0) != 67324752L) {
            return null;
        } else {
            this.flag = ZipUtils.get16(this.tmpbuf, 6);
            int len = ZipUtils.get16(this.tmpbuf, 26);
            int blen = this.b.length;
            if (len > blen) {
                do {
                    blen *= 2;
                } while(len > blen);

                this.b = new byte[blen];
            }

            this.readFully(this.b, 0, len);
//            ZipEntry e = this.createZipEntry((this.flag & 2048) != 0 ? ZipCoder.toStringUTF8(this.b, len) : this.zc.toString(this.b, len));
            ZipEntry e = this.createZipEntry(new String(b, 0, len));
            if ((this.flag & 1) == 1) {
                throw new ZipException("encrypted ZIP entry not supported");
            } else {
                e.method = ZipUtils.get16(this.tmpbuf, 8);
                e.xdostime = ZipUtils.get32(this.tmpbuf, 10);
                if ((this.flag & 8) == 8) {
                    if (e.method != 8) {
                        throw new ZipException("only DEFLATED entries can have EXT descriptor");
                    }
                } else {
                    e.crc = ZipUtils.get32(this.tmpbuf, 14);
                    e.csize = ZipUtils.get32(this.tmpbuf, 18);
                    e.size = ZipUtils.get32(this.tmpbuf, 22);
                }

                len = ZipUtils.get16(this.tmpbuf, 28);
                if (len > 0) {
                    byte[] extra = new byte[len];
                    this.readFully(extra, 0, len);
                    e.setExtra0(extra, e.csize == 4294967295L || e.size == 4294967295L, true);
                }

                return e;
            }
        }
    }

    protected ZipEntry createZipEntry(String name) {
        return new ZipEntry(name);
    }

    private void readEnd(ZipEntry e) throws IOException {
        int n = this.inf.getRemaining();
        if (n > 0) {
            ((PushbackInputStream)this.in).unread(this.buf, this.len - n, n);
        }

        if ((this.flag & 8) == 8) {
            long sig;
            if (this.inf.getBytesWritten() <= 4294967295L && this.inf.getBytesRead() <= 4294967295L) {
                this.readFully(this.tmpbuf, 0, 16);
                sig = ZipUtils.get32(this.tmpbuf, 0);
                if (sig != 134695760L) {
                    e.crc = sig;
                    e.csize = ZipUtils.get32(this.tmpbuf, 4);
                    e.size = ZipUtils.get32(this.tmpbuf, 8);
                    ((PushbackInputStream)this.in).unread(this.tmpbuf, 12, 4);
                } else {
                    e.crc = ZipUtils.get32(this.tmpbuf, 4);
                    e.csize = ZipUtils.get32(this.tmpbuf, 8);
                    e.size = ZipUtils.get32(this.tmpbuf, 12);
                }
            } else {
                this.readFully(this.tmpbuf, 0, 24);
                sig = ZipUtils.get32(this.tmpbuf, 0);
                if (sig != 134695760L) {
                    e.crc = sig;
                    e.csize = ZipUtils.get64(this.tmpbuf, 4);
                    e.size = ZipUtils.get64(this.tmpbuf, 12);
                    ((PushbackInputStream)this.in).unread(this.tmpbuf, 20, 4);
                } else {
                    e.crc = ZipUtils.get32(this.tmpbuf, 4);
                    e.csize = ZipUtils.get64(this.tmpbuf, 8);
                    e.size = ZipUtils.get64(this.tmpbuf, 16);
                }
            }
        }

        if (e.size != this.inf.getBytesWritten()) {
            throw new ZipException("invalid entry size (expected " + e.size + " but got " + this.inf.getBytesWritten() + " bytes)");
        } else if (e.csize != this.inf.getBytesRead()) {
            throw new ZipException("invalid entry compressed size (expected " + e.csize + " but got " + this.inf.getBytesRead() + " bytes)");
        } else if (e.crc != this.crc.getValue()) {
            throw new ZipException("invalid entry CRC (expected 0x" + Long.toHexString(e.crc) + " but got 0x" + Long.toHexString(this.crc.getValue()) + ")");
        }
    }

    private void readFully(byte[] b, int off, int len) throws IOException {
        while(len > 0) {
            int n = this.in.read(b, off, len);
            if (n == -1) {
                throw new EOFException();
            }

            off += n;
            len -= n;
        }

    }
}
