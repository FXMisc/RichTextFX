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

import static com.sun.javafx.PlatformUtil.isMac;
import static com.sun.javafx.scene.control.skin.resources.ControlResources.getString;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import codearea.control.NavigationActions.SelectionPolicy;
import codearea.control.StringChange;
import codearea.control.StyledTextArea;
import codearea.control.TwoLevelNavigator;
import codearea.skin.LineCell;
import codearea.skin.StyledTextAreaSkin;
import codearea.undo.UndoManager;
import codearea.undo.impl.ObservingUndoManager;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.text.HitInfo;


/**
 * Text area behavior.
 */
public class CodeAreaBehavior<S> extends BehaviorBase<StyledTextArea<S>> {

    /**
     * Possible dragging states.
     */
    private enum DragState {
        /** No dragging is happening. */
        NO_DRAG,

        /** Mouse has been pressed, but drag has not been detected yet. */
        POTENTIAL_DRAG,

        /** Drag in progress. */
        DRAG,
    }

    /**************************************************************************
     * Fields                                                                 *
     *************************************************************************/

    protected final StyledTextArea<S> styledTextArea;

    /**
     * Used to keep track of the most recent key event. This is used when
     * handling InputCharacter actions.
     */
    private KeyEvent lastEvent;

    private final UndoManager undoManager;

    private StyledTextAreaSkin<S> skin;
    private final ContextMenu contextMenu = new ContextMenu();

    /**
     * Indicates whether selection is being dragged by the user.
     */
    private DragState dragSelection = DragState.NO_DRAG;

    /**
     * Remembers horizontal position when traversing up / down.
     */
    private double targetCaretOffset = -1;
    public void clearTargetCaretOffset() {
        targetCaretOffset = -1;
    }
    private double getTargetCaretOffset() {
        if(targetCaretOffset == -1)
            targetCaretOffset = skin.getCaretOffsetX();
        return targetCaretOffset;
    }

    /**************************************************************************
     * Constructors                                                           *
     *************************************************************************/

    public CodeAreaBehavior(StyledTextArea<S> styledTextArea) {
        super(styledTextArea, CodeAreaBindings.BINDINGS);

        this.styledTextArea = styledTextArea;
        this.undoManager = new ObservingUndoManager<StringChange>(
                change -> styledTextArea.replaceText(change.getPosition(), change.getPosition() + change.getRemoved().length(), change.getInserted()), // redo lambda
                change -> styledTextArea.replaceText(change.getPosition(), change.getPosition() + change.getInserted().length(), change.getRemoved()), // undo lambda
                (change1, change2) -> change1.mergeWith(change2), // merge lambda
                styledTextArea.textChanges());
    }

    // XXX An unholy back-reference!
    public void setCodeAreaSkin(StyledTextAreaSkin<S> skin) {
        this.skin = skin;
    }

    /**************************************************************************
     * Key handling implementation                                            *
     *************************************************************************/

    /**
     * Records the last KeyEvent we saw.
     * @param e
     */
    @Override protected void callActionForEvent(KeyEvent e) {
        lastEvent = e;
        super.callActionForEvent(e);
    }

    @Override public void callAction(String name) {
        try {
            CodeAreaAction action = CodeAreaAction.valueOf(name);
            callAction(action);
        }
        catch(IllegalArgumentException e) {
            // Not one of code area actions.
            // Must be one of the traversing actions defined in supertype.
            super.callAction(name);
        }
    }

    private void callAction(CodeAreaAction action) {
        // ignore edit actions when not editable
        if(action.isEditAction() && !styledTextArea.isEditable())
            return;

        // invalidate remembered horizontal position
        // on every action except vertical navigation
        switch(action) {
            case PreviousLine:
            case NextLine:
            case PreviousPage:
            case NextPage:
            case SelectPreviousLine:
            case SelectNextLine:
            case SelectPreviousPage:
            case SelectNextPage:
                break;
            default:
                clearTargetCaretOffset();
        }

        switch(action) {
            case Left: left(); break;
            case Right: right(); break;
            case SelectLeft: selectLeft(); break;
            case SelectRight: selectRight(); break;

            case LeftWord: leftWord(SelectionPolicy.CLEAR); break;
            case RightWord: rightWord(SelectionPolicy.CLEAR); break;
            case SelectLeftWord: leftWord(SelectionPolicy.ADJUST); break;
            case SelectRightWord: rightWord(SelectionPolicy.ADJUST); break;
            case SelectLeftWordExtend: leftWord(SelectionPolicy.EXTEND); break;
            case SelectRightWordExtend: rightWord(SelectionPolicy.EXTEND); break;

            case LineStart: styledTextArea.lineStart(SelectionPolicy.CLEAR); break;
            case LineEnd: styledTextArea.lineEnd(SelectionPolicy.CLEAR); break;
            case SelectLineStart: styledTextArea.lineStart(SelectionPolicy.ADJUST); break;
            case SelectLineEnd: styledTextArea.lineEnd(SelectionPolicy.ADJUST); break;
            case SelectLineStartExtend: styledTextArea.lineStart(SelectionPolicy.EXTEND); break;
            case SelectLineEndExtend: styledTextArea.lineEnd(SelectionPolicy.EXTEND); break;

            case TextStart: styledTextArea.start(SelectionPolicy.CLEAR); break;
            case TextEnd: styledTextArea.end(SelectionPolicy.CLEAR); break;
            case SelectTextStart: styledTextArea.start(SelectionPolicy.ADJUST); break;
            case SelectTextEnd: styledTextArea.end(SelectionPolicy.ADJUST); break;
            case SelectTextStartExtend: styledTextArea.start(SelectionPolicy.EXTEND); break;
            case SelectTextEndExtend: styledTextArea.end(SelectionPolicy.EXTEND); break;

            case PreviousLine: previousLine(SelectionPolicy.CLEAR); break;
            case NextLine: nextLine(SelectionPolicy.CLEAR); break;
            case SelectPreviousLine: previousLine(SelectionPolicy.ADJUST); break;
            case SelectNextLine: nextLine(SelectionPolicy.ADJUST); break;

            case PreviousPage: previousPage(SelectionPolicy.CLEAR); break;
            case NextPage: nextPage(SelectionPolicy.CLEAR); break;
            case SelectPreviousPage: previousPage(SelectionPolicy.ADJUST); break;
            case SelectNextPage: nextPage(SelectionPolicy.ADJUST); break;

            case DeletePreviousChar: deleteBackward(); break;
            case DeleteNextChar: deleteForward(); break;

            case DeletePreviousWord: deletePreviousWord(); break;
            case DeleteNextWord: deleteNextWord(); break;

            case InsertNewLine: styledTextArea.replaceSelection("\n"); break;
            case InsertTab: styledTextArea.replaceSelection("\t"); break;
            case InputCharacter: defaultKeyTyped(lastEvent); break;

            case Cut: styledTextArea.cut(); break;
            case Copy: styledTextArea.copy(); break;
            case Paste: styledTextArea.paste(); break;

            case Undo: undoManager.undo(); break;
            case Redo: undoManager.redo(); break;

            case SelectAll: styledTextArea.selectAll(); break;
            case Unselect: styledTextArea.deselect(); break;

            case ToParent: forwardToParent(lastEvent); break;
        }
    }

    private void forwardToParent(KeyEvent event) {
        if (styledTextArea.getParent() != null) {
            styledTextArea.getParent().fireEvent(event);
        }
    }

    /**
     * The default handler for a key typed event, which is called when none of
     * the other key bindings match. This is the method which handles basic
     * text entry.
     * @param event not null
     */
    private void defaultKeyTyped(KeyEvent event) {
        // Sometimes we get events with no key character, in which case
        // we need to bail.
        String character = event.getCharacter();
        if (character.length() == 0)
            return;

        // Filter out control keys except control+Alt on PC or Alt on Mac
        if (event.isControlDown() || event.isAltDown() || (isMac() && event.isMetaDown())) {
            if (!((event.isControlDown() || isMac()) && event.isAltDown()))
                return;
        }

        // Ignore characters in the control range and the ASCII delete
        // character as well as meta key presses
        if (character.charAt(0) > 0x1F
                && character.charAt(0) != 0x7F
                && !event.isMetaDown()) { // Not sure about this one
            styledTextArea.replaceSelection(character);
        }
    }

    private void deleteBackward() {
        IndexRange selection = styledTextArea.getSelection();
        if(selection.getLength() == 0)
            styledTextArea.deletePreviousChar();
        else
            styledTextArea.replaceSelection("");
    }

    private void deleteForward() {
        IndexRange selection = styledTextArea.getSelection();
        if(selection.getLength() == 0)
            styledTextArea.deleteNextChar();
        else
            styledTextArea.replaceSelection("");
    }

    private void left() {
        IndexRange sel = styledTextArea.getSelection();
        if(sel.getLength() == 0)
            styledTextArea.previousChar(SelectionPolicy.CLEAR);
        else
            styledTextArea.positionCaret(sel.getStart(), SelectionPolicy.CLEAR);
    }

    private void right() {
        IndexRange sel = styledTextArea.getSelection();
        if(sel.getLength() == 0)
            styledTextArea.nextChar(SelectionPolicy.CLEAR);
        else
            styledTextArea.positionCaret(sel.getEnd(), SelectionPolicy.CLEAR);
    }

    private void selectLeft() {
        styledTextArea.previousChar(SelectionPolicy.ADJUST);
    }

    private void selectRight() {
        styledTextArea.nextChar(SelectionPolicy.ADJUST);
    }

    private void leftWord(SelectionPolicy selectionPolicy) {
        styledTextArea.previousWord(selectionPolicy);
    }

    private void rightWord(SelectionPolicy selectionPolicy) {
        styledTextArea.nextWord(selectionPolicy);
    }

    private void selectWord() {
        styledTextArea.previousWord(SelectionPolicy.CLEAR);
        styledTextArea.nextWord(SelectionPolicy.ADJUST);
    }

    private void deletePreviousWord() {
        int end = styledTextArea.getCaretPosition();

        if (end > 0) {
            styledTextArea.previousWord(SelectionPolicy.CLEAR);
            int start = styledTextArea.getCaretPosition();
            styledTextArea.replaceText(start, end, "");
        }
    }

    private void deleteNextWord() {
        int start = styledTextArea.getCaretPosition();

        if (start < styledTextArea.getLength()) {
            styledTextArea.nextWord(SelectionPolicy.CLEAR);
            int end = styledTextArea.getCaretPosition();
            styledTextArea.replaceText(start, end, "");
        }
    }

    private void downLines(int nLines, SelectionPolicy selectionPolicy) {
        TwoLevelNavigator<LineCell<S>>.Position currentLine = skin.currentVisualLine();
        TwoLevelNavigator<LineCell<S>>.Position targetLine = currentLine.offsetBy(nLines).clamp();
        if(!currentLine.sameAs(targetLine)) {
            goToVisualLine(targetLine, selectionPolicy);
        }
    }

    private void goToVisualLine(TwoLevelNavigator<LineCell<S>>.Position targetLine, SelectionPolicy selectionPolicy) {
        // compute new caret position
        HitInfo hit = skin.hit(targetLine, getTargetCaretOffset());
        int newCaretPos = hit.getInsertionIndex();

        // update model
        styledTextArea.positionCaret(newCaretPos, selectionPolicy);
    }

    private void previousLine(SelectionPolicy selectionPolicy) {
        downLines(-1, selectionPolicy);
    }

    private void nextLine(SelectionPolicy selectionPolicy) {
        downLines(1, selectionPolicy);
    }

    private void previousPage(SelectionPolicy selectionPolicy) {
        int currentRow = styledTextArea.caretRow.get();
        skin.showAsLast(currentRow);
        int targetRow = skin.getFirstVisibleIndex();
        goToVisualLine(skin.position(targetRow, 0), selectionPolicy);
    }

    private void nextPage(SelectionPolicy selectionPolicy) {
        int currentRow = styledTextArea.caretRow.get();
        skin.showAsFirst(currentRow);
        int targetRow = skin.getLastVisibleIndex();
        goToVisualLine(skin.position(targetRow, 0), selectionPolicy);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // don't respond if disabled
        if (styledTextArea.isDisabled())
            return;

        // ensure focus
        if (!styledTextArea.isFocused())
            styledTextArea.requestFocus();

        // cancel context menu
        if (contextMenu.isShowing())
            contextMenu.hide();

        switch(e.getButton()) {
            case PRIMARY: leftPress(e); break;
            case SECONDARY: rightPress(e); break;
            default: // do nothing
        }
    }

    private void leftPress(MouseEvent e) {
        LineCell<S> lineCell = (LineCell<S>) e.getSource();
        HitInfo hit = lineCell.hit(e);

        if(e.isShiftDown()) {
            // On Mac always extend selection,
            // switching anchor and caret if necessary.
            styledTextArea.positionCaret(hit.getInsertionIndex(), isMac() ? SelectionPolicy.EXTEND : SelectionPolicy.ADJUST);
        }
        else {
            switch (e.getClickCount()) {
                case 1: firstLeftPress(hit); break;
                case 2: selectWord(); break;
                case 3: styledTextArea.selectLine(); break;
                default: // no-op
            }
        }
    }

    private void firstLeftPress(HitInfo hit) {
        clearTargetCaretOffset();
        IndexRange selection = styledTextArea.getSelection();
        if (selection.getLength() != 0 &&
                hit.getCharIndex() >= selection.getStart() &&
                hit.getCharIndex() < selection.getEnd()) {
            // press inside selection
            dragSelection = DragState.POTENTIAL_DRAG;
        }
        else {
            dragSelection = DragState.NO_DRAG;
            styledTextArea.positionCaret(hit.getInsertionIndex(), SelectionPolicy.CLEAR);
        }
    }

    private void rightPress(MouseEvent e) {
        if (contextMenu.isShowing()) {
            contextMenu.hide();
        } else if (styledTextArea.getContextMenu() == null) {
            double screenX = e.getScreenX();
            double screenY = e.getScreenY();
            double sceneX = e.getSceneX();

            populateContextMenu(contextMenu);
            double menuWidth = contextMenu.prefWidth(-1);
            Screen currentScreen = com.sun.javafx.Utils.getScreenForPoint(screenX, 0);
            Rectangle2D bounds = currentScreen.getBounds();

            if (screenX < bounds.getMinX()) {
                styledTextArea.getProperties().put("CONTEXT_MENU_SCREEN_X", screenX);
                styledTextArea.getProperties().put("CONTEXT_MENU_SCENE_X", sceneX);
                contextMenu.show(styledTextArea, bounds.getMinX(), screenY);
            } else if (screenX + menuWidth > bounds.getMaxX()) {
                double leftOver = menuWidth - ( bounds.getMaxX() - screenX);
                styledTextArea.getProperties().put("CONTEXT_MENU_SCREEN_X", screenX);
                styledTextArea.getProperties().put("CONTEXT_MENU_SCENE_X", sceneX);
                contextMenu.show(styledTextArea, screenX - leftOver, screenY);
            } else {
                styledTextArea.getProperties().put("CONTEXT_MENU_SCREEN_X", 0);
                styledTextArea.getProperties().put("CONTEXT_MENU_SCENE_X", 0);
                contextMenu.show(styledTextArea, screenX, screenY);
            }
        }
    }

    public void dragDetected(MouseEvent e) {
        if(dragSelection == DragState.POTENTIAL_DRAG)
            dragSelection = DragState.DRAG;
    }

    public void mouseDragOver(MouseDragEvent e) {
        // don't respond if disabled
        if(styledTextArea.isDisabled())
            return;

        // only respond to primary button alone
        if(e.getButton() != MouseButton.PRIMARY || e.isMiddleButtonDown() || e.isSecondaryButtonDown())
            return;

        // get the position within text
        LineCell<S> lineCell = (LineCell<S>) e.getSource();
        HitInfo hit = lineCell.hit(e);

        if (dragSelection == DragState.DRAG) {
            styledTextArea.positionCaretIndependently(hit.getInsertionIndex());
        }
        else {
            styledTextArea.positionCaret(hit.getInsertionIndex(), SelectionPolicy.ADJUST);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        switch(dragSelection) {
            case POTENTIAL_DRAG:
                // drag didn't happen, position caret
                LineCell<S> lineCell = (LineCell<S>) e.getSource();
                HitInfo hit = lineCell.hit(e);
                styledTextArea.positionCaret(hit.getInsertionIndex(), SelectionPolicy.CLEAR);
                break;
            case DRAG:
                // do nothing, handled by mouseDragReleased
            case NO_DRAG:
                // do nothing, caret already repositioned in mousePressed
        }
        dragSelection = DragState.NO_DRAG;
    }

    public void mouseDragReleased(final MouseDragEvent e) {
        // don't respond if disabled
        if(styledTextArea.isDisabled())
            return;

        if(dragSelection == DragState.DRAG) {
            // get the position within text
            LineCell<S> lineCell = (LineCell<S>) e.getSource();
            HitInfo hit = lineCell.hit(e);

            styledTextArea.moveSelectedText(hit.getInsertionIndex());
        }
    }

    public boolean canUndo() {
        return undoManager.canUndo();
    }

    public boolean canRedo() {
        return undoManager.canRedo();
    }

    class ContextMenuItem extends MenuItem {
        ContextMenuItem(final String action) {
            super(getString("TextInputControl.menu." + action));
            setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent e) {
                    callAction(action);
                }
            });
        }
    }

    final MenuItem undoMI   = new ContextMenuItem("Undo");
    final MenuItem redoMI   = new ContextMenuItem("Redo");
    final MenuItem cutMI    = new ContextMenuItem("Cut");
    final MenuItem copyMI   = new ContextMenuItem("Copy");
    final MenuItem pasteMI  = new ContextMenuItem("Paste");
    final MenuItem deleteMI = new ContextMenuItem("DeleteSelection");
    final MenuItem selectWordMI = new ContextMenuItem("SelectWord");
    final MenuItem selectAllMI = new ContextMenuItem("SelectAll");
    final MenuItem separatorMI = new SeparatorMenuItem();

    public void populateContextMenu(ContextMenu contextMenu) {
        boolean hasSelection = (styledTextArea.getSelection().getLength() > 0);
        ObservableList<MenuItem> items = contextMenu.getItems();

        if (styledTextArea.isEditable()) {
            items.setAll(undoMI, redoMI, cutMI, copyMI, pasteMI, deleteMI,
                         separatorMI, selectAllMI);
        } else {
            items.setAll(copyMI, separatorMI, selectAllMI);
        }
        undoMI.setDisable(!canUndo());
        redoMI.setDisable(!canRedo());
        cutMI.setDisable(!hasSelection);
        copyMI.setDisable(!hasSelection);
        pasteMI.setDisable(!Clipboard.getSystemClipboard().hasString());
        deleteMI.setDisable(!hasSelection);
    }
}
