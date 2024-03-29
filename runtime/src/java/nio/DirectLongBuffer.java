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

class DirectLongBuffer extends LongBuffer {

	protected final ByteBuffer bb;
	protected final int offset;

	DirectLongBuffer(ByteBuffer bb) {

		super(-1, 0, bb.remaining() >> 3, bb.remaining() >> 3);
		this.bb = bb;
		// enforce limit == capacity
		int cap = this.capacity();
		this.limit(cap);
		int pos = this.position();
		assert (pos <= cap);
		offset = pos;

		address = bb.address;
	}

	DirectLongBuffer(ByteBuffer bb, int mark, int pos, int lim, int cap, int off) {

		super(mark, pos, lim, cap);
		this.bb = bb;
		offset = off;

		address = bb.address + off;
	}

	public LongBuffer slice () {
		int pos = this.position();
		int lim = this.limit();
		int rem = (pos <= lim ? lim - pos : 0);
		int off = (pos << 3) + offset;
		assert (off >= 0);
		return new DirectLongBuffer(bb, -1, 0, rem, rem, off);
	}

	public LongBuffer duplicate () {
		return new DirectLongBuffer(bb, this.markValue(), this.position(), this.limit(), this.capacity(), offset);
	}

	public LongBuffer asReadOnlyBuffer () {
		throw new UnsupportedOperationException();
	}

	protected native int ix (int i);/* {
		return (i << 3) + offset;
	}*/

	public native long get ();/* {
		return Bits.getLongL(bb, ix(nextGetIndex()));
	}*/

	public native long get (int i);/* {
		return Bits.getLongL(bb, ix(checkIndex(i)));
	}*/

	public native LongBuffer put (long x);/* {

		Bits.putLongL(bb, ix(nextPutIndex()), x);
		return this;

	}*/

	public native LongBuffer put (int i, long x);/* {

		Bits.putLongL(bb, ix(checkIndex(i)), x);
		return this;

	}*/

	public LongBuffer compact () {

		int pos = position();
		int lim = limit();
		assert (pos <= lim);
		int rem = (pos <= lim ? lim - pos : 0);

		ByteBuffer db = bb.duplicate();
		db.limit(ix(lim));
		db.position(ix(0));
		ByteBuffer sb = db.slice();
		sb.position(pos << 3);
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
