package java.security;

import java.nio.ByteBuffer;

public abstract class MessageDigestSpi {
    private byte[] tempArray;

    public MessageDigestSpi() {
    }

    protected int engineGetDigestLength() {
        return 0;
    }

    protected abstract void engineUpdate(byte var1);

    protected abstract void engineUpdate(byte[] var1, int var2, int var3);

    protected void engineUpdate(ByteBuffer input) {
        if (input.hasRemaining()) {
            int n;
            int chunk;
            if (input.hasArray()) {
                byte[] b = input.array();
                n = input.arrayOffset();
                chunk = input.position();
                int lim = input.limit();
                this.engineUpdate(b, n + chunk, lim - chunk);
                input.position(lim);
            } else {
                int len = input.remaining();
                n = Math.min(4096, len);
                if (this.tempArray == null || n > this.tempArray.length) {
                    this.tempArray = new byte[n];
                }

                while(len > 0) {
                    chunk = Math.min(len, this.tempArray.length);
                    input.get(this.tempArray, 0, chunk);
                    this.engineUpdate(this.tempArray, 0, chunk);
                    len -= chunk;
                }
            }

        }
    }

    protected abstract byte[] engineDigest();

    protected int engineDigest(byte[] buf, int offset, int len) throws DigestException {
        byte[] digest = this.engineDigest();
        if (len < digest.length) {
            throw new DigestException("partial digests not returned");
        } else if (buf.length - offset < digest.length) {
            throw new DigestException("insufficient space in the output buffer to store the digest");
        } else {
            System.arraycopy(digest, 0, buf, offset, digest.length);
            return digest.length;
        }
    }

    protected abstract void engineReset();

    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        } else {
            throw new CloneNotSupportedException();
        }
    }
}
