package org.fxmisc.wellbehaved.skin;

import javafx.scene.Node;
import javafx.scene.control.Control;

/**
 * A Visual that is represented by a single node.
 */
public abstract class SimpleVisualBase<C extends Control> extends VisualBase<C> {

    public SimpleVisualBase(C control) {
        super(control);
    }

    /**
     * Returns the node representing the visual rendering of the control. This
     * node will be attached to the control as its child on skin creation and
     * removed from the control on skin disposal.
     */
    public abstract Node getNode();
}
