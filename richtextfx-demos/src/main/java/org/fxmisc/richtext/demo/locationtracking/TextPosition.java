package org.fxmisc.richtext.demo.locationtracking;

import javafx.beans.property.*;

/**
 * Models a text offset/position within the document.  A read-only property is provided for tracking.
 * The update method is used to update the position with a change to the text offset.
 */
public class TextPosition {
    private ReadOnlyIntegerWrapper posProperty = new ReadOnlyIntegerWrapper();

    public TextPosition(int offset) {
        this.posProperty.setValue(offset);
    }

    public final ReadOnlyIntegerProperty positionProperty() {
        return this.posProperty.getReadOnlyProperty();
    }

    public void update(int diff) {
        int n = this.get() + diff;

        if (n < 0) {
            n = 0;
        }

        this.posProperty.set(n);
    }

    public int get() {
        return this.posProperty.getValue();
    }
}
