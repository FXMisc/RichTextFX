package org.fxmisc.wellbehaved.input;

import static javafx.scene.input.KeyEvent.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import javafx.event.EventType;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

/**
 *
 * @param <T> type of the control the input handler applies to.
 */
public abstract class StatelessInputHandlerTemplate<T extends InputReceiver> implements InputHandlerTemplate<T>, BiConsumer<T, InputEvent> {

    public static abstract class Builder<T extends InputReceiver> {
        private static Builder<InputReceiver> empty() {
            return new Builder<InputReceiver>() {
                @Override
                <U extends InputReceiver> List<BiConsumer<? super U, ? super InputEvent>> getHandlers(int additionalCapacity) {
                    return new ArrayList<>(additionalCapacity);
                }
            };
        }

        // private constructor to prevent subclassing by the user
        private Builder() {}

        public <E extends InputEvent> On<T, E> on(EventPattern<? super InputEvent, E> eventMatcher) {
            return new On<>(this, eventMatcher);
        }

        public <E extends InputEvent> On<T, E> on(EventType<E> eventType) {
            return on(EventPattern.eventTypePattern(eventType));
        }

        public On<T, KeyEvent> onPressed(KeyCombination combination) {
            return on(KEY_PRESSED).where(combination::match);
        }

        public On<T, KeyEvent> onPressed(KeyCode code, KeyCombination.Modifier... modifiers) {
            return onPressed(new KeyCodeCombination(code, modifiers));
        }

        public On<T, KeyEvent> onPressed(String character, KeyCombination.Modifier... modifiers) {
            return onPressed(new KeyCharacterCombination(character, modifiers));
        }

        public On<T, KeyEvent> onReleased(KeyCombination combination) {
            return on(KEY_RELEASED).where(combination::match);
        }

        public On<T, KeyEvent> onReleased(KeyCode code, KeyCombination.Modifier... modifiers) {
            return onReleased(new KeyCodeCombination(code, modifiers));
        }

        public On<T, KeyEvent> onReleased(String character, KeyCombination.Modifier... modifiers) {
            return onReleased(new KeyCharacterCombination(character, modifiers));
        }

        public On<T, KeyEvent> onTyped(String character, KeyCombination.Modifier... modifiers) {
            KeyTypedCombination combination = new KeyTypedCombination(character, modifiers);
            return on(KEY_TYPED).where(combination::match);
        }

        public <U extends T> Builder<U> addHandler(BiConsumer<? super U, ? super InputEvent> handler) {
            return new CompositeBuilder<>(this, handler);
        }

        public StatelessInputHandlerTemplate<T> create() {
            List<BiConsumer<? super T, ? super InputEvent>> handlers = getHandlers();

            return new StatelessInputHandlerTemplate<T>() {
                @Override
                public void accept(T target, InputEvent event) {
                    for(BiConsumer<? super T, ? super InputEvent> handler: handlers) {
                        handler.accept(target, event);
                        if(event.isConsumed()) {
                            break;
                        }
                    }
                }
            };
        }

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
    }

    public static class On<T extends InputReceiver, E extends InputEvent> {
        private final Builder<? super T> previousBuilder;
        private final EventPattern<? super InputEvent, E> eventMatcher;

        private On(Builder<? super T> previous, EventPattern<? super InputEvent, E> eventMatcher) {
            this.previousBuilder = previous;
            this.eventMatcher = eventMatcher;
        }

        public On<T, E> where(Predicate<? super E> condition) {
            return new On<>(previousBuilder, eventMatcher.and(condition));
        }

        public <U extends T> When<U, E> when(Predicate<? super U> condition) {
            return new When<U, E>(previousBuilder, eventMatcher, condition);
        }

        public <U extends T> Builder<U> act(BiConsumer<? super U, ? super E> action) {
            return previousBuilder.addHandler((t, ie) -> {
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
            return previousBuilder.addHandler((u, ie) -> {
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

    public static <E extends InputEvent> On<InputReceiver, E> on(EventType<E> eventType) {
        return Builder.empty().on(eventType);
    }

    public static On<InputReceiver, KeyEvent> onPressed(KeyCombination combination) {
        return Builder.empty().onPressed(combination);
    }

    public static On<InputReceiver, KeyEvent> onPressed(KeyCode code, KeyCombination.Modifier... modifiers) {
        return Builder.empty().onPressed(code, modifiers);
    }

    public static On<InputReceiver, KeyEvent> onPressed(String character, KeyCombination.Modifier... modifiers) {
        return Builder.empty().onPressed(character, modifiers);
    }

    public static On<InputReceiver, KeyEvent> onReleased(KeyCombination combination) {
        return Builder.empty().onReleased(combination);
    }

    public static On<InputReceiver, KeyEvent> onReleased(KeyCode code, KeyCombination.Modifier... modifiers) {
        return Builder.empty().onReleased(code, modifiers);
    }

    public static On<InputReceiver, KeyEvent> onReleased(String character, KeyCombination.Modifier... modifiers) {
        return Builder.empty().onReleased(character, modifiers);
    }

    public static On<InputReceiver, KeyEvent> onTyped(String character, KeyCombination.Modifier... modifiers) {
        return Builder.empty().onTyped(character, modifiers);
    }

    public static <T extends InputReceiver> Builder<T>
    startWith(BiConsumer<? super T, ? super InputEvent> handler) {
        return Builder.empty().addHandler(handler);
    }


    // private constructor to prevent subclassing by the user
    private StatelessInputHandlerTemplate() {}

    @Override
    public final BiConsumer<? super T, ? super InputEvent> getHandler() {
        return this;
    }

    public final <U extends T> StatelessInputHandlerTemplate<U> onlyWhen(Predicate<? super U> condition) {
        return new StatelessInputHandlerTemplate<U>() {

            @Override
            public void accept(U target, InputEvent event) {
                if(condition.test(target)) {
                    StatelessInputHandlerTemplate.this.accept(target, event);
                }
            }
        };
    }
}