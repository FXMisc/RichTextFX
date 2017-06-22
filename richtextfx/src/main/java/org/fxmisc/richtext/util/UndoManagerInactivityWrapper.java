package org.fxmisc.richtext.util;

import javafx.beans.value.ObservableBooleanValue;
import org.fxmisc.undo.UndoManager;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.value.Val;

import java.time.Duration;

/**
 * A wrapper around an {@link UndoManager} that prevents the next emitted change from merging with the previous
 * one after a period of inactivity (i.e., the UndoManager's {@code changeSource} has not emitted an event
 * after a specified period of time.
 *
 * @param <C> the type of change the UndoManager can undo/redo
 */
final class UndoManagerInactivityWrapper<C> implements UndoManager<C> {

    private final UndoManager<C> delegate;
    private final Subscription subscription;

    /**
     * Wraps an {@link UndoManager} and prevents the next emitted change from merging with the previous one
     * after a period of inactivity (i.e., the {@code changeSource} has not emitted an event for
     * {@code preventMergeDelay}). <b>Note:</b> there is no check that insures that the {@code changeSource}
     * parameter is the same one used by the {@code undoManager} parameter
     */
    public UndoManagerInactivityWrapper(UndoManager<C> undoManager, EventStream<C> changeSource, Duration preventMergeDelay) {
        this.delegate = undoManager;
        subscription = changeSource.successionEnds(preventMergeDelay).subscribe(ignore -> preventMerge());
    }

    @Override
    public boolean undo() {
        return delegate.undo();
    }

    @Override
    public boolean redo() {
        return delegate.redo();
    }

    @Override
    public Val<Boolean> undoAvailableProperty() {
        return delegate.undoAvailableProperty();
    }

    @Override
    public boolean isUndoAvailable() {
        return delegate.isUndoAvailable();
    }

    @Override
    public Val<C> nextToUndoProperty() {
        return delegate.nextToUndoProperty();
    }

    @Override
    public C getNextToUndo() {
        return delegate.getNextToUndo();
    }

    @Override
    public Val<C> nextToRedoProperty() {
        return delegate.nextToRedoProperty();
    }

    @Override
    public C getNextToRedo() {
        return delegate.getNextToRedo();
    }

    @Override
    public Val<Boolean> redoAvailableProperty() {
        return delegate.redoAvailableProperty();
    }

    @Override
    public boolean isRedoAvailable() {
        return delegate.isRedoAvailable();
    }

    @Override
    public ObservableBooleanValue performingActionProperty() {
        return delegate.performingActionProperty();
    }

    @Override
    public boolean isPerformingAction() {
        return delegate.isPerformingAction();
    }

    @Override
    public void preventMerge() {
        delegate.preventMerge();
    }

    @Override
    public void forgetHistory() {
        delegate.forgetHistory();
    }

    @Override
    public UndoPosition getCurrentPosition() {
        return delegate.getCurrentPosition();
    }

    @Override
    public void mark() {
        delegate.mark();
    }

    @Override
    public ObservableBooleanValue atMarkedPositionProperty() {
        return delegate.atMarkedPositionProperty();
    }

    @Override
    public boolean isAtMarkedPosition() {
        return delegate.isAtMarkedPosition();
    }

    @Override
    public void close() {
        subscription.unsubscribe();
        delegate.close();
    }
}
