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

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Binding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
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
import javafx.stage.PopupWindow;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.richtext.CssProperties.EditableProperty;
import org.fxmisc.richtext.CssProperties.FontProperty;
import org.fxmisc.richtext.skin.StyledTextAreaBehavior;
import org.fxmisc.richtext.skin.StyledTextAreaVisual;
import org.fxmisc.richtext.util.skin.Skins;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.UndoManagerFactory;
import org.reactfx.EventStream;
import org.reactfx.Guard;
import org.reactfx.Guardian;
import org.reactfx.Indicator;
import org.reactfx.InterceptableEventStream;
import org.reactfx.inhibeans.collection.Collections;
import org.reactfx.inhibeans.collection.ObservableList;

import com.sun.javafx.Utils;

/**
 * Text editing control. Accepts user input (keyboard, mouse) and
 * provides API to assign style to text ranges. It is suitable for
 * syntax highlighting and rich-text editors.
 *
 * <p>Subclassing is allowed to define the type of style, e.g. inline
 * style or style classes.</p>
 *
 * @param <S> type of style that can be applied to text.
 */
public class StyledTextArea<S> extends Control
implements
        TextEditingArea<S>,
        EditActions<S>,
        ClipboardActions<S>,
        NavigationActions<S>,
        UndoActions<S>,
        TwoDimensional {

    /**
     * Index range [0, 0).
     */
    public static final IndexRange EMPTY_RANGE = new IndexRange(0, 0);


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

    private final ObjectProperty<PopupWindow> popupWindow = new SimpleObjectProperty<>();
    public void setPopupWindow(PopupWindow popup) { popupWindow.set(popup); }
    public PopupWindow getPopupWindow() { return popupWindow.get(); }
    public ObjectProperty<PopupWindow> popupWindowProperty() { return popupWindow; }

    @Deprecated
    public void setPopupAtCaret(PopupWindow popup) { popupWindow.set(popup); }
    @Deprecated
    public PopupWindow getPopupAtCaret() { return popupWindow.get(); }
    @Deprecated
    public ObjectProperty<PopupWindow> popupAtCaretProperty() { return popupWindow; }

    private final ObjectProperty<Point2D> popupAnchorOffset = new SimpleObjectProperty<>();
    public void setPopupAnchorOffset(Point2D offset) { popupAnchorOffset.set(offset); }
    public Point2D getPopupAnchorOffset() { return popupAnchorOffset.get(); }
    public ObjectProperty<Point2D> popupAnchorOffsetProperty() { return popupAnchorOffset; }

    private final ObjectProperty<UnaryOperator<Point2D>> popupAnchorAdjustment = new SimpleObjectProperty<>();
    public void setPopupAnchorAdjustment(UnaryOperator<Point2D> f) { popupAnchorAdjustment.set(f); }
    public UnaryOperator<Point2D> getPopupAnchorAdjustment() { return popupAnchorAdjustment.get(); }
    public ObjectProperty<UnaryOperator<Point2D>> popupAnchorAdjustmentProperty() { return popupAnchorAdjustment; }

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
    private final org.reactfx.inhibeans.binding.Binding<String> text;
    @Override public final String getText() { return text.getValue(); }
    @Override public final ObservableValue<String> textProperty() { return text; }

    // rich text
    @Override public final StyledDocument<S> getDocument() { return content.snapshot(); };

    // length
    private final org.reactfx.inhibeans.binding.IntegerBinding length;
    @Override public final int getLength() { return length.get(); }
    @Override public final ObservableIntegerValue lengthProperty() { return length; }

    // caret position
    private final IntegerProperty internalCaretPosition = new SimpleIntegerProperty(0);
    private final org.reactfx.inhibeans.binding.IntegerBinding caretPosition =
            org.reactfx.inhibeans.binding.IntegerBinding.wrap(internalCaretPosition);
    @Override public final int getCaretPosition() { return caretPosition.get(); }
    @Override public final ObservableIntegerValue caretPositionProperty() { return caretPosition; }

    // selection anchor
    private final org.reactfx.inhibeans.property.SimpleIntegerProperty anchor =
            new org.reactfx.inhibeans.property.SimpleIntegerProperty(0);
    @Override public final int getAnchor() { return anchor.get(); }
    @Override public final ObservableIntegerValue anchorProperty() { return anchor; }

    // selection
    private final ObjectProperty<IndexRange> internalSelection = new SimpleObjectProperty<>(EMPTY_RANGE);
    private final org.reactfx.inhibeans.binding.ObjectBinding<IndexRange> selection =
            org.reactfx.inhibeans.binding.ObjectBinding.wrap(internalSelection);
    @Override public final IndexRange getSelection() { return selection.getValue(); }
    @Override public final ObservableValue<IndexRange> selectionProperty() { return selection; }

    // selected text
    private final org.reactfx.inhibeans.binding.StringBinding selectedText;
    @Override public final String getSelectedText() { return selectedText.get(); }
    @Override public final ObservableStringValue selectedTextProperty() { return selectedText; }

    // current paragraph index
    private final org.reactfx.inhibeans.binding.IntegerBinding currentParagraph;
    @Override public final int getCurrentParagraph() { return currentParagraph.get(); }
    @Override public final ObservableIntegerValue currentParagraphProperty() { return currentParagraph; }

    // caret column
    private final org.reactfx.inhibeans.binding.IntegerBinding caretColumn;
    @Override public final int getCaretColumn() { return caretColumn.get(); }
    @Override public final ObservableIntegerValue caretColumnProperty() { return caretColumn; }

    // paragraphs
    private final ObservableList<Paragraph<S>> paragraphs;
    @Override public ObservableList<Paragraph<S>> getParagraphs() {
        return paragraphs;
    }

    // beingUpdated
    private final Indicator beingUpdated = new Indicator();
    public Indicator beingUpdatedProperty() { return beingUpdated; }
    public boolean isBeingUpdated() { return beingUpdated.isOn(); }


    /* ********************************************************************** *
     *                                                                        *
     * Event streams                                                          *
     *                                                                        *
     * ********************************************************************** */

    // text changes
    private final InterceptableEventStream<PlainTextChange> plainTextChanges;
    @Override
    public final EventStream<PlainTextChange> plainTextChanges() { return plainTextChanges; }

    // rich text changes
    private final InterceptableEventStream<RichTextChange<S>> richTextChanges;
    @Override
    public final EventStream<RichTextChange<S>> richChanges() { return richTextChanges; }


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
    private final EditableStyledDocument<S> content;

    /**
     * Style used by default when no other style is provided.
     */
    private final S initialStyle;

    /**
     * Style applicator used by the default skin.
     */
    private final BiConsumer<Text, S> applyStyle;

    /**
     * Indicates whether style should be preserved on undo/redo,
     * copy/paste and text move.
     * TODO: Currently, only undo/redo respect this flag.
     */
    private final boolean preserveStyle;

    private final Guardian omniGuardian;


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
     */
    public StyledTextArea(S initialStyle, BiConsumer<Text, S> applyStyle) {
        this(initialStyle, applyStyle, true);
    }

    public <C> StyledTextArea(S initialStyle, BiConsumer<Text, S> applyStyle,
            boolean preserveStyle) {
        this.initialStyle = initialStyle;
        this.applyStyle = applyStyle;
        this.preserveStyle = preserveStyle;
        content = new EditableStyledDocument<>(initialStyle);
        paragraphs = Collections.wrap(content.getParagraphs());

        text = org.reactfx.inhibeans.binding.Binding.wrap(content.textProperty());
        length = org.reactfx.inhibeans.binding.IntegerBinding.wrap(content.lengthProperty());
        plainTextChanges = content.plainTextChanges().interceptable();
        richTextChanges = content.richChanges().interceptable();

        undoManager = preserveStyle
                ? createRichUndoManager(UndoManagerFactory.unlimitedHistoryFactory())
                : createPlainUndoManager(UndoManagerFactory.unlimitedHistoryFactory());

        Binding<Position> caretPosition2D = EasyBind.map(internalCaretPosition,
                p -> content.offsetToPosition(p.intValue(), Forward));
        paragraphs.addListener((InvalidationListener) (obs -> caretPosition2D.invalidate()));

        currentParagraph = org.reactfx.inhibeans.binding.IntegerBinding.wrap(
                EasyBind.map(caretPosition2D, p -> p.getMajor()));
        caretColumn = org.reactfx.inhibeans.binding.IntegerBinding.wrap(
                EasyBind.map(caretPosition2D, p -> p.getMinor()));

        selectionStart2D = position(0, 0);
        selectionEnd2D = position(0, 0);
        internalSelection.addListener(obs -> {
            IndexRange sel = internalSelection.get();
            selectionStart2D = offsetToPosition(sel.getStart(), Forward);
            selectionEnd2D = sel.getLength() == 0
                    ? selectionStart2D
                    : selectionStart2D.offsetBy(sel.getLength(), Backward);
        });

        selectedText = new org.reactfx.inhibeans.binding.StringBinding() {
            { bind(internalSelection, content.textProperty()); }
            @Override protected String computeValue() {
                return content.getText(internalSelection.get());
            }
        };

        omniGuardian = Guardian.combine(
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
                plainTextChanges::pause,
                richTextChanges::pause,

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

    public Paragraph<S> getParagraph(int index) {
        return paragraphs.get(index);
    }

    @Override
    public StyledDocument<S> subDocument(int start, int end) {
        return content.subSequence(start, end);
    }

    @Override
    public StyledDocument<S> subDocument(int paragraphIndex) {
        return content.subDocument(paragraphIndex);
    }

    /**
     * Returns the selection range in the given paragraph.
     */
    public IndexRange getParagraphSelection(int paragraph) {
        if(isBeingUpdated()) {
            // internalSelection, and hence selectionStart2D and selectionEnd2D
            // might not have been updated yet. This can happen if this method
            // is called from paragraphs' change listener.
            clampInternalSelection();
        }

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
        try(Guard g = omniGuardian.guard()) {
            content.setStyle(from, to, style);
        }
    }

    /**
     * Sets style for the whole paragraph.
     */
    public void setStyle(int paragraph, S style) {
        try(Guard g = omniGuardian.guard()) {
            content.setStyle(paragraph, style);
        }
    }

    /**
     * Sets style for the given range relative in the given paragraph.
     */
    public void setStyle(int paragraph, int from, int to, S style) {
        try(Guard g = omniGuardian.guard()) {
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
        try(Guard g = omniGuardian.guard()) {
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
        try(Guard g = omniGuardian.guard()) {
            content.setStyleSpans(paragraph, from, styleSpans);
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

    @Override
    public void replaceText(int start, int end, String text) {
        text = filterInput(text);
        try(Guard g = omniGuardian.guard()) {
            start = Utils.clamp(0, start, getLength());
            end = Utils.clamp(0, end, getLength());

            content.replaceText(start, end, text);

            int newCaretPos = start + text.length();
            selectRange(newCaretPos, newCaretPos);
        }
    }

    @Override
    public void replace(int start, int end, StyledDocument<S> replacement) {
        try(Guard g = omniGuardian.guard()) {
            start = Utils.clamp(0, start, getLength());
            end = Utils.clamp(0, end, getLength());

            content.replace(start, end, replacement);

            int newCaretPos = start + replacement.length();
            selectRange(newCaretPos, newCaretPos);
        }
    }

    @Override
    public void selectRange(int anchor, int caretPosition) {
        try(Guard g = guard(this.caretPosition, currentParagraph, caretColumn, this.anchor, selection, selectedText)) {
            this.internalCaretPosition.set(Utils.clamp(0, caretPosition, getLength()));
            this.anchor.set(Utils.clamp(0, anchor, getLength()));
            this.internalSelection.set(IndexRange.normalize(getAnchor(), getCaretPosition()));
        }
    }

    @Override
    public void positionCaret(int pos) {
        try(Guard g = guard(caretPosition, currentParagraph, caretColumn)) {
            internalCaretPosition.set(pos);
        }
    }


    /* ********************************************************************** *
     *                                                                        *
     * Look &amp; feel                                                        *
     *                                                                        *
     * ********************************************************************** */

    @Override
    protected Skin<?> createDefaultSkin() {
        return Skins.createSimpleSkin(
                this,
                area -> new StyledTextAreaVisual<>(area, applyStyle),
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

    /**
     * Filters out illegal characters.
     */
    private static String filterInput(String txt) {
        if(txt.chars().allMatch(c -> isLegal((char) c))) {
            return txt;
        } else {
            StringBuilder sb = new StringBuilder(txt.length());
            txt.chars().filter(c -> isLegal((char) c)).forEach(c -> sb.append((char) c));
            return sb.toString();
        }
    }

    private static boolean isLegal(char c) {
        return !Character.isISOControl(c)
                || LineTerminator.isLineTerminatorChar(c)
                || c == '\t';
    }

    private UndoManager createPlainUndoManager(UndoManagerFactory factory) {
        Consumer<PlainTextChange> apply = change -> replaceText(change.getPosition(), change.getPosition() + change.getRemoved().length(), change.getInserted());
        Consumer<PlainTextChange> undo = change -> replaceText(change.getPosition(), change.getPosition() + change.getInserted().length(), change.getRemoved());
        BiFunction<PlainTextChange, PlainTextChange, Optional<PlainTextChange>> merge = (change1, change2) -> change1.mergeWith(change2);
        return factory.create(plainTextChanges(), apply, undo, merge);
    }

    private UndoManager createRichUndoManager(UndoManagerFactory factory) {
        Consumer<RichTextChange<S>> apply = change -> replace(change.getPosition(), change.getPosition() + change.getRemoved().length(), change.getInserted());
        Consumer<RichTextChange<S>> undo = change -> replace(change.getPosition(), change.getPosition() + change.getInserted().length(), change.getRemoved());
        BiFunction<RichTextChange<S>, RichTextChange<S>, Optional<RichTextChange<S>>> merge = (change1, change2) -> change1.mergeWith(change2);
        return factory.create(richChanges(), apply, undo, merge);
    }

    private Guard guard(Guardian... guardians) {
        return Guardian.combine(beingUpdated, Guardian.combine(guardians)).guard();
    }

    /**
     * Ensures that internalSelection (and hence selection, selectionStart2D,
     * selectionEnd2D) is within current document bounds.
     */
    private void clampInternalSelection() {
        int l = getLength();
        IndexRange sel = internalSelection.get();
        int selEnd = Math.min(sel.getEnd(), l);

        // set internalSelection only if changed to avoid unnecessary invalidations
        if(selEnd != sel.getEnd()) {
            int selStart = Math.min(sel.getStart(), l);
            internalSelection.set(new IndexRange(selStart, selEnd));
        }
    }
}
