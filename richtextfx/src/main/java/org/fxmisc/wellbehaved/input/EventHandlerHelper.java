package org.fxmisc.wellbehaved.input;

import java.util.ArrayList;
import java.util.List;

import javafx.event.Event;
import javafx.event.EventHandler;

/**
 * Methods of this class could be added directly to the {@link EventHandler}
 * interface. The interface could further be extended with default methods
 * <pre>
 * {@code
 * EventHandler<? super T> orElse(EventHandler<? super T> other);
 * EventHandler<? super T> without(EventHandler<?> other);
 * }
 * </pre>
 * The latter may replace the {@link #exclude(EventHandler, EventHandler)}
 * static method.
 * @param <T>
 */
public final class EventHandlerHelper<T extends Event> {

    static <T extends Event> EventHandler<T> empty() {
        return EmptyEventHandler.instance();
    }

    @SafeVarargs
    static <T extends Event> EventHandler<? super T> chain(EventHandler<? super T>... handlers) {
        List<EventHandler<? super T>> nonEmptyHandlers = new ArrayList<>(handlers.length);
        for(EventHandler<? super T> handler: handlers) {
            if(handler != empty()) {
                nonEmptyHandlers.add(handler);
            }
        }
        if(nonEmptyHandlers.isEmpty()) {
            return empty();
        } else if(nonEmptyHandlers.size() == 1) {
            return nonEmptyHandlers.get(0);
        } else {
            return new CompositeEventHandler<>(nonEmptyHandlers);
        }
    }

    static <T extends Event> EventHandler<? super T> exclude(EventHandler<T> handler, EventHandler<?> subHandler) {
        if(handler instanceof CompositeEventHandler) {
            return ((CompositeEventHandler<T>) handler).without(subHandler);
        } else if(handler.equals(subHandler)) {
            return empty();
        } else {
            return handler;
        }
    }

    // prevent instantiation
    private EventHandlerHelper() {}
}

final class EmptyEventHandler<T extends Event> implements EventHandler<T> {
    private static EmptyEventHandler<?> INSTANCE = new EmptyEventHandler<>();

    @SuppressWarnings("unchecked")
    static <T extends Event> EmptyEventHandler<T> instance() {
        return (EmptyEventHandler<T>) INSTANCE;
    }

    private EmptyEventHandler() {}

    @Override
    public void handle(T event) {
        // do nothing
    }

}


class CompositeEventHandler<T extends Event> implements EventHandler<T> {
    private final List<EventHandler<? super T>> handlers;

    CompositeEventHandler(List<EventHandler<? super T>> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void handle(T event) {
        for(EventHandler<? super T> handler: handlers) {
            handler.handle(event);
            if(event.isConsumed()) {
                break;
            }
        }
    }

    public EventHandler<? super T> without(EventHandler<?> other) {
        if(this.equals(other)) {
            return EmptyEventHandler.instance();
        } else {
            boolean changed = false;
            List<EventHandler<? super T>> newHandlers = new ArrayList<>(handlers.size());
            for(EventHandler<? super T> handler: handlers) {
                EventHandler<? super T> h = EventHandlerHelper.exclude(handler, other);
                if(h != handler) {
                    changed = true;
                }
                if(h != EmptyEventHandler.instance()) {
                    newHandlers.add(h);
                }
            }

            if(!changed) {
                return this;
            } else if(newHandlers.isEmpty()) {
                return EmptyEventHandler.instance();
            } else if(newHandlers.size() == 1) {
                return newHandlers.get(0);
            } else {
                return new CompositeEventHandler<>(newHandlers);
            }
        }
    }
}