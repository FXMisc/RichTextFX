/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates and Tomas Mikula.
 * All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package codearea.control;

import java.text.BreakIterator;

import javafx.scene.control.IndexRange;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import com.sun.javafx.Utils;

/**
 * Defines a richer set of editing and navigation methods for CodeArea,
 * implemented as thin wrappers around CodeArea's basic methods, such as
 * {@link StyledTextArea#replaceText(int, int, String)} or
 * {@link StyledTextArea#selectRange(int, int)}.
 */
public class StyledTextAreaHelper<S> {

    public static enum SelectionPolicy {
        CLEAR,
        ADJUST,
        EXTEND,
    }

    private final StyledTextArea<S> styledTextArea;

    // Used for previous/next word navigation.
    private final BreakIterator wordBreakIterator = BreakIterator.getWordInstance();

    public StyledTextAreaHelper(StyledTextArea<S> styledTextArea) {
        this.styledTextArea = styledTextArea;
    }


    /**
     * Appends a sequence of characters to the content.
     *
     * @param text a non null String
     */
    public void appendText(String text) {
        insertText(styledTextArea.getLength(), text);
    }

    /**
     * Inserts a sequence of characters into the content.
     *
     * @param index The location to insert the text.
     * @param text The text to insert.
     */
    public void insertText(int index, String text) {
        styledTextArea.replaceText(index, index, text);
    }

    /**
     * Removes a range of characters from the content.
     *
     * @param range The range of text to delete. The range object must not be null.
     *
     * @see #deleteText(int, int)
     */
    public void deleteText(IndexRange range) {
        deleteText(range.getStart(), range.getEnd());
    }

    /**
     * Removes a range of characters from the content.
     *
     * @param start The starting index in the range, inclusive. This must be &gt;= 0 and &lt; the end.
     * @param end The ending index in the range, exclusive. This is one-past the last character to
     *            delete (consistent with the String manipulation methods). This must be &gt; the start,
     *            and &lt;= the length of the text.
     */
    public void deleteText(int start, int end) {
        styledTextArea.replaceText(start, end, "");
    }

    /**
     * Transfers the currently selected range in the text to the clipboard,
     * removing the current selection.
     */
    public void cut() {
        copy();
        IndexRange selection = styledTextArea.getSelection();
        deleteText(selection.getStart(), selection.getEnd());
    }

    /**
     * Transfers the currently selected range in the text to the clipboard,
     * leaving the current selection.
     */
     public void copy() {
        String selectedText = styledTextArea.getSelectedText();
        if (selectedText.length() > 0) {
            ClipboardContent content = new ClipboardContent();
            content.putString(selectedText);
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    /**
     * Transfers the contents in the clipboard into this text,
     * replacing the current selection.  If there is no selection, the contents
     * in the clipboard is inserted at the current caret position.
     */
    public void paste() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasString()) {
            String text = clipboard.getString();
            if (text != null) {
                replaceSelection(text);
            }
        }
    }

    /**
     * Moves the caret backward one char in the text.
     * Based on the given selection policy, anchor either moves with
     * the caret, stays put, or moves to the former caret position.
     */
    public void previousChar(SelectionPolicy selectionPolicy) {
        if (styledTextArea.getCaretPosition() > 0) {
            int newCaretPos = Character.offsetByCodePoints(styledTextArea.getText(), styledTextArea.getCaretPosition(), -1);
            positionCaret(newCaretPos, selectionPolicy);
        }
    }


    /**
     * Moves the caret forward one char in the text.
     * Based on the given selection policy, anchor either moves with
     * the caret, stays put, or moves to the former caret position.
     */
    public void nextChar(SelectionPolicy selectionPolicy) {
        if (styledTextArea.getCaretPosition() < styledTextArea.getLength()) {
            int newCaretPos = Character.offsetByCodePoints(styledTextArea.getText(), styledTextArea.getCaretPosition(), 1);
            positionCaret(newCaretPos, selectionPolicy);
        }
    }


    /**
     * Moves the caret to the beginning of previous word.
     * Based on the given selection policy, anchor either moves with
     * the caret, stays put, or moves to the former caret position.
     */
    public void previousWord(SelectionPolicy selectionPolicy) {
        int textLength = styledTextArea.getLength();
        if (textLength == 0)
            return;

        String text = styledTextArea.getText();
        wordBreakIterator.setText(text);

        int pos = wordBreakIterator.preceding(styledTextArea.getCaretPosition());

        // Skip the non-word region, then move/select to the beginning of the word.
        while (pos != BreakIterator.DONE &&
               !Character.isLetter(text.charAt(pos))) {
            pos = wordBreakIterator.preceding(Utils.clamp(0, pos, textLength-1));
        }

        // move/select
        positionCaret(wordBreakIterator.current(), selectionPolicy);
    }

    /**
     * Moves the caret to the beginning of next word.
     * Based on the given selection policy, anchor either moves with
     * the caret, stays put, or moves to the former caret position.
     */
    public void nextWord(SelectionPolicy selectionPolicy) {
        int textLength = styledTextArea.getLength();
        if (textLength == 0)
            return;

        wordBreakIterator.setText(styledTextArea.getText());
        wordBreakIterator.following(Utils.clamp(0, styledTextArea.getCaretPosition(), textLength-1));
        wordBreakIterator.next();

        positionCaret(wordBreakIterator.current(), selectionPolicy);
    }

    public void selectLine() {
        lineStart(SelectionPolicy.CLEAR);
        lineEnd(SelectionPolicy.ADJUST);
    }

    public void lineStart(SelectionPolicy selectionPolicy) {
        positionCaret(styledTextArea.getCaretPosition() - styledTextArea.caretCol.get(), selectionPolicy);
    }

    public void lineEnd(SelectionPolicy selectionPolicy) {
        int lineLen = styledTextArea.getLines().get(styledTextArea.caretRow.get()).length();
        int newPos = styledTextArea.getCaretPosition() - styledTextArea.caretCol.get() + lineLen;
        positionCaret(newPos, selectionPolicy);
    }

    public void start(SelectionPolicy selectionPolicy) {
        positionCaret(0, selectionPolicy);
    }

    public void end(SelectionPolicy selectionPolicy) {
        positionCaret(styledTextArea.getLength(), selectionPolicy);
    }

    /**
     * Selects all text in the text input.
     */
    public void selectAll() {
        styledTextArea.selectRange(0, styledTextArea.getLength());
    }

    /**
     * Deletes the character that precedes the current caret position from the
     * text.
     */
    public void deletePreviousChar() {
        int end = styledTextArea.getCaretPosition();
        if(end > 0) {
            int start = Character.offsetByCodePoints(styledTextArea.getText(), end, -1);
            deleteText(start, end);
        }
    }

    /**
     * Deletes the character that follows the current caret position from the
     * text.
     */
    public void deleteNextChar() {
        int start = styledTextArea.getCaretPosition();
        if(start < styledTextArea.getLength()) {
            int end = Character.offsetByCodePoints(styledTextArea.getText(), start, 1);
            deleteText(start, end);
        }
    }

    public void positionCaret(int pos) {
        positionCaret(pos, SelectionPolicy.CLEAR);
    }

    /**
     * Positions the caret to the position indicated by {@code pos}.
     * Based on the selection policy, the selection is either <em>cleared</em>
     * (i.e. anchor is set to the same position as caret), <em>adjusted</em>
     * (i.e. anchor is not moved at all), or <em>extended</em>
     * (i.e. {@code pos} becomes the new caret and, if {@code pos} points
     * outside the current selection, the far end of the current selection
     * becomes the anchor.
     */
    public void positionCaret(int pos, SelectionPolicy selectionPolicy) {
        pos = Utils.clamp(0, pos, styledTextArea.getLength());
        switch(selectionPolicy) {
            case CLEAR:
                styledTextArea.selectRange(pos, pos);
                break;
            case ADJUST:
                styledTextArea.selectRange(styledTextArea.getAnchor(), pos);
                break;
            case EXTEND:
                IndexRange sel = styledTextArea.getSelection();
                int anchor;
                if(pos <= sel.getStart())
                    anchor = sel.getEnd();
                else if(pos >= sel.getEnd())
                    anchor = sel.getStart();
                else
                    anchor = styledTextArea.getAnchor();
                styledTextArea.selectRange(anchor, pos);
                break;
        }
    }

    public void moveSelectedText(int pos) {
        pos = Utils.clamp(0, pos, styledTextArea.getLength());
        IndexRange sel = styledTextArea.getSelection();

        if(pos >= sel.getStart() && pos <= sel.getEnd()) {
            // no move, just position caret
            styledTextArea.selectRange(pos, pos);
        }
        else {
            String text = styledTextArea.getSelectedText();
            if(pos > sel.getEnd())
                pos -= sel.getLength();
            deleteText(sel);
            insertText(pos, text);
        }
    }

    /**
     * Clears the text.
     */
    public void clear() {
        deselect();
        styledTextArea.replaceText(0, styledTextArea.getLength(), "");
    }

    /**
     * Clears the selection while keeping the caret position.
     */
    public void deselect() {
        // set the anchor equal to the caret position, which clears the selection
        // while also preserving the caret position
        int p = styledTextArea.getCaretPosition();
        styledTextArea.selectRange(p, p);
    }

    /**
     * Replaces the entire text content with the given text.
     */
    public void replaceText(String replacement) {
        if (replacement == null) {
            throw new NullPointerException();
        }

        styledTextArea.replaceText(0,  styledTextArea.getLength(), replacement);
    }

    /**
     * Replaces the selection with the given replacement String. If there is
     * no selection, then the replacement text is simply inserted at the current
     * caret position. If there was a selection, then the selection is cleared
     * and the given replacement text is inserted.
     */
    public void replaceSelection(String replacement) {
        if (replacement == null) {
            throw new NullPointerException();
        }

        styledTextArea.replaceText(styledTextArea.getSelection(), replacement);
    }
}
