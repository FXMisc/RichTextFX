package codearea.skin;

import static reactfx.EventStreams.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.scene.control.IndexRange;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.util.Duration;
import reactfx.EventStream;
import reactfx.Subscription;
import codearea.behavior.CodeAreaBehavior;
import codearea.control.Paragraph;
import codearea.control.StyledTextArea;
import codearea.control.TwoDimensional.Position;
import codearea.control.TwoLevelNavigator;
import codearea.skin.CssProperties.HighlightFillProperty;
import codearea.skin.CssProperties.HighlightTextFillProperty;

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
    final StyleableObjectProperty<Paint> highlightFill
            = new HighlightFillProperty(this, Color.DODGERBLUE);

    /**
     * Text color for highlighted text.
     */
    final StyleableObjectProperty<Paint> highlightTextFill
            = new HighlightTextFillProperty(this, Color.WHITE);


    /**************************************************************************
     *                                                                        *
     * Observables                                                            *
     *                                                                        *
     **************************************************************************/

    final DoubleProperty wrapWidth = new SimpleDoubleProperty(this, "wrapWidth");

    final BooleanBinding caretVisible;


    /**************************************************************************
     *                                                                        *
     * Private fields                                                         *
     *                                                                        *
     **************************************************************************/

    private Subscription subscriptions = () -> {};

    private final BooleanPulse caretPulse = new BooleanPulse(Duration.seconds(.5));

    private final MyListView<Paragraph<S>> listView;

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

        // initialize navigator
        IntSupplier cellCount = () -> getSkinnable().getParagraphs().size();
        IntUnaryOperator cellLength = i -> getCell(i).getLineCount();
        navigator = new TwoLevelNavigator(cellCount, cellLength);

        // load the default style
        styledTextArea.getStylesheets().add(StyledTextAreaSkin.class.getResource("styled-text-area.css").toExternalForm());

        // Initialize content
        listView = new MyListView<Paragraph<S>>(styledTextArea.getParagraphs());
        getChildren().add(listView);

        // Use LineCell as cell implementation
        listView.setCellFactory(lv -> {
            ParagraphCell<S> cell = new ParagraphCell<S>(StyledTextAreaSkin.this, applyStyle);
            cellCreated(cell);
            return cell;
        });

        // make wrapWidth behave according to the wrapText property
        listenTo(styledTextArea.wrapTextProperty(), o -> updateWrapWidth());
        updateWrapWidth();

        // emits a value every time the area is done updating
        EventStream<?> areaDoneUpdating = styledTextArea.beingUpdatedProperty().offs();

        // update the caret every time the caret position or paragraphs change
        EventStream<Void> caretPosDirty = invalidationsOf(styledTextArea.caretPositionProperty());
        EventStream<Void> paragraphsDirty = invalidationsOf(listView.getItems());
        EventStream<Void> caretDirty = merge(caretPosDirty, paragraphsDirty);
        subscribeTo(emit(caretDirty).on(areaDoneUpdating), x -> refreshCaret());

        // update selection in paragraphs
        EventStream<Void> selectionDirty = invalidationsOf(styledTextArea.selectionProperty());
        subscribeTo(emit(selectionDirty).on(areaDoneUpdating), x -> {
            IndexRange visibleRange = listView.getVisibleRange();
            int startPar = visibleRange.getStart();
            int endPar = visibleRange.getEnd();

            for(int i = startPar; i < endPar; ++i) {
                ParagraphGraphic<S> graphic = getCell(i).getParagraphGraphic();
                graphic.setSelection(styledTextArea.getParagraphSelection(i));
            }
        });

        // blink caret only when focused
        listenTo(styledTextArea.focusedProperty(), (obs, old, isFocused) -> {
            if(isFocused)
                caretPulse.start(true);
            else
                caretPulse.stop(false);
        });
        if(styledTextArea.isFocused())
            caretPulse.start(true);
        manageSubscription(() -> caretPulse.stop());

        // The caret is visible in periodic intervals, but only when
        // the code area is focused, editable and not disabled.
        caretVisible = caretPulse
                .and(styledTextArea.focusedProperty())
                .and(styledTextArea.editableProperty())
                .and(styledTextArea.disabledProperty().not());
        manageSubscription(() -> caretVisible.dispose());
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
        int idx = listView.getSelectionModel().getSelectedIndex();
        return idx == -1 ? 0 : getCell(idx).getCaretOffsetX();
    }

    public HitInfo hit(Position targetLine, double x) {
        return getCell(targetLine.getMajor()).hit(targetLine.getMinor(), x);
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
     * Look & feel                                                            *
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
        return (ParagraphCell<S>) listView.getCell(index).get();
    }

    private void updateWrapWidth() {
        if(getSkinnable().isWrapText()) {
            wrapWidth.bind(listView.widthProperty());
        } else {
            wrapWidth.unbind();
            wrapWidth.set(Region.USE_COMPUTED_SIZE); // no wrapping
        }
    }

    private void refreshCaret() {
        int par = getSkinnable().getCurrentParagraph();
        int col = getSkinnable().getCaretColumn();

        listView.getSelectionModel().select(par);

        // bring the current paragraph to the viewport,
        // then update the caret
        listView.show(par, cell -> {
            ((ParagraphCell<S>) cell).getParagraphGraphic().setCaretPosition(col);
        });
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
        subscriptions = Subscription.multi(subscriptions, subscription);
    }
}