package codearea.undo;

public interface UndoManager {
    boolean undo();
    boolean redo();
    boolean canUndo();
    boolean canRedo();

    /**
     * Prevents the next change from being merged with the last one.
     */
    void preventMerge();

    /**
     * Stops listening to change events.
     */
    void close();
}
