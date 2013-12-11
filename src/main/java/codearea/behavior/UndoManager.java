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

package codearea.behavior;

import java.util.ArrayList;

import codearea.control.StyledTextArea;
import codearea.control.TextChangeListener;

class UndoManager {

    private static class Change {
        final int start;
        final String oldText;
        final String newText;

        public Change(int start, String oldText, String newText) {
            this.start = start;
            this.oldText = oldText;
            this.newText = newText;
        }

        /**
         * Merges this change with the given change, if possible.
         * This change is considered to be the former and the given
         * change is considered to be the latter.
         * Changes can be merged if either
         * <ul>
         *   <li>the latter's start matches the former's added text end; or</li>
         *   <li>the latter's removed text end matches the former's added text end.</li>
         * </ul>
         * @param latter change to merge with this change.
         * @return a new merged change if changes can be merged,
         * {@code null} otherwise.
         */
        public Change mergeWith(Change latter) {
            if(latter.start == this.start + this.newText.length()) {
                String removedText = this.oldText + latter.oldText;
                String addedText = this.newText + latter.newText;
                return new Change(this.start, removedText, addedText);
            }
            else if(latter.start + latter.oldText.length() == this.start + this.newText.length()) {
                if(this.start <= latter.start) {
                    String addedText = this.newText.substring(0, latter.start - this.start) + latter.newText;
                    return new Change(this.start, this.oldText, addedText);
                }
                else {
                    String removedText = latter.oldText.substring(0, this.start - latter.start) + this.oldText;
                    return new Change(latter.start, removedText, latter.newText);
                }
            }
            else {
                return null;
            }
        }
    }

    private final StyledTextArea<?> styledTextArea;

    private final ArrayList<Change> chain = new ArrayList<Change>();
    private int currentIndex = 0;

    boolean appendable;

    /**
     * Indicates whether code area content
     * is being modified by this UndoManager.
     */
    private boolean modifyingText = false;

    /**
     * @param styledTextArea
     */
    UndoManager(StyledTextArea<?> styledTextArea) {
        this.styledTextArea = styledTextArea;
        styledTextArea.textProperty().addListener(new TextChangeListener() {
            @Override
            public void handle(int pos, String removedText, String addedText) {
                if(!modifyingText) // change is not coming from this UndoManager
                    addChange(new Change(pos, removedText, addedText));
            }
        });
    }

    private void addChange(Change change) {
        // dismiss any undone changes
        chain.subList(currentIndex, chain.size()).clear();

        // try merging with the previous change,
        // otherwise add to the chain
        if(!tryMergeWithLast(change)) {
            chain.add(change);
            currentIndex++;
        }

        appendable = true;
    }

    private boolean tryMergeWithLast(Change change) {
        if(!appendable)
            return false;

        if(currentIndex == 0)
            return false;

        Change previousChange = chain.get(currentIndex - 1);
        Change merged = previousChange.mergeWith(change);
        if(merged == null)
            return false;

        chain.set(currentIndex - 1, merged);
        return true;
    }

    public void undo() {
        if (currentIndex > 0) {
            // Apply reverse change here
            Change change = chain.get(currentIndex - 1);
            modifyingText = true;
            this.styledTextArea.replaceText(change.start,
                    change.start + change.newText.length(),
                    change.oldText);
            modifyingText = false;
            currentIndex--;
            appendable = false;
        }
    }

    public void redo() {
        if (currentIndex < chain.size()) {
            // Apply change here
            Change change = chain.get(currentIndex);
            modifyingText = true;
            this.styledTextArea.replaceText(change.start,
                    change.start + change.oldText.length(),
                    change.newText);
            modifyingText = false;
            currentIndex++;
            appendable = false;
        }
    }

    public boolean canUndo() {
        return (currentIndex > 0);
    }

    public boolean canRedo() {
        return (currentIndex < chain.size());
    }

    public void reset() {
        chain.clear();
        currentIndex = 0;
    }
}