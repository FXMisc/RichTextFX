package org.fxmisc.richtext.model;

import static org.fxmisc.richtext.util.Utilities.clamp;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.IndexRange;

import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.UndoManagerFactory;
import org.reactfx.EventStream;
import org.reactfx.Guard;
import org.reactfx.Subscription;
import org.reactfx.Suspendable;
import org.reactfx.SuspendableEventStream;
import org.reactfx.SuspendableNo;
import org.reactfx.collection.LiveList;
import org.reactfx.collection.SuspendableList;
import org.reactfx.value.SuspendableVal;
import org.reactfx.value.Val;

/**
 * Model for {@link org.fxmisc.richtext.GenericStyledArea}
 *
 * @param <S> type of style that can be applied to text.
 * @param <PS> type of style that can be applied to Paragraph
 */
public class StyledTextAreaModel<PS, SEG, S>
        implements
        EditActions<PS, SEG, S>,
        NavigationActions<PS, SEG, S>,
        UndoActions,
        TwoDimensional {

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

    // undo manager
    private UndoManager undoManager;
    @Override public UndoManager getUndoManager() { return undoManager; }
    @Override public void setUndoManager(UndoManagerFactory undoManagerFactory) {
        undoManager.close();
        undoManager = preserveStyle
                ? createRichUndoManager(undoManagerFactory)
                : createPlainUndoManager(undoManagerFactory);
    }

    /**
     * Indicates whether the initial style should also be used for plain text
     * inserted into this text area. When {@code false}, the style immediately
     * preceding the insertion position is used. Default value is {@code false}.
     */
    final BooleanProperty useInitialStyleForInsertion = new SimpleBooleanProperty();
    public BooleanProperty useInitialStyleForInsertionProperty() { return useInitialStyleForInsertion; }
    public void setUseInitialStyleForInsertion(boolean value) { useInitialStyleForInsertion.set(value); }
    public boolean getUseInitialStyleForInsertion() { return useInitialStyleForInsertion.get(); }

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
    @Override public final StyledDocument<PS, SEG, S> getDocument() { return content.snapshot(); }

    // length
    private final SuspendableVal<Integer> length;
    @Override public final int getLength() { return length.getValue(); }
    @Override public final ObservableValue<Integer> lengthProperty() { return length; }

    private final CaretSelectionModel mainCaret;

    // caret position
    @Override public final int getCaretPosition() { return mainCaret.getCaretPosition(); }
    @Override public final ObservableValue<Integer> caretPositionProperty() { return mainCaret.caretPositionProperty(); }

    // selection anchor
    @Override public final int getAnchor() { return mainCaret.getAnchor(); }
    @Override public final ObservableValue<Integer> anchorProperty() { return mainCaret.anchorProperty(); }

    // selection
    @Override public final IndexRange getSelection() { return mainCaret.getSelection(); }
    @Override public final ObservableValue<IndexRange> selectionProperty() { return mainCaret.selectionProperty(); }

    // selected text
    @Override public final String getSelectedText() { return mainCaret.getSelectedText(); }
    @Override public final ObservableValue<String> selectedTextProperty() { return mainCaret.selectedTextProperty(); }

    // caret paragraph
    @Override public final int getCaretParagraph() { return mainCaret.getCaretParagraph(); }
    @Override public final ObservableValue<Integer> caretParagraphProperty() { return mainCaret.caretParagraphProperty(); }

    // caret column
    @Override public final int getCaretColumn() { return mainCaret.getCaretColumn(); }
    @Override public final ObservableValue<Integer> caretColumnProperty() { return mainCaret.caretColumnProperty(); }

    // paragraphs
    private final SuspendableList<Paragraph<PS, SEG, S>> paragraphs;
    @Override public LiveList<Paragraph<PS, SEG, S>> getParagraphs() { return paragraphs; }

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
    private final SuspendableEventStream<RichTextChange<PS, SEG, S>> richTextChanges;
    @Override
    public final EventStream<RichTextChange<PS, SEG, S>> richChanges() { return richTextChanges; }

    /* ********************************************************************** *
     *                                                                        *
     * Private & Package-Private fields                                       *
     *                                                                        *
     * ********************************************************************** */

    private final TextOps<SEG, S> textOps;

    private Subscription subscriptions = () -> {};

    /**
     * content model
     */
    private final EditableStyledDocument<PS, SEG, S> content;

    /**
     * Usually used to create another area (View) that shares
     * the same document (Model).
     * @return this area's {@link EditableStyledDocument}
     */
    public final EditableStyledDocument<PS, SEG, S> getContent() { return content; }

    /**
     * Style used by default when no other style is provided.
     */
    private final S initialTextStyle;
    public final S getInitialTextStyle() { return initialTextStyle; }

    /**
     * Style used by default when no other style is provided.
     */
    private final PS initialParagraphStyle;
    public final PS getInitialParagraphStyle() { return initialParagraphStyle; }

    /**
     * Indicates whether style should be preserved on undo/redo,
     * copy/paste and text move.
     * TODO: Currently, only undo/redo respect this flag.
     */
    private final boolean preserveStyle;
    public final boolean isPreserveStyle() { return preserveStyle; }


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
     * @param initialParagraphStyle style to use in places where no other style is
     * specified (yet).
     */
    public StyledTextAreaModel(PS initialParagraphStyle, S initialTextStyle, TextOps<SEG, S> segmentOps) {
        this(initialParagraphStyle, initialTextStyle, segmentOps, true);
    }

    public StyledTextAreaModel(PS initialParagraphStyle, S initialTextStyle, TextOps<SEG, S> segmentOps, boolean preserveStyle
    ) {
        this(initialParagraphStyle, initialTextStyle,
                new GenericEditableStyledDocumentBase<>(initialParagraphStyle, initialTextStyle, segmentOps),
                segmentOps, preserveStyle);
    }

    /**
     * The same as {@link #StyledTextAreaModel(Object, Object, TextOps)} except that
     * this constructor can be used to create another {@code StyledTextArea} object that
     * shares the same {@link EditableStyledDocument}.
     */
    public StyledTextAreaModel(PS initialParagraphStyle, S initialTextStyle,
                               EditableStyledDocument<PS, SEG, S> document, TextOps<SEG, S> textOps
    ) {
        this(initialParagraphStyle, initialTextStyle, document, textOps, true);
    }

    public StyledTextAreaModel(
            PS initialParagraphStyle,
            S initialTextStyle,
            EditableStyledDocument<PS, SEG, S> document,
            TextOps<SEG, S> textOps,
            boolean preserveStyle
    ) {
        this.textOps = textOps;
        this.initialTextStyle = initialTextStyle;
        this.initialParagraphStyle = initialParagraphStyle;
        this.preserveStyle = preserveStyle;

        content = document;
        paragraphs = LiveList.suspendable(content.getParagraphs());

        text = Val.suspendable(content.textProperty());
        length = Val.suspendable(content.lengthProperty());
        plainTextChanges = content.plainChanges().pausable();
        richTextChanges = content.richChanges().pausable();

        mainCaret = new CaretSelectionModel(this, () -> beingUpdated);
        manageSubscription(mainCaret::dispose);

        undoManager = preserveStyle
                ? createRichUndoManager(UndoManagerFactory.unlimitedHistoryFactory())
                : createPlainUndoManager(UndoManagerFactory.unlimitedHistoryFactory());

        final Suspendable omniSuspendable = Suspendable.combine(
                beingUpdated, // must be first, to be the last one to release
                text,
                length,
                mainCaret.omniSuspendable(),

                // add streams after properties, to be released before them
                plainTextChanges,
                richTextChanges,

                // paragraphs to be released first
                paragraphs);
        manageSubscription(omniSuspendable.suspendWhen(content.beingUpdatedProperty()));
    }


    /* ********************************************************************** *
     *                                                                        *
     * Queries                                                                *
     *                                                                        *
     * Queries are parameterized observables.                                 *
     *                                                                        *
     * ********************************************************************** */

    final String getText(IndexRange range) {
        return content.getText(range);
    }

    @Override
    public final String getText(int start, int end) {
        return content.getText(start, end);
    }

    @Override
    public String getText(int paragraph) {
        return paragraphs.get(paragraph).getText();
    }

    public Paragraph<PS, SEG, S> getParagraph(int index) {
        return paragraphs.get(index);
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
        return mainCaret.getParagraphSelection(paragraph);
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

    /**
     * Sets style for the given character range.
     */
    public void setStyle(int from, int to, S style) {
        content.setStyle(from, to, style);
    }

    /**
     * Sets style for the whole paragraph.
     */
    public void setStyle(int paragraph, S style) {
        content.setStyle(paragraph, style);
    }

    /**
     * Sets style for the given range relative in the given paragraph.
     */
    public void setStyle(int paragraph, int from, int to, S style) {
        content.setStyle(paragraph, from, to, style);
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
        content.setStyleSpans(from, styleSpans);
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
        content.setStyleSpans(paragraph, from, styleSpans);
    }

    /**
     * Sets style for the whole paragraph.
     */
    public void setParagraphStyle(int paragraph, PS paragraphStyle) {
        content.setParagraphStyle(paragraph, paragraphStyle);
    }

    /**
     * Resets the style of the given range to the initial style.
     */
    public void clearStyle(int from, int to) {
        setStyle(from, to, initialTextStyle);
    }

    /**
     * Resets the style of the given paragraph to the initial style.
     */
    public void clearStyle(int paragraph) {
        setStyle(paragraph, initialTextStyle);
    }

    /**
     * Resets the style of the given range in the given paragraph
     * to the initial style.
     */
    public void clearStyle(int paragraph, int from, int to) {
        setStyle(paragraph, from, to, initialTextStyle);
    }

    /**
     * Resets the style of the given paragraph to the initial style.
     */
    public void clearParagraphStyle(int paragraph) {
        setParagraphStyle(paragraph, initialParagraphStyle);
    }

    @Override
    public void replaceText(int start, int end, String text) {
        StyledDocument<PS, SEG, S> doc = ReadOnlyStyledDocument.fromString(
                text, getParagraphStyleForInsertionAt(start), getStyleForInsertionAt(start), textOps);
        replace(start, end, doc);
    }

    @Override
    public void replace(int start, int end, StyledDocument<PS, SEG, S> replacement) {
        try (Guard g = content.beingUpdatedProperty().suspend()) {
            start = clamp(0, start, getLength());
            end = clamp(0, end, getLength());

            content.replace(start, end, replacement);

            int newCaretPos = start + replacement.length();
            selectRange(newCaretPos, newCaretPos);
        }
    }

    @Override
    public void selectRange(int anchor, int caretPosition) {
        mainCaret.selectRange(anchor, caretPosition);
    }

    /**
     * Positions only the caret. Doesn't move the anchor and doesn't change
     * the selection. Can be used to achieve the special case of positioning
     * the caret outside or inside the selection, as opposed to always being
     * at the boundary. Use with care.
     */
    public void positionCaret(int pos) {
        mainCaret.positionCaret(pos);
    }

    /* ********************************************************************** *
     *                                                                        *
     * Public API                                                             *
     *                                                                        *
     * ********************************************************************** */

    public void dispose() {
        subscriptions.unsubscribe();
    }

    /* ********************************************************************** *
     *                                                                        *
     * Private methods                                                        *
     *                                                                        *
     * ********************************************************************** */

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

    private void manageSubscription(Subscription subscription) {
        subscriptions = subscriptions.and(subscription);
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

}
