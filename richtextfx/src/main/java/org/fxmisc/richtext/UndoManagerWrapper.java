package org.fxmisc.richtext;

import org.fxmisc.undo.UndoManager;

/**
 * Created by jordan on 1/25/16.
 */
public class UndoManagerWrapper {

    private UndoManager undoManager;
    public UndoManager getUndoManager() { return undoManager; }
    public void setUndoManager(UndoManager undoManager) {
        this.undoManager.close();
        this.undoManager = undoManager;
    }

    UndoManagerWrapper(UndoManager undoManager) {
        this.undoManager = undoManager;
    }
}
