package undo.impl;

import java.util.NoSuchElementException;

public class ZeroSizeChangeQueue<C> implements ChangeQueue<C> {

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public boolean hasPrev() {
        return false;
    }

    @Override
    public C next() {
        throw new NoSuchElementException();
    }

    @Override
    public C prev() {
        throw new NoSuchElementException();
    }

    @Override
    @SafeVarargs
    public final void push(C... changes) {
        // do nothing
    }

}
