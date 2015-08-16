package org.fxmisc.richtext.skin;

import com.sun.javafx.css.converters.PaintConverter;
import javafx.beans.property.ObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class TextExt extends Text {

    private ObjectProperty<Paint[]> backgroundColor = null;

    public TextExt(String text) {
        super(text);
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        try {
            // Get inner class
            Class<?> clazz = Class.forName("javafx.scene.text.Text$StyleableProperties");

            // Get field and make it accessible
            Field styleablesField = clazz.getDeclaredField("STYLEABLES");
            styleablesField.setAccessible(true);

            // Get list value and make it modifiable
            List<CssMetaData<? extends Styleable, ?>> styleables = (List<CssMetaData<? extends Styleable, ?>>) styleablesField.get(null);
            styleables = new ArrayList<>(styleables);

            // Add new properties
            styleables.add(StyleableProperties.BACKGROUND_COLOR);

            // Set list value
            return styleables;
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    public ObjectProperty<Paint[]> backgroundColorProperty() {
        if (backgroundColor == null) {
            backgroundColor = new StyleableObjectProperty<Paint[]>(null) {
                @Override
                public Object getBean() {
                    return TextExt.this;
                }

                @Override
                public String getName() {
                    return "backgroundColor";
                }

                @Override
                public CssMetaData<TextExt, Paint[]> getCssMetaData() {
                    return StyleableProperties.BACKGROUND_COLOR;
                }

                @Override
                public void invalidated() {
                    // TODO
                }
            };
        }
        return backgroundColor;
    }

    private static class StyleableProperties {

        private static final CssMetaData<TextExt, Paint[]> BACKGROUND_COLOR = new CssMetaData<TextExt, Paint[]>(
                "-fx-background-color",
                PaintConverter.SequenceConverter.getInstance(),
                new Paint[]{Color.TRANSPARENT}) {
            @Override
            public boolean isSettable(TextExt node) {
                return node.backgroundColor == null || !node.backgroundColor.isBound();
            }

            @Override
            public StyleableProperty<Paint[]> getStyleableProperty(TextExt node) {
                return (StyleableProperty<Paint[]>) node.backgroundColorProperty();
            }
        };
    }
}
