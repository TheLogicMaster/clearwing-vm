package java.nio.channels;

import java.io.IOException;
import java.nio.MappedByteBuffer;

public class FileChannel {

	public MappedByteBuffer map(FileChannel.MapMode mode, long position, long size) throws IOException {
		return null;
	}

	public static class MapMode {

		public static final MapMode READ_ONLY = new MapMode("READ_ONLY");

		public static final MapMode READ_WRITE = new MapMode("READ_WRITE");

		public static final MapMode PRIVATE = new MapMode("PRIVATE");

		private final String name;

		private MapMode(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}
	}
}
