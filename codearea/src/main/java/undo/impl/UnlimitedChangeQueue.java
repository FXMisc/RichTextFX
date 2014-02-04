package undo.impl;

import java.util.ArrayList;

public class UnlimitedChangeQueue<C> implements ChangeQueue<C> {

    private final ArrayList<C> changes = new ArrayList<C>();
    private int currentPosition = 0;

    @Override
    public final boolean hasNext() {
        return currentPosition < changes.size();
    }

    @Override
    public final boolean hasPrev() {
        return currentPosition > 0;
    }

    @Override
    public final C next() {
        return changes.get(currentPosition++);
    }

    @Override
    public final C prev() {
        return changes.get(--currentPosition);
    }

    @Override
    @SafeVarargs
    public final void push(C... changes) {
        this.changes.subList(currentPosition, this.changes.size()).clear();
        for(C c: changes) {
            this.changes.add(c);
        }
        currentPosition += changes.length;
    }

}
