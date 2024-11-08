package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.InterruptibleChannel;
//import jdk.internal.misc.SharedSecrets;
//import sun.nio.ch.Interruptible;

public abstract class AbstractInterruptibleChannel implements Channel, InterruptibleChannel {
    private final Object closeLock = new Object();
    private volatile boolean closed;
//    private Interruptible interruptor;
    private volatile Thread interrupted;

    protected AbstractInterruptibleChannel() {
    }

    public final void close() throws IOException {
//        synchronized(this.closeLock) {
//            if (!this.closed) {
//                this.closed = true;
//                this.implCloseChannel();
//            }
//        }
    }

//    protected abstract void implCloseChannel() throws IOException;

    public final boolean isOpen() {
        return !this.closed;
    }

    protected final void begin() {
//        if (this.interruptor == null) {
//            this.interruptor = new Interruptible() {
//                public void interrupt(Thread target) {
//                    synchronized(AbstractInterruptibleChannel.this.closeLock) {
//                        if (!AbstractInterruptibleChannel.this.closed) {
//                            AbstractInterruptibleChannel.this.closed = true;
//                            AbstractInterruptibleChannel.this.interrupted = target;
//
//                            try {
//                                AbstractInterruptibleChannel.this.implCloseChannel();
//                            } catch (IOException var5) {
//                            }
//
//                        }
//                    }
//                }
//            };
//        }
//
//        blockedOn(this.interruptor);
//        Thread me = Thread.currentThread();
//        if (me.isInterrupted()) {
//            this.interruptor.interrupt(me);
//        }

    }

    protected final void end(boolean completed) throws AsynchronousCloseException {
//        blockedOn((Interruptible)null);
//        Thread interrupted = this.interrupted;
//        if (interrupted != null && interrupted == Thread.currentThread()) {
//            this.interrupted = null;
//            throw new ClosedByInterruptException();
//        } else if (!completed && this.closed) {
//            throw new AsynchronousCloseException();
//        }
    }

//    static void blockedOn(Interruptible intr) {
//        SharedSecrets.getJavaLangAccess().blockedOn(intr);
//    }
}