package org.fxmisc.richtext.skin;

import static org.reactfx.EventStreams.*;

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
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.IndexRange;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.PopupWindow;
import javafx.util.Duration;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicObservableValue;
import org.fxmisc.richtext.Paragraph;
import org.fxmisc.richtext.PopupAlignment;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TwoDimensional.Position;
import org.fxmisc.richtext.TwoLevelNavigator;
import org.fxmisc.richtext.behavior.CodeAreaBehavior;
import org.fxmisc.richtext.skin.CssProperties.HighlightFillProperty;
import org.fxmisc.richtext.skin.CssProperties.HighlightTextFillProperty;
import org.reactfx.EventSource;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;

import com.sun.javafx.scene.control.skin.BehaviorSkinBase;
import com.sun.javafx.scene.text.HitInfo;

/**
 * Code area skin.
 */
public class StyledTextAreaSkin<S> extends BehaviorSkinBase<StyledTextArea<S>, CodeAreaBehavior<S>> {

    /**************************************************************************
     *                                                                        *
     * Properties                                                             *
     *                                                                        *
     **************************************************************************/

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


    /**************************************************************************
     *                                                                        *
     * Observables                                                            *
     *                                                                        *
     **************************************************************************/

    final DoubleProperty wrapWidth = new SimpleDoubleProperty(this, "wrapWidth");


    /**************************************************************************
     *                                                                        *
     * Private fields                                                         *
     *                                                                        *
     **************************************************************************/

    private Subscription subscriptions = () -> {};

    private final BooleanPulse caretPulse = new BooleanPulse(Duration.seconds(.5));

    private final BooleanBinding caretVisible;

    private final MyListView<Paragraph<S>, ParagraphCell<S>> listView;

    // used for two-level navigation, where on the higher level are
    // paragraphs and on the lower level are lines within a paragraph
    private final TwoLevelNavigator navigator;


    /**************************************************************************
     *                                                                        *
     * Constructors                                                           *
     *                                                                        *
     **************************************************************************/

    public StyledTextAreaSkin(final StyledTextArea<S> styledTextArea, BiConsumer<Text, S> applyStyle) {
        super(styledTextArea, new CodeAreaBehavior<S>(styledTextArea));
        getBehavior().setCodeAreaSkin(this);

        // load the default style
        styledTextArea.getStylesheets().add(StyledTextAreaSkin.class.getResource("styled-text-area.css").toExternalForm());

        // Initialize content
        listView = new MyListView<>(
                styledTextArea.getParagraphs(),
                lv -> { // Use ParagraphCell as cell implementation
                    ParagraphCell<S> cell = new ParagraphCell<S>(StyledTextAreaSkin.this, applyStyle);
                    cellCreated(cell);
                    return cell;
                });
        getChildren().add(listView);

        // initialize navigator
        IntSupplier cellCount = () -> getSkinnable().getParagraphs().size();
        IntUnaryOperator cellLength = i -> listView.mapCell(i, c -> c.getLineCount());
        navigator = new TwoLevelNavigator(cellCount, cellLength);

        // make wrapWidth behave according to the wrapText property
        listenTo(styledTextArea.wrapTextProperty(), o -> updateWrapWidth());
        updateWrapWidth();

        // emits a value every time the area is done updating
        EventStream<?> areaDoneUpdating = styledTextArea.beingUpdatedProperty().offs();

        // follow the caret every time the caret position or paragraphs change
        EventStream<Void> caretPosDirty = invalidationsOf(styledTextArea.caretPositionProperty());
        EventStream<Void> paragraphsDirty = invalidationsOf(listView.getItems());
        EventStream<Void> selectionDirty = invalidationsOf(styledTextArea.selectionProperty());
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
                graphic.setSelection(styledTextArea.getParagraphSelection(i));
            }

            // force selectionProperty() to be valid to make sure
            // we get invalidation notification on its next change
            styledTextArea.selectionProperty().getValue();
        });

        // blink caret only when focused
        listenTo(styledTextArea.focusedProperty(), (obs, old, isFocused) -> {
            if(isFocused)
                caretPulse.start(true);
            else
                caretPulse.stop(false);
        });
        if(styledTextArea.isFocused()) {
            caretPulse.start(true);
        }
        manageSubscription(() -> caretPulse.stop());

        // The caret is visible in periodic intervals, but only when
        // the code area is focused, editable and not disabled.
        caretVisible = caretPulse
                .and(styledTextArea.focusedProperty())
                .and(styledTextArea.editableProperty())
                .and(styledTextArea.disabledProperty().not());
        manageSubscription(() -> caretVisible.dispose());

        // Adjust popup anchor by either a user-provided function,
        // or user-provided offset, or don't adjust at all.
        MonadicObservableValue<UnaryOperator<Point2D>> userFunction =
                EasyBind.monadic(styledTextArea.popupAnchorAdjustmentProperty());
        MonadicObservableValue<UnaryOperator<Point2D>> userOffset =
                EasyBind.monadic(styledTextArea.popupAnchorOffsetProperty())
                        .map(offset -> anchor -> anchor.add(offset));
        ObservableValue<UnaryOperator<Point2D>> popupAnchorAdjustment = userFunction
                .orElse(userOffset)
                .orElse(UnaryOperator.identity());

        // Position popup window whenever the window itself, its alignment,
        // or the position adjustment function changes.
        manageSubscription(EventStreams.combine(
                EventStreams.valuesOf(styledTextArea.popupWindowProperty()),
                EventStreams.valuesOf(styledTextArea.popupAlignmentProperty()),
                EventStreams.valuesOf(popupAnchorAdjustment))
            .repeatOn(positionPopupImpulse)
            .filter((w, al, adj) -> w != null)
            .subscribe((w, al, adj) -> positionPopup(w, al, adj)));
    }


    /**************************************************************************
     *                                                                        *
     * Actions                                                                *
     *                                                                        *
     **************************************************************************/

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


    /**************************************************************************
     *                                                                        *
     * Queries                                                                *
     *                                                                        *
     **************************************************************************/

    public int getFirstVisibleIndex() {
        return listView.getFirstVisibleIndex();
    }

    public int getLastVisibleIndex() {
        return listView.getLastVisibleIndex();
    }

    public double getCaretOffsetX() {
        int idx = getSkinnable().getCurrentParagraph();
        return idx == -1 ? 0 : getCell(idx).getCaretOffsetX();
    }

    public HitInfo hit(Position targetLine, double x) {
        return listView.mapCell(targetLine.getMajor(), c -> c.hit(targetLine.getMinor(), x));
    }

    /**
     * Returns the current line as a two-level index.
     * The major number is the paragraph index, the minor
     * number is the line number within the paragraph.
     */
    public Position currentLine() {
        int parIdx = getSkinnable().getCurrentParagraph();
        int lineIdx = getCell(parIdx).getCurrentLineIndex();

        return position(parIdx, lineIdx);
    }

    public Position position(int par, int line) {
        return navigator.position(par, line);
    }


    /**************************************************************************
     *                                                                        *
     * Look &amp; feel                                                        *
     *                                                                        *
     **************************************************************************/

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return Arrays.<CssMetaData<? extends Styleable, ?>>asList(
                highlightFill.getCssMetaData(),
                highlightTextFill.getCssMetaData());
    }


    /**************************************************************************
     *                                                                        *
     * Private methods                                                        *
     *                                                                        *
     **************************************************************************/

    private void cellCreated(ParagraphCell<S> cell) {
        BooleanBinding hasCaret = Bindings.equal(
                cell.indexProperty(),
                getSkinnable().currentParagraphProperty());

        // caret is visible only in the paragraph with the caret
        cell.caretVisibleProperty().bind(hasCaret.and(caretVisible));

        cell.highlightFillProperty().bind(highlightFill);
        cell.highlightTextFillProperty().bind(highlightTextFill);

        // bind cell's caret position to area's caret column,
        // when the cell is the one with the caret
        EasyBind.bindConditionally(
                cell.caretPositionProperty(),
                getSkinnable().caretColumnProperty(),
                hasCaret);

        // listen to mouse events on lines
        cell.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            getBehavior().mousePressed(event);
            event.consume();
        });
        cell.addEventHandler(MouseEvent.DRAG_DETECTED, event -> {
            // startFullDrag() causes subsequent drag events to be
            // received by corresponding LineCells, instead of all
            // events being delivered to the original LineCell.
            cell.getScene().startFullDrag();
            getBehavior().dragDetected(event);
            event.consume();
        });
        cell.addEventHandler(MouseDragEvent.MOUSE_DRAG_OVER, event -> {
            getBehavior().mouseDragOver(event);
            event.consume();
        });
        cell.addEventHandler(MouseDragEvent.MOUSE_DRAG_RELEASED, event -> {
            getBehavior().mouseDragReleased(event);
            event.consume();
        });
        cell.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            getBehavior().mouseReleased(event);
            event.consume();
        });
    }

    private ParagraphCell<S> getCell(int index) {
        return listView.getVisibleCell(index).get();
    }

    private void updateWrapWidth() {
        if(getSkinnable().isWrapText()) {
            wrapWidth.bind(listView.widthProperty());
        } else {
            wrapWidth.unbind();
            wrapWidth.set(Region.USE_COMPUTED_SIZE); // no wrapping
        }
    }

    private void followCaret(Runnable callback) {
        int par = getSkinnable().getCurrentParagraph();

        // Bring the current paragraph to the viewport, then update the popup.
        Paragraph<S> paragraph = getSkinnable().getParagraphs().get(par);
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
        return listView.getVisibleCell(getSkinnable().getCurrentParagraph())
                .map(cell -> cell.getParagraphGraphic().getCaretBoundsOnScreen());
    }

    private Optional<Bounds> getSelectionBoundsOnScreen() {
        IndexRange selection = getSkinnable().getSelection();
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