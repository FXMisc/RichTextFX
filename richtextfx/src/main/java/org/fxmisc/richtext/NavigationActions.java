package org.fxmisc.richtext;

import java.text.BreakIterator;

/**
 * Specifies actions for moving the caret and/or making a selection for a {@link TextEditingArea}.
 */
public interface NavigationActions<PS, SEG, S> extends TextEditingArea<PS, SEG, S> {

    /**
     * Indicates how to treat selection when caret is moved.
     */
    static enum SelectionPolicy {
        /** Move the caret and anchor to the same given position, thereby clearing any existing selection */
        CLEAR,
        /**
         * Moves the caret only without also moving the anchor, thereby making a selection
         */
        ADJUST,
        /**
         * Extends the selection by updating the "boundary" of the selection that is closest to the new position
         * (i.e. either the selection's start or end position) to that new position and keeping the other position the
         * same. In either case, the caret position is updated to that new position while the anchor is updated to the
         * opposite boundary.
         * <p>Using the following example, where "||" is the anchor, "|" is the caret" and "_" is the
         * new position, EXTEND updates the values to:
         * <pre><code>
         * "A text ||with| a _selection" -&gt; "A text ||with a |selection"
         * "A _text ||with| a selection" -&gt; "A |text with|| a selection"
         * </code></pre>
         */
        EXTEND,
    }

    /**
     * Moves the caret to the given position in the text and clears any selection.
     */
    default void moveTo(int pos) {
        getCaretSelectionBind().moveTo(pos);
    }

    /**
     * Moves the caret to the position returned from {@code getAbsolutePosition(paragraphIndex, columnIndex)}
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
        getCaretSelectionBind().moveTo(paragraphIndex, columnIndex);
    }

    /**
     * Moves the caret to the given position.
     */
    default void moveTo(int position, SelectionPolicy selectionPolicy) {
        getCaretSelectionBind().moveTo(position, selectionPolicy);
    }

    /**
     * Moves the caret to the position returned from
     * {@code getAbsolutePosition(paragraphIndex, columnIndex)} using the given selection policy.
     *
     * <p><b>Caution:</b> see {@link #getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     */
    default void moveTo(int paragraphIndex, int columnIndex, SelectionPolicy selectionPolicy) {
        getCaretSelectionBind().moveTo(paragraphIndex, columnIndex, selectionPolicy);
    }

    /**
     * Moves the caret backward one char in the text.
     */
    default void previousChar(SelectionPolicy selectionPolicy) {
        getCaretSelectionBind().moveToPrevChar(selectionPolicy);
    }

    /**
     * Moves the caret forward one char in the text.
     */
    default void nextChar(SelectionPolicy selectionPolicy) {
        getCaretSelectionBind().moveToNextChar(selectionPolicy);
    }

    /**
     * Skips n number of word boundaries backwards.
     */
    default void wordBreaksBackwards(int n, SelectionPolicy selectionPolicy) {
        if(getLength() == 0) {
            return;
        }

        BreakIterator wordBreakIterator = BreakIterator.getWordInstance( getLocale() );
        wordBreakIterator.setText(getText());
        wordBreakIterator.preceding(getCaretPosition());
        for (int i = 1; i < n; i++) {
            wordBreakIterator.previous();
        }

        moveTo(wordBreakIterator.current(), selectionPolicy);
    }

    /**
     * Skips n number of word boundaries forward.
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
        getCaretSelectionBind().selectWord();
    }

    /**
     * Moves the caret to the beginning of the current paragraph.
     */
    default void paragraphStart(SelectionPolicy selectionPolicy) {
        getCaretSelectionBind().moveToParStart(selectionPolicy);
    }

    /**
     * Moves the caret to the end of the current paragraph.
     */
    default void paragraphEnd(SelectionPolicy selectionPolicy) {
        getCaretSelectionBind().moveToParEnd(selectionPolicy);
    }

    /**
     * Moves the caret to the beginning of the text.
     */
    default void start(SelectionPolicy selectionPolicy) {
        getCaretSelectionBind().moveToAreaStart(selectionPolicy);
    }

    /**
     * Moves the caret to the end of the text.
     */
    default void end(SelectionPolicy selectionPolicy) {
        getCaretSelectionBind().moveToAreaEnd(selectionPolicy);
    }

    /**
     * Selects the current paragraph.
     */
    default void selectParagraph() {
        getCaretSelectionBind().selectParagraph();
    }

    /**
     * Selects everything in the area.
     */
    default void selectAll() {
        getCaretSelectionBind().selectAll();
    }

    /**
     * Clears the selection while keeping the caret position.
     */
    default void deselect() {
        getCaretSelectionBind().deselect();
    }

}
