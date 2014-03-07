package org.fxmisc.richtext.skin;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.scene.paint.Paint;

import com.sun.javafx.css.converters.PaintConverter;

class CssProperties {

    static class HighlightFillProperty extends StyleableObjectProperty<Paint> {
        private final StyledTextAreaSkin<?> skin;

        private final CssMetaData<? extends Styleable, Paint> cssMetaData;

        public HighlightFillProperty(StyledTextAreaSkin<?> skin, Paint initialValue) {
            super(initialValue);
            this.skin = skin;
            cssMetaData = new PropertyCssMetaData<Styleable, Paint>(
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
        public CssMetaData<? extends Styleable, Paint> getCssMetaData() {
            return cssMetaData;
        }
    };

    static class HighlightTextFillProperty extends StyleableObjectProperty<Paint> {
        private final StyledTextAreaSkin<?> skin;

        private final CssMetaData<? extends Styleable, Paint> cssMetaData;

        public HighlightTextFillProperty(StyledTextAreaSkin<?> skin, Paint initialValue) {
            super(initialValue);
            this.skin = skin;
            cssMetaData = new PropertyCssMetaData<Styleable, Paint>(
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
        public CssMetaData<? extends Styleable, Paint> getCssMetaData() {
            return cssMetaData;
        }
    }
}
