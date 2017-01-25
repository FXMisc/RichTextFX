package org.fxmisc.richtext.demo.richtext;

import javafx.scene.Node;

public class EmptyLinkedImage<S> implements LinkedImage<S> {

    @Override
    public LinkedImage<S> setStyle(S style) {
        return this;
    }

    @Override
    public S getStyle() {
        return null;
    }

    @Override
    public String getImagePath() {
        return "";
    }

    @Override
    public Node createNode() {
        throw new AssertionError("Unreachable code");
    }
}
