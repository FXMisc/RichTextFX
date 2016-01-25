package org.fxmisc.richtext;

import javafx.scene.control.IndexRange;

/**
 * Extended edit actions for {@link TextEditingArea}.
 */
public interface EditActions<PS, S> extends TextEditingArea<PS, S> {

    /**
     * Appends the given text to the end of the text content.
     */
    default void appendText(String text) {
        insertText(getLength(), text);
    }

    /**
     * Appends the given rich-text content to the end of this text-editing area.
     */
    default void append(StyledDocument<PS, S> document) {
        insert(getLength(), document);
    }

    /**
     * Inserts the given text at the given position.
     *
     * @param index The location to insert the text.
     * @param text The text to insert.
     */
    default void insertText(int index, String text) {
        replaceText(index, index, text);
    }

    /**
     * Inserts the given rich-text content at the given position.
     *
     * @param index The location to insert the text.
     * @param document The rich-text content to insert.
     */
    default void insert(int index, StyledDocument<PS, S> document) {
        replace(index, index, document);
    }

    /**
     * Removes a range of text.
     *
     * @param range The range of text to delete. It must not be null.
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
     * @param start Start index of the range to remove, inclusive.
     * @param end End index of the range to remove, exclusive.
     */
    default void deleteText(int start, int end) {
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
     * Clears the text.
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
    default void replace(StyledDocument<PS, S> replacement) {
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
    default void replaceSelection(StyledDocument<PS, S> replacement) {
        replace(getSelection(), replacement);
    }

    default void moveSelectedText(int pos) {
        IndexRange sel = getSelection();

        if(pos >= sel.getStart() && pos <= sel.getEnd()) {
            // no move, just position the caret
            selectRange(pos, pos);
        } else {
            StyledDocument<PS, S> text = this.subDocument(sel.getStart(), sel.getEnd());
            if(pos > sel.getEnd())
                pos -= sel.getLength();
            deleteText(sel);
            insert(pos, text);
        }
    }
}
