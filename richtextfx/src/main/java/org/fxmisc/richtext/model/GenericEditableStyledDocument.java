package org.fxmisc.richtext.model;

/**
 * Provides a basic implementation of {@link EditableStyledDocument}. See {@link SimpleEditableStyledDocument} for
 * a version that is specified for {@link StyledText}.
 * @param <PS>
 * @param <SEG>
 * @param <S>
 */
public final class GenericEditableStyledDocument<PS, SEG, S> extends GenericEditableStyledDocumentBase<PS, SEG, S>
        implements EditableStyledDocument<PS, SEG, S> {

    public GenericEditableStyledDocument(PS initialParagraphStyle, S initialStyle, TextOps<SEG, S> segmentOps) {
        super(initialParagraphStyle, initialStyle, segmentOps);
    }

}
