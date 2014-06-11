package org.fxmisc.richtext.util.skin;

import javafx.scene.Node;

/**
 * A Visual that is represented by a single node.
 */
public interface SimpleVisual extends Visual {
    /**
     * Returns the node representing the visual rendering of the control. This
     * node will be attached to the control as its child on skin creation and
     * removed from the control on skin disposal.
     */
    Node getNode();
}
