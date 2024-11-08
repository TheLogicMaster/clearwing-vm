package java.net;

public class InterfaceAddress {
    private InetAddress address = null;
    private Inet4Address broadcast = null;
    private short maskLength = 0;

    InterfaceAddress() {
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public InetAddress getBroadcast() {
        return this.broadcast;
    }

    public short getNetworkPrefixLength() {
        return this.maskLength;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof InterfaceAddress)) {
            return false;
        } else {
            InterfaceAddress cmp = (InterfaceAddress)obj;
            if (this.address == null) {
                if (cmp.address != null) {
                    return false;
                }
            } else if (!this.address.equals(cmp.address)) {
                return false;
            }

            label27: {
                if (this.broadcast == null) {
                    if (cmp.broadcast == null) {
                        break label27;
                    }
                } else if (this.broadcast.equals(cmp.broadcast)) {
                    break label27;
                }

                return false;
            }

            if (this.maskLength != cmp.maskLength) {
                return false;
            } else {
                return true;
            }
        }
    }

    public int hashCode() {
        return this.address.hashCode() + (this.broadcast != null ? this.broadcast.hashCode() : 0) + this.maskLength;
    }

    public String toString() {
        return this.address + "/" + this.maskLength + " [" + this.broadcast + "]";
    }
}