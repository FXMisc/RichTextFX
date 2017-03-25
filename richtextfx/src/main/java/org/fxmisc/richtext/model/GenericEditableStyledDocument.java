package org.fxmisc.richtext.model;

/**
 * Provides a basic implementation of {@link EditableStyledDocument}. See {@link SimpleEditableStyledDocument} for
 * a version that is specified for {@link StyledText}.
 * @param <PS> type of style that can be applied to paragraphs (e.g. {@link javafx.scene.text.TextFlow}.
 * @param <SEG> type of segment used in {@link Paragraph}. Can be only {@link org.fxmisc.richtext.TextExt text}
 *             (plain or styled) or a type that combines text and other {@link javafx.scene.Node}s.
 * @param <S> type of style that can be applied to a segment.
 */
public final class GenericEditableStyledDocument<PS, SEG, S> extends GenericEditableStyledDocumentBase<PS, SEG, S>
        implements EditableStyledDocument<PS, SEG, S> {

    public GenericEditableStyledDocument(PS initialParagraphStyle, S initialStyle, TextOps<SEG, S> segmentOps) {
        super(initialParagraphStyle, initialStyle, segmentOps);
    }

}
