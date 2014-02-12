package codearea.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class StyleSpansBuilder<S> {

    private static class StyleSpansImpl<S> implements StyleSpans<S> {
        private final List<StyleSpan<S>> spans;
        private final TwoLevelNavigator navigator;
        private int length = -1;

        StyleSpansImpl(List<StyleSpan<S>> spans) {
            this.spans = spans;
            this.navigator = new TwoLevelNavigator(
                    () -> spans.size(),
                    i -> spans.get(i).getLength());
        }

        @Override
        public Iterator<StyleSpan<S>> iterator() {
            return spans.iterator();
        }

        @Override
        public Position position(int major, int minor) {
            return navigator.position(major, minor);
        }

        @Override
        public Position offsetToPosition(int offset, Bias bias) {
            return navigator.offsetToPosition(offset, bias);
        }

        @Override
        public int length() {
            if(length == -1) {
                length = spans.stream().mapToInt(span -> span.getLength()).sum();
            }

            return length;
        }

        @Override
        public int getSpanCount() {
            return spans.size();
        }

        @Override
        public StyleSpan<S> getStyleSpan(int index) {
            return spans.get(index);
        }
    }


    private boolean created = false;
    private final ArrayList<StyleSpan<S>> spans;

    public StyleSpansBuilder(int initialCapacity) {
        this.spans = new ArrayList<>(initialCapacity);
    }

    public StyleSpansBuilder() {
        this.spans = new ArrayList<>();
    }

    public StyleSpansBuilder<S> add(StyleSpan<S> styleSpan) {
        ensureNotCreated();
        spans.add(styleSpan);
        return this;
    }

    public StyleSpansBuilder<S> add(S style, int length) {
        return add(new StyleSpan<>(style, length));
    }

    public StyleSpansBuilder<S> addAll(Collection<? extends StyleSpan<S>> styleSpans) {
        ensureNotCreated();
        spans.addAll(styleSpans);
        return this;
    }

    public StyleSpansBuilder<S> addAll(Iterable<? extends StyleSpan<S>> styleSpans, int sizeHint) {
        ensureNotCreated();
        spans.ensureCapacity(spans.size() + sizeHint);
        for(StyleSpan<S> span: styleSpans) {
            spans.add(span);
        }
        return this;
    }

    public StyleSpansBuilder<S> addAll(Iterable<? extends StyleSpan<S>> styleSpans) {
        ensureNotCreated();
        for(StyleSpan<S> span: styleSpans) {
            spans.add(span);
        }
        return this;
    }

    public StyleSpans<S> create() {
        ensureNotCreated();
        if(spans.isEmpty()) {
            throw new IllegalStateException("No spans have been added");
        }

        created = true;
        return new StyleSpansImpl<>(Collections.unmodifiableList(spans));
    }

    private void ensureNotCreated() {
        if(created) {
            throw new IllegalStateException("Cannot reus StyleRangesBuilder after StyleRanges have been created.");
        }
    }
}


class SubSpans<S> implements StyleSpans<S> {
    private final StyleSpans<S> original;
    private final int firstIdxInOrig;
    private final int spanCount;
    private final StyleSpan<S> firstSpan;
    private final StyleSpan<S> lastSpan;
    private final TwoLevelNavigator navigator;

    int length = -1;

    public SubSpans(StyleSpans<S> original, Position from, Position to) {
        this.original = original;
        this.firstIdxInOrig = from.getMajor();
        this.spanCount = to.getMajor() - from.getMajor() + 1;
        this.navigator = new TwoLevelNavigator(
                () -> spanCount,
                i -> getStyleSpan(i).getLength());

        if(spanCount == 1) {
            StyleSpan<S> span = original.getStyleSpan(firstIdxInOrig);
            int len = to.getMinor() - from.getMinor();
            firstSpan = lastSpan = new StyleSpan<>(span.getStyle(), len);
        } else {
            StyleSpan<S> startSpan = original.getStyleSpan(firstIdxInOrig);
            int len = startSpan.getLength() - from.getMinor();
            firstSpan = new StyleSpan<>(startSpan.getStyle(), len);

            StyleSpan<S> endSpan = original.getStyleSpan(to.getMajor());
            lastSpan = new StyleSpan<>(endSpan.getStyle(), to.getMinor());
        }
    }

    @Override
    public Iterator<StyleSpan<S>> iterator() {
        return new Iterator<StyleSpan<S>>() {
            private int nextToReturn = 0;

            @Override
            public boolean hasNext() {
                return nextToReturn < spanCount;
            }

            @Override
            public StyleSpan<S> next() {
                return getStyleSpan(nextToReturn++);
            }
        };
    }

    @Override
    public Position position(int major, int minor) {
        return navigator.position(major, minor);
    }

    @Override
    public Position offsetToPosition(int offset, Bias bias) {
        return navigator.offsetToPosition(offset, bias);
    }

    @Override
    public int length() {
        if(length == -1) {
            length = 0;
            for(StyleSpan<S> span: this) {
                length += span.getLength();
            }
        }

        return length;
    }

    @Override
    public int getSpanCount() {
        return spanCount;
    }

    @Override
    public StyleSpan<S> getStyleSpan(int index) {
        if(index == 0) {
            return firstSpan;
        } else if(index == spanCount - 1) {
            return lastSpan;
        } else if(index < 0 || index >= spanCount) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        } else {
            return original.getStyleSpan(firstIdxInOrig + index);
        }
    }
}