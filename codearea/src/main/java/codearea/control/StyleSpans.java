package codearea.control;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface StyleSpans<S> extends Iterable<StyleSpan<S>>, TwoDimensional {
    int length();
    int getSpanCount();
    StyleSpan<S> getStyleSpan(int index);
    StyleSpans<S> subView(Position from, Position to);

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
