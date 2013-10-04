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
 * {@link CodeArea#replaceText(int, int, String)} or
 * {@link CodeArea#selectRange(int, int)}.
 */
public class CodeAreaHelper {

    public static enum SelectionPolicy {
        CLEAR,
        ADJUST,
        EXTEND,
    }

    private final CodeArea codeArea;

    // Used for previous/next word navigation.
    private final BreakIterator wordBreakIterator = BreakIterator.getWordInstance();

    public CodeAreaHelper(CodeArea codeArea) {
        this.codeArea = codeArea;
    }


    /**
     * Appends a sequence of characters to the content.
     *
     * @param text a non null String
     */
    public void appendText(String text) {
        insertText(codeArea.getLength(), text);
    }

    /**
     * Inserts a sequence of characters into the content.
     *
     * @param index The location to insert the text.
     * @param text The text to insert.
     */
    public void insertText(int index, String text) {
        codeArea.replaceText(index, index, text);
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
        codeArea.replaceText(start, end, "");
    }

    /**
     * Transfers the currently selected range in the text to the clipboard,
     * removing the current selection.
     */
    public void cut() {
        copy();
        IndexRange selection = codeArea.getSelection();
        deleteText(selection.getStart(), selection.getEnd());
    }

    /**
     * Transfers the currently selected range in the text to the clipboard,
     * leaving the current selection.
     */
     public void copy() {
        String selectedText = codeArea.getSelectedText();
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
        if (codeArea.getCaretPosition() > 0) {
            int newCaretPos = Character.offsetByCodePoints(codeArea.getText(), codeArea.getCaretPosition(), -1);
            positionCaret(newCaretPos, selectionPolicy);
        }
    }


    /**
     * Moves the caret forward one char in the text.
     * Based on the given selection policy, anchor either moves with
     * the caret, stays put, or moves to the former caret position.
     */
    public void nextChar(SelectionPolicy selectionPolicy) {
        if (codeArea.getCaretPosition() < codeArea.getLength()) {
            int newCaretPos = Character.offsetByCodePoints(codeArea.getText(), codeArea.getCaretPosition(), 1);
            positionCaret(newCaretPos, selectionPolicy);
        }
    }


    /**
     * Moves the caret to the beginning of previous word.
     * Based on the given selection policy, anchor either moves with
     * the caret, stays put, or moves to the former caret position.
     */
    public void previousWord(SelectionPolicy selectionPolicy) {
        int textLength = codeArea.getLength();
        if (textLength == 0)
            return;

        String text = codeArea.getText();
        wordBreakIterator.setText(text);

        int pos = wordBreakIterator.preceding(codeArea.getCaretPosition());

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
        int textLength = codeArea.getLength();
        if (textLength == 0)
            return;

        wordBreakIterator.setText(codeArea.getText());
        wordBreakIterator.following(Utils.clamp(0, codeArea.getCaretPosition(), textLength-1));
        wordBreakIterator.next();

        positionCaret(wordBreakIterator.current(), selectionPolicy);
    }

    public void selectLine() {
        lineStart(SelectionPolicy.CLEAR);
        lineEnd(SelectionPolicy.ADJUST);
    }

    public void lineStart(SelectionPolicy selectionPolicy) {
        positionCaret(codeArea.getCaretPosition() - codeArea.caretCol.get(), selectionPolicy);
    }

    public void lineEnd(SelectionPolicy selectionPolicy) {
        int lineLen = codeArea.getLines().get(codeArea.caretRow.get()).length();
        int newPos = codeArea.getCaretPosition() - codeArea.caretCol.get() + lineLen;
        positionCaret(newPos, selectionPolicy);
    }

    public void start(SelectionPolicy selectionPolicy) {
        positionCaret(0, selectionPolicy);
    }

    public void end(SelectionPolicy selectionPolicy) {
        positionCaret(codeArea.getLength(), selectionPolicy);
    }

    /**
     * Selects all text in the text input.
     */
    public void selectAll() {
        codeArea.selectRange(0, codeArea.getLength());
    }

    /**
     * Deletes the character that precedes the current caret position from the
     * text.
     */
    public void deletePreviousChar() {
        int end = codeArea.getCaretPosition();
        if(end > 0) {
            int start = Character.offsetByCodePoints(codeArea.getText(), end, -1);
            deleteText(start, end);
        }
    }

    /**
     * Deletes the character that follows the current caret position from the
     * text.
     */
    public void deleteNextChar() {
        int start = codeArea.getCaretPosition();
        if(start < codeArea.getLength()) {
            int end = Character.offsetByCodePoints(codeArea.getText(), start, 1);
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
        pos = Utils.clamp(0, pos, codeArea.getLength());
        switch(selectionPolicy) {
            case CLEAR:
                codeArea.selectRange(pos, pos);
                break;
            case ADJUST:
                codeArea.selectRange(codeArea.getAnchor(), pos);
                break;
            case EXTEND:
                IndexRange sel = codeArea.getSelection();
                int anchor;
                if(pos <= sel.getStart())
                    anchor = sel.getEnd();
                else if(pos >= sel.getEnd())
                    anchor = sel.getStart();
                else
                    anchor = codeArea.getAnchor();
                codeArea.selectRange(anchor, pos);
                break;
        }
    }

    public void moveSelectedText(int pos) {
        pos = Utils.clamp(0, pos, codeArea.getLength());
        IndexRange sel = codeArea.getSelection();

        if(pos >= sel.getStart() && pos <= sel.getEnd()) {
            // no move, just position caret
            codeArea.selectRange(pos, pos);
        }
        else {
            String text = codeArea.getSelectedText();
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
        codeArea.replaceText(0, codeArea.getLength(), "");
    }

    /**
     * Clears the selection while keeping the caret position.
     */
    public void deselect() {
        // set the anchor equal to the caret position, which clears the selection
        // while also preserving the caret position
        int p = codeArea.getCaretPosition();
        codeArea.selectRange(p, p);
    }

    /**
     * Replaces the selection with the given replacement String. If there is
     * no selection, then the replacement text is simply inserted at the current
     * caret position. If there was a selection, then the selection is cleared
     * and the given replacement text inserted.
     */
    public void replaceSelection(String replacement) {
        if (replacement == null) {
            throw new NullPointerException();
        }

        codeArea.replaceText(codeArea.getSelection(), replacement);
    }
}
