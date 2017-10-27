package org.fxmisc.richtext.model;

import java.util.Optional;

/**
 * Properly implements {@link SegmentOps} when implementing a non-text custom object, such as a shape or image,
 * and reduces boilerplate. Developers may want to override {@link #joinSeg(Object, Object)} and
 * {@link #joinStyle(Object, Object)}.
 *
 * @param <SEG> type of segment
 * @param <S> type of style
 */
public abstract class NodeSegmentOpsBase<SEG, S> extends SegmentOpsBase<SEG, S> {

    public NodeSegmentOpsBase(SEG empty) {
        super(empty);
    }

    @Override
    public char realCharAt(SEG seg, int index) {
        return '\ufffc';
    }

    @Override
    public String realGetText(SEG seg) {
        return "\ufffc";
    }

    @Override
    public SEG realSubSequence(SEG seg, int start, int end) {
        return seg;
    }

    @Override
    public SEG realSubSequence(SEG seg, int start) {
        return seg;
    }

    @Override
    public Optional<SEG> joinSeg(SEG currentSeg, SEG nextSeg) {
        return Optional.empty();
    }
}
