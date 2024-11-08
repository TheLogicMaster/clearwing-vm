//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public abstract class HttpURLConnection extends URLConnection {
    protected String method = "GET";
    protected int chunkLength = -1;
    protected int fixedContentLength = -1;
    protected long fixedContentLengthLong = -1L;
    private static final int DEFAULT_CHUNK_SIZE = 4096;
    protected int responseCode = -1;
    protected String responseMessage = null;
    private static boolean followRedirects = true;
    protected boolean instanceFollowRedirects;
    private static final String[] methods = new String[]{"GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE"};
    public static final int HTTP_OK = 200;
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_ACCEPTED = 202;
    public static final int HTTP_NOT_AUTHORITATIVE = 203;
    public static final int HTTP_NO_CONTENT = 204;
    public static final int HTTP_RESET = 205;
    public static final int HTTP_PARTIAL = 206;
    public static final int HTTP_MULT_CHOICE = 300;
    public static final int HTTP_MOVED_PERM = 301;
    public static final int HTTP_MOVED_TEMP = 302;
    public static final int HTTP_SEE_OTHER = 303;
    public static final int HTTP_NOT_MODIFIED = 304;
    public static final int HTTP_USE_PROXY = 305;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_PAYMENT_REQUIRED = 402;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_BAD_METHOD = 405;
    public static final int HTTP_NOT_ACCEPTABLE = 406;
    public static final int HTTP_PROXY_AUTH = 407;
    public static final int HTTP_CLIENT_TIMEOUT = 408;
    public static final int HTTP_CONFLICT = 409;
    public static final int HTTP_GONE = 410;
    public static final int HTTP_LENGTH_REQUIRED = 411;
    public static final int HTTP_PRECON_FAILED = 412;
    public static final int HTTP_ENTITY_TOO_LARGE = 413;
    public static final int HTTP_REQ_TOO_LONG = 414;
    public static final int HTTP_UNSUPPORTED_TYPE = 415;
    /** @deprecated */
    @Deprecated
    public static final int HTTP_SERVER_ERROR = 500;
    public static final int HTTP_INTERNAL_ERROR = 500;
    public static final int HTTP_NOT_IMPLEMENTED = 501;
    public static final int HTTP_BAD_GATEWAY = 502;
    public static final int HTTP_UNAVAILABLE = 503;
    public static final int HTTP_GATEWAY_TIMEOUT = 504;
    public static final int HTTP_VERSION = 505;

    public void setAuthenticator(Authenticator auth) {
        throw new UnsupportedOperationException("Supplying an authenticator is not supported by " + this.getClass());
    }

    public String getHeaderFieldKey(int n) {
        return null;
    }

    public void setFixedLengthStreamingMode(int contentLength) {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        } else if (this.chunkLength != -1) {
            throw new IllegalStateException("Chunked encoding streaming mode set");
        } else if (contentLength < 0) {
            throw new IllegalArgumentException("invalid content length");
        } else {
            this.fixedContentLength = contentLength;
        }
    }

    public void setFixedLengthStreamingMode(long contentLength) {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        } else if (this.chunkLength != -1) {
            throw new IllegalStateException("Chunked encoding streaming mode set");
        } else if (contentLength < 0L) {
            throw new IllegalArgumentException("invalid content length");
        } else {
            this.fixedContentLengthLong = contentLength;
        }
    }

    public void setChunkedStreamingMode(int chunklen) {
        if (this.connected) {
            throw new IllegalStateException("Can't set streaming mode: already connected");
        } else if (this.fixedContentLength == -1 && this.fixedContentLengthLong == -1L) {
            this.chunkLength = chunklen <= 0 ? 4096 : chunklen;
        } else {
            throw new IllegalStateException("Fixed length streaming mode set");
        }
    }

    public String getHeaderField(int n) {
        return null;
    }

    protected HttpURLConnection(URL u) {
        super(u);
        this.instanceFollowRedirects = followRedirects;
    }

    public static void setFollowRedirects(boolean set) {
//        SecurityManager sec = System.getSecurityManager();
//        if (sec != null) {
//            sec.checkSetFactory();
//        }

        followRedirects = set;
    }

    public static boolean getFollowRedirects() {
        return followRedirects;
    }

    public void setInstanceFollowRedirects(boolean followRedirects) {
        this.instanceFollowRedirects = followRedirects;
    }

    public boolean getInstanceFollowRedirects() {
        return this.instanceFollowRedirects;
    }

    public void setRequestMethod(String method) throws ProtocolException {
        if (this.connected) {
            throw new ProtocolException("Can't reset method: already connected");
        } else {
            for(int i = 0; i < methods.length; ++i) {
                if (methods[i].equals(method)) {
                    if (method.equals("TRACE")) {
//                        SecurityManager s = System.getSecurityManager();
//                        if (s != null) {
//                            s.checkPermission(new NetPermission("allowHttpTrace"));
//                        }
                    }

                    this.method = method;
                    return;
                }
            }

            throw new ProtocolException("Invalid HTTP method: " + method);
        }
    }

    public String getRequestMethod() {
        return this.method;
    }

    public int getResponseCode() throws IOException {
        if (this.responseCode != -1) {
            return this.responseCode;
        } else {
            Exception exc = null;

            try {
                this.getInputStream();
            } catch (Exception var6) {
                Exception e = var6;
                exc = e;
            }

            String statusLine = this.getHeaderField(0);
            if (statusLine == null) {
                if (exc != null) {
                    if (exc instanceof RuntimeException) {
                        throw (RuntimeException)exc;
                    } else {
                        throw (IOException)exc;
                    }
                } else {
                    return -1;
                }
            } else {
                if (statusLine.startsWith("HTTP/1.")) {
                    int codePos = statusLine.indexOf(32);
                    if (codePos > 0) {
                        int phrasePos = statusLine.indexOf(32, codePos + 1);
                        if (phrasePos > 0 && phrasePos < statusLine.length()) {
                            this.responseMessage = statusLine.substring(phrasePos + 1);
                        }

                        if (phrasePos < 0) {
                            phrasePos = statusLine.length();
                        }

                        try {
                            this.responseCode = Integer.parseInt(statusLine.substring(codePos + 1, phrasePos));
                            return this.responseCode;
                        } catch (NumberFormatException var7) {
                        }
                    }
                }

                return -1;
            }
        }
    }

    public String getResponseMessage() throws IOException {
        this.getResponseCode();
        return this.responseMessage;
    }

    public long getHeaderFieldDate(String name, long Default) {
        String dateString = this.getHeaderField(name);

        try {
            if (dateString.indexOf("GMT") == -1) {
                dateString = dateString + " GMT";
            }

            return Date.parse(dateString);
        } catch (Exception var6) {
            return Default;
        }
    }

    public abstract void disconnect();

    public abstract boolean usingProxy();

//    public Permission getPermission() throws IOException {
//        int port = this.url.getPort();
//        port = port < 0 ? 80 : port;
//        String host = this.url.getHost() + ":" + port;
//        Permission permission = new SocketPermission(host, "connect");
//        return permission;
//    }

    public InputStream getErrorStream() {
        return null;
    }
}
