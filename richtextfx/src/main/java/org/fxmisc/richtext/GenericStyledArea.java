package org.fxmisc.richtext;

import static org.fxmisc.richtext.PopupAlignment.*;
import static org.reactfx.EventStreams.*;
import static org.reactfx.util.Tuples.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import javafx.beans.NamedArg;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.event.Event;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.input.MouseEvent;
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
import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.EditActions;
import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.GenericEditableStyledDocument;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.StyleActions;
import org.fxmisc.richtext.model.NavigationActions;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.RichTextChange;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyledDocument;
import org.fxmisc.richtext.model.TextEditingArea;
import org.fxmisc.richtext.model.TextOps;
import org.fxmisc.richtext.model.TwoDimensional;
import org.fxmisc.richtext.model.TwoLevelNavigator;
import org.fxmisc.richtext.model.UndoActions;
import org.fxmisc.richtext.util.UndoUtils;
import org.fxmisc.undo.UndoManager;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.Guard;
import org.reactfx.Subscription;
import org.reactfx.Suspendable;
import org.reactfx.SuspendableEventStream;
import org.reactfx.SuspendableNo;
import org.reactfx.collection.LiveList;
import org.reactfx.collection.SuspendableList;
import org.reactfx.util.Tuple2;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

/**
 * Text editing control. Accepts user input (keyboard, mouse) and
 * provides API to assign style to text ranges. It is suitable for
 * syntax highlighting and rich-text editors.
 *
 * <h3>Adding Scrollbars to the Area</h3>
 *
 * <p>The scroll bars no longer appear when the content spans outside
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
 * <h3>Setting the area's {@link UndoManager}</h3>
 *
 * <p>
 *     The default UndoManager can undo/redo either {@link PlainTextChange}s or {@link RichTextChange}s. To create
 *     your own specialized version that may use changes different than these (or a combination of these changes
 *     with others), create them using the convenient factory methods in {@link UndoUtils}.
 * </p>
 *
 * <h3>Overriding default keyboard behavior</h3>
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
 * <h3>Overriding default mouse behavior</h3>
 *
 * The area's default mouse behavior properly handles auto-scrolling and dragging the selected text to a new location.
 * As such, some parts cannot be partially overridden without it affecting other behavior.
 *
 * <p>The following lists either {@link org.fxmisc.wellbehaved.event.EventPattern}s that cannot be overridden without
 * negatively affecting the default mouse behavior or describe how to safely override things in a special way without
 * disrupting the auto scroll behavior.</p>
 * <ul>
 *     <li>
 *         <em>First (1 click count) Primary Button Mouse Pressed Events:</em>
 *         (<code>EventPattern.mousePressed(MouseButton.PRIMARY).onlyIf(e -&gt; e.getClickCount() == 1)</code>).
 *         Do not override. Instead, use {@link #onOutsideSelectionMousePress},
 *         {@link #onInsideSelectionMousePressRelease}, or see next item.
 *     </li>
 *     <li>(
 *         <em>All Other Mouse Pressed Events (e.g., Primary with 2+ click count):</em>
 *         Aside from hiding the context menu if it is showing (use {@link #hideContextMenu()} some((where in your
 *         overriding InputMap to maintain this behavior), these can be safely overridden via any of the
 *         {@link org.fxmisc.wellbehaved.event.template.InputMapTemplate InputMapTemplate's factory methods} or
 *         {@link org.fxmisc.wellbehaved.event.InputMap InputMap's factory methods}.
 *     </li>
 *     <li>
 *         <em>Primary-Button-only Mouse Drag Detection Events:</em>
 *         (<code>EventPattern.eventType(MouseEvent.DRAG_DETECTED).onlyIf(e -&gt; e.getButton() == MouseButton.PRIMARY &amp;&amp; !e.isMiddleButtonDown() &amp;&amp; !e.isSecondaryButtonDown())</code>).
 *         Do not override. Instead, use {@link #onNewSelectionDrag} or {@link #onSelectionDrag}.
 *     </li>
 *     <li>
 *         <em>Primary-Button-only Mouse Drag Events:</em>
 *         (<code>EventPattern.mouseDragged().onlyIf(e -&gt; e.getButton() == MouseButton.PRIMARY &amp;&amp; !e.isMiddleButtonDown() &amp;&amp; !e.isSecondaryButtonDown())</code>)
 *         Do not override, but see next item.
 *     </li>
 *     <li>
 *         <em>All Other Mouse Drag Events:</em>
 *         You may safely override other Mouse Drag Events using different
 *         {@link org.fxmisc.wellbehaved.event.EventPattern}s without affecting default behavior only if
 *         process InputMaps (
 *         {@link org.fxmisc.wellbehaved.event.template.InputMapTemplate#process(javafx.event.EventType, BiFunction)},
 *         {@link org.fxmisc.wellbehaved.event.template.InputMapTemplate#process(org.fxmisc.wellbehaved.event.EventPattern, BiFunction)},
 *         {@link org.fxmisc.wellbehaved.event.InputMap#process(javafx.event.EventType, Function)}, or
 *         {@link org.fxmisc.wellbehaved.event.InputMap#process(org.fxmisc.wellbehaved.event.EventPattern, Function)}
 *         ) are used and {@link org.fxmisc.wellbehaved.event.InputHandler.Result#PROCEED} is returned.
 *         The area has a "catch all" Mouse Drag InputMap that will auto scroll towards the mouse drag event when it
 *         occurs outside the bounds of the area and will stop auto scrolling when the mouse event occurs within the
 *         area. However, this only works if the event is not consumed before the event reaches that InputMap.
 *         To insure the auto scroll feature is enabled, set {@link #isAutoScrollOnDragDesired()} to true in your
 *         process InputMap. If the feature is not desired for that specific drag event, set it to false in the
 *         process InputMap.
 *         <em>Note: Due to this "catch-all" nature, all Mouse Drag Events are consumed.</em>
 *     </li>
 *     <li>
 *         <em>Primary-Button-only Mouse Released Events:</em>
 *         (<code>EventPattern.mouseReleased().onlyIf(e -&gt; e.getButton() == MouseButton.PRIMARY &amp;&amp; !e.isMiddleButtonDown() &amp;&amp; !e.isSecondaryButtonDown())</code>).
 *         Do not override. Instead, use {@link #onNewSelectionDragEnd}, {@link #onSelectionDrop}, or see next item.
 *     </li>
 *     <li>
 *         <em>All other Mouse Released Events:</em>
 *         You may override other Mouse Released Events using different
 *         {@link org.fxmisc.wellbehaved.event.EventPattern}s without affecting default behavior only if
 *         process InputMaps (
 *         {@link org.fxmisc.wellbehaved.event.template.InputMapTemplate#process(javafx.event.EventType, BiFunction)},
 *         {@link org.fxmisc.wellbehaved.event.template.InputMapTemplate#process(org.fxmisc.wellbehaved.event.EventPattern, BiFunction)},
 *         {@link org.fxmisc.wellbehaved.event.InputMap#process(javafx.event.EventType, Function)}, or
 *         {@link org.fxmisc.wellbehaved.event.InputMap#process(org.fxmisc.wellbehaved.event.EventPattern, Function)}
 *         ) are used and {@link org.fxmisc.wellbehaved.event.InputHandler.Result#PROCEED} is returned.
 *         The area has a "catch-all" InputMap that will consume all mouse released events and stop auto scroll if it
 *         was scrolling. However, this only works if the event is not consumed before the event reaches that InputMap.
 *         <em>Note: Due to this "catch-all" nature, all Mouse Released Events are consumed.</em>
 *     </li>
 * </ul>
 *
 *
 * @param <PS> type of style that can be applied to paragraphs (e.g. {@link TextFlow}.
 * @param <SEG> type of segment used in {@link Paragraph}. Can be only text (plain or styled) or
 *             a type that combines text and other {@link Node}s.
 * @param <S> type of style that can be applied to a segment.
 */
public class GenericStyledArea<PS, SEG, S> extends Region
        implements
        TextEditingArea<PS, SEG, S>,
        EditActions<PS, SEG, S>,
        ClipboardActions<PS, SEG, S>,
        NavigationActions<PS, SEG, S>,
        StyleActions<PS, S>,
        UndoActions,
        ViewActions<PS, SEG, S>,
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
            = new CssProperties.HighlightFillProperty(this, HIGHLIGHT_FILL);

    /**
     * Text color for highlighted text.
     */
    private final StyleableObjectProperty<Paint> highlightTextFill
            = new CssProperties.HighlightTextFillProperty(this, HIGHLIGHT_TEXT_FILL);

    /**
     * Controls the blink rate of the caret, when one is displayed. Setting
     * the duration to zero disables blinking.
     */
    private final StyleableObjectProperty<javafx.util.Duration> caretBlinkRate
            = new CssProperties.CaretBlinkRateProperty(this, CARET_BLINK_RATE);

    // editable property
    private final BooleanProperty editable = new CssProperties.EditableProperty<>(this);
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
    @Override public UndoManager getUndoManager() { return undoManager; }
    @Override public void setUndoManager(UndoManager undoManager) {
        this.undoManager.close();
        this.undoManager = undoManager;
    }

    private final ObjectProperty<Duration> mouseOverTextDelay = new SimpleObjectProperty<>(null);
    @Override public void setMouseOverTextDelay(Duration delay) { mouseOverTextDelay.set(delay); }
    @Override public Duration getMouseOverTextDelay() { return mouseOverTextDelay.get(); }
    @Override public ObjectProperty<Duration> mouseOverTextDelayProperty() { return mouseOverTextDelay; }

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
     * Mouse Behavior Hooks                                                   *
     *                                                                        *
     * Hooks for overriding some of the default mouse behavior                *
     *                                                                        *
     * ********************************************************************** */

    private final Property<Consumer<MouseEvent>> onOutsideSelectionMousePress = new SimpleObjectProperty<>(e -> {
        CharacterHit hit = hit(e.getX(), e.getY());
        moveTo(hit.getInsertionIndex(), SelectionPolicy.CLEAR);
    });
    public final void setOnOutsideSelectionMousePress(Consumer<MouseEvent> consumer) { onOutsideSelectionMousePress.setValue(consumer); }
    public final Consumer<MouseEvent> getOnOutsideSelectionMousePress() { return onOutsideSelectionMousePress.getValue(); }

    private final Property<Consumer<MouseEvent>> onInsideSelectionMousePressRelease = new SimpleObjectProperty<>(e -> {
        CharacterHit hit = hit(e.getX(), e.getY());
        moveTo(hit.getInsertionIndex(), SelectionPolicy.CLEAR);
    });
    public final void setOnInsideSelectionMousePressRelease(Consumer<MouseEvent> consumer) { onInsideSelectionMousePressRelease.setValue(consumer); }
    public final Consumer<MouseEvent> getOnInsideSelectionMousePressRelease() { return onInsideSelectionMousePressRelease.getValue(); }

    private final Property<Consumer<Point2D>> onNewSelectionDrag = new SimpleObjectProperty<>(p -> {
        CharacterHit hit = hit(p.getX(), p.getY());
        moveTo(hit.getInsertionIndex(), SelectionPolicy.ADJUST);
    });
    public final void setOnNewSelectionDrag(Consumer<Point2D> consumer) { onNewSelectionDrag.setValue(consumer); }
    public final Consumer<Point2D> getOnNewSelectionDrag() { return onNewSelectionDrag.getValue(); }

    private final Property<Consumer<MouseEvent>> onNewSelectionDragEnd = new SimpleObjectProperty<>(e -> {
        CharacterHit hit = hit(e.getX(), e.getY());
        moveTo(hit.getInsertionIndex(), SelectionPolicy.ADJUST);
    });
    public final void setOnNewSelectionDragEnd(Consumer<MouseEvent> consumer) { onNewSelectionDragEnd.setValue(consumer); }
    public final Consumer<MouseEvent> getOnNewSelectionDragEnd() { return onNewSelectionDragEnd.getValue(); }

    private final Property<Consumer<Point2D>> onSelectionDrag = new SimpleObjectProperty<>(p -> {
        CharacterHit hit = hit(p.getX(), p.getY());
        displaceCaret(hit.getInsertionIndex());
    });
    public final void setOnSelectionDrag(Consumer<Point2D> consumer) { onSelectionDrag.setValue(consumer); }
    public final Consumer<Point2D> getOnSelectionDrag() { return onSelectionDrag.getValue(); }

    private final Property<Consumer<MouseEvent>> onSelectionDrop = new SimpleObjectProperty<>(e -> {
        CharacterHit hit = hit(e.getX(), e.getY());
        moveSelectedText(hit.getInsertionIndex());
    });
    @Override public final void setOnSelectionDrop(Consumer<MouseEvent> consumer) { onSelectionDrop.setValue(consumer); }
    @Override public final Consumer<MouseEvent> getOnSelectionDrop() { return onSelectionDrop.getValue(); }

    // not a hook, but still plays a part in the default mouse behavior
    private final BooleanProperty autoScrollOnDragDesired = new SimpleBooleanProperty(true);
    public final void setAutoScrollOnDragDesired(boolean val) { autoScrollOnDragDesired.set(val); }
    public final boolean isAutoScrollOnDragDesired() { return autoScrollOnDragDesired.get(); }

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

    private final Caret mainCaret;
    @Override public final Caret getMainCaret() { return mainCaret; }

    private final BoundedSelection<PS, SEG, S> mainSelection;
    @Override public final BoundedSelection<PS, SEG, S> getMainSelection() { return mainSelection; }

    // length
    @Override public final int getLength() { return content.getLength(); }
    @Override public final ObservableValue<Integer> lengthProperty() { return content.lengthProperty(); }

    // paragraphs
    @Override public LiveList<Paragraph<PS, SEG, S>> getParagraphs() { return content.getParagraphs(); }

    private final SuspendableList<Paragraph<PS, SEG, S>> visibleParagraphs;
    @Override public final LiveList<Paragraph<PS, SEG, S>> getVisibleParagraphs() { return visibleParagraphs; }

    // beingUpdated
    private final SuspendableNo beingUpdated = new SuspendableNo();
    public final SuspendableNo beingUpdatedProperty() { return beingUpdated; }
    public final boolean isBeingUpdated() { return beingUpdated.get(); }

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

    private Subscription subscriptions = () -> {};

    // Remembers horizontal position when traversing up / down.
    private Optional<ParagraphBox.CaretOffsetX> targetCaretOffset = Optional.empty();

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

    private final EventStream<Boolean> autoCaretBlinksSteam;
    final EventStream<Boolean> autoCaretBlink() { return autoCaretBlinksSteam; }

    private final EventStream<javafx.util.Duration> caretBlinkRateStream;
    final EventStream<javafx.util.Duration> caretBlinkRateEvents() { return caretBlinkRateStream; }

    final EventStream<?> boundsDirtyFor(EventStream<?> dirtyStream) {
        return EventStreams.merge(viewportDirty, dirtyStream).suppressWhen(beingUpdatedProperty());
    }

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
    public GenericStyledArea(@NamedArg("initialParagraphStyle") PS initialParagraphStyle,
                             @NamedArg("applyParagraphStyle")   BiConsumer<TextFlow, PS> applyParagraphStyle,
                             @NamedArg("initialTextStyle")      S initialTextStyle,
                             @NamedArg("segmentOps")            TextOps<SEG, S> segmentOps,
                             @NamedArg("nodeFactory")           Function<SEG, Node> nodeFactory) {
        this(initialParagraphStyle, applyParagraphStyle, initialTextStyle, segmentOps, true, nodeFactory);
    }

    public GenericStyledArea(@NamedArg("initialParagraphStyle") PS initialParagraphStyle,
                             @NamedArg("applyParagraphStyle")   BiConsumer<TextFlow, PS> applyParagraphStyle,
                             @NamedArg("initialTextStyle")      S initialTextStyle,
                             @NamedArg("segmentOps")            TextOps<SEG, S> segmentOps,
                             @NamedArg("preserveStyle")         boolean preserveStyle,
                             @NamedArg("nodeFactory")           Function<SEG, Node> nodeFactory) {
        this(initialParagraphStyle, applyParagraphStyle, initialTextStyle,
                new GenericEditableStyledDocument<>(initialParagraphStyle, initialTextStyle, segmentOps), segmentOps, preserveStyle, nodeFactory);
    }

    /**
     * The same as {@link #GenericStyledArea(Object, BiConsumer, Object, TextOps, Function)} except that
     * this constructor can be used to create another {@code GenericStyledArea} object that
     * shares the same {@link EditableStyledDocument}.
     */
    public GenericStyledArea(
            @NamedArg("initialParagraphStyle") PS initialParagraphStyle,
            @NamedArg("applyParagraphStyle")   BiConsumer<TextFlow, PS> applyParagraphStyle,
            @NamedArg("initialTextStyle")      S initialTextStyle,
            @NamedArg("document")              EditableStyledDocument<PS, SEG, S> document,
            @NamedArg("segmentOps")            TextOps<SEG, S> segmentOps,
            @NamedArg("nodeFactory")           Function<SEG, Node> nodeFactory) {
        this(initialParagraphStyle, applyParagraphStyle, initialTextStyle, document, segmentOps, true, nodeFactory);

    }

    public GenericStyledArea(
            @NamedArg("initialParagraphStyle") PS initialParagraphStyle,
            @NamedArg("applyParagraphStyle")   BiConsumer<TextFlow, PS> applyParagraphStyle,
            @NamedArg("initialTextStyle")      S initialTextStyle,
            @NamedArg("document")              EditableStyledDocument<PS, SEG, S> document,
            @NamedArg("segmentOps")            TextOps<SEG, S> segmentOps,
            @NamedArg("preserveStyle")         boolean preserveStyle,
            @NamedArg("nodeFactory")           Function<SEG, Node> nodeFactory) {
        this.initialTextStyle = initialTextStyle;
        this.initialParagraphStyle = initialParagraphStyle;
        this.preserveStyle = preserveStyle;
        this.content = document;
        this.applyParagraphStyle = applyParagraphStyle;
        this.segmentOps = segmentOps;

        undoManager = UndoUtils.defaultUndoManager(this);

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

        viewportDirty = merge(
                // no need to check for width & height invalidations as scroll values update when these do

                // scale
                invalidationsOf(scaleXProperty()),
                invalidationsOf(scaleYProperty()),

                // scroll
                invalidationsOf(estimatedScrollXProperty()),
                invalidationsOf(estimatedScrollYProperty())
        ).suppressible();

        autoCaretBlinksSteam = EventStreams.valuesOf(focusedProperty()
                .and(editableProperty())
                .and(disabledProperty().not())
        );
        caretBlinkRateStream = EventStreams.valuesOf(caretBlinkRate);

        mainCaret = new CaretImpl(this);
        mainSelection = new BoundedSelectionImpl<>(this);

        visibleParagraphs = LiveList.map(virtualFlow.visibleCells(), c -> c.getNode().getParagraph()).suspendable();

        final Suspendable omniSuspendable = Suspendable.combine(
                beingUpdated, // must be first, to be the last one to release

                visibleParagraphs
        );
        manageSubscription(omniSuspendable.suspendWhen(content.beingUpdatedProperty()));

        // dispatch MouseOverTextEvents when mouseOverTextDelay is not null
        EventStreams.valuesOf(mouseOverTextDelayProperty())
                .flatMap(delay -> delay != null
                        ? mouseOverTextEvents(nonEmptyCells, delay)
                        : EventStreams.never())
                .subscribe(evt -> Event.fireEvent(this, evt));

        new StyledTextAreaBehavior(this);

        // Code below this point is deprecated Popup API. It will be removed in the future

        // relayout the popup when any of its settings values change (besides the caret being dirty)
        EventStream<?> popupAlignmentDirty = invalidationsOf(popupAlignmentProperty());
        EventStream<?> popupAnchorAdjustmentDirty = invalidationsOf(popupAnchorAdjustmentProperty());
        EventStream<?> popupAnchorOffsetDirty = invalidationsOf(popupAnchorOffsetProperty());
        EventStream<?> popupDirty = merge(popupAlignmentDirty, popupAnchorAdjustmentDirty, popupAnchorOffsetDirty);
        subscribeTo(popupDirty, x -> layoutPopup());

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
    final ParagraphBox.CaretOffsetX getCaretOffsetX(int paragraphIndex) {
        return getCell(paragraphIndex).getCaretOffsetX();
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
        // don't account for padding here since height of virtualFlow is used, not area + potential padding
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
        // mouse position used, so account for padding
        double adjustedX = x - getInsets().getLeft();
        double adjustedY = y - getInsets().getTop();
        VirtualFlowHit<Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>>> hit = virtualFlow.hit(adjustedX, adjustedY);
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

    public final int lineIndex(int paragraphIndex, int column) {
        Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell = virtualFlow.getCell(paragraphIndex);
        return cell.getNode().getCurrentLineIndex(column);
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

    @Override
    public String getText(IndexRange range) {
        return content.getText(range);
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
        return getParagraphSelection(mainSelection, paragraph);
    }

    public IndexRange getParagraphSelection(UnboundedSelection selection, int paragraph) {
        int startPar = selection.getStartParagraphIndex();
        int endPar = selection.getEndPararagraphIndex();

        if(paragraph < startPar || paragraph > endPar) {
            return EMPTY_RANGE;
        }

        int start = paragraph == startPar ? selection.getStartColumnPosition() : 0;
        int end = paragraph == endPar ? selection.getEndColumnPosition() : getParagraphLenth(paragraph);

        // force rangeProperty() to be valid
        selection.getRange();

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
    public void scrollXToPixel(double pixel) {
        suspendVisibleParsWhile(() -> virtualFlow.scrollXToPixel(pixel));
    }

    @Override
    public void scrollYToPixel(double pixel) {
        suspendVisibleParsWhile(() -> virtualFlow.scrollYToPixel(pixel));
    }

    @Override
    public void scrollXBy(double deltaX) {
        suspendVisibleParsWhile(() -> virtualFlow.scrollXBy(deltaX));
    }

    @Override
    public void scrollYBy(double deltaY) {
        suspendVisibleParsWhile(() -> virtualFlow.scrollYBy(deltaY));
    }

    @Override
    public void scrollBy(Point2D deltas) {
        suspendVisibleParsWhile(() -> virtualFlow.scrollBy(deltas));
    }

    void show(double y) {
        suspendVisibleParsWhile(() -> virtualFlow.show(y));
    }

    @Override
    public void showParagraphInViewport(int paragraphIndex) {
        suspendVisibleParsWhile(() -> virtualFlow.show(paragraphIndex));
    }

    @Override
    public void showParagraphAtTop(int paragraphIndex) {
        suspendVisibleParsWhile(() -> virtualFlow.showAsFirst(paragraphIndex));
    }

    @Override
    public void showParagraphAtBottom(int paragraphIndex) {
        suspendVisibleParsWhile(() -> virtualFlow.showAsLast(paragraphIndex));
    }

    void showCaretAtBottom() {
        int parIdx = getCurrentParagraph();
        Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell = virtualFlow.getCell(parIdx);
        Bounds caretBounds = cell.getNode().getCaretBounds();
        double y = caretBounds.getMaxY();
        suspendVisibleParsWhile(() -> virtualFlow.showAtOffset(parIdx, getViewportHeight() - y));
    }

    void showCaretAtTop() {
        int parIdx = getCurrentParagraph();
        Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell = virtualFlow.getCell(parIdx);
        Bounds caretBounds = cell.getNode().getCaretBounds();
        double y = caretBounds.getMinY();
        suspendVisibleParsWhile(() -> virtualFlow.showAtOffset(parIdx, -y));
    }

    @Override
    public void requestFollowCaret() {
        followCaretRequested = true;
        requestLayout();
    }

    /** Assumes this method is called within a {@link #suspendVisibleParsWhile(Runnable)} block */
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

    /**
     * Displaces the caret from the selection by positioning only the caret to the new location without
     * also affecting the selection's {@link #getAnchor() anchor} or the {@link #getSelection() selection}.
     * Do not confuse this method with {@link #moveTo(int)}, which is the normal way of moving the caret.
     * This method can be used to achieve the special case of positioning the caret outside or inside the selection,
     * as opposed to always being at the boundary. Use with care.
     */
    public void displaceCaret(int pos) {
        mainCaret.moveTo(pos);
    }

    /**
     * Hides the area's context menu if it is not {@code null} and it is {@link ContextMenu#isShowing() showing}.
     */
    public final void hideContextMenu() {
        ContextMenu menu = getContextMenu();
        if (menu != null && menu.isShowing()) {
            menu.hide();
        }
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

        int newCaretPos = start + replacement.length();
        selectRange(newCaretPos, newCaretPos);
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
        Insets ins = getInsets();
        visibleParagraphs.suspendWhile(() -> {
            virtualFlow.resizeRelocate(
                    ins.getLeft(), ins.getTop(),
                    getWidth() - ins.getLeft() - ins.getRight(),
                    getHeight() - ins.getTop() - ins.getBottom());

            if(followCaretRequested) {
                followCaretRequested = false;
                try (Guard g = viewportDirty.suspend()) {
                    followCaret();
                }
            }
        });

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
        Val<Boolean> cellCaretVisible = hasCaret.flatMap(x -> x ? mainCaret.visibleProperty() : Val.constant(false));
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

    public final Optional<Bounds> getCaretBoundsOnScreen(int paragraphIndex) {
        return virtualFlow.getCellIfVisible(paragraphIndex)
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

    final Optional<Bounds> impl_bounds_getSelectionBoundsOnScreen(UnboundedSelection selection) {
        if (selection.getLength() == 0) {
            return Optional.empty();
        }
        return impl_getSelectionBoundsOnScreen(selection);
    }

    private Optional<Bounds> impl_getSelectionBoundsOnScreen(UnboundedSelection selection) {
        if (selection.getLength() == 0) {
            return Optional.empty();
        }

        List<Bounds> bounds = new ArrayList<>(selection.getParagraphSpan());
        for (int i = selection.getStartParagraphIndex(); i <= selection.getEndPararagraphIndex(); i++) {
            final int i0 = i;
            virtualFlow.getCellIfVisible(i).ifPresent(c -> {
                IndexRange rangeWithinPar = getParagraphSelection(selection, i0);
                Bounds b = c.getNode().getRangeBoundsOnScreen(rangeWithinPar);
                bounds.add(b);
            });
        }

        return reduceBoundsList(bounds);
    }

    private Optional<Bounds> impl_getSelectionBoundsOnScreen() {
        List<Bounds> bounds = virtualFlow.visibleCells().stream()
                .map(c -> c.getNode().getSelectionBoundsOnScreen())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toCollection(ArrayList::new));

        return reduceBoundsList(bounds);
    }

    private Optional<Bounds> reduceBoundsList(List<Bounds> bounds) {
        if(bounds.size() == 0) {
            return Optional.empty();
        }
        double minX = bounds.stream().mapToDouble(Bounds::getMinX).min().getAsDouble();
        double maxX = bounds.stream().mapToDouble(Bounds::getMaxX).max().getAsDouble();
        double minY = bounds.stream().mapToDouble(Bounds::getMinY).min().getAsDouble();
        double maxY = bounds.stream().mapToDouble(Bounds::getMaxY).max().getAsDouble();
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

    private void suspendVisibleParsWhile(Runnable runnable) {
        Suspendable.combine(beingUpdated, visibleParagraphs).suspendWhile(runnable);
    }

    void clearTargetCaretOffset() {
        targetCaretOffset = Optional.empty();
    }

    ParagraphBox.CaretOffsetX getTargetCaretOffset() {
        return mainCaret.getTargetOffset();
    }

    /* ********************************************************************** *
     *                                                                        *
     * CSS                                                                    *
     *                                                                        *
     * ********************************************************************** */

    private static final CssMetaData<GenericStyledArea<?, ?, ?>, Paint> HIGHLIGHT_FILL
            = new CssMetaData<GenericStyledArea<?, ?, ?>, Paint>("-fx-highlight-fill", StyleConverter.getPaintConverter(), Color.DODGERBLUE
    ) {
        @Override
        public boolean isSettable(GenericStyledArea<?, ?, ?> styleable) {
            return !styleable.highlightFill.isBound();
        }

        @Override
        public StyleableProperty<Paint> getStyleableProperty(GenericStyledArea<?, ?, ?> styleable) {
            return styleable.highlightFill;
        }
    };

    private static final CssMetaData<GenericStyledArea<?, ?, ?>, Paint> HIGHLIGHT_TEXT_FILL
            = new CssMetaData<GenericStyledArea<?, ?, ?>, Paint>("-fx-highlight-text-fill", StyleConverter.getPaintConverter(), Color.WHITE
    ) {
        @Override
        public boolean isSettable(GenericStyledArea<?, ?, ?> styleable) {
            return !styleable.highlightTextFill.isBound();
        }

        @Override
        public StyleableProperty<Paint> getStyleableProperty(GenericStyledArea<?, ?, ?> styleable) {
            return styleable.highlightTextFill;
        }
    };

    private static final CssMetaData<GenericStyledArea<?, ?, ?>, javafx.util.Duration> CARET_BLINK_RATE
            = new CssMetaData<GenericStyledArea<?, ?, ?>, javafx.util.Duration>("-fx-caret-blink-rate", StyleConverter.getDurationConverter(), javafx.util.Duration.millis(500)
    ) {
        @Override
        public boolean isSettable(GenericStyledArea<?, ?, ?> styleable) {
            return !styleable.caretBlinkRate.isBound();
        }

        @Override
        public StyleableProperty<javafx.util.Duration> getStyleableProperty(GenericStyledArea<?, ?, ?> styleable) {
            return styleable.caretBlinkRate;
        }
    };

    private static final List<CssMetaData<? extends Styleable, ?>> CSS_META_DATA_LIST;

    static {
        List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Region.getClassCssMetaData());

        styleables.add(HIGHLIGHT_FILL);
        styleables.add(HIGHLIGHT_TEXT_FILL);
        styleables.add(CARET_BLINK_RATE);

        CSS_META_DATA_LIST = Collections.unmodifiableList(styleables);
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return CSS_META_DATA_LIST;
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS_META_DATA_LIST;
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
