package org.fxmisc.wellbehaved.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

/**
 *
 * @param <T> type of the control the input handler applies to.
 */
public abstract class InputHandlerTemplate<T extends InputReceiver> {

    static abstract class Instance<T extends InputReceiver> extends NonEmptyInputHandler {
        private final T target;

        Instance(T target) {
            this.target = target;
        }

        protected abstract Object getTemplate();

        @Override
        T getTarget() {
            return target;
        }

        @Override
        InputHandler without(InputHandler subHandler) {
            return this.isEqualTo(subHandler) ? EMPTY : this;
        }

        @Override
        boolean isEqualTo(InputHandler other) {
            if(other instanceof InputHandlerTemplate.Instance) {
                Instance<?> that = (Instance<?>) other;
                return this.target == that.target
                        && this.getTemplate() == that.getTemplate();
            } else {
                return false;
            }
        }
    }

    public static abstract class StatelessBuilder<T extends InputReceiver> {
        private static <T extends InputReceiver> StatelessBuilder<T> empty() {
            return new StatelessBuilder<T>() {
                @Override
                <U extends T> List<BiConsumer<? super U, InputEvent>> getHandlers(int additionalCapacity) {
                    return new ArrayList<>(additionalCapacity);
                }

                @Override
                public InputHandlerTemplate<T> create() {
                    throw new UnsupportedOperationException("Cannot create input handler template from an empty builder.");
                }
            };
        }

        public <E extends InputEvent> StatelessOn<T, E> on(EventPattern<? super InputEvent, E> eventMatcher) {
            return new StatelessOn<>(this, eventMatcher);
        }

        public StatelessOn<T, KeyEvent> on(KeyCombination combination) {
            return on(EventPattern.keyCombinationPattern(combination));
        }

        public <S> StatefulBuilder<T, S> initialStateSupplier(Supplier<? extends S> initialStateSupplier) {
            InputHandlerTemplate<T> template = create();
            return StatefulBuilder.initial(template, initialStateSupplier);
        }

        public abstract InputHandlerTemplate<T> create();

        List<BiConsumer<? super T, InputEvent>> getHandlers() {
            return getHandlers(0);
        }

        abstract <U extends T> List<BiConsumer<? super U, InputEvent>> getHandlers(int additionalCapacity);
    }

    private static class StatelessCompositeBuilder<T extends InputReceiver> extends StatelessBuilder<T> {
        private final StatelessBuilder<? super T> previousBuilder;
        private final BiConsumer<? super T, InputEvent> handler;

        private StatelessCompositeBuilder(
                StatelessBuilder<? super T> previousBuilder,
                BiConsumer<? super T, InputEvent> handler) {
            this.previousBuilder = previousBuilder;
            this.handler = handler;
        }

        @Override
        <U extends T> List<BiConsumer<? super U, InputEvent>> getHandlers(int additionalCapacity) {
            List<BiConsumer<? super U, InputEvent>> handlers = previousBuilder.getHandlers(additionalCapacity + 1);
            handlers.add(handler);
            return handlers;
        }

        @Override
        public InputHandlerTemplate<T> create() {
            return new StatelessTemplate<T>(getHandlers());
        }
    }

    public static class StatelessOn<T extends InputReceiver, E extends InputEvent> {
        private final StatelessBuilder<? super T> previousBuilder;
        private final EventPattern<? super InputEvent, E> eventMatcher;

        private StatelessOn(StatelessBuilder<? super T> previous, EventPattern<? super InputEvent, E> eventMatcher) {
            this.previousBuilder = previous;
            this.eventMatcher = eventMatcher;
        }

        public <U extends T> StatelessWhen<U, E> when(Predicate<? super U> condition) {
            return new StatelessWhen<U, E>(previousBuilder, eventMatcher, condition);
        }

        public <U extends T> StatelessBuilder<U> act(BiConsumer<? super U, ? super E> action) {
            return new StatelessCompositeBuilder<>(previousBuilder, (t, ie) -> {
                eventMatcher.match(ie).ifPresent(e -> {
                    action.accept(t, e);
                    e.consume();
                });
            });
        }
    }

    public static class StatelessWhen<T extends InputReceiver, E extends InputEvent> {
        private final StatelessBuilder<? super T> previousBuilder;
        private final EventPattern<? super InputEvent, E> eventMatcher;
        private final Predicate<? super T> condition;

        private StatelessWhen(
                StatelessBuilder<? super T> previousBuilder,
                EventPattern<? super InputEvent, E> eventMatcher,
                Predicate<? super T> condition) {
            this.previousBuilder = previousBuilder;
            this.eventMatcher = eventMatcher;
            this.condition = condition;
        }

        public <U extends T> StatelessBuilder<U> act(BiConsumer<? super U, ? super E> action) {
            return new StatelessCompositeBuilder<>(previousBuilder, (u, ie) -> {
                eventMatcher.match(ie).ifPresent(e -> {
                    if(condition.test(u)) {
                        action.accept(u, e);
                        e.consume();
                    }
                });
            });
        }
    }

    public static abstract class StatefulBuilder<T extends InputReceiver, S> {
        private static <T extends InputReceiver, S> StatefulBuilder<T, S> initial(
                Supplier<? extends S> initialStateSupplier) {
            return initial(Optional.empty(), initialStateSupplier);
        }

        private static <T extends InputReceiver, S> StatefulBuilder<T, S> initial(
                InputHandlerTemplate<? super T> previousTemplate,
                Supplier<? extends S> initialStateSupplier) {
            return initial(Optional.of(previousTemplate), initialStateSupplier);
        }

        private static <T extends InputReceiver, S> StatefulBuilder<T, S> initial(
                Optional<InputHandlerTemplate<? super T>> previousTemplate,
                Supplier<? extends S> initialStateSupplier) {
            return new StatefulBuilder<T, S>() {

                @Override
                StatefulTemplate<T, S> createCurrent() {
                    throw new UnsupportedOperationException("Cannot create input handler template from an empty builder.");
                }

                @Override
                <U extends T> List<TriFunction<? super U, ? super S, InputEvent, ? extends S>> getHandlers(
                        int additionalCapacity) {
                    return new ArrayList<>(additionalCapacity);
                }

                @Override
                Supplier<? extends S> getInitialStateSupplier() {
                    return initialStateSupplier;
                }

                @Override
                Optional<InputHandlerTemplate<? super T>> getPreviousTemplate() {
                    return previousTemplate;
                }
            };
        }

        public <E extends InputEvent> StatefulOn<T, S, E> on(EventPattern<? super InputEvent, E> eventMatcher) {
            return new StatefulOn<>(this, eventMatcher);
        }

        public StatefulOn<T, S, KeyEvent> on(KeyCombination combination) {
            return on(EventPattern.keyCombinationPattern(combination));
        }

        public <Q> StatefulBuilder<T, Q> initialStateSupplier(Supplier<? extends Q> initialStateSupplier) {
            return initial(createCurrent(), initialStateSupplier);
        }

        public final InputHandlerTemplate<T> create() {
            InputHandlerTemplate<T> currentTemplate = createCurrent();
            return getPreviousTemplate()
                    .map(prev -> prev.orElse(currentTemplate))
                    .orElse(currentTemplate);
        }

        List<TriFunction<? super T, ? super S, InputEvent, ? extends S>> getHandlers() {
            return getHandlers(0);
        }

        abstract Optional<? extends InputHandlerTemplate<? super T>> getPreviousTemplate();
        abstract Supplier<? extends S> getInitialStateSupplier();
        abstract <U extends T> List<TriFunction<? super U, ? super S, InputEvent, ? extends S>> getHandlers(int additionalCapacity);
        abstract InputHandlerTemplate<T> createCurrent();
    }

    private static class StatefulCompositeBuilder<T extends InputReceiver, S> extends StatefulBuilder<T, S> {
        private final StatefulBuilder<? super T, S> previousBuilder;
        private final TriFunction<? super T, ? super S, InputEvent, ? extends S> handler;

        private StatefulCompositeBuilder(
                StatefulBuilder<? super T, S> previousBuilder,
                TriFunction<? super T, ? super S, InputEvent, ? extends S> handler) {
            this.previousBuilder = previousBuilder;
            this.handler = handler;
        }

        @Override
        InputHandlerTemplate<T> createCurrent() {
            return new StatefulTemplate<>(getInitialStateSupplier(), getHandlers());
        }

        @Override
        <U extends T> List<TriFunction<? super U, ? super S, InputEvent, ? extends S>> getHandlers(
                int additionalCapacity) {
            List<TriFunction<? super U, ? super S, InputEvent, ? extends S>> handlers = previousBuilder.getHandlers(additionalCapacity + 1);
            handlers.add(handler);
            return handlers;
        }

        @Override
        Supplier<? extends S> getInitialStateSupplier() {
            return previousBuilder.getInitialStateSupplier();
        }

        @Override
        Optional<? extends InputHandlerTemplate<? super T>> getPreviousTemplate() {
            return previousBuilder.getPreviousTemplate();
        }
    }

    public static class StatefulOn<T extends InputReceiver, S, E extends InputEvent> {
        private final StatefulBuilder<? super T, S> previousBuilder;
        private final EventPattern<? super InputEvent, E> eventMatcher;

        private StatefulOn(StatefulBuilder<? super T, S> previousBuilder, EventPattern<? super InputEvent, E> eventMatcher) {
            this.previousBuilder = previousBuilder;
            this.eventMatcher = eventMatcher;
        }

        public <U extends T> StatefulWhen<U, S, E> when(Predicate<? super U> condition) {
            return when((u, s) -> condition.test(u));
        }

        public <U extends T> StatefulWhen<U, S, E> when(BiPredicate<? super U, ? super S> condition) {
            return new StatefulWhen<>(previousBuilder, eventMatcher, condition);
        }

        public <U extends T> StatefulBuilder<U, S> act(BiConsumer<? super U, ? super E> action) {
            return act((u, s, e) -> { action.accept(u, e); return s; });
        }

        public <U extends T> StatefulBuilder<U, S> act(TriFunction<? super U, ? super S, ? super E, ? extends S> action) {
            return new StatefulCompositeBuilder<>(previousBuilder, (u, s, ie) -> {
                Optional<E> optE = eventMatcher.match(ie);
                if(optE.isPresent()) {
                    E e = optE.get();
                    S newState = action.apply(u, s, e);
                    e.consume();
                    return newState;
                } else {
                    return s;
                }
            });
        }
    }

    public static class StatefulWhen<T extends InputReceiver, S, E extends InputEvent> {
        private final StatefulBuilder<? super T, S> previousBuilder;
        private final EventPattern<? super InputEvent, E> eventMatcher;
        private final BiPredicate<? super T, ? super S> condition;

        private StatefulWhen(
                StatefulBuilder<? super T, S> previousBuilder,
                EventPattern<? super InputEvent, E> eventMatcher,
                BiPredicate<? super T, ? super S> condition) {
            this.previousBuilder = previousBuilder;
            this.eventMatcher = eventMatcher;
            this.condition = condition;
        }

        public <U extends T> StatefulBuilder<U, S> act(BiConsumer<? super U, ? super E> action) {
            return act((u, s, e) -> { action.accept(u, e); return s; });
        }

        public <U extends T> StatefulBuilder<U, S> act(TriFunction<? super U, ? super S, ? super E, ? extends S> action) {
            return new StatefulCompositeBuilder<>(previousBuilder, (u, s, ie) -> {
                Optional<E> optE = eventMatcher.match(ie);
                if(optE.isPresent() && condition.test(u, s)) {
                    E e = optE.get();
                    S newState = action.apply(u, s, e);
                    e.consume();
                    return newState;
                } else {
                    return s;
                }
            });
        }
    }

    public static <E extends InputEvent> StatelessOn<InputReceiver, E> on(
            EventPattern<? super InputEvent, E> eventMatcher) {
        return StatelessBuilder.empty().on(eventMatcher);
    }

    public static StatelessOn<InputReceiver, KeyEvent> on(KeyCombination combination) {
        return StatelessBuilder.empty().on(combination);
    }

    public static <S> StatefulBuilder<InputReceiver, ? extends S> initialStateSupplier(
            Supplier<? extends S> initialStateSupplier) {
        return StatefulBuilder.initial(initialStateSupplier);
    }

    // package private constructor to prevent subclassing by the user
    InputHandlerTemplate() {}

    abstract InputHandler instantiateFor(T target);

    public final InputHandler installTo(T target) {
        InputHandler handler = instantiateFor(target);
        InputHandler oldHandler = target.getOnInput();
        target.setOnInput(handler.orElse(oldHandler));
        return handler;
    }

    public <U extends T> InputHandlerTemplate<U> orElse(InputHandlerTemplate<U> other) {
        return new CompositeTemplate<>(this, other);
    }
}


class StatelessTemplate<T extends InputReceiver> extends InputHandlerTemplate<T> {

    private class Instance extends InputHandlerTemplate.Instance<T> {

        private Instance(T target) {
            super(target);
        }

        @Override
        protected Object getTemplate() {
            return StatelessTemplate.this;
        }

        @Override
        public void handle(InputEvent event) {
            for(BiConsumer<? super T, InputEvent> handler: handlers) {
                handler.accept(getTarget(), event);
                if(event.isConsumed()) {
                    break;
                }
            }
        }
    }

    private final List<BiConsumer<? super T, InputEvent>> handlers;

    StatelessTemplate(List<BiConsumer<? super T, InputEvent>> handlers) {
        this.handlers = handlers;
    }

    @Override
    InputHandler instantiateFor(T target) {
        return new Instance(target);
    }
}

class StatefulTemplate<T extends InputReceiver, S> extends InputHandlerTemplate<T> {

    private class Instance extends InputHandlerTemplate.Instance<T> {
        private S state;

        private Instance(T target, S initialState) {
            super(target);
            this.state = initialState;
        }

        @Override
        protected Object getTemplate() {
            return StatefulTemplate.this;
        }

        @Override
        public void handle(InputEvent event) {
            for(TriFunction<? super T, ? super S, InputEvent, ? extends S> handler: handlers) {
                state = handler.apply(getTarget(), state, event);
                if(event.isConsumed()) {
                    break;
                }
            }
        }
    }

    private final Supplier<? extends S> initialStateSupplier;
    private final List<TriFunction<? super T, ? super S, InputEvent, ? extends S>> handlers;

    StatefulTemplate(
            Supplier<? extends S> initialStateSupplier,
            List<TriFunction<? super T, ? super S, InputEvent, ? extends S>> handlers) {
        this.initialStateSupplier = initialStateSupplier;
        this.handlers = handlers;
    }

    @Override
    InputHandler instantiateFor(T target) {
        return new Instance(target, initialStateSupplier.get());
    }
}

class CompositeTemplate<T extends InputReceiver> extends InputHandlerTemplate<T> {
    private final InputHandlerTemplate<? super T> first;
    private final InputHandlerTemplate<? super T> second;

    CompositeTemplate(
            InputHandlerTemplate<? super T> first,
            InputHandlerTemplate<? super T> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    InputHandler instantiateFor(T target) {
        InputHandler handler1 = first.instantiateFor(target);
        InputHandler handler2 = second.instantiateFor(target);
        return handler1.orElse(handler2);
    }

}

@FunctionalInterface
interface TriFunction<A, B, C, D> {
    D apply(A a, B b, C c);
}