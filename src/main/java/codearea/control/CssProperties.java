package codearea.control;

import javafx.css.FontCssMetaData;
import javafx.css.PseudoClass;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.text.Font;


/**
 * CSS stuff related to {@link StyledTextArea}.
 */
class CssProperties {

    static final PseudoClass PSEUDO_CLASS_READONLY
            = PseudoClass.getPseudoClass("readonly");

    static class FontProperty<S> extends StyleableObjectProperty<Font> {
        private final StyledTextArea<S> textArea;
        private final FontCssMetaData<StyledTextArea<S>> cssMetaData
                = new FontCssMetaData<StyledTextArea<S>>("-fx-font", Font.getDefault()) {
                    @Override
                    public boolean isSettable(StyledTextArea<S> node) {
                        assert node == textArea;
                        return !isBound();
                    }

                    @Override
                    public StyleableProperty<Font> getStyleableProperty(StyledTextArea<S> node) {
                        assert node == textArea;
                        return FontProperty.this;
                    }
                };

        public FontProperty(StyledTextArea<S> textArea) {
            this.textArea = textArea;
        }

        @Override
        public Object getBean() { return textArea; }

        @Override
        public String getName() { return "font"; }

        @Override
        public FontCssMetaData<StyledTextArea<S>> getCssMetaData() {
            return cssMetaData;
        }
    }
}
