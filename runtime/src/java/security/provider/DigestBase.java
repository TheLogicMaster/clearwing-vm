package java.security.provider;

import java.security.DigestException;
import java.security.MessageDigestSpi;
import java.security.ProviderException;
import java.util.Arrays;
import java.util.Objects;

abstract class DigestBase extends MessageDigestSpi implements Cloneable {
    private byte[] oneByte;
    private final String algorithm;
    private final int digestLength;
    private final int blockSize;
    byte[] buffer;
    private int bufOfs;
    long bytesProcessed;
    static final byte[] padding = new byte[136];

    DigestBase(String algorithm, int digestLength, int blockSize) {
        this.algorithm = algorithm;
        this.digestLength = digestLength;
        this.blockSize = blockSize;
        this.buffer = new byte[blockSize];
    }

    protected final int engineGetDigestLength() {
        return this.digestLength;
    }

    protected final void engineUpdate(byte b) {
        if (this.oneByte == null) {
            this.oneByte = new byte[1];
        }

        this.oneByte[0] = b;
        this.engineUpdate(this.oneByte, 0, 1);
    }

    protected final void engineUpdate(byte[] b, int ofs, int len) {
        if (len != 0) {
            if (ofs >= 0 && len >= 0 && ofs <= b.length - len) {
                if (this.bytesProcessed < 0L) {
                    this.engineReset();
                }

                this.bytesProcessed += (long)len;
                int limit;
                if (this.bufOfs != 0) {
                    limit = Math.min(len, this.blockSize - this.bufOfs);
                    System.arraycopy(b, ofs, this.buffer, this.bufOfs, limit);
                    this.bufOfs += limit;
                    ofs += limit;
                    len -= limit;
                    if (this.bufOfs >= this.blockSize) {
                        this.implCompress(this.buffer, 0);
                        this.bufOfs = 0;
                    }
                }

                if (len >= this.blockSize) {
                    limit = ofs + len;
                    ofs = this.implCompressMultiBlock(b, ofs, limit - this.blockSize);
                    len = limit - ofs;
                }

                if (len > 0) {
                    System.arraycopy(b, ofs, this.buffer, 0, len);
                    this.bufOfs = len;
                }

            } else {
                throw new ArrayIndexOutOfBoundsException();
            }
        }
    }

    private int implCompressMultiBlock(byte[] b, int ofs, int limit) {
        this.implCompressMultiBlockCheck(b, ofs, limit);
        return this.implCompressMultiBlock0(b, ofs, limit);
    }

    private int implCompressMultiBlock0(byte[] b, int ofs, int limit) {
        while(ofs <= limit) {
            this.implCompress(b, ofs);
            ofs += this.blockSize;
        }

        return ofs;
    }

    private void implCompressMultiBlockCheck(byte[] b, int ofs, int limit) {
        if (limit >= 0) {
            Objects.requireNonNull(b);
            if (ofs >= 0 && ofs < b.length) {
                int endIndex = limit / this.blockSize * this.blockSize + this.blockSize - 1;
                if (endIndex >= b.length) {
                    throw new ArrayIndexOutOfBoundsException(endIndex);
                }
            } else {
                throw new ArrayIndexOutOfBoundsException(ofs);
            }
        }
    }

    protected final void engineReset() {
        if (this.bytesProcessed != 0L) {
            this.implReset();
            this.bufOfs = 0;
            this.bytesProcessed = 0L;
            Arrays.fill(this.buffer, (byte)0);
        }
    }

    protected final byte[] engineDigest() {
        byte[] b = new byte[this.digestLength];

        try {
            this.engineDigest(b, 0, b.length);
            return b;
        } catch (DigestException var3) {
            DigestException e = var3;
            throw (ProviderException)(new ProviderException("Internal error")).initCause(e);
        }
    }

    protected final int engineDigest(byte[] out, int ofs, int len) throws DigestException {
        if (len < this.digestLength) {
            throw new DigestException("Length must be at least " + this.digestLength + " for " + this.algorithm + "digests");
        } else if (ofs >= 0 && len >= 0 && ofs <= out.length - len) {
            if (this.bytesProcessed < 0L) {
                this.engineReset();
            }

            this.implDigest(out, ofs);
            this.bytesProcessed = -1L;
            return this.digestLength;
        } else {
            throw new DigestException("Buffer too short to store digest");
        }
    }

    abstract void implCompress(byte[] var1, int var2);

    abstract void implDigest(byte[] var1, int var2);

    abstract void implReset();

    public Object clone() throws CloneNotSupportedException {
        DigestBase copy = (DigestBase)super.clone();
        copy.buffer = (byte[])copy.buffer.clone();
        return copy;
    }

    static {
        padding[0] = -128;
    }
}
