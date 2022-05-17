package java.util.concurrent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Totally not sketchy ConcurrentHashMap implementation
 */
public class ConcurrentHashMap<K,V> extends HashMap<K,V> implements ConcurrentMap<K,V> {

    @Override
    public synchronized boolean isEmpty () {
        return super.isEmpty();
    }

    @Override
    public synchronized V get (Object key) {
        return super.get(key);
    }

    @Override
    public synchronized boolean containsKey (Object key) {
        return super.containsKey(key);
    }

    @Override
    public synchronized V put (K key, V value) {
        return super.put(key, value);
    }

    @Override
    public synchronized void putAll (Map<? extends K, ? extends V> m) {
        super.putAll(m);
    }

    @Override
    public synchronized V remove (Object key) {
        return super.remove(key);
    }

    @Override
    public synchronized void clear () {
        super.clear();
    }

    @Override
    public synchronized boolean containsValue (Object value) {
        return super.containsValue(value);
    }

    @Override
    public synchronized Set<K> keySet () {
        return new HashMap<>(this).keySet();
    }

    @Override
    public synchronized Collection<V> values () {
        return new HashMap<>(this).values();
    }

    @Override
    public synchronized Set<Entry<K, V>> entrySet () {
        return new HashMap<>(this).entrySet();
    }

    @Override
    public synchronized V putIfAbsent (K key, V value) {
        return super.putIfAbsent(key, value);
    }

    @Override
    public synchronized boolean remove (Object key, Object value) {
        return super.remove(key, value);
    }

    @Override
    public synchronized boolean replace (K key, V oldValue, V newValue) {
        return super.replace(key, oldValue, newValue);
    }

    @Override
    public synchronized V replace (K key, V value) {
        return super.replace(key, value);
    }

    @Override
    public synchronized Object clone () throws CloneNotSupportedException {
        return super.clone();
    }
}
