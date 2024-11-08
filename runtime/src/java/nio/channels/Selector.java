package java.nio.channels;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.spi.SelectorProvider;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public abstract class Selector implements Closeable {
    protected Selector() {
    }

    public static Selector open() throws IOException {
        throw new UnsupportedOperationException();
//        return SelectorProvider.provider().openSelector();
    }

    public abstract boolean isOpen();

    public abstract SelectorProvider provider();

    public abstract Set<SelectionKey> keys();

    public abstract Set<SelectionKey> selectedKeys();

    public abstract int selectNow() throws IOException;

    public abstract int select(long var1) throws IOException;

    public abstract int select() throws IOException;

    public int select(Consumer<SelectionKey> action, long timeout) throws IOException {
        if (timeout < 0L) {
            throw new IllegalArgumentException("Negative timeout");
        } else {
            return this.doSelect((Consumer)Objects.requireNonNull(action), timeout);
        }
    }

    public int select(Consumer<SelectionKey> action) throws IOException {
        return this.select(action, 0L);
    }

    public int selectNow(Consumer<SelectionKey> action) throws IOException {
        return this.doSelect((Consumer)Objects.requireNonNull(action), -1L);
    }

    private int doSelect(Consumer<SelectionKey> action, long timeout) throws IOException {
        return 0;
//        synchronized(this) {
//            Set<SelectionKey> selectedKeys = this.selectedKeys();
//            int var10000;
//            synchronized(selectedKeys) {
//                selectedKeys.clear();
//                int numKeySelected;
//                if (timeout < 0L) {
//                    numKeySelected = this.selectNow();
//                } else {
//                    numKeySelected = this.select(timeout);
//                }
//
//                Set<SelectionKey> keysToConsume = Set.copyOf(selectedKeys);
//
//                assert keysToConsume.size() == numKeySelected;
//
//                selectedKeys.clear();
//                keysToConsume.forEach((k) -> {
//                    action.accept(k);
//                    if (!this.isOpen()) {
//                        throw new ClosedSelectorException();
//                    }
//                });
//                var10000 = numKeySelected;
//            }
//
//            return var10000;
//        }
    }

    public abstract Selector wakeup();

    public abstract void close() throws IOException;
}