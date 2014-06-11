package org.fxmisc.richtext.util.skin;

import java.util.Collections;
import java.util.List;

import javafx.css.CssMetaData;
import javafx.css.Styleable;

/**
 * Represents the view aspect of a JavaFX control. It defines how the control
 * is rendered visually on the screen. The implementations should either
 * implement {@link SimpleVisual}, or extend from {@link ComplexVisualBase}.
 */
public interface Visual {
    /**
     * Called to release resources associated with this Visual when it is no
     * longer being used, in particular to stop observing the control, i.e.
     * remove any listeners, etc.
     */
    void dispose();

    /**
     * Returns information about the extra styleable properties availabe on the
     * skin in addition to those available on the control itself. The default
     * implementation returns an empty list.
     */
    default List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return Collections.emptyList();
    }
}
