package codearea.undo.impl;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import codearea.undo.UndoManager;

public abstract class UndoManagerBase<C> implements UndoManager {

    public static interface ChangeMerger<C> {
        C merge(C c1, C c2);
    }

    private final ChangeQueue<C> queue = new UnlimitedChangeQueue<>();
    boolean canMerge;

    private final Consumer<C> apply;
    private final Consumer<C> undo;
    private final BiFunction<C, C, Optional<C>> merge;

    protected UndoManagerBase(Consumer<C> apply, Consumer<C> undo, BiFunction<C, C, Optional<C>> merge) {
        this.apply = apply;
        this.undo = undo;
        this.merge = merge;
    }

    @SuppressWarnings("unchecked")
    protected void addChange(C change) {
        if(canMerge && queue.hasPrev()) {
            C prev = queue.prev();
            queue.push(merge(prev, change));
        } else {
            queue.push(change);
        }
        canMerge = true;
    }

    @Override
    public boolean undo() {
        if(canUndo()) {
            undo.accept(queue.prev());
            canMerge = false;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean redo() {
        if(canRedo()) {
            apply.accept(queue.next());
            canMerge = false;
            return true;
        } else {
            return false;
        }
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
