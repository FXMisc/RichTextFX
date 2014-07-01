package org.fxmisc.richtext.skin;

import static org.reactfx.EventStreams.*;
import static org.reactfx.util.Tuples.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.event.Event;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.PopupWindow;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicObservableValue;
import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.richtext.MouseOverTextEvent;
import org.fxmisc.richtext.Paragraph;
import org.fxmisc.richtext.PopupAlignment;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TwoDimensional.Position;
import org.fxmisc.richtext.TwoLevelNavigator;
import org.fxmisc.richtext.skin.CssProperties.HighlightFillProperty;
import org.fxmisc.richtext.skin.CssProperties.HighlightTextFillProperty;
import org.fxmisc.richtext.util.skin.SimpleVisual;
import org.reactfx.EventSource;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;
import org.reactfx.util.Tuple2;

import com.sun.javafx.scene.text.HitInfo;

/**
 * Code area skin.
 */
public class StyledTextAreaVisual<S> implements SimpleVisual {

    /* ********************************************************************** *
     *                                                                        *
     * Properties                                                             *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Background fill for highlighted text.
     */
    private final StyleableObjectProperty<Paint> highlightFill
            = new HighlightFillProperty(this, Color.DODGERBLUE);

    /**
     * Text color for highlighted text.
     */
    private final StyleableObjectProperty<Paint> highlightTextFill
            = new HighlightTextFillProperty(this, Color.WHITE);


    /* ********************************************************************** *
     *                                                                        *
     * Event streams                                                          *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Stream of all mouse events on all cells. May be used by behavior.
     */
    private final EventStream<Tuple2<ParagraphBox<S>, MouseEvent>> cellMouseEvents;
    final EventStream<Tuple2<ParagraphBox<S>, MouseEvent>> cellMouseEvents() {
        return cellMouseEvents;
    }


    /* ********************************************************************** *
     *                                                                        *
     * Private fields                                                         *
     *                                                                        *
     * ********************************************************************** */

    private final StyledTextArea<S> area;

    private Subscription subscriptions = () -> {};

    private final BooleanPulse caretPulse = new BooleanPulse(javafx.util.Duration.seconds(.5));

    private final BooleanBinding caretVisible;

    private final VirtualFlow<Paragraph<S>, Cell<Paragraph<S>, ParagraphBox<S>>> virtualFlow;

    // used for two-level navigation, where on the higher level are
    // paragraphs and on the lower level are lines within a paragraph
    private final TwoLevelNavigator navigator;


    /* ********************************************************************** *
     *                                                                        *
     * Constructors                                                           *
     *                                                                        *
     * ********************************************************************** */

    public StyledTextAreaVisual(
            StyledTextArea<S> styledTextArea,
            BiConsumer<Text, S> applyStyle) {
        this.area = styledTextArea;

        // load the default style
        area.getStylesheets().add(StyledTextAreaVisual.class.getResource("styled-text-area.css").toExternalForm());

        // keeps track of currently used non-empty cells
        @SuppressWarnings("unchecked")
        ObservableSet<ParagraphBox<S>> nonEmptyCells = FXCollections.observableSet();

        // Initialize content
        virtualFlow = VirtualFlow.createVertical(
                area.getParagraphs(),
                (index, par) -> {
                    Cell<Paragraph<S>, ParagraphBox<S>> cell = createCell(index, par, applyStyle);
                    nonEmptyCells.add(cell.getNode());
                    return cell.beforeReset(() -> nonEmptyCells.remove(cell.getNode()))
                            .afterUpdateItem((i, p) -> nonEmptyCells.add(cell.getNode()));
                });

        // initialize navigator
        IntSupplier cellCount = () -> area.getParagraphs().size();
        IntUnaryOperator cellLength = i -> virtualFlow.getCell(i).getNode().getLineCount();
        navigator = new TwoLevelNavigator(cellCount, cellLength);

        // emits a value every time the area is done updating
        EventStream<?> areaDoneUpdating = area.beingUpdatedProperty().offs();

        // follow the caret every time the caret position or paragraphs change
        EventStream<Void> caretPosDirty = invalidationsOf(area.caretPositionProperty());
        EventStream<Void> paragraphsDirty = invalidationsOf(area.getParagraphs());
        EventStream<Void> selectionDirty = invalidationsOf(area.selectionProperty());
        // need to reposition popup even when caret hasn't moved, but selection has changed (been deselected)
        EventStream<Void> caretDirty = merge(caretPosDirty, paragraphsDirty, selectionDirty);
        EventSource<Void> positionPopupImpulse = new EventSource<>();
        subscribeTo(caretDirty.emitOn(areaDoneUpdating), x -> {
            followCaret();
            positionPopupImpulse.push(null);
        });

        // blink caret only when focused
        listenTo(area.focusedProperty(), (obs, old, isFocused) -> {
            if(isFocused)
                caretPulse.start(true);
            else
                caretPulse.stop(false);
        });
        if(area.isFocused()) {
            caretPulse.start(true);
        }
        manageSubscription(() -> caretPulse.stop());

        // The caret is visible in periodic intervals, but only when
        // the code area is focused, editable and not disabled.
        caretVisible = caretPulse
                .and(area.focusedProperty())
                .and(area.editableProperty())
                .and(area.disabledProperty().not());
        manageBinding(caretVisible);

        // Adjust popup anchor by either a user-provided function,
        // or user-provided offset, or don't adjust at all.
        MonadicObservableValue<UnaryOperator<Point2D>> userFunction =
                EasyBind.monadic(area.popupAnchorAdjustmentProperty());
        MonadicObservableValue<UnaryOperator<Point2D>> userOffset =
                EasyBind.monadic(area.popupAnchorOffsetProperty())
                        .map(offset -> anchor -> anchor.add(offset));
        ObservableValue<UnaryOperator<Point2D>> popupAnchorAdjustment = userFunction
                .orElse(userOffset)
                .orElse(UnaryOperator.identity());

        // Position popup window whenever the window itself, its alignment,
        // or the position adjustment function changes.
        manageSubscription(EventStreams.combine(
                EventStreams.valuesOf(area.popupWindowProperty()),
                EventStreams.valuesOf(area.popupAlignmentProperty()),
                EventStreams.valuesOf(popupAnchorAdjustment))
            .repeatOn(positionPopupImpulse)
            .filter((w, al, adj) -> w != null)
            .subscribe((w, al, adj) -> positionPopup(w, al, adj)));

        // dispatch MouseOverTextEvents when mouseOverTextDelay is not null
        EventStreams.valuesOf(area.mouseOverTextDelayProperty())
                .flatMap(delay -> delay != null
                        ? mouseOverTextEvents(nonEmptyCells, delay)
                        : EventStreams.never())
                .hook(evt -> Event.fireEvent(area, evt))
                .pin();

        // initialize stream of all mouse events on all cells
        cellMouseEvents = merge(
                nonEmptyCells,
                c -> eventsOf(c, MouseEvent.ANY).map(e -> t(c, e)));
    }


    /* ********************************************************************** *
     *                                                                        *
     * Public API (from Visual)                                               *
     *                                                                        *
     * ********************************************************************** */

    @Override
    public Node getNode() {
        return virtualFlow;
    }

    @Override
    public void dispose() {
        subscriptions.unsubscribe();
        virtualFlow.dispose();
    }


    /* ********************************************************************** *
     *                                                                        *
     * Look &amp; feel                                                        *
     *                                                                        *
     * ********************************************************************** */

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return Arrays.<CssMetaData<? extends Styleable, ?>>asList(
                highlightFill.getCssMetaData(),
                highlightTextFill.getCssMetaData());
    }


    /* ********************************************************************** *
     *                                                                        *
     * Actions                                                                *
     *                                                                        *
     * ********************************************************************** */

    void showAsFirst(int index) {
        virtualFlow.showAsFirst(index);
    }

    void showAsLast(int index) {
        virtualFlow.showAsLast(index);
    }


    /* ********************************************************************** *
     *                                                                        *
     * Queries                                                                *
     *                                                                        *
     * ********************************************************************** */

    int getFirstVisibleIndex() {
        return virtualFlow.getVisibleRange().getStart();
    }

    int getLastVisibleIndex() {
        return virtualFlow.getVisibleRange().getEnd() - 1;
    }

    double getCaretOffsetX() {
        int idx = area.getCurrentParagraph();
        return idx == -1 ? 0 : getCell(idx).getCaretOffsetX();
    }

    int getInsertionIndex(Position targetLine, double x) {
        int parIdx = targetLine.getMajor();
        ParagraphBox<S> cell = virtualFlow.getCell(parIdx).getNode();
        int parInsertionIndex = getCellInsertionIndex(cell, targetLine.getMinor(), x);
        return getParagraphOffset(parIdx) + parInsertionIndex;
    }

    /**
     * Returns the current line as a two-level index.
     * The major number is the paragraph index, the minor
     * number is the line number within the paragraph.
     *
     * <p>This method has a side-effect of bringing the current
     * paragraph to the viewport if it is not already visible.
     */
    Position currentLine() {
        int parIdx = area.getCurrentParagraph();
        Cell<Paragraph<S>, ParagraphBox<S>> cell = virtualFlow.getCell(parIdx);
        int lineIdx = cell.getNode().getCurrentLineIndex();
        return position(parIdx, lineIdx);
    }

    Position position(int par, int line) {
        return navigator.position(par, line);
    }


    /* ********************************************************************** *
     *                                                                        *
     * Private methods                                                        *
     *                                                                        *
     * ********************************************************************** */

    private Cell<Paragraph<S>, ParagraphBox<S>> createCell(
            int index,
            Paragraph<S> paragraph,
            BiConsumer<Text, S> applyStyle) {

        ParagraphBox<S> box = new ParagraphBox<>(index, paragraph, applyStyle);

        box.highlightFillProperty().bind(highlightFill);
        box.highlightTextFillProperty().bind(highlightTextFill);
        box.wrapTextProperty().bind(area.wrapTextProperty());
        box.graphicFactoryProperty().bind(area.paragraphGraphicFactoryProperty());

        BooleanBinding hasCaret = Bindings.equal(
                box.indexProperty(),
                area.currentParagraphProperty());

        // caret is visible only in the paragraph with the caret
        BooleanBinding cellCaretVisible = hasCaret.and(caretVisible);
        box.caretVisibleProperty().bind(cellCaretVisible);

        // bind cell's caret position to area's caret column,
        // when the cell is the one with the caret
        org.fxmisc.easybind.Subscription caretPositionSub =
                EasyBind.bindConditionally(
                        box.caretPositionProperty(),
                        area.caretColumnProperty(),
                        hasCaret);

        // keep paragraph selection updated
        ObjectBinding<IndexRange> cellSelection = Bindings.createObjectBinding(() -> {
            int idx = box.getIndex();
            return idx != -1
                    ? area.getParagraphSelection(idx)
                    : StyledTextArea.EMPTY_RANGE;
        }, area.selectionProperty(), box.indexProperty());
        box.selectionProperty().bind(cellSelection);

        return new Cell<Paragraph<S>, ParagraphBox<S>>() {
            @Override
            public ParagraphBox<S> getNode() {
                return box;
            }

            @Override
            public void updateIndex(int index) {
                box.setIndex(index);
            }

            @Override
            public void dispose() {
                box.highlightFillProperty().unbind();
                box.highlightTextFillProperty().unbind();
                box.wrapTextProperty().unbind();
                box.graphicFactoryProperty().unbind();

                box.caretVisibleProperty().unbind();
                cellCaretVisible.dispose();
                hasCaret.dispose();
                caretPositionSub.unsubscribe();

                box.selectionProperty().unbind();
                cellSelection.dispose();
            }
        };
    }

//    private void cellCreated(ParagraphCell<S> cell) {
//        BooleanBinding hasCaret = Bindings.equal(
//                cell.indexProperty(),
//                area.currentParagraphProperty());
//
//        // caret is visible only in the paragraph with the caret
//        cell.caretVisibleProperty().bind(hasCaret.and(caretVisible));

//        cell.highlightFillProperty().bind(highlightFill);
//        cell.highlightTextFillProperty().bind(highlightTextFill);

//        // bind cell's caret position to area's caret column,
//        // when the cell is the one with the caret
//        EasyBind.bindConditionally(
//                cell.caretPositionProperty(),
//                area.caretColumnProperty(),
//                hasCaret);

//        // keep paragraph selection updated
//        ObjectBinding<IndexRange> cellSelection = Bindings.createObjectBinding(() -> {
//            int idx = cell.getIndex();
//            return idx != -1
//                    ? area.getParagraphSelection(idx)
//                    : StyledTextArea.EMPTY_RANGE;
//        }, area.selectionProperty(), cell.indexProperty());
//        cell.selectionProperty().bind(cellSelection);
//        manageBinding(cellSelection);

        // keep cell's wrap width updated
//        cell.wrapWidthProperty().bind(wrapWidth);

        // Respond to changes in paragraphGraphicFactory.
        // Use event stream because it is unbound from area on unsubscribe,
        // while binding's weak listener will not be removed from area until
        // paragraphGraphicFactoryProperty changes, which might never happen.
//        Binding<Supplier<? extends Node>> cellGraphicFactory =
//                EventStreams.valuesOf(area.paragraphGraphicFactoryProperty())
//                        .<Supplier<? extends Node>> map(f -> f == null ? null : () -> f.apply(cell.getIndex()))
//                        .toBinding(null);
//        cell.graphicFactoryProperty().bind(cellGraphicFactory);
//        manageBinding(cellGraphicFactory);
//    }

    private ParagraphBox<S> getCell(int index) {
        return virtualFlow.getCellIfVisible(index).get().getNode();
    }

    private int getCellInsertionIndex(ParagraphBox<S> cell, int line, double x) {
        return cell.hitText(line, x)
                .map(HitInfo::getInsertionIndex)
                .orElse(cell.getParagraph().length());
    }

    private EventStream<MouseOverTextEvent> mouseOverTextEvents(ObservableSet<ParagraphBox<S>> cells, Duration delay) {
        return merge(cells, c -> c.stationaryIndices(delay).unify(
                l -> l.map((pos, charIdx) -> MouseOverTextEvent.beginAt(c.localToScreen(pos), getParagraphOffset(c.getIndex()) + charIdx)),
                r -> MouseOverTextEvent.end()));
    }

    private int getParagraphOffset(int parIdx) {
        return area.position(parIdx, 0).toOffset();
    }

    private void followCaret() {
        // bring the current paragraph to the viewport
        virtualFlow.show(area.getCurrentParagraph());
    }

    private void positionPopup(PopupWindow popup, PopupAlignment alignment, UnaryOperator<Point2D> adjustment) {
        Optional<Bounds> bounds = null;
        switch(alignment.getAnchorObject()) {
            case CARET: bounds = getCaretBoundsOnScreen(); break;
            case SELECTION: bounds = getSelectionBoundsOnScreen(); break;
        }
        bounds.ifPresent(b -> {
            double x = 0, y = 0;
            switch(alignment.getHorizontalAlignment()) {
                case LEFT: x = b.getMinX(); break;
                case H_CENTER: x = (b.getMinX() + b.getMaxX()) / 2; break;
                case RIGHT: x = b.getMaxX(); break;
            }
            switch(alignment.getVerticalAlignment()) {
                case TOP: y = b.getMinY();
                case V_CENTER: y = (b.getMinY() + b.getMaxY()) / 2; break;
                case BOTTOM: y = b.getMaxY(); break;
            }
            Point2D anchor = adjustment.apply(new Point2D(x, y));
            popup.setAnchorX(anchor.getX());
            popup.setAnchorY(anchor.getY());
        });
    }

    private Optional<Bounds> getCaretBoundsOnScreen() {
        return virtualFlow.getCellIfVisible(area.getCurrentParagraph())
                .map(c -> c.getNode().getCaretBoundsOnScreen());
    }

    private Optional<Bounds> getSelectionBoundsOnScreen() {
        IndexRange selection = area.getSelection();
        if(selection.getLength() == 0) {
            return getCaretBoundsOnScreen();
        }

        Bounds[] bounds = virtualFlow.visibleCells()
                .map(c -> c.getNode().getSelectionBoundsOnScreen())
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get())
                .toArray(n -> new Bounds[n]);

        if(bounds.length == 0) {
            return Optional.empty();
        }
        double minX = Stream.of(bounds).mapToDouble(Bounds::getMinX).min().getAsDouble();
        double maxX = Stream.of(bounds).mapToDouble(Bounds::getMaxX).max().getAsDouble();
        double minY = Stream.of(bounds).mapToDouble(Bounds::getMinY).min().getAsDouble();
        double maxY = Stream.of(bounds).mapToDouble(Bounds::getMaxY).max().getAsDouble();
        return Optional.of(new BoundingBox(minX, minY, maxX-minX, maxY-minY));
    }

    private void listenTo(Observable observable, InvalidationListener listener) {
        observable.addListener(listener);
        manageSubscription(() -> observable.removeListener(listener));
    }

    private <T> void listenTo(ObservableValue<T> observable, ChangeListener<T> listener) {
        observable.addListener(listener);
        manageSubscription(() -> observable.removeListener(listener));
    }

    private <T> void subscribeTo(EventStream<T> src, Consumer<T> consumer) {
        manageSubscription(src.subscribe(consumer));
    }

    private void manageSubscription(Subscription subscription) {
        subscriptions = subscriptions.and(subscription);
    }

    private void manageBinding(Binding<?> binding) {
        subscriptions = subscriptions.and(() -> binding.dispose());
    }
}