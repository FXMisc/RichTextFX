package org.fxmisc.richtext.skin;

import static org.reactfx.util.Tuples.*;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicObservableValue;
import org.fxmisc.richtext.Paragraph;
import org.fxmisc.richtext.util.MouseStationaryHelper;
import org.reactfx.EitherEventStream;
import org.reactfx.util.Either;
import org.reactfx.util.Tuple2;

import com.sun.javafx.scene.text.HitInfo;

class ParagraphCell<S> extends ListCell<Paragraph<S>> {
    private final BiConsumer<Text, S> applyStyle;

    @SuppressWarnings("unchecked")
    private final MonadicObservableValue<ParagraphBox<S>> box = EasyBind.monadic(graphicProperty()).map(g -> (ParagraphBox<S>) g);

    private final MonadicObservableValue<ParagraphText<S>> text = box.map(ParagraphBox::getText);

    private final Property<Boolean> caretVisible = text.selectProperty(ParagraphText::caretVisibleProperty);
    public Property<Boolean> caretVisibleProperty() { return caretVisible; }

    private final Property<Paint> highlightFill = text.selectProperty(ParagraphText::highlightFillProperty);
    public Property<Paint> highlightFillProperty() { return highlightFill; }

    private final Property<Paint> highlightTextFill = text.selectProperty(ParagraphText::highlightTextFillProperty);
    public Property<Paint> highlightTextFillProperty() { return highlightTextFill; }

    private final Property<Number> caretPosition = text.selectProperty(ParagraphText::caretPositionProperty);
    public Property<Number> caretPositionProperty() { return caretPosition; }

    private final Property<IndexRange> selection = text.selectProperty(ParagraphText::selectionProperty);
    public Property<IndexRange> selectionProperty() { return selection; }

    private final DoubleProperty wrapWidth = new SimpleDoubleProperty(Region.USE_COMPUTED_SIZE);
    public DoubleProperty wrapWidthProperty() { return wrapWidth; }
    {
        wrapWidth.addListener((obs, old, w) -> requestLayout());
    }

    private final Property<Supplier<? extends Node>> graphicFactory
            = box.selectProperty(ParagraphBox::graphicFactoryProperty);
    public Property<Supplier<? extends Node>> graphicFactoryProperty() {
        return graphicFactory;
    }

    public ParagraphCell(BiConsumer<Text, S> applyStyle) {
        this.applyStyle = applyStyle;

        DoubleBinding childWrapWidth = Bindings.createDoubleBinding(() -> {
            return wrapWidth.get() == Region.USE_COMPUTED_SIZE
                    ? Region.USE_COMPUTED_SIZE
                    : wrapWidth.get() - this.getInsets().getLeft() - this.getInsets().getRight();

        }, wrapWidth, insetsProperty());
        box.selectProperty(ParagraphBox::wrapWidthProperty).bind(childWrapWidth);
    }

    public EitherEventStream<Tuple2<Point2D, Integer>, Void> stationaryIndices(Duration delay) {
        return new MouseStationaryHelper(this)
                .events(delay)
                .mapLeft(pos -> hit(pos).<Tuple2<Point2D, Integer>>map(hit -> t(pos, hit.getCharIndex())))
                .<Tuple2<Point2D, Integer>>splitLeft(Either::leftOrNull)
                .distinct();
    }

    @Override
    protected void updateItem(Paragraph<S> item, boolean empty) {
        super.updateItem(item, empty);

        if(!empty) {
            setGraphic(new ParagraphBox<S>(item, applyStyle));
        } else {
            setGraphic(null);
        }
    }

    @Override
    protected double computePrefHeight(double ignoredWidth) {
        return box.getOpt()
                .map(b -> {
                    Insets insets = getInsets();
                    double boxHeight = wrapWidth.get() == Region.USE_COMPUTED_SIZE
                            ? b.prefHeight(-1)
                            : b.prefHeight(wrapWidth.get() - insets.getLeft() - insets.getRight());
                    return boxHeight + insets.getTop() + insets.getBottom();
                })
                .orElse(200.0); // go big so that we don't need to construct too many empty cells
    }

    @Override
    protected double computePrefWidth(double height) {
        return box.getOpt()
                .map(b -> {
                    Insets insets = getInsets();
                    return wrapWidth.get() == Region.USE_COMPUTED_SIZE
                            ? b.prefWidth(-1.0) + insets.getLeft() + insets.getRight()
                            : 0; // return 0, ListView will size it to its width anyway
                })
                .orElse(super.computePrefWidth(height));
    }

    /**
     * Returns a HitInfo for the given mouse event.
     *
     * Empty optional is returned if this cell is empty, or if clicked beyond
     * the end of this cell's text,
     */
    public Optional<HitInfo> hit(MouseEvent e) {
        return hit(e.getX(), e.getY());
    }

    private Optional<HitInfo> hit(Point2D pos) {
        return hit(pos.getX(), pos.getY());
    }

    private Optional<HitInfo> hit(double x, double y) {
        if(isEmpty()) { // hit beyond the last line
            return Optional.empty();
        } else {
            return text.getOpt().flatMap(t -> {
                Point2D onScreen = this.localToScreen(x, y);
                Point2D inText = t.screenToLocal(onScreen);
                return t.hit(inText.getX(), inText.getY());
            });
        }
    }

    /**
     * Hits the embedded TextFlow at the given line and x offset.
     * Assumes this cell is non-empty.
     *
     * @param x x coordinate relative to the TextFlow, not relative to the cell.
     * @return HitInfo for the given line and x coordinate, or an empty
     * optional if hit beyond the end.
     */
    Optional<HitInfo> hitText(int line, double x) {
        return text.getOpt().flatMap(t -> t.hit(line, x));
    }

    public double getCaretOffsetX() {
        return text.getOpt().map(ParagraphText::getCaretOffsetX).orElse(0.);
    }

    public int getLineCount() {
        return text.getOpt().map(ParagraphText::getLineCount).orElse(0);
    }

    public int getCurrentLineIndex() {
        return text.getOpt().map(ParagraphText::currentLineIndex).orElse(0);
    }

    public Optional<Bounds> getCaretBoundsOnScreen() {
        return text.getOpt().map(ParagraphText::getCaretBoundsOnScreen);
    }

    public Optional<Bounds> getSelectionBoundsOnScreen() {
        return text.getOpt().flatMap(ParagraphText::getSelectionBoundsOnScreen);
    }
}