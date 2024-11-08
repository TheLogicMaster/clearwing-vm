package java.net;

//import java.security.AccessController;
//import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterators;

public final class NetworkInterface {
    private String name;
    private String displayName;
    private int index;
    private InetAddress[] addrs;
    private InterfaceAddress[] bindings;
    private NetworkInterface[] childs;
    private NetworkInterface parent = null;
    private boolean virtual = false;
    private static final NetworkInterface defaultInterface;
    private static final int defaultIndex;

    NetworkInterface() {
    }

    NetworkInterface(String name, int index, InetAddress[] addrs) {
        this.name = name;
        this.index = index;
        this.addrs = addrs;
    }

    public String getName() {
        return this.name;
    }

    public Enumeration<InetAddress> getInetAddresses() {
        return enumerationFromArray(this.getCheckedInetAddresses());
    }

//    public Stream<InetAddress> inetAddresses() {
//        return streamFromArray(this.getCheckedInetAddresses());
//    }

    private InetAddress[] getCheckedInetAddresses() {
        InetAddress[] local_addrs = new InetAddress[this.addrs.length];
        boolean trusted = true;
//        SecurityManager sec = System.getSecurityManager();
//        if (sec != null) {
//            try {
//                sec.checkPermission(new NetPermission("getNetworkInformation"));
//            } catch (SecurityException var8) {
//                trusted = false;
//            }
//        }

        int i = 0;

        for(int j = 0; j < this.addrs.length; ++j) {
            try {
//                if (!trusted) {
//                    sec.checkConnect(this.addrs[j].getHostAddress(), -1);
//                }

                local_addrs[i++] = this.addrs[j];
            } catch (SecurityException var7) {
            }
        }

        return (InetAddress[])Arrays.copyOf(local_addrs, i);
    }

    public List<InterfaceAddress> getInterfaceAddresses() {
        List<InterfaceAddress> lst = new ArrayList(1);
        if (this.bindings != null) {
//            SecurityManager sec = System.getSecurityManager();

            for(int j = 0; j < this.bindings.length; ++j) {
                try {
//                    if (sec != null) {
//                        sec.checkConnect(this.bindings[j].getAddress().getHostAddress(), -1);
//                    }

                    lst.add(this.bindings[j]);
                } catch (SecurityException var5) {
                }
            }
        }

        return lst;
    }

    public Enumeration<NetworkInterface> getSubInterfaces() {
        return enumerationFromArray(this.childs);
    }

//    public Stream<NetworkInterface> subInterfaces() {
//        return streamFromArray(this.childs);
//    }

    public NetworkInterface getParent() {
        return this.parent;
    }

    public int getIndex() {
        return this.index;
    }

    public String getDisplayName() {
        return "".equals(this.displayName) ? null : this.displayName;
    }

    public static NetworkInterface getByName(String name) throws SocketException {
        if (name == null) {
            throw new NullPointerException();
        } else {
            return null;// getByName0(name);
        }
    }

    public static NetworkInterface getByIndex(int index) throws SocketException {
        if (index < 0) {
            throw new IllegalArgumentException("Interface index can't be negative");
        } else {
            return null;// getByIndex0(index);
        }
    }

    public static NetworkInterface getByInetAddress(InetAddress addr) throws SocketException {
        if (addr == null) {
            throw new NullPointerException();
        } else {
            if (addr instanceof Inet4Address) {
                Inet4Address inet4Address = (Inet4Address)addr;
                if (inet4Address.holder.family != 1) {
                    throw new IllegalArgumentException("invalid family type: " + inet4Address.holder.family);
                }
            } else {
                if (!(addr instanceof Inet6Address)) {
                    throw new IllegalArgumentException("invalid address type: " + addr);
                }

                Inet6Address inet6Address = (Inet6Address)addr;
                if (inet6Address.holder.family != 2) {
                    throw new IllegalArgumentException("invalid family type: " + inet6Address.holder.family);
                }
            }

            return null;// getByInetAddress0(addr);
        }
    }

    public static Enumeration<NetworkInterface> getNetworkInterfaces() throws SocketException {
        throw new SocketException("No network interfaces configured");
//        NetworkInterface[] netifs = getAll();
//        if (netifs != null && netifs.length > 0) {
//            return enumerationFromArray(netifs);
//        } else {
//            throw new SocketException("No network interfaces configured");
//        }
    }

//    public static Stream<NetworkInterface> networkInterfaces() throws SocketException {
//        throw new SocketException("No network interfaces configured");
//        NetworkInterface[] netifs = getAll();
//        if (netifs != null && netifs.length > 0) {
//            return streamFromArray(netifs);
//        } else {
//            throw new SocketException("No network interfaces configured");
//        }
//    }

    private static <T> Enumeration<T> enumerationFromArray(final T[] a) {
        return new Enumeration<T>() {
            int i = 0;

            public T nextElement() {
                if (this.i < a.length) {
                    return a[this.i++];
                } else {
                    throw new NoSuchElementException();
                }
            }

            public boolean hasMoreElements() {
                return this.i < a.length;
            }
        };
    }

//    private static <T> Stream<T> streamFromArray(T[] a) {
//        return StreamSupport.stream(Spliterators.spliterator(a, 1281), false);
//    }

//    private static native NetworkInterface[] getAll() throws SocketException;
//
//    private static native NetworkInterface getByName0(String var0) throws SocketException;
//
//    private static native NetworkInterface getByIndex0(int var0) throws SocketException;
//
//    private static native NetworkInterface getByInetAddress0(InetAddress var0) throws SocketException;

    public boolean isUp() throws SocketException {
        return false;// isUp0(this.name, this.index);
    }

    public boolean isLoopback() throws SocketException {
        return false;// isLoopback0(this.name, this.index);
    }

    public boolean isPointToPoint() throws SocketException {
        return false;// isP2P0(this.name, this.index);
    }

    public boolean supportsMulticast() throws SocketException {
        return false;// supportsMulticast0(this.name, this.index);
    }

    public byte[] getHardwareAddress() throws SocketException {
        return null;
//        SecurityManager sec = System.getSecurityManager();
//        if (sec != null) {
//            try {
//                sec.checkPermission(new NetPermission("getNetworkInformation"));
//            } catch (SecurityException var6) {
//                if (!this.getInetAddresses().hasMoreElements()) {
//                    return null;
//                }
//            }
//        }
//
//        InetAddress[] var2 = this.addrs;
//        int var3 = var2.length;
//
//        for(int var4 = 0; var4 < var3; ++var4) {
//            InetAddress addr = var2[var4];
//            if (addr instanceof Inet4Address) {
//                return getMacAddr0(((Inet4Address)addr).getAddress(), this.name, this.index);
//            }
//        }
//
//        return getMacAddr0((byte[])null, this.name, this.index);
    }

    public int getMTU() throws SocketException {
        return 0;// getMTU0(this.name, this.index);
    }

    public boolean isVirtual() {
        return this.virtual;
    }

//    private static native boolean isUp0(String var0, int var1) throws SocketException;
//
//    private static native boolean isLoopback0(String var0, int var1) throws SocketException;
//
//    private static native boolean supportsMulticast0(String var0, int var1) throws SocketException;
//
//    private static native boolean isP2P0(String var0, int var1) throws SocketException;
//
//    private static native byte[] getMacAddr0(byte[] var0, String var1, int var2) throws SocketException;
//
//    private static native int getMTU0(String var0, int var1) throws SocketException;

    public boolean equals(Object obj) {
        if (!(obj instanceof NetworkInterface)) {
            return false;
        } else {
            NetworkInterface that = (NetworkInterface)obj;
            if (this.name != null) {
                if (!this.name.equals(that.name)) {
                    return false;
                }
            } else if (that.name != null) {
                return false;
            }

            if (this.addrs == null) {
                return that.addrs == null;
            } else if (that.addrs == null) {
                return false;
            } else if (this.addrs.length != that.addrs.length) {
                return false;
            } else {
                InetAddress[] thatAddrs = that.addrs;
                int count = thatAddrs.length;

                for(int i = 0; i < count; ++i) {
                    boolean found = false;

                    for(int j = 0; j < count; ++j) {
                        if (this.addrs[i].equals(thatAddrs[j])) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    public int hashCode() {
        return this.name == null ? 0 : this.name.hashCode();
    }

    public String toString() {
        String result = "name:";
        result = result + (this.name == null ? "null" : this.name);
        if (this.displayName != null) {
            result = result + " (" + this.displayName + ")";
        }

        return result;
    }

//    private static native void init();

    static NetworkInterface getDefault() {
        return defaultInterface;
    }

    static {
//        AccessController.doPrivileged(new PrivilegedAction<Object>() {
//            public Void run() {
//                System.loadLibrary("net");
//                return null;
//            }
//        });
//        init();
        defaultInterface = DefaultInterface.getDefault();
        if (defaultInterface != null) {
            defaultIndex = defaultInterface.getIndex();
        } else {
            defaultIndex = 0;
        }

    }
}