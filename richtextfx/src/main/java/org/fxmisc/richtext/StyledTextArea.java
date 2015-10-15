package org.fxmisc.richtext;

import static org.fxmisc.richtext.PopupAlignment.*;
import static org.fxmisc.richtext.TwoDimensional.Bias.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Skin;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.PopupWindow;

import org.fxmisc.richtext.CssProperties.EditableProperty;
import org.fxmisc.richtext.CssProperties.FontProperty;
import org.fxmisc.richtext.skin.StyledTextAreaBehavior;
import org.fxmisc.richtext.skin.StyledTextAreaVisual;
import org.fxmisc.richtext.skin.TextExt;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.UndoManagerFactory;
import org.fxmisc.wellbehaved.skin.Skins;
import org.reactfx.EventStream;
import org.reactfx.Guard;
import org.reactfx.Suspendable;
import org.reactfx.SuspendableEventStream;
import org.reactfx.SuspendableNo;
import org.reactfx.collection.LiveList;
import org.reactfx.collection.SuspendableList;
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
public class StyledTextArea<S, PS> extends Control
implements
        TextEditingArea<S, PS>,
        EditActions<S, PS>,
        ClipboardActions<S, PS>,
        NavigationActions<S, PS>,
        UndoActions<S>,
        TwoDimensional {

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

    // font property
    /**
     * The default font to use where font is not specified otherwise.
     */
    private final StyleableObjectProperty<Font> font = new FontProperty<>(this);
    public final StyleableObjectProperty<Font> fontProperty() { return font; }
    public final void setFont(Font value) { font.setValue(value); }
    public final Font getFont() { return font.getValue(); }

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

    private final ObjectProperty<IntFunction<? extends Node>> paragraphGraphicFactory = new SimpleObjectProperty<>(null);
    public void setParagraphGraphicFactory(IntFunction<? extends Node> factory) { paragraphGraphicFactory.set(factory); }
    public IntFunction<? extends Node> getParagraphGraphicFactory() { return paragraphGraphicFactory.get(); }
    public ObjectProperty<IntFunction<? extends Node>> paragraphGraphicFactoryProperty() { return paragraphGraphicFactory; }

    /**
     * Indicates whether the initial style should also be used for plain text
     * inserted into this text area. When {@code false}, the style immediately
     * preceding the insertion position is used. Default value is {@code false}.
     */
    public void setUseInitialStyleForInsertion(boolean value) { content.useInitialStyleForInsertion.set(value); }
    public boolean getUseInitialStyleForInsertion() { return content.useInitialStyleForInsertion.get(); }
    public BooleanProperty useInitialStyleForInsertionProperty() { return content.useInitialStyleForInsertion; }

    private Optional<Codec<S>> styleCodec = Optional.empty();
    /**
     * Sets the codec to encode/decode style information to/from binary format.
     * Providing a codec enables clipboard actions to retain the style information.
     */
    public void setStyleCodec(Codec<S> codec) {
        styleCodec = Optional.of(codec);
    }
    @Override
    public Optional<Codec<S>> getStyleCodec() {
        return styleCodec;
    }


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

    private Position selectionStart2D;
    private Position selectionEnd2D;

    /**
     * content model
     */
    private final EditableStyledDocument<S, PS> content;

    /**
     * Style used by default when no other style is provided.
     */
    private final S initialStyle;

    /**
     * Style used by default when no other style is provided.
     */
    private final PS initialParagraphStyle;

    /**
     * Style applicator used by the default skin.
     */
    private final BiConsumer<? super TextExt, S> applyStyle;

    /**
     * Style applicator used by the default skin.
     */
    private final BiConsumer<TextFlow, PS> applyParagraphStyle;

    /**
     * Indicates whether style should be preserved on undo/redo,
     * copy/paste and text move.
     * TODO: Currently, only undo/redo respect this flag.
     */
    private final boolean preserveStyle;

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
    public StyledTextArea(S initialStyle, BiConsumer<? super TextExt, S> applyStyle, PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle) {
        this(initialStyle, applyStyle, initialParagraphStyle, applyParagraphStyle, true);
    }

    public <C> StyledTextArea(S initialStyle, BiConsumer<? super TextExt, S> applyStyle,
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
            boolean preserveStyle) {
        this.initialStyle = initialStyle;
        this.initialParagraphStyle = initialParagraphStyle;
        this.applyStyle = applyStyle;
        this.applyParagraphStyle = applyParagraphStyle;
        this.preserveStyle = preserveStyle;
        content = new EditableStyledDocument<>(initialStyle, initialParagraphStyle);
        paragraphs = LiveList.suspendable(content.getParagraphs());

        text = Val.suspendable(content.textProperty());
        length = Val.suspendable(content.lengthProperty());
        plainTextChanges = content.plainTextChanges().pausable();
        richTextChanges = content.richChanges().pausable();

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
    }


    /* ********************************************************************** *
     *                                                                        *
     * Queries                                                                *
     *                                                                        *
     * Queries are parameterized observables.                                 *
     *                                                                        *
     * ********************************************************************** */

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
        if (paragraph >= 0 && paragraph < paragraphs.size()) {
            paragraphs.set(paragraph, paragraphs.get(paragraph).setParagraphStyle(paragraphStyle));
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
     * Look &amp; feel                                                        *
     *                                                                        *
     * ********************************************************************** */

    @Override
    protected Skin<?> createDefaultSkin() {
        return Skins.<StyledTextArea<S, PS>, StyledTextAreaVisual<S, PS>>createSimpleSkin(
                this,
                area -> new StyledTextAreaVisual<>(area, applyStyle, initialParagraphStyle, applyParagraphStyle),
                StyledTextAreaBehavior::new);
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        List<CssMetaData<? extends Styleable, ?>> superMetaData = super.getControlCssMetaData();
        List<CssMetaData<? extends Styleable, ?>> myMetaData = Arrays.<CssMetaData<? extends Styleable, ?>>asList(
                font.getCssMetaData());
        List<CssMetaData<? extends Styleable, ?>> res = new ArrayList<>(superMetaData.size() + myMetaData.size());
        res.addAll(superMetaData);
        res.addAll(myMetaData);
        return res;
    }


    /* ********************************************************************** *
     *                                                                        *
     * Private methods                                                        *
     *                                                                        *
     * ********************************************************************** */

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
