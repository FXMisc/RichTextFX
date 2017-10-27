package org.fxmisc.richtext;

import javafx.css.CssMetaData;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;

import java.util.function.Function;

/**
 * Reduces boilerplate when creating a custom {@link CssMetaData} object
 */
public class CustomCssMetaData<S extends Styleable, V> extends CssMetaData<S, V> {

    private final Function<S, StyleableObjectProperty<V>> property;

    CustomCssMetaData(String property, StyleConverter<?, V> converter, V initialValue,
                      Function<S, StyleableObjectProperty<V>> getStyleableProperty) {
        super(property, converter, initialValue);
        this.property = getStyleableProperty;
    }

    @Override
    public boolean isSettable(S styleable) {
        return !property.apply(styleable).isBound();
    }

    @Override
    public StyleableProperty<V> getStyleableProperty(S styleable) {
        return property.apply(styleable);
    }


}
