package org.fxmisc.flowless;

import javafx.scene.Node;

@FunctionalInterface
public interface CellFactory<T, C extends Node> {

    /**
     * Creates a new cell for the given item.
     */
    C createCell(int index, T item);

    /**
     * Creates a cell for the given item. May reuse {@code availableCell}.
     *
     * <p>This method shall not be called twice with the same
     * {@code availableCell} instance. If this method does not return
     * {@code availableCell} (adapted for item), {@code availableCell} shall be
     * disposed immediately. As a consequence, it is safe to reuse just some
     * parts of {@code availableCell} and return a new cell.
     *
     * <p>Default implementation creates a fresh cell by calling
     * {@link #createCell(Object)}.
     *
     * @param availableCell unused cell offered for reuse. The cell will have
     * been <em>reset</em> (see {@link #resetCell(Node)}) before it is passed
     * to this method.
     * @param item item to create a cell for
     * @return If {@code availableCell} can be reused to host {@code item},
     * returns {@code availableCell} adapted for {@code item}. Otherwise returns
     * a new cell initialized with {@code item}.
     */
    default C createCell(int index, T item, C availableCell) {
        return createCell(index, item);
    }

    /**
     * Called when {@code cell} is no longer used to display its item.
     * {@code cell} may later be adapted to display a different item by
     * {@link #createCell(Node, Object)}.
     *
     * <p>Default implementation does nothing.
     */
    default void resetCell(C cell) {
        // do nothing by default
    }

    /**
     * Called when {@code cell} is no longer going to be used at all.
     * {@code cell} will have been reset by {@link #resetCell(Node)} before
     * this method is invoked.
     *
     * <p>Default implementation does nothing.
     */
    default void disposeCell(C cell) {
        // do nothing by default
    }

    /**
     * Called to update index of a visible cell.
     */
    default void updateIndex(C cell, int newIndex) {
        // do nothing by default
    }
}
