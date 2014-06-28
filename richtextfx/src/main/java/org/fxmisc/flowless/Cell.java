package org.fxmisc.flowless;

import javafx.scene.Node;

@FunctionalInterface
public interface Cell<T, N extends Node> {
    static <T, N extends Node> Cell<T, N> wrapNode(N node) {
        return () -> node;
    }

    N getNode();

    /**
     * Indicates whether this cell can be reused to display different items.
     *
     * <p>Default implementation returns {@code false}.
     */
    default boolean isReusable() {
        return false;
    }

    /**
     * If this cell is reusable ({@link #isReusable()} returns {@code true}),
     * this method is called to display a different item. {@link #reset()}
     * will have been called before a call to this method.
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param index index of the new item
     * @param item the new item to display
     */
    default void updateItem(int index, T item) {
        throw new UnsupportedOperationException();
    }

    /**
     * Called to update index of a visible cell.
     *
     * <p>Default implementation does nothing.
     */
    default void updateIndex(int index) {
        // do nothing by default
    }

    /**
     * Called when this cell is no longer used to display its item.
     * If this cell is reusable, it may later be asked to display a different
     * item by a call to {@link #updateItem(int, Object)}.
     *
     * <p>Default implementation does nothing.
     */
    default void reset() {
        // do nothing by default
    }

    /**
     * Called when this cell is no longer going to be used at all.
     * {@link #reset()} will have been called before this method is invoked.
     *
     * <p>Default implementation does nothing.
     */
    default void dispose() {
        // do nothing by default
    }
}
