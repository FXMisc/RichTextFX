package org.fxmisc.richtext.demo.richtext;

import java.util.Optional;

import org.fxmisc.richtext.model.SegmentOps;

public class LinkedImageOps<S> implements SegmentOps<LinkedImage<S>, S> {

    private final EmptyLinkedImage<S> emptySeg = new EmptyLinkedImage<>();

    @Override
    public int length(LinkedImage<S> seg) {
        return seg == emptySeg ? 0 : 1;
    }

    @Override
    public char charAt(LinkedImage<S> seg, int index) {
        return seg == emptySeg ? '\0' : '\ufffc';
    }

    @Override
    public String getText(LinkedImage<S> seg) {
        return seg == emptySeg ? "" : "\ufffc";
    }

    @Override
    public LinkedImage<S> subSequence(LinkedImage<S> seg, int start, int end) {
        if (start < 0) {
            throw new IllegalArgumentException("Start cannot be negative. Start = " + start);
        }
        if (end > length(seg)) {
            throw new IllegalArgumentException("End cannot be greater than segment's length");
        }
        return start == 0 && end == 1
                ? seg
                : emptySeg;
    }

    @Override
    public LinkedImage<S> subSequence(LinkedImage<S> seg, int start) {
        if (start < 0) {
            throw new IllegalArgumentException("Start cannot be negative. Start = " + start);
        }
        return start == 0
                ? seg
                : emptySeg;
    }

    @Override
    public S getStyle(LinkedImage<S> seg) {
        return seg.getStyle();
    }

    @Override
    public LinkedImage<S> setStyle(LinkedImage<S> seg, S style) {
        return seg == emptySeg ? emptySeg : seg.setStyle(style);
    }

    @Override
    public Optional<LinkedImage<S>> join(LinkedImage<S> currentSeg, LinkedImage<S> nextSeg) {
        return Optional.empty();
    }

    @Override
    public LinkedImage<S> createEmpty() {
        return emptySeg;
    }
}
