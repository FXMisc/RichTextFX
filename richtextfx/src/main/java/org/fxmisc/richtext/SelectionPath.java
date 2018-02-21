package org.fxmisc.richtext;

import javafx.scene.control.IndexRange;
import org.reactfx.value.Val;

/**
 * Default implementation for {@link SelectionPathBase}. It adds the style class {@code "selection"}.
 */
public class SelectionPath extends SelectionPathBase {

    public SelectionPath(Val<IndexRange> range) {
        super(range);
        getStyleClass().add("selection");
    }

}
