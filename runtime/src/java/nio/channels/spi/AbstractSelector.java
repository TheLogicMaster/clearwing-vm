package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import sun.nio.ch.Interruptible;

public abstract class AbstractSelector extends Selector {
    private final AtomicBoolean selectorOpen = new AtomicBoolean(true);
    private final SelectorProvider provider;
    private final Set<SelectionKey> cancelledKeys = new HashSet();
//    private Interruptible interruptor = null;

    protected AbstractSelector(SelectorProvider provider) {
        this.provider = provider;
    }

    void cancel(SelectionKey k) {
        synchronized(this.cancelledKeys) {
            this.cancelledKeys.add(k);
        }
    }

    public final void close() throws IOException {
        boolean open = this.selectorOpen.getAndSet(false);
        if (open) {
            this.implCloseSelector();
        }
    }

    protected abstract void implCloseSelector() throws IOException;

    public final boolean isOpen() {
        return this.selectorOpen.get();
    }

    public final SelectorProvider provider() {
        return this.provider;
    }

    protected final Set<SelectionKey> cancelledKeys() {
        return this.cancelledKeys;
    }

    protected abstract SelectionKey register(AbstractSelectableChannel var1, int var2, Object var3);

    protected final void deregister(AbstractSelectionKey key) {
        ((AbstractSelectableChannel)key.channel()).removeKey(key);
    }

    protected final void begin() {
//        if (this.interruptor == null) {
//            this.interruptor = new Interruptible() {
//                public void interrupt(Thread ignore) {
//                    AbstractSelector.this.wakeup();
//                }
//            };
//        }
//
//        AbstractInterruptibleChannel.blockedOn(this.interruptor);
//        Thread me = Thread.currentThread();
//        if (me.isInterrupted()) {
//            this.interruptor.interrupt(me);
//        }

    }

    protected final void end() {
//        AbstractInterruptibleChannel.blockedOn((Interruptible)null);
    }
}