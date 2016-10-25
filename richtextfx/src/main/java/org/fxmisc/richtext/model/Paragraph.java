package org.fxmisc.richtext.model;

import javafx.scene.control.IndexRange;

import java.util.List;

public interface Paragraph<PS, SEG, S> {

    public List<SEG> getSegments();

    public PS getParagraphStyle();

    public int length();

    public char charAt(int index);

    public String substring(int from, int to);

    public String substring(int from);

    /**
     * Concatenates this paragraph with the given paragraph {@code p}.
     * The paragraph style of the result will be that of this paragraph,
     * unless this paragraph is empty and {@code p} is non-empty, in which
     * case the paragraph style of the result will be that of {@code p}.
     */
    public Paragraph<PS, SEG, S> concat(Paragraph<PS, SEG, S> p);

    /**
     * Similar to {@link #concat(Paragraph)}, except in case both paragraphs
     * are empty, the result's paragraph style will be that of the argument.
     */
    Paragraph<PS, SEG, S> concatR(Paragraph<PS, SEG, S> that);

    public Paragraph<PS, SEG, S> subSequence(int start, int end);

    public Paragraph<PS, SEG, S> trim(int length);

    public Paragraph<PS, SEG, S> subSequence(int start);

    public Paragraph<PS, SEG, S> delete(int start, int end);

    public Paragraph<PS, SEG, S> restyle(S style);

    public Paragraph<PS, SEG, S> restyle(int from, int to, S style);

    public Paragraph<PS, SEG, S> restyle(int from, StyleSpans<? extends S> styleSpans);

    public Paragraph<PS, SEG, S> setParagraphStyle(PS paragraphStyle);

    /**
     * Returns the style of character with the given index.
     * If {@code charIdx < 0}, returns the style at the beginning of this paragraph.
     * If {@code charIdx >= this.length()}, returns the style at the end of this paragraph.
     */
    public S getStyleOfChar(int charIdx);

    /**
     * Returns the style at the given position. That is the style of the
     * character immediately preceding {@code position}. If {@code position}
     * is 0, then the style of the first character (index 0) in this paragraph
     * is returned. If this paragraph is empty, then some style previously used
     * in this paragraph is returned.
     * If {@code position > this.length()}, then it is equivalent to
     * {@code position == this.length()}.
     *
     * <p>In other words, {@code getStyleAtPosition(p)} is equivalent to
     * {@code getStyleOfChar(p-1)}.
     */
    public S getStyleAtPosition(int position);

    /**
     * Returns the range of homogeneous style that includes the given position.
     * If {@code position} points to a boundary between two styled ranges,
     * then the range preceding {@code position} is returned.
     */
    public IndexRange getStyleRangeAtPosition(int position);

    public StyleSpans<S> getStyleSpans();

    public StyleSpans<S> getStyleSpans(int from, int to);

    /**
     * Returns the plain text content of this paragraph,
     * not including the line terminator.
     */
    public String getText();
}
