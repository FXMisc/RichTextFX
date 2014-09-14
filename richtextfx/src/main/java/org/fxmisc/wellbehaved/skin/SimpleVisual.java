package org.fxmisc.wellbehaved.skin;

import javafx.scene.Node;
import javafx.scene.control.Control;

/**
 * A Visual that is represented by a single node.
 */
public interface SimpleVisual<C extends Control> extends Visual<C> {
    /**
     * Returns the node representing the visual rendering of the control. This
     * node will be attached to the control as its child on skin creation and
     * removed from the control on skin disposal.
     */
    Node getNode();
}
