package org.fxmisc.richtext.util.skin;

import java.util.List;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.scene.Node;

public interface Visual {
    Node getNode();
    void dispose();
    List<CssMetaData<? extends Styleable, ?>> getCssMetaData();
}
