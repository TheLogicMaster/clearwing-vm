package java.nio.channels;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public abstract class SelectionKey {
    public static final int OP_READ = 1;
    public static final int OP_WRITE = 4;
    public static final int OP_CONNECT = 8;
    public static final int OP_ACCEPT = 16;
    private volatile Object attachment;
    private static final AtomicReferenceFieldUpdater<SelectionKey, Object> attachmentUpdater = AtomicReferenceFieldUpdater.newUpdater(SelectionKey.class, Object.class, "attachment");

    protected SelectionKey() {
    }

    public abstract SelectableChannel channel();

    public abstract Selector selector();

    public abstract boolean isValid();

    public abstract void cancel();

    public abstract int interestOps();

    public abstract SelectionKey interestOps(int var1);

    public int interestOpsOr(int ops) {
        synchronized(this) {
            int oldVal = this.interestOps();
            this.interestOps(oldVal | ops);
            return oldVal;
        }
    }

    public int interestOpsAnd(int ops) {
        synchronized(this) {
            int oldVal = this.interestOps();
            this.interestOps(oldVal & ops);
            return oldVal;
        }
    }

    public abstract int readyOps();

    public final boolean isReadable() {
        return (this.readyOps() & 1) != 0;
    }

    public final boolean isWritable() {
        return (this.readyOps() & 4) != 0;
    }

    public final boolean isConnectable() {
        return (this.readyOps() & 8) != 0;
    }

    public final boolean isAcceptable() {
        return (this.readyOps() & 16) != 0;
    }

    public final Object attach(Object ob) {
        return attachmentUpdater.getAndSet(this, ob);
    }

    public final Object attachment() {
        return this.attachment;
    }
}