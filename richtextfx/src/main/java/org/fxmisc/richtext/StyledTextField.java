package org.fxmisc.richtext;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import org.fxmisc.richtext.model.EditableStyledDocument;

import javafx.application.Application;
import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.AccessibleRole;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.text.TextFlow;

public class StyledTextField<PS, S> extends StyledTextArea
{
    private final Pattern VERTICAL_WHITESPACE = Pattern.compile( "\\v+" );
    private final static String STYLE_SHEET;
    private final static double HEIGHT;
    static {
        String globalCSS = System.getProperty( "javafx.userAgentStylesheetUrl" ); // JavaFX preference!
        if ( globalCSS == null ) globalCSS = Application.getUserAgentStylesheet();
        if ( globalCSS == null ) globalCSS = Application.STYLESHEET_MODENA;
        globalCSS = "styled-text-field-"+ globalCSS.toLowerCase() +".css";
        STYLE_SHEET = StyledTextField.class.getResource( globalCSS ).toExternalForm();

        // Ugly hack to get a TextFields default height :(
        // as it differs between Caspian, Modena, etc.
        TextField tf = new TextField( "GetHeight" );
        new Scene(tf); tf.applyCss(); tf.layout();
        HEIGHT = tf.getHeight();
    }

    private boolean selectAll = true;


    public StyledTextField(@NamedArg("initialParagraphStyle") PS initialParagraphStyle,
            @NamedArg("applyParagraphStyle")   BiConsumer<TextFlow, PS> applyParagraphStyle,
            @NamedArg("initialTextStyle")      S initialTextStyle,
            @NamedArg("applyStyle")            BiConsumer<? super TextExt, S> applyStyle,
            @NamedArg("document")              EditableStyledDocument<PS, String, S> document)
    {
        super( initialParagraphStyle, applyParagraphStyle, initialTextStyle, applyStyle, document, true );

        getStylesheets().add( STYLE_SHEET );
        getStyleClass().setAll( "styled-text-field" );

        setAccessibleRole( AccessibleRole.TEXT_FIELD );
        setPrefSize( 135, HEIGHT );

        addEventFilter( KeyEvent.KEY_PRESSED, KE -> {
            if ( KE.getCode() == KeyCode.ENTER ) {
                fireEvent( new ActionEvent( this, null ) );
                KE.consume();
            }
            else if ( KE.getCode() == KeyCode.TAB ) {
            	traverse( this.getParent(), this, KE.isShiftDown() ? -1 : +1 );
                KE.consume();
            }
        });

        addEventFilter( MouseEvent.MOUSE_PRESSED, ME -> selectAll = isFocused() );

        focusedProperty().addListener( (ob,was,focused) -> {
            if ( ! was && focused && selectAll ) {
                selectRange( getLength(), 0 );
            }
            else if ( ! focused && was ) {
                moveTo( 0 ); requestFollowCaret();
            }
            selectAll = true;
        });
    }

    /*
     * There's no public API to move the focus forward or backward
     * without explicitly knowing the node. So here's a basic local
     * implementation to accomplish that.
     */
    private Node traverse( Parent p, Node from, int dir )
    {
        if ( p == null ) return null;

        List<Node> nodeList = p.getChildrenUnmodifiable();
        int len = nodeList.size();
        int neighbor = -1;
        
        if ( from != null ) while ( ++neighbor < len && nodeList.get(neighbor) != from );
        else if ( dir == 1 ) neighbor = -1;
        else neighbor = len;
        
        for ( neighbor += dir; neighbor > -1 && neighbor < len; neighbor += dir ) {

            Node target = nodeList.get( neighbor );

            if ( target instanceof Pane || target instanceof Group ) {
                target = traverse( (Parent) target, null, dir ); // down
                if ( target != null ) return target;
            }
            else if ( target.isVisible() && ! target.isDisabled() && target.isFocusTraversable() ) {
                target.requestFocus();
                return target;
            }
        }

        return traverse( p.getParent(), p, dir ); // up
    }


    public void setText( String text )
    {
        replaceText( text );
    }

    /**
     * The action handler associated with this text field, or
     * {@code null} if no action handler is assigned.
     *
     * The action handler is normally called when the user types the ENTER key.
     */
    private ObjectProperty<EventHandler<ActionEvent>> onAction = new ObjectPropertyBase<EventHandler<ActionEvent>>() {
        @Override
        protected void invalidated() {
            setEventHandler(ActionEvent.ACTION, get());
        }

        @Override
        public Object getBean() {
            return StyledTextField.this;
        }

        @Override
        public String getName() {
            return "onAction";
        }
    };
    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() { return onAction; }
    public final EventHandler<ActionEvent> getOnAction() { return onActionProperty().get(); }
    public final void setOnAction(EventHandler<ActionEvent> value) { onActionProperty().set(value); }

    @Override
    public void replaceText( int start, int end, String text )
    {
        super.replaceText( start, end, VERTICAL_WHITESPACE.matcher( text ).replaceAll( " " ) );
    }
}
