package codearea.control;

import javafx.beans.value.ObservableBooleanValue;
import undo.UndoManager;
import undo.UndoManagerFactory;

/**
 * Undo/redo actions for {@link TextEditingArea}.
 *
 * @param <S> type of style that can be applied to text.
 */
public interface UndoActions<S> {

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
