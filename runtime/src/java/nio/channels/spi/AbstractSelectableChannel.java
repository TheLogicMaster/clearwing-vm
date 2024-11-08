package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public abstract class AbstractSelectableChannel extends SelectableChannel {
    private final SelectorProvider provider;
    private SelectionKey[] keys = null;
    private int keyCount = 0;
    private final Object keyLock = new Object();
    private final Object regLock = new Object();
    private volatile boolean nonBlocking;

    protected AbstractSelectableChannel(SelectorProvider provider) {
        this.provider = provider;
    }

    public final SelectorProvider provider() {
        return this.provider;
    }

    private void addKey(SelectionKey k) {
//        assert Thread.holdsLock(this.keyLock);

        int i = 0;
        if (this.keys != null && this.keyCount < this.keys.length) {
            for(i = 0; i < this.keys.length && this.keys[i] != null; ++i) {
            }
        } else if (this.keys == null) {
            this.keys = new SelectionKey[2];
        } else {
            int n = this.keys.length * 2;
            SelectionKey[] ks = new SelectionKey[n];

            for(i = 0; i < this.keys.length; ++i) {
                ks[i] = this.keys[i];
            }

            this.keys = ks;
            i = this.keyCount;
        }

        this.keys[i] = k;
        ++this.keyCount;
    }

    private SelectionKey findKey(Selector sel) {
//        assert Thread.holdsLock(this.keyLock);

        if (this.keys == null) {
            return null;
        } else {
            for(int i = 0; i < this.keys.length; ++i) {
                if (this.keys[i] != null && this.keys[i].selector() == sel) {
                    return this.keys[i];
                }
            }

            return null;
        }
    }

    void removeKey(SelectionKey k) {
        synchronized(this.keyLock) {
            for(int i = 0; i < this.keys.length; ++i) {
                if (this.keys[i] == k) {
                    this.keys[i] = null;
                    --this.keyCount;
                }
            }

            ((AbstractSelectionKey)k).invalidate();
        }
    }

    private boolean haveValidKeys() {
        synchronized(this.keyLock) {
            if (this.keyCount == 0) {
                return false;
            } else {
                for(int i = 0; i < this.keys.length; ++i) {
                    if (this.keys[i] != null && this.keys[i].isValid()) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    public final boolean isRegistered() {
        synchronized(this.keyLock) {
            return this.keyCount != 0;
        }
    }

    public final SelectionKey keyFor(Selector sel) {
        synchronized(this.keyLock) {
            return this.findKey(sel);
        }
    }

    public final SelectionKey register(Selector sel, int ops, Object att) throws ClosedChannelException {
        if ((ops & ~this.validOps()) != 0) {
            throw new IllegalArgumentException();
        } else if (!this.isOpen()) {
            throw new ClosedChannelException();
        } else {
            synchronized(this.regLock) {
                if (this.isBlocking()) {
                    throw new IllegalBlockingModeException();
                } else {
                    SelectionKey var10000;
                    synchronized(this.keyLock) {
                        if (!this.isOpen()) {
                            throw new ClosedChannelException();
                        }

                        SelectionKey k = this.findKey(sel);
                        if (k != null) {
                            k.attach(att);
                            k.interestOps(ops);
                        } else {
                            k = ((AbstractSelector)sel).register(this, ops, att);
                            this.addKey(k);
                        }

                        var10000 = k;
                    }

                    return var10000;
                }
            }
        }
    }

    protected final void implCloseChannel() throws IOException {
        this.implCloseSelectableChannel();
        SelectionKey[] copyOfKeys = null;
        synchronized(this.keyLock) {
            if (this.keys != null) {
                copyOfKeys = (SelectionKey[])this.keys.clone();
            }
        }

        if (copyOfKeys != null) {
            SelectionKey[] var2 = copyOfKeys;
            int var3 = copyOfKeys.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                SelectionKey k = var2[var4];
                if (k != null) {
                    k.cancel();
                }
            }
        }

    }

    protected abstract void implCloseSelectableChannel() throws IOException;

    public final boolean isBlocking() {
        return !this.nonBlocking;
    }

    public final Object blockingLock() {
        return this.regLock;
    }

    public final SelectableChannel configureBlocking(boolean block) throws IOException {
        synchronized(this.regLock) {
            if (!this.isOpen()) {
                throw new ClosedChannelException();
            } else {
                boolean blocking = !this.nonBlocking;
                if (block != blocking) {
                    if (block && this.haveValidKeys()) {
                        throw new IllegalBlockingModeException();
                    }

                    this.implConfigureBlocking(block);
                    this.nonBlocking = !block;
                }

                return this;
            }
        }
    }

    protected abstract void implConfigureBlocking(boolean var1) throws IOException;
}