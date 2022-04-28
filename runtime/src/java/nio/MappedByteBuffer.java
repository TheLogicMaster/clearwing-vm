package java.nio;

public class MappedByteBuffer extends ByteBuffer {

	MappedByteBuffer () {
		super(0, 0);
		throw new UnsupportedOperationException();
	}
}
