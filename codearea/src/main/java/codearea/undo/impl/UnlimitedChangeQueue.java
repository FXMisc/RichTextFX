package codearea.undo.impl;

import java.util.ArrayList;

public class UnlimitedChangeQueue<C> implements ChangeQueue<C> {

    private final ArrayList<C> changes = new ArrayList<C>();
    private int currentPosition = 0;

    @Override
    public final boolean hasNext() {
        return (currentPosition < changes.size());
    }

    @Override
    public final boolean hasPrev() {
        return (currentPosition > 0);
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
    public final void push(C... change) {
        changes.subList(currentPosition, changes.size()).clear();
        for(C c: change) {
            changes.add(c);
        }
        currentPosition += change.length;
    }

}
