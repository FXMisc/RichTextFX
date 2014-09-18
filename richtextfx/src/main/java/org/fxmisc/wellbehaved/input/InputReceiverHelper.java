package org.fxmisc.wellbehaved.input;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.InputEvent;

public final class InputReceiverHelper<T extends Node> implements InputReceiver {
    private final T target;

    private EventHandler<? super InputEvent> handler = EventHandlerHelper.empty();

    public InputReceiverHelper(T target) {
        this.target = target;
    }

    public T getTarget() {
        return target;
    }

    @Override
    public EventHandler<? super InputEvent> getOnInput() {
        return handler;
    }

    @Override
    public void setOnInput(EventHandler<? super InputEvent> handler) {
        if(this.handler != EventHandlerHelper.empty()) {
            target.removeEventHandler(InputEvent.ANY, handler);
        }

        if(handler != EventHandlerHelper.empty()) {
            target.addEventHandler(InputEvent.ANY, handler);
        }

        this.handler = handler;
    }

    public void dispose() {
        setOnInput(EventHandlerHelper.empty());
    }
}
