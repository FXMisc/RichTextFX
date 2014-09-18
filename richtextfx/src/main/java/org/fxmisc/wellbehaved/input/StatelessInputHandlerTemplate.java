package org.fxmisc.wellbehaved.input;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

/**
 *
 * @param <T> type of the control the input handler applies to.
 */
public final class StatelessInputHandlerTemplate<T extends InputReceiver> implements InputHandlerTemplate<T>, BiConsumer<T, InputEvent> {

    public static abstract class Builder<T extends InputReceiver> {
        private static <T extends InputReceiver> Builder<T> empty() {
            return new Builder<T>() {
                @Override
                <U extends T> List<BiConsumer<? super U, ? super InputEvent>> getHandlers(int additionalCapacity) {
                    return new ArrayList<>(additionalCapacity);
                }

                @Override
                public StatelessInputHandlerTemplate<T> create() {
                    throw new UnsupportedOperationException("Cannot create input handler template from an empty builder.");
                }
            };
        }

        public <E extends InputEvent> On<T, E> on(EventPattern<? super InputEvent, E> eventMatcher) {
            return new On<>(this, eventMatcher);
        }

        public On<T, KeyEvent> on(KeyCombination combination) {
            return on(EventPattern.keyCombinationPattern(combination));
        }

        public <U extends T> Builder<U> addHandler(BiConsumer<? super U, ? super InputEvent> handler) {
            return new CompositeBuilder<>(this, handler);
        }

        public abstract StatelessInputHandlerTemplate<T> create();

        List<BiConsumer<? super T, ? super InputEvent>> getHandlers() {
            return getHandlers(0);
        }

        abstract <U extends T> List<BiConsumer<? super U, ? super InputEvent>> getHandlers(int additionalCapacity);
    }

    private static class CompositeBuilder<T extends InputReceiver> extends Builder<T> {
        private final Builder<? super T> previousBuilder;
        private final BiConsumer<? super T, ? super InputEvent> handler;

        private CompositeBuilder(
                Builder<? super T> previousBuilder,
                BiConsumer<? super T, ? super InputEvent> handler) {
            this.previousBuilder = previousBuilder;
            this.handler = handler;
        }

        @Override
        <U extends T> List<BiConsumer<? super U, ? super InputEvent>> getHandlers(int additionalCapacity) {
            List<BiConsumer<? super U, ? super InputEvent>> handlers = previousBuilder.getHandlers(additionalCapacity + 1);
            handlers.add(handler);
            return handlers;
        }

        @Override
        public StatelessInputHandlerTemplate<T> create() {
            return new StatelessInputHandlerTemplate<T>(getHandlers());
        }
    }

    public static class On<T extends InputReceiver, E extends InputEvent> {
        private final Builder<? super T> previousBuilder;
        private final EventPattern<? super InputEvent, E> eventMatcher;

        private On(Builder<? super T> previous, EventPattern<? super InputEvent, E> eventMatcher) {
            this.previousBuilder = previous;
            this.eventMatcher = eventMatcher;
        }

        public <U extends T> When<U, E> when(Predicate<? super U> condition) {
            return new When<U, E>(previousBuilder, eventMatcher, condition);
        }

        public <U extends T> Builder<U> act(BiConsumer<? super U, ? super E> action) {
            return new CompositeBuilder<>(previousBuilder, (t, ie) -> {
                eventMatcher.match(ie).ifPresent(e -> {
                    action.accept(t, e);
                    e.consume();
                });
            });
        }
    }

    public static class When<T extends InputReceiver, E extends InputEvent> {
        private final Builder<? super T> previousBuilder;
        private final EventPattern<? super InputEvent, E> eventMatcher;
        private final Predicate<? super T> condition;

        private When(
                Builder<? super T> previousBuilder,
                EventPattern<? super InputEvent, E> eventMatcher,
                Predicate<? super T> condition) {
            this.previousBuilder = previousBuilder;
            this.eventMatcher = eventMatcher;
            this.condition = condition;
        }

        public <U extends T> Builder<U> act(BiConsumer<? super U, ? super E> action) {
            return new CompositeBuilder<>(previousBuilder, (u, ie) -> {
                eventMatcher.match(ie).ifPresent(e -> {
                    if(condition.test(u)) {
                        action.accept(u, e);
                        e.consume();
                    }
                });
            });
        }
    }

    public static <E extends InputEvent> On<InputReceiver, E> on(
            EventPattern<? super InputEvent, E> eventMatcher) {
        return Builder.empty().on(eventMatcher);
    }

    public static On<InputReceiver, KeyEvent> on(KeyCombination combination) {
        return Builder.empty().on(combination);
    }

    public static <T extends InputReceiver> Builder<T>
    startWith(BiConsumer<? super T, ? super InputEvent> handler) {
        return Builder.empty().addHandler(handler);
    }


    private final List<BiConsumer<? super T, ? super InputEvent>> handlers;

    private StatelessInputHandlerTemplate(List<BiConsumer<? super T, ? super InputEvent>> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void accept(T target, InputEvent event) {
        for(BiConsumer<? super T, ? super InputEvent> handler: handlers) {
            handler.accept(target, event);
            if(event.isConsumed()) {
                break;
            }
        }
    }

    @Override
    public BiConsumer<? super T, ? super InputEvent> getHandler() {
        return this;
    }
}