package org.fxmisc.richtext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

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


    /**
     * Folds (hides/collapses) paragraphs from <code>startPar</code> to <code>
     * endPar</code>, "into" (i.e. excluding) the first paragraph of the range.
     */
    public void foldParagraphs( int startPar, int endPar ) {
        foldParagraphs( startPar, endPar, getAddFoldStyle() );
    }

    /**
     * Folds (hides/collapses) the currently selected paragraphs,
     * "into" (i.e. excluding) the first paragraph of the range.
     */
    public void foldSelectedParagraphs() {
        foldSelectedParagraphs( getAddFoldStyle() );
    }

    /**
     * Folds (hides/collapses) paragraphs from character position <code>start</code>
     * to <code>end</code>, "into" (i.e. excluding) the first paragraph of the range.
     */
    public void foldText( int start, int end ) {
        fold( start, end, getAddFoldStyle() );
    }

    public boolean isFolded( int paragraph ) {
        return getFoldStyleCheck().test( getParagraph( paragraph ).getParagraphStyle() );
    }

    /**
     * Unfolds paragraphs <code>startingFrom</code> onwards for the currently folded block.
     */
    public void unfoldParagraphs( int startingFromPar ) {
        unfoldParagraphs( startingFromPar, getFoldStyleCheck(), getRemoveFoldStyle() );
    }

    /**
     * Unfolds text <code>startingFromPos</code> onwards for the currently folded block.
     */
    public void unfoldText( int startingFromPos ) {
    	startingFromPos = offsetToPosition( startingFromPos, Bias.Backward ).getMajor();
        unfoldParagraphs( startingFromPos, getFoldStyleCheck(), getRemoveFoldStyle() );
    }


    /**
     * @return a Predicate that given a paragraph style, returns true if it includes folding.
     */
    protected Predicate<Collection<String>> getFoldStyleCheck() {
        return styleList -> styleList != null && styleList.contains( "collapse" );
    }

    /**
     * @return a UnaryOperator that given a paragraph style, returns a style that includes fold styling.
     */
    protected UnaryOperator<Collection<String>> getAddFoldStyle() {
        return styleList -> {
            styleList = new ArrayList<>( styleList );
            // "collapse" is in styled-text-area.css:
            // .collapse { visibility: false; }
            styleList.add( "collapse" );
            return styleList;
        };
    }

    /**
     * @return a UnaryOperator that given a paragraph style, returns a style that excludes fold styling.
     */
    protected UnaryOperator<Collection<String>> getRemoveFoldStyle() {
        return styleList -> {
            styleList = new ArrayList<>( styleList );
            styleList.remove( "collapse" );
            return styleList;
        };
    }
}
