package java.util.zip;

import java.io.Closeable;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

public class ZipFile implements ZipConstants, Closeable {

	public static final int OPEN_READ = 0x1;
	public static final int OPEN_DELETE = 0x4;

	private long handle;
	private final String name;
	private final ArrayList<ZipEntry> entries = new ArrayList<>();

	public ZipFile(String name) throws IOException {
		this(new File(name), OPEN_READ);
	}

	public ZipFile(File file, int mode) throws IOException {
		this(file, mode, StandardCharsets.UTF_8);
	}

	public ZipFile(File file) throws ZipException, IOException {
		this(file, OPEN_READ);
	}

	public ZipFile(String name, Charset charset) throws IOException {
		this(new File(name), OPEN_READ, charset);
	}

	public ZipFile(File file, Charset charset) throws IOException {
		this(file, OPEN_READ, charset);
	}

	public ZipFile(File file, int mode, Charset charset) throws IOException {
		name = file.getPath();
		handle = open(name, entries);
		if (handle == 0)
			throw new IOException("Failed to open ZipFile");
	}

	public String getComment() {
		return null;
	}

	public ZipEntry getEntry(String name) {
		for (ZipEntry entry: entries)
			if (entry.name.equals(name))
				return entry;
		return null;
	}

	public InputStream getInputStream(ZipEntry entry) throws IOException {
		if (entry == null)
			throw new NullPointerException("entry");
		if (handle == 0)
			throw new IllegalStateException("ZipFile is closed");
		return new ZipFileInputStream(entry);
	}

	public String getName() {
		return name;
	}

	public Enumeration<? extends ZipEntry> entries() {
		return Collections.enumeration(entries);
	}

	public int size() {
		return entries.size();
	}

	public void close() throws IOException {
		if (handle == 0)
			return;
		close0(handle);
		handle = 0;
	}

	private static native long open(String path, ArrayList<ZipEntry> entries);
	private static native void close0(long handle);
	private static native long openEntry(long handle, String name);
	private static native int readEntry(long handle);
	private static native void closeEntry(long entry);

	private class ZipFileInputStream extends InputStream {

		private long entryHandle;

		public ZipFileInputStream (ZipEntry entry) throws IOException {
			entryHandle = openEntry(handle, entry.name);
			if (entryHandle == 0)
				throw new IOException("Failed to open ZipEntry InputStream");
		}

		@Override
		public int read () throws IOException {
			if (entryHandle == 0)
				throw new IllegalStateException("ZipFileStream is closed");
			return readEntry(entryHandle);
		}

		@Override
		public void close () throws IOException {
			if (entryHandle == 0)
				return;
			closeEntry(entryHandle);
			entryHandle = 0;
		}
	}
}
