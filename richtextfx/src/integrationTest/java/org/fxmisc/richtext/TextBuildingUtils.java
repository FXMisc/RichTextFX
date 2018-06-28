package org.fxmisc.richtext;

import java.util.function.Function;

public class TextBuildingUtils {

    private TextBuildingUtils() {
        throw new IllegalStateException("Cannot construct an instance of TextBuildingUtils");
    }

    /** Builds {@code totalNumber} of lines that each have the index of the line as their text */
    public static String buildLines(int totalNumber) { return buildLines(totalNumber, String::valueOf); }

    public static String buildLines(int totalNumber, Function<Integer, String> textOnEachLine) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < totalNumber - 1; i++) {
            sb.append(textOnEachLine.apply(i)).append("\n");
        }
        sb.append(textOnEachLine.apply(totalNumber));
        return sb.toString();
    }
}
