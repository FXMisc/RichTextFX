package org.fxmisc.richtext.demo.richtext;

import java.util.function.IntFunction;

import org.fxmisc.richtext.GenericStyledArea;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class BulletFactory implements IntFunction<Node>
{
    private GenericStyledArea<ParStyle,?,?> area;
    
    public BulletFactory( GenericStyledArea<ParStyle,?,?> area )
    {
        this.area = area;
    } 

    @Override
    public Node apply( int value )
    {
        if ( value < 0 ) return null;
        
        ParStyle ps = area.getParagraph( value ).getParagraphStyle();
        if ( ! ps.indent.isPresent() ) return null;

        return createBullet( ps.indent.get() );
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
        
        Label l = new Label( " ", result );
        l.setPadding( new Insets( 0, 0, 0, in.level*in.width ) );
        l.setContentDisplay( ContentDisplay.LEFT );
        return new VBox( 0, l );
    }
}
