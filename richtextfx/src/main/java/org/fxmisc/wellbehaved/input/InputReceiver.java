package org.fxmisc.wellbehaved.input;

public interface InputReceiver {
    InputHandler getOnInput();
    void setOnInput(InputHandler handler);
}
