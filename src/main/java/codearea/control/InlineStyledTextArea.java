package codearea.control;

/**
 * Text area that uses inline style string to define style of text segments.
 */
public class InlineStyledTextArea extends StyledTextArea<String> {

    public InlineStyledTextArea() {
        super("", (text, style) -> text.setStyle(style));
    }

    /**
     * Creates a text area with initial text content.
     * Initial caret position is set at the beginning of text content.
     *
     * @param text Initial text content.
     */
    public InlineStyledTextArea(String text) {
        this();

        replaceText(0, 0, text);

        // position the caret at the beginning
        selectRange(0, 0);
    }

}
