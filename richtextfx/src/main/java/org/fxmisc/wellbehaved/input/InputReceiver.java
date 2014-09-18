package org.fxmisc.wellbehaved.input;

import javafx.event.EventHandler;
import javafx.scene.input.InputEvent;

public interface InputReceiver {
    EventHandler<? super InputEvent> getOnInput();
    void setOnInput(EventHandler<? super InputEvent> handler);
}
