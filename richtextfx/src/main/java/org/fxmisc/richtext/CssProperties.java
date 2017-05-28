package org.fxmisc.richtext;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.function.Supplier;


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

    private static class CustomStyleablePropertyBase<T> extends StyleableObjectProperty<T> {

        private final GenericStyledArea<?, ?, ?> area;
        private final String name;
        private final Supplier<CssMetaData<? extends Styleable, T>> cssMetaDataSupplier;

        public CustomStyleablePropertyBase(T initialValue,
                                           String name,
                                           GenericStyledArea<?, ?, ?> area,
                                           Supplier<CssMetaData<? extends Styleable, T>> cssMetaDataSupplier) {
            super(initialValue);
            this.area = area;
            this.name = name;
            this.cssMetaDataSupplier = cssMetaDataSupplier;
        }

        @Override
        public Object getBean() {
            return area;
        }

        @Override
        public String getName() {
            return "highlightFill";
        }

        @Override
        public CssMetaData<? extends Styleable, T> getCssMetaData() {
            return cssMetaDataSupplier.get();
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

    static class HighlightFillProperty extends CustomStyleablePropertyBase<Paint> {

        public HighlightFillProperty(GenericStyledArea<?, ?, ?> area,
                                     Supplier<CssMetaData<? extends Styleable, Paint>> cssMetaDataSupplier) {
            super(Color.DODGERBLUE, "highlightFill", area, cssMetaDataSupplier);
        }

    }

    static class HighlightTextFillProperty extends CustomStyleablePropertyBase<Paint> {

        public HighlightTextFillProperty(GenericStyledArea<?, ?, ?> area,
                                         Supplier<CssMetaData<? extends Styleable, Paint>> cssMetaDataSupplier) {
            super(Color.WHITE, "highlightTextFill", area, cssMetaDataSupplier);
        }

    }

    static class CaretBlinkRateProperty extends CustomStyleablePropertyBase<Duration> {

        public CaretBlinkRateProperty(GenericStyledArea<?, ?, ?> area,
                                      Supplier<CssMetaData<? extends Styleable, Duration>> cssMetaDataSupplier) {
            super(Duration.millis(500), "caretBlinkRate", area, cssMetaDataSupplier);
        }

    }
}
