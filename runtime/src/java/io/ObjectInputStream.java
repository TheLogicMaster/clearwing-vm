package java.io;

public class ObjectInputStream extends InputStream {
	public ObjectInputStream(InputStream in) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int read () throws IOException {
		return 0;
	}

	public void defaultReadObject() {

	}

	public final Object readObject() {
		return null;
	}

	public int readInt() {
		return 0;
	}
}
