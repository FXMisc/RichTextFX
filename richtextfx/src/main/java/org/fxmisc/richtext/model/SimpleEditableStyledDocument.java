package org.fxmisc.richtext.model;

import javafx.beans.NamedArg;

/**
 * Provides an implementation of {@link EditableStyledDocument} that is specified for {@link StyledText} as its segment.
 * See also {@link GenericEditableStyledDocument}.
 */
public final class SimpleEditableStyledDocument<PS, S> extends GenericEditableStyledDocumentBase<PS, StyledText<S>, S> {

    public SimpleEditableStyledDocument(
            @NamedArg("initialParagraphStyle") PS initialParagraphStyle,
            @NamedArg("initialTextStyle") S initialTextStyle) {
        super(initialParagraphStyle, initialTextStyle, StyledText.textOps());
    }
}
