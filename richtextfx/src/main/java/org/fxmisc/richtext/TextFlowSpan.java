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

    Rectangle2D getBounds() {
        if ( bounds == null ) {
            bounds = new Rectangle2D( 0, y, width, height );
        }
        return bounds;
    }

    float getCenterY() {
        return (float) (y + height / 2);
    }

    int getStart()  { return start; }
    int getLength() { return length; }
    double getHeight() { return height; }
    double getWidth() { return width; }
    
    void setHeight( double h ) { height = h; bounds = null; }
    
    void addLengthAndWidth( int len, double w ) {
        width += w + 1; bounds = null;
        length += len;
    }
}
