/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.nio.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public abstract class CharsetDecoder {
	protected CharsetDecoder(Charset cs, float averageCharsPerByte, float maxCharsPerByte) {
		throw new RuntimeException("Stub!");
	}

	public final Charset charset() {
		throw new RuntimeException("Stub!");
	}

	public final String replacement() {
		throw new RuntimeException("Stub!");
	}

	public final CharsetDecoder replaceWith(String newReplacement) {
		throw new RuntimeException("Stub!");
	}

	protected void implReplaceWith(String newReplacement) {
		throw new RuntimeException("Stub!");
	}

	public CodingErrorAction malformedInputAction() {
		throw new RuntimeException("Stub!");
	}

	public final CharsetDecoder onMalformedInput(CodingErrorAction newAction) {
		throw new RuntimeException("Stub!");
	}

	protected void implOnMalformedInput(CodingErrorAction newAction) {
		throw new RuntimeException("Stub!");
	}

	public CodingErrorAction unmappableCharacterAction() {
		throw new RuntimeException("Stub!");
	}

	public final CharsetDecoder onUnmappableCharacter(CodingErrorAction newAction) {
		throw new RuntimeException("Stub!");
	}

	protected void implOnUnmappableCharacter(CodingErrorAction newAction) {
		throw new RuntimeException("Stub!");
	}

	public final float averageCharsPerByte() {
		throw new RuntimeException("Stub!");
	}

	public final float maxCharsPerByte() {
		throw new RuntimeException("Stub!");
	}

	public final CoderResult decode(ByteBuffer in, CharBuffer out, boolean endOfInput) {
		throw new RuntimeException("Stub!");
	}

	public final CoderResult flush(CharBuffer out) {
		throw new RuntimeException("Stub!");
	}

	protected CoderResult implFlush(CharBuffer out) {
		throw new RuntimeException("Stub!");
	}

	public final CharsetDecoder reset() {
		throw new RuntimeException("Stub!");
	}

	protected void implReset() {
		throw new RuntimeException("Stub!");
	}

	protected abstract CoderResult decodeLoop(ByteBuffer var1, CharBuffer var2);

	public final CharBuffer decode(ByteBuffer in) throws CharacterCodingException {
		throw new RuntimeException("Stub!");
	}

	public boolean isAutoDetecting() {
		throw new RuntimeException("Stub!");
	}

	public boolean isCharsetDetected() {
		throw new RuntimeException("Stub!");
	}

	public Charset detectedCharset() {
		throw new RuntimeException("Stub!");
	}
}
