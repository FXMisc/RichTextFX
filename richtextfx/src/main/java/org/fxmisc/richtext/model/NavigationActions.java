package org.fxmisc.richtext.model;

import java.text.BreakIterator;

import javafx.scene.control.IndexRange;

/**
 * Navigation actions for {@link TextEditingArea}.
 */
public interface NavigationActions<PS, SEG, S> extends TextEditingArea<PS, SEG, S> {

    /**
     * Indicates how to treat selection when caret is moved.
     */
    static enum SelectionPolicy {
        CLEAR,
        ADJUST,
        EXTEND,
    }

    /**
     * Moves the caret to the given position in the text
     * and clears any selection.
     */
    default void moveTo(int pos) {
        selectRange(pos, pos);
    }

    /**
     * Moves the caret to the position returned from
     * {@code getAbsolutePosition(paragraphIndex, columnIndex)}
     * and clears any selection.
     *
     * <p>For example, if "|" represents the caret, "_" represents a newline character, and given the text "Some_Text"
     * (1st line: 'Some'; 2nd line 'Text'), then...</p>
     * <ul>
     *     <li>calling {@code moveTo(0, 4)} results in "Some|_Text" (caret to the left of "_")</li>
     *     <li>calling {@code moveTo(1, -1)} results in "Some|_Text" (caret in the left of "_")</li>
     *     <li>calling {@code moveTo(1, 0)} results in "Some_|Text" (caret in the left of "T")</li>
     *     <li>calling {@code moveTo(0, 5)} results in "Some_|Text" (caret in the left of "T")</li>
     * </ul>
     *
     * <p><b>Caution:</b> see {@link #getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     *
     * @param paragraphIndex the index of the paragraph to which to move the caret
     * @param columnIndex the index to the left of which to move the caret
     */
    default void moveTo(int paragraphIndex, int columnIndex) {
        int pos = getAbsolutePosition(paragraphIndex, columnIndex);
        selectRange(pos, pos);
    }

    /**
     * Moves the caret to the position indicated by {@code pos}.
     * Based on the selection policy, the selection is either <em>cleared</em>
     * (i.e. anchor is set to the same position as caret), <em>adjusted</em>
     * (i.e. anchor is not moved at all), or <em>extended</em>
     * (i.e. {@code pos} becomes the new caret and, if {@code pos} points
     * outside the current selection, the far end of the current selection
     * becomes the anchor.
     */
    default void moveTo(int pos, SelectionPolicy selectionPolicy) {
        switch(selectionPolicy) {
            case CLEAR:
                selectRange(pos, pos);
                break;
            case ADJUST:
                selectRange(getAnchor(), pos);
                break;
            case EXTEND:
                IndexRange sel = getSelection();
                int anchor;
                if(pos <= sel.getStart())
                    anchor = sel.getEnd();
                else if(pos >= sel.getEnd())
                    anchor = sel.getStart();
                else
                    anchor = getAnchor();
                selectRange(anchor, pos);
                break;
        }
    }

    /**
     * Moves the caret to the position returned from
     * {@code getAbsolutePosition(paragraphIndex, columnIndex)}.
     *
     * Based on the selection policy, the selection is either <em>cleared</em>
     * (i.e. anchor is set to the same position as caret), <em>adjusted</em>
     * (i.e. anchor is not moved at all), or <em>extended</em>
     * (i.e. {@code getAbsolutePosition(paragraphIndex, columnIndex} becomes
     * the new caret and, if that returned value points outside the current selection,
     * the far end of the current selection becomes the anchor.
     *
     * <p><b>Caution:</b> see {@link #getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     */
    default void moveTo(int paragraphIndex, int columnIndex, SelectionPolicy selectionPolicy) {
        int pos = getAbsolutePosition(paragraphIndex, columnIndex);
        moveTo(pos, selectionPolicy);
    }

    /**
     * Moves the caret backward one char in the text.
     * Based on the given selection policy, anchor either moves with
     * the caret, stays put, or moves to the former caret position.
     */
    default void previousChar(SelectionPolicy selectionPolicy) {
        if (getCaretPosition() > 0) {
            int newCaretPos = Character.offsetByCodePoints(getText(), getCaretPosition(), -1);
            moveTo(newCaretPos, selectionPolicy);
        }
    }

    /**
     * Moves the caret forward one char in the text.
     * Based on the given selection policy, anchor either moves with
     * the caret, stays put, or moves to the former caret position.
     */
    default void nextChar(SelectionPolicy selectionPolicy) {
        if (getCaretPosition() < getLength()) {
            int newCaretPos = Character.offsetByCodePoints(getText(), getCaretPosition(), 1);
            moveTo(newCaretPos, selectionPolicy);
        }
    }

    /**
     * Skips n number of word boundaries backwards.
     * Based on the given selection policy, anchor either moves with
     * the caret, stays put, or moves to the former caret position.
     */
    default void wordBreaksBackwards(int n, SelectionPolicy selectionPolicy) {
        if(getLength() == 0) {
            return;
        }

        BreakIterator wordBreakIterator = BreakIterator.getWordInstance();
        wordBreakIterator.setText(getText());
        wordBreakIterator.preceding(getCaretPosition());
        for (int i = 1; i < n; i++) {
            wordBreakIterator.previous();
        }

        moveTo(wordBreakIterator.current(), selectionPolicy);
    }

    /**
     * Skips n number of word boundaries forward.
     * Based on the given selection policy, anchor either moves with
     * the caret, stays put, or moves to the former caret position.
     */
    default void wordBreaksForwards(int n, SelectionPolicy selectionPolicy) {
        if(getLength() == 0) {
            return;
        }

        BreakIterator wordBreakIterator = BreakIterator.getWordInstance();
        wordBreakIterator.setText(getText());
        wordBreakIterator.following(getCaretPosition());
        for (int i = 1; i < n; i++) {
            wordBreakIterator.next();
        }

        moveTo(wordBreakIterator.current(), selectionPolicy);
    }

    /**
     * Selects the word closest to the caret
     */
    default void selectWord() {
        wordBreaksBackwards(1, SelectionPolicy.CLEAR);
        wordBreaksForwards(1, SelectionPolicy.ADJUST);
    }

    /**
     * Moves the caret to the beginning of the current paragraph.
     */
    default void paragraphStart(SelectionPolicy selectionPolicy) {
        moveTo(getCaretPosition() - getCaretColumn(), selectionPolicy);
    }

    /**
     * Moves the caret to the end of the current paragraph.
     */
    default void paragraphEnd(SelectionPolicy selectionPolicy) {
        int lineLen = getText(getCurrentParagraph()).length();
        int newPos = getCaretPosition() - getCaretColumn() + lineLen;
        moveTo(newPos, selectionPolicy);
    }

    /**
     * Moves the caret to the beginning of the text.
     */
    default void start(SelectionPolicy selectionPolicy) {
        moveTo(0, selectionPolicy);
    }

    /**
     * Moves the caret to the end of the text.
     */
    default void end(SelectionPolicy selectionPolicy) {
        moveTo(getLength(), selectionPolicy);
    }

    /**
     * Selects the current paragraph.
     */
    default void selectParagraph() {
        paragraphStart(SelectionPolicy.CLEAR);
        paragraphEnd(SelectionPolicy.ADJUST);
    }

    /**
     * Selects all text in the text input.
     */
    default void selectAll() {
        selectRange(0, getLength());
    }

    /**
     * Clears the selection while keeping the caret position.
     */
    default void deselect() {
        int p = getCaretPosition();
        selectRange(p, p);
    }

}
