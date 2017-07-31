package org.fxmisc.richtext;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Duration;


/**
 * CSS stuff related to {@link GenericStyledArea}.
 */
class CssProperties {

    static final PseudoClass PSEUDO_CLASS_READONLY
            = PseudoClass.getPseudoClass("readonly");

    static class EditableProperty<R extends Region> extends SimpleBooleanProperty {
        public EditableProperty(R region) {
            super(region, "editable", true);
        }

        @Override protected void invalidated() {
            ((Region) getBean()).pseudoClassStateChanged(PSEUDO_CLASS_READONLY, !get());
        }
    }

}
