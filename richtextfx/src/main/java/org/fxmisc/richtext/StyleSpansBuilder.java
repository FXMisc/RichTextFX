package org.fxmisc.richtext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;

public class StyleSpansBuilder<S> {

    private static class StyleSpansImpl<S> extends StyleSpansBase<S> {
        private final List<StyleSpan<S>> spans;
        private int length = -1;

        StyleSpansImpl(List<StyleSpan<S>> spans) {
            this.spans = spans;
        }

        @Override
        public Iterator<StyleSpan<S>> iterator() {
            return spans.iterator();
        }

        @Override
        public int length() {
            if(length == -1) {
                length = spans.stream().mapToInt(StyleSpan::getLength).sum();
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

    static <S> StyleSpans<S> overlay(
            StyleSpans<S> s1,
            StyleSpans<S> s2,
            BiFunction<? super S, ? super S, ? extends S> f) {

        StyleSpansBuilder<S> acc = new StyleSpansBuilder<>(s1.getSpanCount() + s2.getSpanCount());

        Iterator<StyleSpan<S>> t1 = s1.iterator();
        Iterator<StyleSpan<S>> t2 = s2.iterator();

        StyleSpan<S> h1 = t1.next(); // remember that all StyleSpans have at least one StyleSpan
        StyleSpan<S> h2 = t2.next(); // remember that all StyleSpans have at least one StyleSpan

        while(true) {
            int len1 = h1.getLength();
            int len2 = h2.getLength();
            if(len1 == len2) {
                acc.add(f.apply(h1.getStyle(), h2.getStyle()), len1);
                if(!t1.hasNext()) {
                    return acc.addAll(t2).create();
                } else if(!t2.hasNext()) {
                    return acc.addAll(t1).create();
                } else {
                    h1 = t1.next();
                    h2 = t2.next();
                }
            } else if(len1 < len2) {
                acc.add(f.apply(h1.getStyle(), h2.getStyle()), len1);
                h2 = new StyleSpan<>(h2.getStyle(), len2 - len1);
                if(t1.hasNext()) {
                    h1 = t1.next();
                } else {
                    return acc.add(h2).addAll(t2).create();
                }
            } else { // len1 > len2
                acc.add(f.apply(h1.getStyle(), h2.getStyle()), len2);
                h1 = new StyleSpan<>(h1.getStyle(), len1 - len2);
                if(t2.hasNext()) {
                    h2 = t2.next();
                } else {
                    return acc.add(h1).addAll(t1).create();
                }
            }
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
        _add(styleSpan);
        return this;
    }

    public StyleSpansBuilder<S> add(S style, int length) {
        return add(new StyleSpan<>(style, length));
    }

    public StyleSpansBuilder<S> addAll(Collection<? extends StyleSpan<S>> styleSpans) {
        return addAll(styleSpans, styleSpans.size());
    }

    public StyleSpansBuilder<S> addAll(Iterable<? extends StyleSpan<S>> styleSpans, int sizeHint) {
        spans.ensureCapacity(spans.size() + sizeHint);
        return addAll(styleSpans);
    }

    public StyleSpansBuilder<S> addAll(Iterable<? extends StyleSpan<S>> styleSpans) {
        ensureNotCreated();
        for(StyleSpan<S> span: styleSpans) {
            _add(span);
        }
        return this;
    }

    public StyleSpansBuilder<S> addAll(Iterator<? extends StyleSpan<S>> styleSpans) {
        ensureNotCreated();
        while(styleSpans.hasNext()) {
            _add(styleSpans.next());
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

    private void _add(StyleSpan<S> span) {
        if(spans.isEmpty()) {
            spans.add(span);
        } else if(span.getLength() > 0) {
            if(spans.size() == 1 && spans.get(0).getLength() == 0) {
                spans.set(0, span);
            } else {
                StyleSpan<S> prev = spans.get(spans.size() - 1);
                if(prev.getStyle().equals(span.getStyle())) {
                    spans.set(spans.size() - 1, new StyleSpan<>(span.getStyle(), prev.getLength() + span.getLength()));
                } else {
                    spans.add(span);
                }
            }
        } else {
            // do nothing, don't add a zero-length span
        }
    }

    private void ensureNotCreated() {
        if(created) {
            throw new IllegalStateException("Cannot reus StyleRangesBuilder after StyleRanges have been created.");
        }
    }
}


abstract class StyleSpansBase<S> implements StyleSpans<S> {
    protected final TwoLevelNavigator navigator = new TwoLevelNavigator(
            this::getSpanCount,
            i -> getStyleSpan(i).getLength());

    @Override
    public Position position(int major, int minor) {
        return navigator.position(major, minor);
    }

    @Override
    public Position offsetToPosition(int offset, Bias bias) {
        return navigator.offsetToPosition(offset, bias);
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof StyleSpans) {
            StyleSpans<?> that = (StyleSpans<?>) other;

            if(this.getSpanCount() != that.getSpanCount()) {
                return false;
            }

            for(int i = 0; i < this.getSpanCount(); ++i) {
                if(!this.getStyleSpan(i).equals(that.getStyleSpan(i))) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = 1;
        for(StyleSpan<S> span: this) {
            result = 31 * result + span.hashCode();
        }
        return result;
    }
}


class SubSpans<S> extends StyleSpansBase<S> {
    private final StyleSpans<S> original;
    private final int firstIdxInOrig;
    private final int spanCount;
    private final StyleSpan<S> firstSpan;
    private final StyleSpan<S> lastSpan;

    int length = -1;

    public SubSpans(StyleSpans<S> original, Position from, Position to) {
        this.original = original;
        this.firstIdxInOrig = from.getMajor();
        this.spanCount = to.getMajor() - from.getMajor() + 1;

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


class AppendedSpans<S> extends StyleSpansBase<S> {
    private final StyleSpans<S> original;
    private final StyleSpan<S> appended;

    private int length = -1;
    private int spanCount = -1;

    public AppendedSpans(StyleSpans<S> original, StyleSpan<S> appended) {
        this.original = original;
        this.appended = appended;
    }

    @Override
    public int length() {
        if(length == -1) {
            length = original.length() + appended.getLength();
        }
        return length;
    }

    @Override
    public int getSpanCount() {
        if(spanCount == -1) {
            spanCount = original.getSpanCount() + 1;
        }
        return spanCount;
    }

    @Override
    public StyleSpan<S> getStyleSpan(int index) {
        if(index == getSpanCount() - 1) {
            return appended;
        } else {
            return original.getStyleSpan(index);
        }
    }
}


class PrependedSpans<S> extends StyleSpansBase<S> {
    private final StyleSpans<S> original;
    private final StyleSpan<S> prepended;

    private int length = -1;
    private int spanCount = -1;

    public PrependedSpans(StyleSpans<S> original, StyleSpan<S> prepended) {
        this.original = original;
        this.prepended = prepended;
    }

    @Override
    public int length() {
        if(length == -1) {
            length = prepended.getLength() + original.length();
        }
        return length;
    }

    @Override
    public int getSpanCount() {
        if(spanCount == -1) {
            spanCount = 1 + original.getSpanCount();
        }
        return spanCount;
    }

    @Override
    public StyleSpan<S> getStyleSpan(int index) {
        if(index == 0) {
            return prepended;
        } else {
            return original.getStyleSpan(index - 1);
        }
    }
}


class UpdatedSpans<S> extends StyleSpansBase<S> {
    private final StyleSpans<S> original;
    private final int index;
    private final StyleSpan<S> update;

    private int length = -1;

    public UpdatedSpans(StyleSpans<S> original, int index, StyleSpan<S> update) {
        this.original = original;
        this.index = index;
        this.update = update;
    }

    @Override
    public int length() {
        if(length == -1) {
            length = original.length() - original.getStyleSpan(index).getLength() + update.getLength();
        }
        return length;
    }

    @Override
    public int getSpanCount() {
        return original.getSpanCount();
    }

    @Override
    public StyleSpan<S> getStyleSpan(int index) {
        if(index == this.index) {
            return update;
        } else {
            return original.getStyleSpan(index);
        }
    }
}

class SingletonSpans<S> extends StyleSpansBase<S> {
    private final StyleSpan<S> span;

    public SingletonSpans(StyleSpan<S> span) {
        this.span = span;
    }

    @Override
    public int length() {
        return span.getLength();
    }

    @Override
    public int getSpanCount() {
        return 1;
    }

    @Override
    public StyleSpan<S> getStyleSpan(int index) {
        if(index == 0) {
            return span;
        } else {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }
    }
}