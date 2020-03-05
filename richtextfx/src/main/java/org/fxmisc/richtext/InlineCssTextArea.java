package org.fxmisc.richtext;


import javafx.beans.NamedArg;
import javafx.scene.text.TextFlow;

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

}
