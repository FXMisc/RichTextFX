package org.fxmisc.richtext;

import static org.fxmisc.richtext.PopupAlignment.*;
import static org.fxmisc.richtext.TwoDimensional.Bias.*;
import static org.reactfx.EventStreams.*;
import static org.reactfx.util.Tuples.*;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
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
import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.UndoManagerFactory;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.Guard;
import org.reactfx.StateMachine;
import org.reactfx.Subscription;
import org.reactfx.Suspendable;
import org.reactfx.SuspendableEventStream;
import org.reactfx.SuspendableNo;
import org.reactfx.collection.LiveList;
import org.reactfx.collection.SuspendableList;
import org.reactfx.util.Tuple2;
import org.reactfx.value.SuspendableVal;
import org.reactfx.value.SuspendableVar;
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
 * of the viewport. To add scroll bars, the area needs to be embedded in
 * a {@link VirtualizedScrollPane}. {@link AreaFactory} is provided to make
 * this more convenient.</p>
 *
 * <h3>Overriding keyboard shortcuts</h3>
 *
 * {@code StyledTextArea} comes with {@link #onKeyTypedProperty()} and
 * {@link #onKeyPressedProperty()} handlers installed to handle keyboard input.
 * Ordinary character input is handled by the {@code onKeyTyped} handler and
 * control key combinations (including Enter and Tab) are handled by the
 * {@code onKeyPressed} handler. To add or override some keyboard shortcuts,
 * but keep the rest in place, you would combine the default event handler with
 * a new one that adds or overrides some of the default key combinations. This
 * is how to bind {@code Ctrl+S} to the {@code save()} operation:
 * <pre>
 * {@code
 * import static javafx.scene.input.KeyCode.*;
 * import static javafx.scene.input.KeyCombination.*;
 * import static org.fxmisc.wellbehaved.event.EventPattern.*;
 *
 * import org.fxmisc.wellbehaved.event.EventHandlerHelper;
 *
 * EventHandler<? super KeyEvent> ctrlS = EventHandlerHelper
 *         .on(keyPressed(S, CONTROL_DOWN)).act(event -> save())
 *         .create();
 *
 * EventHandlerHelper.install(area.onKeyPressedProperty(), ctrlS);
 * }
 * </pre>
 *
 * @param <S> type of style that can be applied to text.
 */
public class StyledTextArea<S, PS> extends Region
        implements
        TextEditingArea<S, PS>,
        EditActions<S, PS>,
        ClipboardActions<S, PS>,
        NavigationActions<S, PS>,
        UndoActions<S>,
        TwoDimensional,
        Virtualized {

    /**
     * Index range [0, 0).
     */
    public static final IndexRange EMPTY_RANGE = new IndexRange(0, 0);

    /**
     * Private helper method.
     */
    private static int clamp(int min, int val, int max) {
        return val < min ? min
             : val > max ? max
             : val;
    }


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

    // editable property
    private final BooleanProperty editable = new EditableProperty<>(this);
    @Override public final boolean isEditable() { return editable.get(); }
    @Override public final void setEditable(boolean value) { editable.set(value); }
    @Override public final BooleanProperty editableProperty() { return editable; }

    // wrapText property
    private final BooleanProperty wrapText = new SimpleBooleanProperty(this, "wrapText");
    @Override public final boolean isWrapText() { return wrapText.get(); }
    @Override public final void setWrapText(boolean value) { wrapText.set(value); }
    @Override public final BooleanProperty wrapTextProperty() { return wrapText; }

    // undo manager
    private UndoManager undoManager;
    @Override
    public UndoManager getUndoManager() { return undoManager; }
    @Override
    public void setUndoManager(UndoManagerFactory undoManagerFactory) {
        undoManager.close();
        undoManager = preserveStyle
                ? createRichUndoManager(undoManagerFactory)
                : createPlainUndoManager(undoManagerFactory);
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
    public BooleanProperty useInitialStyleForInsertionProperty() { return content.useInitialStyleForInsertion; }
    public void setUseInitialStyleForInsertion(boolean value) { content.useInitialStyleForInsertion.set(value); }
    public boolean getUseInitialStyleForInsertion() { return content.useInitialStyleForInsertion.get(); }

    private Optional<Tuple2<Codec<S>, Codec<PS>>> styleCodecs = Optional.empty();
    /**
     * Sets codecs to encode/decode style information to/from binary format.
     * Providing codecs enables clipboard actions to retain the style information.
     */
    public void setStyleCodecs(Codec<S> textStyleCodec, Codec<PS> paragraphStyleCodec) {
        styleCodecs = Optional.of(t(textStyleCodec, paragraphStyleCodec));
    }
    @Override
    public Optional<Tuple2<Codec<S>, Codec<PS>>> getStyleCodecs() {
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
    private final SuspendableVal<String> text;
    @Override public final String getText() { return text.getValue(); }
    @Override public final ObservableValue<String> textProperty() { return text; }

    // rich text
    @Override public final StyledDocument<S, PS> getDocument() { return content.snapshot(); };

    // length
    private final SuspendableVal<Integer> length;
    @Override public final int getLength() { return length.getValue(); }
    @Override public final ObservableValue<Integer> lengthProperty() { return length; }

    // caret position
    private final Var<Integer> internalCaretPosition = Var.newSimpleVar(0);
    private final SuspendableVal<Integer> caretPosition = internalCaretPosition.suspendable();
    @Override public final int getCaretPosition() { return caretPosition.getValue(); }
    @Override public final ObservableValue<Integer> caretPositionProperty() { return caretPosition; }

    // selection anchor
    private final SuspendableVar<Integer> anchor = Var.newSimpleVar(0).suspendable();
    @Override public final int getAnchor() { return anchor.getValue(); }
    @Override public final ObservableValue<Integer> anchorProperty() { return anchor; }

    // selection
    private final Var<IndexRange> internalSelection = Var.newSimpleVar(EMPTY_RANGE);
    private final SuspendableVal<IndexRange> selection = internalSelection.suspendable();
    @Override public final IndexRange getSelection() { return selection.getValue(); }
    @Override public final ObservableValue<IndexRange> selectionProperty() { return selection; }

    // selected text
    private final SuspendableVal<String> selectedText;
    @Override public final String getSelectedText() { return selectedText.getValue(); }
    @Override public final ObservableValue<String> selectedTextProperty() { return selectedText; }

    // current paragraph index
    private final SuspendableVal<Integer> currentParagraph;
    @Override public final int getCurrentParagraph() { return currentParagraph.getValue(); }
    @Override public final ObservableValue<Integer> currentParagraphProperty() { return currentParagraph; }

    // caret column
    private final SuspendableVal<Integer> caretColumn;
    @Override public final int getCaretColumn() { return caretColumn.getValue(); }
    @Override public final ObservableValue<Integer> caretColumnProperty() { return caretColumn; }

    // paragraphs
    private final SuspendableList<Paragraph<S, PS>> paragraphs;
    @Override public ObservableList<Paragraph<S, PS>> getParagraphs() {
        return paragraphs;
    }

    // beingUpdated
    private final SuspendableNo beingUpdated = new SuspendableNo();
    public ObservableBooleanValue beingUpdatedProperty() { return beingUpdated; }
    public boolean isBeingUpdated() { return beingUpdated.get(); }

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
    private final SuspendableEventStream<PlainTextChange> plainTextChanges;
    @Override
    public final EventStream<PlainTextChange> plainTextChanges() { return plainTextChanges; }

    // rich text changes
    private final SuspendableEventStream<RichTextChange<S, PS>> richTextChanges;
    @Override
    public final EventStream<RichTextChange<S, PS>> richChanges() { return richTextChanges; }

    /* ********************************************************************** *
     *                                                                        *
     * Private fields                                                         *
     *                                                                        *
     * ********************************************************************** */

    private final StyledTextAreaBehavior behavior;

    private Subscription subscriptions = () -> {};

    private final Binding<Boolean> caretVisible;

    private final Val<UnaryOperator<Point2D>> _popupAnchorAdjustment;

    private final VirtualFlow<Paragraph<S, PS>, Cell<Paragraph<S, PS>, ParagraphBox<S, PS>>> virtualFlow;

    // used for two-level navigation, where on the higher level are
    // paragraphs and on the lower level are lines within a paragraph
    private final TwoLevelNavigator navigator;

    private boolean followCaretRequested = false;

    private Position selectionStart2D;
    private Position selectionEnd2D;

    /**
     * content model
     */
    private final EditableStyledDocument<S, PS> content;
    protected final EditableStyledDocument<S, PS> getCloneDocument() {
        return content;
    }

    /**
     * Style used by default when no other style is provided.
     */
    private final S initialStyle;
    protected final S getInitialStyle() {
        return initialStyle;
    }

    /**
     * Style used by default when no other style is provided.
     */
    private final PS initialParagraphStyle;
    protected final PS getInitialParagraphStyle() {
        return initialParagraphStyle;
    }

    /**
     * Style applicator used by the default skin.
     */
    private final BiConsumer<? super TextExt, S> applyStyle;
    protected final BiConsumer<? super TextExt, S> getApplyStyle() {
        return applyStyle;
    }

    /**
     * Style applicator used by the default skin.
     */
    private final BiConsumer<TextFlow, PS> applyParagraphStyle;
    protected final BiConsumer<TextFlow, PS> getApplyParagraphStyle() {
        return applyParagraphStyle;
    }

    /**
     * Indicates whether style should be preserved on undo/redo,
     * copy/paste and text move.
     * TODO: Currently, only undo/redo respect this flag.
     */
    private final boolean preserveStyle;
    protected final boolean isPreserveStyle() {
        return preserveStyle;
    }

    private final Suspendable omniSuspendable;


    /* ********************************************************************** *
     *                                                                        *
     * Constructors                                                           *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Creates a text area with empty text content.
     *
     * @param initialStyle style to use in places where no other style is
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
    public StyledTextArea(S initialStyle, BiConsumer<? super TextExt, S> applyStyle,
                          PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle
    ) {
        this(initialStyle, applyStyle, initialParagraphStyle, applyParagraphStyle, true);
    }

    public <C> StyledTextArea(S initialStyle, BiConsumer<? super TextExt, S> applyStyle,
                              PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
                              boolean preserveStyle
    ) {
        this(initialStyle, applyStyle, initialParagraphStyle, applyParagraphStyle,
                new EditableStyledDocument<S, PS>(initialStyle, initialParagraphStyle), preserveStyle);
    }

    /**
     * The same as {@link #StyledTextArea(Object, BiConsumer, Object, BiConsumer)} except that
     * this constructor can be used to create another {@code StyledTextArea} object that
     * shares the same {@link EditableStyledDocument}.
     */
    public StyledTextArea(S initialStyle, BiConsumer<? super TextExt, S> applyStyle,
                          PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
                          EditableStyledDocument<S, PS> document
    ) {
        this(initialStyle, applyStyle, initialParagraphStyle, applyParagraphStyle, document, true);

    }

    public StyledTextArea(S initialStyle, BiConsumer<? super TextExt, S> applyStyle,
                          PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
                          EditableStyledDocument<S, PS> document, boolean preserveStyle
    ) {
        this.initialStyle = initialStyle;
        this.initialParagraphStyle = initialParagraphStyle;
        this.applyStyle = applyStyle;
        this.applyParagraphStyle = applyParagraphStyle;
        this.preserveStyle = preserveStyle;
        content = document;
        paragraphs = LiveList.suspendable(content.getParagraphs());

        text = Val.suspendable(content.textProperty());
        length = Val.suspendable(content.lengthProperty());
        plainTextChanges = content.plainTextChanges().pausable();
        richTextChanges = content.richChanges().pausable();

        // when content is updated by an area, update the caret
        // and selection ranges of all the other
        // clones that also share this document
        subscribeTo(content.plainTextChanges(), plainTextChange -> {
            int changeLength = plainTextChange.getInserted().length() - plainTextChange.getRemoved().length();
            if (changeLength != 0) {
                int indexOfChange = plainTextChange.getPosition();
                // in case of a replacement: "hello there" -> "hi."
                int endOfChange = indexOfChange + Math.abs(changeLength);

                // update caret
                int caretPosition = getCaretPosition();
                if (indexOfChange < caretPosition) {
                    // if caret is within the changed content, move it to indexOfChange
                    // otherwise offset it by changeLength
                    positionCaret(
                        caretPosition < endOfChange
                            ? indexOfChange
                            : caretPosition + changeLength
                    );
                }
                // update selection
                int selectionStart = getSelection().getStart();
                int selectionEnd = getSelection().getEnd();
                if (selectionStart != selectionEnd) {
                    // if start/end is within the changed content, move it to indexOfChange
                    // otherwise, offset it by changeLength
                    // Note: if both are moved to indexOfChange, selection is empty.
                    if (indexOfChange < selectionStart) {
                        selectionStart = selectionStart < endOfChange
                                ? indexOfChange
                                : selectionStart + changeLength;
                    }
                    if (indexOfChange < selectionEnd) {
                        selectionEnd = selectionEnd < endOfChange
                                ? indexOfChange
                                : selectionEnd + changeLength;
                    }
                    selectRange(selectionStart, selectionEnd);
                } else {
                    // force-update internalSelection in case caret is
                    // at the end of area and a character was deleted
                    // (prevents a StringIndexOutOfBoundsException because
                    // selection's end is one char farther than area's length).
                    int internalCaretPos = internalCaretPosition.getValue();
                    selectRange(internalCaretPos, internalCaretPos);
                }
            }
        });

        undoManager = preserveStyle
                ? createRichUndoManager(UndoManagerFactory.unlimitedHistoryFactory())
                : createPlainUndoManager(UndoManagerFactory.unlimitedHistoryFactory());

        Val<Position> caretPosition2D = Val.create(
                () -> content.offsetToPosition(internalCaretPosition.getValue(), Forward),
                internalCaretPosition, paragraphs);

        currentParagraph = caretPosition2D.map(Position::getMajor).suspendable();
        caretColumn = caretPosition2D.map(Position::getMinor).suspendable();

        selectionStart2D = position(0, 0);
        selectionEnd2D = position(0, 0);
        internalSelection.addListener(obs -> {
            IndexRange sel = internalSelection.getValue();
            selectionStart2D = offsetToPosition(sel.getStart(), Forward);
            selectionEnd2D = sel.getLength() == 0
                    ? selectionStart2D
                    : selectionStart2D.offsetBy(sel.getLength(), Backward);
        });

        selectedText = Val.create(
                () -> content.getText(internalSelection.getValue()),
                internalSelection, content.getParagraphs()).suspendable();

        omniSuspendable = Suspendable.combine(
                beingUpdated, // must be first, to be the last one to release
                text,
                length,
                caretPosition,
                anchor,
                selection,
                selectedText,
                currentParagraph,
                caretColumn,

                // add streams after properties, to be released before them
                plainTextChanges,
                richTextChanges,

                // paragraphs to be released first
                paragraphs);

        this.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        getStyleClass().add("styled-text-area");

        // CONSTRUCT THE SKIN

        // keeps track of currently used non-empty cells
        @SuppressWarnings("unchecked")
        ObservableSet<ParagraphBox<S, PS>> nonEmptyCells = FXCollections.observableSet();

        // Initialize content
        virtualFlow = VirtualFlow.createVertical(
                getParagraphs(),
                par -> {
                    Cell<Paragraph<S, PS>, ParagraphBox<S, PS>> cell = createCell(
                            par,
                            applyStyle,
                            initialParagraphStyle,
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

        // whether or not to animate the caret
        BooleanBinding blinkCaret = focusedProperty()
                .and(editableProperty())
                .and(disabledProperty().not());
        manageBinding(blinkCaret);

        // The caret is visible in periodic intervals,
        // but only when blinkCaret is true.
        caretVisible = EventStreams.valuesOf(blinkCaret)
                .flatMap(blink -> blink
                        ? booleanPulse(Duration.ofMillis(500), caretDirty)
                        : EventStreams.valuesOf(Val.constant(false)))
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
        ParagraphBox<S, PS> cell = virtualFlow.getCell(parIdx).getNode();
        CharacterHit parHit = cell.hitTextLine(x, targetLine.getMinor());
        return parHit.offset(getParagraphOffset(parIdx));
    }

    CharacterHit hit(ParagraphBox.CaretOffsetX x, double y) {
        VirtualFlowHit<Cell<Paragraph<S, PS>, ParagraphBox<S, PS>>> hit = virtualFlow.hit(0.0, y);
        if(hit.isBeforeCells()) {
            return CharacterHit.insertionAt(0);
        } else if(hit.isAfterCells()) {
            return CharacterHit.insertionAt(getLength());
        } else {
            int parIdx = hit.getCellIndex();
            int parOffset = getParagraphOffset(parIdx);
            ParagraphBox<S, PS> cell = hit.getCell().getNode();
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
     * @param x
     * @param y
     */
    public CharacterHit hit(double x, double y) {
        VirtualFlowHit<Cell<Paragraph<S, PS>, ParagraphBox<S, PS>>> hit = virtualFlow.hit(x, y);
        if(hit.isBeforeCells()) {
            return CharacterHit.insertionAt(0);
        } else if(hit.isAfterCells()) {
            return CharacterHit.insertionAt(getLength());
        } else {
            int parIdx = hit.getCellIndex();
            int parOffset = getParagraphOffset(parIdx);
            ParagraphBox<S, PS> cell = hit.getCell().getNode();
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
        Cell<Paragraph<S, PS>, ParagraphBox<S, PS>> cell = virtualFlow.getCell(parIdx);
        int lineIdx = cell.getNode().getCurrentLineIndex();
        return _position(parIdx, lineIdx);
    }

    TwoDimensional.Position _position(int par, int line) {
        return navigator.position(par, line);
    }

    @Override
    public final String getText(int start, int end) {
        return content.getText(start, end);
    }

    @Override
    public String getText(int paragraph) {
        return paragraphs.get(paragraph).toString();
    }

    public Paragraph<S, PS> getParagraph(int index) {
        return paragraphs.get(index);
    }

    @Override
    public StyledDocument<S, PS> subDocument(int start, int end) {
        return content.subSequence(start, end);
    }

    @Override
    public StyledDocument<S, PS> subDocument(int paragraphIndex) {
        return content.subDocument(paragraphIndex);
    }

    /**
     * Returns the selection range in the given paragraph.
     */
    public IndexRange getParagraphSelection(int paragraph) {
        int startPar = selectionStart2D.getMajor();
        int endPar = selectionEnd2D.getMajor();

        if(paragraph < startPar || paragraph > endPar) {
            return EMPTY_RANGE;
        }

        int start = paragraph == startPar ? selectionStart2D.getMinor() : 0;
        int end = paragraph == endPar ? selectionEnd2D.getMinor() : paragraphs.get(paragraph).length();

        // force selectionProperty() to be valid
        getSelection();

        return new IndexRange(start, end);
    }

    /**
     * Returns the style of the character with the given index.
     * If {@code index} points to a line terminator character,
     * the last style used in the paragraph terminated by that
     * line terminator is returned.
     */
    public S getStyleOfChar(int index) {
        return content.getStyleOfChar(index);
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
        return content.getStyleAtPosition(position);
    }

    /**
     * Returns the range of homogeneous style that includes the given position.
     * If {@code position} points to a boundary between two styled ranges, then
     * the range preceding {@code position} is returned. If {@code position}
     * points to a boundary between two paragraphs, then the first styled range
     * of the latter paragraph is returned.
     */
    public IndexRange getStyleRangeAtPosition(int position) {
        return content.getStyleRangeAtPosition(position);
    }

    /**
     * Returns the styles in the given character range.
     */
    public StyleSpans<S> getStyleSpans(int from, int to) {
        return content.getStyleSpans(from, to);
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
        return content.getStyleOfChar(paragraph, index);
    }

    /**
     * Returns the style at the given position in the given paragraph.
     * This is equivalent to {@code getStyleOfChar(paragraph, position-1)}.
     */
    public S getStyleAtPosition(int paragraph, int position) {
        return content.getStyleOfChar(paragraph, position);
    }

    /**
     * Returns the range of homogeneous style that includes the given position
     * in the given paragraph. If {@code position} points to a boundary between
     * two styled ranges, then the range preceding {@code position} is returned.
     */
    public IndexRange getStyleRangeAtPosition(int paragraph, int position) {
        return content.getStyleRangeAtPosition(paragraph, position);
    }

    /**
     * Returns styles of the whole paragraph.
     */
    public StyleSpans<S> getStyleSpans(int paragraph) {
        return content.getStyleSpans(paragraph);
    }

    /**
     * Returns the styles in the given character range of the given paragraph.
     */
    public StyleSpans<S> getStyleSpans(int paragraph, int from, int to) {
        return content.getStyleSpans(paragraph, from, to);
    }

    /**
     * Returns the styles in the given character range of the given paragraph.
     */
    public StyleSpans<S> getStyleSpans(int paragraph, IndexRange range) {
        return getStyleSpans(paragraph, range.getStart(), range.getEnd());
    }

    @Override
    public Position position(int row, int col) {
        return content.position(row, col);
    }

    @Override
    public Position offsetToPosition(int charOffset, Bias bias) {
        return content.offsetToPosition(charOffset, bias);
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
        Cell<Paragraph<S, PS>, ParagraphBox<S, PS>> cell = virtualFlow.getCell(parIdx);
        Bounds caretBounds = cell.getNode().getCaretBounds();
        double y = caretBounds.getMaxY();
        virtualFlow.showAtOffset(parIdx, getViewportHeight() - y);
    }

    void showCaretAtTop() {
        int parIdx = getCurrentParagraph();
        Cell<Paragraph<S, PS>, ParagraphBox<S, PS>> cell = virtualFlow.getCell(parIdx);
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
        Cell<Paragraph<S, PS>, ParagraphBox<S, PS>> cell = virtualFlow.getCell(parIdx);
        Bounds caretBounds = cell.getNode().getCaretBounds();
        double graphicWidth = cell.getNode().getGraphicPrefWidth();
        Bounds region = extendLeft(caretBounds, graphicWidth);
        virtualFlow.show(parIdx, region);
    }

    /**
     * Sets style for the given character range.
     */
    public void setStyle(int from, int to, S style) {
        try(Guard g = omniSuspendable.suspend()) {
            content.setStyle(from, to, style);
        }
    }

    /**
     * Sets style for the whole paragraph.
     */
    public void setStyle(int paragraph, S style) {
        try(Guard g = omniSuspendable.suspend()) {
            content.setStyle(paragraph, style);
        }
    }

    /**
     * Sets style for the given range relative in the given paragraph.
     */
    public void setStyle(int paragraph, int from, int to, S style) {
        try(Guard g = omniSuspendable.suspend()) {
            content.setStyle(paragraph, from, to, style);
        }
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
        try(Guard g = omniSuspendable.suspend()) {
            content.setStyleSpans(from, styleSpans);
        }
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
        try(Guard g = omniSuspendable.suspend()) {
            content.setStyleSpans(paragraph, from, styleSpans);
        }
    }

    /**
     * Sets style for the whole paragraph.
     */
    public void setParagraphStyle(int paragraph, PS paragraphStyle) {
        try(Guard g = omniSuspendable.suspend()) {
            content.setParagraphStyle(paragraph, paragraphStyle);
        }
    }

    /**
     * Resets the style of the given range to the initial style.
     */
    public void clearStyle(int from, int to) {
        setStyle(from, to, initialStyle);
    }

    /**
     * Resets the style of the given paragraph to the initial style.
     */
    public void clearStyle(int paragraph) {
        setStyle(paragraph, initialStyle);
    }

    /**
     * Resets the style of the given range in the given paragraph
     * to the initial style.
     */
    public void clearStyle(int paragraph, int from, int to) {
        setStyle(paragraph, from, to, initialStyle);
    }

    /**
     * Resets the style of the given paragraph to the initial style.
     */
    public void clearParagraphStyle(int paragraph) {
        setParagraphStyle(paragraph, initialParagraphStyle);
    }

    @Override
    public void replaceText(int start, int end, String text) {
        StyledDocument<S, PS> doc = ReadOnlyStyledDocument.fromString(
                text, content.getStyleForInsertionAt(start), content.getParagraphStyleForInsertionAt(start));
        replace(start, end, doc);
    }

    @Override
    public void replace(int start, int end, StyledDocument<S, PS> replacement) {
        try(Guard g = omniSuspendable.suspend()) {
            start = clamp(0, start, getLength());
            end = clamp(0, end, getLength());

            content.replace(start, end, replacement);

            int newCaretPos = start + replacement.length();
            selectRange(newCaretPos, newCaretPos);
        }
    }

    @Override
    public void selectRange(int anchor, int caretPosition) {
        try(Guard g = suspend(
                this.caretPosition, currentParagraph,
                caretColumn, this.anchor,
                selection, selectedText)) {
            this.internalCaretPosition.setValue(clamp(0, caretPosition, getLength()));
            this.anchor.setValue(clamp(0, anchor, getLength()));
            this.internalSelection.setValue(IndexRange.normalize(getAnchor(), getCaretPosition()));
        }
    }

    @Override
    public void positionCaret(int pos) {
        try(Guard g = suspend(caretPosition, currentParagraph, caretColumn)) {
            internalCaretPosition.setValue(pos);
        }
    }

    /* ********************************************************************** *
     *                                                                        *
     * Public API                                                             *
     *                                                                        *
     * ********************************************************************** */

    public void dispose() {
        subscriptions.unsubscribe();
        behavior.dispose();
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

    private Cell<Paragraph<S, PS>, ParagraphBox<S, PS>> createCell(
            Paragraph<S, PS> paragraph,
            BiConsumer<? super TextExt, S> applyStyle,
            PS initialParagraphStyle,
            BiConsumer<TextFlow, PS> applyParagraphStyle) {

        ParagraphBox<S, PS> box = new ParagraphBox<>(paragraph, applyStyle, applyParagraphStyle);

        box.highlightFillProperty().bind(highlightFill);
        box.highlightTextFillProperty().bind(highlightTextFill);
        box.wrapTextProperty().bind(wrapTextProperty());
        box.graphicFactoryProperty().bind(paragraphGraphicFactoryProperty());
        box.graphicOffset.bind(virtualFlow.breadthOffsetProperty());

        Val<Boolean> hasCaret = Val.combine(
                box.indexProperty(),
                currentParagraphProperty(),
                (bi, cp) -> bi.intValue() == cp.intValue());

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

        return new Cell<Paragraph<S, PS>, ParagraphBox<S, PS>>() {
            @Override
            public ParagraphBox<S, PS> getNode() {
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

    private ParagraphBox<S, PS> getCell(int index) {
        return virtualFlow.getCell(index).getNode();
    }

    private EventStream<MouseOverTextEvent> mouseOverTextEvents(ObservableSet<ParagraphBox<S, PS>> cells, Duration delay) {
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

    private static EventStream<Boolean> booleanPulse(Duration duration, EventStream<?> restartImpulse) {
        EventStream<?> ticks = EventStreams.restartableTicks(duration, restartImpulse);
        return StateMachine.init(false)
                .on(restartImpulse.withDefaultEvent(null)).transition((state, impulse) -> true)
                .on(ticks).transition((state, tick) -> !state)
                .toStateStream();
    }

    private UndoManager createPlainUndoManager(UndoManagerFactory factory) {
        Consumer<PlainTextChange> apply = change -> replaceText(change.getPosition(), change.getPosition() + change.getRemoved().length(), change.getInserted());
        BiFunction<PlainTextChange, PlainTextChange, Optional<PlainTextChange>> merge = (change1, change2) -> change1.mergeWith(change2);
        return factory.create(plainTextChanges(), PlainTextChange::invert, apply, merge);
    }

    private UndoManager createRichUndoManager(UndoManagerFactory factory) {
        Consumer<RichTextChange<S, PS>> apply = change -> replace(change.getPosition(), change.getPosition() + change.getRemoved().length(), change.getInserted());
        BiFunction<RichTextChange<S, PS>, RichTextChange<S, PS>, Optional<RichTextChange<S, PS>>> merge = (change1, change2) -> change1.mergeWith(change2);
        return factory.create(richChanges(), RichTextChange::invert, apply, merge);
    }

    private Guard suspend(Suspendable... suspendables) {
        return Suspendable.combine(beingUpdated, Suspendable.combine(suspendables)).suspend();
    }
}
