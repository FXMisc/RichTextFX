package org.fxmisc.wellbehaved.skin;

import javafx.scene.control.Control;

import org.fxmisc.wellbehaved.input.InputHandler;
import org.fxmisc.wellbehaved.input.InputReceiverHelper;

abstract class VisualBase<C extends Control> implements Visual<C> {
    private final InputReceiverHelper<C> helper;

    VisualBase(C control) {
        this.helper = new InputReceiverHelper<>(control);
    }

    @Override
    public final InputHandler getOnInput() {
        return helper.getOnInput();
    }

    @Override
    public final void setOnInput(InputHandler handler) {
        helper.setOnInput(handler);
    }

    @Override
    public final C getControl() {
        return helper.getTarget();
    }

    @Override
    public void dispose() {
        helper.dispose();
    }

}
