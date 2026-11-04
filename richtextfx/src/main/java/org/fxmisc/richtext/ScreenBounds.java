package org.fxmisc.richtext;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

class ScreenBounds {
    private final Bounds screen;

    public ScreenBounds(Bounds screen) {
        this.screen = screen;
    }

    /**
     * Evaluate the paragraph node bounds in the area.
     * @param paragraph the paragraph bound position on screen
     * @return the bounds inside the area of the provided paragraph bounds.
     */
    public Bounds getParagraphBoundsFrom(Bounds paragraph) {
        // use area's minX,minY if scrolled right/down and paragraph's left/top is not visible
        double minX = Math.max(paragraph.getMinX(), screen.getMinX());
        double minY = Math.max(paragraph.getMinY(), screen.getMinY());

        // use area's width whether paragraph spans outside of it or not
        // so that short or long paragraph takes up the entire space
        double width = screen.getWidth();

        // use area's maxY if scrolled up vertically and paragraph's bottom is not visible
        double maxY = Math.min(paragraph.getMaxY(), screen.getMaxY());

        return new BoundingBox(minX, minY, width, maxY - minY);
    }
}
