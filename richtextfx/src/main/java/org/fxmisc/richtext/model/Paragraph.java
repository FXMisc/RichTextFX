package org.fxmisc.richtext.model;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.scene.control.IndexRange;

import org.fxmisc.richtext.model.TwoDimensional.Position;

/**
 * This is one Paragraph of the document. Depending on whether the text is wrapped,
 * it corresponds to a single line or it can also span multiple lines. A Paragraph
 * contains of a list of SEG objects which make up the individual segments of the
 * Paragraph. By providing a specific segment object and an associated segment
 * operations object, all required data and the necessary operations on this data
 * for a single segment can be provided. For example, {@link StyledText} is a segment
 * type which is used in {@link org.fxmisc.richtext.StyledTextArea}, a text area which can render simple
 * text only (which is already sufficient to implement all kinds of code editors).
 *
 * <p>For more complex requirements (for example, when images shall be part of the
 * document) a different segment type must be provided (which can make use of
 * {@code StyledText<S>} for the text part and add another segment type for images).
 * <b>Note that Paragraph is an immutable class</b> - to modify a Paragraph, a new
 * Paragraph object must be created. Paragraph itself contains some methods which
 * take care of this, such as concat(), which appends some Paragraph to the current
 * one and returns a new Paragraph.</p>
 *
 * @param <PS> The type of the paragraph style.
 * @param <SEG> The type of the content segments in the paragraph (e.g. {@link StyledText}).
 *              Every paragraph, even an empty paragraph, must have at least one SEG object
 *             (even if that SEG object itself represents an empty segment).
 * @param <S> The type of the style of individual segments.
 */
public final class Paragraph<PS, SEG, S> {

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

    private final List<SEG> segments;
    private final TwoLevelNavigator navigator;
    private final PS paragraphStyle;

    private final SegmentOps<SEG, S> segmentOps;

    @SafeVarargs
    public Paragraph(PS paragraphStyle, SegmentOps<SEG, S> segmentOps, SEG text, SEG... texts) {
        this(paragraphStyle, segmentOps, list(text, texts));
    }

    Paragraph(PS paragraphStyle, SegmentOps<SEG, S> segmentOps, List<SEG> segments) {
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Cannot construct a Paragraph with an empty list of segments");
        }

        this.segmentOps = segmentOps;
        this.segments = segments;
        this.paragraphStyle = paragraphStyle;
        navigator = new TwoLevelNavigator(segments::size,
                i -> segmentOps.length(segments.get(i)));
    }

    public List<SEG> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    public PS getParagraphStyle() {
        return paragraphStyle;
    }

    private int length = -1;
    public int length() {
        if(length == -1) {
            length = segments.stream().mapToInt(segmentOps::length).sum();
        }
        return length;
    }

    public char charAt(int index) {
        Position pos = navigator.offsetToPosition(index, Forward);
        return segmentOps.charAt(segments.get(pos.getMajor()), pos.getMinor());
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
     */
    public Paragraph<PS, SEG, S> concat(Paragraph<PS, SEG, S> p) {
        if(p.length() == 0) {
            return this;
        }

        if(length() == 0) {
            return p;
        }

        SEG left = segments.get(segments.size() - 1);
        SEG right = p.segments.get(0);
        Optional<SEG> joined = segmentOps.join(left, right);
        if(joined.isPresent()) {
            SEG segment = joined.get();
            List<SEG> segs = new ArrayList<>(segments.size() + p.segments.size() - 1);
            segs.addAll(segments.subList(0, segments.size()-1));
            segs.add(segment);
            segs.addAll(p.segments.subList(1, p.segments.size()));
            return new Paragraph<>(paragraphStyle, segmentOps, segs);
        } else {
            List<SEG> segs = new ArrayList<>(segments.size() + p.segments.size());
            segs.addAll(segments);
            segs.addAll(p.segments);
            return new Paragraph<>(paragraphStyle, segmentOps, segs);
        }
    }

    /**
     * Similar to {@link #concat(Paragraph)}, except in case both paragraphs
     * are empty, the result's paragraph style will be that of the argument.
     */
    Paragraph<PS, SEG, S> concatR(Paragraph<PS, SEG, S> that) {
        return this.length() == 0 && that.length() == 0
            ? that
            : concat(that);
    }

    public Paragraph<PS, SEG, S> subSequence(int start, int end) {
        return trim(end).subSequence(start);
    }

    public Paragraph<PS, SEG, S> trim(int length) {
        if(length >= length()) {
            return this;
        } else {
            Position pos = navigator.offsetToPosition(length, Backward);
            int segIdx = pos.getMajor();
            List<SEG> segs = new ArrayList<>(segIdx + 1);
            segs.addAll(segments.subList(0, segIdx));
            segs.add(segmentOps.subSequence(segments.get(segIdx), 0, pos.getMinor()));
            if (segs.isEmpty()) {
                segs.add(segmentOps.createEmpty());
            }
            return new Paragraph<>(paragraphStyle, segmentOps, segs);
        }
    }

    public Paragraph<PS, SEG, S> subSequence(int start) {
        if(start < 0) {
            throw new IllegalArgumentException("start must not be negative (was: " + start + ")");
        } else if(start == 0) {
            return this;
        } else if(start <= length()) {
            Position pos = navigator.offsetToPosition(start, Forward);
            int segIdx = pos.getMajor();
            List<SEG> segs = new ArrayList<>(segments.size() - segIdx);
            segs.add(segmentOps.subSequence(segments.get(segIdx), pos.getMinor()));
            segs.addAll(segments.subList(segIdx + 1, segments.size()));
            if (segs.isEmpty()) {
                segs.add(segmentOps.createEmpty());
            }
            return new Paragraph<>(paragraphStyle, segmentOps, segs);
        } else {
            throw new IndexOutOfBoundsException(start + " not in [0, " + length() + "]");
        }
    }

    public Paragraph<PS, SEG, S> delete(int start, int end) {
        return trim(start).concat(subSequence(end));
    }

    /**
     * Restyles every segment in the paragraph to have the given style.
     *
     * Note: because Paragraph is immutable, this method returns a new Paragraph.
     * The current Paragraph is unchanged.
     *
     * @param style The new style for each segment in the paragraph.
     * @return The new paragraph with the restyled segments.
     */
    public Paragraph<PS, SEG, S> restyle(S style) {
        List<SEG> segs = new ArrayList<>();
        Iterator<SEG> it = segments.iterator();
        segs.add(segmentOps.setStyle(it.next(), style));
        while (it.hasNext()) {
            SEG prev = segs.get(segs.size() - 1);
            SEG cur = segmentOps.setStyle(it.next(), style);
            Optional<SEG> joined = segmentOps.join(prev, cur);
            if(joined.isPresent()) {
                segs.set(segs.size() - 1, joined.get());
            } else {
                segs.add(cur);
            }
        }
        return new Paragraph<>(paragraphStyle, segmentOps, segs);
    }

    public Paragraph<PS, SEG, S> restyle(int from, int to, S style) {
        if(from >= length()) {
            return this;
        } else {
            to = Math.min(to, length());
            Paragraph<PS, SEG, S> left = subSequence(0, from);
            Paragraph<PS, SEG, S> middle = subSequence(from, to).restyle(style);
            Paragraph<PS, SEG, S> right = subSequence(to);
            return left.concat(middle).concat(right);
        }
    }

    public Paragraph<PS, SEG, S> restyle(int from, StyleSpans<? extends S> styleSpans) {
        int len = styleSpans.length();
        if(styleSpans.equals(getStyleSpans(from, from + len)) || length() == 0) {
            return this;
        }

        Paragraph<PS, SEG, S> left = trim(from);
        Paragraph<PS, SEG, S> right = subSequence(from + len);

        Paragraph<PS, SEG, S> middle = subSequence(from, from + len);
        List<SEG> middleSegs = new ArrayList<>(styleSpans.getSpanCount());
        int offset = 0;
        for(StyleSpan<? extends S> span: styleSpans) {
            int end = offset + span.getLength();
            Paragraph<PS, SEG, S> text = middle.subSequence(offset, end);
            middleSegs.addAll(text.restyle(span.getStyle()).segments);
            offset = end;
        }
        Paragraph<PS, SEG, S> newMiddle = new Paragraph<>(paragraphStyle, segmentOps, middleSegs);

        return left.concat(newMiddle).concat(right);
    }

    /**
     * Creates a new Paragraph which has the same contents as the current Paragraph,
     * but the given paragraph style.
     *
     * Note that because Paragraph is immutable, a new Paragraph is returned.
     * Despite the setX name, the current object is unchanged.
     *
     * @param paragraphStyle The new paragraph style
     * @return A new paragraph with the same segment contents, but a new paragraph style.
     */
    public Paragraph<PS, SEG, S> setParagraphStyle(PS paragraphStyle) {
        return new Paragraph<>(paragraphStyle, segmentOps, segments);
    }

    /**
     * Returns the style of character with the given index.
     * If {@code charIdx < 0}, returns the style at the beginning of this paragraph.
     * If {@code charIdx >= this.length()}, returns the style at the end of this paragraph.
     */
    public S getStyleOfChar(int charIdx) {
        if(charIdx < 0) {
            return segmentOps.getStyle(segments.get(0));
        }

        Position pos = navigator.offsetToPosition(charIdx, Forward);
        return segmentOps.getStyle(segments.get(pos.getMajor()));
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
        return segmentOps.getStyle(segments.get(pos.getMajor()));
    }

    /**
     * Returns the range of homogeneous style that includes the given position.
     * If {@code position} points to a boundary between two styled ranges,
     * then the range preceding {@code position} is returned.
     */
    public IndexRange getStyleRangeAtPosition(int position) {
        Position pos = navigator.offsetToPosition(position, Backward);
        int start = position - pos.getMinor();
        int end = start + segmentOps.length(segments.get(pos.getMajor()));
        return new IndexRange(start, end);
    }

    public StyleSpans<S> getStyleSpans() {
        StyleSpansBuilder<S> builder = new StyleSpansBuilder<>(segments.size());
        for(SEG seg: segments) {
            builder.add(segmentOps.getStyle(seg),
                        segmentOps.length(seg));
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
            SEG seg = segments.get(startSegIdx);
            builder.add(segmentOps.getStyle(seg), to - from);
        } else {
            SEG startSeg = segments.get(startSegIdx);
            builder.add(segmentOps.getStyle(startSeg), segmentOps.length(startSeg) - start.getMinor());

            for(int i = startSegIdx + 1; i < endSegIdx; ++i) {
                SEG seg = segments.get(i);
                builder.add(segmentOps.getStyle(seg),
                            segmentOps.length(seg));
            }

            SEG endSeg = segments.get(endSegIdx);
            builder.add(segmentOps.getStyle(endSeg), end.getMinor());
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
            for(SEG seg: segments)
                sb.append(segmentOps.getText(seg));
            text = sb.toString();
        }
        return text;
    }

    @Override
    public String toString() {
        return
                "Par[" + paragraphStyle  + "; " +
                segments.stream().map(Object::toString)
                        .reduce((s1, s2) -> s1 + "," + s2).orElse("") +
                "]";
    }

    /**
     * Two paragraphs are defined to be equal if they have the same style (as defined by
     * PS.equals) and the same list of segments (as defined by SEG.equals).
     */
    @Override
    public boolean equals(Object other) {
        if(other instanceof Paragraph) {
            Paragraph<?, ?, ?> that = (Paragraph<?, ?, ?>) other;
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
