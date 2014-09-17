package org.fxmisc.wellbehaved.input;

import javafx.scene.Node;
import javafx.scene.input.InputEvent;

public final class InputReceiverHelper<T extends Node> implements InputReceiver {
    private final T target;

    private InputHandler handler = InputHandler.EMPTY;

    public InputReceiverHelper(T target) {
        this.target = target;
    }

    public T getTarget() {
        return target;
    }

    @Override
    public InputHandler getOnInput() {
        return handler;
    }

    @Override
    public void setOnInput(InputHandler handler) {
        if(handler != InputHandler.EMPTY && handler.getTarget() != this) {
            throw new IllegalArgumentException("Wrong input target: " + handler.getTarget() + " instead of " + this);
        }

        if(this.handler != InputHandler.EMPTY) {
            target.removeEventHandler(InputEvent.ANY, handler);
        }

        if(handler != InputHandler.EMPTY) {
            target.addEventHandler(InputEvent.ANY, handler);
        }

        this.handler = handler;
    }

    public void dispose() {
        setOnInput(InputHandler.EMPTY);
    }
}
