package org.fxmisc.richtext.model;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.*;

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
import org.reactfx.value.SuspendableVar;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

/**
 * Model for {@link org.fxmisc.richtext.StyledTextArea}
 *
 * @param <S> type of style that can be applied to text.
 * @param <PS> type of style that can be applied to Paragraph
 */
public class StyledTextAreaModel<PS, S>
        implements
        EditActions<PS, S>,
        NavigationActions<PS, S>,
        UndoActions,
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
    @Override public final StyledDocument<PS, S> getDocument() { return content.snapshot(); }

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
    private final SuspendableList<Paragraph<PS, S>> paragraphs;
    @Override public LiveList<Paragraph<PS, S>> getParagraphs() { return paragraphs; }

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
    private final SuspendableEventStream<RichTextChange<PS, S>> richTextChanges;
    @Override
    public final EventStream<RichTextChange<PS, S>> richChanges() { return richTextChanges; }

    /* ********************************************************************** *
     *                                                                        *
     * Private & Package-Private fields                                       *
     *                                                                        *
     * ********************************************************************** */

    private Subscription subscriptions = () -> {};

    private Position selectionStart2D;
    private Position selectionEnd2D;

    /**
     * content model
     */
    private final EditableStyledDocument<PS, S> content;

    /**
     * Usually used to create another area (View) that shares
     * the same document (Model).
     * @return this area's {@link EditableStyledDocument}
     */
    public final EditableStyledDocument<PS, S> getContent() { return content; }

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
    public StyledTextAreaModel(PS initialParagraphStyle, S initialTextStyle) {
        this(initialParagraphStyle, initialTextStyle, true);
    }

    public StyledTextAreaModel(PS initialParagraphStyle, S initialTextStyle, boolean preserveStyle
    ) {
        this(initialParagraphStyle, initialTextStyle,
                new SimpleEditableStyledDocument<>(initialParagraphStyle, initialTextStyle), preserveStyle);
    }

    /**
     * The same as {@link #StyledTextAreaModel(Object, Object)} except that
     * this constructor can be used to create another {@code StyledTextArea} object that
     * shares the same {@link EditableStyledDocument}.
     */
    public StyledTextAreaModel(PS initialParagraphStyle, S initialTextStyle,
                               EditableStyledDocument<PS, S> document
    ) {
        this(initialParagraphStyle, initialTextStyle, document, true);
    }

    public StyledTextAreaModel(PS initialParagraphStyle, S initialTextStyle,
                               EditableStyledDocument<PS, S> document, boolean preserveStyle
    ) {
        this.initialTextStyle = initialTextStyle;
        this.initialParagraphStyle = initialParagraphStyle;
        this.preserveStyle = preserveStyle;

        content = document;
        paragraphs = LiveList.suspendable(content.getParagraphs());

        text = Val.suspendable(content.textProperty());
        length = Val.suspendable(content.lengthProperty());
        plainTextChanges = content.plainChanges().pausable();
        richTextChanges = content.richChanges().pausable();

        // when content is updated by an area, update the caret
        // and selection ranges of all the other
        // clones that also share this document
        subscribeTo(content.plainChanges(), plainTextChange -> {
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

        final Suspendable omniSuspendable = Suspendable.combine(
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
        manageSubscription(omniSuspendable.suspendWhen(content.beingUpdatedProperty()));
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
        return paragraphs.get(paragraph).getText();
    }

    public Paragraph<PS, S> getParagraph(int index) {
        return paragraphs.get(index);
    }

    @Override
    public StyledDocument<PS, S> subDocument(int start, int end) {
        return content.subSequence(start, end);
    }

    @Override
    public StyledDocument<PS, S> subDocument(int paragraphIndex) {
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
        StyledDocument<PS, S> doc = ReadOnlyStyledDocument.fromString(
                text, getParagraphStyleForInsertionAt(start), getStyleForInsertionAt(start));
        replace(start, end, doc);
    }

    @Override
    public void replace(int start, int end, StyledDocument<PS, S> replacement) {
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

    private <T> void subscribeTo(EventStream<T> src, Consumer<T> consumer) {
        manageSubscription(src.subscribe(consumer));
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
        Consumer<RichTextChange<PS, S>> apply = change -> replace(change.getPosition(), change.getPosition() + change.getRemoved().length(), change.getInserted());
        BiFunction<RichTextChange<PS, S>, RichTextChange<PS, S>, Optional<RichTextChange<PS, S>>> merge = RichTextChange<PS, S>::mergeWith;
        return factory.create(richChanges(), RichTextChange::invert, apply, merge);
    }

    private Guard suspend(Suspendable... suspendables) {
        return Suspendable.combine(beingUpdated, Suspendable.combine(suspendables)).suspend();
    }
}
