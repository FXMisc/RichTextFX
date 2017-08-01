package org.fxmisc.richtext.model;

/**
 * Properly implements {@link SegmentOps} when implementing a non-text custom object (e.g. shape, circle, image)
 * and reduces boilerplate. Developers only need to override {@link #realGetStyle(Object)} and
 * {@link #realSetStyle(Object, Object)}. Developers may also want to override {@link #join(Object, Object)}.
 *
 * @param <SEG> type of segment
 * @param <S> type of style
 */
public abstract class NodeSegmentOpsBase<SEG, S> extends SegmentOpsBase<SEG, S> {

    public NodeSegmentOpsBase(SEG empty) {
        super(empty);
    }

    @Override
    public int realLength(SEG seg) {
        return 1;
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
}
