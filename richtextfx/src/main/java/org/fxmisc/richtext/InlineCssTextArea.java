package org.fxmisc.richtext;

/**
 * Text area that uses inline css to define style of text segments and paragraph segments.
 */
public class InlineCssTextArea extends InlineStyleTextArea<String, String> {

    public InlineCssTextArea() {
        super("", css -> css, "", css -> css);
    }

    /**
     * Creates a text area with initial text content.
     * Initial caret position is set at the beginning of text content.
     *
     * @param text Initial text content.
     */
    public InlineCssTextArea(String text) {
        this();

        replaceText(0, 0, text);
        getUndoManager().forgetHistory();
        getUndoManager().mark();

        setStyleCodec(Codec.STRING_CODEC);

        // position the caret at the beginning
        selectRange(0, 0);
    }

}
