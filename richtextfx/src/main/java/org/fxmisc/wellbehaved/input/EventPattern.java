package org.fxmisc.wellbehaved.input;

import java.util.Optional;
import java.util.function.Predicate;

import javafx.event.Event;
import javafx.event.EventType;

@FunctionalInterface
public interface EventPattern<T extends Event, U extends T> {
    Optional<U> match(T event);

    default <V extends U> EventPattern<T, V> andThen(EventPattern<? super U, V> next) {
        return t -> match(t).flatMap(next::match);
    }

    default EventPattern<T, U> and(Predicate<? super U> condition) {
        return t -> match(t).map(u -> condition.test(u) ? u : null);
    }

    static <T extends Event> EventPattern<Event, T> eventTypePattern(EventType<T> eventType) {
        return event -> {
            EventType<? extends Event> actualType = event.getEventType();
            do {
                if(actualType.equals(eventType)) {
                    @SuppressWarnings("unchecked")
                    T res = (T) event;
                    return Optional.of(res);
                }
                actualType = actualType.getSuperType();
            } while(actualType != null);
            return Optional.empty();
        };
    }
}