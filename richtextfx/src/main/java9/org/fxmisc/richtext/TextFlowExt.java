package org.fxmisc.richtext;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.*;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.model.TwoLevelNavigator;

import javafx.scene.shape.PathElement;
import javafx.scene.text.HitInfo;
import javafx.scene.text.TextFlow;
import javafx.scene.shape.*;

/**
 * Adds additional API to {@link TextFlow}.
 */
class TextFlowExt extends TextFlow {
    
    private TextFlowLayout layout;
    
    private TextFlowLayout textLayout()
    {
        if ( layout == null ) {
            layout = new TextFlowLayout( this );
        }
        return layout;
    }

    int getLineCount() {
        return textLayout().getLineCount();
    }

    int getLineStartPosition(int charIdx) {
        TwoLevelNavigator navigator = textLayout().getTwoLevelNavigator();
        int currentLineIndex = navigator.offsetToPosition(charIdx, Forward).getMajor();
        return navigator.position(currentLineIndex, 0).toOffset();
    }

    int getLineEndPosition(int charIdx) {
        TwoLevelNavigator navigator = textLayout().getTwoLevelNavigator();
        int currentLineIndex = navigator.offsetToPosition(charIdx, Forward).getMajor() + 1;
        int minor = (currentLineIndex == getLineCount()) ? 0 : -1;
        return navigator.position(currentLineIndex, minor).toOffset();
    }

    int getLineOfCharacter(int charIdx) {
        TwoLevelNavigator navigator = textLayout().getTwoLevelNavigator();
        return navigator.offsetToPosition(charIdx, Forward).getMajor();
    }

    PathElement[] getCaretShape(int charIdx, boolean isLeading) {
        return caretShape(charIdx, isLeading);
    }

    PathElement[] getRangeShape(IndexRange range) {
        return getRangeShape(range.getStart(), range.getEnd());
    }

    PathElement[] getRangeShape(int from, int to) {
        return rangeShape(from, to);
    }

    PathElement[] getUnderlineShape(IndexRange range) {
        return getUnderlineShape(range.getStart(), range.getEnd());
    }

    PathElement[] getUnderlineShape(int from, int to) {
        return getUnderlineShape(from, to, 0, 0);
    }

    /**
     * @param from The index of the first character.
     * @param to The index of the last character.
     * @param offset The distance below the baseline to draw the underline.
     * @param wave If non-zero, draw a wavy underline with arcs of this radius.
     * @return An array with the PathElement objects which define an
     *         underline from the first to the last character.
     */
    PathElement[] getUnderlineShape(int from, int to, double offset, double wave) {
        // get a Path for the text underline
        List<PathElement> result = new ArrayList<>();
        
        PathElement[] shape = rangeShape( from, to );
        // The shape is a closed Path for one or more rectangles AROUND the selected text. 
        // shape: [MoveTo origin, LineTo top R, LineTo bottom R, LineTo bottom L, LineTo origin, *]

        // Extract the bottom left and right coordinates for each rectangle to get the underline path.
        for ( int ele = 2; ele < shape.length; ele += 5 )
        {
            LineTo bl = (LineTo) shape[ele+1];
            LineTo br = (LineTo) shape[ele];
            double y = br.getY() + offset - 2.5;

            if (wave <= 0) {
                result.add(new MoveTo(bl.getX(), y));
                result.add(new LineTo(br.getX(), y));
            }
            else {
                // Round to pixel locations to reduce ugliness on low-dpi
                // screens where different wave underlines will look very
                // different.
                double x = Math.round(bl.getX());
                y = Math.round(y);
                double rx = br.getX();
                result.add(new MoveTo(x, y));
                boolean sweep = true;
                while (x < rx) {
                    x += wave * 2;
                    if (x > rx) {
                        // Compute the value of y at which the arc intersects
                        // the line x = rx.
                        double dy = wave * Math.sin(Math.acos((wave - x + rx) / wave));
                        if (sweep) y -= dy; else y += dy;
                        x = rx;
                    }
                    result.add(new ArcTo(wave, wave, 0.0, x, y, false, sweep));
                    sweep = !sweep;
                }
            }
        }

        return result.toArray(new PathElement[0]);
    }

    CharacterHit hitLine(double x, int lineIndex) {
        return hit(x, textLayout().getLineCenter( lineIndex ));
    }

    CharacterHit hit(double x, double y) {
        TextFlowSpan span = textLayout().getLineSpan( (float) y );
        Rectangle2D lineBounds = span.getBounds();
        
        HitInfo hit = hitTest(new Point2D(x, y));
        int charIdx = hit.getCharIndex();
        boolean leading = hit.isLeading();

        if (y >= span.getBounds().getMaxY()) {
            return CharacterHit.insertionAt(charIdx);
        }

        if ( ! leading && getLineCount() > 1) {
            // If this is a wrapped paragraph and hit character is at end of hit line, make sure that the
            // "character hit" stays at the end of the hit line (and not at the beginning of the next line).
            leading = (getLineOfCharacter(charIdx) + 1 < getLineCount() && charIdx + 1 >= span.getStart() + span.getLength());
        }

        if(x < lineBounds.getMinX() || x > lineBounds.getMaxX()) {
            if(leading) {
                return CharacterHit.insertionAt(charIdx);
            } else {
                return CharacterHit.insertionAt(charIdx + 1);
            }
        } else {
            if(leading) {
                return CharacterHit.leadingHalfOf(charIdx);
            } else {
                return CharacterHit.trailingHalfOf(charIdx);
            }
        }
    }

}
