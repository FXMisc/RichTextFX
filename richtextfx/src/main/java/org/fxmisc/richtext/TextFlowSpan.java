package org.fxmisc.richtext;

import javafx.geometry.Rectangle2D;

/**
 * @author Jurgen (admedfx@gmail.com)
 */
class TextFlowSpan
{
    private Rectangle2D bounds;
    private double  y, width, height;
    private int start, length;

    TextFlowSpan( int start, int length, double minY, double width, double height ) {
        this.start = start;
        this.length = length;
        this.height = height;
        this.width = width;
        y = minY;
    }

    Rectangle2D bounds() {
        if ( bounds == null ) {
            bounds = new Rectangle2D( 0, y, width, height );
        }
        return bounds;
    }

    @Deprecated float centerY() {
        return (float) (y + height / 2);
    }

    int start()  { return start; }
    @Deprecated int length() { return length; }
    int end()    { return start + length; }
    @Deprecated double height() { return height; }
    @Deprecated double width() { return width; }

    @Deprecated
    void setHeight( double h ) { height = h; bounds = null; }

    @Deprecated
    void addLengthAndWidth( int len, double w ) {
        width += w + 1; bounds = null;
        length += len;
    }
}
