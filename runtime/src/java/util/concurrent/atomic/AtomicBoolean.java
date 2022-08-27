package java.util.concurrent.atomic;

public class AtomicBoolean implements java.io.Serializable {

	private volatile boolean value;

	public AtomicBoolean(boolean initialValue) {
		value = initialValue;
	}

	public AtomicBoolean() {
	}

	public synchronized final boolean get() {
		return value;
	}

	public synchronized final boolean compareAndSet(boolean expect, boolean update) {
		if (value == expect) {
			value = update;
			return true;
		}
		return false;
	}

	public boolean weakCompareAndSet(boolean expect, boolean update) {
		return compareAndSet(expect, update);
	}

	public synchronized final void set(boolean newValue) {
		value = newValue;
	}

	public final void lazySet(boolean newValue) {
		set(newValue);
	}

	public synchronized final boolean getAndSet(boolean newValue) {
		boolean got = value;
		value = newValue;
		return got;
	}

	public String toString() {
		return Boolean.toString(get());
	}

}
