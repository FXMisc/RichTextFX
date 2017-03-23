package org.fxmisc.richtext;

import static javafx.util.Duration.*;
import static org.fxmisc.richtext.PopupAlignment.*;
import static org.fxmisc.richtext.model.TwoDimensional.Bias.Backward;
import static org.fxmisc.richtext.model.TwoDimensional.Bias.Forward;
import static org.reactfx.EventStreams.*;
import static org.reactfx.util.Tuples.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
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
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.event.Event;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.TextFlow;
import javafx.stage.PopupWindow;

import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualFlowHit;
import org.fxmisc.flowless.Virtualized;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CssProperties.EditableProperty;
import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.GenericEditableStyledDocument;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.RichTextChange;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyledDocument;
import org.fxmisc.richtext.model.TextOps;
import org.fxmisc.richtext.model.TwoDimensional;
import org.fxmisc.richtext.model.TwoLevelNavigator;
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
 * <h3>Auto-Scrolling to the Caret</h3>
 *
 * <p>Every time the underlying {@link EditableStyledDocument} changes via user interaction (e.g. typing) through
 * the {@code StyledTextArea}, the area will scroll to insure the caret is kept in view. However, this does not
 * occur if changes are done programmatically. For example, let's say the area is displaying the bottom part
 * of the area's {@link EditableStyledDocument} and some code changes something in the top part of the document
 * that is not currently visible. If there is no call to {@link #requestFollowCaret()} at the end of that code,
 * the area will not auto-scroll to that section of the document. The change will occur, and the user will continue
 * to see the bottom part of the document as before. If such a call is there, then the area will scroll
 * to the top of the document and no longer display the bottom part of it.</p>
 *
 * <p>Additionally, when overriding the default user-interaction behavior, remember to include a call
 * to {@link #requestFollowCaret()}.</p>
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
public class GenericStyledArea<PS, SEG, S> extends Region
        implements
        TextEditingArea<PS, SEG, S>,
        EditActions<PS, SEG, S>,
        ClipboardActions<PS, SEG, S>,
        NavigationActions<PS, SEG, S>,
        StyleActions<PS, S>,
        UndoActions,
        ViewActions,
        MouseBehaviorOverrides,
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
    private final BooleanProperty editable = new EditableProperty<>(this);
    @Override public final boolean isEditable() { return editable.get(); }
    @Override public final void setEditable(boolean value) { editable.set(value); }
    @Override public final BooleanProperty editableProperty() { return editable; }

    // wrapText property
    private final BooleanProperty wrapText = new SimpleBooleanProperty(this, "wrapText");
    @Override public final boolean isWrapText() { return wrapText.get(); }
    @Override public final void setWrapText(boolean value) { wrapText.set(value); }
    @Override public final BooleanProperty wrapTextProperty() { return wrapText; }

    // showCaret property
    private final Var<CaretVisibility> showCaret = Var.newSimpleVar(CaretVisibility.AUTO);
    @Override public final CaretVisibility getShowCaret() { return showCaret.getValue(); }
    @Override public final void setShowCaret(CaretVisibility value) { showCaret.setValue(value); }
    @Override public final Var<CaretVisibility> showCaretProperty() { return showCaret; }

    // undo manager
    private UndoManager undoManager;
    @Override public UndoManager getUndoManager() { return undoManager; }
    @Override public void setUndoManager(UndoManagerFactory undoManagerFactory) {
        undoManager.close();
        undoManager = preserveStyle
                ? createRichUndoManager(undoManagerFactory)
                : createPlainUndoManager(undoManagerFactory);
    }

    private final ObjectProperty<Duration> mouseOverTextDelay = new SimpleObjectProperty<>(null);
    @Override public void setMouseOverTextDelay(Duration delay) { mouseOverTextDelay.set(delay); }
    @Override public Duration getMouseOverTextDelay() { return mouseOverTextDelay.get(); }
    @Override public ObjectProperty<Duration> mouseOverTextDelayProperty() { return mouseOverTextDelay; }

    private final Property<IntConsumer> onSelectionDrop = new SimpleObjectProperty<>(this::moveSelectedText);
    @Override public final void setOnSelectionDrop(IntConsumer consumer) { onSelectionDrop.setValue(consumer); }
    @Override public final IntConsumer getOnSelectionDrop() { return onSelectionDrop.getValue(); }

    private final ObjectProperty<IntFunction<? extends Node>> paragraphGraphicFactory = new SimpleObjectProperty<>(null);
    @Override
    public void setParagraphGraphicFactory(IntFunction<? extends Node> factory) { paragraphGraphicFactory.set(factory); }
    @Override
    public IntFunction<? extends Node> getParagraphGraphicFactory() { return paragraphGraphicFactory.get(); }
    @Override
    public ObjectProperty<IntFunction<? extends Node>> paragraphGraphicFactoryProperty() { return paragraphGraphicFactory; }

    private ObjectProperty<ContextMenu> contextMenu = new SimpleObjectProperty<>(null);
    @Override public final ContextMenu getContextMenu() { return contextMenu.get(); }
    @Override public final void setContextMenu(ContextMenu menu) { contextMenu.setValue(menu); }
    @Override public final ObjectProperty<ContextMenu> contextMenuObjectProperty() { return contextMenu; }
    protected final boolean isContextMenuPresent() { return contextMenu.get() != null; }

    private double contextMenuXOffset = 2;
    @Override public final double getContextMenuXOffset() { return contextMenuXOffset; }
    @Override public final void setContextMenuXOffset(double offset) { contextMenuXOffset = offset; }

    private double contextMenuYOffset = 2;
    @Override public final double getContextMenuYOffset() { return contextMenuYOffset; }
    @Override public final void setContextMenuYOffset(double offset) { contextMenuYOffset = offset; }

    private final BooleanProperty useInitialStyleForInsertion = new SimpleBooleanProperty();
    @Override
    public BooleanProperty useInitialStyleForInsertionProperty() { return useInitialStyleForInsertion; }
    @Override
    public void setUseInitialStyleForInsertion(boolean value) { useInitialStyleForInsertion.set(value); }
    @Override
    public boolean getUseInitialStyleForInsertion() { return useInitialStyleForInsertion.get(); }

    private Optional<Tuple2<Codec<PS>, Codec<SEG>>> styleCodecs = Optional.empty();
    @Override
    public void setStyleCodecs(Codec<PS> paragraphStyleCodec, Codec<SEG> textStyleCodec) {
        styleCodecs = Optional.of(t(paragraphStyleCodec, textStyleCodec));
    }
    @Override
    public Optional<Tuple2<Codec<PS>, Codec<SEG>>> getStyleCodecs() {
        return styleCodecs;
    }

    @Override
    public Var<Double> estimatedScrollXProperty() { return virtualFlow.estimatedScrollXProperty(); }
    @Override
    public double getEstimatedScrollX() { return virtualFlow.estimatedScrollXProperty().getValue(); }
    @Override
    public void setEstimatedScrollX(double value) { virtualFlow.estimatedScrollXProperty().setValue(value); }

    @Override
    public Var<Double> estimatedScrollYProperty() { return virtualFlow.estimatedScrollYProperty(); }
    @Override
    public double getEstimatedScrollY() { return virtualFlow.estimatedScrollYProperty().getValue(); }
    @Override
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
    @Override public final String getText() { return content.getText(); }
    @Override public final ObservableValue<String> textProperty() { return content.textProperty(); }

    // rich text
    @Override public final StyledDocument<PS, SEG, S> getDocument() { return content; }

    // length
    @Override public final int getLength() { return content.getLength(); }
    @Override public final ObservableValue<Integer> lengthProperty() { return content.lengthProperty(); }

    // caret position
    private final Var<Integer> internalCaretPosition = Var.newSimpleVar(0);
    private final SuspendableVal<Integer> caretPosition = internalCaretPosition.suspendable();
    @Override public final int getCaretPosition() { return caretPosition.getValue(); }
    @Override public final ObservableValue<Integer> caretPositionProperty() { return caretPosition; }

    // caret bounds
    private final Val<Optional<Bounds>> caretBounds;
    @Override public final Optional<Bounds> getCaretBounds() { return caretBounds.getValue(); }
    @Override public final ObservableValue<Optional<Bounds>> caretBoundsProperty() { return caretBounds; }

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

    // selection bounds
    private final Val<Optional<Bounds>> selectionBounds;
    @Override public final Optional<Bounds> getSelectionBounds() { return selectionBounds.getValue(); }
    @Override public final ObservableValue<Optional<Bounds>> selectionBoundsProperty() { return selectionBounds; }

    // current paragraph index
    private final SuspendableVal<Integer> currentParagraph;
    @Override public final int getCurrentParagraph() { return currentParagraph.getValue(); }
    @Override public final ObservableValue<Integer> currentParagraphProperty() { return currentParagraph; }

    // caret column
    private final SuspendableVal<Integer> caretColumn;
    @Override public final int getCaretColumn() { return caretColumn.getValue(); }
    @Override public final ObservableValue<Integer> caretColumnProperty() { return caretColumn; }

    // paragraphs
    @Override public LiveList<Paragraph<PS, SEG, S>> getParagraphs() { return content.getParagraphs(); }

    // beingUpdated
    private final SuspendableNo beingUpdated = new SuspendableNo();
    public ObservableBooleanValue beingUpdatedProperty() { return beingUpdated; }
    public boolean isBeingUpdated() { return beingUpdated.get(); }

    // total width estimate
    @Override
    public Val<Double> totalWidthEstimateProperty() { return virtualFlow.totalWidthEstimateProperty(); }
    @Override
    public double getTotalWidthEstimate() { return virtualFlow.totalWidthEstimateProperty().getValue(); }

    // total height estimate
    @Override
    public Val<Double> totalHeightEstimateProperty() { return virtualFlow.totalHeightEstimateProperty(); }
    @Override
    public double getTotalHeightEstimate() { return virtualFlow.totalHeightEstimateProperty().getValue(); }

    /* ********************************************************************** *
     *                                                                        *
     * Event streams                                                          *
     *                                                                        *
     * ********************************************************************** */

    // text changes
    @Override public final EventStream<PlainTextChange> plainTextChanges() { return content.plainChanges(); }

    // rich text changes
    @Override public final EventStream<RichTextChange<PS, SEG, S>> richChanges() { return content.richChanges(); }

    /* ********************************************************************** *
     *                                                                        *
     * Private fields                                                         *
     *                                                                        *
     * ********************************************************************** */

    private Position selectionStart2D;
    private Position selectionEnd2D;

    private Subscription subscriptions = () -> {};

    // Remembers horizontal position when traversing up / down.
    private Optional<ParagraphBox.CaretOffsetX> targetCaretOffset = Optional.empty();

    private final Binding<Boolean> caretVisible;

    private final Val<UnaryOperator<Point2D>> _popupAnchorAdjustment;

    private final VirtualFlow<Paragraph<PS, SEG, S>, Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>>> virtualFlow;

    // used for two-level navigation, where on the higher level are
    // paragraphs and on the lower level are lines within a paragraph
    private final TwoLevelNavigator navigator;

    private boolean followCaretRequested = false;

    private final SuspendableEventStream<?> viewportDirty;

    /* ********************************************************************** *
     *                                                                        *
     * Fields necessary for Cloning                                           *
     *                                                                        *
     * ********************************************************************** */

    private final EditableStyledDocument<PS, SEG, S> content;
    /**
     * The underlying document that can be displayed by multiple {@code StyledTextArea}s.
     */
    public final EditableStyledDocument<PS, SEG, S> getContent() { return content; }

    private final S initialTextStyle;
    @Override public final S getInitialTextStyle() { return initialTextStyle; }

    private final PS initialParagraphStyle;
    @Override public final PS getInitialParagraphStyle() { return initialParagraphStyle; }

    private final BiConsumer<TextFlow, PS> applyParagraphStyle;
    @Override
    public final BiConsumer<TextFlow, PS> getApplyParagraphStyle() { return applyParagraphStyle; }

    // TODO: Currently, only undo/redo respect this flag.
    private final boolean preserveStyle;
    @Override public final boolean isPreserveStyle() { return preserveStyle; }

    /* ********************************************************************** *
     *                                                                        *
     * Miscellaneous                                                          *
     *                                                                        *
     * ********************************************************************** */

    private final TextOps<SEG, S> segmentOps;
    @Override public final SegmentOps<SEG, S> getSegOps() { return segmentOps; }

    /* ********************************************************************** *
     *                                                                        *
     * Constructors                                                           *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Creates a text area with empty text content.
     *
     * @param initialParagraphStyle style to use in places where no other style is
     * specified (yet).
     * @param applyParagraphStyle function that, given a {@link TextFlow} node and
     * a style, applies the style to the paragraph node. This function is
     * used by the default skin to apply style to paragraph nodes.
     * @param initialTextStyle style to use in places where no other style is
     * specified (yet).
     * @param segmentOps The operations which are defined on the text segment objects.
     * @param nodeFactory A function which is used to create the JavaFX scene nodes for a
     *        particular segment.
     */
    public GenericStyledArea(PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
                             S initialTextStyle, TextOps<SEG, S> segmentOps,
                             Function<SEG, Node> nodeFactory) {
        this(initialParagraphStyle, applyParagraphStyle, initialTextStyle, segmentOps, true, nodeFactory);
    }

    public GenericStyledArea(PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
                          S initialTextStyle, TextOps<SEG, S> segmentOps,
                          boolean preserveStyle, Function<SEG, Node> nodeFactory) {
        this(initialParagraphStyle, applyParagraphStyle, initialTextStyle,
                new GenericEditableStyledDocument<>(initialParagraphStyle, initialTextStyle, segmentOps), segmentOps, preserveStyle, nodeFactory);
    }

    /**
     * The same as {@link #GenericStyledArea(Object, BiConsumer, Object, TextOps, Function)} except that
     * this constructor can be used to create another {@code GenericStyledArea} object that
     * shares the same {@link EditableStyledDocument}.
     */
    public GenericStyledArea(
            PS initialParagraphStyle,
            BiConsumer<TextFlow, PS> applyParagraphStyle,
            S initialTextStyle,
            EditableStyledDocument<PS, SEG, S> document,
            TextOps<SEG, S> textOps,
            Function<SEG, Node> nodeFactory) {
        this(initialParagraphStyle, applyParagraphStyle, initialTextStyle, document, textOps, true, nodeFactory);

    }

    public GenericStyledArea(
            PS initialParagraphStyle,
            BiConsumer<TextFlow, PS> applyParagraphStyle,
            S initialTextStyle,
            EditableStyledDocument<PS, SEG, S> document,
            TextOps<SEG, S> textOps,
            boolean preserveStyle,
            Function<SEG, Node> nodeFactory) {
        this.initialTextStyle = initialTextStyle;
        this.initialParagraphStyle = initialParagraphStyle;
        this.preserveStyle = preserveStyle;
        this.content = document;
        this.applyParagraphStyle = applyParagraphStyle;
        this.segmentOps = textOps;

        undoManager = preserveStyle
                ? createRichUndoManager(UndoManagerFactory.unlimitedHistoryFactory())
                : createPlainUndoManager(UndoManagerFactory.unlimitedHistoryFactory());

        Val<Position> caretPosition2D = Val.create(
                () -> content.offsetToPosition(internalCaretPosition.getValue(), Forward),
                internalCaretPosition, getParagraphs());

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

        final Suspendable omniSuspendable = Suspendable.combine(
                beingUpdated, // must be first, to be the last one to release

                caretPosition,
                anchor,
                selection,
                selectedText,
                currentParagraph,
                caretColumn);
        manageSubscription(omniSuspendable.suspendWhen(content.beingUpdatedProperty()));

        // when content is updated by an area, update the caret
        // and selection ranges of all the other
        // clones that also share this document
        subscribeTo(plainTextChanges(), plainTextChange -> {
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

        // allow tab traversal into area
        setFocusTraversable(true);

        this.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        getStyleClass().add("styled-text-area");
        getStylesheets().add(StyledTextArea.class.getResource("styled-text-area.css").toExternalForm());

        // keeps track of currently used non-empty cells
        @SuppressWarnings("unchecked")
        ObservableSet<ParagraphBox<PS, SEG, S>> nonEmptyCells = FXCollections.observableSet();

        // Initialize content
        virtualFlow = VirtualFlow.createVertical(
                getParagraphs(),
                par -> {
                    Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell = createCell(
                            par,
                            applyParagraphStyle,
                            nodeFactory);
                    nonEmptyCells.add(cell.getNode());
                    return cell.beforeReset(() -> nonEmptyCells.remove(cell.getNode()))
                            .afterUpdateItem(p -> nonEmptyCells.add(cell.getNode()));
                });
        getChildren().add(virtualFlow);

        // initialize navigator
        IntSupplier cellCount = () -> getParagraphs().size();
        IntUnaryOperator cellLength = i -> virtualFlow.getCell(i).getNode().getLineCount();
        navigator = new TwoLevelNavigator(cellCount, cellLength);

        // relayout the popup when any of its settings values change (besides the caret being dirty)
        EventStream<?> popupAlignmentDirty = invalidationsOf(popupAlignmentProperty());
        EventStream<?> popupAnchorAdjustmentDirty = invalidationsOf(popupAnchorAdjustmentProperty());
        EventStream<?> popupAnchorOffsetDirty = invalidationsOf(popupAnchorOffsetProperty());
        EventStream<?> popupDirty = merge(popupAlignmentDirty, popupAnchorAdjustmentDirty, popupAnchorOffsetDirty);
        subscribeTo(popupDirty, x -> layoutPopup());

        // follow the caret every time the caret position or paragraphs change
        EventStream<?> caretPosDirty = invalidationsOf(caretPositionProperty());
        EventStream<?> paragraphsDirty = invalidationsOf(getParagraphs());
        EventStream<?> selectionDirty = invalidationsOf(selectionProperty());
        // need to reposition popup even when caret hasn't moved, but selection has changed (been deselected)
        EventStream<?> caretDirty = merge(caretPosDirty, paragraphsDirty, selectionDirty);

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

        viewportDirty = merge(
                // no need to check for width & height invalidations as scroll values update when these do

                // scale
                invalidationsOf(scaleXProperty()),
                invalidationsOf(scaleYProperty()),

                // scroll
                invalidationsOf(estimatedScrollXProperty()),
                invalidationsOf(estimatedScrollYProperty())
        ).suppressible();
        EventStream<?> caretBoundsDirty = merge(viewportDirty, caretDirty)
                .suppressWhen(beingUpdatedProperty());
        EventStream<?> selectionBoundsDirty = merge(viewportDirty, invalidationsOf(selectionProperty()))
                .suppressWhen(beingUpdatedProperty());

        // updates the bounds of the caret/selection
        caretBounds = Val.create(this::getCaretBoundsOnScreen, caretBoundsDirty);
        selectionBounds = Val.create(this::impl_bounds_getSelectionBoundsOnScreen, selectionBoundsDirty);

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

        new StyledTextAreaBehavior(this);
    }


    /* ********************************************************************** *
     *                                                                        *
     * CSS                                                                    *
     *                                                                        *
     * ********************************************************************** */

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<>(Region.getClassCssMetaData());
        styleables.add(highlightFill.getCssMetaData());
        styleables.add(highlightTextFill.getCssMetaData());
        styleables.add(caretBlinkRate.getCssMetaData());
        return styleables;
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
    Optional<Bounds> getCaretBoundsInViewport() {
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
        ParagraphBox<PS, SEG, S> cell = virtualFlow.getCell(parIdx).getNode();
        CharacterHit parHit = cell.hitTextLine(x, targetLine.getMinor());
        return parHit.offset(getParagraphOffset(parIdx));
    }

    CharacterHit hit(ParagraphBox.CaretOffsetX x, double y) {
        VirtualFlowHit<Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>>> hit = virtualFlow.hit(0.0, y);
        if(hit.isBeforeCells()) {
            return CharacterHit.insertionAt(0);
        } else if(hit.isAfterCells()) {
            return CharacterHit.insertionAt(getLength());
        } else {
            int parIdx = hit.getCellIndex();
            int parOffset = getParagraphOffset(parIdx);
            ParagraphBox<PS, SEG, S> cell = hit.getCell().getNode();
            Point2D cellOffset = hit.getCellOffset();
            CharacterHit parHit = cell.hitText(x, cellOffset.getY());
            return parHit.offset(parOffset);
        }
    }

    @Override
    public CharacterHit hit(double x, double y) {
        VirtualFlowHit<Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>>> hit = virtualFlow.hit(x, y);
        if(hit.isBeforeCells()) {
            return CharacterHit.insertionAt(0);
        } else if(hit.isAfterCells()) {
            return CharacterHit.insertionAt(getLength());
        } else {
            int parIdx = hit.getCellIndex();
            int parOffset = getParagraphOffset(parIdx);
            ParagraphBox<PS, SEG, S> cell = hit.getCell().getNode();
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
        Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell = virtualFlow.getCell(parIdx);
        int lineIdx = cell.getNode().getCurrentLineIndex();
        return _position(parIdx, lineIdx);
    }

    TwoDimensional.Position _position(int par, int line) {
        return navigator.position(par, line);
    }

    @Override
    public int getParagraphLinesCount(int paragraphIndex) {
        return virtualFlow.getCell(paragraphIndex).getNode().getLineCount();
    }

    @Override
    public Optional<Bounds> getCharacterBoundsOnScreen(int from, int to) {
        if (from < 0) {
            throw new IllegalArgumentException("From is negative: " + from);
        }
        if (from > to) {
            throw new IllegalArgumentException(String.format("From is greater than to. from=%s to=%s", from, to));
        }
        if (to > getLength()) {
            throw new IllegalArgumentException(String.format("To is greater than area's length. length=%s, to=%s", getLength(), to));
        }

        // no bounds exist if range is just a newline character
        if (getText(from, to).equals("\n")) {
            return Optional.empty();
        }

        // if 'from' is the newline character at the end of a multi-line paragraph, it returns a Bounds that whose
        //  minX & minY are the minX and minY of the paragraph itself, not the newline character. So, ignore it.
        int realFrom = getText(from, from + 1).equals("\n") ? from + 1 : from;

        Position startPosition = offsetToPosition(realFrom, Bias.Forward);
        int startRow = startPosition.getMajor();
        Position endPosition = startPosition.offsetBy(to - realFrom, Bias.Forward);
        int endRow = endPosition.getMajor();
        if (startRow == endRow) {
            return getRangeBoundsOnScreen(startRow, startPosition.getMinor(), endPosition.getMinor());
        } else {
            Optional<Bounds> rangeBounds = getRangeBoundsOnScreen(startRow, startPosition.getMinor(),
                    getParagraph(startRow).length());
            for (int i = startRow + 1; i <= endRow; i++) {
                Optional<Bounds> nextLineBounds = getRangeBoundsOnScreen(i, 0,
                        i == endRow
                                ? endPosition.getMinor()
                                : getParagraph(i).length()
                );
                if (nextLineBounds.isPresent()) {
                    if (rangeBounds.isPresent()) {
                        Bounds lineBounds = nextLineBounds.get();
                        rangeBounds = rangeBounds.map(b -> {
                            double minX = Math.min(b.getMinX(),   lineBounds.getMinX());
                            double minY = Math.min(b.getMinY(),   lineBounds.getMinY());
                            double maxX = Math.max(b.getMaxX(),   lineBounds.getMaxX());
                            double maxY = Math.max(b.getMaxY(),   lineBounds.getMaxY());
                            return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
                        });
                    } else {
                        rangeBounds = nextLineBounds;
                    }
                }
            }
            return rangeBounds;
        }
    }

    @Override
    public final String getText(int start, int end) {
        return content.getText(start, end);
    }

    @Override
    public String getText(int paragraph) {
        return content.getText(paragraph);
    }

    public Paragraph<PS, SEG, S> getParagraph(int index) {
        return content.getParagraph(index);
    }

    public int getParagraphLenth(int index) {
        return content.getParagraphLength(index);
    }

    @Override
    public StyledDocument<PS, SEG, S> subDocument(int start, int end) {
        return content.subSequence(start, end);
    }

    @Override
    public StyledDocument<PS, SEG, S> subDocument(int paragraphIndex) {
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
        int end = paragraph == endPar ? selectionEnd2D.getMinor() : getParagraphLenth(paragraph);

        // force selectionProperty() to be valid
        getSelection();

        return new IndexRange(start, end);
    }

    @Override
    public S getStyleOfChar(int index) {
        return content.getStyleOfChar(index);
    }

    @Override
    public S getStyleAtPosition(int position) {
        return content.getStyleAtPosition(position);
    }

    @Override
    public IndexRange getStyleRangeAtPosition(int position) {
        return content.getStyleRangeAtPosition(position);
    }

    @Override
    public StyleSpans<S> getStyleSpans(int from, int to) {
        return content.getStyleSpans(from, to);
    }

    @Override
    public S getStyleOfChar(int paragraph, int index) {
        return content.getStyleOfChar(paragraph, index);
    }

    @Override
    public S getStyleAtPosition(int paragraph, int position) {
        return content.getStyleAtPosition(paragraph, position);
    }

    @Override
    public IndexRange getStyleRangeAtPosition(int paragraph, int position) {
        return content.getStyleRangeAtPosition(paragraph, position);
    }

    @Override
    public StyleSpans<S> getStyleSpans(int paragraph) {
        return content.getStyleSpans(paragraph);
    }

    @Override
    public StyleSpans<S> getStyleSpans(int paragraph, int from, int to) {
        return content.getStyleSpans(paragraph, from, to);
    }

    @Override
    public int getAbsolutePosition(int paragraphIndex, int columnIndex) {
        return content.getAbsolutePosition(paragraphIndex, columnIndex);
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

    @Override
    public void scrollBy(Point2D deltas) {
        virtualFlow.scrollXBy(deltas.getX());
        virtualFlow.scrollYBy(deltas.getY());
    }

    void show(double y) {
        virtualFlow.show(y);
    }

    @Override
    public void showParagraphInViewport(int paragraphIndex) {
        virtualFlow.show(paragraphIndex);
    }

    @Override
    public void showParagraphAtTop(int paragraphIndex) {
        virtualFlow.showAsFirst(paragraphIndex);
    }

    @Override
    public void showParagraphAtBottom(int paragraphIndex) {
        virtualFlow.showAsLast(paragraphIndex);
    }

    void showCaretAtBottom() {
        int parIdx = getCurrentParagraph();
        Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell = virtualFlow.getCell(parIdx);
        Bounds caretBounds = cell.getNode().getCaretBounds();
        double y = caretBounds.getMaxY();
        virtualFlow.showAtOffset(parIdx, getViewportHeight() - y);
    }

    void showCaretAtTop() {
        int parIdx = getCurrentParagraph();
        Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell = virtualFlow.getCell(parIdx);
        Bounds caretBounds = cell.getNode().getCaretBounds();
        double y = caretBounds.getMinY();
        virtualFlow.showAtOffset(parIdx, -y);
    }

    @Override
    public void requestFollowCaret() {
        followCaretRequested = true;
        requestLayout();
    }

    private void followCaret() {
        int parIdx = getCurrentParagraph();
        Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell = virtualFlow.getCell(parIdx);
        Bounds caretBounds = cell.getNode().getCaretBounds();
        double graphicWidth = cell.getNode().getGraphicPrefWidth();
        Bounds region = extendLeft(caretBounds, graphicWidth);
        virtualFlow.show(parIdx, region);
    }

    @Override
    public void lineStart(SelectionPolicy policy) {
        int columnPos = virtualFlow.getCell(getCurrentParagraph()).getNode().getCurrentLineStartPosition();
        moveTo(getCurrentParagraph(), columnPos, policy);
    }

    @Override
    public void lineEnd(SelectionPolicy policy) {
        int columnPos = virtualFlow.getCell(getCurrentParagraph()).getNode().getCurrentLineEndPosition();
        moveTo(getCurrentParagraph(), columnPos, policy);
    }

    @Override
    public void selectLine() {
        lineStart(SelectionPolicy.CLEAR);
        lineEnd(SelectionPolicy.ADJUST);
    }

    @Override
    public void prevPage(SelectionPolicy selectionPolicy) {
        showCaretAtBottom();
        CharacterHit hit = hit(getTargetCaretOffset(), 1.0);
        moveTo(hit.getInsertionIndex(), selectionPolicy);
    }

    @Override
    public void nextPage(SelectionPolicy selectionPolicy) {
        showCaretAtTop();
        CharacterHit hit = hit(getTargetCaretOffset(), getViewportHeight() - 1.0);
        moveTo(hit.getInsertionIndex(), selectionPolicy);
    }

    @Override
    public void setStyle(int from, int to, S style) {
        content.setStyle(from, to, style);
    }

    @Override
    public void setStyle(int paragraph, S style) {
        content.setStyle(paragraph, style);
    }

    @Override
    public void setStyle(int paragraph, int from, int to, S style) {
        content.setStyle(paragraph, from, to, style);
    }

    @Override
    public void setStyleSpans(int from, StyleSpans<? extends S> styleSpans) {
        content.setStyleSpans(from, styleSpans);
    }

    @Override
    public void setStyleSpans(int paragraph, int from, StyleSpans<? extends S> styleSpans) {
        content.setStyleSpans(paragraph, from, styleSpans);
    }

    @Override
    public void setParagraphStyle(int paragraph, PS paragraphStyle) {
        content.setParagraphStyle(paragraph, paragraphStyle);
    }
    @Override
    public void replaceText(int start, int end, String text) {
        StyledDocument<PS, SEG, S> doc = ReadOnlyStyledDocument.fromString(
                text, getParagraphStyleForInsertionAt(start), getStyleForInsertionAt(start), segmentOps);
        replace(start, end, doc);
    }

    @Override
    public void replace(int start, int end, StyledDocument<PS, SEG, S> replacement) {
        content.replace(start, end, replacement);
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
            try (Guard g = viewportDirty.suspend()) {
                followCaret();
            }
        }

        // position popup
        layoutPopup();
    }

    /* ********************************************************************** *
     *                                                                        *
     * Private methods                                                        *
     *                                                                        *
     * ********************************************************************** */

    private Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> createCell(
            Paragraph<PS, SEG, S> paragraph,
            BiConsumer<TextFlow, PS> applyParagraphStyle,
            Function<SEG, Node> nodeFactory) {

        ParagraphBox<PS, SEG, S> box = new ParagraphBox<>(paragraph, applyParagraphStyle, nodeFactory);

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

        return new Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>>() {
            @Override
            public ParagraphBox<PS, SEG, S> getNode() {
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

    private ParagraphBox<PS, SEG, S> getCell(int index) {
        return virtualFlow.getCell(index).getNode();
    }

    private EventStream<MouseOverTextEvent> mouseOverTextEvents(ObservableSet<ParagraphBox<PS, SEG, S>> cells, Duration delay) {
        return merge(cells, c -> c.stationaryIndices(delay).map(e -> e.unify(
                l -> l.map((pos, charIdx) -> MouseOverTextEvent.beginAt(c.localToScreen(pos), getParagraphOffset(c.getIndex()) + charIdx)),
                r -> MouseOverTextEvent.end())));
    }

    private int getParagraphOffset(int parIdx) {
        return position(parIdx, 0).toOffset();
    }

    private void layoutPopup() {
        PopupWindow popup = getPopupWindow();
        PopupAlignment alignment = getPopupAlignment();
        UnaryOperator<Point2D> adjustment = _popupAnchorAdjustment.getValue();
        if(popup != null) {
            positionPopup(popup, alignment, adjustment);
        }
    }

    private void positionPopup(
            PopupWindow popup,
            PopupAlignment alignment,
            UnaryOperator<Point2D> adjustment) {
        Optional<Bounds> bounds = null;
        switch(alignment.getAnchorObject()) {
            case CARET: bounds = getCaretBoundsOnScreen(); break;
            case SELECTION: bounds = impl_popup_getSelectionBoundsOnScreen(); break;
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

    private Optional<Bounds> getRangeBoundsOnScreen(int paragraphIndex, int from, int to) {
        return virtualFlow.getCellIfVisible(paragraphIndex)
                .map(c -> c.getNode().getRangeBoundsOnScreen(from, to));
    }

    private Optional<Bounds> getCaretBoundsOnScreen() {
        return virtualFlow.getCellIfVisible(getCurrentParagraph())
                .map(c -> c.getNode().getCaretBoundsOnScreen());
    }

    private Optional<Bounds> impl_popup_getSelectionBoundsOnScreen() {
        IndexRange selection = getSelection();
        if(selection.getLength() == 0) {
            return getCaretBoundsOnScreen();
        }

        return impl_getSelectionBoundsOnScreen();
    }

    private Optional<Bounds> impl_bounds_getSelectionBoundsOnScreen() {
        IndexRange selection = getSelection();
        if (selection.getLength() == 0) {
            return Optional.empty();
        }
        return impl_getSelectionBoundsOnScreen();
    }

    private Optional<Bounds> impl_getSelectionBoundsOnScreen() {
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

    private S getStyleForInsertionAt(int pos) {
        if(useInitialStyleForInsertion.get()) {
            return initialTextStyle;
        } else {
            return content.getStyleAtPosition(pos);
        }
    }

    private PS getParagraphStyleForInsertionAt(int pos) {
        if(useInitialStyleForInsertion.get()) {
            return initialParagraphStyle;
        } else {
            return content.getParagraphStyleAtPosition(pos);
        }
    }

    private UndoManager createPlainUndoManager(UndoManagerFactory factory) {
        Consumer<PlainTextChange> apply = change -> replaceText(change.getPosition(), change.getPosition() + change.getRemoved().length(), change.getInserted());
        BiFunction<PlainTextChange, PlainTextChange, Optional<PlainTextChange>> merge = PlainTextChange::mergeWith;
        return factory.create(plainTextChanges(), PlainTextChange::invert, apply, merge);
    }

    private UndoManager createRichUndoManager(UndoManagerFactory factory) {
        Consumer<RichTextChange<PS, SEG, S>> apply = change -> replace(change.getPosition(), change.getPosition() + change.getRemoved().length(), change.getInserted());
        BiFunction<RichTextChange<PS, SEG, S>, RichTextChange<PS, SEG, S>, Optional<RichTextChange<PS, SEG, S>>> merge = RichTextChange<PS, SEG, S>::mergeWith;
        return factory.create(richChanges(), RichTextChange::invert, apply, merge);
    }

    private Guard suspend(Suspendable... suspendables) {
        return Suspendable.combine(beingUpdated, Suspendable.combine(suspendables)).suspend();
    }

    /**
     * Positions only the caret. Doesn't move the anchor and doesn't change
     * the selection. Can be used to achieve the special case of positioning
     * the caret outside or inside the selection, as opposed to always being
     * at the boundary. Use with care.
     */
    void positionCaret(int pos) {
        try(Guard g = suspend(caretPosition, currentParagraph, caretColumn)) {
            internalCaretPosition.setValue(pos);
        }
    }

    void clearTargetCaretOffset() {
        targetCaretOffset = Optional.empty();
    }

    ParagraphBox.CaretOffsetX getTargetCaretOffset() {
        if(!targetCaretOffset.isPresent())
            targetCaretOffset = Optional.of(getCaretOffsetX());
        return targetCaretOffset.get();
    }

    private static EventStream<Boolean> booleanPulse(javafx.util.Duration javafxDuration, EventStream<?> restartImpulse) {
        Duration duration = Duration.ofMillis(Math.round(javafxDuration.toMillis()));
        EventStream<?> ticks = EventStreams.restartableTicks(duration, restartImpulse);
        return StateMachine.init(false)
                .on(restartImpulse.withDefaultEvent(null)).transition((state, impulse) -> true)
                .on(ticks).transition((state, tick) -> !state)
                .toStateStream();
    }

    /* ********************************************************************** *
     *                                                                        *
     * Deprecated Popup API  (Originally a part of "Properties" section       *
     *                                                                        *
     * Code was moved to bottom of this file to make it easier to stay        *
     * focused on code still in use. This whole section should be deleted     *
     * at a later time.                                                       *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Popup window that will be positioned by this text area relative to the
     * caret or selection. Use {@link #popupAlignmentProperty()} to specify
     * how the popup should be positioned relative to the caret or selection.
     * Use {@link #popupAnchorOffsetProperty()} or
     * {@link #popupAnchorAdjustmentProperty()} to further adjust the position.
     *
     * @deprecated Use {@link #getCaretBounds()}/{@link #caretBoundsProperty()} or {@link #getSelectionBounds()}/
     * {@link #selectionBoundsProperty()} instead.
     */
    @Deprecated
    private final ObjectProperty<PopupWindow> popupWindow = new SimpleObjectProperty<>();
    /**
     * @deprecated Use {@link #getCaretBounds()}/{@link #caretBoundsProperty()} or {@link #getSelectionBounds()}/
     * {@link #selectionBoundsProperty()} instead.
     */
    @Deprecated
    public void setPopupWindow(PopupWindow popup) { popupWindow.set(popup); }
    /**
     * @deprecated Use {@link #getCaretBounds()}/{@link #caretBoundsProperty()} or {@link #getSelectionBounds()}/
     * {@link #selectionBoundsProperty()} instead.
     */
    @Deprecated
    public PopupWindow getPopupWindow() { return popupWindow.get(); }
    /**
     * @deprecated Use {@link #getCaretBounds()}/{@link #caretBoundsProperty()} or {@link #getSelectionBounds()}/
     * {@link #selectionBoundsProperty()} instead.
     */
    @Deprecated
    public ObjectProperty<PopupWindow> popupWindowProperty() { return popupWindow; }

    /**
     * @deprecated Use {@link #getCaretBounds()}/{@link #caretBoundsProperty()} or {@link #getSelectionBounds()}/
     * {@link #selectionBoundsProperty()} instead.
     */
    @Deprecated
    public void setPopupAtCaret(PopupWindow popup) { popupWindow.set(popup); }
    /**
     * @deprecated Use {@link #getCaretBounds()}/{@link #caretBoundsProperty()} or {@link #getSelectionBounds()}/
     * {@link #selectionBoundsProperty()} instead.
     */
    @Deprecated
    public PopupWindow getPopupAtCaret() { return popupWindow.get(); }
    /**
     * @deprecated Use {@link #getCaretBounds()}/{@link #caretBoundsProperty()} or {@link #getSelectionBounds()}/
     * {@link #selectionBoundsProperty()} instead.
     */
    @Deprecated
    public ObjectProperty<PopupWindow> popupAtCaretProperty() { return popupWindow; }

    /**
     * Specifies further offset (in pixels) of the popup window from the
     * position specified by {@link #popupAlignmentProperty()}.
     *
     * <p>If {@link #popupAnchorAdjustmentProperty()} is also specified, then
     * it overrides the offset set by this property.
     *
     * @deprecated Use {@link #getCaretBounds()}/{@link #caretBoundsProperty()} or {@link #getSelectionBounds()}/
     * {@link #selectionBoundsProperty()} instead.
     */
    @Deprecated
    private final ObjectProperty<Point2D> popupAnchorOffset = new SimpleObjectProperty<>();
    /**
     * @deprecated Use {@link #getCaretBounds()}/{@link #caretBoundsProperty()} or {@link #getSelectionBounds()}/
     * {@link #selectionBoundsProperty()} instead.
     */
    @Deprecated
    public void setPopupAnchorOffset(Point2D offset) { popupAnchorOffset.set(offset); }
    /**
     * @deprecated Use {@link #getCaretBounds()}/{@link #caretBoundsProperty()} or {@link #getSelectionBounds()}/
     * {@link #selectionBoundsProperty()} instead.
     */
    @Deprecated
    public Point2D getPopupAnchorOffset() { return popupAnchorOffset.get(); }
    /**
     * @deprecated Use {@link #getCaretBounds()}/{@link #caretBoundsProperty()} or {@link #getSelectionBounds()}/
     * {@link #selectionBoundsProperty()} instead.
     */
    @Deprecated
    public ObjectProperty<Point2D> popupAnchorOffsetProperty() { return popupAnchorOffset; }

    /**
     * Specifies how to adjust the popup window's anchor point. The given
     * operator is invoked with the screen position calculated according to
     * {@link #popupAlignmentProperty()} and should return a new screen
     * position. This position will be used as the popup window's anchor point.
     *
     * <p>Setting this property overrides {@link #popupAnchorOffsetProperty()}.
     */
    @Deprecated
    private final ObjectProperty<UnaryOperator<Point2D>> popupAnchorAdjustment = new SimpleObjectProperty<>();
    /**
     * @deprecated Use {@link #getCaretBounds()}/{@link #caretBoundsProperty()} or {@link #getSelectionBounds()}/
     * {@link #selectionBoundsProperty()} instead.
     */
    @Deprecated
    public void setPopupAnchorAdjustment(UnaryOperator<Point2D> f) { popupAnchorAdjustment.set(f); }
    /**
     * @deprecated Use {@link #getCaretBounds()}/{@link #caretBoundsProperty()} or {@link #getSelectionBounds()}/
     * {@link #selectionBoundsProperty()} instead.
     */
    @Deprecated
    public UnaryOperator<Point2D> getPopupAnchorAdjustment() { return popupAnchorAdjustment.get(); }
    /**
     * @deprecated Use {@link #getCaretBounds()}/{@link #caretBoundsProperty()} or {@link #getSelectionBounds()}/
     * {@link #selectionBoundsProperty()} instead.
     */
    @Deprecated
    public ObjectProperty<UnaryOperator<Point2D>> popupAnchorAdjustmentProperty() { return popupAnchorAdjustment; }

    /**
     * Defines where the popup window given in {@link #popupWindowProperty()}
     * is anchored, i.e. where its anchor point is positioned. This position
     * can further be adjusted by {@link #popupAnchorOffsetProperty()} or
     * {@link #popupAnchorAdjustmentProperty()}.
     *
     * @deprecated Use {@link #getCaretBounds()}/{@link #caretBoundsProperty()} or {@link #getSelectionBounds()}/
     * {@link #selectionBoundsProperty()} instead.
     */
    @Deprecated
    private final ObjectProperty<PopupAlignment> popupAlignment = new SimpleObjectProperty<>(CARET_TOP);
    /**
     * @deprecated Use {@link #getCaretBounds()}/{@link #caretBoundsProperty()} or {@link #getSelectionBounds()}/
     * {@link #selectionBoundsProperty()} instead.
     */
    @Deprecated
    public void setPopupAlignment(PopupAlignment pos) { popupAlignment.set(pos); }
    /**
     * @deprecated Use {@link #getCaretBounds()}/{@link #caretBoundsProperty()} or {@link #getSelectionBounds()}/
     * {@link #selectionBoundsProperty()} instead.
     */
    @Deprecated
    public PopupAlignment getPopupAlignment() { return popupAlignment.get(); }
    /**
     * @deprecated Use {@link #getCaretBounds()}/{@link #caretBoundsProperty()} or {@link #getSelectionBounds()}/
     * {@link #selectionBoundsProperty()} instead.
     */
    @Deprecated
    public ObjectProperty<PopupAlignment> popupAlignmentProperty() { return popupAlignment; }
}
