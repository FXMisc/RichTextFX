package org.fxmisc.richtext.skin;

import static org.reactfx.EventStreams.*;

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

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.event.Event;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.IndexRange;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.PopupWindow;

import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualFlowHit;
import org.fxmisc.richtext.MouseOverTextEvent;
import org.fxmisc.richtext.Paragraph;
import org.fxmisc.richtext.PopupAlignment;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TwoDimensional.Position;
import org.fxmisc.richtext.TwoLevelNavigator;
import org.fxmisc.richtext.skin.CssProperties.HighlightFillProperty;
import org.fxmisc.richtext.skin.CssProperties.HighlightTextFillProperty;
import org.fxmisc.wellbehaved.skin.SimpleVisualBase;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;
import org.reactfx.value.Val;

public class StyledTextAreaVisual<S> extends SimpleVisualBase<StyledTextArea<S>> {
    private final StyledTextAreaView<S> node;

    public StyledTextAreaVisual(StyledTextArea<S> control, BiConsumer<Text, S> applyStyle) {
        super(control);
        this.node = new StyledTextAreaView<>(control, applyStyle);
    }

    @Override
    public void dispose() {
        node.dispose();
    }

    @Override
    public StyledTextAreaView<S> getNode() {
        return node;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return node.getCssMetaData();
    }
}

/**
 * StyledTextArea skin.
 */
class StyledTextAreaView<S> extends Region {

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
     * Private fields                                                         *
     *                                                                        *
     * ********************************************************************** */

    private final StyledTextArea<S> area;

    private Subscription subscriptions = () -> {};

    private final BooleanPulse caretPulse = new BooleanPulse(javafx.util.Duration.seconds(.5));

    private final BooleanBinding caretVisible;

    private final Val<UnaryOperator<Point2D>> popupAnchorAdjustment;

    private final VirtualFlow<Paragraph<S>, Cell<Paragraph<S>, ParagraphBox<S>>> virtualFlow;

    // used for two-level navigation, where on the higher level are
    // paragraphs and on the lower level are lines within a paragraph
    private final TwoLevelNavigator navigator;

    private boolean followCaretRequested = false;


    /* ********************************************************************** *
     *                                                                        *
     * Constructors                                                           *
     *                                                                        *
     * ********************************************************************** */

    public StyledTextAreaView(
            StyledTextArea<S> styledTextArea,
            BiConsumer<Text, S> applyStyle) {
        this.area = styledTextArea;

        // load the default style
        area.getStylesheets().add(StyledTextAreaView.class.getResource("styled-text-area.css").toExternalForm());

        // keeps track of currently used non-empty cells
        @SuppressWarnings("unchecked")
        ObservableSet<ParagraphBox<S>> nonEmptyCells = FXCollections.observableSet();

        // Initialize content
        virtualFlow = VirtualFlow.createVertical(
                area.getParagraphs(),
                par -> {
                    Cell<Paragraph<S>, ParagraphBox<S>> cell = createCell(par, applyStyle);
                    nonEmptyCells.add(cell.getNode());
                    return cell.beforeReset(() -> nonEmptyCells.remove(cell.getNode()))
                            .afterUpdateItem(p -> nonEmptyCells.add(cell.getNode()));
                });
        getChildren().add(virtualFlow);

        // initialize navigator
        IntSupplier cellCount = () -> area.getParagraphs().size();
        IntUnaryOperator cellLength = i -> virtualFlow.getCell(i).getNode().getLineCount();
        navigator = new TwoLevelNavigator(cellCount, cellLength);

        // follow the caret every time the caret position or paragraphs change
        EventStream<?> caretPosDirty = invalidationsOf(area.caretPositionProperty());
        EventStream<?> paragraphsDirty = invalidationsOf(area.getParagraphs());
        EventStream<?> selectionDirty = invalidationsOf(area.selectionProperty());
        // need to reposition popup even when caret hasn't moved, but selection has changed (been deselected)
        EventStream<?> caretDirty = merge(caretPosDirty, paragraphsDirty, selectionDirty);
        subscribeTo(caretDirty, x -> requestFollowCaret());

        // blink caret only when focused
        manageSubscription(EventStreams.valuesOf(area.focusedProperty()).subscribe(isFocused -> {
            if(isFocused) {
                caretPulse.start(true);
            } else {
                caretPulse.stop(false);
            }
        }));
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
        Val<UnaryOperator<Point2D>> userOffset = Val.map(
                area.popupAnchorOffsetProperty(),
                offset -> anchor -> anchor.add(offset));
        this.popupAnchorAdjustment =
                Val.orElse(
                        area.popupAnchorAdjustmentProperty(),
                        userOffset)
                .orElseConst(UnaryOperator.identity());

        // dispatch MouseOverTextEvents when mouseOverTextDelay is not null
        EventStreams.valuesOf(area.mouseOverTextDelayProperty())
                .flatMap(delay -> delay != null
                        ? mouseOverTextEvents(nonEmptyCells, delay)
                        : EventStreams.never())
                .subscribe(evt -> Event.fireEvent(area, evt));
    }


    /* ********************************************************************** *
     *                                                                        *
     * Public API                                                             *
     *                                                                        *
     * ********************************************************************** */

    public void dispose() {
        subscriptions.unsubscribe();
        virtualFlow.dispose();
    }


    /* ********************************************************************** *
     *                                                                        *
     * Layout                                                                 *
     *                                                                        *
     * ********************************************************************** */

    @Override
    protected void layoutChildren() {
        virtualFlow.resize(getWidth(), getHeight());
        if(followCaretRequested) {
            followCaretRequested = false;
            followCaret();
        }

        // position popup
        PopupWindow popup = area.getPopupWindow();
        PopupAlignment alignment = area.getPopupAlignment();
        UnaryOperator<Point2D> adjustment = popupAnchorAdjustment.getValue();
        if(popup != null) {
            positionPopup(popup, alignment, adjustment);
        }
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

    void scrollBy(Point2D deltas) {
        virtualFlow.scrollX(deltas.getX());
        virtualFlow.scrollY(deltas.getY());
    }

    void show(double y) {
        virtualFlow.show(y);
    }

    void showCaretAtBottom() {
        int parIdx = area.getCurrentParagraph();
        Cell<Paragraph<S>, ParagraphBox<S>> cell = virtualFlow.getCell(parIdx);
        Bounds caretBounds = cell.getNode().getCaretBounds();
        double y = caretBounds.getMaxY();
        virtualFlow.showAtOffset(parIdx, getViewportHeight() - y);
    }

    void showCaretAtTop() {
        int parIdx = area.getCurrentParagraph();
        Cell<Paragraph<S>, ParagraphBox<S>> cell = virtualFlow.getCell(parIdx);
        Bounds caretBounds = cell.getNode().getCaretBounds();
        double y = caretBounds.getMinY();
        virtualFlow.showAtOffset(parIdx, -y);
    }

    void requestFollowCaret() {
        followCaretRequested = true;
        requestLayout();
    }

    private void followCaret() {
        int parIdx = area.getCurrentParagraph();
        Cell<Paragraph<S>, ParagraphBox<S>> cell = virtualFlow.getCell(parIdx);
        Bounds caretBounds = cell.getNode().getCaretBounds();
        double graphicWidth = cell.getNode().getGraphicPrefWidth();
        Bounds region = extendLeft(caretBounds, graphicWidth);
        virtualFlow.show(parIdx, region);
    }


    /* ********************************************************************** *
     *                                                                        *
     * Queries                                                                *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Returns caret bounds relative to the viewport, i.e. the visual bounds
     * of the embedded VirtualFlow.
     */
    Optional<Bounds> getCaretBounds() {
        return virtualFlow.getCellIfVisible(area.getCurrentParagraph())
                .map(c -> {
                    Bounds cellBounds = c.getNode().getCaretBounds();
                    return virtualFlow.cellToViewport(c, cellBounds);
                });
    }

    /**
     * Returns x coordinate of the caret relative to the current TextFlow, not
     * relative to the skin.
     */
    double getCaretOffsetX() {
        int idx = area.getCurrentParagraph();
        return idx == -1 ? 0 : getCell(idx).getCaretOffsetX();
    }

    double getViewportHeight() {
        return virtualFlow.getViewportHeight();
    }

    @Deprecated
    int getInsertionIndex(double textX, Position targetLine) {
        int parIdx = targetLine.getMajor();
        ParagraphBox<S> cell = virtualFlow.getCell(parIdx).getNode();
        int parInsertionIndex = getCellInsertionIndex(cell, textX, targetLine.getMinor());
        return getParagraphOffset(parIdx) + parInsertionIndex;
    }

    @Deprecated
    int getInsertionIndex(double textX, double y) {
        VirtualFlowHit<Cell<Paragraph<S>, ParagraphBox<S>>> hit = virtualFlow.hit(0.0, y);
        if(hit.isBeforeCells()) {
            return 0;
        } else if(hit.isAfterCells()) {
            return area.getLength();
        } else {
            int parIdx = hit.getCellIndex();
            ParagraphBox<S> cell = hit.getCell().getNode();
            double cellY = hit.getCellOffset().getY();
            int parInsertionIndex = getCellInsertionIndex(cell, textX, cellY);
            return getParagraphOffset(parIdx) + parInsertionIndex;
        }
    }

    CharacterHit hit(double x, double y) {
        VirtualFlowHit<Cell<Paragraph<S>, ParagraphBox<S>>> hit = virtualFlow.hit(x, y);
        if(hit.isBeforeCells()) {
            return CharacterHit.before(0);
        } else if(hit.isAfterCells()) {
            return CharacterHit.after(area.getLength() - 1);
        } else {
            ParagraphBox<S> cell = hit.getCell().getNode();
            Point2D cellOffset = hit.getCellOffset();
            CharacterHit cellHit = cell.hit(cellOffset);
            int parOffset = getParagraphOffset(cell.getIndex());
            return new CharacterHit(
                    parOffset + cellHit.getCharacterIndex(),
                    cellHit.getHitType());
        }
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
            Paragraph<S> paragraph,
            BiConsumer<Text, S> applyStyle) {

        ParagraphBox<S> box = new ParagraphBox<>(paragraph, applyStyle);

        box.highlightFillProperty().bind(highlightFill);
        box.highlightTextFillProperty().bind(highlightTextFill);
        box.wrapTextProperty().bind(area.wrapTextProperty());
        box.graphicFactoryProperty().bind(area.paragraphGraphicFactoryProperty());
        box.graphicOffset.bind(virtualFlow.breadthOffsetProperty());

        Val<Boolean> hasCaret = Val.combine(
                box.indexProperty(),
                area.currentParagraphProperty(),
                (bi, cp) -> bi.intValue() == cp.intValue());

        // caret is visible only in the paragraph with the caret
        Val<Boolean> cellCaretVisible = Val.combine(hasCaret, caretVisible, (a, b) -> a && b);
        box.caretVisibleProperty().bind(cellCaretVisible);

        // bind cell's caret position to area's caret column,
        // when the cell is the one with the caret
        box.caretPositionProperty().bind(hasCaret.flatMap(has -> has
                ? area.caretColumnProperty()
                : Val.constant(0)));

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
                box.graphicOffset.unbind();

                box.caretVisibleProperty().unbind();
                box.caretPositionProperty().unbind();

                box.selectionProperty().unbind();
                cellSelection.dispose();
            }
        };
    }

    private ParagraphBox<S> getCell(int index) {
        return virtualFlow.getCell(index).getNode();
    }

    @Deprecated
    private int getCellInsertionIndex(ParagraphBox<S> cell, double x, int line) {
        return cell.hitTextLine(x, line).getInsertionIndex();
    }

    @Deprecated
    private int getCellInsertionIndex(ParagraphBox<S> cell, double x, double y) {
        return cell.hitText(x, y).getInsertionIndex();
    }

    private EventStream<MouseOverTextEvent> mouseOverTextEvents(ObservableSet<ParagraphBox<S>> cells, Duration delay) {
        return merge(cells, c -> c.stationaryIndices(delay).map(e -> e.unify(
                l -> l.map((pos, charIdx) -> MouseOverTextEvent.beginAt(c.localToScreen(pos), getParagraphOffset(c.getIndex()) + charIdx)),
                r -> MouseOverTextEvent.end())));
    }

    private int getParagraphOffset(int parIdx) {
        return area.position(parIdx, 0).toOffset();
    }

    private void positionPopup(
            PopupWindow popup,
            PopupAlignment alignment,
            UnaryOperator<Point2D> adjustment) {
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

        Bounds[] bounds = virtualFlow.visibleCells().stream()
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

    private <T> void subscribeTo(EventStream<T> src, Consumer<T> consumer) {
        manageSubscription(src.subscribe(consumer));
    }

    private void manageSubscription(Subscription subscription) {
        subscriptions = subscriptions.and(subscription);
    }

    private void manageBinding(Binding<?> binding) {
        subscriptions = subscriptions.and(() -> binding.dispose());
    }

    private static Bounds extendLeft(Bounds b, double w) {
        if(w == 0) {
            return b;
        } else {
            return new BoundingBox(
                        b.getMinX() - w, b.getMinY(),
                        b.getWidth() + w, b.getHeight());
        }
    }
}