package org.fxmisc.richtext;

import javafx.beans.property.ObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.scene.control.IndexRange;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import org.reactfx.value.Val;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Path used to render a portion of a mulit-paragraph selection or all of a single-paragraph selection
 * with additional CSS styling; it does not have a style class associated with it.
 */
public class SelectionPath extends Path {

    private final StyleableObjectProperty<Paint> highlightFill
            = new CustomStyleableProperty<>(Color.DODGERBLUE, "highlightFill", this, HIGHLIGHT_FILL);

    /**
     * Background fill for highlighted/selected text. Can be styled using "-fx-highlight-fill".
     */
    public final ObjectProperty<Paint> highlightFillProperty() { return highlightFill; }
    public final Paint getHighlightFill() { return highlightFill.get(); }
    public final void setHighlightFill(Paint paint) { highlightFill.set(paint); }

    private final Val<IndexRange> range;
    final Val<IndexRange> rangeProperty() { return range; }

    SelectionPath(Val<IndexRange> range) {
        setManaged(false);
        this.range = range;
        highlightFill.addListener( (ob,ov,nv) -> setFill( nv ) );
        setFill( getHighlightFill() );
        setStrokeWidth( 0.0 );
    }

    @Override
    public String toString() {
        return String.format(
                "SelectionPath(styleclass=%s path=%s", getStyleClass(), super.toString()
        );
    }

    private static final CssMetaData<SelectionPath, Paint> HIGHLIGHT_FILL = new CustomCssMetaData<>(
            "-fx-highlight-fill", StyleConverter.getPaintConverter(), Color.DODGERBLUE, s -> s.highlightFill
    );

    private static final List<CssMetaData<? extends Styleable, ?>> CSS_META_DATA_LIST;

    static {
        List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Path.getClassCssMetaData());

        styleables.add(HIGHLIGHT_FILL);

        CSS_META_DATA_LIST = Collections.unmodifiableList(styleables);
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return CSS_META_DATA_LIST;
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS_META_DATA_LIST;
    }
}
