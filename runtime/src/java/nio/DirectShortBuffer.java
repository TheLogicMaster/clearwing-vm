/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.nio;

class DirectShortBuffer extends ShortBuffer {

	protected final ByteBuffer bb;
	protected final int offset;

	DirectShortBuffer(ByteBuffer bb) {

		super(-1, 0, bb.remaining() >> 1, bb.remaining() >> 1);
		this.bb = bb;
		// enforce limit == capacity
		int cap = this.capacity();
		this.limit(cap);
		int pos = this.position();
		assert (pos <= cap);
		offset = pos;

		address = bb.address;
	}

	DirectShortBuffer(ByteBuffer bb, int mark, int pos, int lim, int cap, int off) {

		super(mark, pos, lim, cap);
		this.bb = bb;
		offset = off;

		address = bb.address + off;
	}

	public ShortBuffer slice () {
		int pos = this.position();
		int lim = this.limit();
		int rem = (pos <= lim ? lim - pos : 0);
		int off = (pos << 1) + offset;
		assert (off >= 0);
		return new DirectShortBuffer(bb, -1, 0, rem, rem, off);
	}

	public ShortBuffer duplicate () {
		return new DirectShortBuffer(bb, this.markValue(), this.position(), this.limit(), this.capacity(), offset);
	}

	public ShortBuffer asReadOnlyBuffer () {
		throw new UnsupportedOperationException();
	}

	protected native int ix (int i);/* {
		return (i << 1) + offset;
	}*/

	public native short get ();/* {
		return Bits.getShortL(bb, ix(nextGetIndex()));
	}*/

	public native short get (int i);/* {
		return Bits.getShortL(bb, ix(checkIndex(i)));
	}*/

	public native ShortBuffer put (short x);/* {

		Bits.putShortL(bb, ix(nextPutIndex()), x);
		return this;

	}*/

	public native ShortBuffer put (int i, short x);/* {

		Bits.putShortL(bb, ix(checkIndex(i)), x);
		return this;

	}*/

	public ShortBuffer compact () {

		int pos = position();
		int lim = limit();
		assert (pos <= lim);
		int rem = (pos <= lim ? lim - pos : 0);

		ByteBuffer db = bb.duplicate();
		db.limit(ix(lim));
		db.position(ix(0));
		ByteBuffer sb = db.slice();
		sb.position(pos << 1);
		sb.compact();
		position(rem);
		limit(capacity());
		discardMark();
		return this;

	}

	public boolean isDirect () {
		return true;
	}

	public boolean isReadOnly () {
		return false;
	}

	public ByteOrder order () {

		return bb.order();

	}

}