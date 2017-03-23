package org.fxmisc.richtext.model;

import javafx.beans.NamedArg;

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

    public GenericEditableStyledDocument(
            @NamedArg("initialParagraphStyle") PS initialParagraphStyle,
            @NamedArg("initialTextStyle")      S initialTextStyle,
            @NamedArg("segmentOps")            TextOps<SEG, S> segmentOps) {
        super(initialParagraphStyle, initialTextStyle, segmentOps);
    }

}
