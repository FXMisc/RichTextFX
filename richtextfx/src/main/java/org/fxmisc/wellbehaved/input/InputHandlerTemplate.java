package org.fxmisc.wellbehaved.input;

import java.util.function.BiConsumer;

import javafx.scene.input.InputEvent;

@FunctionalInterface
public interface InputHandlerTemplate<T extends InputReceiver> {
    BiConsumer<? super T, ? super InputEvent> getHandler();

    default AffinedEventHandler bind(T target) {
        BiConsumer<? super T, ? super InputEvent> handler = getHandler();
        return new AffinedEventHandler() {

            @Override
            public void handle(InputEvent event) {
                handler.accept(target, event);
            }

            @Override
            public InputReceiver getTarget() {
                return target;
            }
        };
    }

    default <U extends T> InputHandlerTemplate<U> orElse(InputHandlerTemplate<U> that) {
        return () -> {
            BiConsumer<? super T, ? super InputEvent> thisHandler = InputHandlerTemplate.this.getHandler();
            BiConsumer<? super U, ? super InputEvent> thatHandler = that.getHandler();
            return (u, e) -> {
                thisHandler.accept(u, e);
                if(!e.isConsumed()) {
                    thatHandler.accept(u, e);
                }
            };
        };
    }

    default <U extends T> InputHandlerTemplate<U> ifConsumed(BiConsumer<? super U, ? super InputEvent> postConsumption) {
        return () -> {
            BiConsumer<? super T, ? super InputEvent> thisHandler = InputHandlerTemplate.this.getHandler();
            return (u, e) -> {
                thisHandler.accept(u, e);
                if(e.isConsumed()) {
                    postConsumption.accept(u, e);
                }
            };
        };
    }
}
