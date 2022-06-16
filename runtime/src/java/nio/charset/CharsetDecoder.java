package java.nio.charset;

import java.io.UnsupportedOperationException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public class CharsetDecoder {

	public final CharsetDecoder onMalformedInput(CodingErrorAction newAction) {
		throw new UnsupportedOperationException();
	}

	public final CharsetDecoder onUnmappableCharacter(CodingErrorAction newAction) {
		throw new UnsupportedOperationException();
	}

	public final CoderResult decode(ByteBuffer in, CharBuffer out, boolean endOfInput) {
		throw new UnsupportedOperationException();
	}

	public final CharBuffer decode(ByteBuffer in) {
		throw new UnsupportedOperationException();
	}
}
