package java.net;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class IPAddressUtil {
    private static final int INADDR4SZ = 4;
    private static final int INADDR16SZ = 16;
    private static final int INT16SZ = 2;
    private static final long L_IPV6_DELIMS = 0L;
    private static final long H_IPV6_DELIMS = 671088640L;
    private static final long L_GEN_DELIMS = -8935000888854970368L;
    private static final long H_GEN_DELIMS = 671088641L;
    private static final long L_AUTH_DELIMS = 288230376151711744L;
    private static final long H_AUTH_DELIMS = 671088641L;
    private static final long L_COLON = 288230376151711744L;
    private static final long H_COLON = 0L;
    private static final long L_SLASH = 140737488355328L;
    private static final long H_SLASH = 0L;
    private static final long L_BACKSLASH = 0L;
    private static final long H_BACKSLASH = 268435456L;
    private static final long L_NON_PRINTABLE = 4294967295L;
    private static final long H_NON_PRINTABLE = Long.MIN_VALUE;
    private static final long L_EXCLUDE = -8935000884560003073L;
    private static final long H_EXCLUDE = -9223372035915251711L;
    private static final char[] OTHERS = new char[]{'⁇', '⁈', '⁉', '℀', '℁', '℅', '℆', '⩴', '﹕', '﹖', '﹟', '﹫', '＃', '／', '：', '？', '＠'};
    private static final ConcurrentHashMap<InetAddress, InetAddress> cache = new ConcurrentHashMap();
    private static final int HEXADECIMAL = 16;
    private static final int DECIMAL = 10;
    private static final int OCTAL = 8;
    private static final int[] SUPPORTED_RADIXES = new int[]{16, 8, 10};
    private static final long CANT_PARSE_IN_RADIX = -1L;
    private static final long TERMINAL_PARSE_ERROR = -2L;
    private static final String ALLOW_AMBIGUOUS_IPADDRESS_LITERALS_SP = "jdk.net.allowAmbiguousIPAddressLiterals";
    private static final boolean ALLOW_AMBIGUOUS_IPADDRESS_LITERALS_SP_VALUE = false;//Boolean.valueOf(GetPropertyAction.privilegedGetProperty("jdk.net.allowAmbiguousIPAddressLiterals", "false"));

    public IPAddressUtil() {
    }

    public static byte[] textToNumericFormatV4(String src) {
        byte[] res = new byte[4];
        long tmpValue = 0L;
        int currByte = 0;
        boolean newOctet = true;
        int len = src.length();
        if (len != 0 && len <= 15) {
            for(int i = 0; i < len; ++i) {
                char c = src.charAt(i);
                if (c == '.') {
                    if (newOctet || tmpValue < 0L || tmpValue > 255L || currByte == 3) {
                        return null;
                    }

                    res[currByte++] = (byte)((int)(tmpValue & 255L));
                    tmpValue = 0L;
                    newOctet = true;
                } else {
                    int digit = digit(c, 10);
                    if (digit < 0) {
                        return null;
                    }

                    tmpValue *= 10L;
                    tmpValue += (long)digit;
                    newOctet = false;
                }
            }

            if (!newOctet && tmpValue >= 0L && tmpValue < 1L << (4 - currByte) * 8) {
                switch (currByte) {
                    case 0:
                        res[0] = (byte)((int)(tmpValue >> 24 & 255L));
                    case 1:
                        res[1] = (byte)((int)(tmpValue >> 16 & 255L));
                    case 2:
                        res[2] = (byte)((int)(tmpValue >> 8 & 255L));
                    case 3:
                        res[3] = (byte)((int)(tmpValue >> 0 & 255L));
                    default:
                        return res;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static byte[] validateNumericFormatV4(String src) {
        byte[] parsedBytes = textToNumericFormatV4(src);
        if (!ALLOW_AMBIGUOUS_IPADDRESS_LITERALS_SP_VALUE && parsedBytes == null && isBsdParsableV4(src)) {
            throw new IllegalArgumentException("Invalid IP address literal: " + src);
        } else {
            return parsedBytes;
        }
    }

    public static byte[] textToNumericFormatV6(String src) {
        if (src.length() < 2) {
            return null;
        } else {
            char[] srcb = src.toCharArray();
            byte[] dst = new byte[16];
            int srcb_length = srcb.length;
            int pc = src.indexOf(37);
            if (pc == srcb_length - 1) {
                return null;
            } else {
                if (pc != -1) {
                    srcb_length = pc;
                }

                int colonp = -1;
                int i = 0;
                int j = 0;
                if (srcb[i] == ':') {
                    ++i;
                    if (srcb[i] != ':') {
                        return null;
                    }
                }

                int curtok = i;
                boolean saw_xdigit = false;
                int val = 0;

                while(true) {
                    int n;
                    while(i < srcb_length) {
                        char ch = srcb[i++];
                        n = digit(ch, 16);
                        if (n != -1) {
                            val <<= 4;
                            val |= n;
                            if (val > 65535) {
                                return null;
                            }

                            saw_xdigit = true;
                        } else {
                            if (ch != ':') {
                                if (ch == '.' && j + 4 <= 16) {
                                    String ia4 = src.substring(curtok, srcb_length);
                                    int dot_count = 0;

                                    for(int index = 0; (index = ia4.indexOf(46, index)) != -1; ++index) {
                                        ++dot_count;
                                    }

                                    if (dot_count != 3) {
                                        return null;
                                    }

                                    byte[] v4addr = textToNumericFormatV4(ia4);
                                    if (v4addr == null) {
                                        return null;
                                    }

                                    for(int k = 0; k < 4; ++k) {
                                        dst[j++] = v4addr[k];
                                    }

                                    saw_xdigit = false;
                                    break;
                                }

                                return null;
                            }

                            curtok = i;
                            if (!saw_xdigit) {
                                if (colonp != -1) {
                                    return null;
                                }

                                colonp = j;
                            } else {
                                if (i == srcb_length) {
                                    return null;
                                }

                                if (j + 2 > 16) {
                                    return null;
                                }

                                dst[j++] = (byte)(val >> 8 & 255);
                                dst[j++] = (byte)(val & 255);
                                saw_xdigit = false;
                                val = 0;
                            }
                        }
                    }

                    if (saw_xdigit) {
                        if (j + 2 > 16) {
                            return null;
                        }

                        dst[j++] = (byte)(val >> 8 & 255);
                        dst[j++] = (byte)(val & 255);
                    }

                    if (colonp != -1) {
                        n = j - colonp;
                        if (j == 16) {
                            return null;
                        }

                        for(i = 1; i <= n; ++i) {
                            dst[16 - i] = dst[colonp + n - i];
                            dst[colonp + n - i] = 0;
                        }

                        j = 16;
                    }

                    if (j != 16) {
                        return null;
                    }

                    byte[] newdst = convertFromIPv4MappedAddress(dst);
                    if (newdst != null) {
                        return newdst;
                    }

                    return dst;
                }
            }
        }
    }

    public static boolean isIPv4LiteralAddress(String src) {
        return textToNumericFormatV4(src) != null;
    }

    public static boolean isIPv6LiteralAddress(String src) {
        return textToNumericFormatV6(src) != null;
    }

    public static byte[] convertFromIPv4MappedAddress(byte[] addr) {
        if (isIPv4MappedAddress(addr)) {
            byte[] newAddr = new byte[4];
            System.arraycopy(addr, 12, newAddr, 0, 4);
            return newAddr;
        } else {
            return null;
        }
    }

    private static boolean isIPv4MappedAddress(byte[] addr) {
        if (addr.length < 16) {
            return false;
        } else {
            return addr[0] == 0 && addr[1] == 0 && addr[2] == 0 && addr[3] == 0 && addr[4] == 0 && addr[5] == 0 && addr[6] == 0 && addr[7] == 0 && addr[8] == 0 && addr[9] == 0 && addr[10] == -1 && addr[11] == -1;
        }
    }

    public static boolean match(char c, long lowMask, long highMask) {
        if (c < '@') {
            return (1L << c & lowMask) != 0L;
        } else if (c < 128) {
            return (1L << c - 64 & highMask) != 0L;
        } else {
            return false;
        }
    }

    public static int scan(String s, long lowMask, long highMask) {
        int i = -1;
        int len;
        if (s != null && (len = s.length()) != 0) {
            boolean match = false;

            do {
                ++i;
            } while(i < len && !(match = match(s.charAt(i), lowMask, highMask)));

            return match ? i : -1;
        } else {
            return -1;
        }
    }

    public static int scan(String s, long lowMask, long highMask, char[] others) {
        int i = -1;
        int len;
        if (s != null && (len = s.length()) != 0) {
            boolean match = false;
            char c0 = others[0];

            while(true) {
                ++i;
                char c;
                if (i >= len || (match = match(c = s.charAt(i), lowMask, highMask))) {
                    break;
                }

                if (c >= c0 && Arrays.binarySearch(others, c) > -1) {
                    match = true;
                    break;
                }
            }

            return match ? i : -1;
        } else {
            return -1;
        }
    }

    private static String describeChar(char c) {
        if (c >= ' ' && c != 127) {
            return c == '\\' ? "'\\'" : "'" + c + "'";
        } else if (c == '\n') {
            return "LF";
        } else {
            return c == '\r' ? "CR" : "control char (code=" + c + ")";
        }
    }

    private static String checkUserInfo(String str) {
        int index = scan(str, -9223231260711714817L, -9223372035915251711L);
        return index >= 0 ? "Illegal character found in user-info: " + describeChar(str.charAt(index)) : null;
    }

    private static String checkHost(String str) {
        int index;
        if (str.startsWith("[") && str.endsWith("]")) {
            str = str.substring(1, str.length() - 1);
            if (isIPv6LiteralAddress(str)) {
                index = str.indexOf(37);
                if (index >= 0) {
                    index = scan(str = str.substring(index), 4294967295L, -9223372036183687168L);
                    if (index >= 0) {
                        return "Illegal character found in IPv6 scoped address: " + describeChar(str.charAt(index));
                    }
                }

                return null;
            } else {
                return "Unrecognized IPv6 address format";
            }
        } else {
            index = scan(str, -8935000884560003073L, -9223372035915251711L);
            return index >= 0 ? "Illegal character found in host: " + describeChar(str.charAt(index)) : null;
        }
    }

    private static String checkAuth(String str) {
        int index = scan(str, -9223231260711714817L, -9223372036586340352L);
        return index >= 0 ? "Illegal character found in authority: " + describeChar(str.charAt(index)) : null;
    }

    public static String checkAuthority(URL url) {
        return null;
//        if (url == null) {
//            return null;
//        } else {
//            String s;
//            String u;
//            if ((s = checkUserInfo(u = url.getUserInfo())) != null) {
//                return s;
//            } else {
//                String h;
//                if ((s = checkHost(h = url.getHost())) != null) {
//                    return s;
//                } else {
//                    return h == null && u == null ? checkAuth(url.getAuthority()) : null;
//                }
//            }
//        }
    }

    public static String checkExternalForm(URL url) {
        return null;
//        if (url == null) {
//            return null;
//        } else {
//            String s;
//            int index = scan(s = url.getUserInfo(), 140741783322623L, Long.MIN_VALUE);
//            if (index >= 0) {
//                return "Illegal character found in authority: " + describeChar(s.charAt(index));
//            } else {
//                return (s = checkHostString(url.getHost())) != null ? s : null;
//            }
//        }
    }

    public static String checkHostString(String host) {
        if (host == null) {
            return null;
        } else {
            int index = scan(host, 140741783322623L, Long.MIN_VALUE, OTHERS);
            return index >= 0 ? "Illegal character found in host: " + describeChar(host.charAt(index)) : null;
        }
    }

    public static InetAddress toScopedAddress(InetAddress address) throws SocketException {
        return address;
//        if (address instanceof Inet6Address && address.isLinkLocalAddress() && ((Inet6Address)address).getScopeId() == 0) {
//            InetAddress cached = null;
//
//            try {
//                cached = (InetAddress)cache.computeIfAbsent(address, (k) -> {
//                    return findScopedAddress(k);
//                });
//            } catch (UncheckedIOException var3) {
//                UncheckedIOException e = var3;
//                throw (SocketException)e.getCause();
//            }
//
//            return cached != null ? cached : address;
//        } else {
//            return address;
//        }
    }

    public static InetSocketAddress toScopedAddress(InetSocketAddress address) throws SocketException {
        InetAddress orig = address.getAddress();
        InetAddress addr;
        return (addr = toScopedAddress(orig)) == orig ? address : new InetSocketAddress(addr, address.getPort());
    }

    private static InetAddress findScopedAddress(InetAddress address) {
        return address;
//        PrivilegedExceptionAction<List<InetAddress>> pa = () -> {
//            return (List)NetworkInterface.networkInterfaces().flatMap(NetworkInterface::inetAddresses).filter((a) -> {
//                return a instanceof Inet6Address && address.equals(a) && ((Inet6Address)a).getScopeId() != 0;
//            }).collect(Collectors.toList());
//        };
//
//        try {
//            List<InetAddress> result = (List) AccessController.doPrivileged(pa);
//            int sz = result.size();
//            if (sz == 0) {
//                return null;
//            } else if (sz > 1) {
//                throw new UncheckedIOException(new SocketException("Duplicate link local addresses: must specify scope-id"));
//            } else {
//                return (InetAddress)result.get(0);
//            }
//        } catch (PrivilegedActionException var4) {
//            return null;
//        }
    }

    public static int digit(char ch, int radix) {
        return ALLOW_AMBIGUOUS_IPADDRESS_LITERALS_SP_VALUE ? Character.digit(ch, radix) : parseAsciiDigit(ch, radix);
    }

    public static boolean isBsdParsableV4(String input) {
        char firstSymbol = input.charAt(0);
        if (parseAsciiDigit(firstSymbol, 10) == -1) {
            return false;
        } else {
            char lastSymbol = input.charAt(input.length() - 1);
            if (lastSymbol != '.' && parseAsciiHexDigit(lastSymbol) != -1) {
                CharBuffer charBuffer = CharBuffer.wrap(input);
                int fieldNumber = 0;

                long fieldValue;
                do {
                    if (!charBuffer.hasRemaining()) {
                        return true;
                    }

                    fieldValue = -1L;
                    int[] var7 = SUPPORTED_RADIXES;
                    int var8 = var7.length;

                    for(int var9 = 0; var9 < var8; ++var9) {
                        int radix = var7[var9];
                        fieldValue = parseV4FieldBsd(radix, charBuffer, fieldNumber);
                        if (fieldValue >= 0L) {
                            ++fieldNumber;
                            break;
                        }

                        if (fieldValue == -2L) {
                            return false;
                        }
                    }
                } while(fieldValue >= 0L);

                return false;
            } else {
                return false;
            }
        }
    }

    private static long parseV4FieldBsd(int radix, CharBuffer buffer, int fieldNumber) {
        int initialPos = buffer.position();
        long val = 0L;
        int digitsCount = 0;
        if (!checkPrefix(buffer, radix)) {
            val = -1L;
        }

        boolean dotSeen = false;

        while(buffer.hasRemaining() && val != -1L && !dotSeen) {
            char c = buffer.get();
            if (c == '.') {
                dotSeen = true;
                if (fieldNumber == 3) {
                    return -2L;
                }

                if (digitsCount == 0) {
                    return -2L;
                }

                if (val > 255L) {
                    return -2L;
                }
            } else {
                int dv = parseAsciiDigit(c, radix);
                if (dv < 0) {
                    return -2L;
                }

                ++digitsCount;
                val *= (long)radix;
                val += (long)dv;
            }
        }

        if (val == -1L) {
            buffer.position(initialPos);
        } else if (!dotSeen) {
            long maxValue = (1L << (4 - fieldNumber) * 8) - 1L;
            if (val > maxValue) {
                return -2L;
            }
        }

        return val;
    }

    private static boolean checkPrefix(CharBuffer buffer, int radix) {
        switch (radix) {
            case 8:
                return isOctalFieldStart(buffer);
            case 10:
                return isDecimalFieldStart(buffer);
            case 16:
                return isHexFieldStart(buffer);
            default:
                throw new AssertionError("Not supported radix");
        }
    }

    private static boolean isOctalFieldStart(CharBuffer cb) {
        if (cb.remaining() < 2) {
            return false;
        } else {
            int position = cb.position();
            char first = cb.get();
            char second = cb.get();
            boolean isOctalPrefix = first == '0' && second != '.';
            if (isOctalPrefix) {
                cb.position(position + 1);
            }

            return isOctalPrefix;
        }
    }

    private static boolean isDecimalFieldStart(CharBuffer cb) {
        return cb.hasRemaining();
    }

    private static boolean isHexFieldStart(CharBuffer cb) {
        if (cb.remaining() < 2) {
            return false;
        } else {
            char first = cb.get();
            char second = cb.get();
            return first == '0' && (second == 'x' || second == 'X');
        }
    }

    public static int parseAsciiDigit(char c, int radix) {
        assert radix == 8 || radix == 10 || radix == 16;

        if (radix == 16) {
            return parseAsciiHexDigit(c);
        } else {
            int val = c - 48;
            return val >= 0 && val < radix ? val : -1;
        }
    }

    private static int parseAsciiHexDigit(char digit) {
        char c = Character.toLowerCase(digit);
        return c >= 'a' && c <= 'f' ? c - 97 + 10 : parseAsciiDigit(c, 10);
    }
}
