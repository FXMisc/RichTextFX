package org.fxmisc.richtext.demo.hyperlink;

import org.fxmisc.richtext.model.SegmentOpsBase;

import java.util.Optional;

public class HyperlinkOps<S> extends SegmentOpsBase<Hyperlink, S> {

    public HyperlinkOps() {
        super(new Hyperlink("", "", ""));
    }

    @Override
    public int length(Hyperlink hyperlink) {
        return hyperlink.length();
    }

    @Override
    public char realCharAt(Hyperlink hyperlink, int index) {
        return hyperlink.charAt(index);
    }

    @Override
    public String realGetText(Hyperlink hyperlink) {
        return hyperlink.getDisplayedText();
    }

    @Override
    public Hyperlink realSubSequence(Hyperlink hyperlink, int start, int end) {
        return hyperlink.subSequence(start, end);
    }

    @Override
    public Hyperlink realSubSequence(Hyperlink hyperlink, int start) {
        return hyperlink.subSequence(start);
    }

    @Override
    public Optional<Hyperlink> joinSeg(Hyperlink currentSeg, Hyperlink nextSeg) {
        if (currentSeg.isEmpty()) {
            if (nextSeg.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(nextSeg);
            }
        } else {
            if (nextSeg.isEmpty()) {
                return Optional.of(currentSeg);
            } else {
                return concatHyperlinks(currentSeg, nextSeg);
            }
        }
    }

    private Optional<Hyperlink> concatHyperlinks(Hyperlink leftSeg, Hyperlink rightSeg) {
        if (!leftSeg.shareSameAncestor(rightSeg)) {
            return Optional.empty();
        }

        String leftText = leftSeg.getDisplayedText();
        String rightText = rightSeg.getDisplayedText();

        return Optional.of(leftSeg.mapDisplayedText(leftText + rightText));
    }

}
