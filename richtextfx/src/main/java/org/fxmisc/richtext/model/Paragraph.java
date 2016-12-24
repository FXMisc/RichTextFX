package org.fxmisc.richtext.model;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javafx.scene.control.IndexRange;

import org.fxmisc.richtext.model.TwoDimensional.Position;

public final class Paragraph<PS, S> {

    @SafeVarargs
    private static <T> List<T> list(T head, T... tail) {
        if(tail.length == 0) {
            return Collections.singletonList(head);
        } else {
            ArrayList<T> list = new ArrayList<>(1 + tail.length);
            list.add(head);
            Collections.addAll(list, tail);
            return list;
        }
    }

    private final List<Segment<S>> segments;
    private final TwoLevelNavigator navigator;
    private final PS paragraphStyle;

    /**
     * Creates a new Paragraph with one styled text segment which contains the given text.
     *
     * @param paragraphStyle The paragraph style to use.
     * @param text The text contained in the paragraph.
     * @param style The style of the text.
     */
    public Paragraph(PS paragraphStyle, String text, S style) {
        this(paragraphStyle, new StyledText<>(text, style));
    }

    @SafeVarargs
    public Paragraph(PS paragraphStyle, Segment<S> text, Segment<S>... texts) {
        this(paragraphStyle, list(text, texts));
    }

    Paragraph(PS paragraphStyle, List<Segment<S>> segments) {
        assert !segments.isEmpty();
        this.segments = segments;
        this.paragraphStyle = paragraphStyle;
        navigator = new TwoLevelNavigator(segments::size,
                i -> segments.get(i).length());
    }

    public List<Segment<S>> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    public PS getParagraphStyle() {
        return paragraphStyle;
    }

    private int length = -1;
    public int length() {
        if(length == -1) {
            length = segments.stream().mapToInt(Segment::length).sum();
        }
        return length;
    }

    public char charAt(int index) {
        Position pos = navigator.offsetToPosition(index, Forward);
        return segments.get(pos.getMajor()).charAt(pos.getMinor());
    }

    public String substring(int from, int to) {
        return getText().substring(from, Math.min(to, length()));
    }

    public String substring(int from) {
        return getText().substring(from);
    }

    /**
     * Concatenates this paragraph with the given paragraph {@code p}.
     * The paragraph style of the result will be that of this paragraph,
     * unless this paragraph is empty and {@code p} is non-empty, in which
     * case the paragraph style of the result will be that of {@code p}.
     * 
     * @param p The paragraph to append.
     * @return A paragraph which contains all text segments from this paragraph plus all
     *         text segments from the other paragraph. If the style of the last text segment from this
     *         paragraph matches the style of the first text segment of the other
     *         paragraph, the text segments are combined into one single text segment.
     */
    public Paragraph<PS, S> concat(Paragraph<PS, S> p) {
        if(p.length() == 0) {
            return this;
        }

        if(length() == 0) {
            return p;
        }

        Segment<S> left = segments.get(segments.size() - 1);
        Segment<S> right = p.segments.get(0);
        if(canJoin(left, right)) {
            Segment<S> segment = left.append(right.getText());
            List<Segment<S>> segs = new ArrayList<>(segments.size() + p.segments.size() - 1);
            segs.addAll(segments.subList(0, segments.size()-1));
            segs.add(segment);
            segs.addAll(p.segments.subList(1, p.segments.size()));
            return new Paragraph<>(paragraphStyle, segs);
        } else {
            List<Segment<S>> segs = new ArrayList<>(segments.size() + p.segments.size());
            segs.addAll(segments);
            segs.addAll(p.segments);
            return new Paragraph<>(paragraphStyle, segs);
        }
    }

    private boolean canJoin(Segment<S> left, Segment<S> right) {
        // if either of the segments are not StyledText segments, they can not be joined 
        if (left.getTypeId() != DefaultSegmentTypes.STYLED_TEXT || right.getTypeId() != DefaultSegmentTypes.STYLED_TEXT) {
            return false;
        }

        // otherwise, if the segments have the same style, they can be joined.
        return Objects.equals(left.getStyle(), right.getStyle());
    }

    /**
     * Similar to {@link #concat(Paragraph)}, except in case both paragraphs
     * are empty, the result's paragraph style will be that of the argument.
     */
    Paragraph<PS, S> concatR(Paragraph<PS, S> that) {
        return this.length() == 0 && that.length() == 0
            ? that
            : concat(that);
    }

    public Paragraph<PS, S> append(String str) {
        if(str.length() == 0) {
            return this;
        }

        List<Segment<S>> segs = new ArrayList<>(segments);
        int lastIdx = segments.size() - 1;
        segs.set(lastIdx, segments.get(lastIdx).append(str));
        return new Paragraph<>(paragraphStyle, segs);
    }

    public Paragraph<PS, S> insert(int offset, CharSequence str) {
        if(offset < 0 || offset > length()) {
            throw new IndexOutOfBoundsException(String.valueOf(offset));
        }

        Position pos = navigator.offsetToPosition(offset, Backward);
        int segIdx = pos.getMajor();
        int segPos = pos.getMinor();
        Segment<S> seg = segments.get(segIdx);
        Segment<S> replacement = seg.spliced(segPos, segPos, str);
        List<Segment<S>> segs = new ArrayList<>(segments);
        segs.set(segIdx, replacement);
        return new Paragraph<>(paragraphStyle, segs);
    }

    public Paragraph<PS, S> subSequence(int start, int end) {
        return trim(end).subSequence(start);
    }

    public Paragraph<PS, S> trim(int length) {
        if(length >= length()) {
            return this;
        } else {
            Position pos = navigator.offsetToPosition(length, Backward);
            int segIdx = pos.getMajor();
            List<Segment<S>> segs = new ArrayList<>(segIdx + 1);
            segs.addAll(segments.subList(0, segIdx));
            segs.add(segments.get(segIdx).subSequence(0, pos.getMinor()));
            return new Paragraph<>(paragraphStyle, segs);
        }
    }

    public Paragraph<PS, S> subSequence(int start) {
        if(start < 0) {
            throw new IllegalArgumentException("start must not be negative (was: " + start + ")");
        } else if(start == 0) {
            return this;
        } else if(start <= length()) {
            Position pos = navigator.offsetToPosition(start, Forward);
            int segIdx = pos.getMajor();
            List<Segment<S>> segs = new ArrayList<>(segments.size() - segIdx);
            segs.add(segments.get(segIdx).subSequence(pos.getMinor()));
            segs.addAll(segments.subList(segIdx + 1, segments.size()));
            return new Paragraph<>(paragraphStyle, segs);
        } else {
            throw new IndexOutOfBoundsException(start + " not in [0, " + length() + "]");
        }
    }

    public Paragraph<PS, S> delete(int start, int end) {
        return trim(start).concat(subSequence(end));
    }

    public Paragraph<PS, S> restyle(S style) {
        return new Paragraph<>(paragraphStyle, getText(), style);
    }

    public Paragraph<PS, S> restyle(int from, int to, S style) {
        if(from >= length()) {
            return this;
        } else {
            to = Math.min(to, length());
            Paragraph<PS, S> left = subSequence(0, from);
            Paragraph<PS, S> middle = new Paragraph<>(paragraphStyle, substring(from, to), style);
            Paragraph<PS, S> right = subSequence(to);
            return left.concat(middle).concat(right);
        }
    }

    public Paragraph<PS, S> restyle(int from, StyleSpans<? extends S> styleSpans) {
        int len = styleSpans.length();
        if(styleSpans.equals(getStyleSpans(from, from + len))) {
            return this;
        }

        Paragraph<PS, S> left = trim(from);
        Paragraph<PS, S> right = subSequence(from + len);

        String middleString = substring(from, from + len);
        List<Segment<S>> middleSegs = new ArrayList<>(styleSpans.getSpanCount());
        int offset = 0;
        for(StyleSpan<? extends S> span: styleSpans) {
            int end = offset + span.getLength();
            String text = middleString.substring(offset, end);
            middleSegs.add(new StyledText<>(text, span.getStyle()));
            offset = end;
        }
        Paragraph<PS, S> middle = new Paragraph<>(paragraphStyle, middleSegs);

        return left.concat(middle).concat(right);
    }

    public Paragraph<PS, S> setParagraphStyle(PS paragraphStyle) {
        return new Paragraph<>(paragraphStyle, segments);
    }

    /**
     * Returns the style of character with the given index.
     * If {@code charIdx < 0}, returns the style at the beginning of this paragraph.
     * If {@code charIdx >= this.length()}, returns the style at the end of this paragraph.
     */
    public S getStyleOfChar(int charIdx) {
        if(charIdx < 0) {
            return segments.get(0).getStyle();
        }

        Position pos = navigator.offsetToPosition(charIdx, Forward);
        return segments.get(pos.getMajor()).getStyle();
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
        if(position < 0) {
            throw new IllegalArgumentException("Paragraph position cannot be negative (" + position + ")");
        }

        Position pos = navigator.offsetToPosition(position, Backward);
        return segments.get(pos.getMajor()).getStyle();
    }

    /**
     * Returns the range of homogeneous style that includes the given position.
     * If {@code position} points to a boundary between two styled ranges,
     * then the range preceding {@code position} is returned.
     */
    public IndexRange getStyleRangeAtPosition(int position) {
        Position pos = navigator.offsetToPosition(position, Backward);
        int start = position - pos.getMinor();
        int end = start + segments.get(pos.getMajor()).length();
        return new IndexRange(start, end);
    }

    public StyleSpans<S> getStyleSpans() {
        StyleSpansBuilder<S> builder = new StyleSpansBuilder<>(segments.size());
        for(Segment<S> seg: segments) {
            builder.add(seg.getStyle(), seg.length());
        }
        return builder.create();
    }

    public StyleSpans<S> getStyleSpans(int from, int to) {
        Position start = navigator.offsetToPosition(from, Forward);
        Position end = to == from
                ? start
                : start.offsetBy(to - from, Backward);
        int startSegIdx = start.getMajor();
        int endSegIdx = end.getMajor();

        int n = endSegIdx - startSegIdx + 1;
        StyleSpansBuilder<S> builder = new StyleSpansBuilder<>(n);

        if(startSegIdx == endSegIdx) {
            Segment<S> seg = segments.get(startSegIdx);
            builder.add(seg.getStyle(), to - from);
        } else {
            Segment<S> startSeg = segments.get(startSegIdx);
            builder.add(startSeg.getStyle(), startSeg.length() - start.getMinor());

            for(int i = startSegIdx + 1; i < endSegIdx; ++i) {
                Segment<S> seg = segments.get(i);
                builder.add(seg.getStyle(), seg.length());
            }

            Segment<S> endSeg = segments.get(endSegIdx);
            builder.add(endSeg.getStyle(), end.getMinor());
        }

        return builder.create();
    }

    private String text = null;
    /**
     * Returns the plain text content of this paragraph,
     * not including the line terminator.
     */
    public String getText() {
        if(text == null) {
            StringBuilder sb = new StringBuilder(length());
            for(Segment<S> seg: segments)
                sb.append(seg.getText());
            text = sb.toString();
        }
        return text;
    }

    @Override
    public String toString() {
        return
                "Par[" + paragraphStyle  + "; " +
                segments.stream().map(Segment::toString)
                        .reduce((s1, s2) -> s1 + "," + s2).orElse("") +
                "]";
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof Paragraph) {
            Paragraph<?, ?> that = (Paragraph<?, ?>) other;
            return Objects.equals(this.paragraphStyle, that.paragraphStyle)
                && Objects.equals(this.segments, that.segments);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(paragraphStyle, segments);
    }

}
