package codearea.control;

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
    void setPlainUndoManager(UndoManagerFactory<PlainTextChange> undoManagerFactory);
    void setRichUndoManager(UndoManagerFactory<RichTextChange<S>> undoManagerFactory);

    default void undo() { getUndoManager().undo(); }

    default void redo() { getUndoManager().redo(); }

    default boolean canUndo() { return getUndoManager().canUndo(); }

    default boolean canRedo() { return getUndoManager().canRedo(); }
}
