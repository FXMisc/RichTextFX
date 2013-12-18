package codearea.undo;

public interface UndoManager {
    boolean undo();
    boolean redo();
    boolean canUndo();
    boolean canRedo();
    void preventMerge();
}
