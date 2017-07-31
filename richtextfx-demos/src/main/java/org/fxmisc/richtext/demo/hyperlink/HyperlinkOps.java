package org.fxmisc.richtext.demo.hyperlink;

import org.fxmisc.richtext.model.SegmentOps;

import java.util.Optional;

public class HyperlinkOps<S> implements SegmentOps<Hyperlink<S>, S> {

    @Override
    public int length(Hyperlink<S> hyperlink) {
        return hyperlink.length();
    }

    @Override
    public char charAt(Hyperlink<S> hyperlink, int index) {
        return hyperlink.charAt(index);
    }

    @Override
    public String getText(Hyperlink<S> hyperlink) {
        return hyperlink.getDisplayedText();
    }

    @Override
    public Hyperlink<S> subSequence(Hyperlink<S> hyperlink, int start, int end) {
        return hyperlink.subSequence(start, end);
    }

    @Override
    public Hyperlink<S> subSequence(Hyperlink<S> hyperlink, int start) {
        return hyperlink.subSequence(start);
    }

    @Override
    public S getStyle(Hyperlink<S> hyperlink) {
        return hyperlink.getStyle();
    }

    @Override
    public Hyperlink<S> setStyle(Hyperlink<S> hyperlink, S style) {
        return hyperlink.setStyle(style);
    }

    @Override
    public Optional<Hyperlink<S>> join(Hyperlink<S> currentSeg, Hyperlink<S> nextSeg) {
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

    private Optional<Hyperlink<S>> concatHyperlinks(Hyperlink<S> leftSeg, Hyperlink<S> rightSeg) {
        if (!leftSeg.shareSameAncestor(rightSeg)) {
            return Optional.empty();
        }

        String original = leftSeg.getOriginalDisplayedText();
        String leftText = leftSeg.getDisplayedText();
        String rightText = rightSeg.getDisplayedText();
        int leftOffset = 0;
        int rightOffset = 0;
        for (int i = 0; i <= original.length() - leftText.length(); i++) {
            if (original.regionMatches(i, leftText, 0, leftText.length())) {
                leftOffset = i;
                break;
            }
        }
        for (int i = 0; i <= original.length() - rightText.length(); i++) {
            if (original.regionMatches(i, rightText, 0, rightText.length())) {
                rightOffset = i;
                break;
            }
        }

        if (rightOffset + rightText.length() == leftOffset) {
            return Optional.of(leftSeg.mapDisplayedText(rightText + leftText));
        } else if (leftOffset + leftText.length() == rightOffset) {
            return Optional.of(leftSeg.mapDisplayedText(leftText + rightText));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Hyperlink<S> createEmpty() {
        return new Hyperlink<>("", "", null, "");
    }
}
