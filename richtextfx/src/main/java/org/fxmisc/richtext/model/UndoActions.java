package org.fxmisc.richtext.model;

import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.UndoManagerFactory;
import org.reactfx.value.Val;

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
    default Val<Boolean> undoAvailableProperty() { return getUndoManager().undoAvailableProperty(); }

    default boolean isRedoAvailable() { return getUndoManager().isRedoAvailable(); }
    default Val<Boolean> redoAvailableProperty() { return getUndoManager().redoAvailableProperty(); }
}
