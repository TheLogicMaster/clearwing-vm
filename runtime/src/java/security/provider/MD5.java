package java.security.provider;

import java.util.Arrays;

public final class MD5 extends DigestBase {
    private int[] state = new int[4];
    private int[] x = new int[16];

    public MD5() {
        super("MD5", 16, 64);
        this.resetHashes();
    }

    public Object clone() throws CloneNotSupportedException {
        MD5 copy = (MD5)super.clone();
        copy.state = (int[])copy.state.clone();
        copy.x = new int[16];
        return copy;
    }

    void implReset() {
        this.resetHashes();
        Arrays.fill(this.x, 0);
    }

    private void resetHashes() {
        this.state[0] = 1732584193;
        this.state[1] = -271733879;
        this.state[2] = -1732584194;
        this.state[3] = 271733878;
    }

    void implDigest(byte[] out, int ofs) {
        long bitsProcessed = this.bytesProcessed << 3;
        int index = (int)this.bytesProcessed & 63;
        int padLen = index < 56 ? 56 - index : 120 - index;
        this.engineUpdate(padding, 0, padLen);
        ByteArrayAccess.i2bLittle4((int)bitsProcessed, this.buffer, 56);
        ByteArrayAccess.i2bLittle4((int)(bitsProcessed >>> 32), this.buffer, 60);
        this.implCompress(this.buffer, 0);
        ByteArrayAccess.i2bLittle(this.state, 0, out, ofs, 16);
    }

    private static int FF(int a, int b, int c, int d, int x, int s, int ac) {
        a += (b & c | ~b & d) + x + ac;
        return (a << s | a >>> 32 - s) + b;
    }

    private static int GG(int a, int b, int c, int d, int x, int s, int ac) {
        a += (b & d | c & ~d) + x + ac;
        return (a << s | a >>> 32 - s) + b;
    }

    private static int HH(int a, int b, int c, int d, int x, int s, int ac) {
        a += (b ^ c ^ d) + x + ac;
        return (a << s | a >>> 32 - s) + b;
    }

    private static int II(int a, int b, int c, int d, int x, int s, int ac) {
        a += (c ^ (b | ~d)) + x + ac;
        return (a << s | a >>> 32 - s) + b;
    }

    void implCompress(byte[] buf, int ofs) {
        ByteArrayAccess.b2iLittle64(buf, ofs, this.x);
        int a = this.state[0];
        int b = this.state[1];
        int c = this.state[2];
        int d = this.state[3];
        a = FF(a, b, c, d, this.x[0], 7, -680876936);
        d = FF(d, a, b, c, this.x[1], 12, -389564586);
        c = FF(c, d, a, b, this.x[2], 17, 606105819);
        b = FF(b, c, d, a, this.x[3], 22, -1044525330);
        a = FF(a, b, c, d, this.x[4], 7, -176418897);
        d = FF(d, a, b, c, this.x[5], 12, 1200080426);
        c = FF(c, d, a, b, this.x[6], 17, -1473231341);
        b = FF(b, c, d, a, this.x[7], 22, -45705983);
        a = FF(a, b, c, d, this.x[8], 7, 1770035416);
        d = FF(d, a, b, c, this.x[9], 12, -1958414417);
        c = FF(c, d, a, b, this.x[10], 17, -42063);
        b = FF(b, c, d, a, this.x[11], 22, -1990404162);
        a = FF(a, b, c, d, this.x[12], 7, 1804603682);
        d = FF(d, a, b, c, this.x[13], 12, -40341101);
        c = FF(c, d, a, b, this.x[14], 17, -1502002290);
        b = FF(b, c, d, a, this.x[15], 22, 1236535329);
        a = GG(a, b, c, d, this.x[1], 5, -165796510);
        d = GG(d, a, b, c, this.x[6], 9, -1069501632);
        c = GG(c, d, a, b, this.x[11], 14, 643717713);
        b = GG(b, c, d, a, this.x[0], 20, -373897302);
        a = GG(a, b, c, d, this.x[5], 5, -701558691);
        d = GG(d, a, b, c, this.x[10], 9, 38016083);
        c = GG(c, d, a, b, this.x[15], 14, -660478335);
        b = GG(b, c, d, a, this.x[4], 20, -405537848);
        a = GG(a, b, c, d, this.x[9], 5, 568446438);
        d = GG(d, a, b, c, this.x[14], 9, -1019803690);
        c = GG(c, d, a, b, this.x[3], 14, -187363961);
        b = GG(b, c, d, a, this.x[8], 20, 1163531501);
        a = GG(a, b, c, d, this.x[13], 5, -1444681467);
        d = GG(d, a, b, c, this.x[2], 9, -51403784);
        c = GG(c, d, a, b, this.x[7], 14, 1735328473);
        b = GG(b, c, d, a, this.x[12], 20, -1926607734);
        a = HH(a, b, c, d, this.x[5], 4, -378558);
        d = HH(d, a, b, c, this.x[8], 11, -2022574463);
        c = HH(c, d, a, b, this.x[11], 16, 1839030562);
        b = HH(b, c, d, a, this.x[14], 23, -35309556);
        a = HH(a, b, c, d, this.x[1], 4, -1530992060);
        d = HH(d, a, b, c, this.x[4], 11, 1272893353);
        c = HH(c, d, a, b, this.x[7], 16, -155497632);
        b = HH(b, c, d, a, this.x[10], 23, -1094730640);
        a = HH(a, b, c, d, this.x[13], 4, 681279174);
        d = HH(d, a, b, c, this.x[0], 11, -358537222);
        c = HH(c, d, a, b, this.x[3], 16, -722521979);
        b = HH(b, c, d, a, this.x[6], 23, 76029189);
        a = HH(a, b, c, d, this.x[9], 4, -640364487);
        d = HH(d, a, b, c, this.x[12], 11, -421815835);
        c = HH(c, d, a, b, this.x[15], 16, 530742520);
        b = HH(b, c, d, a, this.x[2], 23, -995338651);
        a = II(a, b, c, d, this.x[0], 6, -198630844);
        d = II(d, a, b, c, this.x[7], 10, 1126891415);
        c = II(c, d, a, b, this.x[14], 15, -1416354905);
        b = II(b, c, d, a, this.x[5], 21, -57434055);
        a = II(a, b, c, d, this.x[12], 6, 1700485571);
        d = II(d, a, b, c, this.x[3], 10, -1894986606);
        c = II(c, d, a, b, this.x[10], 15, -1051523);
        b = II(b, c, d, a, this.x[1], 21, -2054922799);
        a = II(a, b, c, d, this.x[8], 6, 1873313359);
        d = II(d, a, b, c, this.x[15], 10, -30611744);
        c = II(c, d, a, b, this.x[6], 15, -1560198380);
        b = II(b, c, d, a, this.x[13], 21, 1309151649);
        a = II(a, b, c, d, this.x[4], 6, -145523070);
        d = II(d, a, b, c, this.x[11], 10, -1120210379);
        c = II(c, d, a, b, this.x[2], 15, 718787259);
        b = II(b, c, d, a, this.x[9], 21, -343485551);
        int[] var10000 = this.state;
        var10000[0] += a;
        var10000 = this.state;
        var10000[1] += b;
        var10000 = this.state;
        var10000[2] += c;
        var10000 = this.state;
        var10000[3] += d;
    }
}
