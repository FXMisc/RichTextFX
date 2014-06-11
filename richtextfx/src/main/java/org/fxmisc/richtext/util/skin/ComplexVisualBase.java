package org.fxmisc.richtext.util.skin;

import java.lang.reflect.Method;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Control;

public abstract class ComplexVisualBase<C extends Control> implements Visual {
    private final C control;

    public ComplexVisualBase(C control) {
        this.control = control;
    }

    @Override
    public void dispose() {
        // Force to override dispose.
        // The constructor must have been modifying the child list,
        // so dispose() should undo these changes.
        throw new AbstractMethodError("must override dispose()");
    }

    protected final C getControl() {
        return control;
    }

    @SuppressWarnings("unchecked")
    protected ObservableList<Node> getChildren() {
        // Use reflection because control.getControlChildren is package private.
        // This would be unnecessary if this class was in javafx.scene control.
        try {
            Method m = Control.class.getDeclaredMethod("getControlChildren");
            m.setAccessible(true);
            return (ObservableList<Node>) m.invoke(control);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
}
