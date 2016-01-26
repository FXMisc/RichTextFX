package org.fxmisc.richtext;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;


/**
 * CSS stuff related to {@link StyledTextArea}.
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

    static class HighlightFillProperty extends StyleableObjectProperty<Paint> {
        private final Object bean;

        private final CssMetaData<? extends Styleable, Paint> cssMetaData;

        public HighlightFillProperty(Object bean, Paint initialValue) {
            super(initialValue);
            this.bean = bean;
            cssMetaData = new PropertyCssMetaData<>(
                    this, "-fx-highlight-fill",
                    StyleConverter.getPaintConverter(), initialValue);
        }

        @Override
        public Object getBean() {
            return bean;
        }

        @Override
        public String getName() {
            return "highlightFill";
        }

        @Override
        public CssMetaData<? extends Styleable, Paint> getCssMetaData() {
            return cssMetaData;
        }
    }

    static class HighlightTextFillProperty extends StyleableObjectProperty<Paint> {
        private final Object bean;

        private final CssMetaData<? extends Styleable, Paint> cssMetaData;

        public HighlightTextFillProperty(Object bean, Paint initialValue) {
            super(initialValue);
            this.bean = bean;
            cssMetaData = new PropertyCssMetaData<>(
                    this, "-fx-highlight-text-fill",
                    StyleConverter.getPaintConverter(), initialValue);
        }

        @Override
        public Object getBean() {
            return bean;
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
