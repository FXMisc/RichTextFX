package undo.impl;

import inhibeans.Hold;
import inhibeans.value.Indicator;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;
import reactfx.Source;
import reactfx.Subscription;
import undo.UndoManager;

public class UndoManagerImpl<C> implements UndoManager {

    private final ChangeQueue<C> queue = new UnlimitedChangeQueue<>();
    private final Consumer<C> apply;
    private final Consumer<C> undo;
    private final BiFunction<C, C, Optional<C>> merge;
    private final Subscription subscription;

    private final BooleanBinding undoAvailable = new BooleanBinding() {
        @Override
        protected boolean computeValue() {
            return isUndoAvailable();
        }
    };

    private final BooleanBinding redoAvailable = new BooleanBinding() {
        @Override
        protected boolean computeValue() {
            return isRedoAvailable();
        }
    };

    boolean canMerge;

    private final Indicator ignoreChanges = new Indicator();

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
        if(isUndoAvailable()) {
            try(Hold h = ignoreChanges.on()) {
                undo.accept(queue.prev());
            }
            canMerge = false;
            undoAvailable.invalidate();
            redoAvailable.invalidate();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean redo() {
        if(isRedoAvailable()) {
            try(Hold h = ignoreChanges.on()) {
                apply.accept(queue.next());
            }
            canMerge = false;
            undoAvailable.invalidate();
            redoAvailable.invalidate();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isUndoAvailable() {
        return queue.hasPrev();
    }

    @Override
    public ObservableBooleanValue undoAvailableProperty() {
        return undoAvailable;
    }

    @Override
    public boolean isRedoAvailable() {
        return queue.hasNext();
    }

    @Override
    public ObservableBooleanValue redoAvailableProperty() {
        return redoAvailable;
    }

    @Override
    public void preventMerge() {
        canMerge = false;
    }

    private void changeObserved(C change) {
        if(!ignoreChanges.isOn()) {
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
        undoAvailable.invalidate();
        redoAvailable.invalidate();
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
