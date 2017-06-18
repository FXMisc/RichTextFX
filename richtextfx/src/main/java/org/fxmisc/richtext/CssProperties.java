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
        private final CssMetaData<? extends Styleable, T> cssMetaData;

        public CustomStyleablePropertyBase(T initialValue,
                                           String name,
                                           GenericStyledArea<?, ?, ?> area,
                                           CssMetaData<? extends Styleable, T> cssMetaData) {
            super(initialValue);
            this.area = area;
            this.name = name;
            this.cssMetaData = cssMetaData;
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
            return cssMetaData;
        }

    }

    static class HighlightFillProperty extends CustomStyleablePropertyBase<Paint> {

        public HighlightFillProperty(GenericStyledArea<?, ?, ?> area,
                                     CssMetaData<? extends Styleable, Paint> cssMetaDataSupplier) {
            super(Color.DODGERBLUE, "highlightFill", area, cssMetaDataSupplier);
        }

    }

    static class HighlightTextFillProperty extends CustomStyleablePropertyBase<Paint> {

        public HighlightTextFillProperty(GenericStyledArea<?, ?, ?> area,
                                         CssMetaData<? extends Styleable, Paint> cssMetaData) {
            super(Color.WHITE, "highlightTextFill", area, cssMetaData);
        }

    }

    static class CaretBlinkRateProperty extends CustomStyleablePropertyBase<Duration> {

        public CaretBlinkRateProperty(GenericStyledArea<?, ?, ?> area,
                                      CssMetaData<? extends Styleable, Duration> cssMetaData) {
            super(Duration.millis(500), "caretBlinkRate", area, cssMetaData);
        }

    }
}
