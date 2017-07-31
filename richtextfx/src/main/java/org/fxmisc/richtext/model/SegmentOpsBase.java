package org.fxmisc.richtext.model;

import java.util.Optional;

/**
 * Properly implements the {@link SegmentOps} interface and reduces boilerplate, so that developer only needs to
 * implement methods for real segments, not empty ones. Optionally, {@link #join(Object, Object)} can be overridden
 * as well.
 *
 * @param <SEG> the type of segment
 * @param <S> the type of style
 */
public abstract class SegmentOpsBase<SEG, S> implements SegmentOps<SEG, S> {

    private final SEG empty;

    public SegmentOpsBase(SEG emptySEg) {
        this.empty = emptySEg;
    }

    @Override
    public final int length(SEG seg) {
        return seg == empty ? 0 : realLength(seg);
    }
    public abstract int realLength(SEG seg);

    @Override
    public final char charAt(SEG seg, int index) {
        return seg == empty ? '\0' : realCharAt(seg, index);
    }
    public abstract char realCharAt(SEG seg, int index);

    @Override
    public final String getText(SEG seg) {
        return seg == empty ? "\0" : realGetText(seg);
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
        return seg == empty || start == end
                ? empty
                : realSubsequence(seg, start, end);
    }
    public abstract SEG realSubsequence(SEG seg, int start, int end);

    @Override
    public final SEG subSequence(SEG seg, int start) {
        return seg == empty || length(seg) == start
                ? empty
                : realSubsequence(seg, start);
    }
    public SEG realSubsequence(SEG seg, int start) {
        return realSubsequence(seg, start, length(seg));
    }

    @Override
    public final S getStyle(SEG seg) {
        return seg == empty ? null : realGetStyle(seg);
    }
    public abstract S realGetStyle(SEG seg);

    @Override
    public final SEG setStyle(SEG seg, S style) {
        return seg == empty ? empty : realSetStyle(seg, style);
    }
    public abstract SEG realSetStyle(SEG seg, S style);

    @Override
    public final Optional<SEG> join(SEG currentSeg, SEG nextSeg) {
        return Optional.empty();
    }

    @Override
    public final SEG createEmpty() {
        return empty;
    }
}
