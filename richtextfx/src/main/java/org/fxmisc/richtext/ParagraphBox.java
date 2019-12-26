package org.fxmisc.richtext;

import static org.reactfx.util.Tuples.*;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.text.TextFlow;

import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyledSegment;
import org.fxmisc.richtext.event.MouseStationaryHelper;
import org.reactfx.EventStream;
import org.reactfx.util.Either;
import org.reactfx.util.Tuple2;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

/**
 * Node responsible for rendering a single paragraph in the viewport, which may include a paragraph graphic factory
 * (an {@link IntFunction} that takes the paragraph's index as an argument and returns a node), and definitely
 * includes the segments of the paragraph itself. The paragraph graphic factory is often used to display
 * the paragraph's line number.
 *
 * @param <PS> paragraph style type
 * @param <SEG> segment type
 * @param <S> segment style type
 */
class ParagraphBox<PS, SEG, S> extends Region {

    /**
     * An opaque class representing horizontal caret offset.
     * Although it is just a wrapper around double, its purpose is to increase
     * type safety.
     */
    public static class CaretOffsetX {
        private final double value;

        private CaretOffsetX(double value) {
            this.value = value;
        }
    }

    private final ParagraphText<PS, SEG, S> text;

    private final ObjectProperty<IntFunction<? extends Node>> graphicFactory
            = new SimpleObjectProperty<>(null);
    public ObjectProperty<IntFunction<? extends Node>> graphicFactoryProperty() {
        return graphicFactory;
    }

    private final Val<Node> graphic;

    final DoubleProperty graphicOffset = new SimpleDoubleProperty(0.0);

    private final BooleanProperty wrapText = new SimpleBooleanProperty(false);
    public BooleanProperty wrapTextProperty() { return wrapText; }
    {
        wrapText.addListener((obs, old, w) -> requestLayout());
    }

    private final Var<Integer> index;
    public Val<Integer> indexProperty() { return index; }
    public void setIndex(int index) { this.index.setValue(index); }
    public int getIndex() { return index.getValue(); }

    public final ObservableSet<CaretNode> caretsProperty() { return text.caretsProperty(); }

    public final ObservableMap<Selection<PS, SEG, S>, SelectionPath> selectionsProperty() {
        return text.selectionsProperty();
    }

    ParagraphBox(Paragraph<PS, SEG, S> par, BiConsumer<TextFlow, PS> applyParagraphStyle,
                 Function<StyledSegment<SEG, S>, Node> nodeFactory) {
        this.getStyleClass().add("paragraph-box");
        this.text = new ParagraphText<>(par, nodeFactory);
        applyParagraphStyle.accept(this.text, par.getParagraphStyle());
        
        // start at -1 so that the first time it is displayed, the caret at pos 0 is not
        // accidentally removed from its parent and moved to this node's ParagraphText
        // before this node gets updated to its real index and therefore removes
        // caret from the SceneGraph completely
        this.index = Var.newSimpleVar(-1);

        getChildren().add(text);
        graphic = Val.combine(
                graphicFactory,
                this.index,
                (f, i) -> f != null && i > -1 ? f.apply(i) : null);
        graphic.addListener((obs, oldG, newG) -> {
            if(oldG != null) {
                getChildren().remove(oldG);
            }
            if(newG != null) {
                getChildren().add(newG);
            }
        });
        graphicOffset.addListener(obs -> requestLayout());
    }

    void dispose() {
        text.dispose();
    }

    @Override
    public String toString() {
        return String.format(
                "ParagraphBox@%s[%s|%s]",
                hashCode(), (graphic.isPresent() ? "#" : ""), text.getParagraph()
        );
    }

    public Property<Paint> highlightTextFillProperty() { return text.highlightTextFillProperty(); }

    Paragraph<PS, SEG, S> getParagraph() {
        return text.getParagraph();
    }
    
    Node getGraphic() {
        if(graphic.isPresent()) {
            return graphic.getValue();
        } else {
            return null;
        }
    }

    public EventStream<Either<Tuple2<Point2D, Integer>, Object>> stationaryIndices(Duration delay) {
        EventStream<Either<Point2D, Void>> stationaryEvents = new MouseStationaryHelper(this).events(delay);
        EventStream<Tuple2<Point2D, Integer>> hits = stationaryEvents.filterMap(Either::asLeft)
                .filterMap(p -> {
                    OptionalInt charIdx = hit(p).getCharacterIndex();
                    if(charIdx.isPresent()) {
                        return Optional.of(t(p, charIdx.getAsInt()));
                    } else {
                        return Optional.empty();
                    }
                });
        EventStream<?> stops = stationaryEvents.filter(Either::isRight).map(Either::getRight);
        return hits.or(stops);
    }

    public CharacterHit hit(Point2D pos) {
        return hit(pos.getX(), pos.getY());
    }

    public CharacterHit hit(double x, double y) {
        Point2D onScreen = this.localToScreen(x, y);
        Point2D inText = text.screenToLocal(onScreen);
        Insets textInsets = text.getInsets();
        return text.hit(inText.getX() - textInsets.getLeft(), inText.getY() - textInsets.getTop());
    }

    public <T extends Node & Caret> CaretOffsetX getCaretOffsetX(T caret) {
        layout(); // ensure layout, is a no-op if not dirty
        return new CaretOffsetX(text.getCaretOffsetX(caret));
    }

    public int getCurrentLineStartPosition(Caret caret) {
        layout(); // ensure layout, is a no-op if not dirty
        return text.getCurrentLineStartPosition(caret);
    }

    public int getCurrentLineEndPosition(Caret caret) {
        layout(); // ensure layout, is a no-op if not dirty
        return text.getCurrentLineEndPosition(caret);
    }

    public int getLineCount() {
        layout(); // ensure layout, is a no-op if not dirty
        return text.getLineCount();
    }

    public int getCurrentLineIndex(Caret caret) {
        layout(); // ensure layout, is a no-op if not dirty
        return text.currentLineIndex(caret);
    }

    public int getCurrentLineIndex(int position) {
        layout(); // ensure layout, is a no-op if not dirty
        return text.currentLineIndex(position);
    }

    public <T extends Node & Caret> Bounds getCaretBounds(T caret) {
        layout(); // ensure layout, is a no-op if not dirty
        Bounds b = text.getCaretBounds(caret);
        return text.localToParent(b);
    }

    public <T extends Node & Caret> Bounds getCaretBoundsOnScreen(T caret) {
        layout(); // ensure layout, is a no-op if not dirty
        return text.getCaretBoundsOnScreen(caret);
    }

    public Optional<Bounds> getSelectionBoundsOnScreen(Selection<PS, SEG, S> selection) {
        layout(); // ensure layout, is a no-op if not dirty
        return text.getSelectionBoundsOnScreen(selection);
    }

    public Bounds getRangeBoundsOnScreen(int from, int to) {
        layout(); // ensure layout, is a no-op if not dirty
        return text.getRangeBoundsOnScreen(from, to);
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
                : getGraphicPrefWidth() + text.prefWidth(-1) + insets.getLeft() + insets.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets insets = getInsets();
        double overhead = getGraphicPrefWidth() + insets.getLeft() + insets.getRight();
        return text.prefHeight(width - overhead) + insets.getTop() + insets.getBottom() + text.getLineSpacing();
    }

    @Override
    protected void layoutChildren() {
        Insets ins = getInsets();
        double w = getWidth() - ins.getLeft() - ins.getRight();
        double h = getHeight() - ins.getTop() - ins.getBottom();
        double graphicWidth = getGraphicPrefWidth();
        double half = text.getLineSpacing() / 2.0;

        text.resizeRelocate(graphicWidth + ins.getLeft(), ins.getTop() + half, w - graphicWidth, h - half);

        graphic.ifPresent(g -> g.resizeRelocate(graphicOffset.get() + ins.getLeft(), ins.getTop(), graphicWidth, h));
    }

    double getGraphicPrefWidth() {
        if(graphic.isPresent()) {
            return graphic.getValue().prefWidth(-1);
        } else {
            return 0.0;
        }
    }

    /**
     * Hits the embedded TextFlow at the given line and x offset.
     *
     * @param x x coordinate relative to the embedded TextFlow.
     * @param line index of the line in the embedded TextFlow.
     * @return hit info for the given line and x coordinate
     */
    CharacterHit hitTextLine(CaretOffsetX x, int line) {
        return text.hitLine(x.value, line);
    }

    /**
     * Hits the embedded TextFlow at the given x and y offset.
     *
     * @return hit info for the given x and y coordinates
     */
    CharacterHit hitText(CaretOffsetX x, double y) {
        return text.hit(x.value, y);
    }
}
