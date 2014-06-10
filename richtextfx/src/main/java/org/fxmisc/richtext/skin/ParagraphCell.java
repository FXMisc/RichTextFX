package org.fxmisc.richtext.skin;

import static org.reactfx.util.Tuples.*;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BiConsumer;

import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicObservableValue;
import org.fxmisc.richtext.Paragraph;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.util.MouseStationaryHelper;
import org.reactfx.EitherEventStream;
import org.reactfx.util.Either;
import org.reactfx.util.Tuple2;

import com.sun.javafx.scene.text.HitInfo;

public class ParagraphCell<S> extends ListCell<Paragraph<S>> {
    private final StyledTextAreaVisual<S> visual;
    private final BiConsumer<Text, S> applyStyle;
    private final InvalidationListener onWrapWidthChange = obs -> requestLayout();

    @SuppressWarnings("unchecked")
    private final MonadicObservableValue<ParagraphGraphic<S>> graphic = EasyBind.monadic(graphicProperty()).map(g -> (ParagraphGraphic<S>) g);

    private final Property<Boolean> caretVisible = graphic.selectProperty(ParagraphGraphic::caretVisibleProperty);
    public Property<Boolean> caretVisibleProperty() { return caretVisible; }

    private final Property<Paint> highlightFill = graphic.selectProperty(ParagraphGraphic::highlightFillProperty);
    public Property<Paint> highlightFillProperty() { return highlightFill; }

    private final Property<Paint> highlightTextFill = graphic.selectProperty(ParagraphGraphic::highlightTextFillProperty);
    public Property<Paint> highlightTextFillProperty() { return highlightTextFill; }

    private final Property<Number> caretPosition = graphic.selectProperty(ParagraphGraphic::caretPositionProperty);
    public Property<Number> caretPositionProperty() { return caretPosition; }

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
            ParagraphGraphic<S> graphic = new ParagraphGraphic<S>(item, applyStyle);

            StyledTextArea<S> area = visual.getArea();
            graphic.setSelection(area.getParagraphSelection(getIndex()));

            setGraphic(graphic);
        } else {
            setGraphic(null);
        }
    }

    @Override
    protected double computePrefHeight(double width) {
        // XXX we cannot rely on the given width, because ListView does not pass
        // the correct width (https://javafx-jira.kenai.com/browse/RT-35041)
        // So we have to get the width by our own means.
        width = getWrapWidth();

        if(isEmpty()) {
            // go big so that we don't need to construct too many empty cells
            return 200;
        } else {
            return getParagraphGraphic().prefHeight(width) + snappedTopInset() + snappedBottomInset();
        }
    }

    @Override
    protected double computePrefWidth(double height) {
        if(isEmpty()) {
            return super.computePrefWidth(height);
        } else if(getWrapWidth() == Region.USE_COMPUTED_SIZE) {
                return getParagraphGraphic().prefWidth(-1.0) + snappedLeftInset() + snappedRightInset();
        } else {
            return 0;
        }
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
            ParagraphGraphic<S> textFlow = getParagraphGraphic();
            return textFlow.hit(x - textFlow.getLayoutX(), y);
        }
    }

    /**
     * Hits the embedded TextFlow at the given line and x offset.
     * Assumes this cell is non-empty.
     *
     * @param x x coordinate relative to the graphic (TextFlow),
     * not relative to the cell.
     * @return HitInfo for the given line and x coordinate, or an empty
     * optional if hit beyond the end.
     */
    Optional<HitInfo> hitGraphic(int line, double x) {
        return getParagraphGraphic().hit(line, x);
    }

    public double getCaretOffsetX() {
        return getParagraphGraphic().getCaretOffsetX();
    }

    ParagraphGraphic<S> getParagraphGraphic() {
        Optional<ParagraphGraphic<S>> graphic = tryGetParagraphGraphic();
        if(graphic.isPresent()) {
            return graphic.get();
        } else {
            throw new AssertionError("There's no graphic in this cell");
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<ParagraphGraphic<S>> tryGetParagraphGraphic() {
        Node graphic = getGraphic();
        if(graphic != null) {
            return Optional.of((ParagraphGraphic<S>) graphic);
        } else {
            return Optional.empty();
        }
    }

    public int getLineCount() {
        return getParagraphGraphic().getLineCount();
    }

    public int getCurrentLineIndex() {
        return getParagraphGraphic().currentLineIndex();
    }
}