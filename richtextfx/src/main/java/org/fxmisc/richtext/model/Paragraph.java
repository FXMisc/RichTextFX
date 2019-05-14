package org.fxmisc.richtext.model;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.scene.control.IndexRange;

import org.fxmisc.richtext.model.TwoDimensional.Position;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuples;

/**
 * One paragraph in the document that can itself be styled and which contains a list of styled segments.
 *
 * <p>
 *     It corresponds to a single line when the
 *     text is not wrapped or spans multiple lines when the text is wrapped. A Paragraph
 *     contains of a list of {@link SEG} objects which make up the individual segments of the
 *     Paragraph. By providing a specific segment object and an associated
 *     {@link SegmentOps segment operations} object, all required data and the necessary
 *     operations on this data for a single segment can be provided.
 * </p>
 *
 * <p>For more complex requirements (for example, when both text and images shall be part
 * of the document), a different segment type must be provided. One should use something
 * like {@code Either<String, Image>} for their segment type.
 *
 * <b>Note that Paragraph is an immutable class</b> - to modify a Paragraph, a new
 * Paragraph object must be created. Paragraph itself contains some methods which
 * take care of this, such as concat(), which appends some Paragraph to the current
 * one and returns a new Paragraph.</p>
 *
 * @param <PS> The type of the paragraph style.
 * @param <SEG> The type of the content segments in the paragraph (e.g. {@link String}).
 *              Every paragraph, even an empty paragraph, must have at least one {@link SEG} object
 *              (even if that {@link SEG} object itself represents an empty segment).
 * @param <S> The type of the style of individual segments.
 */
public final class Paragraph<PS, SEG, S> {

    private static <SEG, S> Tuple2<List<SEG>, StyleSpans<S>> decompose(List<StyledSegment<SEG, S>> list,
                                                                       SegmentOps<SEG, S> segmentOps) {
        List<SEG> segs = new ArrayList<>();
        StyleSpansBuilder<S> builder = new StyleSpansBuilder<>();
        for (StyledSegment<SEG, S> styledSegment : list) {
            // attempt to merge differently-styled consecutive segments into one
            if (segs.isEmpty()) {
                segs.add(styledSegment.getSegment());
            } else {
                int lastIndex = segs.size() - 1;
                SEG previousSeg = segs.get(lastIndex);
                Optional<SEG> merged = segmentOps.joinSeg(previousSeg, styledSegment.getSegment());
                if (merged.isPresent()) {
                    segs.set(lastIndex, merged.get());
                } else {
                    segs.add(styledSegment.getSegment());
                }
            }
            // builder merges styles shared between consecutive different segments
            builder.add(styledSegment.getStyle(), segmentOps.length(styledSegment.getSegment()));
        }
        return Tuples.t(segs, builder.create());
    }

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
    private final StyleSpans<S> styles;
    private final TwoLevelNavigator navigator;
    private final PS paragraphStyle;

    private final SegmentOps<SEG, S> segmentOps;

    /**
     * Creates a paragraph using a list of styled segments
     */
    public Paragraph(PS paragraphStyle, SegmentOps<SEG, S> segmentOps, List<StyledSegment<SEG, S>> styledSegments) {
        this(paragraphStyle, segmentOps, decompose(styledSegments, segmentOps));
    }

    private Paragraph(PS paragraphStyle, SegmentOps<SEG, S> segmentOps, Tuple2<List<SEG>, StyleSpans<S>> decomposedList) {
        this(paragraphStyle, segmentOps, decomposedList._1, decomposedList._2);
    }

    /**
     * Creates a paragraph that has only one segment that has the same given style throughout.
     */
    public Paragraph(PS paragraphStyle, SegmentOps<SEG, S> segmentOps, SEG segment, S style) {
        this(paragraphStyle, segmentOps, segment, StyleSpans.singleton(style, segmentOps.length(segment)));
    }

    /**
     * Creates a paragraph that has only one segment but a number of different styles throughout that segment
     */
    public Paragraph(PS paragraphStyle, SegmentOps<SEG, S> segmentOps, SEG segment, StyleSpans<S> styles) {
        this(paragraphStyle, segmentOps, Collections.singletonList(segment), styles);
    }

    /**
     * Creates a paragraph that has multiple segments with multiple styles throughout those segments
     */
    public Paragraph(PS paragraphStyle, SegmentOps<SEG, S> segmentOps, List<SEG> segments, StyleSpans<S> styles) {
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Cannot construct a Paragraph with an empty list of segments");
        }
        if (styles.getSpanCount() == 0) {
            throw new IllegalArgumentException(
                    "Cannot construct a Paragraph with StyleSpans object that contains no StyleSpan objects"
            );
        }

        this.segmentOps = segmentOps;
        this.segments = segments;
        this.styles = styles;
        this.paragraphStyle = paragraphStyle;
        navigator = new TwoLevelNavigator(segments::size,
                i -> segmentOps.length(segments.get(i))
        );
    }

    private List<StyledSegment<SEG, S>> styledSegments = null;

    /**
     * Since the segments and styles in a paragraph are stored separate from another, combines these two collections
     * into a single collection where each segment and its corresponding style are grouped into the same object.
     * Essentially, returns {@code List<Tuple2<Segment, Style>>}.
     */
    public List<StyledSegment<SEG, S>> getStyledSegments() {
        if (styledSegments == null) {
            if (segments.size() == 1 && styles.getSpanCount() == 1) {
                styledSegments = Collections.singletonList(
                        new StyledSegment<>(segments.get(0), styles.getStyleSpan(0).getStyle())
                );
            } else {
                styledSegments = createStyledSegments();
            }
        }
        return styledSegments;
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

        List<SEG> updatedSegs;
        SEG leftSeg = segments.get(segments.size() - 1);
        SEG rightSeg = p.segments.get(0);
        Optional<SEG> joined = segmentOps.joinSeg(leftSeg, rightSeg);
        if(joined.isPresent()) {
            SEG segment = joined.get();
            updatedSegs = new ArrayList<>(segments.size() + p.segments.size() - 1);
            updatedSegs.addAll(segments.subList(0, segments.size()-1));
            updatedSegs.add(segment);
            updatedSegs.addAll(p.segments.subList(1, p.segments.size()));
        } else {
            updatedSegs = new ArrayList<>(segments.size() + p.segments.size());
            updatedSegs.addAll(segments);
            updatedSegs.addAll(p.segments);
        }

        StyleSpans<S> updatedStyles;
        StyleSpan<S> leftSpan = styles.getStyleSpan(styles.getSpanCount() - 1);
        StyleSpan<S> rightSpan = p.styles.getStyleSpan(0);
        Optional<S> merge = segmentOps.joinStyle(leftSpan.getStyle(), rightSpan.getStyle());
        if (merge.isPresent()) {
            int startOfMerge = styles.position(styles.getSpanCount() - 1, 0).toOffset();
            StyleSpans<S> updatedLeftSpan = styles.subView(0, startOfMerge);
            int endOfMerge = p.styles.position(1, 0).toOffset();
            StyleSpans<S> updatedRightSpan = p.styles.subView(endOfMerge, p.styles.length());
            updatedStyles = updatedLeftSpan
                    .append(merge.get(), leftSpan.getLength() + rightSpan.getLength())
                    .concat(updatedRightSpan);
        } else {
            updatedStyles = styles.concat(p.styles);
        }
        return new Paragraph<>(paragraphStyle, segmentOps, updatedSegs, updatedStyles);
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
                segs.add(segmentOps.createEmptySeg());
            }
            return new Paragraph<>(paragraphStyle, segmentOps, segs, styles.subView(0, length));
        }
    }

    public Paragraph<PS, SEG, S> subSequence(int start) {
        if(start < 0) {
            throw new IllegalArgumentException("start must not be negative (was: " + start + ")");
        } else if(start == 0) {
            return this;
        } else if (start == length()) {
            // in case one is using EitherOps<SegmentOps, SegmentOps>, force the empty segment
            // to use the left ops' default empty seg, not the right one's empty seg
            return new Paragraph<>(paragraphStyle, segmentOps, segmentOps.createEmptySeg(), styles.subView(start,start));
        } else if(start < length()) {
            Position pos = navigator.offsetToPosition(start, Forward);
            int segIdx = pos.getMajor();
            List<SEG> segs = new ArrayList<>(segments.size() - segIdx);
            segs.add(segmentOps.subSequence(segments.get(segIdx), pos.getMinor()));
            segs.addAll(segments.subList(segIdx + 1, segments.size()));
            if (segs.isEmpty()) {
                segs.add(segmentOps.createEmptySeg());
            }
            return new Paragraph<>(paragraphStyle, segmentOps, segs, styles.subView(start, styles.length()));
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
        return new Paragraph<>(paragraphStyle, segmentOps, segments, StyleSpans.singleton(style, length()));
    }

    public Paragraph<PS, SEG, S> restyle(int from, int to, S style) {
        if(from >= length()) {
            return this;
        } else {
            StyleSpans<S> left = styles.subView(0, from);
            StyleSpans<S> right = styles.subView(to, length());
            StyleSpans<S> updatedStyles = left.append(style, to - from).concat(right);
            return new Paragraph<>(paragraphStyle, segmentOps, segments, updatedStyles);
        }
    }

    public Paragraph<PS, SEG, S> restyle(int from, StyleSpans<? extends S> styleSpans) {
        int len = styleSpans.length();
        if(styleSpans.equals(getStyleSpans(from, from + len)) || styleSpans.getSpanCount() == 0) {
        	return this;
        }
        if(length() == 0) {
        	return new Paragraph<>(paragraphStyle, segmentOps, segments, (StyleSpans<S>) styleSpans);
        }

        StyleSpans<S> left = styles.subView(0, from);
        StyleSpans<S> right = styles.subView(from + len, length());

        // type issue with concat
        StyleSpans<S> castedSpans = (StyleSpans<S>) styleSpans;
        StyleSpans<S> updatedStyles = left.concat(castedSpans).concat(right);
        return new Paragraph<>(paragraphStyle, segmentOps, segments, updatedStyles);
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
        return new Paragraph<>(paragraphStyle, segmentOps, segments, styles);
    }

    /**
     * Returns the style of character with the given index.
     * If {@code charIdx < 0}, returns the style at the beginning of this paragraph.
     * If {@code charIdx >= this.length()}, returns the style at the end of this paragraph.
     */
    public S getStyleOfChar(int charIdx) {
        if(charIdx < 0) {
            return styles.getStyleSpan(0).getStyle();
        }

        Position pos = styles.offsetToPosition(charIdx, Forward);
        return styles.getStyleSpan(pos.getMajor()).getStyle();
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

        Position pos = styles.offsetToPosition(position, Backward);
        return styles.getStyleSpan(pos.getMajor()).getStyle();
    }

    /**
     * Returns the range of homogeneous style that includes the given position.
     * If {@code position} points to a boundary between two styled ranges,
     * then the range preceding {@code position} is returned.
     */
    public IndexRange getStyleRangeAtPosition(int position) {
        Position pos = styles.offsetToPosition(position, Backward);
        int start = position - pos.getMinor();
        int end = start + styles.getStyleSpan(pos.getMajor()).getLength();
        return new IndexRange(start, end);
    }

    public StyleSpans<S> getStyleSpans() {
        return styles;
    }

    public StyleSpans<S> getStyleSpans(int from, int to) {
        return styles.subView(from, to);
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
                getStyledSegments().stream().map(Object::toString)
                        .reduce((s1, s2) -> s1 + ", " + s2).orElse("") +
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
                && Objects.equals(this.segments, that.segments)
                && Objects.equals(this.styles, that.styles);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(paragraphStyle, segments, styles);
    }

    private List<StyledSegment<SEG, S>> createStyledSegments() {
        List<StyledSegment<SEG, S>> styledSegments = new LinkedList<>();
        Iterator<SEG> segIterator = segments.iterator();
        Iterator<StyleSpan<S>> styleIterator = styles.iterator();
        SEG segCurrent = segIterator.next();
        StyleSpan<S> styleCurrent = styleIterator.next();
        int segOffset = 0, styleOffset = 0;
        boolean finished = false;
        while (!finished) {
            int segLength = segmentOps.length(segCurrent) - segOffset;
            int styleLength = styleCurrent.getLength() - styleOffset;

            if (segLength < styleLength) {
                SEG splitSeg = segmentOps.subSequence(segCurrent, segOffset);
                styledSegments.add(new StyledSegment<>(splitSeg, styleCurrent.getStyle()));
                segCurrent = segIterator.next();
                segOffset = 0;
                styleOffset += segLength;
            } else if (styleLength < segLength) {
                SEG splitSeg = segmentOps.subSequence(segCurrent, segOffset, segOffset + styleLength);
                styledSegments.add(new StyledSegment<>(splitSeg, styleCurrent.getStyle()));
                styleCurrent = styleIterator.next();
                styleOffset = 0;
                segOffset += styleLength;
            } else {
                SEG splitSeg = segmentOps.subSequence(segCurrent, segOffset, segOffset + styleLength);
                styledSegments.add(new StyledSegment<>(splitSeg, styleCurrent.getStyle()));
                if (segIterator.hasNext() && styleIterator.hasNext()) {
                    segCurrent = segIterator.next();
                    segOffset = 0;
                    styleCurrent = styleIterator.next();
                    styleOffset = 0;
                } else {
                    finished = true;
                }
            }
        }
        return styledSegments;
    }
}
