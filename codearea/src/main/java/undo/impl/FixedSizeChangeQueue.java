package undo.impl;

import java.util.NoSuchElementException;

public class FixedSizeChangeQueue<C> implements ChangeQueue<C> {

    private final C[] changes;
    private final int capacity;
    private int start = 0;
    private int size = 0;

    // current position is always from the interval [0, size],
    // i.e. not offset by start
    private int currentPosition = 0;

    @SuppressWarnings("unchecked")
    public FixedSizeChangeQueue(int capacity) {
        if(capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }

        this.capacity = capacity;
        this.changes = (C[]) new Object[capacity];
    }

    @Override
    public boolean hasNext() {
        return currentPosition < size;
    }

    @Override
    public boolean hasPrev() {
        return currentPosition > 0;
    }

    @Override
    public C next() {
        if(currentPosition < size) {
            return changes[(start + currentPosition++) % capacity];
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public C prev() {
        if(currentPosition > 0) {
            return changes[(start + --currentPosition) % capacity];
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    @SafeVarargs
    public final void push(C... changes) {
        for(C c: changes) {
            this.changes[(start + currentPosition++) % capacity] = c;
        }

        if(currentPosition > capacity) {
            start = (start + currentPosition) % capacity;
            currentPosition = capacity;
            size = capacity;
        } else {
            size = currentPosition;
        }
    }

}
