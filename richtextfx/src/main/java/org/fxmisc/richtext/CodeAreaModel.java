package org.fxmisc.richtext;

import java.util.Collection;

/**
 * Provides a base model class for subclasses of {@link CodeArea}
 */
public class CodeAreaModel extends StyleClassedTextAreaModel {

    public CodeAreaModel(Collection<String> initialParagraphStyle, Collection<String> initialTextStyle,
                                     EditableStyledDocument<Collection<String>, Collection<String>> document,
                                     boolean preserveStyle) {
        super(initialParagraphStyle, initialTextStyle, document, preserveStyle);
    }

    public CodeAreaModel(boolean preserveStyle) {
        super(preserveStyle);
    }

    /**
     * Constructs a clone
     */
    public CodeAreaModel(CodeAreaModel model) {
        super(model);
    }

    /**
     * Creates a text area with empty text content.
     */
    public CodeAreaModel() {
        super(false);
    }

    /**
     * Creates a text area with initial text content.
     * Initial caret position is set at the beginning of text content.
     *
     * @param text Initial text content.
     */
    public CodeAreaModel(String text) {
        super();

        appendText(text);
        getUndoManager().forgetHistory();
        getUndoManager().mark();

        // position the caret at the beginning
        selectRange(0, 0);
    }
}
