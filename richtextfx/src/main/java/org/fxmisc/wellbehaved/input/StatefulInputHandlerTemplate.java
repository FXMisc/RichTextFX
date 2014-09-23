package org.fxmisc.wellbehaved.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.event.EventType;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

public final class StatefulInputHandlerTemplate<T extends InputReceiver, S> implements InputHandlerTemplate<T> {

    @FunctionalInterface
    public interface StateTransitioningHandler<T extends InputReceiver, S, E extends InputEvent> {
        S transition(T target, S state, E event);
    }

    public static abstract class Builder<T extends InputReceiver, S> {

        private static <T extends InputReceiver, S> Builder<T, S> initial(
                Supplier<? extends S> initialStateSupplier) {
            return new Builder<T, S>() {

                @Override
                public StatefulInputHandlerTemplate<T, S> create() {
                    throw new UnsupportedOperationException("Cannot create input handler template from an empty builder.");
                }

                @Override
                <U extends T> List<StateTransitioningHandler<? super U, S, InputEvent>> getHandlers(
                        int additionalCapacity) {
                    return new ArrayList<>(additionalCapacity);
                }

                @Override
                Supplier<? extends S> getInitialStateSupplier() {
                    return initialStateSupplier;
                }
            };
        }

        public <E extends InputEvent> On<T, S, E> on(EventPattern<? super InputEvent, E> eventMatcher) {
            return new On<>(this, eventMatcher);
        }

        public On<T, S, KeyEvent> on(KeyCombination combination) {
            return on(EventPattern.keyCombinationPattern(combination));
        }

        public On<T, S, KeyEvent> on(String character, KeyCombination.Modifier... modifiers) {
            return on(new KeyCharacterCombination(character, modifiers));
        }

        public On<T, S, KeyEvent> on(KeyCode code, KeyCombination.Modifier... modifiers) {
            return on(new KeyCodeCombination(code, modifiers));
        }

        public <E extends InputEvent> On<T, S, E> on(EventType<E> eventType) {
            return on(EventPattern.eventTypePattern(eventType));
        }

        public <U extends T> Builder<U, S> addHandler(StateTransitioningHandler<? super U, S, InputEvent> handler) {
            return new CompositeBuilder<>(this, handler);
        }

        public abstract StatefulInputHandlerTemplate<T, S> create();

        List<StateTransitioningHandler<? super T, S, InputEvent>> getHandlers() {
            return getHandlers(0);
        }

        abstract Supplier<? extends S> getInitialStateSupplier();
        abstract <U extends T> List<StateTransitioningHandler<? super U, S, InputEvent>> getHandlers(int additionalCapacity);
    }

    private static class CompositeBuilder<T extends InputReceiver, S> extends Builder<T, S> {
        private final Builder<? super T, S> previousBuilder;
        private final StateTransitioningHandler<? super T, S, InputEvent> handler;

        private CompositeBuilder(
                Builder<? super T, S> previousBuilder,
                StateTransitioningHandler<? super T, S, InputEvent> handler) {
            this.previousBuilder = previousBuilder;
            this.handler = handler;
        }

        @Override
        public StatefulInputHandlerTemplate<T, S> create() {
            return new StatefulInputHandlerTemplate<>(getInitialStateSupplier(), getHandlers());
        }

        @Override
        <U extends T> List<StateTransitioningHandler<? super U, S, InputEvent>> getHandlers(
                int additionalCapacity) {
            List<StateTransitioningHandler<? super U, S, InputEvent>> handlers = previousBuilder.getHandlers(additionalCapacity + 1);
            handlers.add(handler);
            return handlers;
        }

        @Override
        Supplier<? extends S> getInitialStateSupplier() {
            return previousBuilder.getInitialStateSupplier();
        }
    }

    public static class On<T extends InputReceiver, S, E extends InputEvent> {
        private final Builder<? super T, S> previousBuilder;
        private final EventPattern<? super InputEvent, E> eventMatcher;

        private On(Builder<? super T, S> previousBuilder, EventPattern<? super InputEvent, E> eventMatcher) {
            this.previousBuilder = previousBuilder;
            this.eventMatcher = eventMatcher;
        }

        public <U extends T> When<U, S, E> when(Predicate<? super U> condition) {
            return when((u, s) -> condition.test(u));
        }

        public <U extends T> When<U, S, E> when(BiPredicate<? super U, ? super S> condition) {
            return new When<>(previousBuilder, eventMatcher, condition);
        }

        public <U extends T> Builder<U, S> act(BiConsumer<? super U, ? super E> action) {
            return act((u, s, e) -> { action.accept(u, e); return s; });
        }

        public <U extends T> Builder<U, S> act(StateTransitioningHandler<? super U, S, ? super E> action) {
            return new CompositeBuilder<>(previousBuilder, (u, s, ie) -> {
                Optional<E> optE = eventMatcher.match(ie);
                if(optE.isPresent()) {
                    E e = optE.get();
                    S newState = action.transition(u, s, e);
                    e.consume();
                    return newState;
                } else {
                    return s;
                }
            });
        }
    }

    public static class When<T extends InputReceiver, S, E extends InputEvent> {
        private final Builder<? super T, S> previousBuilder;
        private final EventPattern<? super InputEvent, E> eventMatcher;
        private final BiPredicate<? super T, ? super S> condition;

        private When(
                Builder<? super T, S> previousBuilder,
                EventPattern<? super InputEvent, E> eventMatcher,
                BiPredicate<? super T, ? super S> condition) {
            this.previousBuilder = previousBuilder;
            this.eventMatcher = eventMatcher;
            this.condition = condition;
        }

        public <U extends T> Builder<U, S> act(BiConsumer<? super U, ? super E> action) {
            return act((u, s, e) -> { action.accept(u, e); return s; });
        }

        public <U extends T> Builder<U, S> act(StateTransitioningHandler<? super U, S, ? super E> action) {
            return new CompositeBuilder<>(previousBuilder, (u, s, ie) -> {
                Optional<E> optE = eventMatcher.match(ie);
                if(optE.isPresent() && condition.test(u, s)) {
                    E e = optE.get();
                    S newState = action.transition(u, s, e);
                    e.consume();
                    return newState;
                } else {
                    return s;
                }
            });
        }
    }

    public static <S> Builder<InputReceiver, S> initialStateSupplier(
            Supplier<? extends S> initialStateSupplier) {
        return Builder.initial(initialStateSupplier);
    }


    private final Supplier<? extends S> initialStateSupplier;
    private final List<StateTransitioningHandler<? super T, S, InputEvent>> handlers;

    StatefulInputHandlerTemplate(
            Supplier<? extends S> initialStateSupplier,
            List<StateTransitioningHandler<? super T, S, InputEvent>> handlers) {
        this.initialStateSupplier = initialStateSupplier;
        this.handlers = handlers;
    }

    @Override
    public BiConsumer<? super T, ? super InputEvent> getHandler() {
        return new BiConsumer<T, InputEvent>() {
            private S state = initialStateSupplier.get();

            @Override
            public void accept(T t, InputEvent e) {
                for(StateTransitioningHandler<? super T, S, InputEvent> handler: handlers) {
                    state = handler.transition(t, state, e);
                    if(e.isConsumed()) {
                        break;
                    }
                }
            }
        };
    }
}