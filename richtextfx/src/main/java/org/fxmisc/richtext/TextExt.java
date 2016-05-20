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

    private final StyleableObjectProperty<Number> underlineStrokeWidth = new StyleableObjectProperty<Number>(null) {
        @Override
        public Object getBean() {
            return TextExt.this;
        }

        @Override
        public String getName() {
            return "underlineStrokeWidth";
        }

        @Override
        public CssMetaData<TextExt, Number> getCssMetaData() {
            return StyleableProperties.UNDERLINE_STROKE_WIDTH;
        }
    };

    private final StyleableObjectProperty<Number> underlineStrokeDashSize = new StyleableObjectProperty<Number>(null) {
        @Override
        public Object getBean() {
            return TextExt.this;
        }

        @Override
        public String getName() {
            return "underlineStrokeDashSize";
        }

        @Override
        public CssMetaData<TextExt, Number> getCssMetaData() {
            return StyleableProperties.UNDERLINE_STROKE_DASH_SIZE;
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
        styleables.add(StyleableProperties.UNDERLINE_STROKE_WIDTH);
        styleables.add(StyleableProperties.UNDERLINE_STROKE_DASH_SIZE);

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

    // Color of the text underline
    public Paint getUnderlineStroke() { return underlineStroke.get(); }
    public void setUnderlineStroke(Paint fill) { underlineStroke.set(fill); }
    public ObjectProperty<Paint> underlineStrokeProperty() { return underlineStroke; }

    // Width of the text underline
    public Number getUnderlineStrokeWidth() { return underlineStrokeWidth.get(); }
    public void setUnderlineStrokeWidth(Number width) { underlineStrokeWidth.set(width); }
    public ObjectProperty<Number> underlineStrokeWidthProperty() { return underlineStrokeWidth; }

    // Dash size for the text underline (XXX Pending: use an array) 
    public Number getUnderlineStrokeDashSize() { return underlineStrokeDashSize.get(); }
    public void setUnderlineStrokeDashSize(Number width) { underlineStrokeDashSize.set(width); }
    public ObjectProperty<Number> underlineStrokeDashSizeProperty() { return underlineStrokeDashSize; }

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

        private static final CssMetaData<TextExt, Number> UNDERLINE_STROKE_WIDTH = new CssMetaData<TextExt, Number>(
                "-fx-underline-stroke-width",
                StyleConverter.getSizeConverter(), 
                0) {
            @Override
            public boolean isSettable(TextExt node) {
                return !node.underlineStrokeWidth.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(TextExt node) {
                return node.underlineStrokeWidth;
            }
        };

        private static final CssMetaData<TextExt, Number> UNDERLINE_STROKE_DASH_SIZE = new CssMetaData<TextExt, Number>(
                "-fx-underline-stroke-dash-size",
                StyleConverter.getSizeConverter(), 
                0) {
            @Override
            public boolean isSettable(TextExt node) {
                return !node.underlineStrokeDashSize.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(TextExt node) {
                return node.underlineStrokeDashSize;
            }
        };
    }
}
