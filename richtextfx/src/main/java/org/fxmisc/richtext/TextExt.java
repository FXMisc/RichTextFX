package org.fxmisc.richtext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.css.converter.SizeConverter;
import javafx.css.CssMetaData;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Text;

/**
 * A class which adds some more RichTextFX-specific styleable properties to JavaFX's {@link Text} class.
 *
 * The extra items can be styled using the properties (and accessors/mutators) or via CSS.
 * Each property is documented with its CSS property. Each CSS property begins with the "-rtfx"
 * prefix.
 *
 * <p>Note that the underline properties specified here are orthogonal to the {@link #underlineProperty()} inherited
 * from {@link Text}. The underline properties defined here in {@link TextExt} will cause an underline to be
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
        styleables.add(StyleableProperties.BORDER_COLOR);
        styleables.add(StyleableProperties.BORDER_WIDTH);
        styleables.add(StyleableProperties.BORDER_TYPE);
        styleables.add(StyleableProperties.BORDER_DASH_ARRAY);
        styleables.add(StyleableProperties.UNDERLINE_COLOR);
        styleables.add(StyleableProperties.UNDERLINE_WIDTH);
        styleables.add(StyleableProperties.UNDERLINE_OFFSET);
        styleables.add(StyleableProperties.UNDERLINE_WAVE_RADIUS);
        styleables.add(StyleableProperties.UNDERLINE_DASH_ARRAY);
        styleables.add(StyleableProperties.UNDERLINE_DOUBLE_GAP);
        styleables.add(StyleableProperties.UNDERLINE_CAP);

        CSS_META_DATA_LIST = Collections.unmodifiableList(styleables);
    }

    private final StyleableObjectProperty<Paint> backgroundColor = new CustomStyleableProperty<>(
            null, "backgroundColor", this, StyleableProperties.BACKGROUND_COLOR
    );

    private final StyleableObjectProperty<Paint> borderStrokeColor = new CustomStyleableProperty<>(
            null, "borderStrokeColor", this, StyleableProperties.BORDER_COLOR
    );

    private final StyleableObjectProperty<Number> borderStrokeWidth = new CustomStyleableProperty<>(
            null, "borderStrokeWidth", this, StyleableProperties.BORDER_WIDTH
    );

    private final StyleableObjectProperty<StrokeType> borderStrokeType = new CustomStyleableProperty<>(
            null, "borderStrokeType", this, StyleableProperties.BORDER_TYPE
    );

    private final StyleableObjectProperty<Number[]> borderStrokeDashArray = new CustomStyleableProperty<>(
            null, "borderStrokeDashArray", this, StyleableProperties.BORDER_DASH_ARRAY
    );

    private final StyleableObjectProperty<Paint> underlineColor = new CustomStyleableProperty<>(
            null, "underlineColor", this, StyleableProperties.UNDERLINE_COLOR
    );

    private final StyleableObjectProperty<Number> underlineWidth = new CustomStyleableProperty<>(
            null, "underlineWidth", this, StyleableProperties.UNDERLINE_WIDTH
    );

    private final StyleableObjectProperty<Number> underlineOffset = new CustomStyleableProperty<>(
            null, "underlineOffset", this, StyleableProperties.UNDERLINE_OFFSET
    );

    private final StyleableObjectProperty<Number> underlineWaveRadius = new CustomStyleableProperty<>(
            null, "underlineWaveRadius", this, StyleableProperties.UNDERLINE_WAVE_RADIUS
    );

    private final StyleableObjectProperty<Number[]> underlineDashArray = new CustomStyleableProperty<>(
            null, "underlineDashArray", this, StyleableProperties.UNDERLINE_DASH_ARRAY
    );

    private final StyleableObjectProperty<Number> underlineDoubleGap = new CustomStyleableProperty<>(
            null, "underlineDoubleGap", this, StyleableProperties.UNDERLINE_DOUBLE_GAP
    );

    private final StyleableObjectProperty<StrokeLineCap> underlineCap = new CustomStyleableProperty<>(
            null, "underlineCap", this, StyleableProperties.UNDERLINE_CAP
    );

    public TextExt(String text) {
        super(text);
    }

    public TextExt() {
        super();
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

    public Paint getBorderStrokeColor() {
        return borderStrokeColor.get();
    }

    public void setBorderStrokeColor(Paint fill) {
        borderStrokeColor.set(fill);
    }

    /**
     * The border stroke color of the section of text.  By default, JavaFX doesn't
     * support a border for Text (as it is a Shape item), but RichTextFX
     * does support drawing a different background for different sections of text. The border
     * will only be drawn if this has a non-null value and {@link #borderStrokeWidthProperty()}
     * a positive value
     *
     * <p>Note that this is actually a Paint type, so you can specify gradient or image fills
     * rather than a flat colour.  But due to line wrapping, it's possible that
     * the fill may be used multiple times on separate lines even for the same
     * segment of text.</p>
     *
     * Can be styled from CSS using the "-rtfx-border-stroke-color" property.
     */
    public ObjectProperty<Paint> borderStrokeColorProperty() {
        return borderStrokeColor;
    }

    // Width of the text underline
    public Number getBorderStrokeWidth() { return borderStrokeWidth.get(); }
    public void setBorderStrokeWidth(Number width) { borderStrokeWidth.set(width); }

    /**
     * The width of the border stroke.  The border stroke will only be drawn
     * if borderStrokeColorProperty() (which determines the color) has a non-null value.
     * See {@link #borderStrokeColorProperty()} for more details.
     *
     * Can be styled from CSS using the "-rtfx-border-stroke-width" property.
     */
    public ObjectProperty<Number> borderStrokeWidthProperty() { return borderStrokeWidth; }

    public StrokeType getBorderStrokeType() { return borderStrokeType.get(); }
    public void setBorderStrokeType(StrokeType type) { borderStrokeType.set(type); }

    /**
     * The stroke type of the border stroke. The border stroke will only be drawn
     * if borderStrokeColorProperty() (which determines the color) has a non-null value.
     * See {@link #borderStrokeColorProperty()} for more details.
     *
     * Can be styled from CSS using the "-rtfx-border-stroke-type" property.
     */
    public ObjectProperty<StrokeType> borderStrokeTypeProperty() { return borderStrokeType; }

    public Number[] getBorderStrokeDashArray() { return borderStrokeDashArray.get(); }
    public void setBorderStrokeDashArray(Number[] array) { borderStrokeDashArray.set(array); }

    /**
     * The dash array used for drawing the border for a section of text. The border stroke will only be drawn
     * if borderStrokeColorProperty() (which determines the color) has a non-null value.
     * See {@link #borderStrokeColorProperty()} for more details.
     *
     * Can be styled from CSS using the "-rtfx-border-stroke-dash-array" property.
     */
    public ObjectProperty<Number[]> borderStrokeDashArrayProperty() { return borderStrokeDashArray; }

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

    public Number getUnderlineOffset() { return underlineOffset.get(); }
    public void setUnderlineOffset(Number width) { underlineOffset.set(width); }

    /**
     * The offset of the underline for a section of text.  If null or zero,
     * the underline will be drawn along the baseline of the text.
     *
     * Can be styled from CSS using the "-rtfx-underline-offset" property.
     *
     * <p>Note that the underline properties specified here are orthogonal to the {@link #underlineProperty()} inherited
     * from {@link Text}.  The underline properties defined here in {@link TextExt} will cause an underline to be
     * drawn if {@link #underlineWidthProperty()} is non-null and greater than zero, regardless of
     * the value of {@link #underlineProperty()}.</p>
     */
    public ObjectProperty<Number> underlineOffsetProperty() { return underlineOffset; }

    public Number getUnderlineWaveRadius() { return underlineWaveRadius.get(); }
    public void setUnderlineWaveRadius(Number radius) { underlineWaveRadius.set(radius); }

    /**
     * The arc radius used to draw a wavy underline.  If null or zero, the
     * underline will be a simple line.
     *
     * Can be styled from CSS using the "-rtfx-underline-wave-radius" property.
     *
     * <p>Note that the underline properties specified here are orthogonal to the {@link #underlineProperty()} inherited
     * from {@link Text}.  The underline properties defined here in {@link TextExt} will cause an underline to be
     * drawn if {@link #underlineWidthProperty()} is non-null and greater than zero, regardless of
     * the value of {@link #underlineProperty()}.</p>
     */
    public ObjectProperty<Number> underlineWaveRadiusProperty() { return underlineWaveRadius; }

    public Number getUnderlineDoubleGap() { return underlineDoubleGap.get(); }
    public void setUnderlineDoubleGap(Number radius) { underlineDoubleGap.set(radius); }

    /**
     * The size of the gap between two parallel underline lines or wave forms.  If null or zero, the
     * underline will be a single line or wave form.
     *
     * Can be styled from CSS using the "-rtfx-underline-double-gap" property.
     *
     * <p>Note that the underline properties specified here are orthogonal to the {@link #underlineProperty()} inherited
     * from {@link Text}.  The underline properties defined here in {@link TextExt} will cause an underline to be
     * drawn if {@link #underlineWidthProperty()} is non-null and greater than zero, regardless of
     * the value of {@link #underlineProperty()}.</p>
     */
    public ObjectProperty<Number> underlineDoubleGapProperty() { return underlineDoubleGap; }

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

        private static final CssMetaData<TextExt, Paint> BACKGROUND_COLOR = new CustomCssMetaData<>(
                "-rtfx-background-color", StyleConverter.getPaintConverter(),
                Color.TRANSPARENT, n -> n.backgroundColor
        );

        private static final CssMetaData<TextExt, Paint> BORDER_COLOR = new CustomCssMetaData<>(
                "-rtfx-border-stroke-color", StyleConverter.getPaintConverter(),
                Color.TRANSPARENT, n -> n.borderStrokeColor
        );

        private static final CssMetaData<TextExt, Number> BORDER_WIDTH = new CustomCssMetaData<>(
                "-rtfx-border-stroke-width", StyleConverter.getSizeConverter(),
                0, n -> n.borderStrokeWidth
        );

        private static final CssMetaData<TextExt, StrokeType> BORDER_TYPE = new CustomCssMetaData<>(
                "-rtfx-border-stroke-type", StyleConverter.getEnumConverter(StrokeType.class),
                StrokeType.INSIDE, n -> n.borderStrokeType
        );

        private static final CssMetaData<TextExt, Number[]> BORDER_DASH_ARRAY = new CustomCssMetaData<>(
                "-rtfx-border-stroke-dash-array", SizeConverter.SequenceConverter.getInstance(),
                new Double[0], n -> n.borderStrokeDashArray
        );

        private static final CssMetaData<TextExt, Paint> UNDERLINE_COLOR = new CustomCssMetaData<>(
                "-rtfx-underline-color", StyleConverter.getPaintConverter(),
                Color.TRANSPARENT, n -> n.underlineColor
        );

        private static final CssMetaData<TextExt, Number> UNDERLINE_WIDTH = new CustomCssMetaData<>(
                "-rtfx-underline-width", StyleConverter.getSizeConverter(),
                0, n -> n.underlineWidth
        );

        private static final CssMetaData<TextExt, Number> UNDERLINE_OFFSET = new CustomCssMetaData<>(
            "-rtfx-underline-offset", StyleConverter.getSizeConverter(),
            0, n -> n.underlineOffset
        );

        private static final CssMetaData<TextExt, Number> UNDERLINE_WAVE_RADIUS = new CustomCssMetaData<>(
            "-rtfx-underline-wave-radius", StyleConverter.getSizeConverter(),
            0, n -> n.underlineWaveRadius
        );

        private static final CssMetaData<TextExt, Number[]> UNDERLINE_DASH_ARRAY = new CustomCssMetaData<>(
                "-rtfx-underline-dash-array", SizeConverter.SequenceConverter.getInstance(),
                new Double[0], n -> n.underlineDashArray
        );

        private static final CssMetaData<TextExt, Number> UNDERLINE_DOUBLE_GAP = new CustomCssMetaData<>(
                "-rtfx-underline-double-gap", StyleConverter.getSizeConverter(),
                0, n -> n.underlineDoubleGap
        );

        private static final CssMetaData<TextExt, StrokeLineCap> UNDERLINE_CAP = new CustomCssMetaData<>(
                "-rtfx-underline-cap", StyleConverter.getEnumConverter(StrokeLineCap.class),
                StrokeLineCap.SQUARE, n -> n.underlineCap
        );
    }
}
