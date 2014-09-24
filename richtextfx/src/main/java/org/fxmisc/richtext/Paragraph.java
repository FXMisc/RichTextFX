package org.fxmisc.richtext;

import static org.fxmisc.richtext.TwoDimensional.Bias.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.scene.control.IndexRange;

import org.fxmisc.richtext.TwoDimensional.Position;

public final class Paragraph<S> implements CharSequence {

    @SafeVarargs
    private static <T> List<T> list(T head, T... tail) {
        if(tail.length == 0) {
            return Collections.singletonList(head);
        } else {
            ArrayList<T> list = new ArrayList<>(1 + tail.length);
            list.add(head);
            for(T t: tail) list.add(t);
            return list;
        }
    }

    private final List<StyledText<S>> segments;
    private final Optional<LineTerminator> terminator;
    private final TwoLevelNavigator navigator;

    public Paragraph(String text, S style) {
        this(new StyledText<S>(text, style));
    }

    @SafeVarargs
    public Paragraph(StyledText<S> text, StyledText<S>... texts) {
        this(list(text, texts), Optional.empty());
    }

    private Paragraph(String text, S style, LineTerminator terminator) {
        this(text, style, Optional.of(terminator));
    }

    private Paragraph(String text, S style, Optional<LineTerminator> terminator) {
        this(new StyledText<>(text, style), terminator);
    }

    private Paragraph(StyledText<S> text, Optional<LineTerminator> terminator) {
        this(Arrays.asList(text), terminator);
    }

    private Paragraph(List<StyledText<S>> segments, LineTerminator terminator) {
        this(segments, Optional.of(terminator));
    }

    private Paragraph(List<StyledText<S>> segments, Optional<LineTerminator> terminator) {
        assert !segments.isEmpty();
        this.segments = segments;
        this.terminator = terminator;
        navigator = new TwoLevelNavigator(
                () -> segments.size(),
                i -> segments.get(i).length());
    }

    public List<StyledText<S>> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    public Optional<LineTerminator> getLineTerminator() {
        return terminator;
    }

    private int length = -1;
    @Override
    public int length() {
        if(length == -1) {
            length = segments.stream().mapToInt(s -> s.length()).sum();
        }
        return length;
    }

    private int fullLength = -1;
    public int fullLength() {
        if(fullLength == -1) {
            fullLength = length() + terminator.map(LineTerminator::length).orElse(0);
        }
        return fullLength;
    }

    public boolean isTerminated() {
        return terminator.isPresent();
    }

    public Paragraph<S> terminate(LineTerminator terminator) {
        if(isTerminated()) {
            throw new IllegalStateException("Paragraph alread terminated.");
        }

        return new Paragraph<>(segments, terminator);
    }

    @Override
    public char charAt(int index) {
        Position pos = navigator.offsetToPosition(index, Forward);
        return segments.get(pos.getMajor()).charAt(pos.getMinor());
    }

    public String substring(int from, int to) {
        return fullText().substring(from, Math.min(to, fullLength()));
    }

    public String substring(int from) {
        return fullText().substring(from);
    }

    public Paragraph<S> concat(Paragraph<S> p) {
        if(isTerminated()) {
            throw new IllegalStateException("Cannot append to a paragraph with line terminator.");
        }

        if(length() == 0) {
            return p;
        }

        if(p.fullLength() == 0) {
            return this;
        }

        StyledText<S> left = segments.get(segments.size()-1);
        StyledText<S> right = p.segments.get(0);
        if(Objects.equals(left.getStyle(), right.getStyle())) {
            StyledText<S> segment = left.concat(right);
            List<StyledText<S>> segs = new ArrayList<>(segments.size() + p.segments.size() - 1);
            segs.addAll(segments.subList(0, segments.size()-1));
            segs.add(segment);
            segs.addAll(p.segments.subList(1, p.segments.size()));
            return new Paragraph<S>(segs, p.terminator);
        } else {
            List<StyledText<S>> segs = new ArrayList<>(segments.size() + p.segments.size());
            segs.addAll(segments);
            segs.addAll(p.segments);
            return new Paragraph<S>(segs, p.terminator);
        }
    }

    public Paragraph<S> concat(CharSequence str) {
        if(isTerminated()) {
            throw new IllegalStateException("Cannot append to a paragraph with line terminator.");
        }

        if(str.length() == 0) {
            return this;
        }

        List<StyledText<S>> segs = new ArrayList<>(segments);
        int lastIdx = segments.size() - 1;
        segs.set(lastIdx, segments.get(lastIdx).concat(str));
        return new Paragraph<S>(segs, Optional.empty());
    }

    public Paragraph<S> insert(int offset, CharSequence str) {
        if(offset < 0 || offset > length())
            throw new IndexOutOfBoundsException(String.valueOf(offset));

        Position pos = navigator.offsetToPosition(offset, Backward);
        int segIdx = pos.getMajor();
        int segPos = pos.getMinor();
        StyledText<S> seg = segments.get(segIdx);
        StyledText<S> replacement = seg.spliced(segPos, segPos, str);
        List<StyledText<S>> segs = new ArrayList<>(segments);
        segs.set(segIdx, replacement);
        return new Paragraph<S>(segs, terminator);
    }

    @Override
    public Paragraph<S> subSequence(int start, int end) {
        return trim(end).subSequence(start);
    }

    public Paragraph<S> trim(int length) {
        if(length >= fullLength()) {
            return this;
        } else if(length > length()) { // slicing the line terminator
            LineTerminator newTerminator = terminator.get().trim(length - length());
            return new Paragraph<>(segments, newTerminator);
        } else if(length == length()) { // cut off the line terminator
            return new Paragraph<>(segments, Optional.empty());
        } else { // length < length()
            Position pos = navigator.offsetToPosition(length, Backward);
            int segIdx = pos.getMajor();
            List<StyledText<S>> segs = new ArrayList<>(segIdx + 1);
            segs.addAll(segments.subList(0, segIdx));
            segs.add(segments.get(segIdx).subSequence(0, pos.getMinor()));
            return new Paragraph<S>(segs, Optional.empty());
        }
    }

    public Paragraph<S> subSequence(int start) {
        if(start < 0) {
            throw new IllegalArgumentException("start must not be negative (was: " + start + ")");
        } else if(start == 0) {
            return this;
        } else if(start < length()) {
            Position pos = navigator.offsetToPosition(start, Forward);
            int segIdx = pos.getMajor();
            List<StyledText<S>> segs = new ArrayList<>(segments.size() - segIdx);
            segs.add(segments.get(segIdx).subSequence(pos.getMinor()));
            segs.addAll(segments.subList(segIdx + 1, segments.size()));
            return new Paragraph<S>(segs, terminator);
        } else if(start < fullLength()) {
            LineTerminator newTerminator = terminator.get().subSequence(start - length());
            S lastStyle = segments.get(segments.size() - 1).getStyle();
            return new Paragraph<>("", lastStyle, newTerminator);
        } else if(isTerminated()) {
            throw new IndexOutOfBoundsException(start + " not in [0, " + fullLength() + ")");
        } else if(start == fullLength()) { // == length()
            S lastStyle = segments.get(segments.size() - 1).getStyle();
            return new Paragraph<>("", lastStyle);
        } else {
            throw new IndexOutOfBoundsException(start + " not in [0, " + length() + "]");
        }
    }

    public Paragraph<S> delete(int start, int end) {
        return trim(start).concat(subSequence(end));
    }

    public Paragraph<S> restyle(S style) {
        return new Paragraph<S>(toString(), style, terminator);
    }

    public Paragraph<S> restyle(int from, int to, S style) {
        if(from >= length()) {
            return this;
        } else {
            to = Math.min(to, length());
            Paragraph<S> left = subSequence(0, from);
            Paragraph<S> middle = new Paragraph<S>(substring(from, to), style);
            Paragraph<S> right = subSequence(to);
            return left.concat(middle).concat(right);
        }
    }

    public Paragraph<S> restyle(int from, StyleSpans<? extends S> styleSpans) {
        int len = styleSpans.length();
        if(styleSpans.equals(getStyleSpans(from, from + len))) {
            return this;
        }

        Paragraph<S> left = trim(from);
        Paragraph<S> right = subSequence(from + len);

        String middleString = substring(from, from + len);
        List<StyledText<S>> middleSegs = new ArrayList<>(styleSpans.getSpanCount());
        int offset = 0;
        for(StyleSpan<? extends S> span: styleSpans) {
            int end = offset + span.getLength();
            String text = middleString.substring(offset, end);
            middleSegs.add(new StyledText<>(text, span.getStyle()));
            offset = end;
        }
        Paragraph<S> middle = new Paragraph<>(middleSegs, Optional.empty());

        return left.concat(middle).concat(right);
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
        for(StyledText<S> seg: segments) {
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
            StyledText<S> seg = segments.get(startSegIdx);
            builder.add(seg.getStyle(), to - from);
        } else {
            StyledText<S> startSeg = segments.get(startSegIdx);
            builder.add(startSeg.getStyle(), startSeg.length() - start.getMinor());

            for(int i = startSegIdx + 1; i < endSegIdx; ++i) {
                StyledText<S> seg = segments.get(i);
                builder.add(seg.getStyle(), seg.length());
            }

            StyledText<S> endSeg = segments.get(endSegIdx);
            builder.add(endSeg.getStyle(), end.getMinor());
        }

        return builder.create();
    }

    private String text = null;
    /**
     * Returns the string content of this paragraph,
     * excluding the line terminator.
     */
    @Override
    public String toString() {
        if(text == null) {
            StringBuilder sb = new StringBuilder(length());
            for(StyledText<S> seg: segments)
                sb.append(seg);
            text = sb.toString();
        }
        return text;
    }

    private String fullText = null;
    /**
     * Returns the string content of this paragraph,
     * including the line terminator.
     */
    public String fullText() {
        if(fullText == null) {
            if(isTerminated()) {
                fullText = toString() + terminator.get().asString();
            } else {
                fullText = toString();
            }
        }
        return fullText;
    }
}
