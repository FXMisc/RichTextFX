package org.fxmisc.richtext;

/**
 * Provides a base model class for subclasses of {@link InlineCssTextArea}
 */
public class InlineCssTextAreaModel extends StyledTextAreaModel<String, String> {

    public InlineCssTextAreaModel(String initialParagraphStyle, String initialTextStyle,
                             EditableStyledDocument<String, String> document) {
        super(initialParagraphStyle, initialTextStyle, document, true);
    }

    public InlineCssTextAreaModel(String initialParagraphStyle, String initialTextStyle) {
        this(initialParagraphStyle, initialTextStyle,
                new EditableStyledDocument<>(initialParagraphStyle, initialTextStyle));
    }

    /**
     * Creates a clone
     */
    public InlineCssTextAreaModel(InlineCssTextAreaModel model) {
        super(model);
    }

    public InlineCssTextAreaModel() {
        super("", "");
    }

    /**
     * Creates a model with initial text content.
     * Initial caret position is set at the beginning of text content.
     *
     * @param text Initial text content.
     */
    public InlineCssTextAreaModel(String text) {
        this();

        replaceText(0, 0, text);
        getUndoManager().forgetHistory();
        getUndoManager().mark();

        // position the caret at the beginning
        selectRange(0, 0);
    }
}
