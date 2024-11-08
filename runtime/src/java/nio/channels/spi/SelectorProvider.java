package java.nio.channels.spi;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import sun.nio.ch.DefaultSelectorProvider;

public abstract class SelectorProvider {
    private static final Object lock = new Object();
    private static SelectorProvider provider = null;

    private static Void checkPermission() {
//        SecurityManager sm = System.getSecurityManager();
//        if (sm != null) {
//            sm.checkPermission(new RuntimePermission("selectorProvider"));
//        }

        return null;
    }

    private SelectorProvider(Void ignore) {
    }

    protected SelectorProvider() {
        this(checkPermission());
    }

    private static boolean loadProviderFromProperty() {
        return false;
//        String cn = System.getProperty("java.nio.channels.spi.SelectorProvider");
//        if (cn == null) {
//            return false;
//        } else {
//            try {
//                Object tmp = Class.forName(cn, true, ClassLoader.getSystemClassLoader()).newInstance();
//                provider = (SelectorProvider)tmp;
//                return true;
//            } catch (ClassNotFoundException var2) {
//                ClassNotFoundException x = var2;
//                throw new ServiceConfigurationError((String)null, x);
//            } catch (IllegalAccessException var3) {
//                IllegalAccessException x = var3;
//                throw new ServiceConfigurationError((String)null, x);
//            } catch (InstantiationException var4) {
//                InstantiationException x = var4;
//                throw new ServiceConfigurationError((String)null, x);
//            } catch (SecurityException var5) {
//                SecurityException x = var5;
//                throw new ServiceConfigurationError((String)null, x);
//            }
//        }
    }

//    private static boolean loadProviderAsService() {
//        ServiceLoader<SelectorProvider> sl = ServiceLoader.load(SelectorProvider.class, ClassLoader.getSystemClassLoader());
//        Iterator<SelectorProvider> i = sl.iterator();
//
//        while(true) {
//            try {
//                if (!i.hasNext()) {
//                    return false;
//                }
//
//                provider = (SelectorProvider)i.next();
//                return true;
//            } catch (ServiceConfigurationError var3) {
//                ServiceConfigurationError sce = var3;
//                if (!(sce.getCause() instanceof SecurityException)) {
//                    throw sce;
//                }
//            }
//        }
//    }

    public static SelectorProvider provider() {
        return null;
//        synchronized(lock) {
//            return provider != null ? provider : (SelectorProvider)AccessController.doPrivileged(new PrivilegedAction<SelectorProvider>() {
//                public SelectorProvider run() {
//                    if (SelectorProvider.loadProviderFromProperty()) {
//                        return SelectorProvider.provider;
//                    } else if (SelectorProvider.loadProviderAsService()) {
//                        return SelectorProvider.provider;
//                    } else {
//                        SelectorProvider.provider = DefaultSelectorProvider.create();
//                        return SelectorProvider.provider;
//                    }
//                }
//            });
//        }
    }

//    public abstract DatagramChannel openDatagramChannel() throws IOException;
//
//    public abstract DatagramChannel openDatagramChannel(ProtocolFamily var1) throws IOException;
//
//    public abstract Pipe openPipe() throws IOException;
//
    public abstract AbstractSelector openSelector() throws IOException;
//
//    public abstract ServerSocketChannel openServerSocketChannel() throws IOException;
//
//    public abstract SocketChannel openSocketChannel() throws IOException;
//
//    public Channel inheritedChannel() throws IOException {
//        return null;
//    }
}
