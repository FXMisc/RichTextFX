package org.fxmisc.richtext;

/**
 * Possible dragging states.
 */
public enum DragState {
    /** No dragging is happening. */
    NO_DRAG,

    /** Mouse has been pressed, but drag has not been detected yet. */
    POTENTIAL_DRAG,

    /** Drag in progress. */
    DRAG,
}
