package org.fxmisc.richtext.skin;

import static org.reactfx.util.Tuples.*;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BiConsumer;

import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
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
    private final StyledTextAreaVisual<S> visual;
    private final BiConsumer<Text, S> applyStyle;
    private final InvalidationListener onWrapWidthChange = obs -> requestLayout();

    @SuppressWarnings("unchecked")
    private final MonadicObservableValue<ParagraphText<S>> textFlow = EasyBind.monadic(graphicProperty()).map(g -> (ParagraphText<S>) g);

    private final Property<Boolean> caretVisible = textFlow.selectProperty(ParagraphText::caretVisibleProperty);
    public Property<Boolean> caretVisibleProperty() { return caretVisible; }

    private final Property<Paint> highlightFill = textFlow.selectProperty(ParagraphText::highlightFillProperty);
    public Property<Paint> highlightFillProperty() { return highlightFill; }

    private final Property<Paint> highlightTextFill = textFlow.selectProperty(ParagraphText::highlightTextFillProperty);
    public Property<Paint> highlightTextFillProperty() { return highlightTextFill; }

    private final Property<Number> caretPosition = textFlow.selectProperty(ParagraphText::caretPositionProperty);
    public Property<Number> caretPositionProperty() { return caretPosition; }

    private final Property<IndexRange> selection = textFlow.selectProperty(ParagraphText::selectionProperty);
    public Property<IndexRange> selectionProperty() { return selection; }

    public ParagraphCell(StyledTextAreaVisual<S> visual, BiConsumer<Text, S> applyStyle) {
        this.visual = visual;
        this.applyStyle = applyStyle;

        emptyProperty().addListener((obs, wasEmpty, isEmpty) -> {
            if(wasEmpty && !isEmpty) {
                startListening();
            } else if(!wasEmpty && isEmpty) {
                stopListening();
            }
        });
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
            setGraphic(new ParagraphText<S>(item, applyStyle));
        } else {
            setGraphic(null);
        }
    }

    @Override
    protected double computePrefHeight(double width) {
        // XXX we cannot rely on the given width, because ListView does not pass
        // the correct width (https://javafx-jira.kenai.com/browse/RT-35041)
        // So we have to get the width by our own means.
        double w = getWrapWidth();

        return textFlow.getOpt()
                .map(t -> t.prefHeight(w) + snappedTopInset() + snappedBottomInset())
                .orElse(200.0); // go big so that we don't need to construct too many empty cells
    }

    @Override
    protected double computePrefWidth(double height) {
        return textFlow.getOpt()
                .map(t -> getWrapWidth() == Region.USE_COMPUTED_SIZE
                        ? t.prefWidth(-1.0) + snappedLeftInset() + snappedRightInset()
                        : 0)
                .orElse(super.computePrefWidth(height));
    }

    private double getWrapWidth() {
        double skinWrapWidth = visual.wrapWidth.get();
        if(skinWrapWidth == Region.USE_COMPUTED_SIZE) {
            return Region.USE_COMPUTED_SIZE;
        } else {
            return skinWrapWidth - snappedLeftInset() - snappedRightInset();
        }
    }

    private void startListening() {
        visual.wrapWidth.addListener(onWrapWidthChange);
    }

    private void stopListening() {
        visual.wrapWidth.removeListener(onWrapWidthChange);
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
            return textFlow.getOpt().flatMap(t -> t.hit(x - t.getLayoutX(), y));
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
        return textFlow.getOpt().flatMap(t -> t.hit(line, x));
    }

    public double getCaretOffsetX() {
        return textFlow.getOpt().map(ParagraphText::getCaretOffsetX).orElse(0.);
    }

    public int getLineCount() {
        return textFlow.getOpt().map(ParagraphText::getLineCount).orElse(0);
    }

    public int getCurrentLineIndex() {
        return textFlow.getOpt().map(ParagraphText::currentLineIndex).orElse(0);
    }

    public Optional<Bounds> getCaretBoundsOnScreen() {
        return textFlow.getOpt().map(ParagraphText::getCaretBoundsOnScreen);
    }

    public Optional<Bounds> getSelectionBoundsOnScreen() {
        return textFlow.getOpt().flatMap(ParagraphText::getSelectionBoundsOnScreen);
    }
}