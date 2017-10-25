package org.fxmisc.richtext;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;

/**
 * Reduces the boilerplate when creating a custom CSS property (i.e. {@link javafx.css.StyleableProperty}).
 */
public class CustomStyleableProperty<T> extends StyleableObjectProperty<T> {

    private final Object bean;
    private final String name;
    private final CssMetaData<? extends Styleable, T> cssMetaData;

    public CustomStyleableProperty(T initialValue, String name, Object bean,
                                   CssMetaData<? extends Styleable, T> cssMetaData) {
        super(initialValue);
        this.bean = bean;
        this.name = name;
        this.cssMetaData = cssMetaData;
    }

    @Override
    public Object getBean() {
        return bean;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CssMetaData<? extends Styleable, T> getCssMetaData() {
        return cssMetaData;
    }

}