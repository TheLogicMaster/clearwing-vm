package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Permission;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public abstract class URLConnection {
    protected URL url;
//    ContentHandler defaultHandler = new DefaultContentHandler();
    protected long ifModifiedSince;
    protected boolean useCaches;
    protected boolean connected;
    protected boolean doOutput;
    protected boolean doInput;
    protected boolean allowUserInteraction;
    static Hashtable<String, Object> contentHandlers = new Hashtable();
    
    public static FileNameMap getFileNameMap() { return null; }

    public static void setFileNameMap(FileNameMap map) {}

    public abstract void connect() throws IOException;

    public void setConnectTimeout(int timeout) {}

    public int getConnectTimeout() { return 0; }

    public void setReadTimeout(int timeout) {}
    
    public int getReadTimeout() { return 0; }

    protected URLConnection(URL url) {}

    public URL getURL() { return null; }

    public int getContentLength() { return 0; }

    public long getContentLengthLong() { return 0; }

    public String getContentType() {
        return getHeaderField("content-type");
    }

    public String getContentEncoding() {
        return getHeaderField("content-encoding");
    }

    public long getExpiration() {
        return getHeaderFieldDate("expires", 0);
    }

    public long getDate() {
        return getHeaderFieldDate("date", 0);
    }

    public long getLastModified() {
        return getHeaderFieldDate("last-modified", 0);
    }

    public String getHeaderField(String name) {
        return null;
    }

    public Map<String,List<String>> getHeaderFields() { return null; }

    public int getHeaderFieldInt(String name, int Default) {
        String value = getHeaderField(name);
        try {
            return Integer.parseInt(value);
        } catch (Exception e) { }
        return Default;
    }

    public long getHeaderFieldLong(String name, long Default) {
        String value = getHeaderField(name);
        try {
            return Long.parseLong(value);
        } catch (Exception e) { }
        return Default;
    }

    @SuppressWarnings("deprecation")
    public long getHeaderFieldDate(String name, long Default) { return 0; }

    public String getHeaderFieldKey(int n) { return null; }

    public String getHeaderField(int n) { return null; }

    public Object getContent() throws IOException { return null; }

    public Object getContent(Class<?>[] classes) throws IOException { return null; }

    public Permission getPermission() throws IOException { return null; }

    public InputStream getInputStream() throws IOException { return null; }

    public OutputStream getOutputStream() throws IOException { return null; }

    public String toString() { return null; }

    public void setDoInput(boolean doinput) {}

    public boolean getDoInput() { return false; }

    public void setDoOutput(boolean dooutput) {}

    public boolean getDoOutput() { return false; }

    public void setAllowUserInteraction(boolean allowuserinteraction) {}

    public boolean getAllowUserInteraction() { return false; }

    public static void setDefaultAllowUserInteraction(boolean defaultallowuserinteraction) {}

    public static boolean getDefaultAllowUserInteraction() { return false; }

    public void setUseCaches(boolean usecaches) {}

    public boolean getUseCaches() { return false; }

    public void setIfModifiedSince(long ifmodifiedsince) {}

    public long getIfModifiedSince() { return 0; }

    public boolean getDefaultUseCaches() { return false; }

    public void setDefaultUseCaches(boolean defaultusecaches) {}

    public static void setDefaultUseCaches(String protocol, boolean defaultVal) {}

    public static boolean getDefaultUseCaches(String protocol) { return false; }

    public void setRequestProperty(String key, String value) {}

    public void addRequestProperty(String key, String value) {}

    public String getRequestProperty(String key)  { return null; }

    public Map<String, List<String>> getRequestProperties() { return null; }

    @Deprecated
    public static void setDefaultRequestProperty(String key, String value) {}

    @Deprecated
    public static String getDefaultRequestProperty(String key) { return null; }

    public static synchronized void setContentHandlerFactory(ContentHandlerFactory fac) {}

    public static String guessContentTypeFromName(String fname) { return null; }

    public static String guessContentTypeFromStream(InputStream is) throws IOException { return null; }
}
