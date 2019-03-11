package org.fxmisc.richtext;

import java.util.ArrayList;
import java.util.List;

import org.fxmisc.richtext.model.TwoLevelNavigator;

import javafx.beans.Observable;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * @author Jurgen (admedfx@gmail.com)
 */
class TextFlowLayout
{
    private TextFlow flow;
    private List<Node>  children;
    private List<TextFlowSpan> lineMetrics = new ArrayList<>();
    private int lineCount = -1;

    TextFlowLayout( TextFlow tf, List<Node> managedChildren ) {
        tf.getChildren().addListener( (Observable ob) -> lineCount = -1 );
        tf.widthProperty().addListener( (Observable ob) -> lineCount = -1 );
        children = managedChildren;
        flow = tf;
    }


    float getLineCenter( int lineNo ) {
        return getLineCount() > 0 ? lineMetrics.get( lineNo ).getCenterY() : 1.0f;
    }


    int getLineLength( int lineNo ) {
        return getLineSpan( lineNo ).getLength();
    }


    TextFlowSpan getLineSpan( int lineNo ) {
        return getLineCount() > 0 ? lineMetrics.get( lineNo ) : null;
    }


    TextFlowSpan getLineSpan( float y ) {
        final int lastLine = getLineCount() - 1;
        return lineMetrics.stream().filter( tfs -> y < tfs.getBounds().getMaxY() )
                .findFirst().orElse( lineMetrics.get( lastLine ) );
    }


    TwoLevelNavigator getTwoLevelNavigator() {
        return new TwoLevelNavigator( this::getLineCount, this::getLineLength );
    }
    

    /*
     * Iterate through the nodes in the TextFlow to determine the number of lines of text.
     * Also calculates the following metrics for each line along the way: line height,
     * line width, centerY, length (character count), start (character offset from 1st line)
     */
    int getLineCount() {
       
        if ( lineCount > -1 ) return lineCount;
        
        lineMetrics.clear();
        double totLines = 0.0, prevMinY = 1.0, prevMaxY = -1.0;
        int totCharSoFar = 0;

        for ( Node n : children ) {
           
            Bounds nodeBounds = n.getBoundsInParent();
            int length = (n instanceof Text) ? ((Text) n).getText().length() : 1;
            PathElement[] shape = flow.rangeShape( totCharSoFar, totCharSoFar+length );
            double lines = Math.max( 1.0, Math.floor( shape.length / 5 ) );
            double nodeMinY = Math.max( 0.0, nodeBounds.getMinY() );
            
            if ( nodeMinY >= prevMinY && lines > 1 )  totLines += lines - 1;  // Multiline Text node 
            else if ( nodeMinY >= prevMaxY )  totLines += lines;

            if ( lineMetrics.size() < totLines ) {                            // Add additional lines
               
                if ( shape.length == 0 ) {
                   lineMetrics.add( new TextFlowSpan( totCharSoFar, length, nodeMinY, nodeBounds.getWidth(), nodeBounds.getHeight() ) );
                    totCharSoFar += length;
                }
                else for ( int ele = 1; ele < shape.length; ele += 5 ) {
                    // Calculate the segment's line's length and width up to this point
                    LineTo eleLine = (LineTo) shape[ele];
                    double segWidth = eleLine.getX(), lineMinY = eleLine.getY();
                    double charHeight = ((LineTo) shape[ele+1]).getY() - lineMinY;
                    Point2D endPoint = new Point2D( segWidth-1, lineMinY + charHeight / 2 );

                    // hitTest queries TextFlow layout internally and returns the position of the
                    // last char (nearest endPoint) on the line, irrespective of the current Text node !
                    int segLen = flow.hitTest( endPoint ).getCharIndex();
                    segLen -= totCharSoFar - 1;

                    if ( ele == 1 && nodeMinY < prevMaxY ) {
                        adjustLineMetrics( segLen, segWidth - ((MoveTo) shape[ele-1]).getX(), charHeight );
                    }
                    else {
                       lineMetrics.add( new TextFlowSpan( totCharSoFar, segLen, lineMinY, segWidth, charHeight ) );
                    }

                    totCharSoFar += segLen;
                }
            }
            else {
                // Adjust current line metrics with additional Text or Node embedded in this line 
                adjustLineMetrics( length, nodeBounds.getWidth(), nodeBounds.getHeight() );
                totCharSoFar += length;
            }

            prevMaxY = nodeBounds.getMaxY();
            prevMinY = nodeMinY;
        }
        
        lineCount = (int) totLines;
        return lineCount;
    }


    private void adjustLineMetrics( int length, double width, double height ) {
        TextFlowSpan span = lineMetrics.get( lineMetrics.size()-1 );
        span.addLengthAndWidth( length, width );
        if ( height > span.getHeight() ) {
            span.setHeight( height );
        }
    }
}
