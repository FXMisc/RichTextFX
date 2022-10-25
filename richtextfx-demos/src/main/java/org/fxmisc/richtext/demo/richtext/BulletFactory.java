package org.fxmisc.richtext.demo.richtext;

import java.util.function.IntFunction;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;

import org.fxmisc.richtext.demo.richtext.RichTextDemo.FoldableStyledArea;

public class BulletFactory implements IntFunction<Node>
{
    private FoldableStyledArea area;

    private static final Font DEFAULT_FONT = Font.font("monospace", FontPosture.ITALIC, 13);

    public BulletFactory( FoldableStyledArea area )
    {
        area.getParagraphs().sizeProperty().addListener( (ob,ov,nv) -> {
            if ( nv <= ov ) Platform.runLater( () -> deleteParagraphCheck() );
            else  Platform.runLater( () -> insertParagraphCheck() );
        });
        this.area = area;
    }

    @Override
    public Node apply( int value )
    {
        ParStyle ps = area.getParagraph( value ).getParagraphStyle();
        return createGraphic( ps, value );
    }

    private Node createGraphic( ParStyle ps, int idx )
    {
        Label foldIndicator = new Label( "  " );
        VBox.setVgrow( foldIndicator, Priority.ALWAYS );
        foldIndicator.setMaxHeight( Double.MAX_VALUE );
        foldIndicator.setAlignment( Pos.TOP_LEFT );
        foldIndicator.setFont( DEFAULT_FONT );

        if ( area.getParagraphs().size() > idx+1 ) {
            if ( area.getParagraph( idx+1 ).getParagraphStyle().isFolded() && ! ps.isFolded() ) {
                foldIndicator.setOnMouseClicked( ME -> area.unfoldParagraphs( idx ) );
                foldIndicator.getStyleClass().add( "fold-indicator" );
                foldIndicator.setCursor( Cursor.HAND );
                foldIndicator.setText( "+ " );
            }
        }

        if ( ps.isIndented() && ! ps.isFolded() ) {
            foldIndicator.setGraphic( createBullet( ps.getIndent() ) );
            foldIndicator.setContentDisplay( ContentDisplay.RIGHT );
        }

        return new VBox( 0, foldIndicator );
    }

    private Node createBullet(Indent in) {
        Node result;
        switch( in.level ) {
            case 1 : {
                    Circle c = new Circle(2.5);
                    c.setFill(Color.BLACK);
                    c.setStroke(Color.BLACK);
                    result = c;
                }
                break;

            case 2 : {
                    Circle c = new Circle(2.5);
                    c.setFill(Color.WHITE);
                    c.setStroke(Color.BLACK);
                    result = c;
                }
                break;

            case 3 : {
                    Rectangle r = new Rectangle(5, 5);
                    r.setFill(Color.BLACK);
                    r.setStroke(Color.BLACK);
                    result = r;
                }
                break;

            default : {
                    Rectangle r = new Rectangle(5, 5);
                    r.setFill(Color.WHITE);
                    r.setStroke(Color.BLACK);
                    result = r;
                }
                break;
        }

        Label bullet = new Label( " ", result );
        bullet.setPadding( new Insets( 0, 0, 0, in.level*in.width ) );
        bullet.setContentDisplay( ContentDisplay.LEFT );
        return bullet;
    }

    private void deleteParagraphCheck()
    {
        int p = area.getCurrentParagraph();
        // Was the deleted paragraph in the viewport ?
        if ( p >= area.firstVisibleParToAllParIndex() && p <= area.lastVisibleParToAllParIndex() )
        {
            int col = area.getCaretColumn();
            // Delete was pressed on an empty paragraph, and so the cursor is now at the start of the next paragraph.
            if ( col == 0 ) {
                // Check if the now current paragraph is folded.
                if ( area.getParagraph( p ).getParagraphStyle().isFolded() ) {
                    p = Math.max( p-1, 0 );              // Adjust to previous paragraph.
                    area.recreateParagraphGraphic( p );  // Show fold/unfold indicator on previous paragraph.
                    area.moveTo( p, 0 );                 // Move cursor to previous paragraph.
                }
            }
            // Backspace was pressed on an empty paragraph, and so the cursor is now at the end of the previous paragraph.
            else if ( col == area.getParagraph( p ).length() ) {
                area.recreateParagraphGraphic( p ); // Shows fold/unfold indicator on current paragraph if needed.
            }
            // In all other cases the paragraph graphic is created/updated automatically.
        }
    }

    private void insertParagraphCheck()
    {
        int p = area.getCurrentParagraph();
        // Is the inserted paragraph in the viewport ?
        if ( p > area.firstVisibleParToAllParIndex() && p <= area.lastVisibleParToAllParIndex() ) {
            // Check limits, as p-1 and p+1 are accessed ...
            if ( p > 0 && p+1 < area.getParagraphs().size() ) {
                // Now check if the inserted paragraph is before a folded block ?
                if ( area.getParagraph( p+1 ).getParagraphStyle().isFolded() ) {
                    area.recreateParagraphGraphic( p-1 ); // Remove the unfold indicator.
                }
            }
        }
    }
}
