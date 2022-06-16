package java.nio.charset;

import java.io.UnsupportedOperationException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public class CharsetEncoder {

	public final CharsetEncoder onMalformedInput(CodingErrorAction newAction) {
		throw new UnsupportedOperationException();
	}

	public final CharsetEncoder onUnmappableCharacter(CodingErrorAction newAction) {
		throw new UnsupportedOperationException();
	}

	public final CoderResult encode(CharBuffer in, ByteBuffer out, boolean endOfInput) {
		throw new UnsupportedOperationException();
	}

	public final ByteBuffer encode(CharBuffer in) {
		throw new UnsupportedOperationException();
	}
}
