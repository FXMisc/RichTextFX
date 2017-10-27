package org.fxmisc.richtext;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.model.StyleSpans;

/**
 * Specifies actions related to getting and setting styles throughout a {@link TextEditingArea}.
 *
 * @param <PS> the paragraph style
 * @param <S> the segment's style
 */
public interface StyleActions<PS, S> {

    /**
     * Indicates whether the initial style should also be used for plain text
     * inserted into this text area. When {@code false}, the style immediately
     * preceding the insertion position is used. Default value is {@code false}.
     */
    default boolean getUseInitialStyleForInsertion() { return useInitialStyleForInsertionProperty().get(); }
    default void setUseInitialStyleForInsertion(boolean value) { useInitialStyleForInsertionProperty().set(value); }
    BooleanProperty useInitialStyleForInsertionProperty();

    /**
     * Style used by default when no other style is provided.
     */
    S getInitialTextStyle();

    /**
     * Style used by default when no other style is provided.
     */
    PS getInitialParagraphStyle();

    /**
     * Returns {@link #getInitialTextStyle()} if {@link #getUseInitialStyleForInsertion()} is true;
     * otherwise, returns the style at the given position.
     */
    S getTextStyleForInsertionAt(int pos);

    /**
     * Returns {@link #getInitialParagraphStyle()} if {@link #getUseInitialStyleForInsertion()} is true;
     * otherwise, returns the paragraph style at the given position.
     */
    PS getParagraphStyleForInsertionAt(int pos);

    /**
     * Indicates whether style should be preserved on undo/redo (and in the future copy/paste and text move).
     */
    boolean isPreserveStyle();

    /**
     * Returns the style of the character with the given index.
     * If {@code index} points to a line terminator character,
     * the last style used in the paragraph terminated by that
     * line terminator is returned.
     */
    S getStyleOfChar(int index);

    /**
     * Returns the style at the given position. That is the style of the
     * character immediately preceding {@code position}, except when
     * {@code position} points to a paragraph boundary, in which case it
     * is the style at the beginning of the latter paragraph.
     *
     * <p>In other words, most of the time {@code getStyleAtPosition(p)}
     * is equivalent to {@code getStyleOfChar(p-1)}, except when {@code p}
     * points to a paragraph boundary, in which case it is equivalent to
     * {@code getStyleOfChar(p)}.
     */
    S getStyleAtPosition(int position);

    /**
     * Returns the range of homogeneous style that includes the given position.
     * If {@code position} points to a boundary between two styled ranges, then
     * the range preceding {@code position} is returned. If {@code position}
     * points to a boundary between two paragraphs, then the first styled range
     * of the latter paragraph is returned.
     */
    IndexRange getStyleRangeAtPosition(int position);

    /**
     * Returns the styles in the given character range.
     */
    StyleSpans<S> getStyleSpans(int from, int to);

    /**
     * Returns the styles in the given character range.
     */
    default StyleSpans<S> getStyleSpans(IndexRange range) {
        return getStyleSpans(range.getStart(), range.getEnd());
    }

    /**
     * Returns the style of the character with the given index in the given
     * paragraph. If {@code index} is beyond the end of the paragraph, the
     * style at the end of line is returned. If {@code index} is negative, it
     * is the same as if it was 0.
     */
    S getStyleOfChar(int paragraph, int index);

    /**
     * Returns the style at the given position in the given paragraph.
     * This is equivalent to {@code getStyleOfChar(paragraph, position-1)}.
     */
    S getStyleAtPosition(int paragraph, int position);

    /**
     * Returns the range of homogeneous style that includes the given position
     * in the given paragraph. If {@code position} points to a boundary between
     * two styled ranges, then the range preceding {@code position} is returned.
     */
    IndexRange getStyleRangeAtPosition(int paragraph, int position);

    /**
     * Returns styles of the whole paragraph.
     */
    StyleSpans<S> getStyleSpans(int paragraph);

    /**
     * Returns the styles in the given character range of the given paragraph.
     */
    StyleSpans<S> getStyleSpans(int paragraph, int from, int to);

    /**
     * Returns the styles in the given character range of the given paragraph.
     */
    default StyleSpans<S> getStyleSpans(int paragraph, IndexRange range) {
        return getStyleSpans(paragraph, range.getStart(), range.getStart());
    }

    /**
     * Sets style for the given character range.
     */
    void setStyle(int from, int to, S style);

    /**
     * Sets style for the whole paragraph.
     */
    void setStyle(int paragraph, S style);

    /**
     * Sets style for the given range relative in the given paragraph.
     */
    void setStyle(int paragraph, int from, int to, S style);

    /**
     * Set multiple style ranges at once. This is equivalent to
     * <pre>
     * for(StyleSpan{@code <S>} span: styleSpans) {
     *     setStyle(from, from + span.getLength(), span.getStyle());
     *     from += span.getLength();
     * }
     * </pre>
     * but the actual implementation in {@link org.fxmisc.richtext.model.SimpleEditableStyledDocument} is
     * more efficient.
     */
    void setStyleSpans(int from, StyleSpans<? extends S> styleSpans);

    /**
     * Set multiple style ranges of a paragraph at once. This is equivalent to
     * <pre>
     * for(StyleSpan{@code <S>} span: styleSpans) {
     *     setStyle(paragraph, from, from + span.getLength(), span.getStyle());
     *     from += span.getLength();
     * }
     * </pre>
     * but the actual implementation in {@link org.fxmisc.richtext.model.SimpleEditableStyledDocument} is
     * more efficient.
     */
    void setStyleSpans(int paragraph, int from, StyleSpans<? extends S> styleSpans);

    /**
     * Sets style for the whole paragraph.
     */
    void setParagraphStyle(int paragraph, PS paragraphStyle);

    /**
     * Resets the style of the given range to the initial style.
     */
    default void clearStyle(int from, int to) {
        setStyle(from, to, getInitialTextStyle());
    }

    /**
     * Resets the style of the given range in the given paragraph
     * to the initial style.
     */
    default void clearStyle(int paragraph, int from, int to) {
        setStyle(paragraph, from, to, getInitialTextStyle());
    }

    /**
     * Resets the style of the given paragraph to the initial style.
     */
    default void clearStyle(int paragraph) {
        setStyle(paragraph, getInitialTextStyle());
    }

    /**
     * Resets the style of the given paragraph to the initial style.
     */
    default void clearParagraphStyle(int paragraph) {
        setParagraphStyle(paragraph, getInitialParagraphStyle());
    }
}
