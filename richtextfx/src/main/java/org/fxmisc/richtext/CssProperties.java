package org.fxmisc.richtext;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.scene.control.Control;
import javafx.scene.text.Font;

import org.fxmisc.richtext.skin.PropertyCssMetaData;


/**
 * CSS stuff related to {@link StyledTextArea}.
 */
class CssProperties {

    static final PseudoClass PSEUDO_CLASS_READONLY
            = PseudoClass.getPseudoClass("readonly");

    static class EditableProperty<C extends Control> extends SimpleBooleanProperty {
        public EditableProperty(C control) {
            super(control, "editable", true);
        }

        @Override protected void invalidated() {
            ((Control) getBean()).pseudoClassStateChanged(PSEUDO_CLASS_READONLY, !get());
        }
    }

    static class FontProperty<S extends Styleable> extends StyleableObjectProperty<Font> {
        private final S textArea;
        private final CssMetaData<S, Font> cssMetaData;

        public FontProperty(S textArea) {
            this.textArea = textArea;
            this.cssMetaData = new PropertyCssMetaData<S, Font>(
                    this, "-fx-font", StyleConverter.getFontConverter(),
                    Font.getDefault());
        }

        @Override
        public Object getBean() { return textArea; }

        @Override
        public String getName() { return "font"; }

        @Override
        public CssMetaData<S, Font> getCssMetaData() {
            return cssMetaData;
        }
    }
}
