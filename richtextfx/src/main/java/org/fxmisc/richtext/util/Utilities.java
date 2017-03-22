package org.fxmisc.richtext.util;

import javafx.scene.control.IndexRange;

/**
 * A class of static methods or properties used throughout the project.
 */
public class Utilities {

    /**
     * Index range [0, 0).
     */
    public static final IndexRange EMPTY_RANGE = new IndexRange(0, 0);

    /**
     * Clamps the given {@code val} to insure it is within the bounds of {@code min} or {@code max}.
     */
    public static int clamp(int min, int val, int max) {
        return val < min ? min
                : val > max ? max
                : val;
    }
}
