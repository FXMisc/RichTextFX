package org.fxmisc.richtext;

import java.util.Collection;
import java.util.Collections;

import javafx.beans.NamedArg;
import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.SimpleEditableStyledDocument;

/**
 * Text area that uses style classes to define style of text segments and paragraph segments.
 */
public class StyleClassedTextArea extends StyledTextArea<Collection<String>, Collection<String>> {

    public StyleClassedTextArea(@NamedArg("document") EditableStyledDocument<Collection<String>, String, Collection<String>> document,
                                @NamedArg("preserveStyle") boolean preserveStyle) {
        super(Collections.<String>emptyList(),
                (paragraph, styleClasses) -> paragraph.getStyleClass().addAll(styleClasses),
                Collections.<String>emptyList(),
                (text, styleClasses) -> text.getStyleClass().addAll(styleClasses),
                document, preserveStyle
        );

        setStyleCodecs(
                Codec.collectionCodec(Codec.STRING_CODEC),
                Codec.styledTextCodec(Codec.collectionCodec(Codec.STRING_CODEC))
        );
    }
    public StyleClassedTextArea(@NamedArg("preserveStyle") boolean preserveStyle) {
        this(
                new SimpleEditableStyledDocument<>(
                    Collections.<String>emptyList(), Collections.<String>emptyList()
                ), preserveStyle);
    }

    /**
     * Creates a text area with empty text content.
     */
    public StyleClassedTextArea() {
        this(true);
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
}
