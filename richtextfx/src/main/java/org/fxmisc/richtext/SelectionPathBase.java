package org.fxmisc.richtext;

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
 * A base class for a path which describes a selection shape in the Scene graph; it does not have a style class
 * associated with it.
 */
public abstract class SelectionPathBase extends Path {

    /**
     * Background fill for highlighted text.
     */
    private final StyleableObjectProperty<Paint> highlightFill
            = new CustomStyleableProperty<>(Color.DODGERBLUE, "highlightFill", this, HIGHLIGHT_FILL);
    public final Paint getHighlightFill() { return highlightFill.get(); }
    public final void setHighlightFill(Paint paint) { highlightFill.set(paint); }

    private final Val<IndexRange> range;
    public final Val<IndexRange> rangeProperty() { return range; }

    public SelectionPathBase(Val<IndexRange> range) {
        setManaged(false);
        this.range = range;
    }

    /**
     * Disposes this path and prevents memory leaks
     */
    public void dispose() {
        // do nothing by default
    }

    private static final CssMetaData<SelectionPathBase, Paint> HIGHLIGHT_FILL = new CustomCssMetaData<>(
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
