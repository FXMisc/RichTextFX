package org.fxmisc.richtext.keyboard.navigation;

import org.fxmisc.richtext.GenericStyledArea;

import java.util.Arrays;

public final class Utils {

    private Utils() {
        throw new IllegalStateException("Cannot instantiate Utils class");
    }

    public static int entityStart(int entityIndex, String[] array) {
        if (entityIndex == 0) {
            return 0;
        } else {
            return Arrays.stream(array)
                    .map(String::length)
                    .limit(entityIndex)
                    .reduce(0, (a, b) -> a + b)
                    + entityIndex; // for delimiter characters
        }
    }

    public static int entityEnd(int entityIndex, String[] array, GenericStyledArea<?, ?, ?> area) {
        if (entityIndex == array.length - 1) {
            return area.getLength();
        } else {
            return entityStart(entityIndex + 1, array) - 1;
        }
    }
}
