package org.fxmisc.richtext.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StyleSpanTest {
    private void checkSpan(StyleSpan<String> span, String style, int pos, int len) {
        assertEquals(len, span.getLength(), "Incorrect length");
        assertEquals(pos, span.getStart(), "Incorrect position");
        assertEquals(style, span.getStyle(), "Incorrect style");
    }

    @Test
    @DisplayName("Style do not equal if their length or style is different")
    void equals() {
        assertEquals(
                new StyleSpan<>("myStyle", 2, 3),
                new StyleSpan<>("myStyle", 1, 3)
        );
        assertNotEquals(
                new StyleSpan<>("myStyle", 2, 3),
                new StyleSpan<>("myStyle", 2, 2)
        );
        assertNotEquals(
                new StyleSpan<>("myStyle", 2, 3),
                new StyleSpan<>("other", 2, 3)
        );
    }

    @Test
    @DisplayName("Create a valid span")
    void createValidSpan() {
        StyleSpan<String> a = new StyleSpan<>("myStyle", 10);
        StyleSpan<String> b = new StyleSpan<>("myStyle", 0, 10);
        checkSpan(a, "myStyle", 0, 10);
        checkSpan(b, "myStyle", 0, 10);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("Move span to a different location")
    void moveSpan() {
        StyleSpan<String> a = new StyleSpan<>("myStyle", 10);
        StyleSpan<String> b = new StyleSpan<>("myStyle", 2, 10);
        assertEquals(a, b); // position does not matter for equality
        assertEquals(a.hashCode(), b.hashCode());
        StyleSpan<String> c = a.moveTo(2);
        assertEquals(c, b);
        checkSpan(a, "myStyle", 0, 10);
        checkSpan(b, "myStyle", 2, 10);
        checkSpan(c, "myStyle", 2, 10);
    }

    @Test
    @DisplayName("Cannot create a span with a negative length")
    void cannotCreateWithNegativeLength() {
        // This is bad practice to throw exception in constructor, but that's the way it is. Other solutions would
        // have been to use a static create method, a factory, or to change length to 0
        assertThrows(IllegalArgumentException.class, () -> new StyleSpan<>("test", -1));
        // This is not consistent, as it doesn't throw
        checkSpan(new StyleSpan<>("test", 0, -1), "test", 0, -1);
    }
}
