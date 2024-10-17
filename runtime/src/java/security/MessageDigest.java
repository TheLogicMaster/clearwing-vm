package java.security;

import java.nio.ByteBuffer;
import java.security.provider.MD5;
import java.util.Objects;

public abstract class MessageDigest extends MessageDigestSpi {
    private final String algorithm;
    private final Provider provider;

    protected MessageDigest(String algorithm) {
        this.algorithm = algorithm;
        provider = null;
    }

    private MessageDigest(String algorithm, Provider p) {
        this.algorithm = algorithm;
        this.provider = p;
    }

    public static MessageDigest getInstance(String algorithm) throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        switch (algorithm) {
            case "MD5": return Delegate.of(new MD5(), algorithm, null);
            default: throw new NoSuchAlgorithmException(algorithm);
        }
    }

    public static MessageDigest getInstance(String algorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        return getInstance(algorithm);
    }

    public static MessageDigest getInstance(String algorithm, Provider provider) throws NoSuchAlgorithmException {
        return getInstance(algorithm);
    }

    public final Provider getProvider() {
        return provider;
    }

    public void update(byte input) {
        this.engineUpdate(input);
    }

    public void update(byte[] input, int offset, int len) {
        if (input == null) {
            throw new IllegalArgumentException("No input buffer given");
        } else if (input.length - offset < len) {
            throw new IllegalArgumentException("Input buffer too short");
        } else {
            this.engineUpdate(input, offset, len);
        }
    }

    public void update(byte[] input) {
        this.engineUpdate(input, 0, input.length);
    }

    public final void update(ByteBuffer input) {
        if (input == null) {
            throw new NullPointerException();
        } else {
            this.engineUpdate(input);
        }
    }

    public byte[] digest() {
        return this.engineDigest();
    }

    public int digest(byte[] buf, int offset, int len) throws DigestException {
        if (buf == null) {
            throw new IllegalArgumentException("No output buffer given");
        } else if (buf.length - offset < len) {
            throw new IllegalArgumentException("Output buffer too small for specified offset and length");
        } else {
            return this.engineDigest(buf, offset, len);
        }
    }

    public byte[] digest(byte[] input) {
        this.update(input);
        return this.digest();
    }

    private String getProviderName() {
        return this.provider == null ? "(no provider)" : this.provider.getName();
    }

    public String toString() {
        return algorithm + " Message Digest from " + getProviderName();
    }

    public static boolean isEqual(byte[] digesta, byte[] digestb) {
        if (digesta == digestb) {
            return true;
        } else if (digesta != null && digestb != null) {
            int lenA = digesta.length;
            int lenB = digestb.length;
            if (lenB == 0) {
                return lenA == 0;
            } else {
                int result = 0;
                result |= lenA - lenB;

                for(int i = 0; i < lenA; ++i) {
                    int indexB = (i - lenB >>> 31) * i;
                    result |= digesta[i] ^ digestb[indexB];
                }

                return result == 0;
            }
        } else {
            return false;
        }
    }

    public void reset() {
        this.engineReset();
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public final int getDigestLength() {
        int digestLen = this.engineGetDigestLength();
        if (digestLen == 0) {
            try {
                MessageDigest md = (MessageDigest)this.clone();
                byte[] digest = md.digest();
                return digest.length;
            } catch (CloneNotSupportedException var4) {
                return digestLen;
            }
        } else {
            return digestLen;
        }
    }

    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        } else {
            throw new CloneNotSupportedException();
        }
    }

    private static class Delegate extends MessageDigest {
        private final MessageDigestSpi digestSpi;

        static Delegate of(MessageDigestSpi digestSpi, String algo, Provider p) {
            Objects.requireNonNull(digestSpi);
            boolean isCloneable = digestSpi instanceof Cloneable;
            if (isCloneable) {
                try {
                    digestSpi.clone();
                } catch (CloneNotSupportedException var5) {
                    isCloneable = false;
                }
            }

            return (Delegate)(isCloneable ? new CloneableDelegate(digestSpi, algo, p) : new Delegate(digestSpi, algo, p));
        }

        private Delegate(MessageDigestSpi digestSpi, String algorithm, Provider p) {
            super(algorithm, p);
            this.digestSpi = digestSpi;
        }

        public Object clone() throws CloneNotSupportedException {
            if (this instanceof Cloneable) {
                return new CloneableDelegate((MessageDigestSpi) this.digestSpi.clone(), super.algorithm, super.provider);
            } else {
                throw new CloneNotSupportedException();
            }
        }

        protected int engineGetDigestLength() {
            return this.digestSpi.engineGetDigestLength();
        }

        protected void engineUpdate(byte input) {
            this.digestSpi.engineUpdate(input);
        }

        protected void engineUpdate(byte[] input, int offset, int len) {
            this.digestSpi.engineUpdate(input, offset, len);
        }

        protected void engineUpdate(ByteBuffer input) {
            this.digestSpi.engineUpdate(input);
        }

//        public void engineUpdate(SecretKey key) throws InvalidKeyException {
//            throw new UnsupportedOperationException("Digest does not support update of SecretKey object");
//        }

        protected byte[] engineDigest() {
            return this.digestSpi.engineDigest();
        }

        protected int engineDigest(byte[] buf, int offset, int len) throws DigestException {
            return this.digestSpi.engineDigest(buf, offset, len);
        }

        protected void engineReset() {
            this.digestSpi.engineReset();
        }

        private static final class CloneableDelegate extends Delegate implements Cloneable {
            private CloneableDelegate(MessageDigestSpi digestSpi, String algorithm, Provider p) {
                super(digestSpi, algorithm, p);
            }
        }
    }
}
