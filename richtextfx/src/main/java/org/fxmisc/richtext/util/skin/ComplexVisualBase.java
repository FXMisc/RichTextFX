package org.fxmisc.richtext.util.skin;

import java.lang.reflect.Method;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Control;

/**
 * Base class for Visuals that need to manipulate control's child list directly.
 * An implementation of this class is responsible for attaching any nodes to
 * control's child list as well as removing them on {@link #dispose()}.
 * @param <C> type of the control.
 */
public abstract class ComplexVisualBase<C extends Control> implements Visual {
    private final C control;
    private final ObservableList<Node> children;

    @SuppressWarnings("unchecked")
    public ComplexVisualBase(C control) {
        this.control = control;

        // Use reflection because control.getControlChildren is package private.
        // This would be unnecessary if this class was in javafx.scene control.
        try {
            Method m = Control.class.getDeclaredMethod("getControlChildren");
            m.setAccessible(true);
            children = (ObservableList<Node>) m.invoke(control);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * In addition, it should remove any nodes it had previously attached to
     * the control.
     */
    @Override
    public abstract void dispose();

    /**
     * Returns the control this Visual is used for.
     */
    protected final C getControl() {
        return control;
    }

    /**
     * Provides direct access to control's child list.
     */
    protected ObservableList<Node> getChildren() {
        return children;
    }
}
