package org.fxmisc.richtext;

import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.model.StyledDocument;

/**
 * Specifies actions for editing the content of a {@link TextEditingArea}.
 */
public interface EditActions<PS, SEG, S> extends TextEditingArea<PS, SEG, S> {

    /**
     * Appends the given text to the end of the text content.
     */
    default void appendText(String text) {
        insertText(getLength(), text);
    }

    /**
     * Appends the given rich-text content to the end of this text-editing area.
     */
    default void append(StyledDocument<PS, SEG, S> document) {
        insert(getLength(), document);
    }

    /**
     * Append text with a style.
     */
    default void append(SEG seg, S style) {
        insert(getLength(), seg, style);
    }

    /**
     * Inserts the given text at the given position.
     *
     * @param position The position to insert the text.
     * @param text The text to insert.
     */
    default void insertText(int position, String text) {
        replaceText(position, position, text);
    }

    /**
     * Inserts the given text at the position returned from
     * {@code getAbsolutePosition(paragraphIndex, columnPosition)}.
     *
     * <p><b>Caution:</b> see {@link #getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     *
     * @param text The text to insert
     */
    default void insertText(int paragraphIndex, int columnPosition, String text) {
        int index = getAbsolutePosition(paragraphIndex, columnPosition);
        replaceText(index, index, text);
    }

    /**
     * Inserts the given rich-text content at the given position.
     *
     * @param position The position to insert the text.
     * @param document The rich-text content to insert.
     */
    default void insert(int position, StyledDocument<PS, SEG, S> document) {
        replace(position, position, document);
    }

    /**
     * Inserts text with a style at the given position.
     */
    default void insert(int position, SEG seg, S style) {
        replace(position, position, seg, style);
    }

    /**
     * Inserts the given rich-text content at the position returned from
     * {@code getAbsolutePosition(paragraphIndex, columnPosition)}.
     *
     * <p><b>Caution:</b> see {@link #getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     *
     * @param document The rich-text content to insert.
     */
    default void insert(int paragraphIndex, int columnPosition, StyledDocument<PS, SEG, S> document) {
        int pos = getAbsolutePosition(paragraphIndex, columnPosition);
        replace(pos, pos, document);
    }

    /**
     * Removes a range of text.
     *
     * @param range The range of text to delete. It must not be null. Its start and end values specify the start
     *              and end positions within the area.
     *
     * @see #deleteText(int, int)
     */
    default void deleteText(IndexRange range) {
        deleteText(range.getStart(), range.getEnd());
    }

    /**
     * Removes a range of text.
     *
     * It must hold {@code 0 <= start <= end <= getLength()}.
     *
     * @param start Start position of the range to remove
     * @param end End position of the range to remove
     */
    default void deleteText(int start, int end) {
        replaceText(start, end, "");
    }

    /**
     * Removes a range of text.
     *
     * It must hold {@code 0 <= start <= end <= getLength()} where
     * {@code start = getAbsolutePosition(startParagraph, startColumn);} and is <b>inclusive</b>, and
     * {@code int end = getAbsolutePosition(endParagraph, endColumn);} and is <b>exclusive</b>.
     *
     * <p><b>Caution:</b> see {@link #getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     */
    default void deleteText(int startParagraph, int startColumn, int endParagraph, int endColumn) {
        int start = getAbsolutePosition(startParagraph, startColumn);
        int end = getAbsolutePosition(endParagraph, endColumn);
        replaceText(start, end, "");
    }

    /**
     * Deletes the character that precedes the current caret position
     * from the text.
     */
    default void deletePreviousChar() {
        int end = getCaretPosition();
        if(end > 0) {
            int start = Character.offsetByCodePoints(getText(), end, -1);
            deleteText(start, end);
        }
    }

    /**
     * Deletes the character that follows the current caret position
     * from the text.
     */
    default void deleteNextChar() {
        int start = getCaretPosition();
        if(start < getLength()) {
            int end = Character.offsetByCodePoints(getText(), start, 1);
            deleteText(start, end);
        }
    }

    /**
     * Clears the area, so that it displays only an empty paragraph.
     */
    default void clear() {
        replaceText(0, getLength(), "");
    }

    /**
     * Replaces the entire content with the given text.
     */
    default void replaceText(String replacement) {
        replaceText(0,  getLength(), replacement);
    }

    /**
     * Replaces the entire content with the given rich-text content.
     */
    default void replace(StyledDocument<PS, SEG, S> replacement) {
        replace(0, getLength(), replacement);
    }

    /**
     * Replaces the selection with the given replacement String. If there is
     * no selection, then the replacement text is simply inserted at the current
     * caret position. If there was a selection, then the selection is cleared
     * and the given replacement text is inserted.
     */
    default void replaceSelection(String replacement) {
        replaceText(getSelection(), replacement);
    }

    /**
     * Replaces the selection with the given rich-text replacement. If there is
     * no selection, then the replacement is simply inserted at the current
     * caret position. If there was a selection, then the selection is cleared
     * and the given replacement text is inserted.
     */
    default void replaceSelection(StyledDocument<PS, SEG, S> replacement) {
        replace(getSelection(), replacement);
    }

    /**
     * If something is currently selected and the given position is outside of the selection, moves the selected
     * rich-text document to the given position by deleting it from the area and re-inserting it at the given position.
     * If nothing is selected, moves the caret ot that position.
     */
    default void moveSelectedText(int position) {
        IndexRange sel = getSelection();

        if((position >= sel.getStart() && position <= sel.getEnd()) || sel.equals(GenericStyledArea.EMPTY_RANGE)) {
            // no move, just position the caret
            selectRange(position, position);
        } else {
            StyledDocument<PS, SEG, S> text = this.subDocument(sel.getStart(), sel.getEnd());
            if(position > sel.getEnd())
                position -= sel.getLength();

            createMultiChange(2)
                    .deleteText(sel)
                    .insertAbsolutely(position, text)
                    .commit();

            // select moved text
            selectRange(position, position + text.length());
        }
    }
}
