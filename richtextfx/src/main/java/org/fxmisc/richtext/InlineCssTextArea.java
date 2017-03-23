package org.fxmisc.richtext;


import javafx.beans.NamedArg;
import javafx.scene.text.TextFlow;

import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.SimpleEditableStyledDocument;
import org.fxmisc.richtext.model.StyledText;

/**
 * Text area that uses inline css to define style of text segments and paragraph segments.
 */
public class InlineCssTextArea extends StyledTextArea<String, String> {

    public InlineCssTextArea() {
        this(new SimpleEditableStyledDocument<>("", ""));
    }

    public InlineCssTextArea(@NamedArg("document") EditableStyledDocument<String, StyledText<String>, String> document) {
        super(
                "", TextFlow::setStyle,
                "", TextExt::setStyle,
                document,
                true
        );
    }

    /**
     * Creates a text area with initial text content.
     * Initial caret position is set at the beginning of text content.
     *
     * @param text Initial text content.
     */
    public InlineCssTextArea(@NamedArg("text") String text) {
        this();

        replaceText(0, 0, text);
        getUndoManager().forgetHistory();
        getUndoManager().mark();

        setStyleCodecs(Codec.STRING_CODEC, StyledText.codec(Codec.STRING_CODEC));

        // position the caret at the beginning
        selectRange(0, 0);
    }
}
