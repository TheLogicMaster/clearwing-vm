/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.util;

import java.io.UnsupportedOperationException;

/**
 *
 * @author shannah
 */
public class EnumSet<E> extends AbstractSet<E> {
    private final HashSet<E> internal = new HashSet<E>();
    private final Class<E> elementType;

    public EnumSet (Class<E> elementType) {
        this.elementType = elementType;
    }

    @Override
    public Iterator<E> iterator() {
        return internal.iterator();
    }

    @Override
    public int size() {
        return internal.size();
    }

    @Override
    public boolean add (E e) {
        return internal.add(e);
    }

    @Override
    public boolean remove (Object o) {
        return internal.remove(o);
    }

    public static <E extends Enum<E>> EnumSet<E> noneOf(Class<E> elementType) {
        return new EnumSet<>(elementType);
    }

    public static <E extends Enum<E>> EnumSet<E> allOf(Class<E> elementType) {
        EnumSet<E> set = new EnumSet<>(elementType);
        for (E e: (E[])elementType.getEnumConstants())
            set.internal.add(e);
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> copyOf(EnumSet<E> s) {
        try {
            return (EnumSet<E>)s.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public static <E extends Enum<E>> EnumSet<E> copyOf(Collection<E> c) {
        // Todo: Class generic type information
        throw new UnsupportedOperationException();
    }

    public static <E extends Enum<E>> EnumSet<E> complementOf(EnumSet<E> s) {
        EnumSet<E> set = allOf(s.elementType);
        for (E e: s)
            set.internal.remove(e);
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e) {
        EnumSet<E> result = noneOf(e.getDeclaringClass());
        result.add(e);
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2) {
        EnumSet<E> result = noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2, E e3) {
        EnumSet<E> result = noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        result.add(e3);
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2, E e3, E e4) {
        EnumSet<E> result = noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        result.add(e3);
        result.add(e4);
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2, E e3, E e4, E e5) {
        EnumSet<E> result = noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        result.add(e3);
        result.add(e4);
        result.add(e5);
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E first, E... rest) {
        EnumSet<E> result = noneOf(first.getDeclaringClass());
        result.add(first);
        result.addAll(Arrays.asList(rest));
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> range(E from, E to) {
        EnumSet<E> set = new EnumSet<>(from.getDeclaringClass());
        boolean started = false;
        for (E e: (E[])from.getDeclaringClass().getEnumConstants()) {
            if (e == from)
                started = true;
            if (started)
                set.internal.add(e);
            if (e == to)
                break;
        }
        return set;
    }
}
