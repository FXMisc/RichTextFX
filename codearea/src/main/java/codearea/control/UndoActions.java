package codearea.control;

import undo.UndoManager;
import undo.UndoManagerProvider;

/**
 * Undo/redo actions for {@link TextEditingArea}.
 *
 * @param <C> type of undoable changes.
 */
public interface UndoActions<C> {

    /**
     * Undo manager of this text area.
     */
    UndoManager getUndoManager();
    void setUndoManager(UndoManagerProvider<C> undoManagerProvider);

    default void undo() { getUndoManager().undo(); }

    default void redo() { getUndoManager().redo(); }

    default boolean canUndo() { return getUndoManager().canUndo(); }

    default boolean canRedo() { return getUndoManager().canRedo(); }
}
