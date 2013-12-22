package codearea.control;

import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.StyleableObjectProperty;
import javafx.scene.text.Font;

import codearea.skin.PropertyCssMetaData;

import com.sun.javafx.css.converters.FontConverter;


/**
 * CSS stuff related to {@link StyledTextArea}.
 */
class CssProperties {

    static final PseudoClass PSEUDO_CLASS_READONLY
            = PseudoClass.getPseudoClass("readonly");

    static class FontProperty<S> extends StyleableObjectProperty<Font> {
        private final StyledTextArea<S> textArea;
        private final CssMetaData<StyledTextArea<S>, Font> cssMetaData;

        public FontProperty(StyledTextArea<S> textArea) {
            this.textArea = textArea;
            this.cssMetaData = new PropertyCssMetaData<StyledTextArea<S>, Font>(
                    this, "-fx-font", FontConverter.getInstance(),
                    Font.getDefault());
        }

        @Override
        public Object getBean() { return textArea; }

        @Override
        public String getName() { return "font"; }

        @Override
        public CssMetaData<StyledTextArea<S>, Font> getCssMetaData() {
            return cssMetaData;
        }
    }
}
