package org.fxmisc.wellbehaved.input;

import static javafx.scene.input.KeyEvent.*;

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
    public interface StateTransition<T extends InputReceiver, S, E extends InputEvent> {
        S transition(T target, S state, E event);
    }

    /**
     * An instance of this interface is expected to <em>consume</em> the event
     * if it successfully handled the event. If the event was not handled by an
     * instance of this interface, the event should be left unconsumed and the
     * returned state should be unchanged.
     */
    @FunctionalInterface
    public interface StateTransitioningHandler<T extends InputReceiver, S> {
        S handle(T target, S state, InputEvent event);

        default <U extends T> StateTransitioningHandler<U, S> orElse(StateTransitioningHandler<? super U, S> nextHandler) {
            return (u, s, e) -> {
                S newState = StateTransitioningHandler.this.handle(u, s, e);
                if(e.isConsumed()) {
                    return newState;
                } else {
                    return nextHandler.handle(u, s, e);
                }
            };
        }

        default <U extends T> StateTransitioningHandler<U, S> onlyWhen(BiPredicate<? super U, ? super S> condition) {
            return (u, s, e) -> {
                return condition.test(u, s)
                        ? StateTransitioningHandler.this.handle(u, s, e)
                        : s;
            };
        }

        default StatefulInputHandlerTemplate<T, S> initialStateSupplier(Supplier<? extends S> initialStateSupplier) {
            return new StatefulInputHandlerTemplate<>(this, initialStateSupplier);
        }
    }

    public static abstract class Builder<T extends InputReceiver, S> {

        private static <S> Builder<InputReceiver, S> empty() {
            return new Builder<InputReceiver, S>() {
                @Override
                <U extends InputReceiver> List<StateTransitioningHandler<? super U, S>> getHandlers(
                        int additionalCapacity) {
                    return new ArrayList<>(additionalCapacity);
                }
            };
        }

        // private constructor to prevent subclassing by the user
        private Builder() {}

        public <E extends InputEvent> On<T, S, E> on(EventPattern<? super InputEvent, E> eventMatcher) {
            return new On<>(this, eventMatcher);
        }

        public <E extends InputEvent> On<T, S, E> on(EventType<E> eventType) {
            return on(EventPattern.eventTypePattern(eventType));
        }

        public On<T, S, KeyEvent> onPressed(KeyCombination combination) {
            return on(KEY_PRESSED).where(combination::match);
        }

        public On<T, S, KeyEvent> onPressed(KeyCode code, KeyCombination.Modifier... modifiers) {
            return onPressed(new KeyCodeCombination(code, modifiers));
        }

        public On<T, S, KeyEvent> onPressed(String character, KeyCombination.Modifier... modifiers) {
            return onPressed(new KeyCharacterCombination(character, modifiers));
        }

        public On<T, S, KeyEvent> onReleased(KeyCombination combination) {
            return on(KEY_RELEASED).where(combination::match);
        }

        public On<T, S, KeyEvent> onReleased(KeyCode code, KeyCombination.Modifier... modifiers) {
            return onReleased(new KeyCodeCombination(code, modifiers));
        }

        public On<T, S, KeyEvent> onReleased(String character, KeyCombination.Modifier... modifiers) {
            return onReleased(new KeyCharacterCombination(character, modifiers));
        }

        public On<T, S, KeyEvent> onTyped(String character, KeyCombination.Modifier... modifiers) {
            KeyTypedCombination combination = new KeyTypedCombination(character, modifiers);
            return on(KEY_TYPED).where(combination::match);
        }

        public <U extends T> Builder<U, S> addHandler(StateTransitioningHandler<? super U, S> handler) {
            return new CompositeBuilder<>(this, handler);
        }

        public StateTransitioningHandler<T, S> createHandler() {
            return (t, s, e) -> {
                S newState = s;
                for(StateTransitioningHandler<? super T, S> handler: getHandlers()) {
                    newState = handler.handle(t, newState, e);
                    if(e.isConsumed()) {
                        break;
                    }
                }
                return newState;
            };
        }

        public StatefulInputHandlerTemplate<T, S> initialStateSupplier(Supplier<? extends S> initialStateSupplier) {
            return createHandler().initialStateSupplier(initialStateSupplier);
        }

        List<StateTransitioningHandler<? super T, S>> getHandlers() {
            return getHandlers(0);
        }

        abstract <U extends T> List<StateTransitioningHandler<? super U, S>> getHandlers(int additionalCapacity);
    }

    private static class CompositeBuilder<T extends InputReceiver, S> extends Builder<T, S> {
        private final Builder<? super T, S> previousBuilder;
        private final StateTransitioningHandler<? super T, S> handler;

        private CompositeBuilder(
                Builder<? super T, S> previousBuilder,
                StateTransitioningHandler<? super T, S> handler) {
            this.previousBuilder = previousBuilder;
            this.handler = handler;
        }

        @Override
        <U extends T> List<StateTransitioningHandler<? super U, S>> getHandlers(
                int additionalCapacity) {
            List<StateTransitioningHandler<? super U, S>> handlers = previousBuilder.getHandlers(additionalCapacity + 1);
            handlers.add(handler);
            return handlers;
        }
    }

    public static class On<T extends InputReceiver, S, E extends InputEvent> {
        private final Builder<? super T, S> previousBuilder;
        private final EventPattern<? super InputEvent, E> eventMatcher;

        private On(Builder<? super T, S> previousBuilder, EventPattern<? super InputEvent, E> eventMatcher) {
            this.previousBuilder = previousBuilder;
            this.eventMatcher = eventMatcher;
        }

        public On<T, S, E> where(Predicate<? super E> condition) {
            return new On<>(previousBuilder, eventMatcher.and(condition));
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

        public <U extends T> Builder<U, S> act(StateTransition<? super U, S, ? super E> action) {
            return previousBuilder.addHandler((u, s, ie) -> {
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

        public <U extends T> Builder<U, S> act(StateTransition<? super U, S, ? super E> action) {
            return previousBuilder.addHandler((u, s, ie) -> {
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

    public static <E extends InputEvent, S> On<InputReceiver, S, E> on(
            EventPattern<? super InputEvent, E> eventMatcher) {
        return Builder.<S>empty().on(eventMatcher);
    }

    public static <E extends InputEvent, S> On<InputReceiver, S, E> on(EventType<E> eventType) {
        return Builder.<S>empty().on(eventType);
    }

    public static <S> On<InputReceiver, S, KeyEvent> onPressed(KeyCombination combination) {
        return Builder.<S>empty().onPressed(combination);
    }

    public static <S> On<InputReceiver, S, KeyEvent> onPressed(KeyCode code, KeyCombination.Modifier... modifiers) {
        return Builder.<S>empty().onPressed(code, modifiers);
    }

    public static <S> On<InputReceiver, S, KeyEvent> onPressed(String character, KeyCombination.Modifier... modifiers) {
        return Builder.<S>empty().onPressed(character, modifiers);
    }

    public static <S> On<InputReceiver, S, KeyEvent> onReleased(KeyCombination combination) {
        return Builder.<S>empty().onReleased(combination);
    }

    public static <S> On<InputReceiver, S, KeyEvent> onReleased(KeyCode code, KeyCombination.Modifier... modifiers) {
        return Builder.<S>empty().onReleased(code, modifiers);
    }

    public static <S> On<InputReceiver, S, KeyEvent> onReleased(String character, KeyCombination.Modifier... modifiers) {
        return Builder.<S>empty().onReleased(character, modifiers);
    }

    public static <S> On<InputReceiver, S, KeyEvent> onTyped(String character, KeyCombination.Modifier... modifiers) {
        return Builder.<S>empty().onTyped(character, modifiers);
    }

    public static <T extends InputReceiver, S> Builder<T, S>
    startWith(StateTransitioningHandler<? super T, S> handler) {
        return Builder.<S>empty().addHandler(handler);
    }


    private final Supplier<? extends S> initialStateSupplier;
    private final StateTransitioningHandler<? super T, S> handler;

    StatefulInputHandlerTemplate(
            StateTransitioningHandler<? super T, S> handler,
            Supplier<? extends S> initialStateSupplier) {
        this.initialStateSupplier = initialStateSupplier;
        this.handler = handler;
    }

    @Override
    public BiConsumer<? super T, ? super InputEvent> getHandler() {
        return new BiConsumer<T, InputEvent>() {
            private S state = initialStateSupplier.get();

            @Override
            public void accept(T t, InputEvent e) {
                state = handler.handle(t, state, e);
            }
        };
    }

    public <U extends T> StatefulInputHandlerTemplate<U, S> onlyWhen(BiPredicate<? super U, ? super S> condition) {
        return handler.<U>onlyWhen(condition).initialStateSupplier(initialStateSupplier);
    }

    public <U extends T> StatefulInputHandlerTemplate<U, S> addHandler(StateTransitioningHandler<? super U, S> nextHandler) {
        return handler.<U>orElse(nextHandler).initialStateSupplier(initialStateSupplier);
    }
}