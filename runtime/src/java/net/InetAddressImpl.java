package java.net;

import java.io.IOException;

interface InetAddressImpl {
    String getLocalHostName() throws UnknownHostException;

    InetAddress[] lookupAllHostAddr(String var1) throws UnknownHostException;

    String getHostByAddr(byte[] var1) throws UnknownHostException;

    InetAddress anyLocalAddress();

    InetAddress loopbackAddress();

    boolean isReachable(InetAddress var1, int var2, NetworkInterface var3, int var4) throws IOException;
}
