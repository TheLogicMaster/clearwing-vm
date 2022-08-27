/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;

/**
 * A bounded {@linkplain BlockingQueue blocking queue} backed by an
 * array.  This queue orders elements FIFO (first-in-first-out).  The
 * <em>head</em> of the queue is that element that has been on the
 * queue the longest time.  The <em>tail</em> of the queue is that
 * element that has been on the queue the shortest time. New elements
 * are inserted at the tail of the queue, and the queue retrieval
 * operations obtain elements at the head of the queue.
 *
 * <p>This is a classic &quot;bounded buffer&quot;, in which a
 * fixed-sized array holds elements inserted by producers and
 * extracted by consumers.  Once created, the capacity cannot be
 * changed.  Attempts to {@code put} an element into a full queue
 * will result in the operation blocking; attempts to {@code take} an
 * element from an empty queue will similarly block.
 *
 * <p>This class supports an optional fairness policy for ordering
 * waiting producer and consumer threads.  By default, this ordering
 * is not guaranteed. However, a queue constructed with fairness set
 * to {@code true} grants threads access in FIFO order. Fairness
 * generally decreases throughput but reduces variability and avoids
 * starvation.
 *
 * <p>This class and its iterator implement all of the
 * <em>optional</em> methods of the {@link Collection} and {@link
 * Iterator} interfaces.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this collection
 */
public class ArrayBlockingQueue<E> extends AbstractQueue<E>
	implements BlockingQueue<E>, java.io.Serializable {

	/**
	 * Serialization ID. This class relies on default serialization
	 * even for the items array, which is default-serialized, even if
	 * it is empty. Otherwise it could not be declared final, which is
	 * necessary here.
	 */
	private static final long serialVersionUID = -817911632652898426L;

	/** The queued items */
	final Object[] items;

	/** items index for next take, poll, peek or remove */
	volatile int takeIndex;

	/** items index for next put, offer, or add */
	volatile int putIndex;

	/** Number of elements in the queue */
	volatile int count;

	/**
	 * Circularly decrement i.
	 */
	final synchronized int dec(int i) {
		return ((i == 0) ? items.length : i) - 1;
	}

	/**
	 * Returns item at index i.
	 */
	@SuppressWarnings("unchecked")
	final synchronized E itemAt(int i) {
		return (E) items[i];
	}

	/**
	 * Throws NullPointerException if argument is null.
	 *
	 * @param v the element
	 */
	private synchronized static void checkNotNull(Object v) {
		if (v == null)
			throw new NullPointerException();
	}

	/**
	 * Inserts element at current put position, advances, and signals.
	 * Call only when holding lock.
	 */
	private synchronized void enqueue(E x) {
		// assert lock.getHoldCount() == 1;
		// assert items[putIndex] == null;
		final Object[] items = this.items;
		items[putIndex] = x;
		if (++putIndex == items.length)
			putIndex = 0;
		count++;
	}

	/**
	 * Extracts element at current take position, advances, and signals.
	 * Call only when holding lock.
	 */
	private synchronized E dequeue() {
		// assert lock.getHoldCount() == 1;
		// assert items[takeIndex] != null;
		final Object[] items = this.items;
		@SuppressWarnings("unchecked")
		E x = (E) items[takeIndex];
		items[takeIndex] = null;
		if (++takeIndex == items.length)
			takeIndex = 0;
		count--;
		return x;
	}

	/**
	 * Deletes item at array index removeIndex.
	 * Utility for remove(Object) and iterator.remove.
	 * Call only when holding lock.
	 */
	synchronized void removeAt(final int removeIndex) {
		// assert lock.getHoldCount() == 1;
		// assert items[removeIndex] != null;
		// assert removeIndex >= 0 && removeIndex < items.length;
		final Object[] items = this.items;
		if (removeIndex == takeIndex) {
			// removing front item; just advance
			items[takeIndex] = null;
			if (++takeIndex == items.length)
				takeIndex = 0;
			count--;
		} else {
			// an "interior" remove

			// slide over all others up through putIndex.
			final int putIndex = this.putIndex;
			for (int i = removeIndex;;) {
				int next = i + 1;
				if (next == items.length)
					next = 0;
				if (next != putIndex) {
					items[i] = items[next];
					i = next;
				} else {
					items[i] = null;
					this.putIndex = i;
					break;
				}
			}
			count--;
		}
	}

	/**
	 * Creates an {@code ArrayBlockingQueue} with the given (fixed)
	 * capacity and default access policy.
	 *
	 * @param capacity the capacity of this queue
	 * @throws IllegalArgumentException if {@code capacity < 1}
	 */
	public ArrayBlockingQueue(int capacity) {
		this(capacity, false);
	}

	/**
	 * Creates an {@code ArrayBlockingQueue} with the given (fixed)
	 * capacity and the specified access policy.
	 *
	 * @param capacity the capacity of this queue
	 * @param fair if {@code true} then queue accesses for threads blocked
	 *        on insertion or removal, are processed in FIFO order;
	 *        if {@code false} the access order is unspecified.
	 * @throws IllegalArgumentException if {@code capacity < 1}
	 */
	public ArrayBlockingQueue(int capacity, boolean fair) {
		if (capacity <= 0)
			throw new IllegalArgumentException();
		this.items = new Object[capacity];
	}

	/**
	 * Creates an {@code ArrayBlockingQueue} with the given (fixed)
	 * capacity, the specified access policy and initially containing the
	 * elements of the given collection,
	 * added in traversal order of the collection's iterator.
	 *
	 * @param capacity the capacity of this queue
	 * @param fair if {@code true} then queue accesses for threads blocked
	 *        on insertion or removal, are processed in FIFO order;
	 *        if {@code false} the access order is unspecified.
	 * @param c the collection of elements to initially contain
	 * @throws IllegalArgumentException if {@code capacity} is less than
	 *         {@code c.size()}, or less than 1.
	 * @throws NullPointerException if the specified collection or any
	 *         of its elements are null
	 */
	public ArrayBlockingQueue(int capacity, boolean fair,
		Collection<? extends E> c) {
		this(capacity, fair);

		int i = 0;
		try {
			for (E e : c) {
				checkNotNull(e);
				items[i++] = e;
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new IllegalArgumentException();
		}
		count = i;
		putIndex = (i == capacity) ? 0 : i;
	}

	/**
	 * Inserts the specified element at the tail of this queue if it is
	 * possible to do so immediately without exceeding the queue's capacity,
	 * returning {@code true} upon success and throwing an
	 * {@code IllegalStateException} if this queue is full.
	 *
	 * @param e the element to add
	 * @return {@code true} (as specified by {@link Collection#add})
	 * @throws IllegalStateException if this queue is full
	 * @throws NullPointerException if the specified element is null
	 */
	public synchronized boolean add(E e) {
		return super.add(e);
	}

	/**
	 * Inserts the specified element at the tail of this queue if it is
	 * possible to do so immediately without exceeding the queue's capacity,
	 * returning {@code true} upon success and {@code false} if this queue
	 * is full.  This method is generally preferable to method {@link #add},
	 * which can fail to insert an element only by throwing an exception.
	 *
	 * @throws NullPointerException if the specified element is null
	 */
	public synchronized boolean offer(E e) {
		checkNotNull(e);
		if (count == items.length)
			return false;
		else {
			enqueue(e);
			return true;
		}
	}

	/**
	 * Inserts the specified element at the tail of this queue, waiting
	 * for space to become available if the queue is full.
	 *
	 * @throws InterruptedException {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 */
	public void put(E e) throws InterruptedException {
		synchronized (this) {
			checkNotNull(e);
		}
		while (true) {
			synchronized (this) {
				if (count != items.length)
					break;
			}
			Thread.sleep(0);
		}
		enqueue(e);
	}

	/**
	 * Inserts the specified element at the tail of this queue, waiting
	 * up to the specified wait time for space to become available if
	 * the queue is full.
	 *
	 * @throws InterruptedException {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 */
	public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
		checkNotNull(e);
		long startTime = System.nanoTime();
		long nanos = unit.toNanos(timeout);
		while (true) {
			synchronized (this) {
				if (count != items.length)
					break;
			}
			if (System.nanoTime() - startTime > nanos)
				return false;
			Thread.sleep(0);
		}
		enqueue(e);
		return true;
	}

	public synchronized E poll() {
		return (count == 0) ? null : dequeue();
	}

	public E take() throws InterruptedException {
		while (true) {
			synchronized (this) {
				if (count != 0)
					break;
			}
			Thread.sleep(0);
		}
		return dequeue();
	}

	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		long startTime = System.nanoTime();
		long nanos = unit.toNanos(timeout);
		while (true) {
			synchronized (this) {
				if (count != items.length)
					break;
			}
			if (System.nanoTime() - startTime > nanos)
				return null;
			Thread.sleep(0);
		}
		return dequeue();
	}

	public synchronized E peek() {
		return itemAt(takeIndex);
	}

	// this doc comment is overridden to remove the reference to collections
	// greater in size than Integer.MAX_VALUE
	/**
	 * Returns the number of elements in this queue.
	 *
	 * @return the number of elements in this queue
	 */
	public synchronized int size() {
		return count;
	}

	// this doc comment is a modified copy of the inherited doc comment,
	// without the reference to unlimited queues.
	/**
	 * Returns the number of additional elements that this queue can ideally
	 * (in the absence of memory or resource constraints) accept without
	 * blocking. This is always equal to the initial capacity of this queue
	 * less the current {@code size} of this queue.
	 *
	 * <p>Note that you <em>cannot</em> always tell if an attempt to insert
	 * an element will succeed by inspecting {@code remainingCapacity}
	 * because it may be the case that another thread is about to
	 * insert or remove an element.
	 */
	public synchronized int remainingCapacity() {
		return items.length - count;
	}

	/**
	 * Removes a single instance of the specified element from this queue,
	 * if it is present.  More formally, removes an element {@code e} such
	 * that {@code o.equals(e)}, if this queue contains one or more such
	 * elements.
	 * Returns {@code true} if this queue contained the specified element
	 * (or equivalently, if this queue changed as a result of the call).
	 *
	 * <p>Removal of interior elements in circular array based queues
	 * is an intrinsically slow and disruptive operation, so should
	 * be undertaken only in exceptional circumstances, ideally
	 * only when the queue is known not to be accessible by other
	 * threads.
	 *
	 * @param o element to be removed from this queue, if present
	 * @return {@code true} if this queue changed as a result of the call
	 */
	public synchronized boolean remove(Object o) {
		if (o == null) return false;
		final Object[] items = this.items;
		if (count > 0) {
			final int putIndex = this.putIndex;
			int i = takeIndex;
			do {
				if (o.equals(items[i])) {
					removeAt(i);
					return true;
				}
				if (++i == items.length)
					i = 0;
			} while (i != putIndex);
		}
		return false;
	}

	/**
	 * Returns {@code true} if this queue contains the specified element.
	 * More formally, returns {@code true} if and only if this queue contains
	 * at least one element {@code e} such that {@code o.equals(e)}.
	 *
	 * @param o object to be checked for containment in this queue
	 * @return {@code true} if this queue contains the specified element
	 */
	public synchronized boolean contains(Object o) {
		if (o == null) return false;
		final Object[] items = this.items;
		if (count > 0) {
			final int putIndex = this.putIndex;
			int i = takeIndex;
			do {
				if (o.equals(items[i]))
					return true;
				if (++i == items.length)
					i = 0;
			} while (i != putIndex);
		}
		return false;
	}

	/**
	 * Returns an array containing all of the elements in this queue, in
	 * proper sequence.
	 *
	 * <p>The returned array will be "safe" in that no references to it are
	 * maintained by this queue.  (In other words, this method must allocate
	 * a new array).  The caller is thus free to modify the returned array.
	 *
	 * <p>This method acts as bridge between array-based and collection-based
	 * APIs.
	 *
	 * @return an array containing all of the elements in this queue
	 */
	public synchronized Object[] toArray() {
		Object[] a;
		final int count = this.count;
		a = new Object[count];
		int n = items.length - takeIndex;
		if (count <= n)
			System.arraycopy(items, takeIndex, a, 0, count);
		else {
			System.arraycopy(items, takeIndex, a, 0, n);
			System.arraycopy(items, 0, a, n, count - n);
		}
		return a;
	}

	/**
	 * Returns an array containing all of the elements in this queue, in
	 * proper sequence; the runtime type of the returned array is that of
	 * the specified array.  If the queue fits in the specified array, it
	 * is returned therein.  Otherwise, a new array is allocated with the
	 * runtime type of the specified array and the size of this queue.
	 *
	 * <p>If this queue fits in the specified array with room to spare
	 * (i.e., the array has more elements than this queue), the element in
	 * the array immediately following the end of the queue is set to
	 * {@code null}.
	 *
	 * <p>Like the {@link #toArray()} method, this method acts as bridge between
	 * array-based and collection-based APIs.  Further, this method allows
	 * precise control over the runtime type of the output array, and may,
	 * under certain circumstances, be used to save allocation costs.
	 *
	 * <p>Suppose {@code x} is a queue known to contain only strings.
	 * The following code can be used to dump the queue into a newly
	 * allocated array of {@code String}:
	 *
	 *  <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
	 *
	 * Note that {@code toArray(new Object[0])} is identical in function to
	 * {@code toArray()}.
	 *
	 * @param a the array into which the elements of the queue are to
	 *          be stored, if it is big enough; otherwise, a new array of the
	 *          same runtime type is allocated for this purpose
	 * @return an array containing all of the elements in this queue
	 * @throws ArrayStoreException if the runtime type of the specified array
	 *         is not a supertype of the runtime type of every element in
	 *         this queue
	 * @throws NullPointerException if the specified array is null
	 */
	@SuppressWarnings("unchecked")
	public synchronized <T> T[] toArray(T[] a) {
		final Object[] items = this.items;
		final int count = this.count;
		final int len = a.length;
		if (len < count)
			a = (T[])java.lang.reflect.Array.newInstance(
				a.getClass().getComponentType(), count);
		int n = items.length - takeIndex;
		if (count <= n)
			System.arraycopy(items, takeIndex, a, 0, count);
		else {
			System.arraycopy(items, takeIndex, a, 0, n);
			System.arraycopy(items, 0, a, n, count - n);
		}
		if (len > count)
			a[count] = null;
		return a;
	}

	public synchronized String toString() {
		int k = count;
		if (k == 0)
			return "[]";

		final Object[] items = this.items;
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i = takeIndex; ; ) {
			Object e = items[i];
			sb.append(e == this ? "(this Collection)" : e);
			if (--k == 0)
				return sb.append(']').toString();
			sb.append(',').append(' ');
			if (++i == items.length)
				i = 0;
		}
	}

	/**
	 * Atomically removes all of the elements from this queue.
	 * The queue will be empty after this call returns.
	 */
	public synchronized void clear() {
		final Object[] items = this.items;
		int k = count;
		if (k > 0) {
			final int putIndex = this.putIndex;
			int i = takeIndex;
			do {
				items[i] = null;
				if (++i == items.length)
					i = 0;
			} while (i != putIndex);
			takeIndex = putIndex;
			count = 0;
		}
	}

	/**
	 * @throws UnsupportedOperationException {@inheritDoc}
	 * @throws ClassCastException            {@inheritDoc}
	 * @throws NullPointerException          {@inheritDoc}
	 * @throws IllegalArgumentException      {@inheritDoc}
	 */
	public synchronized int drainTo(Collection<? super E> c) {
		return drainTo(c, Integer.MAX_VALUE);
	}

	/**
	 * @throws UnsupportedOperationException {@inheritDoc}
	 * @throws ClassCastException            {@inheritDoc}
	 * @throws NullPointerException          {@inheritDoc}
	 * @throws IllegalArgumentException      {@inheritDoc}
	 */
	public synchronized int drainTo(Collection<? super E> c, int maxElements) {
		checkNotNull(c);
		if (c == this)
			throw new IllegalArgumentException();
		if (maxElements <= 0)
			return 0;
		final Object[] items = this.items;
		int n = Math.min(maxElements, count);
		int take = takeIndex;
		int i = 0;
		try {
			while (i < n) {
				@SuppressWarnings("unchecked")
				E x = (E) items[take];
				c.add(x);
				items[take] = null;
				if (++take == items.length)
					take = 0;
				i++;
			}
			return n;
		} finally {
			// Restore invariants even if c.add() threw
			if (i > 0) {
				count -= i;
				takeIndex = take;
			}
		}
	}

	/**
	 * Returns an iterator over the elements in this queue in proper sequence.
	 * The elements will be returned in order from first (head) to last (tail).
	 *
	 * <p>The returned iterator is
	 * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
	 *
	 * @return an iterator over the elements in this queue in proper sequence
	 */
	public synchronized Iterator<E> iterator() {
		return null;
	}
}
