package java.util.concurrent.atomic;

public class AtomicInteger extends Number implements java.io.Serializable {
    private volatile int value;

    public AtomicInteger (int initialValue) {
        value = initialValue;
    }

    public AtomicInteger () {
    }

    public synchronized final int get() {
        return value;
    }

    public synchronized final void set(int newValue) {
        value = newValue;
    }

    public final void lazySet(int newValue) {
        set(newValue);
    }

    public synchronized final int getAndSet(int newValue) {
        int old = value;
        value = newValue;
        return old;
    }

    public synchronized final boolean compareAndSet(int expect, int update) {
        if (value == expect) {
            value = update;
            return true;
        }
        return false;
    }

    public final boolean weakCompareAndSet(int expect, int update) {
        return compareAndSet(expect, update);
    }

    public synchronized final int getAndIncrement() {
        return value++;
    }

    public synchronized final int getAndDecrement() {
        return value--;
    }

    public synchronized final int getAndAdd(int delta) {
        value += delta;
        return value - delta;
    }

    public synchronized final int incrementAndGet() {
        return ++value;
    }

    public synchronized final int decrementAndGet() {
        return --value;
    }

    public synchronized final int addAndGet(int delta) {
        return value += delta;
    }

    public String toString() {
        return Integer.toString(get());
    }

    public int intValue() {
        return get();
    }

    public long longValue() {
        return get();
    }

    public float floatValue() {
        return get();
    }

    public double doubleValue() {
        return get();
    }
}
