package org.fxmisc.richtext;

import javafx.beans.property.ObjectProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.PathElement;

class LineSelection<PS, SEG, S> extends SelectionImpl<PS, SEG, S>
{
    LineSelection( GenericStyledArea<PS, SEG, S> area, ObjectProperty<Paint> lineHighlighterFill )
    {
        super( "line-highlighter", area, path ->
        {
            if ( lineHighlighterFill == null ) path.setHighlightFill( Color.YELLOW );
            else path.highlightFillProperty().bind( lineHighlighterFill );

            path.getElements().addListener( (Change<? extends PathElement> chg) ->
            {
                if ( chg.next() && chg.wasAdded() || chg.wasReplaced() ) {
                    double width = path.getParent().getLayoutBounds().getWidth();
                    // The path is limited to the bounds of the text, so here it's altered to the area's width
                    chg.getAddedSubList().stream().skip(1).limit(2).forEach( ele -> ((LineTo) ele).setX( width ) );
                    // The path can wrap onto another line if enough text is inserted, so here it's trimmed
                    if ( chg.getAddedSize() > 5 ) path.getElements().remove( 5, 10 );
                    // Highlight masks the downward selection of text on the last line, so move it behind
                    path.toBack();
                }
            } );
        } );
    }

    @Override
    public void selectRange( int start, int end )
    {
        selectCurrentLine();
    }

    public void selectCurrentLine()
    {
        int p = getArea().getCurrentParagraph();
        int start = getArea().getAbsolutePosition( p, getArea().getCurrentLineStartInParargraph() );
        int end = getArea().getAbsolutePosition( p, getArea().getCurrentLineEndInParargraph() );
        super.selectRange( start, (end > start) ? end : start+1  ); // +1 for empty lines
    }
}
