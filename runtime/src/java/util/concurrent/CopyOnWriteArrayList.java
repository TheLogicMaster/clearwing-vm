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

package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class CopyOnWriteArrayList<E> implements List<E>, RandomAccess, Cloneable, Serializable {
    private static final long serialVersionUID = 8673264195747942595L;
    transient Object lock = new Object();
    private transient volatile Object[] array;

    final Object[] getArray() {
        return this.array;
    }

    final void setArray(Object[] a) {
        this.array = a;
    }

    public CopyOnWriteArrayList() {
        this.setArray(new Object[0]);
    }

    public CopyOnWriteArrayList(Collection<? extends E> c) {
        Object[] es;
        if (c.getClass() == CopyOnWriteArrayList.class) {
            es = ((CopyOnWriteArrayList) c).getArray();
        } else {
            es = c.toArray();
            if (c.getClass() != ArrayList.class) {
                es = Arrays.copyOf(es, es.length, Object[].class);
            }
        }

        this.setArray(es);
    }

    public CopyOnWriteArrayList(E[] toCopyIn) {
        this.setArray(Arrays.copyOf(toCopyIn, toCopyIn.length, Object[].class));
    }

    public int size() {
        return this.getArray().length;
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }

    private static int indexOfRange(Object o, Object[] es, int from, int to) {
        int i;
        if (o == null) {
            for (i = from; i < to; ++i) {
                if (es[i] == null) {
                    return i;
                }
            }
        } else {
            for (i = from; i < to; ++i) {
                if (o.equals(es[i])) {
                    return i;
                }
            }
        }

        return -1;
    }

    private static int lastIndexOfRange(Object o, Object[] es, int from, int to) {
        int i;
        if (o == null) {
            for (i = to - 1; i >= from; --i) {
                if (es[i] == null) {
                    return i;
                }
            }
        } else {
            for (i = to - 1; i >= from; --i) {
                if (o.equals(es[i])) {
                    return i;
                }
            }
        }

        return -1;
    }

    public boolean contains(Object o) {
        return this.indexOf(o) >= 0;
    }

    public int indexOf(Object o) {
        Object[] es = this.getArray();
        return indexOfRange(o, es, 0, es.length);
    }

    public int indexOf(E e, int index) {
        Object[] es = this.getArray();
        return indexOfRange(e, es, index, es.length);
    }

    public int lastIndexOf(Object o) {
        Object[] es = this.getArray();
        return lastIndexOfRange(o, es, 0, es.length);
    }

    public int lastIndexOf(E e, int index) {
        Object[] es = this.getArray();
        return lastIndexOfRange(e, es, 0, index + 1);
    }

    public Object clone() {
        try {
            CopyOnWriteArrayList<E> clone = (CopyOnWriteArrayList) super.clone();
            clone.lock = new Object();
            return clone;
        } catch (CloneNotSupportedException var2) {
            throw new InternalError();
        }
    }

    public Object[] toArray() {
        return (Object[]) this.getArray().clone();
    }

    public <T> T[] toArray(T[] a) {
        Object[] es = this.getArray();
        int len = es.length;
        if (a.length < len) {
            return (T[])Arrays.copyOf(es, len, a.getClass());
        } else {
            System.arraycopy(es, 0, a, 0, len);
            if (a.length > len) {
                a[len] = null;
            }

            return a;
        }
    }

    static <E> E elementAt(Object[] a, int index) {
        return (E)a[index];
    }

    static String outOfBounds(int index, int size) {
        return "Index: " + index + ", Size: " + size;
    }

    public E get(int index) {
        return elementAt(this.getArray(), index);
    }

    public E set(int index, E element) {
        synchronized (this.lock) {
            Object[] es = this.getArray();
            E oldValue = elementAt(es, index);
            if (oldValue != element) {
                es = es.clone();
                es[index] = element;
            }

            this.setArray(es);
            return oldValue;
        }
    }

    public boolean add(E e) {
        synchronized (this.lock) {
            Object[] es = this.getArray();
            int len = es.length;
            es = Arrays.copyOf(es, len + 1);
            es[len] = e;
            this.setArray(es);
            return true;
        }
    }

    public void add(int index, E element) {
        synchronized (this.lock) {
            Object[] es = this.getArray();
            int len = es.length;
            if (index <= len && index >= 0) {
                int numMoved = len - index;
                Object[] newElements;
                if (numMoved == 0) {
                    newElements = Arrays.copyOf(es, len + 1);
                } else {
                    newElements = new Object[len + 1];
                    System.arraycopy(es, 0, newElements, 0, index);
                    System.arraycopy(es, index, newElements, index + 1, numMoved);
                }

                newElements[index] = element;
                this.setArray(newElements);
            } else {
                throw new IndexOutOfBoundsException(outOfBounds(index, len));
            }
        }
    }

    public E remove(int index) {
        synchronized (this.lock) {
            Object[] es = this.getArray();
            int len = es.length;
            E oldValue = elementAt(es, index);
            int numMoved = len - index - 1;
            Object[] newElements;
            if (numMoved == 0) {
                newElements = Arrays.copyOf(es, len - 1);
            } else {
                newElements = new Object[len - 1];
                System.arraycopy(es, 0, newElements, 0, index);
                System.arraycopy(es, index + 1, newElements, index, numMoved);
            }

            this.setArray(newElements);
            return oldValue;
        }
    }

    public boolean remove(Object o) {
        Object[] snapshot = this.getArray();
        int index = indexOfRange(o, snapshot, 0, snapshot.length);
        return index >= 0 && this.remove(o, snapshot, index);
    }

    private boolean remove(Object o, Object[] snapshot, int index) {
        synchronized (this.lock) {
            Object[] current = this.getArray();
            int len = current.length;
            if (snapshot != current) {
                int prefix = Math.min(index, len);
                int i = 0;

                while (true) {
                    if (i >= prefix) {
                        if (index >= len) {
                            return false;
                        }

                        if (current[index] != o) {
                            index = indexOfRange(o, current, index, len);
                            if (index < 0) {
                                return false;
                            }
                        }
                        break;
                    }

                    if (current[i] != snapshot[i] && Objects.equals(o, current[i])) {
                        index = i;
                        break;
                    }

                    ++i;
                }
            }

            Object[] newElements = new Object[len - 1];
            System.arraycopy(current, 0, newElements, 0, index);
            System.arraycopy(current, index + 1, newElements, index, len - index - 1);
            this.setArray(newElements);
            return true;
        }
    }

    void removeRange(int fromIndex, int toIndex) {
        synchronized (this.lock) {
            Object[] es = this.getArray();
            int len = es.length;
            if (fromIndex >= 0 && toIndex <= len && toIndex >= fromIndex) {
                int newlen = len - (toIndex - fromIndex);
                int numMoved = len - toIndex;
                if (numMoved == 0) {
                    this.setArray(Arrays.copyOf(es, newlen));
                } else {
                    Object[] newElements = new Object[newlen];
                    System.arraycopy(es, 0, newElements, 0, fromIndex);
                    System.arraycopy(es, toIndex, newElements, fromIndex, numMoved);
                    this.setArray(newElements);
                }

            } else {
                throw new IndexOutOfBoundsException();
            }
        }
    }

    public boolean addIfAbsent(E e) {
        Object[] snapshot = this.getArray();
        return indexOfRange(e, snapshot, 0, snapshot.length) < 0 && this.addIfAbsent(e, snapshot);
    }

    private boolean addIfAbsent(E e, Object[] snapshot) {
        synchronized (this.lock) {
            Object[] current = this.getArray();
            int len = current.length;
            if (snapshot != current) {
                int common = Math.min(snapshot.length, len);

                for (int i = 0; i < common; ++i) {
                    if (current[i] != snapshot[i] && Objects.equals(e, current[i])) {
                        return false;
                    }
                }

                if (indexOfRange(e, current, common, len) >= 0) {
                    return false;
                }
            }

            Object[] newElements = Arrays.copyOf(current, len + 1);
            newElements[len] = e;
            this.setArray(newElements);
            return true;
        }
    }

    public boolean containsAll(Collection<?> c) {
        Object[] es = this.getArray();
        int len = es.length;
        Iterator var4 = c.iterator();

        Object e;
        do {
            if (!var4.hasNext()) {
                return true;
            }

            e = var4.next();
        } while (indexOfRange(e, es, 0, len) >= 0);

        return false;
    }

    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return this.bulkRemove((e) -> {
            return c.contains(e);
        });
    }

    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return this.bulkRemove((e) -> {
            return !c.contains(e);
        });
    }

    public int addAllAbsent(Collection<? extends E> c) {
        Object[] cs = c.toArray();
        if (c.getClass() != ArrayList.class) {
            cs = cs.clone();
        }

        if (cs.length == 0) {
            return 0;
        } else {
            synchronized (this.lock) {
                Object[] es = this.getArray();
                int len = es.length;
                int added = 0;

                for (int i = 0; i < cs.length; ++i) {
                    Object e = cs[i];
                    if (indexOfRange(e, es, 0, len) < 0 && indexOfRange(e, cs, 0, added) < 0) {
                        cs[added++] = e;
                    }
                }

                if (added > 0) {
                    Object[] newElements = Arrays.copyOf(es, len + added);
                    System.arraycopy(cs, 0, newElements, len, added);
                    this.setArray(newElements);
                }

                return added;
            }
        }
    }

    public void clear() {
        synchronized (this.lock) {
            this.setArray(new Object[0]);
        }
    }

    public boolean addAll(Collection<? extends E> c) {
        Object[] cs = c.getClass() == CopyOnWriteArrayList.class ? ((CopyOnWriteArrayList) c).getArray() : c.toArray();
        if (cs.length == 0) {
            return false;
        } else {
            synchronized (this.lock) {
                Object[] es = this.getArray();
                int len = es.length;
                Object[] newElements;
                if (len != 0 || c.getClass() != CopyOnWriteArrayList.class && c.getClass() != ArrayList.class) {
                    newElements = Arrays.copyOf(es, len + cs.length);
                    System.arraycopy(cs, 0, newElements, len, cs.length);
                } else {
                    newElements = cs;
                }

                this.setArray(newElements);
                return true;
            }
        }
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        Object[] cs = c.toArray();
        synchronized (this.lock) {
            Object[] es = this.getArray();
            int len = es.length;
            if (index <= len && index >= 0) {
                if (cs.length == 0) {
                    return false;
                } else {
                    int numMoved = len - index;
                    Object[] newElements;
                    if (numMoved == 0) {
                        newElements = Arrays.copyOf(es, len + cs.length);
                    } else {
                        newElements = new Object[len + cs.length];
                        System.arraycopy(es, 0, newElements, 0, index);
                        System.arraycopy(es, index, newElements, index + cs.length, numMoved);
                    }

                    System.arraycopy(cs, 0, newElements, index, cs.length);
                    this.setArray(newElements);
                    return true;
                }
            } else {
                throw new IndexOutOfBoundsException(outOfBounds(index, len));
            }
        }
    }

    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        Object[] var2 = this.getArray();
        int var3 = var2.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            Object x = var2[var4];
            E e = (E)x;
            action.accept(e);
        }

    }

    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        return this.bulkRemove(filter);
    }

    private static long[] nBits(int n) {
        return new long[(n - 1 >> 6) + 1];
    }

    private static void setBit(long[] bits, int i) {
        bits[i >> 6] |= 1L << i;
    }

    private static boolean isClear(long[] bits, int i) {
        return (bits[i >> 6] & 1L << i) == 0L;
    }

    private boolean bulkRemove(Predicate<? super E> filter) {
        synchronized (this.lock) {
            return this.bulkRemove(filter, 0, this.getArray().length);
        }
    }

    boolean bulkRemove(Predicate<? super E> filter, int i, int end) {
        Object[] es;
        for (es = this.getArray(); i < end && !filter.test(elementAt(es, i)); ++i) {
        }

        if (i < end) {
            int beg = i;
            long[] deathRow = nBits(end - beg);
            int deleted = 1;
            deathRow[0] = 1L;

            for (i = beg + 1; i < end; ++i) {
                if (filter.test(elementAt(es, i))) {
                    setBit(deathRow, i - beg);
                    ++deleted;
                }
            }

            if (es != this.getArray()) {
                throw new ConcurrentModificationException();
            } else {
                Object[] newElts = Arrays.copyOf(es, es.length - deleted);
                int w = beg;

                for (i = beg; i < end; ++i) {
                    if (isClear(deathRow, i - beg)) {
                        newElts[w++] = es[i];
                    }
                }

                System.arraycopy(es, i, newElts, w, es.length - i);
                this.setArray(newElts);
                return true;
            }
        } else if (es != this.getArray()) {
            throw new ConcurrentModificationException();
        } else {
            return false;
        }
    }

    public void replaceAll(UnaryOperator<E> operator) {
        synchronized (this.lock) {
            this.replaceAllRange(operator, 0, this.getArray().length);
        }
    }

    void replaceAllRange(UnaryOperator<E> operator, int i, int end) {
        Objects.requireNonNull(operator);

        Object[] es;
        for (es = (Object[]) this.getArray().clone(); i < end; ++i) {
            es[i] = operator.apply(elementAt(es, i));
        }

        this.setArray(es);
    }

    public void sort(Comparator<? super E> c) {
        synchronized (this.lock) {
            this.sortRange(c, 0, this.getArray().length);
        }
    }

    void sortRange(Comparator<? super E> c, int i, int end) {
        Object[] es = this.getArray().clone();
        Arrays.sort((E[])es, i, end, c);
        this.setArray(es);
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        Object[] es = this.getArray();
        s.writeInt(es.length);
        Object[] var3 = es;
        int var4 = es.length;

        for (int var5 = 0; var5 < var4; ++var5) {
            Object element = var3[var5];
            s.writeObject(element);
        }

    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        int len = s.readInt();
        Object[] es = new Object[len];

        for (int i = 0; i < len; ++i) {
            es[i] = s.readObject();
        }

        this.setArray(es);
    }

    public String toString() {
        return Arrays.toString(this.getArray());
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof List)) {
            return false;
        } else {
            List<?> list = (List) o;
            Iterator<?> it = list.iterator();
            Object[] var4 = this.getArray();
            int var5 = var4.length;

            for (int var6 = 0; var6 < var5; ++var6) {
                Object element = var4[var6];
                if (!it.hasNext() || !Objects.equals(element, it.next())) {
                    return false;
                }
            }

            return !it.hasNext();
        }
    }

    private static int hashCodeOfRange(Object[] es, int from, int to) {
        int hashCode = 1;

        for (int i = from; i < to; ++i) {
            Object x = es[i];
            hashCode = 31 * hashCode + (x == null ? 0 : x.hashCode());
        }

        return hashCode;
    }

    public int hashCode() {
        Object[] es = this.getArray();
        return hashCodeOfRange(es, 0, es.length);
    }

    public Iterator<E> iterator() {
        return new COWIterator(this.getArray(), 0);
    }

    public ListIterator<E> listIterator() {
        return new COWIterator(this.getArray(), 0);
    }

    public ListIterator<E> listIterator(int index) {
        Object[] es = this.getArray();
        int len = es.length;
        if (index >= 0 && index <= len) {
            return new COWIterator(es, index);
        } else {
            throw new IndexOutOfBoundsException(outOfBounds(index, len));
        }
    }

    public List<E> subList(int fromIndex, int toIndex) {
        synchronized (this.lock) {
            Object[] es = this.getArray();
            int len = es.length;
            int size = toIndex - fromIndex;
            if (fromIndex >= 0 && toIndex <= len && size >= 0) {
                return new COWSubList(es, fromIndex, size);
            } else {
                throw new IndexOutOfBoundsException();
            }
        }
    }
    
    static final class COWIterator<E> implements ListIterator<E> {
        private final Object[] snapshot;
        private int cursor;

        COWIterator(Object[] es, int initialCursor) {
            this.cursor = initialCursor;
            this.snapshot = es;
        }

        public boolean hasNext() {
            return this.cursor < this.snapshot.length;
        }

        public boolean hasPrevious() {
            return this.cursor > 0;
        }

        public E next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            } else {
                return (E)this.snapshot[this.cursor++];
            }
        }

        public E previous() {
            if (!this.hasPrevious()) {
                throw new NoSuchElementException();
            } else {
                return (E)this.snapshot[--this.cursor];
            }
        }

        public int nextIndex() {
            return this.cursor;
        }

        public int previousIndex() {
            return this.cursor - 1;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        public void add(E e) {
            throw new UnsupportedOperationException();
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            int size = this.snapshot.length;
            int i = this.cursor;

            for (this.cursor = size; i < size; ++i) {
                action.accept(CopyOnWriteArrayList.elementAt(this.snapshot, i));
            }

        }
    }

    private class COWSubList implements List<E>, RandomAccess {
        private final int offset;
        private int size;
        private Object[] expectedArray;

        COWSubList(Object[] es, int offset, int size) {
            this.expectedArray = es;
            this.offset = offset;
            this.size = size;
        }

        private void checkForComodification() {
            if (CopyOnWriteArrayList.this.getArray() != this.expectedArray) {
                throw new ConcurrentModificationException();
            }
        }

        private Object[] getArrayChecked() {
            Object[] a = CopyOnWriteArrayList.this.getArray();
            if (a != this.expectedArray) {
                throw new ConcurrentModificationException();
            } else {
                return a;
            }
        }

        private void rangeCheck(int index) {
            if (index < 0 || index >= this.size) {
                throw new IndexOutOfBoundsException(CopyOnWriteArrayList.outOfBounds(index, this.size));
            }
        }

        private void rangeCheckForAdd(int index) {
            if (index < 0 || index > this.size) {
                throw new IndexOutOfBoundsException(CopyOnWriteArrayList.outOfBounds(index, this.size));
            }
        }

        public Object[] toArray() {
            Object[] es;
            int offset;
            int size;
            synchronized (CopyOnWriteArrayList.this.lock) {
                es = this.getArrayChecked();
                offset = this.offset;
                size = this.size;
            }

            return Arrays.copyOfRange(es, offset, offset + size);
        }

        public <T> T[] toArray(T[] a) {
            Object[] es;
            int offset;
            int size;
            synchronized (CopyOnWriteArrayList.this.lock) {
                es = this.getArrayChecked();
                offset = this.offset;
                size = this.size;
            }

            if (a.length < size) {
                return (T[])Arrays.copyOfRange(es, offset, offset + size, a.getClass());
            } else {
                System.arraycopy(es, offset, a, 0, size);
                if (a.length > size) {
                    a[size] = null;
                }

                return a;
            }
        }

        public int indexOf(Object o) {
            Object[] es;
            int offset;
            int size;
            synchronized (CopyOnWriteArrayList.this.lock) {
                es = this.getArrayChecked();
                offset = this.offset;
                size = this.size;
            }

            int i = CopyOnWriteArrayList.indexOfRange(o, es, offset, offset + size);
            return i == -1 ? -1 : i - offset;
        }

        public int lastIndexOf(Object o) {
            Object[] es;
            int offset;
            int size;
            synchronized (CopyOnWriteArrayList.this.lock) {
                es = this.getArrayChecked();
                offset = this.offset;
                size = this.size;
            }

            int i = CopyOnWriteArrayList.lastIndexOfRange(o, es, offset, offset + size);
            return i == -1 ? -1 : i - offset;
        }

        public boolean contains(Object o) {
            return this.indexOf(o) >= 0;
        }

        public boolean containsAll(Collection<?> c) {
            Object[] es;
            int offset;
            int size;
            synchronized (CopyOnWriteArrayList.this.lock) {
                es = this.getArrayChecked();
                offset = this.offset;
                size = this.size;
            }

            Iterator var5 = c.iterator();

            Object o;
            do {
                if (!var5.hasNext()) {
                    return true;
                }

                o = var5.next();
            } while (CopyOnWriteArrayList.indexOfRange(o, es, offset, offset + size) >= 0);

            return false;
        }

        public boolean isEmpty() {
            return this.size() == 0;
        }

        public String toString() {
            return Arrays.toString(this.toArray());
        }

        public int hashCode() {
            Object[] es;
            int offset;
            int size;
            synchronized (CopyOnWriteArrayList.this.lock) {
                es = this.getArrayChecked();
                offset = this.offset;
                size = this.size;
            }

            return CopyOnWriteArrayList.hashCodeOfRange(es, offset, offset + size);
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (!(o instanceof List)) {
                return false;
            } else {
                Iterator<?> it = ((List) o).iterator();
                Object[] es;
                int offset;
                int size;
                synchronized (CopyOnWriteArrayList.this.lock) {
                    es = this.getArrayChecked();
                    offset = this.offset;
                    size = this.size;
                }

                int i = offset;

                for (int end = offset + size; i < end; ++i) {
                    if (!it.hasNext() || !Objects.equals(es[i], it.next())) {
                        return false;
                    }
                }

                return !it.hasNext();
            }
        }

        public E set(int index, E element) {
            synchronized (CopyOnWriteArrayList.this.lock) {
                this.rangeCheck(index);
                this.checkForComodification();
                E x = CopyOnWriteArrayList.this.set(this.offset + index, element);
                this.expectedArray = CopyOnWriteArrayList.this.getArray();
                return x;
            }
        }

        public E get(int index) {
            synchronized (CopyOnWriteArrayList.this.lock) {
                this.rangeCheck(index);
                this.checkForComodification();
                return CopyOnWriteArrayList.this.get(this.offset + index);
            }
        }

        public int size() {
            synchronized (CopyOnWriteArrayList.this.lock) {
                this.checkForComodification();
                return this.size;
            }
        }

        public boolean add(E element) {
            synchronized (CopyOnWriteArrayList.this.lock) {
                this.checkForComodification();
                CopyOnWriteArrayList.this.add(this.offset + this.size, element);
                this.expectedArray = CopyOnWriteArrayList.this.getArray();
                ++this.size;
                return true;
            }
        }

        public void add(int index, E element) {
            synchronized (CopyOnWriteArrayList.this.lock) {
                this.checkForComodification();
                this.rangeCheckForAdd(index);
                CopyOnWriteArrayList.this.add(this.offset + index, element);
                this.expectedArray = CopyOnWriteArrayList.this.getArray();
                ++this.size;
            }
        }

        public boolean addAll(Collection<? extends E> c) {
            synchronized (CopyOnWriteArrayList.this.lock) {
                Object[] oldArray = this.getArrayChecked();
                boolean modified = CopyOnWriteArrayList.this.addAll(this.offset + this.size, c);
                this.size += (this.expectedArray = CopyOnWriteArrayList.this.getArray()).length - oldArray.length;
                return modified;
            }
        }

        public boolean addAll(int index, Collection<? extends E> c) {
            synchronized (CopyOnWriteArrayList.this.lock) {
                this.rangeCheckForAdd(index);
                Object[] oldArray = this.getArrayChecked();
                boolean modified = CopyOnWriteArrayList.this.addAll(this.offset + index, c);
                this.size += (this.expectedArray = CopyOnWriteArrayList.this.getArray()).length - oldArray.length;
                return modified;
            }
        }

        public void clear() {
            synchronized (CopyOnWriteArrayList.this.lock) {
                this.checkForComodification();
                CopyOnWriteArrayList.this.removeRange(this.offset, this.offset + this.size);
                this.expectedArray = CopyOnWriteArrayList.this.getArray();
                this.size = 0;
            }
        }

        public E remove(int index) {
            synchronized (CopyOnWriteArrayList.this.lock) {
                this.rangeCheck(index);
                this.checkForComodification();
                E result = CopyOnWriteArrayList.this.remove(this.offset + index);
                this.expectedArray = CopyOnWriteArrayList.this.getArray();
                --this.size;
                return result;
            }
        }

        public boolean remove(Object o) {
            synchronized (CopyOnWriteArrayList.this.lock) {
                this.checkForComodification();
                int index = this.indexOf(o);
                if (index == -1) {
                    return false;
                } else {
                    this.remove(index);
                    return true;
                }
            }
        }

        public Iterator<E> iterator() {
            return this.listIterator(0);
        }

        public ListIterator<E> listIterator() {
            return this.listIterator(0);
        }

        public ListIterator<E> listIterator(int index) {
            synchronized (CopyOnWriteArrayList.this.lock) {
                this.checkForComodification();
                this.rangeCheckForAdd(index);
                return new COWSubListIterator(CopyOnWriteArrayList.this, index, this.offset, this.size);
            }
        }

        public List<E> subList(int fromIndex, int toIndex) {
            synchronized (CopyOnWriteArrayList.this.lock) {
                this.checkForComodification();
                if (fromIndex >= 0 && toIndex <= this.size && fromIndex <= toIndex) {
                    return CopyOnWriteArrayList.this.new COWSubList(this.expectedArray, fromIndex + this.offset, toIndex - fromIndex);
                } else {
                    throw new IndexOutOfBoundsException();
                }
            }
        }

        public void forEach(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            int i;
            int end;
            Object[] es;
            synchronized (CopyOnWriteArrayList.this.lock) {
                es = this.getArrayChecked();
                i = this.offset;
                end = i + this.size;
            }

            while (i < end) {
                action.accept(CopyOnWriteArrayList.elementAt(es, i));
                ++i;
            }

        }

        public void replaceAll(UnaryOperator<E> operator) {
            synchronized (CopyOnWriteArrayList.this.lock) {
                this.checkForComodification();
                CopyOnWriteArrayList.this.replaceAllRange(operator, this.offset, this.offset + this.size);
                this.expectedArray = CopyOnWriteArrayList.this.getArray();
            }
        }

        public void sort(Comparator<? super E> c) {
            synchronized (CopyOnWriteArrayList.this.lock) {
                this.checkForComodification();
                CopyOnWriteArrayList.this.sortRange(c, this.offset, this.offset + this.size);
                this.expectedArray = CopyOnWriteArrayList.this.getArray();
            }
        }

        public boolean removeAll(Collection<?> c) {
            Objects.requireNonNull(c);
            return this.bulkRemove((e) -> {
                return c.contains(e);
            });
        }

        public boolean retainAll(Collection<?> c) {
            Objects.requireNonNull(c);
            return this.bulkRemove((e) -> {
                return !c.contains(e);
            });
        }

        public boolean removeIf(Predicate<? super E> filter) {
            Objects.requireNonNull(filter);
            return this.bulkRemove(filter);
        }

        private boolean bulkRemove(Predicate<? super E> filter) {
            synchronized (CopyOnWriteArrayList.this.lock) {
                Object[] oldArray = this.getArrayChecked();
                boolean modified = CopyOnWriteArrayList.this.bulkRemove(filter, this.offset, this.offset + this.size);
                this.size += (this.expectedArray = CopyOnWriteArrayList.this.getArray()).length - oldArray.length;
                return modified;
            }
        }

        public Spliterator<E> spliterator() {
            synchronized (CopyOnWriteArrayList.this.lock) {
                return Spliterators.spliterator(this.getArrayChecked(), this.offset, this.offset + this.size, 1040);
            }
        }
    }

    private static class COWSubListIterator<E> implements ListIterator<E> {
        private final ListIterator<E> it;
        private final int offset;
        private final int size;

        COWSubListIterator(List<E> l, int index, int offset, int size) {
            this.offset = offset;
            this.size = size;
            this.it = l.listIterator(index + offset);
        }

        public boolean hasNext() {
            return this.nextIndex() < this.size;
        }

        public E next() {
            if (this.hasNext()) {
                return this.it.next();
            } else {
                throw new NoSuchElementException();
            }
        }

        public boolean hasPrevious() {
            return this.previousIndex() >= 0;
        }

        public E previous() {
            if (this.hasPrevious()) {
                return this.it.previous();
            } else {
                throw new NoSuchElementException();
            }
        }

        public int nextIndex() {
            return this.it.nextIndex() - this.offset;
        }

        public int previousIndex() {
            return this.it.previousIndex() - this.offset;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        public void add(E e) {
            throw new UnsupportedOperationException();
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);

            while (this.hasNext()) {
                action.accept(this.it.next());
            }

        }
    }
}
