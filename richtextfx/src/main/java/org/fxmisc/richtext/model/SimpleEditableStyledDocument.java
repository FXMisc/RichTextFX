package org.fxmisc.richtext.model;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Provides an implementation of {@link EditableStyledDocument} that specifies its segment type as {@link String}.
 * For a version that can specify the segment's generics, see {@link GenericEditableStyledDocument}.
 */
public final class SimpleEditableStyledDocument<PS, S> extends GenericEditableStyledDocumentBase<PS, String, S> {

    /**
     * Creates a document that does not merge consecutive styles
     */
    public SimpleEditableStyledDocument(PS initialParagraphStyle, S initialStyle) {
        this(initialParagraphStyle, initialStyle, (s1, s2) -> Optional.empty());
    }

    /**
     * Creates a document that uses {@link SegmentOps#styledTextOps(BiFunction)} to operate on segments and merge
     * consecutive styles.
     */
    public SimpleEditableStyledDocument(PS initialParagraphStyle, S initialStyle,
                                        BiFunction<S, S, Optional<S>> mergeStyle) {
        this(initialParagraphStyle, initialStyle, SegmentOps.styledTextOps(mergeStyle));
    }

    /**
     * Creates a document that uses a custom {@link SegmentOps} to operate on segments and merge styles.
     */
    public SimpleEditableStyledDocument(PS initialParagraphStyle, S initialTextStyle, SegmentOps<String, S> segOps) {
        super(initialParagraphStyle, initialTextStyle, segOps);
    }

    /**
     * Creates an {@link EditableStyledDocument} with given paragraph as its initial content
     */
    public SimpleEditableStyledDocument(Paragraph<PS, String, S> initialParagraph) {
        super(initialParagraph);
    }

    /**
     * Creates an {@link EditableStyledDocument} with the given document as its initial content
     */
    public SimpleEditableStyledDocument(ReadOnlyStyledDocument<PS, String, S> initialContent) {
        super(initialContent);
    }
}
