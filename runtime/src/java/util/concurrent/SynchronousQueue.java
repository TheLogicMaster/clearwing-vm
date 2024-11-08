package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;

public class SynchronousQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {

    private final ArrayBlockingQueue<E> queue = new ArrayBlockingQueue<>(4);
    
    @Override
    public boolean add(E e) {
        return queue.add(e);
    }

    @Override
    public boolean offer(E e) {
        return queue.offer(e);
    }

    @Override
    public E remove() {
        return queue.remove();
    }

    @Override
    public E poll() {
        return queue.poll();
    }

    @Override
    public E element() {
        return queue.element();
    }

    @Override
    public E peek() {
        return queue.peek();
    }

    @Override
    public void put(E e) throws InterruptedException {
        queue.put(e);
    }

    @Override
    public boolean offer(E e, long l, TimeUnit timeUnit) throws InterruptedException {
        return queue.offer(e, l, timeUnit);
    }

    @Override
    public E take() throws InterruptedException {
        return queue.take();
    }

    @Override
    public E poll(long l, TimeUnit timeUnit) throws InterruptedException {
        return queue.poll(l, timeUnit);
    }

    @Override
    public int remainingCapacity() {
        return queue.remainingCapacity();
    }

    @Override
    public boolean remove(Object o) {
        return queue.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return queue.containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        return queue.addAll(collection);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return queue.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return queue.retainAll(collection);
    }

    @Override
    public void clear() {
        queue.clear();
    }

    @Override
    public boolean equals(Object o) {
        return queue.equals(o);
    }

    @Override
    public int hashCode() {
        return queue.hashCode();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return queue.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return queue.iterator();
    }

    @Override
    public Object[] toArray() {
        return queue.toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return queue.toArray(ts);
    }

    @Override
    public int drainTo(Collection<? super E> collection) {
        return queue.drainTo(collection);
    }

    @Override
    public int drainTo(Collection<? super E> collection, int i) {
        return queue.drainTo(collection, i);
    }
}
