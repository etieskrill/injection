package org.etieskrill.engine.util;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Collection;

/**
 * An implementation of {@link ArrayDeque} with a fixed size. If any element is added while the maximum capacity is
 * reached, the first element that was added from the perspective of the writing direction will be overridden, which is
 * the element at {@code tail} for {@link FixedArrayDeque#addFirst(Object)}, and {@code head} for
 * {@link FixedArrayDeque#addLast(Object)}.
 *
 * @param <E> the type of elements in the collection
 */
public class FixedArrayDeque<E> extends ArrayDeque<E> {

    private final @Getter int capacity;

    public FixedArrayDeque(int numElements) {
        super(numElements);
        this.capacity = numElements;
    }

    @Override
    public void addFirst(@NotNull E e) {
        if (size() > 0 && size() >= capacity)
            super.removeLast();
        super.addFirst(e);
    }

    @Override
    public void addLast(@NotNull E e) {
        if (size() > 0 && size() >= capacity)
            super.removeFirst();
        super.addLast(e);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        for (E element : c) addFirst(element);
        return true;
    }

    public boolean isFull() {
        return size() == capacity;
    }

    public void fill(E e) {
        for (int i = 0; i <= capacity; i++) push(e);
    }

}
