package java.security.provider;

final class ByteArrayAccess {
    private ByteArrayAccess() {
    }

    static void b2iLittle(byte[] in, int inOfs, int[] out, int outOfs, int len) {
        if (inOfs >= 0 && in.length - inOfs >= len && outOfs >= 0 && out.length - outOfs >= len / 4) {
            for(len += inOfs; inOfs < len; inOfs += 4) {
                out[outOfs++] = in[inOfs] & 255 | (in[inOfs + 1] & 255) << 8 | (in[inOfs + 2] & 255) << 16 | in[inOfs + 3] << 24;
            }
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    static void b2iLittle64(byte[] in, int inOfs, int[] out) {
        if (inOfs >= 0 && in.length - inOfs >= 64 && out.length >= 16) {
            b2iLittle(in, inOfs, out, 0, 64);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    static void i2bLittle(int[] in, int inOfs, byte[] out, int outOfs, int len) {
        if (inOfs >= 0 && in.length - inOfs >= len / 4 && outOfs >= 0 && out.length - outOfs >= len) {
            int i;
            for(len += outOfs; outOfs < len; out[outOfs++] = (byte)(i >> 24)) {
                i = in[inOfs++];
                out[outOfs++] = (byte)i;
                out[outOfs++] = (byte)(i >> 8);
                out[outOfs++] = (byte)(i >> 16);
            }
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    static void i2bLittle4(int val, byte[] out, int outOfs) {
        if (outOfs >= 0 && out.length - outOfs >= 4) {
            out[outOfs] = (byte)val;
            out[outOfs + 1] = (byte)(val >> 8);
            out[outOfs + 2] = (byte)(val >> 16);
            out[outOfs + 3] = (byte)(val >> 24);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    static void b2iBig(byte[] in, int inOfs, int[] out, int outOfs, int len) {
        if (inOfs >= 0 && in.length - inOfs >= len && outOfs >= 0 && out.length - outOfs >= len / 4) {
            for(len += inOfs; inOfs < len; inOfs += 4) {
                out[outOfs++] = in[inOfs + 3] & 255 | (in[inOfs + 2] & 255) << 8 | (in[inOfs + 1] & 255) << 16 | in[inOfs] << 24;
            }
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    static void b2iBig64(byte[] in, int inOfs, int[] out) {
        if (inOfs >= 0 && in.length - inOfs >= 64 && out.length >= 16) {
            b2iBig(in, inOfs, out, 0, 64);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    static void i2bBig(int[] in, int inOfs, byte[] out, int outOfs, int len) {
        if (inOfs >= 0 && in.length - inOfs >= len / 4 && outOfs >= 0 && out.length - outOfs >= len) {
            int i;
            for(len += outOfs; outOfs < len; out[outOfs++] = (byte)i) {
                i = in[inOfs++];
                out[outOfs++] = (byte)(i >> 24);
                out[outOfs++] = (byte)(i >> 16);
                out[outOfs++] = (byte)(i >> 8);
            }
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    static void i2bBig4(int val, byte[] out, int outOfs) {
        if (outOfs >= 0 && out.length - outOfs >= 4) {
            out[outOfs] = (byte)(val >> 24);
            out[outOfs + 1] = (byte)(val >> 16);
            out[outOfs + 2] = (byte)(val >> 8);
            out[outOfs + 3] = (byte)val;
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    static void b2lBig(byte[] in, int inOfs, long[] out, int outOfs, int len) {
        if (inOfs >= 0 && in.length - inOfs >= len && outOfs >= 0 && out.length - outOfs >= len / 8) {
            for(len += inOfs; inOfs < len; inOfs += 4) {
                int i1 = in[inOfs + 3] & 255 | (in[inOfs + 2] & 255) << 8 | (in[inOfs + 1] & 255) << 16 | in[inOfs] << 24;
                inOfs += 4;
                int i2 = in[inOfs + 3] & 255 | (in[inOfs + 2] & 255) << 8 | (in[inOfs + 1] & 255) << 16 | in[inOfs] << 24;
                out[outOfs++] = (long)i1 << 32 | (long)i2 & 4294967295L;
            }
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    static void b2lBig128(byte[] in, int inOfs, long[] out) {
        if (inOfs >= 0 && in.length - inOfs >= 128 && out.length >= 16) {
            b2lBig(in, inOfs, out, 0, 128);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    static void l2bBig(long[] in, int inOfs, byte[] out, int outOfs, int len) {
        if (inOfs >= 0 && in.length - inOfs >= len / 8 && outOfs >= 0 && out.length - outOfs >= len) {
            long i;
            for(len += outOfs; outOfs < len; out[outOfs++] = (byte)((int)i)) {
                i = in[inOfs++];
                out[outOfs++] = (byte)((int)(i >> 56));
                out[outOfs++] = (byte)((int)(i >> 48));
                out[outOfs++] = (byte)((int)(i >> 40));
                out[outOfs++] = (byte)((int)(i >> 32));
                out[outOfs++] = (byte)((int)(i >> 24));
                out[outOfs++] = (byte)((int)(i >> 16));
                out[outOfs++] = (byte)((int)(i >> 8));
            }
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    static void b2lLittle(byte[] in, int inOfs, long[] out, int outOfs, int len) {
        if (inOfs >= 0 && in.length - inOfs >= len && outOfs >= 0 && out.length - outOfs >= len / 8) {
            for(len += inOfs; inOfs < len; inOfs += 8) {
                out[outOfs++] = (long)in[inOfs] & 255L | ((long)in[inOfs + 1] & 255L) << 8 | ((long)in[inOfs + 2] & 255L) << 16 | ((long)in[inOfs + 3] & 255L) << 24 | ((long)in[inOfs + 4] & 255L) << 32 | ((long)in[inOfs + 5] & 255L) << 40 | ((long)in[inOfs + 6] & 255L) << 48 | ((long)in[inOfs + 7] & 255L) << 56;
            }
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    static void l2bLittle(long[] in, int inOfs, byte[] out, int outOfs, int len) {
        if (inOfs >= 0 && in.length - inOfs >= len / 8 && outOfs >= 0 && out.length - outOfs >= len) {
            long i;
            for(len += outOfs; outOfs < len; out[outOfs++] = (byte)((int)(i >> 56))) {
                i = in[inOfs++];
                out[outOfs++] = (byte)((int)i);
                out[outOfs++] = (byte)((int)(i >> 8));
                out[outOfs++] = (byte)((int)(i >> 16));
                out[outOfs++] = (byte)((int)(i >> 24));
                out[outOfs++] = (byte)((int)(i >> 32));
                out[outOfs++] = (byte)((int)(i >> 40));
                out[outOfs++] = (byte)((int)(i >> 48));
            }
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }
}
