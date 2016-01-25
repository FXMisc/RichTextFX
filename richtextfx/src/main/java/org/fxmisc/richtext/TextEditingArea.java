package org.fxmisc.richtext;

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
public interface TextEditingArea<PS, S> {

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
    StyledDocument<PS, S> getDocument();

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
    ObservableList<Paragraph<PS, S>> getParagraphs();


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
    EventStream<RichTextChange<PS, S>> richChanges();


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
     * Returns rich-text content of the given paragraph.
     */
    StyledDocument<PS, S> subDocument(int paragraphIndex);

    /**
     * Returns rich-text content of the given character range.
     */
    StyledDocument<PS, S> subDocument(int start, int end);

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
     * Replaces a range of characters with the given rich-text document.
     */
    void replace(int start, int end, StyledDocument<PS, S> replacement);

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
    default void replace(IndexRange range, StyledDocument<PS, S> replacement) {
        replace(range.getStart(), range.getEnd(), replacement);
    }

    /**
     * Positions only the caret. Doesn't move the anchor and doesn't change
     * the selection. Can be used to achieve the special case of positioning
     * the caret outside or inside the selection, as opposed to always being
     * at the boundary. Use with care.
     */
    public void positionCaret(int pos);
}
