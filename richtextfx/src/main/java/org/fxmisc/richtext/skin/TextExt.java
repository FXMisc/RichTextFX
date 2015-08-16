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

import java.util.ArrayList;
import java.util.List;

public class TextExt extends Text {

    private ObjectProperty<Paint[]> backgroundColor = null;

    public TextExt(String text) {
        super(text);
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        // Get list value and make it modifiable
        List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(super.getCssMetaData());

        // Add new properties
        styleables.add(StyleableProperties.BACKGROUND_COLOR);

        // Return list value
        return styleables;
    }

    public Paint[] getBackgroundColor() {
        return backgroundColor.get();
    }

    public void setBackgroundColor(Paint[] backgroundColor) {
        this.backgroundColor.set(backgroundColor);
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
