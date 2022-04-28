package java.io;

import java.nio.channels.FileChannel;

public class RandomAccessFile implements Closeable {

	public RandomAccessFile (File file, String mode) {
		throw new UnsupportedOperationException();
	}

	public final FileChannel getChannel() {
		return null;
	}

	@Override
	public void close () throws IOException {

	}
}
