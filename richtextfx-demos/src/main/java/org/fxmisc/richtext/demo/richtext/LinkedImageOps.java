package org.fxmisc.richtext.demo.richtext;

import java.util.Optional;

import org.fxmisc.richtext.model.SegmentOps;

public class LinkedImageOps<S> implements SegmentOps<LinkedImage<S>, S> {

    @Override
    public int length(LinkedImage<S> seg) {
        return 1;
    }

    @Override
    public char charAt(LinkedImage<S> seg, int index) {
        return '\ufffc';
    }

    @Override
    public String getText(LinkedImage<S> seg) {
        return "\ufffc";
    }

    @Override
    public LinkedImage<S> subSequence(LinkedImage<S> seg, int start, int end) {
        return seg;
    }

    @Override
    public LinkedImage<S> subSequence(LinkedImage<S> seg, int start) {
        return seg;
    }

    @Override
    public S getStyle(LinkedImage<S> seg) {
        return seg.getStyle();
    }

    @Override
    public LinkedImage<S> setStyle(LinkedImage<S> seg, S style) {
        return seg.setStyle(style);
    }

    @Override
    public Optional<LinkedImage<S>> join(LinkedImage<S> currentSeg, LinkedImage<S> nextSeg) {
        return Optional.empty();
    }

}
