package undo.impl;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import reactfx.Source;
import reactfx.Subscription;
import undo.UndoManager;

public class UndoManagerImpl<C> implements UndoManager {

    private final ChangeQueue<C> queue = new UnlimitedChangeQueue<>();
    private final Consumer<C> apply;
    private final Consumer<C> undo;
    private final BiFunction<C, C, Optional<C>> merge;
    private final Subscription subscription;

    boolean canMerge;

    private boolean ignoreChanges;

    public UndoManagerImpl(Consumer<C> apply, Consumer<C> undo, BiFunction<C, C, Optional<C>> merge, Source<C> changeSource) {
        this.apply = apply;
        this.undo = undo;
        this.merge = merge;
        subscription = changeSource.subscribe(this::changeObserved);
    }

    @Override
    public void close() {
        subscription.unsubscribe();
    }

    @Override
    public boolean undo() {
        ignoreChanges = true;
        boolean undone = false;
        if(canUndo()) {
            undo.accept(queue.prev());
            canMerge = false;
            undone = true;
        }
        ignoreChanges = false;
        return undone;
    }

    @Override
    public boolean redo() {
        ignoreChanges = true;
        boolean redone = false;
        if(canRedo()) {
            apply.accept(queue.next());
            canMerge = false;
            redone = true;
        }
        ignoreChanges = false;
        return redone;
    }

    @Override
    public boolean canUndo() {
        return queue.hasPrev();
    }

    @Override
    public boolean canRedo() {
        return queue.hasNext();
    }

    @Override
    public void preventMerge() {
        canMerge = false;
    }

    private void changeObserved(C change) {
        if(!ignoreChanges) {
            addChange(change);
        }
    }

    @SuppressWarnings("unchecked")
    private void addChange(C change) {
        if(canMerge && queue.hasPrev()) {
            C prev = queue.prev();
            queue.push(merge(prev, change));
        } else {
            queue.push(change);
        }
        canMerge = true;
    }

    @SuppressWarnings("unchecked")
    private C[] merge(C c1, C c2) {
        Optional<C> merged = merge.apply(c1, c2);
        if(merged.isPresent()) {
            return (C[]) new Object[] { merged.get() };
        } else {
            return (C[]) new Object[] { c1, c2 };
        }
    }
}
