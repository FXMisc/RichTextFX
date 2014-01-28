package undo;

import javafx.beans.value.ObservableBooleanValue;

public interface UndoManager {
    boolean undo();

    boolean redo();

    ObservableBooleanValue undoAvailableProperty();
    boolean isUndoAvailable();

    ObservableBooleanValue redoAvailableProperty();
    boolean isRedoAvailable();

    /**
     * Prevents the next change from being merged with the last one.
     */
    void preventMerge();

    /**
     * Stops listening to change events.
     */
    void close();
}
