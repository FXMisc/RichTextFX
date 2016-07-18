package org.fxmisc.richtext;

import static javafx.util.Duration.*;
import static org.fxmisc.richtext.PopupAlignment.*;
import static org.reactfx.EventStreams.*;
import static org.reactfx.util.Tuples.*;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.css.PseudoClass;
import javafx.css.StyleableObjectProperty;
import javafx.event.Event;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.PopupWindow;

import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualFlowHit;
import org.fxmisc.flowless.Virtualized;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CssProperties.EditableProperty;
import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.EditActions;
import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.NavigationActions;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.RichTextChange;
import org.fxmisc.richtext.model.SimpleEditableStyledDocument;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyledDocument;
import org.fxmisc.richtext.model.StyledTextAreaModel;
import org.fxmisc.richtext.model.TextEditingArea;
import org.fxmisc.richtext.model.TwoDimensional;
import org.fxmisc.richtext.model.TwoLevelNavigator;
import org.fxmisc.richtext.model.UndoActions;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.UndoManagerFactory;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.StateMachine;
import org.reactfx.Subscription;
import org.reactfx.collection.LiveList;
import org.reactfx.util.Tuple2;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

/**
 * Text editing control. Accepts user input (keyboard, mouse) and
 * provides API to assign style to text ranges. It is suitable for
 * syntax highlighting and rich-text editors.
 *
 * <p>Subclassing is allowed to define the type of style, e.g. inline
 * style or style classes.</p>
 *
 * <p>Note: Scroll bars no longer appear when the content spans outside
 * of the viewport. To add scroll bars, the area needs to be wrapped in
 * a {@link VirtualizedScrollPane}. For example, </p>
 * <pre>
 * {@code
 * // shows area without scroll bars
 * InlineCssTextArea area = new InlineCssTextArea();
 *
 * // add scroll bars that will display as needed
 * VirtualizedScrollPane<InlineCssTextArea> vsPane = new VirtualizedScrollPane(area);
 *
 * Parent parent = //;
 * parent.getChildren().add(vsPane)
 * }
 * </pre>
 *
 * <h3>Overriding keyboard shortcuts</h3>
 *
 * {@code StyledTextArea} uses {@code KEY_TYPED} handler to handle ordinary
 * character input and {@code KEY_PRESSED} handler to handle control key
 * combinations (including Enter and Tab). To add or override some keyboard
 * shortcuts, while keeping the rest in place, you would combine the default
 * event handler with a new one that adds or overrides some of the default
 * key combinations. This is how to bind {@code Ctrl+S} to the {@code save()}
 * operation:
 * <pre>
 * {@code
 * import static javafx.scene.input.KeyCode.*;
 * import static javafx.scene.input.KeyCombination.*;
 * import static org.fxmisc.wellbehaved.event.EventPattern.*;
 * import static org.fxmisc.wellbehaved.event.InputMap.*;
 *
 * import org.fxmisc.wellbehaved.event.Nodes;
 *
 * Nodes.addInputMap(area, consume(keyPressed(S, CONTROL_DOWN), event -> save()));
 * }
 * </pre>
 *
 * @param <S> type of style that can be applied to text.
 */
public class StyledTextArea<PS, S> extends Region
        implements
        TextEditingArea<PS, S>,
        EditActions<PS, S>,
        ClipboardActions<PS, S>,
        NavigationActions<PS, S>,
        UndoActions,
        TwoDimensional,
        Virtualized {

    /**
     * Index range [0, 0).
     */
    public static final IndexRange EMPTY_RANGE = new IndexRange(0, 0);

    private static final PseudoClass HAS_CARET = PseudoClass.getPseudoClass("has-caret");
    private static final PseudoClass FIRST_PAR = PseudoClass.getPseudoClass("first-paragraph");
    private static final PseudoClass LAST_PAR  = PseudoClass.getPseudoClass("last-paragraph");


    /* ********************************************************************** *
     *                                                                        *
     * Properties                                                             *
     *                                                                        *
     * Properties affect behavior and/or appearance of this control.          *
     *                                                                        *
     * They are readable and writable by the client code and never change by  *
     * other means, i.e. they contain either the default value or the value   *
     * set by the client code.                                                *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Background fill for highlighted text.
     */
    private final StyleableObjectProperty<Paint> highlightFill
            = new CssProperties.HighlightFillProperty(this, Color.DODGERBLUE);

    /**
     * Text color for highlighted text.
     */
    private final StyleableObjectProperty<Paint> highlightTextFill
            = new CssProperties.HighlightTextFillProperty(this, Color.WHITE);

    /**
     * Controls the blink rate of the caret, when one is displayed. Setting
     * the duration to zero disables blinking.
     */
    private final StyleableObjectProperty<javafx.util.Duration> caretBlinkRate
            = new CssProperties.CaretBlinkRateProperty(this, javafx.util.Duration.millis(500));

    // editable property
    /**
     * Indicates whether this text area can be edited by the user.
     * Note that this property doesn't affect editing through the API.
     */
    private final BooleanProperty editable = new EditableProperty<>(this);
    public final boolean isEditable() { return editable.get(); }
    public final void setEditable(boolean value) { editable.set(value); }
    public final BooleanProperty editableProperty() { return editable; }

    // wrapText property
    /**
     * When a run of text exceeds the width of the text region,
     * then this property indicates whether the text should wrap
     * onto another line.
     */
    private final BooleanProperty wrapText = new SimpleBooleanProperty(this, "wrapText");
    public final boolean isWrapText() { return wrapText.get(); }
    public final void setWrapText(boolean value) { wrapText.set(value); }
    public final BooleanProperty wrapTextProperty() { return wrapText; }

    // showCaret property
    /**
     * Indicates when this text area should display a caret.
     */
    private final Var<CaretVisibility> showCaret = Var.newSimpleVar(CaretVisibility.AUTO);
    public final CaretVisibility getShowCaret() { return showCaret.getValue(); }
    public final void setShowCaret(CaretVisibility value) { showCaret.setValue(value); }
    public final Var<CaretVisibility> showCaretProperty() { return showCaret; }

    public static enum CaretVisibility {
        /** Caret is displayed. */
        ON,
        /** Caret is displayed when area is focused, enabled, and editable. */
        AUTO,
        /** Caret is not displayed. */
        OFF
    }

    // undo manager
    @Override public UndoManager getUndoManager() { return model.getUndoManager(); }
    @Override public void setUndoManager(UndoManagerFactory undoManagerFactory) {
        model.setUndoManager(undoManagerFactory);
    }

    /**
     * Popup window that will be positioned by this text area relative to the
     * caret or selection. Use {@link #popupAlignmentProperty()} to specify
     * how the popup should be positioned relative to the caret or selection.
     * Use {@link #popupAnchorOffsetProperty()} or
     * {@link #popupAnchorAdjustmentProperty()} to further adjust the position.
     */
    private final ObjectProperty<PopupWindow> popupWindow = new SimpleObjectProperty<>();
    public void setPopupWindow(PopupWindow popup) { popupWindow.set(popup); }
    public PopupWindow getPopupWindow() { return popupWindow.get(); }
    public ObjectProperty<PopupWindow> popupWindowProperty() { return popupWindow; }

    /** @deprecated Use {@link #setPopupWindow(PopupWindow)}. */
    @Deprecated
    public void setPopupAtCaret(PopupWindow popup) { popupWindow.set(popup); }
    /** @deprecated Use {@link #getPopupWindow()}. */
    @Deprecated
    public PopupWindow getPopupAtCaret() { return popupWindow.get(); }
    /** @deprecated Use {@link #popupWindowProperty()}. */
    @Deprecated
    public ObjectProperty<PopupWindow> popupAtCaretProperty() { return popupWindow; }

    /**
     * Specifies further offset (in pixels) of the popup window from the
     * position specified by {@link #popupAlignmentProperty()}.
     *
     * <p>If {@link #popupAnchorAdjustmentProperty()} is also specified, then
     * it overrides the offset set by this property.
     */
    private final ObjectProperty<Point2D> popupAnchorOffset = new SimpleObjectProperty<>();
    public void setPopupAnchorOffset(Point2D offset) { popupAnchorOffset.set(offset); }
    public Point2D getPopupAnchorOffset() { return popupAnchorOffset.get(); }
    public ObjectProperty<Point2D> popupAnchorOffsetProperty() { return popupAnchorOffset; }

    /**
     * Specifies how to adjust the popup window's anchor point. The given
     * operator is invoked with the screen position calculated according to
     * {@link #popupAlignmentProperty()} and should return a new screen
     * position. This position will be used as the popup window's anchor point.
     *
     * <p>Setting this property overrides {@link #popupAnchorOffsetProperty()}.
     */
    private final ObjectProperty<UnaryOperator<Point2D>> popupAnchorAdjustment = new SimpleObjectProperty<>();
    public void setPopupAnchorAdjustment(UnaryOperator<Point2D> f) { popupAnchorAdjustment.set(f); }
    public UnaryOperator<Point2D> getPopupAnchorAdjustment() { return popupAnchorAdjustment.get(); }
    public ObjectProperty<UnaryOperator<Point2D>> popupAnchorAdjustmentProperty() { return popupAnchorAdjustment; }

    /**
     * Defines where the popup window given in {@link #popupWindowProperty()}
     * is anchored, i.e. where its anchor point is positioned. This position
     * can further be adjusted by {@link #popupAnchorOffsetProperty()} or
     * {@link #popupAnchorAdjustmentProperty()}.
     */
    private final ObjectProperty<PopupAlignment> popupAlignment = new SimpleObjectProperty<>(CARET_TOP);
    public void setPopupAlignment(PopupAlignment pos) { popupAlignment.set(pos); }
    public PopupAlignment getPopupAlignment() { return popupAlignment.get(); }
    public ObjectProperty<PopupAlignment> popupAlignmentProperty() { return popupAlignment; }

    /**
     * Defines how long the mouse has to stay still over the text before a
     * {@link MouseOverTextEvent} of type {@code MOUSE_OVER_TEXT_BEGIN} is
     * fired on this text area. When set to {@code null}, no
     * {@code MouseOverTextEvent}s are fired on this text area.
     *
     * <p>Default value is {@code null}.
     */
    private final ObjectProperty<Duration> mouseOverTextDelay = new SimpleObjectProperty<>(null);
    public void setMouseOverTextDelay(Duration delay) { mouseOverTextDelay.set(delay); }
    public Duration getMouseOverTextDelay() { return mouseOverTextDelay.get(); }
    public ObjectProperty<Duration> mouseOverTextDelayProperty() { return mouseOverTextDelay; }

    /**
     * Defines how to handle an event in which the user has selected some text, dragged it to a
     * new location within the area, and released the mouse at some character {@code index}
     * within the area.
     *
     * <p>By default, this will relocate the selected text to the character index where the mouse
     * was released. To override it, use {@link #setOnSelectionDrop(IntConsumer)}.
     */
    private Property<IntConsumer> onSelectionDrop = new SimpleObjectProperty<>(this::moveSelectedText);
    public final void setOnSelectionDrop(IntConsumer consumer) { onSelectionDrop.setValue(consumer); }
    public final IntConsumer getOnSelectionDrop() { return onSelectionDrop.getValue(); }

    private final ObjectProperty<IntFunction<? extends Node>> paragraphGraphicFactory = new SimpleObjectProperty<>(null);
    public void setParagraphGraphicFactory(IntFunction<? extends Node> factory) { paragraphGraphicFactory.set(factory); }
    public IntFunction<? extends Node> getParagraphGraphicFactory() { return paragraphGraphicFactory.get(); }
    public ObjectProperty<IntFunction<? extends Node>> paragraphGraphicFactoryProperty() { return paragraphGraphicFactory; }

    /**
     * Indicates whether the initial style should also be used for plain text
     * inserted into this text area. When {@code false}, the style immediately
     * preceding the insertion position is used. Default value is {@code false}.
     */
    public BooleanProperty useInitialStyleForInsertionProperty() { return model.useInitialStyleForInsertionProperty(); }
    public void setUseInitialStyleForInsertion(boolean value) { model.setUseInitialStyleForInsertion(value); }
    public boolean getUseInitialStyleForInsertion() { return model.getUseInitialStyleForInsertion(); }

    private Optional<Tuple2<Codec<PS>, Codec<S>>> styleCodecs = Optional.empty();
    /**
     * Sets codecs to encode/decode style information to/from binary format.
     * Providing codecs enables clipboard actions to retain the style information.
     */
    public void setStyleCodecs(Codec<PS> paragraphStyleCodec, Codec<S> textStyleCodec) {
        styleCodecs = Optional.of(t(paragraphStyleCodec, textStyleCodec));
    }
    @Override
    public Optional<Tuple2<Codec<PS>, Codec<S>>> getStyleCodecs() {
        return styleCodecs;
    }

    /**
     * The <em>estimated</em> scrollX value. This can be set in order to scroll the content.
     * Value is only accurate when area does not wrap lines and uses the same font size
     * throughout the entire area.
     */
    @Override
    public Var<Double> estimatedScrollXProperty() { return virtualFlow.estimatedScrollXProperty(); }
    public double getEstimatedScrollX() { return virtualFlow.estimatedScrollXProperty().getValue(); }
    public void setEstimatedScrollX(double value) { virtualFlow.estimatedScrollXProperty().setValue(value); }

    /**
     * The <em>estimated</em> scrollY value. This can be set in order to scroll the content.
     * Value is only accurate when area does not wrap lines and uses the same font size
     * throughout the entire area.
     */
    @Override
    public Var<Double> estimatedScrollYProperty() { return virtualFlow.estimatedScrollYProperty(); }
    public double getEstimatedScrollY() { return virtualFlow.estimatedScrollYProperty().getValue(); }
    public void setEstimatedScrollY(double value) { virtualFlow.estimatedScrollYProperty().setValue(value); }


    /* ********************************************************************** *
     *                                                                        *
     * Observables                                                            *
     *                                                                        *
     * Observables are "dynamic" (i.e. changing) characteristics of this      *
     * control. They are not directly settable by the client code, but change *
     * in response to user input and/or API actions.                          *
     *                                                                        *
     * ********************************************************************** */

    // text
    @Override public final String getText() { return model.getText(); }
    @Override public final ObservableValue<String> textProperty() { return model.textProperty(); }

    // rich text
    @Override public final StyledDocument<PS, S> getDocument() { return model.getDocument(); }

    // length
    @Override public final int getLength() { return model.getLength(); }
    @Override public final ObservableValue<Integer> lengthProperty() { return model.lengthProperty(); }

    // caret position
    @Override public final int getCaretPosition() { return model.getCaretPosition(); }
    @Override public final ObservableValue<Integer> caretPositionProperty() { return model.caretPositionProperty(); }

    // selection anchor
    @Override public final int getAnchor() { return model.getAnchor(); }
    @Override public final ObservableValue<Integer> anchorProperty() { return model.anchorProperty(); }

    // selection
    @Override public final IndexRange getSelection() { return model.getSelection(); }
    @Override public final ObservableValue<IndexRange> selectionProperty() { return model.selectionProperty(); }

    // selected text
    @Override public final String getSelectedText() { return model.getSelectedText(); }
    @Override public final ObservableValue<String> selectedTextProperty() { return model.selectedTextProperty(); }

    // current paragraph index
    @Override public final int getCurrentParagraph() { return model.getCurrentParagraph(); }
    @Override public final ObservableValue<Integer> currentParagraphProperty() { return model.currentParagraphProperty(); }

    // caret column
    @Override public final int getCaretColumn() { return model.getCaretColumn(); }
    @Override public final ObservableValue<Integer> caretColumnProperty() { return model.caretColumnProperty(); }

    // paragraphs
    @Override public LiveList<Paragraph<PS, S>> getParagraphs() { return model.getParagraphs(); }

    // beingUpdated
    public ObservableBooleanValue beingUpdatedProperty() { return model.beingUpdatedProperty(); }
    public boolean isBeingUpdated() { return model.isBeingUpdated(); }

    // total width estimate
    /**
     * The <em>estimated</em> width of the entire document. Accurate when area does not wrap lines and
     * uses the same font size throughout the entire area. Value is only supposed to be <em>set</em> by
     * the skin, not the user.
     */
    @Override
    public Val<Double> totalWidthEstimateProperty() { return virtualFlow.totalWidthEstimateProperty(); }
    public double getTotalWidthEstimate() { return virtualFlow.totalWidthEstimateProperty().getValue(); }

    // total height estimate
    /**
     * The <em>estimated</em> height of the entire document. Accurate when area does not wrap lines and
     * uses the same font size throughout the entire area. Value is only supposed to be <em>set</em> by
     * the skin, not the user.
     */
    @Override
    public Val<Double> totalHeightEstimateProperty() { return virtualFlow.totalHeightEstimateProperty(); }
    public double getTotalHeightEstimate() { return virtualFlow.totalHeightEstimateProperty().getValue(); }

    /* ********************************************************************** *
     *                                                                        *
     * Event streams                                                          *
     *                                                                        *
     * ********************************************************************** */

    // text changes
    @Override public final EventStream<PlainTextChange> plainTextChanges() { return model.plainTextChanges(); }

    // rich text changes
    @Override public final EventStream<RichTextChange<PS, S>> richChanges() { return model.richChanges(); }

    /* ********************************************************************** *
     *                                                                        *
     * Private fields                                                         *
     *                                                                        *
     * ********************************************************************** */

    private final StyledTextAreaBehavior behavior;

    private Subscription subscriptions = () -> {};

    private final Binding<Boolean> caretVisible;

    private final Val<UnaryOperator<Point2D>> _popupAnchorAdjustment;

    private final VirtualFlow<Paragraph<PS, S>, Cell<Paragraph<PS, S>, ParagraphBox<PS, S>>> virtualFlow;

    // used for two-level navigation, where on the higher level are
    // paragraphs and on the lower level are lines within a paragraph
    private final TwoLevelNavigator navigator;

    private boolean followCaretRequested = false;

    /**
     * model
     */
    private final StyledTextAreaModel<PS, S> model;

    /**
     * @return this area's {@link StyledTextAreaModel}
     */
    final StyledTextAreaModel<PS, S> getModel() {
        return model;
    }

    /* ********************************************************************** *
     *                                                                        *
     * Fields necessary for Cloning                                           *
     *                                                                        *
     * ********************************************************************** */

    /**
     * The underlying document that can be displayed by multiple {@code StyledTextArea}s.
     */
    public final EditableStyledDocument<PS, S> getContent() { return model.getContent(); }

    /**
     * Style used by default when no other style is provided.
     */
    public final S getInitialTextStyle() { return model.getInitialTextStyle(); }

    /**
     * Style used by default when no other style is provided.
     */
    public final PS getInitialParagraphStyle() { return model.getInitialParagraphStyle(); }

    /**
     * Style applicator used by the default skin.
     */
    private final BiConsumer<? super TextExt, S> applyStyle;
    public final BiConsumer<? super TextExt, S> getApplyStyle() { return applyStyle; }

    /**
     * Style applicator used by the default skin.
     */
    private final BiConsumer<TextFlow, PS> applyParagraphStyle;
    public final BiConsumer<TextFlow, PS> getApplyParagraphStyle() { return applyParagraphStyle; }

    /**
     * Indicates whether style should be preserved on undo/redo,
     * copy/paste and text move.
     * TODO: Currently, only undo/redo respect this flag.
     */
    public final boolean isPreserveStyle() { return model.isPreserveStyle(); }

    /* ********************************************************************** *
     *                                                                        *
     * Constructors                                                           *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Creates a text area with empty text content.
     *
     * @param initialTextStyle style to use in places where no other style is
     * specified (yet).
     * @param applyStyle function that, given a {@link Text} node and
     * a style, applies the style to the text node. This function is
     * used by the default skin to apply style to text nodes.
     * @param initialParagraphStyle style to use in places where no other style is
     * specified (yet).
     * @param applyParagraphStyle function that, given a {@link TextFlow} node and
     * a style, applies the style to the paragraph node. This function is
     * used by the default skin to apply style to paragraph nodes.
     */
    public StyledTextArea(PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
                          S initialTextStyle, BiConsumer<? super TextExt, S> applyStyle
    ) {
        this(initialParagraphStyle, applyParagraphStyle, initialTextStyle, applyStyle, true);
    }

    public StyledTextArea(PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
                              S initialTextStyle, BiConsumer<? super TextExt, S> applyStyle,
                              boolean preserveStyle
    ) {
        this(initialParagraphStyle, applyParagraphStyle, initialTextStyle, applyStyle,
                new SimpleEditableStyledDocument<>(initialParagraphStyle, initialTextStyle), preserveStyle);
    }

    /**
     * The same as {@link #StyledTextArea(Object, BiConsumer, Object, BiConsumer)} except that
     * this constructor can be used to create another {@code StyledTextArea} object that
     * shares the same {@link EditableStyledDocument}.
     */
    public StyledTextArea(PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
                          S initialTextStyle, BiConsumer<? super TextExt, S> applyStyle,
                          EditableStyledDocument<PS, S> document
    ) {
        this(initialParagraphStyle, applyParagraphStyle, initialTextStyle, applyStyle, document, true);

    }

    public StyledTextArea(PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
                          S initialTextStyle, BiConsumer<? super TextExt, S> applyStyle,
                          EditableStyledDocument<PS, S> document, boolean preserveStyle
    ) {
        this.model = new StyledTextAreaModel<>(initialParagraphStyle, initialTextStyle, document, preserveStyle);
        this.applyStyle = applyStyle;
        this.applyParagraphStyle = applyParagraphStyle;

        // allow tab traversal into area
        setFocusTraversable(true);

        this.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        getStyleClass().add("styled-text-area");
        getStylesheets().add(StyledTextArea.class.getResource("styled-text-area.css").toExternalForm());

        // keeps track of currently used non-empty cells
        @SuppressWarnings("unchecked")
        ObservableSet<ParagraphBox<PS, S>> nonEmptyCells = FXCollections.observableSet();

        // Initialize content
        virtualFlow = VirtualFlow.createVertical(
                getParagraphs(),
                par -> {
                    Cell<Paragraph<PS, S>, ParagraphBox<PS, S>> cell = createCell(
                            par,
                            applyStyle,
                            applyParagraphStyle);
                    nonEmptyCells.add(cell.getNode());
                    return cell.beforeReset(() -> nonEmptyCells.remove(cell.getNode()))
                            .afterUpdateItem(p -> nonEmptyCells.add(cell.getNode()));
                });
        getChildren().add(virtualFlow);

        // initialize navigator
        IntSupplier cellCount = () -> getParagraphs().size();
        IntUnaryOperator cellLength = i -> virtualFlow.getCell(i).getNode().getLineCount();
        navigator = new TwoLevelNavigator(cellCount, cellLength);

        // follow the caret every time the caret position or paragraphs change
        EventStream<?> caretPosDirty = invalidationsOf(caretPositionProperty());
        EventStream<?> paragraphsDirty = invalidationsOf(getParagraphs());
        EventStream<?> selectionDirty = invalidationsOf(selectionProperty());
        // need to reposition popup even when caret hasn't moved, but selection has changed (been deselected)
        EventStream<?> caretDirty = merge(caretPosDirty, paragraphsDirty, selectionDirty);
        subscribeTo(caretDirty, x -> requestFollowCaret());

        // whether or not to display the caret
        EventStream<Boolean> blinkCaret = EventStreams.valuesOf(showCaretProperty())
                .flatMap(mode -> {
                    switch (mode) {
                        case ON:
                            return EventStreams.valuesOf(Val.constant(true));
                        case OFF:
                            return EventStreams.valuesOf(Val.constant(false));
                        default:
                        case AUTO:
                            return EventStreams.valuesOf(focusedProperty()
                                    .and(editableProperty())
                                    .and(disabledProperty().not()));
                        }
                });

        // the rate at which to display the caret
        EventStream<javafx.util.Duration> blinkRate = EventStreams.valuesOf(caretBlinkRate);

        // The caret is visible in periodic intervals,
        // but only when blinkCaret is true.
        caretVisible = EventStreams.combine(blinkCaret, blinkRate)
                .flatMap(tuple -> {
                    Boolean blink = tuple.get1();
                    javafx.util.Duration rate = tuple.get2();
                    if(blink) {
                        return rate.lessThanOrEqualTo(ZERO)
                            ? EventStreams.valuesOf(Val.constant(true))
                            : booleanPulse(rate, caretDirty);
                    } else {
                        return EventStreams.valuesOf(Val.constant(false));
                    }
                })
                .toBinding(false);
        manageBinding(caretVisible);

        // Adjust popup anchor by either a user-provided function,
        // or user-provided offset, or don't adjust at all.
        Val<UnaryOperator<Point2D>> userOffset = Val.map(
                popupAnchorOffsetProperty(),
                offset -> anchor -> anchor.add(offset));
        _popupAnchorAdjustment =
                Val.orElse(
                        popupAnchorAdjustmentProperty(),
                        userOffset)
                        .orElseConst(UnaryOperator.identity());

        // dispatch MouseOverTextEvents when mouseOverTextDelay is not null
        EventStreams.valuesOf(mouseOverTextDelayProperty())
                .flatMap(delay -> delay != null
                        ? mouseOverTextEvents(nonEmptyCells, delay)
                        : EventStreams.never())
                .subscribe(evt -> Event.fireEvent(this, evt));

        behavior = new StyledTextAreaBehavior(this);
    }


    /* ********************************************************************** *
     *                                                                        *
     * Queries                                                                *
     *                                                                        *
     * Queries are parameterized observables.                                 *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Returns caret bounds relative to the viewport, i.e. the visual bounds
     * of the embedded VirtualFlow.
     */
    Optional<Bounds> getCaretBounds() {
        return virtualFlow.getCellIfVisible(getCurrentParagraph())
                .map(c -> {
                    Bounds cellBounds = c.getNode().getCaretBounds();
                    return virtualFlow.cellToViewport(c, cellBounds);
                });
    }

    /**
     * Returns x coordinate of the caret in the current paragraph.
     */
    ParagraphBox.CaretOffsetX getCaretOffsetX() {
        int idx = getCurrentParagraph();
        return getCell(idx).getCaretOffsetX();
    }

    double getViewportHeight() {
        return virtualFlow.getHeight();
    }

    CharacterHit hit(ParagraphBox.CaretOffsetX x, TwoDimensional.Position targetLine) {
        int parIdx = targetLine.getMajor();
        ParagraphBox<PS, S> cell = virtualFlow.getCell(parIdx).getNode();
        CharacterHit parHit = cell.hitTextLine(x, targetLine.getMinor());
        return parHit.offset(getParagraphOffset(parIdx));
    }

    CharacterHit hit(ParagraphBox.CaretOffsetX x, double y) {
        VirtualFlowHit<Cell<Paragraph<PS, S>, ParagraphBox<PS, S>>> hit = virtualFlow.hit(0.0, y);
        if(hit.isBeforeCells()) {
            return CharacterHit.insertionAt(0);
        } else if(hit.isAfterCells()) {
            return CharacterHit.insertionAt(getLength());
        } else {
            int parIdx = hit.getCellIndex();
            int parOffset = getParagraphOffset(parIdx);
            ParagraphBox<PS, S> cell = hit.getCell().getNode();
            Point2D cellOffset = hit.getCellOffset();
            CharacterHit parHit = cell.hitText(x, cellOffset.getY());
            return parHit.offset(parOffset);
        }
    }

    /**
     * Helpful for determining which letter is at point x, y:
     * <pre>
     *     {@code
     *     StyledTextArea area = // creation code
     *     area.addEventHandler(MouseEvent.MOUSE_PRESSED, (MouseEvent e) -> {
     *         CharacterHit hit = area.hit(e.getX(), e.getY());
     *         int characterPosition = hit.getInsertionIndex();
     *
     *         // move the caret to that character's position
     *         area.moveTo(characterPosition, SelectionPolicy.CLEAR);
     *     }}
     * </pre>
     */
    public CharacterHit hit(double x, double y) {
        VirtualFlowHit<Cell<Paragraph<PS, S>, ParagraphBox<PS, S>>> hit = virtualFlow.hit(x, y);
        if(hit.isBeforeCells()) {
            return CharacterHit.insertionAt(0);
        } else if(hit.isAfterCells()) {
            return CharacterHit.insertionAt(getLength());
        } else {
            int parIdx = hit.getCellIndex();
            int parOffset = getParagraphOffset(parIdx);
            ParagraphBox<PS, S> cell = hit.getCell().getNode();
            Point2D cellOffset = hit.getCellOffset();
            CharacterHit parHit = cell.hit(cellOffset);
            return parHit.offset(parOffset);
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
    TwoDimensional.Position currentLine() {
        int parIdx = getCurrentParagraph();
        Cell<Paragraph<PS, S>, ParagraphBox<PS, S>> cell = virtualFlow.getCell(parIdx);
        int lineIdx = cell.getNode().getCurrentLineIndex();
        return _position(parIdx, lineIdx);
    }

    TwoDimensional.Position _position(int par, int line) {
        return navigator.position(par, line);
    }

    @Override
    public final String getText(int start, int end) {
        return model.getText(start, end);
    }

    @Override
    public String getText(int paragraph) {
        return model.getText(paragraph);
    }

    public Paragraph<PS, S> getParagraph(int index) {
        return model.getParagraph(index);
    }

    @Override
    public StyledDocument<PS, S> subDocument(int start, int end) {
        return model.subDocument(start, end);
    }

    @Override
    public StyledDocument<PS, S> subDocument(int paragraphIndex) {
        return model.subDocument(paragraphIndex);
    }

    /**
     * Returns the selection range in the given paragraph.
     */
    public IndexRange getParagraphSelection(int paragraph) {
        return model.getParagraphSelection(paragraph);
    }

    /**
     * Returns the style of the character with the given index.
     * If {@code index} points to a line terminator character,
     * the last style used in the paragraph terminated by that
     * line terminator is returned.
     */
    public S getStyleOfChar(int index) {
        return model.getStyleOfChar(index);
    }

    /**
     * Returns the style at the given position. That is the style of the
     * character immediately preceding {@code position}, except when
     * {@code position} points to a paragraph boundary, in which case it
     * is the style at the beginning of the latter paragraph.
     *
     * <p>In other words, most of the time {@code getStyleAtPosition(p)}
     * is equivalent to {@code getStyleOfChar(p-1)}, except when {@code p}
     * points to a paragraph boundary, in which case it is equivalent to
     * {@code getStyleOfChar(p)}.
     */
    public S getStyleAtPosition(int position) {
        return model.getStyleAtPosition(position);
    }

    /**
     * Returns the range of homogeneous style that includes the given position.
     * If {@code position} points to a boundary between two styled ranges, then
     * the range preceding {@code position} is returned. If {@code position}
     * points to a boundary between two paragraphs, then the first styled range
     * of the latter paragraph is returned.
     */
    public IndexRange getStyleRangeAtPosition(int position) {
        return model.getStyleRangeAtPosition(position);
    }

    /**
     * Returns the styles in the given character range.
     */
    public StyleSpans<S> getStyleSpans(int from, int to) {
        return model.getStyleSpans(from, to);
    }

    /**
     * Returns the styles in the given character range.
     */
    public StyleSpans<S> getStyleSpans(IndexRange range) {
        return getStyleSpans(range.getStart(), range.getEnd());
    }

    /**
     * Returns the style of the character with the given index in the given
     * paragraph. If {@code index} is beyond the end of the paragraph, the
     * style at the end of line is returned. If {@code index} is negative, it
     * is the same as if it was 0.
     */
    public S getStyleOfChar(int paragraph, int index) {
        return model.getStyleOfChar(paragraph, index);
    }

    /**
     * Returns the style at the given position in the given paragraph.
     * This is equivalent to {@code getStyleOfChar(paragraph, position-1)}.
     */
    public S getStyleAtPosition(int paragraph, int position) {
        return model.getStyleAtPosition(paragraph, position);
    }

    /**
     * Returns the range of homogeneous style that includes the given position
     * in the given paragraph. If {@code position} points to a boundary between
     * two styled ranges, then the range preceding {@code position} is returned.
     */
    public IndexRange getStyleRangeAtPosition(int paragraph, int position) {
        return model.getStyleRangeAtPosition(paragraph, position);
    }

    /**
     * Returns styles of the whole paragraph.
     */
    public StyleSpans<S> getStyleSpans(int paragraph) {
        return model.getStyleSpans(paragraph);
    }

    /**
     * Returns the styles in the given character range of the given paragraph.
     */
    public StyleSpans<S> getStyleSpans(int paragraph, int from, int to) {
        return model.getStyleSpans(paragraph, from, to);
    }

    /**
     * Returns the styles in the given character range of the given paragraph.
     */
    public StyleSpans<S> getStyleSpans(int paragraph, IndexRange range) {
        return getStyleSpans(paragraph, range.getStart(), range.getEnd());
    }

    @Override
    public int getAbsolutePosition(int paragraphIndex, int columnIndex) {
        return model.getAbsolutePosition(paragraphIndex, columnIndex);
    }

    @Override
    public Position position(int row, int col) {
        return model.position(row, col);
    }

    @Override
    public Position offsetToPosition(int charOffset, Bias bias) {
        return model.offsetToPosition(charOffset, bias);
    }


    /* ********************************************************************** *
     *                                                                        *
     * Actions                                                                *
     *                                                                        *
     * Actions change the state of this control. They typically cause a       *
     * change of one or more observables and/or produce an event.             *
     *                                                                        *
     * ********************************************************************** */

    void scrollBy(Point2D deltas) {
        virtualFlow.scrollXBy(deltas.getX());
        virtualFlow.scrollYBy(deltas.getY());
    }

    void show(double y) {
        virtualFlow.show(y);
    }

    void showCaretAtBottom() {
        int parIdx = getCurrentParagraph();
        Cell<Paragraph<PS, S>, ParagraphBox<PS, S>> cell = virtualFlow.getCell(parIdx);
        Bounds caretBounds = cell.getNode().getCaretBounds();
        double y = caretBounds.getMaxY();
        virtualFlow.showAtOffset(parIdx, getViewportHeight() - y);
    }

    void showCaretAtTop() {
        int parIdx = getCurrentParagraph();
        Cell<Paragraph<PS, S>, ParagraphBox<PS, S>> cell = virtualFlow.getCell(parIdx);
        Bounds caretBounds = cell.getNode().getCaretBounds();
        double y = caretBounds.getMinY();
        virtualFlow.showAtOffset(parIdx, -y);
    }

    void requestFollowCaret() {
        followCaretRequested = true;
        requestLayout();
    }

    private void followCaret() {
        int parIdx = getCurrentParagraph();
        Cell<Paragraph<PS, S>, ParagraphBox<PS, S>> cell = virtualFlow.getCell(parIdx);
        Bounds caretBounds = cell.getNode().getCaretBounds();
        double graphicWidth = cell.getNode().getGraphicPrefWidth();
        Bounds region = extendLeft(caretBounds, graphicWidth);
        virtualFlow.show(parIdx, region);
    }

    /**
     * Sets style for the given character range.
     */
    public void setStyle(int from, int to, S style) {
        model.setStyle(from, to, style);
    }

    /**
     * Sets style for the whole paragraph.
     */
    public void setStyle(int paragraph, S style) {
        model.setStyle(paragraph, style);
    }

    /**
     * Sets style for the given range relative in the given paragraph.
     */
    public void setStyle(int paragraph, int from, int to, S style) {
        model.setStyle(paragraph, from, to, style);
    }

    /**
     * Set multiple style ranges at once. This is equivalent to
     * <pre>
     * for(StyleSpan{@code <S>} span: styleSpans) {
     *     setStyle(from, from + span.getLength(), span.getStyle());
     *     from += span.getLength();
     * }
     * </pre>
     * but the actual implementation is more efficient.
     */
    public void setStyleSpans(int from, StyleSpans<? extends S> styleSpans) {
        model.setStyleSpans(from, styleSpans);
    }

    /**
     * Set multiple style ranges of a paragraph at once. This is equivalent to
     * <pre>
     * for(StyleSpan{@code <S>} span: styleSpans) {
     *     setStyle(paragraph, from, from + span.getLength(), span.getStyle());
     *     from += span.getLength();
     * }
     * </pre>
     * but the actual implementation is more efficient.
     */
    public void setStyleSpans(int paragraph, int from, StyleSpans<? extends S> styleSpans) {
        model.setStyleSpans(paragraph, from, styleSpans);
    }

    /**
     * Sets style for the whole paragraph.
     */
    public void setParagraphStyle(int paragraph, PS paragraphStyle) {
        model.setParagraphStyle(paragraph, paragraphStyle);
    }

    /**
     * Resets the style of the given range to the initial style.
     */
    public void clearStyle(int from, int to) {
        model.clearStyle(from, to);
    }

    /**
     * Resets the style of the given paragraph to the initial style.
     */
    public void clearStyle(int paragraph) {
        model.clearStyle(paragraph);
    }

    /**
     * Resets the style of the given range in the given paragraph
     * to the initial style.
     */
    public void clearStyle(int paragraph, int from, int to) {
        model.clearStyle(paragraph, from, to);
    }

    /**
     * Resets the style of the given paragraph to the initial style.
     */
    public void clearParagraphStyle(int paragraph) {
        model.clearParagraphStyle(paragraph);
    }

    @Override
    public void replaceText(int start, int end, String text) {
        model.replaceText(start, end, text);
    }

    @Override
    public void replace(int start, int end, StyledDocument<PS, S> replacement) {
        model.replace(start, end, replacement);
    }

    @Override
    public void selectRange(int anchor, int caretPosition) {
        model.selectRange(anchor, caretPosition);
    }

    /**
     * {@inheritDoc}
     * @deprecated You probably meant to use {@link #moveTo(int)}. This method will be made
     * package-private in the future
     */
    @Deprecated
    @Override
    public void positionCaret(int pos) {
        model.positionCaret(pos);
    }

    /* ********************************************************************** *
     *                                                                        *
     * Public API                                                             *
     *                                                                        *
     * ********************************************************************** */

    public void dispose() {
        subscriptions.unsubscribe();
        model.dispose();
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
        PopupWindow popup = getPopupWindow();
        PopupAlignment alignment = getPopupAlignment();
        UnaryOperator<Point2D> adjustment = _popupAnchorAdjustment.getValue();
        if(popup != null) {
            positionPopup(popup, alignment, adjustment);
        }
    }

    /* ********************************************************************** *
     *                                                                        *
     * Private methods                                                        *
     *                                                                        *
     * ********************************************************************** */

    private Cell<Paragraph<PS, S>, ParagraphBox<PS, S>> createCell(
            Paragraph<PS, S> paragraph,
            BiConsumer<? super TextExt, S> applyStyle,
            BiConsumer<TextFlow, PS> applyParagraphStyle) {

        ParagraphBox<PS, S> box = new ParagraphBox<>(paragraph, applyParagraphStyle, applyStyle);

        box.highlightFillProperty().bind(highlightFill);
        box.highlightTextFillProperty().bind(highlightTextFill);
        box.wrapTextProperty().bind(wrapTextProperty());
        box.graphicFactoryProperty().bind(paragraphGraphicFactoryProperty());
        box.graphicOffset.bind(virtualFlow.breadthOffsetProperty());

        Val<Boolean> hasCaret = Val.combine(
                box.indexProperty(),
                currentParagraphProperty(),
                (bi, cp) -> bi.intValue() == cp.intValue());

        Subscription hasCaretPseudoClass = hasCaret.values().subscribe(value -> box.pseudoClassStateChanged(HAS_CARET, value));
        Subscription firstParPseudoClass = box.indexProperty().values().subscribe(idx -> box.pseudoClassStateChanged(FIRST_PAR, idx == 0));
        Subscription lastParPseudoClass = EventStreams.combine(
                box.indexProperty().values(),
                getParagraphs().sizeProperty().values()
        ).subscribe(in -> in.exec((i, n) -> box.pseudoClassStateChanged(LAST_PAR, i == n-1)));

        // caret is visible only in the paragraph with the caret
        Val<Boolean> cellCaretVisible = hasCaret.flatMap(x -> x ? caretVisible : Val.constant(false));
        box.caretVisibleProperty().bind(cellCaretVisible);

        // bind cell's caret position to area's caret column,
        // when the cell is the one with the caret
        box.caretPositionProperty().bind(hasCaret.flatMap(has -> has
                ? caretColumnProperty()
                : Val.constant(0)));

        // keep paragraph selection updated
        ObjectBinding<IndexRange> cellSelection = Bindings.createObjectBinding(() -> {
            int idx = box.getIndex();
            return idx != -1
                    ? getParagraphSelection(idx)
                    : StyledTextArea.EMPTY_RANGE;
        }, selectionProperty(), box.indexProperty());
        box.selectionProperty().bind(cellSelection);

        return new Cell<Paragraph<PS, S>, ParagraphBox<PS, S>>() {
            @Override
            public ParagraphBox<PS, S> getNode() {
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

                hasCaretPseudoClass.unsubscribe();
                firstParPseudoClass.unsubscribe();
                lastParPseudoClass.unsubscribe();

                box.caretVisibleProperty().unbind();
                box.caretPositionProperty().unbind();

                box.selectionProperty().unbind();
                cellSelection.dispose();
            }
        };
    }

    private ParagraphBox<PS, S> getCell(int index) {
        return virtualFlow.getCell(index).getNode();
    }

    private EventStream<MouseOverTextEvent> mouseOverTextEvents(ObservableSet<ParagraphBox<PS, S>> cells, Duration delay) {
        return merge(cells, c -> c.stationaryIndices(delay).map(e -> e.unify(
                l -> l.map((pos, charIdx) -> MouseOverTextEvent.beginAt(c.localToScreen(pos), getParagraphOffset(c.getIndex()) + charIdx)),
                r -> MouseOverTextEvent.end())));
    }

    private int getParagraphOffset(int parIdx) {
        return position(parIdx, 0).toOffset();
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
        return virtualFlow.getCellIfVisible(getCurrentParagraph())
                .map(c -> c.getNode().getCaretBoundsOnScreen());
    }

    private Optional<Bounds> getSelectionBoundsOnScreen() {
        IndexRange selection = getSelection();
        if(selection.getLength() == 0) {
            return getCaretBoundsOnScreen();
        }

        Bounds[] bounds = virtualFlow.visibleCells().stream()
                .map(c -> c.getNode().getSelectionBoundsOnScreen())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toArray(Bounds[]::new);

        if(bounds.length == 0) {
            return Optional.empty();
        }
        double minX = Stream.of(bounds).mapToDouble(Bounds::getMinX).min().getAsDouble();
        double maxX = Stream.of(bounds).mapToDouble(Bounds::getMaxX).max().getAsDouble();
        double minY = Stream.of(bounds).mapToDouble(Bounds::getMinY).min().getAsDouble();
        double maxY = Stream.of(bounds).mapToDouble(Bounds::getMaxY).max().getAsDouble();
        return Optional.of(new BoundingBox(minX, minY, maxX-minX, maxY-minY));
    }

    private <T> void subscribeTo(EventStream<T> src, Consumer<T> cOnsumer) {
        manageSubscription(src.subscribe(cOnsumer));
    }

    private void manageSubscription(Subscription subscription) {
        subscriptions = subscriptions.and(subscription);
    }

    private void manageBinding(Binding<?> binding) {
        subscriptions = subscriptions.and(binding::dispose);
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

    private static EventStream<Boolean> booleanPulse(javafx.util.Duration javafxDuration, EventStream<?> restartImpulse) {
        Duration duration = Duration.ofMillis(Math.round(javafxDuration.toMillis()));
        EventStream<?> ticks = EventStreams.restartableTicks(duration, restartImpulse);
        return StateMachine.init(false)
                .on(restartImpulse.withDefaultEvent(null)).transition((state, impulse) -> true)
                .on(ticks).transition((state, tick) -> !state)
                .toStateStream();
    }
}
