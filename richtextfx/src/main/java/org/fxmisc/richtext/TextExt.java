package org.fxmisc.richtext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.SizeConverter;

import javafx.beans.property.ObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Text;

/**
 * A class which adds some more styleable properties to JavaFX's {@link Text} class.
 *
 * The extra items can be styled using the properties (and accessors/mutators) or via CSS.
 * Each property is documented with its CSS property.  Each CSS property begins with the "-rtfx"
 * prefix.
 *
 * <p>Note that the underline properties specified here are orthogonal to the {@link #underlineProperty()} inherited
 * from {@link Text}.  The underline properties defined here in {@link TextExt} will cause an underline to be
 * drawn if {@link #underlineWidthProperty()} is non-null and greater than zero, regardless of
 * the value of {@link #underlineProperty()}.</p>
 */
public class TextExt extends Text {

    private static final List<CssMetaData<? extends Styleable, ?>> CSS_META_DATA_LIST;
    static {
        // Get list value and make it modifiable
        List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Text.getClassCssMetaData());

        // Add new properties
        styleables.add(StyleableProperties.BACKGROUND_COLOR);
        styleables.add(StyleableProperties.UNDERLINE_COLOR);
        styleables.add(StyleableProperties.UNDERLINE_WIDTH);
        styleables.add(StyleableProperties.UNDERLINE_DASH_ARRAY);
        styleables.add(StyleableProperties.UNDERLINE_CAP);

        CSS_META_DATA_LIST = Collections.unmodifiableList(styleables);
    }
	
    private final StyleableObjectProperty<Paint> backgroundColor = new StyleableObjectProperty<Paint>(null) {
        @Override
        public Object getBean() {
            return TextExt.this;
        }

        @Override
        public String getName() {
            return "backgroundColor";
        }

        @Override
        public CssMetaData<TextExt, Paint> getCssMetaData() {
            return StyleableProperties.BACKGROUND_COLOR;
        }
    };

    private final StyleableObjectProperty<Paint> underlineColor = new StyleableObjectProperty<Paint>(null) {
        @Override
        public Object getBean() {
            return TextExt.this;
        }

        @Override
        public String getName() {
            return "underlineColor";
        }

        @Override
        public CssMetaData<TextExt, Paint> getCssMetaData() {
            return StyleableProperties.UNDERLINE_COLOR;
        }
    };

    private final StyleableObjectProperty<Number> underlineWidth = new StyleableObjectProperty<Number>(null) {
        @Override
        public Object getBean() {
            return TextExt.this;
        }

        @Override
        public String getName() {
            return "underlineWidth";
        }

        @Override
        public CssMetaData<TextExt, Number> getCssMetaData() {
            return StyleableProperties.UNDERLINE_WIDTH;
        }
    };

    private final StyleableObjectProperty<Number[]> underlineDashArray = new StyleableObjectProperty<Number[]>(null) {
        @Override
        public Object getBean() {
            return TextExt.this;
        }

        @Override
        public String getName() {
            return "underlineDashArray";
        }

        @Override
        public CssMetaData<TextExt, Number[]> getCssMetaData() {
            return StyleableProperties.UNDERLINE_DASH_ARRAY;
        }
    };


    private final StyleableObjectProperty<StrokeLineCap> underlineCap = new StyleableObjectProperty<StrokeLineCap>(null) {
        @Override
        public Object getBean() {
            return TextExt.this;
        }

        @Override
        public String getName() {
            return "underlineCap";
        }

        @Override
        public CssMetaData<TextExt, StrokeLineCap> getCssMetaData() {
            return StyleableProperties.UNDERLINE_CAP;
        }
    };

    TextExt(String text) {
        super(text);
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return CSS_META_DATA_LIST;
    }

    public Paint getBackgroundColor() {
        return backgroundColor.get();
    }

    public void setBackgroundColor(Paint fill) {
        backgroundColor.set(fill);
    }

    /**
     * The background color of the section of text.  By default, JavaFX doesn't
     * support a background for Text (as it is a Shape item), but RichTextFX
     * does support drawing a different background for different sections of text.
     *
     * <p>Note that this is actually a Paint type, so you can specify gradient or image fills
     * rather than a flat colour.  But due to line wrapping, it's possible that
     * the fill may be used multiple times on separate lines even for the same
     * segment of text.</p>
     *
     * Can be styled from CSS using the "-rtfx-background-color" property.
     */
    public ObjectProperty<Paint> backgroundColorProperty() {
        return backgroundColor;
    }

    // Color of the text underline (-fx-underline is already defined by JavaFX)
    public Paint getUnderlineColor() { return underlineColor.get(); }
    public void setUnderlineColor(Paint fill) { underlineColor.set(fill); }

    /**
     * The underline color of the section of text.
     *
     * <p>Note that this is actually a Paint type, so you can specify gradient or image fills
     * rather than a flat colour.  But due to line wrapping, it's possible that
     * the fill may be used multiple times on separate lines even for the
     * same segment of text.</p>
     *
     * Can be styled from CSS using the "-rtfx-underline-color" property
     * (not to be confused with JavaFX's separate "-fx-underline" property).
     *
     * <p>Note that the underline properties specified here are orthogonal to the {@link #underlineProperty()} inherited
     * from {@link Text}.  The underline properties defined here in {@link TextExt} will cause an underline to be
     * drawn if {@link #underlineWidthProperty()} is non-null and greater than zero, regardless of
     * the value of {@link #underlineProperty()}.</p>
     */
    public ObjectProperty<Paint> underlineColorProperty() { return underlineColor; }

    // Width of the text underline
    public Number getUnderlineWidth() { return underlineWidth.get(); }
    public void setUnderlineWidth(Number width) { underlineWidth.set(width); }

    /**
     * The width of the underline for a section of text.  If null or zero,
     * the underline will not be drawn.
     *
     * Can be styled from CSS using the "-rtfx-underline-width" property.
     *
     * <p>Note that the underline properties specified here are orthogonal to the {@link #underlineProperty()} inherited
     * from {@link Text}.  The underline properties defined here in {@link TextExt} will cause an underline to be
     * drawn if {@link #underlineWidthProperty()} is non-null and greater than zero, regardless of
     * the value of {@link #underlineProperty()}.</p>
     */
    public ObjectProperty<Number> underlineWidthProperty() { return underlineWidth; }

    // Dash array for the text underline 
    public Number[] getUnderlineDashArray() { return underlineDashArray.get(); }
    public void setUnderlineDashArray(Number[] dashArray) { underlineDashArray.set(dashArray); }

    /**
     * The dash array used for drawing the underline for a section of text.
     *
     * Can be styled from CSS using the "-rtfx-underline-dash-array" property.
     *
     * <p>Note that the underline properties specified here are orthogonal to the {@link #underlineProperty()} inherited
     * from {@link Text}.  The underline properties defined here in {@link TextExt} will cause an underline to be
     * drawn if {@link #underlineWidthProperty()} is non-null and greater than zero, regardless of
     * the value of {@link #underlineProperty()}.</p>
     */
    public ObjectProperty<Number[]> underlineDashArrayProperty() { return underlineDashArray; }

    // The end cap style of each dash in a dashed underline
    public StrokeLineCap getUnderlineCap() { return underlineCap.get(); }
    public void setUnderlineCap(StrokeLineCap cap) { underlineCap.set(cap); }
    /**
     * The end cap style used for drawing each dash in a dashed underline for a section of text.
     *
     * Can be styled from CSS using the "-rtfx-underline-cap" property.
     *
     * <p>Note that the underline properties specified here are orthogonal to the {@link #underlineProperty()} inherited
     * from {@link Text}.  The underline properties defined here in {@link TextExt} will cause an underline to be
     * drawn if {@link #underlineWidthProperty()} is non-null and greater than zero, regardless of
     * the value of {@link #underlineProperty()}.</p>
     */
    public ObjectProperty<StrokeLineCap> underlineCapProperty() { return underlineCap; }

    private static class StyleableProperties {

        private static final CssMetaData<TextExt, Paint> BACKGROUND_COLOR = new CssMetaData<TextExt, Paint>(
                "-rtfx-background-color",
                StyleConverter.getPaintConverter(),
                Color.TRANSPARENT) {
            @Override
            public boolean isSettable(TextExt node) {
                return !node.backgroundColor.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(TextExt node) {
                return node.backgroundColor;
            }
        };


        private static final CssMetaData<TextExt, Paint> UNDERLINE_COLOR = new CssMetaData<TextExt, Paint>(
                "-rtfx-underline-color",
                StyleConverter.getPaintConverter(),
                Color.TRANSPARENT) {
            @Override
            public boolean isSettable(TextExt node) {
                return !node.underlineColor.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(TextExt node) {
                return node.underlineColor;
            }
        };

        private static final CssMetaData<TextExt, Number> UNDERLINE_WIDTH = new CssMetaData<TextExt, Number>(
                "-rtfx-underline-width",
                StyleConverter.getSizeConverter(),
                0) {

            @Override
            public boolean isSettable(TextExt node) {
                return !node.underlineWidth.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(TextExt node) {
                return node.underlineWidth;
            }
        };

        private static final CssMetaData<TextExt, Number[]> UNDERLINE_DASH_ARRAY = new CssMetaData<TextExt, Number[]>(
                "-rtfx-underline-dash-array",
                SizeConverter.SequenceConverter.getInstance(),
                new Double[0]) {

            @Override
            public boolean isSettable(TextExt node) {
                return !node.underlineDashArray.isBound();
            }

            @Override
            public StyleableProperty<Number[]> getStyleableProperty(TextExt node) {
                return node.underlineDashArray;
            }
        };

        private static final CssMetaData<TextExt, StrokeLineCap> UNDERLINE_CAP = new CssMetaData<TextExt, StrokeLineCap>(
                "-rtfx-underline-cap",
                new EnumConverter<StrokeLineCap>(StrokeLineCap.class),
                StrokeLineCap.SQUARE) {

            @Override
            public boolean isSettable(TextExt node) {
                return !node.underlineCap.isBound();
            }

            @Override
            public StyleableProperty<StrokeLineCap> getStyleableProperty(TextExt node) {
                return node.underlineCap;
            }
        };
    }
}
