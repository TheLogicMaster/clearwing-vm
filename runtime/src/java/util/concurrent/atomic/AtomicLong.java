package java.util.concurrent.atomic;

public class AtomicLong implements java.io.Serializable {

	private volatile long value;

	public AtomicLong (long initialValue) {
		value = initialValue;
	}

	public AtomicLong () {
	}

	public synchronized final long get() {
		return value;
	}

	public synchronized final boolean compareAndSet(long expect, long update) {
		if (value == expect) {
			value = update;
			return true;
		}
		return false;
	}

	public boolean weakCompareAndSet(long expect, long update) {
		return compareAndSet(expect, update);
	}

	public synchronized final void set(long newValue) {
		value = newValue;
	}

	public final void lazySet(long newValue) {
		set(newValue);
	}

	public synchronized final long getAndSet(long newValue) {
		long got = value;
		value = newValue;
		return got;
	}

	public synchronized final long getAndIncrement() {
		return value++;
	}

	public synchronized final long getAndDecrement() {
		return value--;
	}

	public synchronized final long getAndAdd(int delta) {
		value += delta;
		return value - delta;
	}

	public synchronized final long incrementAndGet() {
		return ++value;
	}

	public synchronized final long decrementAndGet() {
		return --value;
	}

	public synchronized final long addAndGet(int delta) {
		return value += delta;
	}

	public String toString() {
		return Long.toString(get());
	}
}
