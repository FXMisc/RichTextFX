package org.fxmisc.richtext.model;

import java.util.Objects;

/**
 * Essentially, a {@link org.reactfx.util.Tuple2} of a given style {@link S} that spans a given length.
 *
 * @param <S> the style type
 */
public class StyleSpan<S> {
    private final S style;
    private final int length;
    private int startPos = 0;

    /**
     * Creates a style span. Note: length cannot be negative.
     */
    public StyleSpan(S style, int length) {
        if(length < 0) {
            throw new IllegalArgumentException("StyleSpan's length cannot be negative");
        }
        this.style = style;
        this.length = length;
    }

    StyleSpan(S style, int start, int length) {
        this.style = style;
        this.startPos = start;
        this.length = length;
    }

    public S getStyle() {
        return style;
    }

    public int getLength() {
        return length;
    }

    // TODO This one is only used in the factory, can't se make the start position immutable and use "moveTo"
    //      instead (essentially turning this into an immutable style)
    void setStart( int start ) {
        startPos = start;
    }

    StyleSpan<S> moveTo(int start) {
        return new StyleSpan<>(style, start, length);
    }

    int getStart() {
        return startPos;
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

    @Override
    public String toString() {
        return String.format("StyleSpan[length=%s, style=%s]", length, style);
    }
}
