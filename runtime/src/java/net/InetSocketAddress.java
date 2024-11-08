package java.net;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import jdk.internal.misc.Unsafe;

public class InetSocketAddress extends SocketAddress {
    private final transient InetSocketAddressHolder holder;
    private static final long serialVersionUID = 5076001401234631237L;
    private static final ObjectStreamField[] serialPersistentFields;
//    private static final Unsafe UNSAFE;
//    private static final long FIELDS_OFFSET;

    private static int checkPort(int port) {
        if (port >= 0 && port <= 65535) {
            return port;
        } else {
            throw new IllegalArgumentException("port out of range:" + port);
        }
    }

    private static String checkHost(String hostname) {
        if (hostname == null) {
            throw new IllegalArgumentException("hostname can't be null");
        } else {
            return hostname;
        }
    }

    public InetSocketAddress(int port) {
        this(InetAddress.anyLocalAddress(), port);
    }

    public InetSocketAddress(InetAddress addr, int port) {
        this.holder = new InetSocketAddressHolder((String)null, addr == null ? InetAddress.anyLocalAddress() : addr, checkPort(port));
    }

    public InetSocketAddress(String hostname, int port) {
        checkHost(hostname);
        InetAddress addr = null;
        String host = null;

        try {
            addr = InetAddress.getByName(hostname);
        } catch (UnknownHostException var6) {
            host = hostname;
        }

        this.holder = new InetSocketAddressHolder(host, addr, checkPort(port));
    }

    private InetSocketAddress(int port, String hostname) {
        this.holder = new InetSocketAddressHolder(hostname, (InetAddress)null, port);
    }

    public static InetSocketAddress createUnresolved(String host, int port) {
        return new InetSocketAddress(checkPort(port), checkHost(host));
    }

//    private void writeObject(ObjectOutputStream out) throws IOException {
//        ObjectOutputStream.PutField pfields = out.putFields();
//        pfields.put("hostname", this.holder.hostname);
//        pfields.put("addr", this.holder.addr);
//        pfields.put("port", this.holder.port);
//        out.writeFields();
//    }
//
//    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
//        ObjectInputStream.GetField oisFields = in.readFields();
//        String oisHostname = (String)oisFields.get("hostname", (Object)null);
//        InetAddress oisAddr = (InetAddress)oisFields.get("addr", (Object)null);
//        int oisPort = oisFields.get("port", -1);
//        checkPort(oisPort);
//        if (oisHostname == null && oisAddr == null) {
//            throw new InvalidObjectException("hostname and addr can't both be null");
//        } else {
//            InetSocketAddressHolder h = new InetSocketAddressHolder(oisHostname, oisAddr, oisPort);
//            UNSAFE.putObject(this, FIELDS_OFFSET, h);
//        }
//    }

    private void readObjectNoData() throws ObjectStreamException {
        throw new InvalidObjectException("Stream data required");
    }

    public final int getPort() {
        return this.holder.getPort();
    }

    public final InetAddress getAddress() {
        return this.holder.getAddress();
    }

    public final String getHostName() {
        return this.holder.getHostName();
    }

    public final String getHostString() {
        return this.holder.getHostString();
    }

    public final boolean isUnresolved() {
        return this.holder.isUnresolved();
    }

    public String toString() {
        return this.holder.toString();
    }

    public final boolean equals(Object obj) {
        return obj != null && obj instanceof InetSocketAddress ? this.holder.equals(((InetSocketAddress)obj).holder) : false;
    }

    public final int hashCode() {
        return this.holder.hashCode();
    }

    static {
        serialPersistentFields = new ObjectStreamField[]{new ObjectStreamField("hostname", String.class), new ObjectStreamField("addr", InetAddress.class), new ObjectStreamField("port", Integer.TYPE)};
//        UNSAFE = Unsafe.getUnsafe();
//        FIELDS_OFFSET = UNSAFE.objectFieldOffset(InetSocketAddress.class, "holder");
    }

    private static class InetSocketAddressHolder {
        private String hostname;
        private InetAddress addr;
        private int port;

        private InetSocketAddressHolder(String hostname, InetAddress addr, int port) {
            this.hostname = hostname;
            this.addr = addr;
            this.port = port;
        }

        private int getPort() {
            return this.port;
        }

        private InetAddress getAddress() {
            return this.addr;
        }

        private String getHostName() {
            if (this.hostname != null) {
                return this.hostname;
            } else {
                return this.addr != null ? this.addr.getHostName() : null;
            }
        }

        private String getHostString() {
            if (this.hostname != null) {
                return this.hostname;
            } else if (this.addr != null) {
                return this.addr.holder().getHostName() != null ? this.addr.holder().getHostName() : this.addr.getHostAddress();
            } else {
                return null;
            }
        }

        private boolean isUnresolved() {
            return this.addr == null;
        }

        public String toString() {
            return this.isUnresolved() ? this.hostname + ":" + this.port : this.addr.toString() + ":" + this.port;
        }

        public final boolean equals(Object obj) {
            if (obj != null && obj instanceof InetSocketAddressHolder) {
                InetSocketAddressHolder that = (InetSocketAddressHolder)obj;
                boolean sameIP;
                if (this.addr != null) {
                    sameIP = this.addr.equals(that.addr);
                } else if (this.hostname != null) {
                    sameIP = that.addr == null && this.hostname.equalsIgnoreCase(that.hostname);
                } else {
                    sameIP = that.addr == null && that.hostname == null;
                }

                return sameIP && this.port == that.port;
            } else {
                return false;
            }
        }

        public final int hashCode() {
            if (this.addr != null) {
                return this.addr.hashCode() + this.port;
            } else {
                return this.hostname != null ? this.hostname.toLowerCase().hashCode() + this.port : this.port;
            }
        }
    }
}