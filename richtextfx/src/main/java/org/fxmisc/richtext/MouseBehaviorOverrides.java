package org.fxmisc.richtext;

import java.util.function.IntConsumer;

/**
 * Hook methods that allow proper overriding of default mouse behavior.
 */
public interface MouseBehaviorOverrides {

    /**
     * Defines how to handle an event in which the user has selected some text, dragged it to a
     * new location within the area, and released the mouse at some character {@code index}
     * within the area.
     *
     * <p>By default, this will relocate the selected text to the character index where the mouse
     * was released. To override it, use {@link #setOnSelectionDrop(IntConsumer)}.
     */
    IntConsumer getOnSelectionDrop();
    void setOnSelectionDrop(IntConsumer consumer);

}
