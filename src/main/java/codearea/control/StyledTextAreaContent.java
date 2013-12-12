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

import java.util.ArrayList;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.ReadOnlyStringPropertyBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Code area content model.
 */
final class StyledTextAreaContent<S> extends ReadOnlyStringPropertyBase implements ObservableTextValue {
    final ObservableList<Line<S>> lines = FXCollections.observableArrayList();

    /**
     * stores the last value returned by get().
     */
    private String cachedValue = null;

    private int contentLength;
    private final IntegerBinding lengthBinding = new IntegerBinding() {
        @Override
        protected int computeValue() {
            return contentLength;
        }
    };
    @Override public IntegerBinding length() {
        return lengthBinding;
    }

    /*
     * text change events
     */
    private final List<TextChangeListener> textChangeListeners = new ArrayList<>();
    @Override public void addListener(TextChangeListener listener) {
        textChangeListeners.add(listener);
    }
    @Override public void removeListener(TextChangeListener listener) {
        textChangeListeners.remove(listener);
    }
    private void fireTextChange(int pos, String removedText, String addedText) {
        for(TextChangeListener l: textChangeListeners)
            l.handle(pos, removedText, addedText);
    }

    StyledTextAreaContent(S initialStyle) {
        lines.add(new Line<S>("", initialStyle));

        this.addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable content) {
                // Invalidate length on text change.
                // Do this in a text invalidation listener so that by the time
                // the length is invalidated, the text is already invalid, too.
                // Prior to calling fireValueChangedEvent(), contentLength had
                // already been set to the new length, thus the length returned
                // from the next call to computeValue() will be consistent with
                // the current text value.
                lengthBinding.invalidate();

                // forget the cached value
                cachedValue = null;
            }
        });
    }

    @Override public Object getBean() {
        return null;
    }

    @Override public String getName() {
        return "text";
    }

    @Override public String get() {
        if(cachedValue == null)
            cachedValue = get(0, contentLength);

        return cachedValue;
    }

    public String get(int start, int end) {
        int length = end - start;
        StringBuilder textBuilder = new StringBuilder(length);

        int lineCount = lines.size();
        int lineIndex = 0;
        while (lineIndex < lineCount) {
            Line<S> line = lines.get(lineIndex);
            int lineLen = line.length() + 1;

            if (start < lineLen)
                break;

            start -= lineLen;
            lineIndex++;
        }

        // Read characters until end is reached, appending to text builder
        // and moving to next line as needed
        Line<S> line = lines.get(lineIndex);

        for(int i = 0; i < length; ++i) {
            if (start == line.length()) {
                textBuilder.append('\n');
                line = lines.get(++lineIndex);
                start = 0;
            } else {
                textBuilder.append(line.charAt(start++));
            }
        }

        return textBuilder.toString();
    }

    public void replaceText(int start, int end, String replacement) {
        if (replacement == null)
            throw new NullPointerException("replacement text is null");

        int[] range2D = rangeToRowAndCol(start, end);
        int leadingLineIndex = range2D[0];
        int leadingLineFrom = range2D[1];
        int trailingLineIndex = range2D[2];
        int trailingLineTo = range2D[3];

        replacement = filterInput(replacement);
        String replacedText = get(start, end);

        // Get the leftovers after cutting out the deletion
        Line<S> leadingLine = lines.get(leadingLineIndex);
        Line<S> trailingLine = lines.get(trailingLineIndex);
        Line<S> left = leadingLine.split(leadingLineFrom)[0];
        Line<S> right = trailingLine.split(trailingLineTo)[1];

        String[] replacementLines = replacement.split("\n", -1);
        int n = replacementLines.length;

        S replacementStyle = leadingLine.getStyleAt(leadingLineFrom-1);

        if(n == 1) {
            // replacement is just a single line,
            // use it to join the two leftover lines
            left.append(replacementLines[0]);
            left.appendFrom(right);

            // replace the affected liens with the merger of leftovers and the replacement line
            // TODO: use setAll(from, to, col) when implemented (see https://javafx-jira.kenai.com/browse/RT-32655)
            lines.set(leadingLineIndex, left); // use set() instead of remove and add to make sure the number of lines is never 0
            lines.remove(leadingLineIndex+1, trailingLineIndex+1);
        }
        else {
            // append the first replacement line to the left leftover
            // and prepend the last replacement line to the right leftover
            left.append(replacementLines[0]);
            right.insert(0, replacementLines[n-1]);

            // create list of new lines to replace the affected lines
            List<Line<S>> newLines = new ArrayList<>(n-1);
            for(int i = 1; i < n - 1; ++i)
                newLines.add(new Line<S>(replacementLines[i], replacementStyle));
            newLines.add(right);

            // replace the affected lines with the new lines
            // TODO: use setAll(from, to, col) when implemented (see https://javafx-jira.kenai.com/browse/RT-32655)
            lines.set(leadingLineIndex, left); // use set() instead of remove and add to make sure the number of lines is never 0
            lines.remove(leadingLineIndex+1, trailingLineIndex+1);
            lines.addAll(leadingLineIndex+1, newLines);
        }

        // Update content length
        contentLength = contentLength - (end - start) + replacement.length();

        fireValueChangedEvent();
        fireTextChange(start, replacedText, replacement);
    }

    public void setStyle(int from, int to, S style) {
        int[] range2D = rangeToRowAndCol(from, to);
        int firstLineIndex = range2D[0];
        int firstLineFrom = range2D[1];
        int lastLineIndex = range2D[2];
        int lastLineTo = range2D[3];

        if(from == to)
            return;

        if(firstLineIndex == lastLineIndex) {
            setStyle(firstLineIndex, firstLineFrom, lastLineTo, style);
        }
        else {
            int firstLineLen = lines.get(firstLineIndex).length();
            setStyle(firstLineIndex, firstLineFrom, firstLineLen, style);
            for(int i=firstLineIndex+1; i<lastLineIndex; ++i) {
                setStyle(i, style);
            }
            setStyle(lastLineIndex, 0, lastLineTo, style);
        }
    }

    public void setStyle(int line, S style) {
        Line<S> l = lines.get(line);
        l.setStyle(style);
        lines.set(line, l); // to generate change event
    }

    public void setStyle(int line, int fromCol, int toCol, S style) {
        Line<S> l = lines.get(line);
        l.setStyle(fromCol, toCol, style);
        lines.set(line, l); // to generate change event
    }

    public S getStyleAt(int pos) {
        int[] pos2D = positionToRowAndCol(pos);
        int line = pos2D[0];
        int col = pos2D[1];
        return lines.get(line).getStyleAt(col);
    }

    public S getStyleAt(int line, int column) {
        return lines.get(line).getStyleAt(column);
    }

    int[] positionToRowAndCol(int pos) {
        return positionToRowAndCol(pos, 0);
    }

    private int[] rangeToRowAndCol(int start, int end) {
        if (start > end)
            throw new IllegalArgumentException(
                    "end has to be greater than or equal to start"
                    + " (start: " + start + ", end: " + end + ")");

        if (start < 0 || end > contentLength)
            throw new IndexOutOfBoundsException();

        int[] start2D = positionToRowAndCol(start);
        int startRow = start2D[0];
        int startCol = start2D[1];

        int startRowOffset = start - startCol;
        int[] end2D = positionToRowAndCol(end - startRowOffset, startRow);
        int endRow = end2D[0];
        int endCol = end2D[1];

        return new int[]{ startRow, startCol, endRow, endCol };
    }

    private int[] positionToRowAndCol(int pos, int fromLine) {
        Line<S> line = lines.get(fromLine);
        while(pos >= line.length()+1) {
            pos -= line.length()+1;
            line = lines.get(++fromLine);
        }
        return new int[]{ fromLine, pos };
    }

    /**
     * A little utility method for stripping out unwanted characters.
     *
     * @param txt
     * @return The string after having the unwanted characters stripped out.
     */
    private static String filterInput(String txt) {
        // Most of the time, when text is inserted, there are no illegal
        // characters. So we'll do a "cheap" check for illegal characters.
        // If we find one, we'll do a longer replace algorithm. In the
        // case of illegal characters, this may at worst be an O(2n) solution.
        // Strip out any characters that are outside the printed range
        if (containsInvalidCharacters(txt)) {
            StringBuilder s = new StringBuilder(txt.length());
            for (int i=0; i<txt.length(); i++) {
                final char c = txt.charAt(i);
                if (!isInvalidCharacter(c)) {
                    s.append(c);
                }
            }
            txt = s.toString();
        }
        return txt;
    }

    private static boolean containsInvalidCharacters(String txt) {
        for (int i=0; i<txt.length(); i++) {
            final char c = txt.charAt(i);
            if (isInvalidCharacter(c)) return true;
        }
        return false;
    }

    private static boolean isInvalidCharacter(char c) {
        if (c == 0x7F) return true;
        if (c == 0xA) return false; // newline
        if (c == 0x9) return false; // tab
        if (c < 0x20) return true;
        return false;
    }
}