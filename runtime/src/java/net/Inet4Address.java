package java.net;

import java.io.ObjectStreamException;

public final class Inet4Address extends InetAddress {
    static final int INADDRSZ = 4;
    private static final long serialVersionUID = 3286316764910316507L;

    Inet4Address() {
        this.holder().hostName = null;
        this.holder().address = 0;
        this.holder().family = 1;
    }

    Inet4Address(String hostName, byte[] addr) {
        this.holder().hostName = hostName;
        this.holder().family = 1;
        if (addr != null && addr.length == 4) {
            int address = addr[3] & 255;
            address |= addr[2] << 8 & '\uff00';
            address |= addr[1] << 16 & 16711680;
            address |= addr[0] << 24 & -16777216;
            this.holder().address = address;
        }

        this.holder().originalHostName = hostName;
    }

    Inet4Address(String hostName, int address) {
        this.holder().hostName = hostName;
        this.holder().family = 1;
        this.holder().address = address;
        this.holder().originalHostName = hostName;
    }

    private Object writeReplace() throws ObjectStreamException {
        InetAddress inet = new InetAddress();
        inet.holder().hostName = this.holder().getHostName();
        inet.holder().address = this.holder().getAddress();
        inet.holder().family = 2;
        return inet;
    }

    public boolean isMulticastAddress() {
        return (this.holder().getAddress() & -268435456) == -536870912;
    }

    public boolean isAnyLocalAddress() {
        return this.holder().getAddress() == 0;
    }

    public boolean isLoopbackAddress() {
        byte[] byteAddr = this.getAddress();
        return byteAddr[0] == 127;
    }

    public boolean isLinkLocalAddress() {
        int address = this.holder().getAddress();
        return (address >>> 24 & 255) == 169 && (address >>> 16 & 255) == 254;
    }

    public boolean isSiteLocalAddress() {
        int address = this.holder().getAddress();
        return (address >>> 24 & 255) == 10 || (address >>> 24 & 255) == 172 && (address >>> 16 & 240) == 16 || (address >>> 24 & 255) == 192 && (address >>> 16 & 255) == 168;
    }

    public boolean isMCGlobal() {
        byte[] byteAddr = this.getAddress();
        return (byteAddr[0] & 255) >= 224 && (byteAddr[0] & 255) <= 238 && ((byteAddr[0] & 255) != 224 || byteAddr[1] != 0 || byteAddr[2] != 0);
    }

    public boolean isMCNodeLocal() {
        return false;
    }

    public boolean isMCLinkLocal() {
        int address = this.holder().getAddress();
        return (address >>> 24 & 255) == 224 && (address >>> 16 & 255) == 0 && (address >>> 8 & 255) == 0;
    }

    public boolean isMCSiteLocal() {
        int address = this.holder().getAddress();
        return (address >>> 24 & 255) == 239 && (address >>> 16 & 255) == 255;
    }

    public boolean isMCOrgLocal() {
        int address = this.holder().getAddress();
        return (address >>> 24 & 255) == 239 && (address >>> 16 & 255) >= 192 && (address >>> 16 & 255) <= 195;
    }

    public byte[] getAddress() {
        int address = this.holder().getAddress();
        byte[] addr = new byte[]{(byte)(address >>> 24 & 255), (byte)(address >>> 16 & 255), (byte)(address >>> 8 & 255), (byte)(address & 255)};
        return addr;
    }

    public String getHostAddress() {
        return numericToTextFormat(this.getAddress());
    }

    public int hashCode() {
        return this.holder().getAddress();
    }

    public boolean equals(Object obj) {
        return obj != null && obj instanceof Inet4Address && ((InetAddress)obj).holder().getAddress() == this.holder().getAddress();
    }

    static String numericToTextFormat(byte[] src) {
        return (src[0] & 255) + "." + (src[1] & 255) + "." + (src[2] & 255) + "." + (src[3] & 255);
    }

//    private static native void init();
//
//    static {
//        init();
//    }
}