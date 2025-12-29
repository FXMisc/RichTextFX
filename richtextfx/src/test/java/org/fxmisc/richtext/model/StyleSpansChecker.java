package org.fxmisc.richtext.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StyleSpansChecker<T> {
    private final StyleSpans<T> styleSpans;

    public StyleSpansChecker(StyleSpans<T> styleSpans) {
        this.styleSpans = styleSpans;
    }

    public void check(int length, T[] styles, int... ranges) {
        if(ranges.length % 2 == 1 || styles.length != ranges.length / 2) {
            throw new IllegalArgumentException("Ranges must come in pair [start;end] and correspond to the style count");
        }
        assertEquals(length, styleSpans.length());
        assertEquals(ranges.length/2, styleSpans.getSpanCount(), "Style segment count invalid");
        for (int i = 0; i < ranges.length/2 ; i++) {
            StyleSpan<T> style = styleSpans.getStyleSpan(i);
            assertEquals(ranges[i*2], style.getStart(), "Start not matching for " + i);
            assertEquals(ranges[i*2 + 1] - ranges[i*2], style.getLength(), "Length not matching for " + i);
            assertEquals(styles[i], style.getStyle(), "Incorrect style for " + i);
        }
    }

    public static <T> void checkStyle(Paragraph<?, ?, T> paragraph, int length, T[] styles, int... ranges) {
        checkStyle(paragraph.getStyleSpans(), length, styles, ranges);
    }

    public static <T> void checkStyle(StyleSpans<T> styleSpans, int length, T[] styles, int... ranges) {
        new StyleSpansChecker<T>(styleSpans).check(length, styles, ranges);
    }
}
