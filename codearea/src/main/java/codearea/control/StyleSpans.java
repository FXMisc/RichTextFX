package codearea.control;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface StyleSpans<S> extends Iterable<StyleSpan<S>>, TwoDimensional {

    static <S> StyleSpans<S> singleton(S style, int length) {
        return singleton(new StyleSpan<>(style, length));
    }

    static <S> StyleSpans<S> singleton(StyleSpan<S> span) {
        return new SingletonSpans<>(span);
    }

    int length();
    int getSpanCount();
    StyleSpan<S> getStyleSpan(int index);

    @Override
    default Iterator<StyleSpan<S>> iterator() {
        return new Iterator<StyleSpan<S>>() {
            private int nextToReturn = 0;
            private final int spanCount = getSpanCount();

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

    default StyleSpans<S> append(S style, int length) {
        return append(new StyleSpan<>(style, length));
    }

    default StyleSpans<S> append(StyleSpan<S> span) {
        if(span.getLength() == 0) {
            return this;
        } else if(length() == 0) {
            return singleton(span);
        }

        int lastIdx = getSpanCount() - 1;
        StyleSpan<S> myLastSpan = getStyleSpan(lastIdx);
        if(Objects.equals(myLastSpan.getStyle(), span.getStyle())) {
            StyleSpan<S> newLastSpan = new StyleSpan<>(span.getStyle(), myLastSpan.getLength() + span.getLength());
            return new UpdatedSpans<>(this, lastIdx, newLastSpan);
        } else {
            return new AppendedSpans<>(this, span);
        }
    }

    default StyleSpans<S> prepend(S style, int length) {
        return prepend(new StyleSpan<>(style, length));
    }

    default StyleSpans<S> prepend(StyleSpan<S> span) {
        if(span.getLength() == 0) {
            return this;
        } else if(length() == 0) {
            return singleton(span);
        }

        StyleSpan<S> myFirstSpan = getStyleSpan(0);
        if(Objects.equals(span.getStyle(), myFirstSpan.getStyle())) {
            StyleSpan<S> newFirstSpan = new StyleSpan<>(span.getStyle(), span.getLength() + myFirstSpan.getLength());
            return new UpdatedSpans<>(this, 0, newFirstSpan);
        } else {
            return new PrependedSpans<>(this, span);
        }
    }

    default StyleSpans<S> subView(Position from, Position to) {
        return new SubSpans<>(this, from, to);
    }

    default StyleSpans<S> concat(StyleSpans<S> that) {
        if(that.length() == 0) {
            return this;
        } else if(this.length() == 0) {
            return that;
        }

        int n1 = this.getSpanCount();
        int n2 = that.getSpanCount();

        StyleSpan<S> myLast = this.getStyleSpan(n1 - 1);
        StyleSpan<S> theirFirst = that.getStyleSpan(0);

        StyleSpansBuilder<S> builder;
        if(Objects.equals(myLast.getStyle(), theirFirst.getStyle())) {
            builder = new StyleSpansBuilder<>(n1 + n2 - 1);
            for(int i = 0; i < n1 - 1; ++i) {
                builder.add(this.getStyleSpan(i));
            }
            builder.add(myLast.getStyle(), myLast.getLength() + theirFirst.getLength());
            for(int i = 1; i < n2; ++i) {
                builder.add(that.getStyleSpan(i));
            }
        } else {
            builder = new StyleSpansBuilder<>(n1 + n2);
            builder.addAll(this, n1);
            builder.addAll(that, n2);
        }

        return builder.create();
    }

    default StyleSpans<S> mapStyles(UnaryOperator<S> mapper) {
        StyleSpansBuilder<S> builder = new StyleSpansBuilder<>(getSpanCount());
        for(StyleSpan<S> span: this) {
            builder.add(mapper.apply(span.getStyle()), span.getLength());
        }
        return builder.create();
    }

    default Stream<S> styleStream() {
        return stream().map(span -> span.getStyle());
    }

    default Stream<StyleSpan<S>> stream() {
        Spliterator<StyleSpan<S>> spliterator = new Spliterator<StyleSpan<S>>() {
            private final Iterator<StyleSpan<S>> iterator = iterator();

            @Override
            public boolean tryAdvance(Consumer<? super StyleSpan<S>> action) {
                if(iterator.hasNext()) {
                    action.accept(iterator.next());
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public Spliterator<StyleSpan<S>> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return getSpanCount();
            }

            @Override
            public int characteristics() {
                return Spliterator.IMMUTABLE | Spliterator.SIZED;
            }
        };

        return StreamSupport.stream(spliterator, false);
    }
}
