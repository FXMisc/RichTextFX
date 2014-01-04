package codearea.control;

import codearea.undo.UndoManager;

public interface UndoActions {

    UndoManager getUndoManager();

    default void undo() { getUndoManager().undo(); }

    default void redo() { getUndoManager().redo(); }

    default boolean canUndo() { return getUndoManager().canUndo(); }

    default boolean canRedo() { return getUndoManager().canRedo(); }
}
