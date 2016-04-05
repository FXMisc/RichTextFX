package org.fxmisc.richtext;

/**
 * Created by Geoff on 4/4/2016.
 */
public class HighlightedTextInteveral {

    private final int lowerBound;
    private final int upperBound;
    private final String styleClass;

    public HighlightedTextInteveral(int lowerBound, int upperBound, String styleClass) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.styleClass = styleClass;
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }

    public String getStyleClass() {
        return styleClass;
    }
}
