package java.io;

public class ObjectOutputStream extends OutputStream {

	public ObjectOutputStream(OutputStream out) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void write (int b) throws IOException {

	}

	public void defaultWriteObject() {

	}

	public final void writeObject(Object obj) {

	}

	public void writeInt(int val) {

	}
}
