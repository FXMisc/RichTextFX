package org.fxmisc.richtext;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

public class TextExt extends Text {

    private final StyleableObjectProperty<Paint> backgroundFill = new StyleableObjectProperty<Paint>(null) {
        @Override
        public Object getBean() {
            return TextExt.this;
        }

        @Override
        public String getName() {
            return "backgroundFill";
        }

        @Override
        public CssMetaData<TextExt, Paint> getCssMetaData() {
            return StyleableProperties.BACKGROUND_FILL;
        }
    };

    private final StyleableObjectProperty<Paint> underlineStroke = new StyleableObjectProperty<Paint>(null) {
        @Override
        public Object getBean() {
            return TextExt.this;
        }

        @Override
        public String getName() {
            return "underlineStroke";
        }

        @Override
        public CssMetaData<TextExt, Paint> getCssMetaData() {
            return StyleableProperties.UNDERLINE_STROKE;
        }
    };

    TextExt(String text) {
        super(text);
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        // Get list value and make it modifiable
        List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(super.getCssMetaData());

        // Add new properties
        styleables.add(StyleableProperties.BACKGROUND_FILL);
        styleables.add(StyleableProperties.UNDERLINE_STROKE);

        // Return list value
        return styleables;
    }

    public Paint getBackgroundFill() {
        return backgroundFill.get();
    }

    public void setBackgroundFill(Paint fill) {
        backgroundFill.set(fill);
    }

    public ObjectProperty<Paint> backgroundFillProperty() {
        return backgroundFill;
    }

    public Paint getUnderlineStroke() {
        return underlineStroke.get();
    }

    public void setUnderlineStroke(Paint fill) {
        underlineStroke.set(fill);
    }

    public ObjectProperty<Paint> underlineStrokeProperty() {
        return underlineStroke;
    }

    private static class StyleableProperties {

        private static final CssMetaData<TextExt, Paint> BACKGROUND_FILL = new CssMetaData<TextExt, Paint>(
                "-fx-background-fill",
                StyleConverter.getPaintConverter(),
                Color.TRANSPARENT) {
            @Override
            public boolean isSettable(TextExt node) {
                return !node.backgroundFill.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(TextExt node) {
                return node.backgroundFill;
            }
        };

    
        private static final CssMetaData<TextExt, Paint> UNDERLINE_STROKE = new CssMetaData<TextExt, Paint>(
                "-fx-underline-stroke",
                StyleConverter.getPaintConverter(),
                Color.TRANSPARENT) {
            @Override
            public boolean isSettable(TextExt node) {
                return !node.underlineStroke.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(TextExt node) {
                return node.underlineStroke;
            }
        };
}
}
