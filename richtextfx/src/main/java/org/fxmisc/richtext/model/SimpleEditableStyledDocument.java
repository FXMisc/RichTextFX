package org.fxmisc.richtext.model;

/**
 * Provides an implementation of {@link EditableStyledDocument} that is specified for {@link StyledText} as its segment.
 * See also {@link GenericEditableStyledDocument}.
 */
public final class SimpleEditableStyledDocument<PS, S> extends GenericEditableStyledDocumentBase<PS, StyledText<S>, S> {

    public SimpleEditableStyledDocument(PS initialParagraphStyle, S initialStyle) {
        super(initialParagraphStyle, initialStyle, StyledText.textOps());
    }
}
