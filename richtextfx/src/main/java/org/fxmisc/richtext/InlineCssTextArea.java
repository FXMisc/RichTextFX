package org.fxmisc.richtext;

/**
 * Text area that uses inline css to define style of text segments and paragraph segments.
 */
public class InlineCssTextArea extends StyledTextArea<String, String> {

    public InlineCssTextArea(StyledTextAreaModel<String, String> model) {
        super(model);
    }

    public InlineCssTextArea() {
        this(new StyledTextAreaModel<String, String>(
                "", (text, style) -> text.setStyle(style),
                "", (paragraph, style) -> paragraph.setStyle(style),
                new EditableStyledDocument<String, String>("", ""),
                true
        ));
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

        setStyleCodecs(Codec.STRING_CODEC, Codec.STRING_CODEC);

        // position the caret at the beginning
        selectRange(0, 0);
    }
}
