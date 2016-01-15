package org.fxmisc.richtext;

import javafx.scene.control.IndexRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class EmptyParagraph<S, PS> implements Paragraph<S, PS> {

    public static final IndexRange EMPTY_RANGE = new IndexRange(0,0);

    private final List<StyledText<S>> segments = new ArrayList<>(0);
    private final PS paragraphStyle;
    private final S emptyTextStyle;

    EmptyParagraph(PS paragraphStyle, S emptyTextStyle) {
        this.paragraphStyle = paragraphStyle;
        this.emptyTextStyle = emptyTextStyle;
    }

    public List<StyledText<S>> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    public PS getParagraphStyle() {
        return paragraphStyle;
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public char charAt(int index) {
        return '\n';
    }

    public String substring(int from, int to) {
        return "";
    }

    public String substring(int from) {
        return "";
    }

    public Paragraph<S, PS> concat(Paragraph<S, PS> p) {
        return p;
    }

    // Refactor: use this to find cases where a CharSequence is used
    public Paragraph<S, PS> concat(CharSequence str) {
        throw new UnsupportedOperationException(
                "Empty Paragraph cannot create a new NormalParagraph because " +
                "it doesn't know what Style object to pass for the NormalParagraph's constructor. " +
                "Use `concat(Paragraph<S, PS> p)` instead"
        );
    }

    public Paragraph<S, PS> insert(int offset, Paragraph<S, PS> p) {
        return p;
    }

    // Refactor: use this to find cases where a CharSequence is used
    public Paragraph<S, PS> insert(int offset, CharSequence str) {
        throw new UnsupportedOperationException(
                "Empty Paragraph cannot create a new NormalParagraph because " +
                "it doesn't know what Style object to pass for the NormalParagraph's constructor. " +
                "Use `concat(Paragraph<S, PS> p)` instead"
        );
    }

    @Override
    public Paragraph<S, PS> subSequence(int start, int end) {
        return this;
    }

    public Paragraph<S, PS> trim(int length) {
        return this;
    }

    public Paragraph<S, PS> subSequence(int start) {
        return this;
    }

    public Paragraph<S, PS> delete(int start, int end) {
        return this;
    }

    public Paragraph<S, PS> restyle(S style) {
        return this;
    }

    public Paragraph<S, PS> restyle(int from, int to, S style) {
        return this;
    }

    public Paragraph<S, PS> restyle(int from, StyleSpans<? extends S> styleSpans) {
        return this;
    }

    public Paragraph<S, PS> setParagraphStyle(PS paragraphStyle) {
        return new EmptyParagraph<>(paragraphStyle, emptyTextStyle);
    }

    /**
     * Returns the style of character with the given index.
     * If {@code charIdx < 0}, returns the style at the beginning of this paragraph.
     * If {@code charIdx >= this.length()}, returns the style at the end of this paragraph.
     */
    public S getStyleOfChar(int charIdx) {
        return emptyTextStyle;
    }

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
    public S getStyleAtPosition(int position) {
        return emptyTextStyle;
    }

    /**
     * Returns the range of homogeneous style that includes the given position.
     * If {@code position} points to a boundary between two styled ranges,
     * then the range preceding {@code position} is returned.
     */
    public IndexRange getStyleRangeAtPosition(int position) {
        return EMPTY_RANGE;
    }

    public StyleSpans<S> getStyleSpans() {
        return new StyleSpansBuilder<S>(1)
                .add(emptyTextStyle, 0)
                .create();
    }

    public StyleSpans<S> getStyleSpans(int from, int to) {
        return getStyleSpans();
    }

    /**
     * Returns the string content of this paragraph (an empty string),
     * excluding the line terminator.
     */
    @Override
    public String toString() {
        return "";
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof EmptyParagraph) {
            EmptyParagraph<?, ?> that = (EmptyParagraph<?, ?>) other;
            return Objects.equals(this.paragraphStyle, that.paragraphStyle);
        } else {
            return false;
        }
    }
}
