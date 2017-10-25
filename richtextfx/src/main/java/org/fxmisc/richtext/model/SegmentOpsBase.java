package org.fxmisc.richtext.model;

import java.util.Optional;

/**
 * Provides a base for properly implementing the {@link SegmentOps} interface and reduces boilerplate,
 * so that a developer only needs to implement methods for real segments, not empty ones. Optionally,
 * {@link #joinSeg(Object, Object)} and {@link #joinStyle(Object, Object)} can be overridden as well.
 *
 * @param <SEG> the type of segment
 * @param <S> the type of style
 */
public abstract class SegmentOpsBase<SEG, S> implements SegmentOps<SEG, S> {

    private final SEG empty;

    /**
     * Creates a {@link SegmentOpsBase} that returns {@code emptySeg} every time an empty segment should be returned
     */
    public SegmentOpsBase(SEG emptySeg) {
        this.empty = emptySeg;
    }

    @Override
    public final char charAt(SEG seg, int index) {
        return length(seg) == 0 ? '\0' : realCharAt(seg, index);
    }
    public abstract char realCharAt(SEG seg, int index);

    @Override
    public final String getText(SEG seg) {
        return length(seg) == 0 ? "" : realGetText(seg);
    }
    public abstract String realGetText(SEG seg);

    @Override
    public final SEG subSequence(SEG seg, int start, int end) {
        if (start < 0) {
            throw new IllegalArgumentException("Start cannot be negative. Start=" + start);
        }
        if (end > length(seg)) {
            throw new IllegalArgumentException(
                    String.format("End cannot be greater than segment's length. End=%s Length=%s", end, length(seg))
            );
        }
        return length(seg) == 0 || start == end
                ? empty
                : realSubSequence(seg, start, end);
    }
    public abstract SEG realSubSequence(SEG seg, int start, int end);

    @Override
    public final SEG subSequence(SEG seg, int start) {
        return length(seg) == 0 || start == length(seg)
                ? empty
                : realSubSequence(seg, start);
    }
    public SEG realSubSequence(SEG seg, int start) {
        return realSubSequence(seg, start, length(seg));
    }

    @Override
    public final SEG createEmptySeg() {
        return empty;
    }

    @Override
    public Optional<S> joinStyle(S currentStyle, S nextStyle) {
        return Optional.empty();
    }
}
