package undo.impl;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import reactfx.Source;
import reactfx.Subscription;

public final class ObservingUndoManager<C> extends UndoManagerBase<C> {

    private final Subscription subscription;

    private boolean ignoreChanges;

    public ObservingUndoManager(Consumer<C> apply, Consumer<C> undo,
            BiFunction<C, C, Optional<C>> merge, Source<C> changeSource) {

        super(apply, undo, merge);
        subscription = changeSource.subscribe(this::changeObserved);
    }

    private void changeObserved(C change) {
        if(!ignoreChanges) {
            addChange(change);
        }
    }

    @Override
    public boolean undo() {
        ignoreChanges = true;
        boolean undone = super.undo();
        ignoreChanges = false;
        return undone;
    }

    @Override
    public boolean redo() {
        ignoreChanges = true;
        boolean redone = super.redo();
        ignoreChanges = false;
        return redone;
    }

    @Override
    public void close() {
        subscription.unsubscribe();
    }
}
