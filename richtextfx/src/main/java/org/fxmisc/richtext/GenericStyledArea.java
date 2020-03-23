package org.fxmisc.richtext;

import static org.reactfx.EventStreams.*;
import static org.reactfx.util.Tuples.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableSet;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
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
import javafx.scene.shape.LineTo;
import javafx.scene.shape.PathElement;
import javafx.scene.text.TextFlow;

import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualFlowHit;
import org.fxmisc.flowless.Virtualized;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.GenericEditableStyledDocument;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.Replacement;
import org.fxmisc.richtext.model.RichTextChange;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyledDocument;
import org.fxmisc.richtext.model.StyledSegment;
import org.fxmisc.richtext.model.TextOps;
import org.fxmisc.richtext.model.TwoDimensional;
import org.fxmisc.richtext.model.TwoLevelNavigator;
import org.fxmisc.richtext.event.MouseOverTextEvent;
import org.fxmisc.richtext.util.SubscribeableContentsObsSet;
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
 * Text editing control that renders and edits a {@link EditableStyledDocument}.
 *
 * Accepts user input (keyboard, mouse) and provides API to assign style to text ranges. It is suitable for
 * syntax highlighting and rich-text editors.
 *
 * <h3>Adding Scrollbars to the Area</h3>
 *
 * <p>By default, scroll bars do not appear when the content spans outside of the viewport.
 * To add scroll bars, the area needs to be wrapped in a {@link VirtualizedScrollPane}. For example, </p>
 * <pre><code>
 * // shows area without scroll bars
 * InlineCssTextArea area = new InlineCssTextArea();
 *
 * // add scroll bars that will display as needed
 * VirtualizedScrollPane&lt;InlineCssTextArea&gt; vsPane = new VirtualizedScrollPane&lt;&gt;(area);
 *
 * Parent parent = // creation code
 * parent.getChildren().add(vsPane)
 * </code></pre>
 *
 * <h3>Auto-Scrolling to the Caret</h3>
 *
 * <p>Every time the underlying {@link EditableStyledDocument} changes via user interaction (e.g. typing) through
 * the {@code GenericStyledArea}, the area will scroll to insure the caret is kept in view. However, this does not
 * occur if changes are done programmatically. For example, let's say the area is displaying the bottom part
 * of the area's {@link EditableStyledDocument} and some code changes something in the top part of the document
 * that is not currently visible. If there is no call to {@link #requestFollowCaret()} at the end of that code,
 * the area will not auto-scroll to that section of the document. The change will occur, and the user will continue
 * to see the bottom part of the document as before. If such a call is there, then the area will scroll
 * to the top of the document and no longer display the bottom part of it.</p>
 * <p>For example...</p>
 * <pre><code>
 * // assuming the user is currently seeing the top of the area
 *
 * // then changing the bottom, currently not visible part of the area...
 * int startParIdx = 40;
 * int startColPosition = 2;
 * int endParIdx = 42;
 * int endColPosition = 10;
 *
 * // ...by itself will not scroll the viewport to where the change occurs
 * area.replaceText(startParIdx, startColPosition, endParIdx, endColPosition, "replacement text");
 *
 * // adding this line after the last modification to the area will cause the viewport to scroll to that change
 * // leaving the following line out will leave the viewport unaffected and the user will not notice any difference
 * area.requestFollowCaret();
 * </code></pre>
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
 * {@code GenericStyledArea} uses {@link javafx.scene.input.KeyEvent#KEY_TYPED KEY_TYPED} to handle ordinary
 * character input and {@link javafx.scene.input.KeyEvent#KEY_PRESSED KEY_PRESSED} to handle control key
 * combinations (including Enter and Tab). To add or override some keyboard
 * shortcuts, while keeping the rest in place, you would combine the default
 * event handler with a new one that adds or overrides some of the default
 * key combinations.
 * <p>
 *     For example, this is how to bind {@code Ctrl+S} to the {@code save()} operation:
 * </p>
 * <pre><code>
 * import static javafx.scene.input.KeyCode.*;
 * import static javafx.scene.input.KeyCombination.*;
 * import static org.fxmisc.wellbehaved.event.EventPattern.*;
 * import static org.fxmisc.wellbehaved.event.InputMap.*;
 *
 * import org.fxmisc.wellbehaved.event.Nodes;
 *
 * // installs the following consume InputMap,
 * // so that a CTRL+S event saves the document and consumes the event
 * Nodes.addInputMap(area, consume(keyPressed(S, CONTROL_DOWN), event -&gt; save()));
 * </code></pre>
 *
 * <h3>Overriding default mouse behavior</h3>
 *
 * The area's default mouse behavior properly handles auto-scrolling and dragging the selected text to a new location.
 * As such, some parts cannot be partially overridden without it affecting other behavior.
 *
 * <p>The following lists either {@link org.fxmisc.wellbehaved.event.EventPattern}s that cannot be
 * overridden without negatively affecting the default mouse behavior or describe how to safely override things
 * in a special way without disrupting the auto scroll behavior.</p>
 * <ul>
 *     <li>
 *         <em>First (1 click count) Primary Button Mouse Pressed Events:</em>
 *         (<code>EventPattern.mousePressed(MouseButton.PRIMARY).onlyIf(e -&gt; e.getClickCount() == 1)</code>).
 *         Do not override. Instead, use {@link #onOutsideSelectionMousePressed},
 *         {@link #onInsideSelectionMousePressReleased}, or see next item.
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
 *         Do not override. Instead, use {@link #onNewSelectionDragFinished}, {@link #onSelectionDropped}, or see next item.
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
 * <h3>CSS, Style Classes, and Pseudo Classes</h3>
 * <p>
 *     Refer to the <a href="https://github.com/FXMisc/RichTextFX/wiki/RichTextFX-CSS-Reference-Guide">
 *         RichTextFX CSS Reference Guide
 *     </a>.
 * </p>
 *
 * <h3>Area Actions and Other Operations</h3>
 * <p>
 *     To distinguish the actual operations one can do on this area from the boilerplate methods
 *     within this area (e.g. properties and their getters/setters, etc.), look at the interfaces
 *     this area implements. Each lists and documents methods that fall under that category.
 * </p>
 * <p>
 *     To update multiple portions of the area's underlying document in one call, see {@link #createMultiChange()}.
 * </p>
 *
 * <h3>Calculating a Position Within the Area</h3>
 * <p>
 *     To calculate a position or index within the area, read through the javadoc of
 *     {@link org.fxmisc.richtext.model.TwoDimensional} and {@link org.fxmisc.richtext.model.TwoDimensional.Bias}.
 *     Also, read the difference between "position" and "index" in
 *     {@link org.fxmisc.richtext.model.StyledDocument#getAbsolutePosition(int, int)}.
 * </p>
 *
 * @see EditableStyledDocument
 * @see TwoDimensional
 * @see org.fxmisc.richtext.model.TwoDimensional.Bias
 * @see VirtualFlow
 * @see VirtualizedScrollPane
 * @see Caret
 * @see Selection
 * @see CaretSelectionBind
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

    private static final PseudoClass READ_ONLY = PseudoClass.getPseudoClass("readonly");
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
     * Text color for highlighted text.
     */
    private final StyleableObjectProperty<Paint> highlightTextFill
            = new CustomStyleableProperty<>(Color.WHITE, "highlightTextFill", this, HIGHLIGHT_TEXT_FILL);

    // editable property
    private final BooleanProperty editable = new SimpleBooleanProperty(this, "editable", true) {
        @Override
        protected void invalidated() {
            ((Region) getBean()).pseudoClassStateChanged(READ_ONLY, !get());
        }
    };
    @Override public final BooleanProperty editableProperty() { return editable; }
    // Don't remove as FXMLLoader doesn't recognise default methods !
    @Override public void setEditable(boolean value) { editable.set(value); }
    @Override public boolean isEditable() { return editable.get(); }

    // wrapText property
    private final BooleanProperty wrapText = new SimpleBooleanProperty(this, "wrapText");
    @Override public final BooleanProperty wrapTextProperty() { return wrapText; }
    // Don't remove as FXMLLoader doesn't recognise default methods !
    @Override public void setWrapText(boolean value) { wrapText.set(value); }
    @Override public boolean isWrapText() { return wrapText.get(); }

    // undo manager
    private UndoManager undoManager;
    @Override public UndoManager getUndoManager() { return undoManager; }
    /**
     * @param undoManager may be null in which case a no op undo manager will be set.
     */
    @Override public void setUndoManager(UndoManager undoManager) {
        this.undoManager.close();
        this.undoManager = undoManager != null ? undoManager : UndoUtils.noOpUndoManager();
    }

    private Locale textLocale = Locale.getDefault();
    /**
     * This is used to determine word and sentence breaks while navigating or selecting.
     * Override this method if your paragraph or text style accommodates Locales as well.
     * @return Locale.getDefault() by default
     */
    @Override
    public Locale getLocale() { return textLocale; }
    public void setLocale( Locale editorLocale ) {
        textLocale = editorLocale;
    }

    private final ObjectProperty<Duration> mouseOverTextDelay = new SimpleObjectProperty<>(null);
    @Override public ObjectProperty<Duration> mouseOverTextDelayProperty() { return mouseOverTextDelay; }

    private final ObjectProperty<IntFunction<? extends Node>> paragraphGraphicFactory = new SimpleObjectProperty<>(null);
    @Override public ObjectProperty<IntFunction<? extends Node>> paragraphGraphicFactoryProperty() { return paragraphGraphicFactory; }
    
    public void recreateParagraphGraphic( int parNdx ) {
        ObjectProperty<IntFunction<? extends Node>> gProp;
        gProp = getCell(parNdx).graphicFactoryProperty();
        gProp.unbind();
        gProp.bind(paragraphGraphicFactoryProperty());
    }

    public Node getParagraphGraphic( int parNdx ) {
        return getCell(parNdx).getGraphic();
    }

    /**
     * This Node is shown to the user, centered over the area, when the area has no text content.
     */
    private ObjectProperty<Node> placeHolderProp = new SimpleObjectProperty<>(this, "placeHolder", null);
    public final ObjectProperty<Node> placeholderProperty() { return placeHolderProp; }
    public final void setPlaceholder(Node value) { placeHolderProp.set(value); }
    public final Node getPlaceholder() { return placeHolderProp.get(); }
    
    private ObjectProperty<ContextMenu> contextMenu = new SimpleObjectProperty<>(null);
    @Override public final ObjectProperty<ContextMenu> contextMenuObjectProperty() { return contextMenu; }
    // Don't remove as FXMLLoader doesn't recognise default methods !
    @Override public void setContextMenu(ContextMenu menu) { contextMenu.set(menu); }
    @Override public ContextMenu getContextMenu() { return contextMenu.get(); }

    protected final boolean isContextMenuPresent() { return contextMenu.get() != null; }

    private DoubleProperty contextMenuXOffset = new SimpleDoubleProperty(2);
    @Override public final DoubleProperty contextMenuXOffsetProperty() { return contextMenuXOffset; }
    // Don't remove as FXMLLoader doesn't recognise default methods !
    @Override public void setContextMenuXOffset(double offset) { contextMenuXOffset.set(offset); }
    @Override public double getContextMenuXOffset() { return contextMenuXOffset.get(); }

    private DoubleProperty contextMenuYOffset = new SimpleDoubleProperty(2);
    @Override public final DoubleProperty contextMenuYOffsetProperty() { return contextMenuYOffset; }
    // Don't remove as FXMLLoader doesn't recognise default methods !
    @Override public void setContextMenuYOffset(double offset) { contextMenuYOffset.set(offset); }
    @Override public double getContextMenuYOffset() { return contextMenuYOffset.get(); }

    private final BooleanProperty useInitialStyleForInsertion = new SimpleBooleanProperty();
    @Override public BooleanProperty useInitialStyleForInsertionProperty() { return useInitialStyleForInsertion; }

    private Optional<Tuple2<Codec<PS>, Codec<StyledSegment<SEG, S>>>> styleCodecs = Optional.empty();
    @Override public void setStyleCodecs(Codec<PS> paragraphStyleCodec, Codec<StyledSegment<SEG, S>> styledSegCodec) {
        styleCodecs = Optional.of(t(paragraphStyleCodec, styledSegCodec));
    }
    @Override public Optional<Tuple2<Codec<PS>, Codec<StyledSegment<SEG, S>>>> getStyleCodecs() {
        return styleCodecs;
    }

    @Override public Var<Double> estimatedScrollXProperty() { return virtualFlow.estimatedScrollXProperty(); }

    @Override public Var<Double> estimatedScrollYProperty() { return virtualFlow.estimatedScrollYProperty(); }

    private final SubscribeableContentsObsSet<CaretNode> caretSet;
    private final SubscribeableContentsObsSet<Selection<PS, SEG, S>> selectionSet;

    public final boolean addCaret(CaretNode caret) {
        if (caret.getArea() != this) {
            throw new IllegalArgumentException(String.format(
                    "The caret (%s) is associated with a different area (%s), " +
                            "not this area (%s)", caret, caret.getArea(), this));
        }
        return caretSet.add(caret);
    }

    public final boolean removeCaret(CaretNode caret) {
        if (caret != caretSelectionBind.getUnderlyingCaret()) {
            return caretSet.remove(caret);
        } else {
            return false;
        }
    }

    public final boolean addSelection(Selection<PS, SEG, S> selection) {
        if (selection.getArea() != this) {
            throw new IllegalArgumentException(String.format(
                    "The selection (%s) is associated with a different area (%s), " +
                            "not this area (%s)", selection, selection.getArea(), this));
        }
        return selectionSet.add(selection);
    }

    public final boolean removeSelection(Selection<PS, SEG, S> selection) {
        if (selection != caretSelectionBind.getUnderlyingSelection()) {
            return selectionSet.remove(selection);
        } else {
            return false;
        }
    }

    /* ********************************************************************** *
     *                                                                        *
     * Mouse Behavior Hooks                                                   *
     *                                                                        *
     * Hooks for overriding some of the default mouse behavior                *
     *                                                                        *
     * ********************************************************************** */

    @Override public final EventHandler<MouseEvent> getOnOutsideSelectionMousePressed() { return onOutsideSelectionMousePressed.get(); }
    @Override public final void setOnOutsideSelectionMousePressed(EventHandler<MouseEvent> handler) { onOutsideSelectionMousePressed.set( handler ); }
    @Override public final ObjectProperty<EventHandler<MouseEvent>> onOutsideSelectionMousePressedProperty() { return onOutsideSelectionMousePressed; }
    private final ObjectProperty<EventHandler<MouseEvent>> onOutsideSelectionMousePressed = new SimpleObjectProperty<>( e -> {
        moveTo( hit( e.getX(), e.getY() ).getInsertionIndex(), SelectionPolicy.CLEAR );
    });

    @Override public final EventHandler<MouseEvent> getOnInsideSelectionMousePressReleased() { return onInsideSelectionMousePressReleased.get(); }
    @Override public final void setOnInsideSelectionMousePressReleased(EventHandler<MouseEvent> handler) { onInsideSelectionMousePressReleased.set( handler ); }
    @Override public final ObjectProperty<EventHandler<MouseEvent>> onInsideSelectionMousePressReleasedProperty() { return onInsideSelectionMousePressReleased; }
    private final ObjectProperty<EventHandler<MouseEvent>> onInsideSelectionMousePressReleased = new SimpleObjectProperty<>( e -> {
        moveTo( hit( e.getX(), e.getY() ).getInsertionIndex(), SelectionPolicy.CLEAR );
    });

    private final ObjectProperty<Consumer<Point2D>> onNewSelectionDrag = new SimpleObjectProperty<>(p -> {
        CharacterHit hit = hit(p.getX(), p.getY());
        moveTo(hit.getInsertionIndex(), SelectionPolicy.ADJUST);
    });
    @Override public final ObjectProperty<Consumer<Point2D>> onNewSelectionDragProperty() { return onNewSelectionDrag; }

    @Override public final EventHandler<MouseEvent> getOnNewSelectionDragFinished() { return onNewSelectionDragFinished.get(); }
    @Override public final void setOnNewSelectionDragFinished(EventHandler<MouseEvent> handler) { onNewSelectionDragFinished.set( handler ); }
    @Override public final ObjectProperty<EventHandler<MouseEvent>> onNewSelectionDragFinishedProperty() { return onNewSelectionDragFinished; }
    private final ObjectProperty<EventHandler<MouseEvent>> onNewSelectionDragFinished = new SimpleObjectProperty<>( e -> {
        CharacterHit hit = hit(e.getX(), e.getY());
        moveTo(hit.getInsertionIndex(), SelectionPolicy.ADJUST);
    });

    private final ObjectProperty<Consumer<Point2D>> onSelectionDrag = new SimpleObjectProperty<>(p -> {
        CharacterHit hit = hit(p.getX(), p.getY());
        displaceCaret(hit.getInsertionIndex());
    });
    @Override public final ObjectProperty<Consumer<Point2D>> onSelectionDragProperty() { return onSelectionDrag; }

    @Override public final EventHandler<MouseEvent> getOnSelectionDropped() { return onSelectionDropped.get(); }
    @Override public final void setOnSelectionDropped(EventHandler<MouseEvent> handler) { onSelectionDropped.set( handler ); }
    @Override public final ObjectProperty<EventHandler<MouseEvent>> onSelectionDroppedProperty() { return onSelectionDropped; }
    private final ObjectProperty<EventHandler<MouseEvent>> onSelectionDropped = new SimpleObjectProperty<>( e -> {
        moveSelectedText( hit( e.getX(), e.getY() ).getInsertionIndex() );
    });

    // not a hook, but still plays a part in the default mouse behavior
    private final BooleanProperty autoScrollOnDragDesired = new SimpleBooleanProperty(true);
    @Override public final BooleanProperty autoScrollOnDragDesiredProperty() { return autoScrollOnDragDesired; }
    // Don't remove as FXMLLoader doesn't recognise default methods !
    @Override public void setAutoScrollOnDragDesired(boolean val) { autoScrollOnDragDesired.set(val); }
    @Override public boolean isAutoScrollOnDragDesired() { return autoScrollOnDragDesired.get(); }

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
    @Override public final ObservableValue<String> textProperty() { return content.textProperty(); }

    // rich text
    @Override public final StyledDocument<PS, SEG, S> getDocument() { return content; }

    private CaretSelectionBind<PS, SEG, S> caretSelectionBind;
    @Override public final CaretSelectionBind<PS, SEG, S> getCaretSelectionBind() { return caretSelectionBind; }

    // length
    @Override public final ObservableValue<Integer> lengthProperty() { return content.lengthProperty(); }

    // paragraphs
    @Override public LiveList<Paragraph<PS, SEG, S>> getParagraphs() { return content.getParagraphs(); }

    private final SuspendableList<Paragraph<PS, SEG, S>> visibleParagraphs;
    @Override public final LiveList<Paragraph<PS, SEG, S>> getVisibleParagraphs() { return visibleParagraphs; }

    // beingUpdated
    private final SuspendableNo beingUpdated = new SuspendableNo();
    @Override public final SuspendableNo beingUpdatedProperty() { return beingUpdated; }

    // total width estimate
    @Override public Val<Double> totalWidthEstimateProperty() { return virtualFlow.totalWidthEstimateProperty(); }

    // total height estimate
    @Override public Val<Double> totalHeightEstimateProperty() { return virtualFlow.totalHeightEstimateProperty(); }

    /* ********************************************************************** *
     *                                                                        *
     * Event streams                                                          *
     *                                                                        *
     * ********************************************************************** */

    @Override public EventStream<List<RichTextChange<PS, SEG, S>>> multiRichChanges() { return content.multiRichChanges(); }

    @Override public EventStream<List<PlainTextChange>> multiPlainChanges() { return content.multiPlainChanges(); }

    // text changes
    @Override public final EventStream<PlainTextChange> plainTextChanges() { return content.plainChanges(); }

    // rich text changes
    @Override public final EventStream<RichTextChange<PS, SEG, S>> richChanges() { return content.richChanges(); }

    private final SuspendableEventStream<?> viewportDirty;
    @Override public final EventStream<?> viewportDirtyEvents() { return viewportDirty; }

    /* ********************************************************************** *
     *                                                                        *
     * Private fields                                                         *
     *                                                                        *
     * ********************************************************************** */

    private Subscription subscriptions = () -> {};

    private final VirtualFlow<Paragraph<PS, SEG, S>, Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>>> virtualFlow;

    // used for two-level navigation, where on the higher level are
    // paragraphs and on the lower level are lines within a paragraph
    private final TwoLevelNavigator paragraphLineNavigator;

    private boolean paging, followCaretRequested = false;

    /* ********************************************************************** *
     *                                                                        *
     * Fields necessary for Cloning                                           *
     *                                                                        *
     * ********************************************************************** */

    private final EditableStyledDocument<PS, SEG, S> content;
    @Override public final EditableStyledDocument<PS, SEG, S> getContent() { return content; }

    private final S initialTextStyle;
    @Override public final S getInitialTextStyle() { return initialTextStyle; }

    private final PS initialParagraphStyle;
    @Override public final PS getInitialParagraphStyle() { return initialParagraphStyle; }

    private final BiConsumer<TextFlow, PS> applyParagraphStyle;
    @Override public final BiConsumer<TextFlow, PS> getApplyParagraphStyle() { return applyParagraphStyle; }

    // TODO: Currently, only undo/redo respect this flag.
    private final boolean preserveStyle;
    @Override public final boolean isPreserveStyle() { return preserveStyle; }

    /* ********************************************************************** *
     *                                                                        *
     * Miscellaneous                                                          *
     *                                                                        *
     * ********************************************************************** */

    private final TextOps<SEG, S> segmentOps;
    @Override public final TextOps<SEG, S> getSegOps() { return segmentOps; }

    private final EventStream<Boolean> autoCaretBlinksSteam;
    final EventStream<Boolean> autoCaretBlink() { return autoCaretBlinksSteam; }

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
                             @NamedArg("nodeFactory")           Function<StyledSegment<SEG, S>, Node> nodeFactory) {
        this(initialParagraphStyle, applyParagraphStyle, initialTextStyle, segmentOps, true, nodeFactory);
    }

    /**
     * Same as {@link #GenericStyledArea(Object, BiConsumer, Object, TextOps, Function)} but also allows one
     * to specify whether the undo manager should be a plain or rich undo manager via {@code preserveStyle}.
     *
     * @param initialParagraphStyle style to use in places where no other style is specified (yet).
     * @param applyParagraphStyle function that, given a {@link TextFlow} node and
     *                            a style, applies the style to the paragraph node. This function is
     *                            used by the default skin to apply style to paragraph nodes.
     * @param initialTextStyle style to use in places where no other style is specified (yet).
     * @param segmentOps The operations which are defined on the text segment objects.
     * @param preserveStyle whether to use an undo manager that can undo/redo {@link RichTextChange}s or
     *                      {@link PlainTextChange}s
     * @param nodeFactory A function which is used to create the JavaFX scene node for a particular segment.
     */
    public GenericStyledArea(@NamedArg("initialParagraphStyle") PS initialParagraphStyle,
                             @NamedArg("applyParagraphStyle")   BiConsumer<TextFlow, PS> applyParagraphStyle,
                             @NamedArg("initialTextStyle")      S initialTextStyle,
                             @NamedArg("segmentOps")            TextOps<SEG, S> segmentOps,
                             @NamedArg("preserveStyle")         boolean preserveStyle,
                             @NamedArg("nodeFactory")           Function<StyledSegment<SEG, S>, Node> nodeFactory) {
        this(initialParagraphStyle, applyParagraphStyle, initialTextStyle,
                new GenericEditableStyledDocument<>(initialParagraphStyle, initialTextStyle, segmentOps), segmentOps, preserveStyle, nodeFactory);
    }

    /**
     * The same as {@link #GenericStyledArea(Object, BiConsumer, Object, TextOps, Function)} except that
     * this constructor can be used to create another {@code GenericStyledArea} that renders and edits the same
     * {@link EditableStyledDocument} or when one wants to use a custom {@link EditableStyledDocument} implementation.
     */
    public GenericStyledArea(
            @NamedArg("initialParagraphStyle") PS initialParagraphStyle,
            @NamedArg("applyParagraphStyle")   BiConsumer<TextFlow, PS> applyParagraphStyle,
            @NamedArg("initialTextStyle")      S initialTextStyle,
            @NamedArg("document")              EditableStyledDocument<PS, SEG, S> document,
            @NamedArg("segmentOps")            TextOps<SEG, S> segmentOps,
            @NamedArg("nodeFactory")           Function<StyledSegment<SEG, S>, Node> nodeFactory) {
        this(initialParagraphStyle, applyParagraphStyle, initialTextStyle, document, segmentOps, true, nodeFactory);

    }

    /**
     * Creates an area with flexibility in all of its options.
     *
     * @param initialParagraphStyle style to use in places where no other style is specified (yet).
     * @param applyParagraphStyle function that, given a {@link TextFlow} node and
     *                            a style, applies the style to the paragraph node. This function is
     *                            used by the default skin to apply style to paragraph nodes.
     * @param initialTextStyle style to use in places where no other style is specified (yet).
     * @param document the document to render and edit
     * @param segmentOps The operations which are defined on the text segment objects.
     * @param preserveStyle whether to use an undo manager that can undo/redo {@link RichTextChange}s or
     *                      {@link PlainTextChange}s
     * @param nodeFactory A function which is used to create the JavaFX scene node for a particular segment.
     */
    public GenericStyledArea(
            @NamedArg("initialParagraphStyle") PS initialParagraphStyle,
            @NamedArg("applyParagraphStyle")   BiConsumer<TextFlow, PS> applyParagraphStyle,
            @NamedArg("initialTextStyle")      S initialTextStyle,
            @NamedArg("document")              EditableStyledDocument<PS, SEG, S> document,
            @NamedArg("segmentOps")            TextOps<SEG, S> segmentOps,
            @NamedArg("preserveStyle")         boolean preserveStyle,
            @NamedArg("nodeFactory")           Function<StyledSegment<SEG, S>, Node> nodeFactory) {
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

        caretSet = new SubscribeableContentsObsSet<>();
        manageSubscription(() -> {
            List<CaretNode> l = new ArrayList<>(caretSet);
            caretSet.clear();
            l.forEach(CaretNode::dispose);
        });

        selectionSet = new SubscribeableContentsObsSet<>();
        manageSubscription(() -> {
            List<Selection<PS, SEG, S>> l = new ArrayList<>(selectionSet);
            selectionSet.clear();
            l.forEach(Selection::dispose);
        });

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
        paragraphLineNavigator = new TwoLevelNavigator(cellCount, cellLength);

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

        caretSelectionBind = new CaretSelectionBindImpl<>("main-caret", "main-selection",this);
        caretSet.add(caretSelectionBind.getUnderlyingCaret());
        selectionSet.add(caretSelectionBind.getUnderlyingSelection());

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

        new GenericStyledAreaBehavior(this);

        // Setup place holder visibility & placement 
        final Val<Boolean> showPlaceholder = Val.create
        (
            () -> getLength() == 0 && ! isFocused(),
            lengthProperty(), focusedProperty()
        );

        placeHolderProp.addListener( (ob,ov,newNode) -> displayPlaceHolder( showPlaceholder.getValue(), newNode ) );
        showPlaceholder.addListener( (ob,ov,show) -> displayPlaceHolder( show, getPlaceholder() ) );
    }

    private Node placeholder;

    private void displayPlaceHolder( boolean show, Node newNode )
    {
        if ( placeholder != null && (! show || newNode != placeholder) )
        {
            placeholder.layoutXProperty().unbind();
            placeholder.layoutYProperty().unbind();
            getChildren().remove( placeholder );
            placeholder = null;
            setClip( null );
        }
        if ( newNode != null && show && newNode != placeholder )
        {
            configurePlaceholder( newNode );
            getChildren().add( newNode );
            placeholder = newNode;
        }
    }

    protected void configurePlaceholder( Node placeholder )
    {
        placeholder.layoutYProperty().bind( Bindings.createDoubleBinding( () ->
            (getHeight() - placeholder.getLayoutBounds().getHeight()) / 2,
            heightProperty(), placeholder.layoutBoundsProperty() )
        );

        placeholder.layoutXProperty().bind( Bindings.createDoubleBinding( () ->
            (getWidth() - placeholder.getLayoutBounds().getWidth()) / 2,
            widthProperty(), placeholder.layoutBoundsProperty() )
        );
    }

    /* ********************************************************************** *
     *                                                                        *
     * Queries                                                                *
     *                                                                        *
     * Queries are parameterized observables.                                 *
     *                                                                        *
     * ********************************************************************** */

    @Override
    public final double getViewportHeight() {
        return virtualFlow.getHeight();
    }

    @Override
    public final Optional<Integer> allParToVisibleParIndex(int allParIndex) {
        if (allParIndex < 0) {
            throw new IllegalArgumentException("The given paragraph index (allParIndex) cannot be negative but was " + allParIndex);
        }
        if (allParIndex >= getParagraphs().size()) {
            throw new IllegalArgumentException(String.format(
                    "Paragraphs' last index is [%s] but allParIndex was [%s]",
                    getParagraphs().size() - 1, allParIndex)
            );
        }
        List<Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>>> visibleList = virtualFlow.visibleCells();
        int firstVisibleParIndex = visibleList.get( 0 ).getNode().getIndex();
        int targetIndex = allParIndex - firstVisibleParIndex;
        
        if ( allParIndex >= firstVisibleParIndex && targetIndex < visibleList.size() )
        {
            if ( visibleList.get( targetIndex ).getNode().getIndex() == allParIndex )
            {
                return Optional.of( targetIndex );
            }
        }
        return Optional.empty();
    }

    @Override
    public final int visibleParToAllParIndex(int visibleParIndex) {
        if (visibleParIndex < 0) {
            throw new IllegalArgumentException("Visible paragraph index cannot be negative but was " + visibleParIndex);
        }
        if (visibleParIndex >= getVisibleParagraphs().size()) {
            throw new IllegalArgumentException(String.format(
                    "Visible paragraphs' last index is [%s] but visibleParIndex was [%s]",
                    getVisibleParagraphs().size() - 1, visibleParIndex)
            );
        }
        return virtualFlow.visibleCells().get( visibleParIndex ).getNode().getIndex();
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

    @Override
    public final int lineIndex(int paragraphIndex, int columnPosition) {
        Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell = virtualFlow.getCell(paragraphIndex);
        return cell.getNode().getCurrentLineIndex(columnPosition);
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

    @Override
    public StyledDocument<PS, SEG, S> subDocument(int start, int end) {
        return content.subSequence(start, end);
    }

    @Override
    public StyledDocument<PS, SEG, S> subDocument(int paragraphIndex) {
        return content.subDocument(paragraphIndex);
    }

    @Override
    public IndexRange getParagraphSelection(Selection selection, int paragraph) {
        int startPar = selection.getStartParagraphIndex();
        int endPar = selection.getEndParagraphIndex();

        if(selection.getLength() == 0 || paragraph < startPar || paragraph > endPar) {
            return EMPTY_RANGE;
        }

        int start = paragraph == startPar ? selection.getStartColumnPosition() : 0;
        int end = paragraph == endPar ? selection.getEndColumnPosition() : getParagraphLength(paragraph) + 1;

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

    @Override
    public Bounds getVisibleParagraphBoundsOnScreen(int visibleParagraphIndex) {
        return getParagraphBoundsOnScreen(virtualFlow.visibleCells().get(visibleParagraphIndex));
    }

    @Override
    public Optional<Bounds> getParagraphBoundsOnScreen(int paragraphIndex) {
        return virtualFlow.getCellIfVisible(paragraphIndex).map(this::getParagraphBoundsOnScreen);
    }

    @Override
    public final <T extends Node & Caret> Optional<Bounds> getCaretBoundsOnScreen(T caret) {
        return virtualFlow.getCellIfVisible(caret.getParagraphIndex())
                .map(c -> c.getNode().getCaretBoundsOnScreen(caret));
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

    @Override
    public void showParagraphRegion(int paragraphIndex, Bounds region) {
        suspendVisibleParsWhile(() -> virtualFlow.show(paragraphIndex, region));
    }

    @Override
    public void requestFollowCaret() {
        followCaretRequested = true;
        requestLayout();
    }

    @Override
    public void lineStart(SelectionPolicy policy) {
        moveTo(getCurrentParagraph(), getCurrentLineStartInParargraph(), policy);
    }

    @Override
    public void lineEnd(SelectionPolicy policy) {
        moveTo(getCurrentParagraph(), getCurrentLineEndInParargraph(), policy);
    }

    public int getCurrentLineStartInParargraph() {
        return virtualFlow.getCell(getCurrentParagraph()).getNode().getCurrentLineStartPosition(caretSelectionBind.getUnderlyingCaret());
    }

    public int getCurrentLineEndInParargraph() {
        return virtualFlow.getCell(getCurrentParagraph()).getNode().getCurrentLineEndPosition(caretSelectionBind.getUnderlyingCaret());
    }
    
    private double caretPrevY = -1;
    private Selection<PS, SEG, S> lineHighlighter;
    private ObjectProperty<Paint> lineHighlighterFill; 
    
    /**
     * The default fill is "highlighter" yellow. It can also be styled using CSS with:<br>
     * <code>.styled-text-area .line-highlighter { -fx-fill: lime; }</code><br>
     * CSS selectors from Path, Shape, and Node can also be used.
     */
    public void setLineHighlighterFill( Paint highlight )
    {
        if ( lineHighlighterFill != null && highlight != null ) {
            lineHighlighterFill.set( highlight );
        }
        else {
            boolean lineHighlightOn = isLineHighlighterOn();
            if ( lineHighlightOn ) setLineHighlighterOn( false );
            
            if ( highlight == null ) lineHighlighterFill = null;
            else lineHighlighterFill = new SimpleObjectProperty( highlight );
            
            if ( lineHighlightOn ) setLineHighlighterOn( true );
        }
    }
    
    public boolean isLineHighlighterOn() {
        return lineHighlighter != null && selectionSet.contains( lineHighlighter ) ;
    }
    
    /**
     * Highlights the line that the main caret is on.<br>
     * Line highlighting automatically follows the caret.
     */
    public void setLineHighlighterOn( boolean show )
    {
        if ( show )
        {
            if ( lineHighlighter != null ) return;
            
            lineHighlighter = new SelectionImpl<>( "line-highlighter", this, path ->
            {
                if ( lineHighlighterFill == null ) path.setHighlightFill( Color.YELLOW );
                else path.highlightFillProperty().bind( lineHighlighterFill );
                
                path.getElements().addListener( (Change<? extends PathElement> chg) -> 
                {
                    if ( chg.next() && chg.wasAdded() || chg.wasReplaced() ) {
                        double width = getLayoutBounds().getWidth();
                        // The path is limited to the bounds of the text, so here it's altered to the area's width
                        chg.getAddedSubList().stream().skip(1).limit(2).forEach( ele -> ((LineTo) ele).setX( width ) );
                        // The path can wrap onto another line if enough text is inserted, so here it's trimmed
                        if ( chg.getAddedSize() > 5 ) path.getElements().remove( 5, 10 );
                        // Highlight masks the downward selection of text on the last line, so move it behind
                        path.toBack();
                    }
                } );
            } );
            
            Runnable adjustHighlighterRange = () ->
            {
                if ( lineHighlighter != null )
                {
                    int p = getCurrentParagraph();
                    int start = getCurrentLineStartInParargraph();
                    int end = getCurrentLineEndInParargraph();
                    if (end == 0) end++;// +1 for empty lines
                    lineHighlighter.selectRange( p, start, p, end );
                }
            };
            
            Consumer<Bounds> caretListener = b -> 
            {
                if ( b.getMinY() != caretPrevY || getCaretColumn() == 1 ) {
                	adjustHighlighterRange.run();
                    caretPrevY = b.getMinY();
                }
            };
            
            widthProperty().addListener( (ob,ov,nv) -> Platform.runLater( adjustHighlighterRange ) );
            caretBoundsProperty().addListener( (ob,ov,nv) -> nv.ifPresent( caretListener ) );
            getCaretBounds().ifPresent( caretListener );
            selectionSet.add( lineHighlighter );
        }
        else if ( lineHighlighter != null ) {
            selectionSet.remove( lineHighlighter );
            lineHighlighter.deselect();
            lineHighlighter = null;
            caretPrevY = -1;
        }
    }

    @Override
    public void prevPage(SelectionPolicy selectionPolicy) {
    	page( -1, selectionPolicy );
    }

    @Override
    public void nextPage(SelectionPolicy selectionPolicy) {
    	page( +1, selectionPolicy );
    }
    
    /**
     * @param pgCount the number of pages to page up/down.
     * <br>Negative numbers for paging up and positive for down.
     */
    private void page(int pgCount, SelectionPolicy selectionPolicy)
    {
        // Use underlying caret to get the same behaviour as navigating up/down a line where the x position is sticky
        Optional<Bounds> cb = caretSelectionBind.getUnderlyingCaret().getCaretBounds();
    	
        paging = true; // Prevent scroll from reverting back to the current caret position
        scrollYBy( pgCount * getViewportHeight() );

        cb.map( this::screenToLocal ) // Place caret near the same on screen position as before
            .map( b -> hit( b.getMinX(), b.getMinY()+b.getHeight()/2.0 ).getInsertionIndex() )
            .ifPresent( i -> caretSelectionBind.moveTo( i, selectionPolicy ) );

        // Adjust scroll by a few pixels to get the caret at the exact on screen location as before 
        cb.ifPresent( prev -> getCaretBounds().map( newB -> newB.getMinY() - prev.getMinY() )
            .filter( delta -> delta != 0.0 ).ifPresent( delta -> scrollYBy( delta ) ) ); 
    }

    @Override
    public void displaceCaret(int pos) {
        caretSelectionBind.displaceCaret(pos);
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

    /**
     * If you want to preset the style to be used for inserted text. Note that useInitialStyleForInsertion overrides this if true.
     */
    public final void setTextInsertionStyle( S txtStyle ) { insertionTextStyle = txtStyle; }
    public final S getTextInsertionStyle() { return insertionTextStyle; }
    private S insertionTextStyle;

    @Override
    public final S getTextStyleForInsertionAt(int pos) {
    	if ( insertionTextStyle != null ) {
        	return insertionTextStyle;
        } else if ( useInitialStyleForInsertion.get() ) {
            return initialTextStyle;
        } else {
            return content.getStyleAtPosition(pos);
        }
    }

    private PS insertionParagraphStyle;
    /**
     * If you want to preset the style to be used. Note that useInitialStyleForInsertion overrides this if true.
     */
    public final void setParagraphInsertionStyle( PS paraStyle ) { insertionParagraphStyle = paraStyle; }
    public final PS getParagraphInsertionStyle() { return insertionParagraphStyle; }

    @Override
    public final PS getParagraphStyleForInsertionAt(int pos) {
    	if ( insertionParagraphStyle != null ) {
        	return insertionParagraphStyle;
        } else if ( useInitialStyleForInsertion.get() ) {
            return initialParagraphStyle;
        } else {
            return content.getParagraphStyleAtPosition(pos);
        }
    }

    @Override
    public void replaceText(int start, int end, String text) {
        StyledDocument<PS, SEG, S> doc = ReadOnlyStyledDocument.fromString(
                text, getParagraphStyleForInsertionAt(start), getTextStyleForInsertionAt(start), segmentOps
        );
        replace(start, end, doc);
    }

    @Override
    public void replace(int start, int end, SEG seg, S style) {
        StyledDocument<PS, SEG, S> doc = ReadOnlyStyledDocument.fromSegment(
                seg, getParagraphStyleForInsertionAt(start), style, segmentOps
        );
        replace(start, end, doc);
    }

    @Override
    public void replace(int start, int end, StyledDocument<PS, SEG, S> replacement) {
        content.replace(start, end, replacement);

        int newCaretPos = start + replacement.length();
        selectRange(newCaretPos, newCaretPos);
    }

    void replaceMulti(List<Replacement<PS, SEG, S>> replacements) {
        content.replaceMulti(replacements);

        // don't update selection as this is not the main method through which the area is updated
        // leave that up to the developer using it to determine what to do
    }

    @Override
    public MultiChangeBuilder<PS, SEG, S> createMultiChange() {
        return new MultiChangeBuilder<>(this);
    }

    @Override
    public MultiChangeBuilder<PS, SEG, S> createMultiChange(int initialNumOfChanges) {
        return new MultiChangeBuilder<>(this, initialNumOfChanges);
    }

    /* ********************************************************************** *
     *                                                                        *
     * Public API                                                             *
     *                                                                        *
     * ********************************************************************** */

    @Override
    public void dispose() {
        if (undoManager != null) {
            undoManager.close();
        }
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

            if(followCaretRequested && ! paging) {
                try (Guard g = viewportDirty.suspend()) {
                    followCaret();
                }
            }
            followCaretRequested = false;
            paging = false;
        });

        Node holder = placeholder;
        if (holder != null && holder.isResizable() && holder.isManaged()) {
            holder.autosize();
        }
    }

    /* ********************************************************************** *
     *                                                                        *
     * Package-Private methods                                                *
     *                                                                        *
     * ********************************************************************** */

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
        int lineIdx = cell.getNode().getCurrentLineIndex(caretSelectionBind.getUnderlyingCaret());
        return paragraphLineNavigator.position(parIdx, lineIdx);
    }

    void showCaretAtBottom() {
        int parIdx = getCurrentParagraph();
        Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell = virtualFlow.getCell(parIdx);
        Bounds caretBounds = cell.getNode().getCaretBounds(caretSelectionBind.getUnderlyingCaret());
        double y = caretBounds.getMaxY();
        suspendVisibleParsWhile(() -> virtualFlow.showAtOffset(parIdx, getViewportHeight() - y));
    }

    void showCaretAtTop() {
        int parIdx = getCurrentParagraph();
        Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell = virtualFlow.getCell(parIdx);
        Bounds caretBounds = cell.getNode().getCaretBounds(caretSelectionBind.getUnderlyingCaret());
        double y = caretBounds.getMinY();
        suspendVisibleParsWhile(() -> virtualFlow.showAtOffset(parIdx, -y));
    }

    /**
     * Returns x coordinate of the caret in the current paragraph.
     */
    final ParagraphBox.CaretOffsetX getCaretOffsetX(CaretNode caret) {
        return getCell(caret.getParagraphIndex()).getCaretOffsetX(caret);
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

    final Optional<Bounds> getSelectionBoundsOnScreen(Selection<PS, SEG, S> selection) {
        if (selection.getLength() == 0) {
            return Optional.empty();
        }

        List<Bounds> bounds = new ArrayList<>(selection.getParagraphSpan());
        for (int i = selection.getStartParagraphIndex(); i <= selection.getEndParagraphIndex(); i++) {
            virtualFlow.getCellIfVisible(i)
                    .ifPresent(c -> c.getNode()
                            .getSelectionBoundsOnScreen(selection)
                            .ifPresent(bounds::add)
                    );
        }

        if(bounds.size() == 0) {
            return Optional.empty();
        }
        double minX = bounds.stream().mapToDouble(Bounds::getMinX).min().getAsDouble();
        double maxX = bounds.stream().mapToDouble(Bounds::getMaxX).max().getAsDouble();
        double minY = bounds.stream().mapToDouble(Bounds::getMinY).min().getAsDouble();
        double maxY = bounds.stream().mapToDouble(Bounds::getMaxY).max().getAsDouble();
        return Optional.of(new BoundingBox(minX, minY, maxX-minX, maxY-minY));
    }

    void clearTargetCaretOffset() {
        caretSelectionBind.clearTargetOffset();
    }

    ParagraphBox.CaretOffsetX getTargetCaretOffset() {
        return caretSelectionBind.getTargetOffset();
    }

    /* ********************************************************************** *
     *                                                                        *
     * Private methods                                                        *
     *                                                                        *
     * ********************************************************************** */

    private Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> createCell(
            Paragraph<PS, SEG, S> paragraph,
            BiConsumer<TextFlow, PS> applyParagraphStyle,
            Function<StyledSegment<SEG, S>, Node> nodeFactory) {

        ParagraphBox<PS, SEG, S> box = new ParagraphBox<>(paragraph, applyParagraphStyle, nodeFactory);

        box.highlightTextFillProperty().bind(highlightTextFill);
        box.wrapTextProperty().bind(wrapTextProperty());
        box.graphicFactoryProperty().bind(paragraphGraphicFactoryProperty());
        box.graphicOffset.bind(virtualFlow.breadthOffsetProperty());

        EventStream<Integer> boxIndexValues = box.indexProperty().values().filter(i -> i != -1);

        Subscription firstParPseudoClass = boxIndexValues.subscribe(idx -> box.pseudoClassStateChanged(FIRST_PAR, idx == 0));
        Subscription lastParPseudoClass = EventStreams.combine(
                boxIndexValues,
                getParagraphs().sizeProperty().values()
        ).subscribe(in -> in.exec((i, n) -> box.pseudoClassStateChanged(LAST_PAR, i == n-1)));

        // set up caret
        Function<CaretNode, Subscription> subscribeToCaret = caret -> {
            EventStream<Integer> caretIndexStream = EventStreams.nonNullValuesOf(caret.paragraphIndexProperty());

            // a new event stream needs to be created for each caret added, so that it will immediately
            // fire the box's current index value as an event, thereby running the code in the subscribe block
            // Reusing boxIndexValues will not fire its most recent event, leading to a caret not being added
            // Thus, we'll call the new event stream "fresh" box index values
            EventStream<Integer> freshBoxIndexValues = box.indexProperty().values().filter(i -> i != -1);

            return EventStreams.combine(caretIndexStream, freshBoxIndexValues)
                    .subscribe(t -> {
                        int caretParagraphIndex = t.get1();
                        int boxIndex = t.get2();
                        if (caretParagraphIndex == boxIndex) {
                            box.caretsProperty().add(caret);
                        } else {
                            box.caretsProperty().remove(caret);
                        }
                    });
        };
        Subscription caretSubscription = caretSet.addSubscriber(subscribeToCaret);

        // TODO: how should 'hasCaret' be handled now?
        Subscription hasCaretPseudoClass = EventStreams
                .combine(boxIndexValues, Val.wrap(currentParagraphProperty()).values())
                // box index (t1) == caret paragraph index (t2)
                .map(t -> t.get1().equals(t.get2()))
                .subscribe(value -> box.pseudoClassStateChanged(HAS_CARET, value));

        Function<Selection<PS, SEG, S>, Subscription> subscribeToSelection = selection -> {
            EventStream<Integer> startParagraphValues = EventStreams.nonNullValuesOf(selection.startParagraphIndexProperty());
            EventStream<Integer> endParagraphValues = EventStreams.nonNullValuesOf(selection.endParagraphIndexProperty());

            // see comment in caret section about why a new box index EventStream is needed
            EventStream<Integer> freshBoxIndexValues = box.indexProperty().values().filter(i -> i != -1);
            return EventStreams.combine(startParagraphValues, endParagraphValues, freshBoxIndexValues)
                    .subscribe(t -> {
                        int startPar = t.get1();
                        int endPar = t.get2();
                        int boxIndex = t.get3();
                        if (startPar <= boxIndex && boxIndex <= endPar) {
                            //   So that we don't add multiple paths for the same selection,
                            //   which leads to not removing the additional paths when selection is removed,
                            // this is a `Map#putIfAbsent(Key, Value)` implementation that creates the path lazily
                            SelectionPath p = box.selectionsProperty().get(selection);
                            if (p == null) {
                                // create & configure path
                                Val<IndexRange> range = Val.create(
                                        () -> boxIndex != -1
                                                ? getParagraphSelection(selection, boxIndex)
                                                : EMPTY_RANGE,
                                        selection.rangeProperty()
                                );
                                SelectionPath path = new SelectionPath(range);
                                path.getStyleClass().add( selection.getSelectionName() );
                                selection.configureSelectionPath(path);

                                box.selectionsProperty().put(selection, path);
                            }
                        } else {
                            box.selectionsProperty().remove(selection);
                        }
                    });
        };
        Subscription selectionSubscription = selectionSet.addSubscriber(subscribeToSelection);

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
                box.highlightTextFillProperty().unbind();
                box.wrapTextProperty().unbind();
                box.graphicFactoryProperty().unbind();
                box.graphicOffset.unbind();
                box.dispose();

                firstParPseudoClass.unsubscribe();
                lastParPseudoClass.unsubscribe();

                caretSubscription.unsubscribe();
                hasCaretPseudoClass.unsubscribe();

                selectionSubscription.unsubscribe();
            }
        };
    }

    /** Assumes this method is called within a {@link #suspendVisibleParsWhile(Runnable)} block */
    private void followCaret() {
        int parIdx = getCurrentParagraph();
        Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell = virtualFlow.getCell(parIdx);
        Bounds caretBounds = cell.getNode().getCaretBounds(caretSelectionBind.getUnderlyingCaret());
        double graphicWidth = cell.getNode().getGraphicPrefWidth();
        Bounds region = extendLeft(caretBounds, graphicWidth);
        virtualFlow.show(parIdx, region);
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

    private Bounds getParagraphBoundsOnScreen(Cell<Paragraph<PS, SEG, S>, ParagraphBox<PS, SEG, S>> cell) {
        Bounds nodeLocal = cell.getNode().getBoundsInLocal();
        Bounds nodeScreen = cell.getNode().localToScreen(nodeLocal);
        Bounds areaLocal = getBoundsInLocal();
        Bounds areaScreen = localToScreen(areaLocal);

        // use area's minX if scrolled right and paragraph's left is not visible
        double minX = nodeScreen.getMinX() < areaScreen.getMinX()
                ? areaScreen.getMinX()
                : nodeScreen.getMinX();
        // use area's minY if scrolled down vertically and paragraph's top is not visible
        double minY = nodeScreen.getMinY() < areaScreen.getMinY()
                ? areaScreen.getMinY()
                : nodeScreen.getMinY();
        // use area's width whether paragraph spans outside of it or not
        // so that short or long paragraph takes up the entire space
        double width = areaScreen.getWidth();
        // use area's maxY if scrolled up vertically and paragraph's bottom is not visible
        double maxY = nodeScreen.getMaxY() < areaScreen.getMaxY()
                ? nodeScreen.getMaxY()
                : areaScreen.getMaxY();
        return new BoundingBox(minX, minY, width, maxY - minY);
    }

    private Optional<Bounds> getRangeBoundsOnScreen(int paragraphIndex, int from, int to) {
        return virtualFlow.getCellIfVisible(paragraphIndex)
                .map(c -> c.getNode().getRangeBoundsOnScreen(from, to));
    }

    private void manageSubscription(Subscription subscription) {
        subscriptions = subscriptions.and(subscription);
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

    private void suspendVisibleParsWhile(Runnable runnable) {
        Suspendable.combine(beingUpdated, visibleParagraphs).suspendWhile(runnable);
    }

    /* ********************************************************************** *
     *                                                                        *
     * CSS                                                                    *
     *                                                                        *
     * ********************************************************************** */

    private static final CssMetaData<GenericStyledArea<?, ?, ?>, Paint> HIGHLIGHT_TEXT_FILL = new CustomCssMetaData<>(
            "-fx-highlight-text-fill", StyleConverter.getPaintConverter(), Color.WHITE, s -> s.highlightTextFill
    );

    private static final List<CssMetaData<? extends Styleable, ?>> CSS_META_DATA_LIST;

    static {
        List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Region.getClassCssMetaData());

        styleables.add(HIGHLIGHT_TEXT_FILL);

        CSS_META_DATA_LIST = Collections.unmodifiableList(styleables);
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return CSS_META_DATA_LIST;
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS_META_DATA_LIST;
    }

}
