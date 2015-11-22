package org.fxmisc.richtext;

import javafx.css.CssMetaData;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;

public class PropertyCssMetaData<S extends Styleable, V> extends CssMetaData<S, V> {

    private final StyleableObjectProperty<V> property;

    public PropertyCssMetaData(
            StyleableObjectProperty<V> property,
            String cssProperty,
            StyleConverter<?, V> converter,
            V initialValue) {
        super(cssProperty, converter, initialValue);
        this.property = property;
    }

    @Override
    public boolean isSettable(S node) {
        return !property.isBound();
    }

    @Override
    public StyleableProperty<V> getStyleableProperty(S node) {
        return property;
    }
}