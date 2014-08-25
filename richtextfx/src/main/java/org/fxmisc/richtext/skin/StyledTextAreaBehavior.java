package org.fxmisc.richtext.skin;

import static com.sun.javafx.PlatformUtil.*;
import static javafx.scene.input.MouseDragEvent.*;
import static javafx.scene.input.MouseEvent.*;
import static org.fxmisc.richtext.TwoDimensional.Bias.*;
import static org.reactfx.util.Tuples.*;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.geometry.Bounds;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;

import org.fxmisc.richtext.NavigationActions.SelectionPolicy;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TwoDimensional.Position;
import org.fxmisc.richtext.util.skin.Behavior;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;

import com.sun.javafx.scene.text.HitInfo;


@FunctionalInterface
interface Action extends Consumer<StyledTextAreaBehavior> {
    default boolean isEdit() { return false; }
    default boolean isVerticalNavigation() { return false; }
}

@FunctionalInterface
interface EditAction extends Action {
    @Override
    default boolean isEdit() { return true; }
}

/**
 * Actions that use the remembered caret offset.
 */
@FunctionalInterface
interface VerticalNavigation extends Action {
    @Override
    default boolean isVerticalNavigation() { return true; }
}

/**
 * Controller for StyledTextArea.
 */
public class StyledTextAreaBehavior implements Behavior {

    static final class Actions {
        public static final Action Left = StyledTextAreaBehavior::left;
        public static final Action Right = StyledTextAreaBehavior::right;
        public static final Action SelectLeft = StyledTextAreaBehavior::selectLeft;
        public static final Action SelectRight = StyledTextAreaBehavior::selectRight;

        public static final Action LeftWord = b -> b.leftWord(SelectionPolicy.CLEAR);
        public static final Action RightWord = b -> b.rightWord(SelectionPolicy.CLEAR);
        public static final Action SelectLeftWord = b -> b.leftWord(SelectionPolicy.ADJUST);
        public static final Action SelectRightWord = b -> b.rightWord(SelectionPolicy.ADJUST);
        public static final Action SelectLeftWordExtend = b -> b.leftWord(SelectionPolicy.EXTEND);
        public static final Action SelectRightWordExtend = b -> b.rightWord(SelectionPolicy.EXTEND);

        public static final Action LineStart = b -> b.area.lineStart(SelectionPolicy.CLEAR);
        public static final Action LineEnd = b -> b.area.lineEnd(SelectionPolicy.CLEAR);
        public static final Action SelectLineStart = b -> b.area.lineStart(SelectionPolicy.ADJUST);
        public static final Action SelectLineEnd = b -> b.area.lineEnd(SelectionPolicy.ADJUST);
        public static final Action SelectLineStartExtend = b -> b.area.lineStart(SelectionPolicy.EXTEND);
        public static final Action SelectLineEndExtend = b -> b.area.lineEnd(SelectionPolicy.EXTEND);

        public static final Action TextStart = b -> b.area.start(SelectionPolicy.CLEAR);
        public static final Action TextEnd = b -> b.area.end(SelectionPolicy.CLEAR);
        public static final Action SelectTextStart = b -> b.area.start(SelectionPolicy.ADJUST);
        public static final Action SelectTextEnd = b -> b.area.end(SelectionPolicy.ADJUST);
        public static final Action SelectTextStartExtend = b -> b.area.start(SelectionPolicy.EXTEND);
        public static final Action SelectTextEndExtend = b -> b.area.end(SelectionPolicy.EXTEND);

        public static final VerticalNavigation PreviousLine = b -> b.previousLine(SelectionPolicy.CLEAR);
        public static final VerticalNavigation NextLine = b -> b.nextLine(SelectionPolicy.CLEAR);
        public static final VerticalNavigation SelectPreviousLine = b -> b. previousLine(SelectionPolicy.ADJUST);
        public static final VerticalNavigation SelectNextLine = b -> b.nextLine(SelectionPolicy.ADJUST);

        public static final VerticalNavigation PreviousPage = b -> b.previousPage(SelectionPolicy.CLEAR);
        public static final VerticalNavigation NextPage = b -> b.nextPage(SelectionPolicy.CLEAR);
        public static final VerticalNavigation SelectPreviousPage = b -> b.previousPage(SelectionPolicy.ADJUST);
        public static final VerticalNavigation SelectNextPage = b -> b.nextPage(SelectionPolicy.ADJUST);

        public static final EditAction DeletePreviousChar = StyledTextAreaBehavior::deleteBackward;
        public static final EditAction DeleteNextChar = StyledTextAreaBehavior::deleteForward;

        public static final EditAction DeletePreviousWord = StyledTextAreaBehavior::deletePreviousWord;
        public static final EditAction DeleteNextWord = StyledTextAreaBehavior::deleteNextWord;

        public static final EditAction InsertNewLine = b -> b.area.replaceSelection("\n");
        public static final EditAction InsertTab = b -> b.area.replaceSelection("\t");
        public static final Function<KeyEvent, EditAction> InputCharacter = e -> b -> b.keyTyped(e);

        public static final EditAction Cut = b -> b.area.cut();
        public static final Action Copy = b -> b.area.copy();
        public static final EditAction Paste = b -> b.area.paste();

        public static final EditAction Undo = b -> b.area.undo();
        public static final EditAction Redo = b -> b.area.redo();

        public static final Action SelectAll = b -> b.area.selectAll();
        public static final Action Unselect = b -> b.area.deselect();

        // does nothing, but causes the event to be consumed
        public static final Action Consume = b -> {};
    }

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

    /* ********************************************************************** *
     * Fields                                                                 *
     * ********************************************************************** */

    private final StyledTextArea<?> area;
    private final StyledTextAreaVisual<?> visual;

    private final Subscription subscription;


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
            targetCaretOffset = visual.getCaretOffsetX();
        return targetCaretOffset;
    }

    /* ********************************************************************** *
     * Constructors                                                           *
     * ********************************************************************** */

    public StyledTextAreaBehavior(
            StyledTextArea<?> styledTextArea,
            StyledTextAreaVisual<?> visual) {
        this.area = styledTextArea;
        this.visual = visual;
        subscription = Subscription.multi(
                visual.cellMouseEvents()
                        .watch(pair -> pair.exec(this::handleMouseEvent), Throwable::printStackTrace),
                EventStreams.eventsOf(area, MouseEvent.ANY)
                        .watch(this::handleMouseEvent, Throwable::printStackTrace),
                EventStreams.eventsOf(area, KeyEvent.ANY)
                        .watch(this::handleKeyEvent, Throwable::printStackTrace));
    }

    /* ********************************************************************** *
     * Public API (from Behavior)                                             *
     * ********************************************************************** */

    @Override
    public void dispose() {
        subscription.unsubscribe();
    }

    /* ********************************************************************** *
     * Key handling implementation                                            *
     * ********************************************************************** */

    private void handleKeyEvent(KeyEvent e) {
        actionForEvent(e).ifPresent(action -> {
            callAction(action);
            e.consume();
        });
    }

    private Optional<Action> actionForEvent(KeyEvent e) {
        return KeyBindings.BINDINGS.stream()
                .map(b -> t(b, b.getSpecificity(e)))
                .filter(t -> t._2 > 0)
                .reduce((a, b) -> a._2 > b._2 ? a : b)
                .map(t -> t._1.getAction(e));
    }

    private void callAction(Action action) {
        // ignore edit actions when not editable
        if(action.isEdit() && !area.isEditable()) {
            return;
        }

        // invalidate remembered horizontal position
        // on every action except vertical navigation
        if(!action.isVerticalNavigation()) {
            clearTargetCaretOffset();
        }

        action.accept(this);
    }

    private void keyTyped(KeyEvent event) {
        // filter out control keys
        if(event.isControlDown() || event.isAltDown() || event.isMetaDown()) {
            return;
        }

        String text = event.getCharacter();
        int n = text.length();

        if(n == 0) {
            return;
        }

        for(int i = 0; i < n; ++i) {
            if(!isLegal(text.charAt(i))) {
                return;
            }
        }

        area.replaceSelection(text);
    }

    private static boolean isLegal(char c) {
        return !Character.isISOControl(c)
                || c == '\t';
    }

    private void deleteBackward() {
        IndexRange selection = area.getSelection();
        if(selection.getLength() == 0) {
            area.deletePreviousChar();
        } else {
            area.replaceSelection("");
        }
    }

    private void deleteForward() {
        IndexRange selection = area.getSelection();
        if(selection.getLength() == 0) {
            area.deleteNextChar();
        } else {
            area.replaceSelection("");
        }
    }

    private void left() {
        IndexRange sel = area.getSelection();
        if(sel.getLength() == 0) {
            area.previousChar(SelectionPolicy.CLEAR);
        } else {
            area.moveTo(sel.getStart(), SelectionPolicy.CLEAR);
        }
    }

    private void right() {
        IndexRange sel = area.getSelection();
        if(sel.getLength() == 0) {
            area.nextChar(SelectionPolicy.CLEAR);
        } else {
            area.moveTo(sel.getEnd(), SelectionPolicy.CLEAR);
        }
    }

    private void selectLeft() {
        area.previousChar(SelectionPolicy.ADJUST);
    }

    private void selectRight() {
        area.nextChar(SelectionPolicy.ADJUST);
    }

    private void leftWord(SelectionPolicy selectionPolicy) {
        area.previousWord(selectionPolicy);
    }

    private void rightWord(SelectionPolicy selectionPolicy) {
        area.nextWord(selectionPolicy);
    }

    private void selectWord() {
        area.previousWord(SelectionPolicy.CLEAR);
        area.nextWord(SelectionPolicy.ADJUST);
    }

    private void deletePreviousWord() {
        int end = area.getCaretPosition();

        if (end > 0) {
            area.previousWord(SelectionPolicy.CLEAR);
            int start = area.getCaretPosition();
            area.replaceText(start, end, "");
        }
    }

    private void deleteNextWord() {
        int start = area.getCaretPosition();

        if (start < area.getLength()) {
            area.nextWord(SelectionPolicy.CLEAR);
            int end = area.getCaretPosition();
            area.replaceText(start, end, "");
        }
    }

    private void downLines(int nLines, SelectionPolicy selectionPolicy) {
        Position currentLine = visual.currentLine();
        Position targetLine = currentLine.offsetBy(nLines, Forward).clamp();
        if(!currentLine.sameAs(targetLine)) {
            // compute new caret position
            int newCaretPos = visual.getInsertionIndex(getTargetCaretOffset(), targetLine);

            // update model
            area.moveTo(newCaretPos, selectionPolicy);
        }
    }

    private void previousLine(SelectionPolicy selectionPolicy) {
        downLines(-1, selectionPolicy);
    }

    private void nextLine(SelectionPolicy selectionPolicy) {
        downLines(1, selectionPolicy);
    }

    private void previousPage(SelectionPolicy selectionPolicy) {
        visual.followCaret(); // make sure caret is in the viewport
        double height = visual.getViewportHeight();
        Bounds caretBounds = visual.getCaretBounds().get();
        double caretMidY = caretBounds.getMinY() + caretBounds.getHeight() / 2;

        int newCaretPos = visual.getInsertionIndex(getTargetCaretOffset(), caretMidY - height);
        visual.show(-height);
        area.moveTo(newCaretPos, selectionPolicy);
    }

    private void nextPage(SelectionPolicy selectionPolicy) {
        visual.followCaret(); // make sure caret is in the viewport
        double height = visual.getViewportHeight();
        Bounds caretBounds = visual.getCaretBounds().get();
        double caretMidY = caretBounds.getMinY() + caretBounds.getHeight() / 2;

        int newCaretPos = visual.getInsertionIndex(getTargetCaretOffset(), caretMidY + height);
        visual.show(2*height);
        area.moveTo(newCaretPos, selectionPolicy);
    }

    /**
     * Handle mouse event on void space, i.e. beyond cells.
     */
    private void handleMouseEvent(MouseEvent e) {
        if(e.getEventType() == MOUSE_PRESSED && e.getButton() == MouseButton.PRIMARY) {
            area.requestFocus();
            area.end(SelectionPolicy.CLEAR);
            e.consume();
        }
    }

    private void handleMouseEvent(ParagraphBox<?> cell, MouseEvent e) {
        if(e.getEventType() == MOUSE_PRESSED) {
            mousePressed(cell, e);
            e.consume();
        } else if(e.getEventType() == DRAG_DETECTED) {
            // startFullDrag() causes subsequent drag events to be
            // received by corresponding ParagraphCells, instead of all
            // events being delivered to the original ParagraphCell.
            cell.getScene().startFullDrag();
            dragDetected(e);
            e.consume();
        } else if(e.getEventType() == MOUSE_DRAG_OVER) {
            mouseDragOver(cell, (MouseDragEvent) e);
            e.consume();
        } else if(e.getEventType() == MOUSE_DRAG_RELEASED) {
            mouseDragReleased(cell, (MouseDragEvent) e);
            e.consume();
        } else if(e.getEventType() == MOUSE_RELEASED) {
            mouseReleased(cell, e);
            e.consume();
        }
    }

    private void mousePressed(ParagraphBox<?> cell, MouseEvent e) {
        // don't respond if disabled
        if(area.isDisabled()) {
            return;
        }

        // ensure focus
        area.requestFocus();

        switch(e.getButton()) {
            case PRIMARY: leftPress(cell, e); break;
            default: // do nothing
        }
    }

    private void leftPress(ParagraphBox<?> cell, MouseEvent e) {
        HitInfo hit = hitCell(cell, e);

        if(e.isShiftDown()) {
            // On Mac always extend selection,
            // switching anchor and caret if necessary.
            area.moveTo(hit.getInsertionIndex(), isMac() ? SelectionPolicy.EXTEND : SelectionPolicy.ADJUST);
        } else {
            switch (e.getClickCount()) {
                case 1: firstLeftPress(hit); break;
                case 2: selectWord(); break;
                case 3: area.selectLine(); break;
                default: // do nothing
            }
        }
    }

    private void firstLeftPress(HitInfo hit) {
        clearTargetCaretOffset();
        IndexRange selection = area.getSelection();
        if(selection.getLength() != 0 &&
                hit.getCharIndex() >= selection.getStart() &&
                hit.getCharIndex() < selection.getEnd()) {
            // press inside selection
            dragSelection = DragState.POTENTIAL_DRAG;
        } else {
            dragSelection = DragState.NO_DRAG;
            area.moveTo(hit.getInsertionIndex(), SelectionPolicy.CLEAR);
        }
    }

    private void dragDetected(MouseEvent e) {
        if(dragSelection == DragState.POTENTIAL_DRAG) {
            dragSelection = DragState.DRAG;
        }
    }

    private void mouseDragOver(ParagraphBox<?> cell, MouseDragEvent e) {
        // don't respond if disabled
        if(area.isDisabled()) {
            return;
        }

        // only respond to primary button alone
        if(e.getButton() != MouseButton.PRIMARY || e.isMiddleButtonDown() || e.isSecondaryButtonDown()) {
            return;
        }

        // get the position within text
        HitInfo hit = hitCell(cell, e);

        if(dragSelection == DragState.DRAG) {
            area.positionCaret(hit.getInsertionIndex());
        } else {
            area.moveTo(hit.getInsertionIndex(), SelectionPolicy.ADJUST);
        }
    }

    private void mouseReleased(ParagraphBox<?> cell, MouseEvent e) {
        switch(dragSelection) {
            case POTENTIAL_DRAG:
                // drag didn't happen, position caret
                HitInfo hit = hitCell(cell, e);
                area.moveTo(hit.getInsertionIndex(), SelectionPolicy.CLEAR);
                break;
            case DRAG:
                // do nothing, handled by mouseDragReleased
            case NO_DRAG:
                // do nothing, caret already repositioned in mousePressed
        }
        dragSelection = DragState.NO_DRAG;
    }

    private void mouseDragReleased(ParagraphBox<?> cell, MouseDragEvent e) {
        // don't respond if disabled
        if(area.isDisabled()) {
            return;
        }

        if(dragSelection == DragState.DRAG) {
            // get the position within text
            HitInfo hit = hitCell(cell, e);

            area.moveSelectedText(hit.getInsertionIndex());
        }
    }

    private HitInfo hitCell(ParagraphBox<?> cell, MouseEvent e) {
        int cellIdx = cell.getIndex();
        int cellOffset = area.position(cellIdx, 0).toOffset();
        return cell.hit(e).map(hit -> {
            hit.setCharIndex(hit.getCharIndex() + cellOffset);
            return hit;
        }).orElseGet(() -> leadingEdgeOf(cellOffset + cell.getParagraph().length()));
    }

    private static HitInfo leadingEdgeOf(int charIdx) {
        HitInfo hit = new HitInfo();
        hit.setCharIndex(charIdx);
        hit.setLeading(true);
        return hit;
    }
}
