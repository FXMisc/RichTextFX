package org.fxmisc.richtext;

import java.util.Objects;

public class StyleSpan<S> {

    private final S style;
    private final int length;

    public StyleSpan(S style, int length) {
        if(length < 0) {
            throw new IllegalArgumentException("StyleSpan's length cannot be negative");
        }

        this.style = style;
        this.length = length;
    }

    public S getStyle() {
        return style;
    }

    public int getLength() {
        return length;
    }

    /**
     * Two {@code StyleSpan}s are considered equal if they have equal length and
     * equal style.
     */
    @Override
    public boolean equals(Object other) {
        if(other instanceof StyleSpan) {
            StyleSpan<?> that = (StyleSpan<?>) other;
            return this.length == that.length
                    && Objects.equals(this.style, that.style);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(style, length);
    }
}
