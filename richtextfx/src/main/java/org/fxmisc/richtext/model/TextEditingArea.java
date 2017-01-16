package org.fxmisc.richtext.model;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.IndexRange;

import org.reactfx.EventStream;

/**
 * Interface for a text editing control.
 *
 * Defines the core methods. Other interfaces define default
 * higher-level methods implemented on top of the core methods.
 *
 * @param <S> type of style that can be applied to text.
 */
public interface TextEditingArea<PS, SEG, S> {

    /*******************
     *                 *
     *   Observables   *
     *                 *
     *******************/

    /**
     * The number of characters in this text-editing area.
     */
    int getLength();
    ObservableValue<Integer> lengthProperty();

    /**
     * Text content of this text-editing area.
     */
    String getText();
    ObservableValue<String> textProperty();

    /**
     * Rich-text content of this text-editing area.
     * The returned document is immutable, it does not reflect
     * subsequent edits of this text-editing area.
     */
    StyledDocument<PS, SEG, S> getDocument();

    /**
     * The current position of the caret, as a character offset in the text.
     *
     * Most of the time, caret is at the boundary of the selection (if there
     * is any selection). However, there are circumstances when the caret is
     * positioned inside or outside the selected text. For example, when the
     * user is dragging the selected text, the caret moves with the cursor
     * to point at the position where the selected text moves upon release.
     */
    int getCaretPosition();
    ObservableValue<Integer> caretPositionProperty();

    /**
     * The anchor of the selection.
     * If there is no selection, this is the same as caret position.
     */
    int getAnchor();
    ObservableValue<Integer> anchorProperty();

    /**
     * The selection range.
     *
     * One boundary is always equal to anchor, and the other one is most
     * of the time equal to caret position.
     */
    IndexRange getSelection();
    ObservableValue<IndexRange> selectionProperty();

    /**
     * The selected text.
     */
    String getSelectedText();
    ObservableValue<String> selectedTextProperty();

    /**
     * Index of the current paragraph, i.e. the paragraph with the caret.
     */
    int getCurrentParagraph();
    ObservableValue<Integer> currentParagraphProperty();

    /**
     * The caret position within the current paragraph.
     */
    int getCaretColumn();
    ObservableValue<Integer> caretColumnProperty();

    /**
     * Unmodifiable observable list of paragraphs in this text area.
     */
    ObservableList<Paragraph<PS, SEG, S>> getParagraphs();


    /*********************
     *                   *
     *   Event streams   *
     *                   *
     *********************/

    /**
     * Stream of text changes.
     */
    EventStream<PlainTextChange> plainTextChanges();

    /**
     * Stream of rich text changes.
     */
    EventStream<RichTextChange<PS, SEG, S>> richChanges();


    /***************
     *             *
     *   Queries   *
     *             *
     ***************/

    /**
     * Returns text content of the given paragraph.
     */
    String getText(int paragraphIndex);

    /**
     * Returns text content of the given character range.
     */
    String getText(int start, int end);

    /**
     * Returns text content of the given character range.
     *
     * <p><b>Caution:</b> see {@link #getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     */
    default String getText(int startParagraph, int startColumn, int endParagraph, int endColumn) {
        int start = getAbsolutePosition(startParagraph, startColumn);
        int end = getAbsolutePosition(endParagraph, endColumn);
        return getText(start, end);
    }

    /**
     * Returns rich-text content of the given paragraph.
     */
    StyledDocument<PS, SEG, S> subDocument(int paragraphIndex);

    /**
     * Returns rich-text content of the given character range.
     */
    StyledDocument<PS, SEG, S> subDocument(int start, int end);

    /**
     * Returns rich-text content of the given character range.
     *
     * <p><b>Caution:</b> see {@link #getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     */
    default StyledDocument<PS, SEG, S> subDocument(int startParagraph, int startColumn, int endParagraph, int endColumn) {
        int start = getAbsolutePosition(startParagraph, startColumn);
        int end = getAbsolutePosition(endParagraph, endColumn);
        return subDocument(start, end);
    }

    /***************
     *             *
     *   Actions   *
     *             *
     ***************/

    /**
     * Positions the anchor and caretPosition explicitly,
     * effectively creating a selection.
     */
    void selectRange(int anchor, int caretPosition);

    /**
     * Positions the anchor and caretPosition explicitly,
     * effectively creating a selection.
     *
     * <p><b>Caution:</b> see {@link #getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     */
    default void selectRange(int anchorParagraph, int anchorColumn, int caretPositionParagraph, int caretPositionColumn) {
        int anchor = getAbsolutePosition(anchorParagraph, anchorColumn);
        int caretPosition = getAbsolutePosition(caretPositionParagraph, caretPositionColumn);
        selectRange(anchor, caretPosition);
    }

    /**
     * Replaces a range of characters with the given text.
     *
     * It must hold {@code 0 <= start <= end <= getLength()}.
     *
     * @param start Start index of the range to replace, inclusive.
     * @param end End index of the range to replace, exclusive.
     * @param text The text to put in place of the deleted range.
     * It must not be null.
     */
    void replaceText(int start, int end, String text);

    /**
     * Replaces a range of characters with the given text.
     *
     * It must hold {@code 0 <= start <= end <= getLength()} where
     * {@code start = getAbsolutePosition(startParagraph, startColumn);} and is <b>inclusive</b>, and
     * {@code int end = getAbsolutePosition(endParagraph, endColumn);} and is <b>exclusive</b>.
     *
     * <p><b>Caution:</b> see {@link #getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     *
     * @param text The text to put in place of the deleted range.
     * It must not be null.
     */
    default void replaceText(int startParagraph, int startColumn, int endParagraph, int endColumn, String text) {
        int start = getAbsolutePosition(startParagraph, startColumn);
        int end = getAbsolutePosition(endParagraph, endColumn);
        replaceText(start, end, text);
    }

    /**
     * Replaces a range of characters with the given rich-text document.
     */
    void replace(int start, int end, StyledDocument<PS, SEG, S> replacement);

    /**
     * Replaces a range of characters with the given rich-text document.
     *
     * <p><b>Caution:</b> see {@link #getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     */
    default void replace(int startParagraph, int startColumn, int endParagraph, int endColumn, StyledDocument<PS, SEG, S> replacement) {
        int start = getAbsolutePosition(startParagraph, startColumn);
        int end = getAbsolutePosition(endParagraph, endColumn);
        replace(start, end, replacement);
    }

    /**
     * Replaces a range of characters with the given text.
     *
     * @param range The range to replace. It must not be null.
     * @param text The text to put in place of the deleted range.
     * It must not be null.
     *
     * @see #replaceText(int, int, String)
     */
    default void replaceText(IndexRange range, String text) {
        replaceText(range.getStart(), range.getEnd(), text);
    }

    /**
     * Equivalent to
     * {@code replace(range.getStart(), range.getEnd(), replacement)}.
     */
    default void replace(IndexRange range, StyledDocument<PS, SEG, S> replacement) {
        replace(range.getStart(), range.getEnd(), replacement);
    }

    /**
     * Returns the absolute position (i.e. the spot in-between characters) to the left of the given column in the given paragraph.
     *
     * <p>For example, given a text with only one line {@code "text"} and the columnIndex value of {@code 1}, "position 1" would be returned:</p>
     * <pre>
     *  ┌ character index 0
     *  | ┌ character index 1
     *  | |   ┌ character index 3
     *  | |   |
     *  v v   v
     *
     * |t|e|x|t|
     *
     * ^ ^     ^
     * | |     |
     * | |     └ position 4
     * | └ position 1
     * └ position 0
     * </pre>
     *
     * <h3>Warning: Off-By-One errors can easily occur</h3>
     * <p>If the column index spans outside of the given paragraph's length, the returned value will
     * pass on to the previous/next paragraph. In other words, given a document with two paragraphs
     * (where the first paragraph's text is "some" and the second "thing"), then the following statements are true:</p>
     * <ul>
     *     <li><code>getAbsolutePosition(0, "some".length()) == 4 == getAbsolutePosition(1, -1)</code></li>
     *     <li><code>getAbsolutePosition(0, "some".length() + 1) == 5 == getAbsolutePosition(1, 0)</code></li>
     * </ul>
     *
     * @param paragraphIndex The index of the paragraph from which to start.
     * @param columnIndex If positive, the index going forward (the given paragraph's line or the next one(s)).
     *                    If negative, the index going backward (the previous paragraph's line(s)
     */
    int getAbsolutePosition(int paragraphIndex, int columnIndex);
}
