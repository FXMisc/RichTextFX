package org.fxmisc.richtext;

import java.util.Collection;
import java.util.Collections;

/**
 * Provides a base model class for subclasses of {@link StyleClassedTextArea}.
 */
public class StyleClassedTextAreaModel extends StyledTextAreaModel<Collection<String>, Collection<String>> {

    public StyleClassedTextAreaModel(Collection<String> initialParagraphStyle, Collection<String> initialTextStyle,
                                     EditableStyledDocument<Collection<String>, Collection<String>> document,
                                     boolean preserveStyle) {
        super(initialParagraphStyle, initialTextStyle, document, preserveStyle);
    }

    public StyleClassedTextAreaModel(boolean preserveStyle) {
        this(Collections.emptyList(), Collections.emptyList(),
                new EditableStyledDocument<>(Collections.<String>emptyList(), Collections.<String>emptyList()), preserveStyle);
    }

    /**
     * Creates a text area with empty text content.
     */
    public StyleClassedTextAreaModel() {
        this(true);
    }

    /**
     * Constructs a clone
     */
    public StyleClassedTextAreaModel(StyleClassedTextAreaModel model) {
        super(model);
    }


}
