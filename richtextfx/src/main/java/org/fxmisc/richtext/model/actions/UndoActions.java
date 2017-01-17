package org.fxmisc.richtext.model.actions;

import javafx.beans.value.ObservableBooleanValue;

import org.fxmisc.richtext.model.actions.TextEditingArea;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.UndoManagerFactory;

/**
 * Undo/redo actions for {@link TextEditingArea}.
 */
public interface UndoActions {

    /**
     * Undo manager of this text area.
     */
    UndoManager getUndoManager();
    void setUndoManager(UndoManagerFactory undoManagerFactory);

    default void undo() { getUndoManager().undo(); }

    default void redo() { getUndoManager().redo(); }

    default boolean isUndoAvailable() { return getUndoManager().isUndoAvailable(); }
    default ObservableBooleanValue undoAvailableProperty() { return getUndoManager().undoAvailableProperty(); }

    default boolean isRedoAvailable() { return getUndoManager().isRedoAvailable(); }
    default ObservableBooleanValue redoAvailableProperty() { return getUndoManager().redoAvailableProperty(); }
}
