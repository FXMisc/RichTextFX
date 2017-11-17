package org.fxmisc.richtext.model;

/**
 * Provides a basic implementation of {@link EditableStyledDocument} while still allowing a developer to specify its
 * generics. See {@link SimpleEditableStyledDocument} for a version that specifies its segment style to {@link String}.
 *
 * @param <PS> type of style that can be applied to paragraphs (e.g. {@link javafx.scene.text.TextFlow}.
 * @param <SEG> type of segment used in {@link Paragraph}. Can be only {@link org.fxmisc.richtext.TextExt text}
 *             (plain or styled) or a type that combines text and other {@link javafx.scene.Node}s via
 *             {@code Either<String, Node>}.
 * @param <S> type of style that can be applied to a segment.
 */
public final class GenericEditableStyledDocument<PS, SEG, S> extends GenericEditableStyledDocumentBase<PS, SEG, S>
        implements EditableStyledDocument<PS, SEG, S> {

    /**
     * Creates an {@link EditableStyledDocument} with the given document as its initial content
     */
    public GenericEditableStyledDocument(ReadOnlyStyledDocument<PS, SEG, S> initialContent) {
        super(initialContent);
    }

    /**
     * Creates an {@link EditableStyledDocument} with given paragraph as its initial content
     */
    public GenericEditableStyledDocument(Paragraph<PS, SEG, S> initialParagraph) {
        super(initialParagraph);
    }

    /**
     * Creates an empty {@link EditableStyledDocument}
     */
    public GenericEditableStyledDocument(PS initialParagraphStyle, S initialStyle, SegmentOps<SEG, S> segmentOps) {
        super(initialParagraphStyle, initialStyle, segmentOps);
    }

}
