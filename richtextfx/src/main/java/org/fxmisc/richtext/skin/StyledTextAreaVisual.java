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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.PopupWindow;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicObservableValue;
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
     * Observables                                                            *
     *                                                                        *
     * ********************************************************************** */

    final DoubleProperty wrapWidth = new SimpleDoubleProperty(this, "wrapWidth");


    /* ********************************************************************** *
     *                                                                        *
     * Event streams                                                          *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Stream of all mouse events on all cells. May be used by behavior.
     */
    private final EventStream<Tuple2<ParagraphCell<S>, MouseEvent>> cellMouseEvents;
    public final EventStream<Tuple2<ParagraphCell<S>, MouseEvent>> cellMouseEvents() {
        return cellMouseEvents;
    }

    /**
     * Convenient way to subscribe to events on the control.
     */
    public <E extends Event> EventStream<E> events(EventType<E> eventType) {
        return EventStreams.eventsOf(area, eventType);
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

    private final MyListView<Paragraph<S>, ParagraphCell<S>> listView;

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

        // keeps track of all cells
        @SuppressWarnings("unchecked")
        ObservableSet<ParagraphCell<S>> cells = FXCollections.observableSet();

        // keeps track of currently used non-empty cells
        @SuppressWarnings("unchecked")
        ObservableSet<ParagraphCell<S>> nonEmptyCells = FXCollections.observableSet();

        // Initialize content
        listView = new MyListView<>(
                area.getParagraphs(),
                lv -> { // Use ParagraphCell as cell implementation
                    ParagraphCell<S> cell = new ParagraphCell<S>(StyledTextAreaVisual.this, applyStyle);
                    cells.add(cell);
                    valuesOf(cell.emptyProperty()).subscribe(empty -> {
                        if(empty) {
                            nonEmptyCells.remove(cell);
                        } else {
                            nonEmptyCells.add(cell);
                        }
                    });
                    cellCreated(cell);
                    return cell;
                });

        // initialize navigator
        IntSupplier cellCount = () -> area.getParagraphs().size();
        IntUnaryOperator cellLength = i -> listView.mapCell(i, c -> c.getLineCount());
        navigator = new TwoLevelNavigator(cellCount, cellLength);

        // make wrapWidth behave according to the wrapText property
        listenTo(area.wrapTextProperty(), o -> updateWrapWidth());
        updateWrapWidth();

        // emits a value every time the area is done updating
        EventStream<?> areaDoneUpdating = area.beingUpdatedProperty().offs();

        // follow the caret every time the caret position or paragraphs change
        EventStream<Void> caretPosDirty = invalidationsOf(area.caretPositionProperty());
        EventStream<Void> paragraphsDirty = invalidationsOf(listView.getItems());
        EventStream<Void> selectionDirty = invalidationsOf(area.selectionProperty());
        // need to reposition popup even when caret hasn't moved, but selection has changed (been deselected)
        EventStream<Void> caretDirty = merge(caretPosDirty, paragraphsDirty, selectionDirty);
        EventSource<Void> positionPopupImpulse = new EventSource<>();
        subscribeTo(caretDirty.emitOn(areaDoneUpdating), x -> followCaret(() -> positionPopupImpulse.push(null)));

        // update selection in paragraphs
        subscribeTo(selectionDirty.emitOn(areaDoneUpdating), x -> {
            IndexRange visibleRange = listView.getVisibleRange();
            int startPar = visibleRange.getStart();
            int endPar = visibleRange.getEnd();

            for(int i = startPar; i < endPar; ++i) {
                ParagraphGraphic<S> graphic = getCell(i).getParagraphGraphic();
                graphic.setSelection(area.getParagraphSelection(i));
            }

            // force selectionProperty() to be valid to make sure
            // we get invalidation notification on its next change
            area.selectionProperty().getValue();
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
        manageSubscription(() -> caretVisible.dispose());

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
                cells,
                c -> eventsOf(c, MouseEvent.ANY).map(e -> t(c, e)));
    }


    /* ********************************************************************** *
     *                                                                        *
     * Public API (from Visual)                                               *
     *                                                                        *
     * ********************************************************************** */

    @Override
    public Node getNode() {
        return listView;
    }


    /* ********************************************************************** *
     *                                                                        *
     * Actions                                                                *
     *                                                                        *
     * ********************************************************************** */

    public void showAsFirst(int index) {
        listView.showAsFirst(index);
    }

    public void showAsLast(int index) {
        listView.showAsLast(index);
    }

    @Override
    public void dispose() {
        subscriptions.unsubscribe();
    }


    /* ********************************************************************** *
     *                                                                        *
     * Queries                                                                *
     *                                                                        *
     * ********************************************************************** */

    public int getFirstVisibleIndex() {
        return listView.getFirstVisibleIndex();
    }

    public int getLastVisibleIndex() {
        return listView.getLastVisibleIndex();
    }

    public double getCaretOffsetX() {
        int idx = area.getCurrentParagraph();
        return idx == -1 ? 0 : getCell(idx).getCaretOffsetX();
    }

    public int getInsertionIndex(Position targetLine, double x) {
        int parIdx = targetLine.getMajor();
        int parInsertionIndex = listView.mapCell(parIdx, c -> getCellInsertionIndex(c, targetLine.getMinor(), x));
        return getParagraphOffset(parIdx) + parInsertionIndex;
    }

    /**
     * Returns the current line as a two-level index.
     * The major number is the paragraph index, the minor
     * number is the line number within the paragraph.
     */
    public Position currentLine() {
        int parIdx = area.getCurrentParagraph();
        int lineIdx = getCell(parIdx).getCurrentLineIndex();

        return position(parIdx, lineIdx);
    }

    public Position position(int par, int line) {
        return navigator.position(par, line);
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
     * Package private methods                                                *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Used by ParagraphCell, but should refactor so that ParagraphCell doesn't
     * need it.
     */
    @Deprecated
    StyledTextArea<S> getArea() {
        return area;
    }


    /* ********************************************************************** *
     *                                                                        *
     * Private methods                                                        *
     *                                                                        *
     * ********************************************************************** */

    private void cellCreated(ParagraphCell<S> cell) {
        BooleanBinding hasCaret = Bindings.equal(
                cell.indexProperty(),
                area.currentParagraphProperty());

        // caret is visible only in the paragraph with the caret
        cell.caretVisibleProperty().bind(hasCaret.and(caretVisible));

        cell.highlightFillProperty().bind(highlightFill);
        cell.highlightTextFillProperty().bind(highlightTextFill);

        // bind cell's caret position to area's caret column,
        // when the cell is the one with the caret
        EasyBind.bindConditionally(
                cell.caretPositionProperty(),
                area.caretColumnProperty(),
                hasCaret);
    }

    private ParagraphCell<S> getCell(int index) {
        return listView.getVisibleCell(index).get();
    }

    private int getCellInsertionIndex(ParagraphCell<S> cell, int line, double x) {
        return cell.hitGraphic(line, x)
                .map(HitInfo::getInsertionIndex)
                .orElse(cell.getItem().length());
    }

    private EventStream<MouseOverTextEvent> mouseOverTextEvents(ObservableSet<ParagraphCell<S>> cells, Duration delay) {
        return merge(cells, c -> c.stationaryIndices(delay).unify(
                l -> l.map((pos, charIdx) -> MouseOverTextEvent.beginAt(c.localToScreen(pos), getParagraphOffset(c.getIndex()) + charIdx)),
                r -> MouseOverTextEvent.end()));
    }

    private int getParagraphOffset(int parIdx) {
        return area.position(parIdx, 0).toOffset();
    }

    private void updateWrapWidth() {
        if(area.isWrapText()) {
            wrapWidth.bind(listView.widthProperty());
        } else {
            wrapWidth.unbind();
            wrapWidth.set(Region.USE_COMPUTED_SIZE); // no wrapping
        }
    }

    private void followCaret(Runnable callback) {
        int par = area.getCurrentParagraph();

        // Bring the current paragraph to the viewport, then update the popup.
        Paragraph<S> paragraph = area.getParagraphs().get(par);
        listView.show(par, item -> {
            // Since this callback is executed on the next pulse,
            // make sure the item (paragraph) hasn't changed in the meantime.
            if(item == paragraph) {
                callback.run();
            }
        });
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
        return listView.getVisibleCell(area.getCurrentParagraph())
                .map(cell -> cell.getParagraphGraphic().getCaretBoundsOnScreen());
    }

    private Optional<Bounds> getSelectionBoundsOnScreen() {
        IndexRange selection = area.getSelection();
        if(selection.getLength() == 0) {
            return getCaretBoundsOnScreen();
        }

        IndexRange visibleRange = listView.getVisibleRange();
        Bounds[] bounds = IntStream.range(visibleRange.getStart(), visibleRange.getEnd())
                .<Optional<Bounds>>mapToObj(i -> listView.getVisibleCell(i)
                        .flatMap(cell -> cell.getParagraphGraphic().getSelectionBoundsOnScreen()))
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
}