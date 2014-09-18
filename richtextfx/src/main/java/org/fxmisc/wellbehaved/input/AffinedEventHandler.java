package org.fxmisc.wellbehaved.input;

import javafx.event.EventHandler;
import javafx.scene.input.InputEvent;

public interface AffinedEventHandler extends EventHandler<InputEvent> {

    InputReceiver getTarget();

    default void install() {
        EventHandler<? super InputEvent> oldHandler = getTarget().getOnInput();
        getTarget().setOnInput(EventHandlerHelper.chain(this, oldHandler));
    }

    default void remove() {
        EventHandler<? super InputEvent> oldHandler = getTarget().getOnInput();
        getTarget().setOnInput(EventHandlerHelper.exclude(oldHandler, this));
    }
}