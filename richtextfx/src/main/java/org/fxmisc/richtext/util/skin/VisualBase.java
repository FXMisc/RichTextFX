package org.fxmisc.richtext.util.skin;

import java.util.Collections;
import java.util.List;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.Control;

import org.reactfx.EventStream;
import org.reactfx.EventStreams;

public abstract class VisualBase<C extends Control> implements Visual {
    private final C control;

    public VisualBase(C control) {
        this.control = control;
    }

    @Override
    public void dispose() {
        // do nothing, override in subclasses
    }

    public final <T extends Event> void addEventHandler(
            EventType<T> eventType,
            EventHandler<? super T> eventHandler) {
        control.addEventHandler(eventType, eventHandler);
    }

    public <E extends Event> EventStream<E> events(EventType<E> eventType) {
        return EventStreams.eventsOf(control, eventType);
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return Collections.emptyList();
    }

    protected C getControl() {
        return control;
    }
}
