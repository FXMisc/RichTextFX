package org.fxmisc.richtext;


import javafx.beans.NamedArg;
import javafx.scene.text.TextFlow;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.SimpleEditableStyledDocument;

import static org.fxmisc.richtext.model.Codec.styledTextCodec;

/**
 * Text area that uses inline css to define style of text segments and paragraphs.
 */
public class InlineCssTextArea extends StyledTextArea<String, String> {

    /**
     * Creates a blank area
     */
    public InlineCssTextArea() {
        this(new SimpleEditableStyledDocument<>("", ""));
    }

    /**
     * Creates an area that can render and edit another area's {@link EditableStyledDocument} or a developer's
     * custom implementation of {@link EditableStyledDocument}.
     */
    public InlineCssTextArea(@NamedArg("document") EditableStyledDocument<String, String, String> document) {
        super(
                "", TextFlow::setStyle,
                "", TextExt::setStyle,
                document,
                true
        );

        setStyleCodecs(Codec.STRING_CODEC, styledTextCodec(Codec.STRING_CODEC));
    }

    /**
     * Creates a text area with initial text content.
     * Initial caret position is set at the beginning of text content.
     *
     * @param text Initial text content.
     */
    public InlineCssTextArea(@NamedArg("text") String text) {
        this();

        replace(0, 0, ReadOnlyStyledDocument.fromString(text, getInitialParagraphStyle(), getInitialTextStyle(), getSegOps()));
        getUndoManager().forgetHistory();
        getUndoManager().mark();

        // position the caret at the beginning
        selectRange(0, 0);
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
     * Unfolds paragraphs <code>startingFromPar</code> onwards for the currently folded block.
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
    protected Predicate<String> getFoldStyleCheck() {
        return pstyle -> pstyle != null && pstyle.contains( "collapse" );
    }

    /**
     * @return a UnaryOperator that given a paragraph style, returns a String that includes fold styling.
     */
    protected UnaryOperator<String> getAddFoldStyle() {
        return pstyle -> "visibility: collapse;"+ (pstyle != null ? pstyle : "");
    }

    /**
     * @return a UnaryOperator that given a paragraph style, returns a String that excludes fold styling.
     */
    protected UnaryOperator<String> getRemoveFoldStyle() {
        return pstyle -> pstyle.replaceFirst( "visibility: collapse;", "" );
    }
}
