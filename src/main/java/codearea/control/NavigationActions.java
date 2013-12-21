/*
 * Copyright (c) 2013, Tomas Mikula. All rights reserved.
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

/**
 * Navigation actions for {@link TextEditingArea}.
 */
public interface NavigationActions extends TextEditingArea {

    /**
     * Indicates how to treat selection when caret is moved.
     */
    static enum SelectionPolicy {
        CLEAR,
        ADJUST,
        EXTEND,
    }

    /**
     * Positions the caret at the given position in the text
     * and clears any selection.
     */
    default void positionCaret(int pos) {
        selectRange(pos, pos);
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
    default void positionCaret(int pos, SelectionPolicy selectionPolicy) {
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
     * Moves the caret backward one char in the text.
     * Based on the given selection policy, anchor either moves with
     * the caret, stays put, or moves to the former caret position.
     */
    default void previousChar(SelectionPolicy selectionPolicy) {
        if (getCaretPosition() > 0) {
            int newCaretPos = Character.offsetByCodePoints(getText(), getCaretPosition(), -1);
            positionCaret(newCaretPos, selectionPolicy);
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
            positionCaret(newCaretPos, selectionPolicy);
        }
    }


    /**
     * Moves the caret to the beginning of preceding word.
     * Based on the given selection policy, anchor either moves with
     * the caret, stays put, or moves to the former caret position.
     */
    default void previousWord(SelectionPolicy selectionPolicy) {
        int textLength = getLength();
        if (textLength == 0)
            return;

        String text = getText();
        BreakIterator wordBreakIterator = BreakIterator.getWordInstance();
        wordBreakIterator.setText(text);

        int pos = wordBreakIterator.preceding(getCaretPosition());
        if(pos != BreakIterator.DONE &&
               !Character.isLetter(text.charAt(pos))) {
            // we ended at the end of the word, skip to the beginning
            wordBreakIterator.preceding(pos);
        }

        // move/select
        positionCaret(wordBreakIterator.current(), selectionPolicy);
    }

    /**
     * Moves the caret to the beginning of the following word.
     * Based on the given selection policy, anchor either moves with
     * the caret, stays put, or moves to the former caret position.
     */
    default void nextWord(SelectionPolicy selectionPolicy) {
        int textLength = getLength();
        if (textLength == 0)
            return;

        BreakIterator wordBreakIterator = BreakIterator.getWordInstance();
        wordBreakIterator.setText(getText());
        wordBreakIterator.following(getCaretPosition());
        wordBreakIterator.next();

        positionCaret(wordBreakIterator.current(), selectionPolicy);
    }

    /**
     * Moves the caret to the beginning of the current line.
     */
    default void lineStart(SelectionPolicy selectionPolicy) {
        positionCaret(getCaretPosition() - getCaretColumn(), selectionPolicy);
    }

    /**
     * Moves the caret to the end of the current line.
     */
    default void lineEnd(SelectionPolicy selectionPolicy) {
        int lineLen = getText(getCurrentParagraph()).length();
        int newPos = getCaretPosition() - getCaretColumn() + lineLen;
        positionCaret(newPos, selectionPolicy);
    }

    /**
     * Moves the caret to the beginning of the text.
     */
    default void start(SelectionPolicy selectionPolicy) {
        positionCaret(0, selectionPolicy);
    }

    /**
     * Moves the caret to the end of the text.
     */
    default void end(SelectionPolicy selectionPolicy) {
        positionCaret(getLength(), selectionPolicy);
    }

    /**
     * Selects the current line.
     */
    default void selectLine() {
        lineStart(SelectionPolicy.CLEAR);
        lineEnd(SelectionPolicy.ADJUST);
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
