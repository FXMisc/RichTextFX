package org.fxmisc.richtext;

import javafx.scene.control.IndexRange;

import java.util.List;

public interface Paragraph<S, PS> extends CharSequence {

    public List<StyledText<S>> getSegments();

    public PS getParagraphStyle();

    public String substring(int from, int to);

    public String substring(int from);

    public Paragraph<S, PS> concat(Paragraph<S, PS> p);

    public Paragraph<S, PS> concat(CharSequence str);

    public Paragraph<S, PS> insert(int offset, CharSequence str);

    public Paragraph<S, PS> trim(int length);

    public Paragraph<S, PS> subSequence(int start);

    public Paragraph<S, PS> subSequence(int start, int end);

    public Paragraph<S, PS> delete(int start, int end);

    public Paragraph<S, PS> restyle(S style);

    public Paragraph<S, PS> restyle(int from, int to, S style);

    public Paragraph<S, PS> restyle(int from, StyleSpans<? extends S> styleSpans);

    public Paragraph<S, PS> setParagraphStyle(PS paragraphStyle);

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
}
