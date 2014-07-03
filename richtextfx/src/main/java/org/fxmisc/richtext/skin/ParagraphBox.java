package org.fxmisc.richtext.skin;

import static org.reactfx.util.Tuples.*;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicBinding;
import org.fxmisc.richtext.Paragraph;
import org.fxmisc.richtext.util.MouseStationaryHelper;
import org.reactfx.EventStream;
import org.reactfx.util.Either;
import org.reactfx.util.Tuple2;

import com.sun.javafx.scene.text.HitInfo;

class ParagraphBox<S> extends Region {

    private final ParagraphText<S> text;

    private final ObjectProperty<IntFunction<? extends Node>> graphicFactory
            = new SimpleObjectProperty<>(null);
    public ObjectProperty<IntFunction<? extends Node>> graphicFactoryProperty() {
        return graphicFactory;
    }

    private final MonadicBinding<Node> graphic;

    private final BooleanProperty wrapText = new SimpleBooleanProperty(false);
    public BooleanProperty wrapTextProperty() { return wrapText; }
    {
        wrapText.addListener((obs, old, w) -> requestLayout());
    }

    private final IntegerProperty index;
    public IntegerProperty indexProperty() { return index; }
    public void setIndex(int index) { this.index.set(index); }
    public int getIndex() { return index.get(); }

    public ParagraphBox(int index, Paragraph<S> par, BiConsumer<Text, S> applyStyle) {
        this.getStyleClass().add("paragraph-box");
        this.text = new ParagraphText<>(par, applyStyle);
        this.index = new SimpleIntegerProperty(index);
        getChildren().add(text);
        graphic = EasyBind.combine(graphicFactory, this.index, (f, i) -> f != null ? f.apply(i.intValue()) : null);
        graphic.addListener((obs, oldG, newG) -> {
            if(oldG != null) {
                getChildren().remove(oldG);
            }
            if(newG != null) {
                getChildren().add(newG);
            }
        });
        boundsInParentProperty().addListener((obs, old, bounds) -> {
            graphic.ifPresent(g -> g.relocate(-bounds.getMinX(), 0));
        });
    }

    public Property<Boolean> caretVisibleProperty() { return text.caretVisibleProperty(); }

    public Property<Paint> highlightFillProperty() { return text.highlightFillProperty(); }

    public Property<Paint> highlightTextFillProperty() { return text.highlightTextFillProperty(); }

    public Property<Number> caretPositionProperty() { return text.caretPositionProperty(); }

    public Property<IndexRange> selectionProperty() { return text.selectionProperty(); }

    Paragraph<S> getParagraph() {
        return text.getParagraph();
    }

    public EventStream<Either<Tuple2<Point2D, Integer>, Object>> stationaryIndices(Duration delay) {
        EventStream<Either<Point2D, Void>> stationaryEvents = new MouseStationaryHelper(this).events(delay);
        EventStream<Tuple2<Point2D, Integer>> hits = stationaryEvents.filterMap(Either::asLeft)
                .<Tuple2<Point2D, Integer>>filterMap(pos -> hit(pos).map(hit -> t(pos, hit.getCharIndex())));
        EventStream<?> stops = stationaryEvents.filterMap(Either::isRight, Either::getRight);
        return hits.or(stops);
    }

    /**
     * Returns a HitInfo for the given mouse event.
     *
     * Empty optional is returned if clicked beyond the end of this cell's text,
     */
    public Optional<HitInfo> hit(MouseEvent e) {
        return hit(e.getX(), e.getY());
    }

    public double getCaretOffsetX() {
        return text.getCaretOffsetX();
    }

    public int getLineCount() {
        return text.getLineCount();
    }

    public int getCurrentLineIndex() {
        return text.currentLineIndex();
    }

    public Bounds getCaretBounds() {
        Bounds b = text.getCaretBounds();
        return text.localToParent(b);
    }

    public Bounds getCaretBoundsOnScreen() {
        return text.getCaretBoundsOnScreen();
    }

    public Optional<Bounds> getSelectionBoundsOnScreen() {
        return text.getSelectionBoundsOnScreen();
    }

    @Override
    protected double computeMinWidth(double ignoredHeight) {
        return computePrefWidth(-1);
    }

    @Override
    protected double computePrefWidth(double ignoredHeight) {
        Insets insets = getInsets();
        return wrapText.get()
                ? 0 // return 0, VirtualFlow will size it to its width anyway
                : getGraphicWidth() + text.prefWidth(-1) + insets.getLeft() + insets.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets insets = getInsets();
        return text.prefHeight(width) + insets.getTop() + insets.getBottom();
    }

    @Override
    protected
    void layoutChildren() {
        Bounds bounds = getLayoutBounds();
        double w = bounds.getWidth();
        double h = bounds.getHeight();
        double graphicWidth = getGraphicWidth();
        text.resizeRelocate(graphicWidth, 0, w - graphicWidth, h);

        graphic.ifPresent(g -> {
            g.resize(graphicWidth, h);
        });
    }

    double getGraphicWidth() {
        return graphic.getOpt().map(g -> g.prefWidth(-1)).orElse(0.0);
    }

    /**
     * Hits the embedded TextFlow at the given line and x offset.
     *
     * @param x x coordinate relative to the TextFlow, not relative to the cell.
     * @return HitInfo for the given line and x coordinate, or an empty
     * optional if hit beyond the end.
     */
    Optional<HitInfo> hitText(double x, int line) {
        return text.hit(x, line);
    }

    /**
     * Hits the embedded TextFlow at the given x and y offset. Offsets are
     * relative to the embedded TextFlow, not relative to this ParagraphBox.
     *
     * @return HitInfo for the given x and y coordinates, or an empty
     * optional if hit beyond the end.
     */
    Optional<HitInfo> hitText(double x, double y) {
        return text.hit(x, y);
    }

    private Optional<HitInfo> hit(Point2D pos) {
        return hit(pos.getX(), pos.getY());
    }

    private Optional<HitInfo> hit(double x, double y) {
        Point2D onScreen = this.localToScreen(x, y);
        Point2D inText = text.screenToLocal(onScreen);
        return text.hit(inText.getX(), inText.getY());
    }
}
