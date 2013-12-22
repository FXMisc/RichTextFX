package codearea.skin;

import javafx.css.CssMetaData;
import javafx.css.StyleableObjectProperty;
import javafx.scene.paint.Paint;
import codearea.control.StyledTextArea;

import com.sun.javafx.css.converters.PaintConverter;

class CssProperties {

    static class HighlightFillProperty extends StyleableObjectProperty<Paint> {
        private final StyledTextAreaSkin<?> skin;

        private final CssMetaData<StyledTextArea<?>, Paint> cssMetaData;

        public HighlightFillProperty(StyledTextAreaSkin<?> skin, Paint initialValue) {
            super(initialValue);
            this.skin = skin;
            cssMetaData = new PropertyCssMetaData<StyledTextArea<?>, Paint>(
                    this, "-fx-highlight-fill", PaintConverter.getInstance(),
                    initialValue);
        }

        @Override
        public Object getBean() {
            return skin;
        }

        @Override
        public String getName() {
            return "highlightFill";
        }

        @Override
        public CssMetaData<StyledTextArea<?>, Paint> getCssMetaData() {
            return cssMetaData;
        }
    };

    static class HighlightTextFillProperty extends StyleableObjectProperty<Paint> {
        private final StyledTextAreaSkin<?> skin;

        private final CssMetaData<StyledTextArea<?>, Paint> cssMetaData;

        public HighlightTextFillProperty(StyledTextAreaSkin<?> skin, Paint initialValue) {
            super(initialValue);
            this.skin = skin;
            cssMetaData = new PropertyCssMetaData<StyledTextArea<?>, Paint>(
                    this, "-fx-highlight-text-fill",
                    PaintConverter.getInstance(), initialValue);
        }

        @Override
        public Object getBean() {
            return skin;
        }

        @Override
        public String getName() {
            return "highlightTextFill";
        }

        @Override
        public CssMetaData<StyledTextArea<?>, Paint> getCssMetaData() {
            return cssMetaData;
        }
    }
}
