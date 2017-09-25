package org.fxmisc.richtext.model;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Provides an implementation of {@link EditableStyledDocument} that is specified for {@link String} as its segment.
 * See also {@link GenericEditableStyledDocument}.
 */
public final class SimpleEditableStyledDocument<PS, S> extends GenericEditableStyledDocumentBase<PS, String, S> {

    public SimpleEditableStyledDocument(PS initialParagraphStyle, S initialStyle) {
        this(initialParagraphStyle, initialStyle, (s1, s2) -> Optional.empty());
    }

    public SimpleEditableStyledDocument(PS initialParagraphStyle, S initialStyle,
                                        BiFunction<S, S, Optional<S>> mergeStyle) {
        this(initialParagraphStyle, initialStyle, SegmentOps.styledTextOps(mergeStyle));
    }

    public SimpleEditableStyledDocument(PS initialParagraphStyle, S initialTextStyle, SegmentOps<String, S> segOps) {
        super(initialParagraphStyle, initialTextStyle, segOps);
    }
}
