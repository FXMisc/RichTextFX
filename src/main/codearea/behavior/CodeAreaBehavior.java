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
import codearea.control.CodeArea;
import codearea.control.CodeAreaHelper;
import codearea.control.Line;
import codearea.control.CodeAreaHelper.SelectionPolicy;
import codearea.skin.CodeAreaSkin;
import codearea.skin.LineCell;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.text.HitInfo;


/**
 * Text area behavior.
 */
public class CodeAreaBehavior extends BehaviorBase<CodeArea> {

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

    protected final CodeArea codeArea;
    protected final CodeAreaHelper codeAreaHelper;

    /**
     * Used to keep track of the most recent key event. This is used when
     * handling InputCharacter actions.
     */
    private KeyEvent lastEvent;

    private final UndoManager undoManager;

    private CodeAreaSkin skin;
    private final ContextMenu contextMenu = new ContextMenu();

    /**
     * Indicates whether selection is being dragged by the user.
     */
    private DragState dragSelection = DragState.NO_DRAG;

    /**
     * Remembers horizontal position when traversing up / down.
     */
    private int targetCaretCol = -1;
    public void clearTargetCaretColumn() {
    	targetCaretCol = -1;
    }
    private int getTargetCaretColumn() {
    	if(targetCaretCol == -1)
    		targetCaretCol = codeArea.caretCol.get();
    	return targetCaretCol;
    }

    /**************************************************************************
     * Constructors                                                           *
     *************************************************************************/

    public CodeAreaBehavior(CodeArea codeArea) {
        super(codeArea, CodeAreaBindings.BINDINGS);

        this.codeArea = codeArea;
        this.codeAreaHelper = new CodeAreaHelper(codeArea);
        this.undoManager = new UndoManager(codeArea);
    }

    // XXX An unholy back-reference!
    public void setCodeAreaSkin(CodeAreaSkin skin) {
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
        if(action.isEditAction() && !codeArea.isEditable())
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
        		clearTargetCaretColumn();
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

            case LineStart: codeAreaHelper.lineStart(SelectionPolicy.CLEAR); break;
            case LineEnd: codeAreaHelper.lineEnd(SelectionPolicy.CLEAR); break;
            case SelectLineStart: codeAreaHelper.lineStart(SelectionPolicy.ADJUST); break;
            case SelectLineEnd: codeAreaHelper.lineEnd(SelectionPolicy.ADJUST); break;
            case SelectLineStartExtend: codeAreaHelper.lineStart(SelectionPolicy.EXTEND); break;
            case SelectLineEndExtend: codeAreaHelper.lineEnd(SelectionPolicy.EXTEND); break;

            case TextStart: codeAreaHelper.start(SelectionPolicy.CLEAR); break;
            case TextEnd: codeAreaHelper.end(SelectionPolicy.CLEAR); break;
            case SelectTextStart: codeAreaHelper.start(SelectionPolicy.ADJUST); break;
            case SelectTextEnd: codeAreaHelper.end(SelectionPolicy.ADJUST); break;
            case SelectTextStartExtend: codeAreaHelper.start(SelectionPolicy.EXTEND); break;
            case SelectTextEndExtend: codeAreaHelper.end(SelectionPolicy.EXTEND); break;

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

            case InsertNewLine: codeAreaHelper.replaceSelection("\n"); break;
            case InsertTab: codeAreaHelper.replaceSelection("\t"); break;
            case InputCharacter: defaultKeyTyped(lastEvent); break;

            case Cut: codeAreaHelper.cut(); break;
            case Copy: codeAreaHelper.copy(); break;
            case Paste: codeAreaHelper.paste(); break;

            case Undo: undoManager.undo(); break;
            case Redo: undoManager.redo(); break;

            case SelectAll: codeAreaHelper.selectAll(); break;
            case Unselect: codeAreaHelper.deselect(); break;

            case ToParent: forwardToParent(lastEvent); break;
        }
    }

    private void forwardToParent(KeyEvent event) {
        if (codeArea.getParent() != null) {
            codeArea.getParent().fireEvent(event);
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
            codeAreaHelper.replaceSelection(character);
        }
    }

    private void deleteBackward() {
        IndexRange selection = codeArea.getSelection();
        if(selection.getLength() == 0)
            codeAreaHelper.deletePreviousChar();
        else
            codeAreaHelper.replaceSelection("");
    }

    private void deleteForward() {
        IndexRange selection = codeArea.getSelection();
        if(selection.getLength() == 0)
            codeAreaHelper.deleteNextChar();
        else
            codeAreaHelper.replaceSelection("");
    }

    private void left() {
    	IndexRange sel = codeArea.getSelection();
    	if(sel.getLength() == 0)
    		codeAreaHelper.previousChar(SelectionPolicy.CLEAR);
    	else
    		codeAreaHelper.positionCaret(sel.getStart(), SelectionPolicy.CLEAR);
    }

    private void right() {
    	IndexRange sel = codeArea.getSelection();
    	if(sel.getLength() == 0)
    		codeAreaHelper.nextChar(SelectionPolicy.CLEAR);
    	else
    		codeAreaHelper.positionCaret(sel.getEnd(), SelectionPolicy.CLEAR);
    }

    private void selectLeft() {
    	codeAreaHelper.previousChar(SelectionPolicy.ADJUST);
    }

    private void selectRight() {
    	codeAreaHelper.nextChar(SelectionPolicy.ADJUST);
    }

    private void leftWord(SelectionPolicy selectionPolicy) {
        codeAreaHelper.previousWord(selectionPolicy);
    }

    private void rightWord(SelectionPolicy selectionPolicy) {
        codeAreaHelper.nextWord(selectionPolicy);
    }

    private void selectWord() {
    	codeAreaHelper.previousWord(SelectionPolicy.CLEAR);
    	codeAreaHelper.nextWord(SelectionPolicy.ADJUST);
    }

    private void deletePreviousWord() {
        int end = codeArea.getCaretPosition();

        if (end > 0) {
        	codeAreaHelper.previousWord(SelectionPolicy.CLEAR);
            int start = codeArea.getCaretPosition();
            codeArea.replaceText(start, end, "");
        }
    }

    private void deleteNextWord() {
        int start = codeArea.getCaretPosition();

        if (start < codeArea.getLength()) {
            codeAreaHelper.nextWord(SelectionPolicy.CLEAR);
            int end = codeArea.getCaretPosition();
            codeArea.replaceText(start, end, "");
        }
    }

    private void downLines(int nLines, SelectionPolicy selectionPolicy) {
    	// get target line number
    	int targetRow = codeArea.caretRow.get() + nLines;
    	targetRow = Math.min(targetRow, codeArea.getLines().size()-1);
    	targetRow = Math.max(targetRow, 0);
    	if(targetRow == codeArea.caretRow.get())
    		return;

    	// compute new caret position
    	Line targetLine = codeArea.getLines().get(targetRow);
    	int targetCaretCol = Math.min(getTargetCaretColumn(), targetLine.length());
    	int newCaretPos = codeArea.getLineOffset(targetRow) + targetCaretCol;

    	// update model
        codeAreaHelper.positionCaret(newCaretPos, selectionPolicy);
    }

    private void previousLine(SelectionPolicy selectionPolicy) {
        downLines(-1, selectionPolicy);
    }

    private void nextLine(SelectionPolicy selectionPolicy) {
        downLines(1, selectionPolicy);
    }

    private void previousPage(SelectionPolicy selectionPolicy) {
        downLines(-skin.getDisplayedRowCount(), selectionPolicy);
    }

    private void nextPage(SelectionPolicy selectionPolicy) {
        downLines(skin.getDisplayedRowCount(), selectionPolicy);
    }

    @Override
	public void mousePressed(MouseEvent e) {
        // don't respond if disabled
        if (codeArea.isDisabled())
        	return;

        // ensure focus
        if (!codeArea.isFocused())
            codeArea.requestFocus();

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
        LineCell lineCell = (LineCell) e.getSource();
        HitInfo hit = lineCell.hit(e);

        if(e.isShiftDown()) {
            // On Mac always extend selection,
        	// switching anchor and caret if necessary.
            codeAreaHelper.positionCaret(hit.getInsertionIndex(), isMac() ? SelectionPolicy.EXTEND : SelectionPolicy.ADJUST);
        }
        else {
            switch (e.getClickCount()) {
                case 1: firstLeftPress(hit); break;
                case 2: selectWord(); break;
                case 3: codeAreaHelper.selectLine(); break;
                default: // no-op
            }
        }
    }

    private void firstLeftPress(HitInfo hit) {
    	clearTargetCaretColumn();
        IndexRange selection = codeArea.getSelection();
    	if (selection.getLength() != 0 &&
        		hit.getCharIndex() >= selection.getStart() &&
        		hit.getCharIndex() < selection.getEnd()) {
    	    // press inside selection
            dragSelection = DragState.POTENTIAL_DRAG;
        }
    	else {
            dragSelection = DragState.NO_DRAG;
			codeAreaHelper.positionCaret(hit.getInsertionIndex(), SelectionPolicy.CLEAR);
    	}
    }

    private void rightPress(MouseEvent e) {
        if (contextMenu.isShowing()) {
            contextMenu.hide();
        } else if (codeArea.getContextMenu() == null) {
            double screenX = e.getScreenX();
            double screenY = e.getScreenY();
            double sceneX = e.getSceneX();

            populateContextMenu(contextMenu);
            double menuWidth = contextMenu.prefWidth(-1);
            Screen currentScreen = com.sun.javafx.Utils.getScreenForPoint(screenX, 0);
            Rectangle2D bounds = currentScreen.getBounds();

            if (screenX < bounds.getMinX()) {
                codeArea.getProperties().put("CONTEXT_MENU_SCREEN_X", screenX);
                codeArea.getProperties().put("CONTEXT_MENU_SCENE_X", sceneX);
                contextMenu.show(codeArea, bounds.getMinX(), screenY);
            } else if (screenX + menuWidth > bounds.getMaxX()) {
                double leftOver = menuWidth - ( bounds.getMaxX() - screenX);
                codeArea.getProperties().put("CONTEXT_MENU_SCREEN_X", screenX);
                codeArea.getProperties().put("CONTEXT_MENU_SCENE_X", sceneX);
                contextMenu.show(codeArea, screenX - leftOver, screenY);
            } else {
                codeArea.getProperties().put("CONTEXT_MENU_SCREEN_X", 0);
                codeArea.getProperties().put("CONTEXT_MENU_SCENE_X", 0);
                contextMenu.show(codeArea, screenX, screenY);
            }
        }
    }

    public void dragDetected(MouseEvent e) {
    	if(dragSelection == DragState.POTENTIAL_DRAG)
    		dragSelection = DragState.DRAG;
    }

    public void mouseDragOver(MouseDragEvent e) {
        // don't respond if disabled
        if(codeArea.isDisabled())
        	return;

        // only respond to primary button alone
        if(e.getButton() != MouseButton.PRIMARY || e.isMiddleButtonDown() || e.isSecondaryButtonDown())
        	return;

        // get the position within text
        LineCell lineCell = (LineCell) e.getSource();
        HitInfo hit = lineCell.hit(e);

        if (dragSelection == DragState.DRAG) {
        	codeArea.positionCaretIndependently(hit.getInsertionIndex());
        }
        else {
            codeAreaHelper.positionCaret(hit.getInsertionIndex(), SelectionPolicy.ADJUST);
        }
    }

    @Override
	public void mouseReleased(MouseEvent e) {
    	switch(dragSelection) {
			case POTENTIAL_DRAG:
				// drag didn't happen, position caret
		        LineCell lineCell = (LineCell) e.getSource();
		        HitInfo hit = lineCell.hit(e);
		        codeAreaHelper.positionCaret(hit.getInsertionIndex(), SelectionPolicy.CLEAR);
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
        if(codeArea.isDisabled())
        	return;

        if(dragSelection == DragState.DRAG) {
            // get the position within text
            LineCell lineCell = (LineCell) e.getSource();
            HitInfo hit = lineCell.hit(e);

            codeAreaHelper.moveSelectedText(hit.getInsertionIndex());
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
        boolean hasSelection = (codeArea.getSelection().getLength() > 0);
        ObservableList<MenuItem> items = contextMenu.getItems();

        if (codeArea.isEditable()) {
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
