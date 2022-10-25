package org.fxmisc.richtext;

import java.util.Collection;
import java.util.Collections;

import org.fxmisc.richtext.model.SimpleEditableStyledDocument;

import javafx.scene.text.TextAlignment;

/**
 * A TextField that uses style classes, i.e. <code>getStyleClass().add(String)</code>, to define the styles of text segments.
 * <p>Use CSS Style Class ".styled-text-field" for styling the control.
 * @author Jurgen
 */
public class StyleClassedTextField extends StyledTextField<Collection<String>, Collection<String>>
{
    public StyleClassedTextField() {
        super(
            Collections.<String>emptyList(),
            (paragraph, styleClasses) -> paragraph.getStyleClass().addAll(styleClasses),
            Collections.<String>emptyList(),
            (text, styleClasses) -> text.getStyleClass().addAll(styleClasses),
            new SimpleEditableStyledDocument<>( Collections.<String>emptyList(), Collections.<String>emptyList() )
        );
    }

    public StyleClassedTextField( String text ) {
        this(); replaceText( text );
        getUndoManager().forgetHistory();
        getUndoManager().mark();
    }

    /**
     * Convenient method to append text together with a single style class.
     */
    public void append( String text, String styleClass ) {
        insert( getLength(), text, styleClass );
    }

    /**
     * Convenient method to insert text together with a single style class.
     */
    public void insert( int position, String text, String styleClass ) {
        replace( position, position, text, Collections.singleton( styleClass ) );
    }

    /**
     * Convenient method to replace text together with a single style class.
     */
    public void replace( int start, int end, String text, String styleClass ) {
        replace( start, end, text, Collections.singleton( styleClass ) );
    }

    /**
     * Convenient method to assign a single style class.
     */
    public void setStyleClass( int from, int to, String styleClass ) {
        setStyle( from, to, Collections.singletonList( styleClass ) );
    }

    @Override
    protected void changeAlignment( TextAlignment txtAlign ) {
        // Set to style class as defined in "styled-text-field-caspian.css" AND "styled-text-field-modena.css"
        setParagraphStyle( 0, Collections.singletonList( txtAlign.toString().toLowerCase() ) );
    }
}
