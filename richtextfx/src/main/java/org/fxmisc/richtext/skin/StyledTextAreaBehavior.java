package org.fxmisc.richtext.skin;

import static com.sun.javafx.PlatformUtil.*;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyCombination.*;
import static javafx.scene.input.KeyEvent.*;
import static javafx.scene.input.MouseDragEvent.*;
import static javafx.scene.input.MouseEvent.*;
import static org.fxmisc.richtext.TwoDimensional.Bias.*;
import static org.fxmisc.wellbehaved.input.EventPattern.*;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.control.IndexRange;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;

import org.fxmisc.richtext.NavigationActions.SelectionPolicy;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TwoDimensional.Position;
import org.fxmisc.wellbehaved.input.EventHandlerHelper;
import org.fxmisc.wellbehaved.input.EventHandlerTemplate;
import org.fxmisc.wellbehaved.skin.Behavior;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.scene.text.HitInfo;

/**
 * Controller for StyledTextArea.
 */
public class StyledTextAreaBehavior implements Behavior {

    private static final EventHandlerTemplate<StyledTextAreaBehavior, InputEvent> TEMPLATE;
    static {
        SelectionPolicy selPolicy = PlatformUtil.isMac()
                ? SelectionPolicy.EXTEND
                : SelectionPolicy.ADJUST;

        EventHandlerTemplate<StyledTextAreaBehavior, InputEvent> edits = EventHandlerTemplate
                .<StyledTextAreaBehavior, InputEvent, KeyEvent>
                // deletion
                 on(keyPressed(DELETE))                   .act(StyledTextAreaBehavior::deleteForward)
                .on(keyPressed(BACK_SPACE))               .act(StyledTextAreaBehavior::deleteBackward)
                .on(keyPressed(DELETE,     SHORTCUT_DOWN)).act(StyledTextAreaBehavior::deleteNextWord)
                .on(keyPressed(BACK_SPACE, SHORTCUT_DOWN)).act(StyledTextAreaBehavior::deletePrevWord)
                // cut
                .on(keyPressed(CUT))               .act((b, e) -> b.area.cut())
                .on(keyPressed(X, SHORTCUT_DOWN))  .act((b, e) -> b.area.cut())
                .on(keyPressed(DELETE, SHIFT_DOWN)).act((b, e) -> b.area.cut())
                // paste
                .on(keyPressed(PASTE))             .act((b, e) -> b.area.paste())
                .on(keyPressed(V, SHORTCUT_DOWN))  .act((b, e) -> b.area.paste())
                .on(keyPressed(INSERT, SHIFT_DOWN)).act((b, e) -> b.area.paste())
                // tab & newline
                .on(keyPressed(ENTER)).act((b, e) -> b.area.replaceSelection("\n"))
                .on(keyPressed(TAB))  .act((b, e) -> b.area.replaceSelection("\t"))
                // undo/redo,
                .on(keyPressed(Z, SHORTCUT_DOWN))            .act((b, e) -> b.area.undo())
                .on(keyPressed(Y, SHORTCUT_DOWN))            .act((b, e) -> b.area.redo())
                .on(keyPressed(Z, SHORTCUT_DOWN, SHIFT_DOWN)).act((b, e) -> b.area.redo())
                // Consume KEY_TYPED events for Enter and Tab,
                // because they are already handled as KEY_PRESSED
                .on(KEY_TYPED).where(e -> e.getCharacter().equals("\t")).act((b, e) -> {})
                .on(KEY_TYPED).where(e -> e.getCharacter().equals("\n")).act((b, e) -> {})
                .on(KEY_TYPED).where(e -> e.getCharacter().equals("\r")).act((b, e) -> {})
                .on(KEY_TYPED).where(e -> e.getCharacter().equals("\r\n")).act((b, e) -> {})
                // character input
                .on(KEY_TYPED).where(e -> !e.isControlDown() && !e.isAltDown() && !e.isMetaDown())
                        .act(StyledTextAreaBehavior::keyTyped)

                .create()
                .onlyWhen(b -> b.area.isEditable());

        EventHandlerTemplate<StyledTextAreaBehavior, InputEvent> verticalNavigation = EventHandlerTemplate
                .<StyledTextAreaBehavior, InputEvent, KeyEvent>
                // vertical caret movement
                 on(keyPressed(UP))       .act((b, e) -> b.prevLine(SelectionPolicy.CLEAR))
                .on(keyPressed(KP_UP))    .act((b, e) -> b.prevLine(SelectionPolicy.CLEAR))
                .on(keyPressed(DOWN))     .act((b, e) -> b.nextLine(SelectionPolicy.CLEAR))
                .on(keyPressed(KP_DOWN))  .act((b, e) -> b.nextLine(SelectionPolicy.CLEAR))
                .on(keyPressed(PAGE_UP))  .act((b, e) -> b.prevPage(SelectionPolicy.CLEAR))
                .on(keyPressed(PAGE_DOWN)).act((b, e) -> b.nextPage(SelectionPolicy.CLEAR))
                // vertical selection
                .on(keyPressed(UP,        SHIFT_DOWN)).act((b, e) -> b.prevLine(SelectionPolicy.ADJUST))
                .on(keyPressed(KP_UP,     SHIFT_DOWN)).act((b, e) -> b.prevLine(SelectionPolicy.ADJUST))
                .on(keyPressed(DOWN,      SHIFT_DOWN)).act((b, e) -> b.nextLine(SelectionPolicy.ADJUST))
                .on(keyPressed(KP_DOWN,   SHIFT_DOWN)).act((b, e) -> b.nextLine(SelectionPolicy.ADJUST))
                .on(keyPressed(PAGE_UP,   SHIFT_DOWN)).act((b, e) -> b.prevPage(SelectionPolicy.ADJUST))
                .on(keyPressed(PAGE_DOWN, SHIFT_DOWN)).act((b, e) -> b.nextPage(SelectionPolicy.ADJUST))

                .create();

        EventHandlerTemplate<StyledTextAreaBehavior, InputEvent> otherNavigation = EventHandlerTemplate
                .<StyledTextAreaBehavior, InputEvent, KeyEvent>
                // caret movement
                 on(keyPressed(RIGHT))   .act(StyledTextAreaBehavior::right)
                .on(keyPressed(KP_RIGHT)).act(StyledTextAreaBehavior::right)
                .on(keyPressed(LEFT))    .act(StyledTextAreaBehavior::left)
                .on(keyPressed(KP_LEFT)) .act(StyledTextAreaBehavior::left)
                .on(keyPressed(HOME))    .act((b, e) -> b.area.lineStart(SelectionPolicy.CLEAR))
                .on(keyPressed(END))     .act((b, e) -> b.area.lineEnd(SelectionPolicy.CLEAR))
                .on(keyPressed(RIGHT,    SHORTCUT_DOWN)).act((b, e) -> b.area.nextWord(SelectionPolicy.CLEAR))
                .on(keyPressed(KP_RIGHT, SHORTCUT_DOWN)).act((b, e) -> b.area.nextWord(SelectionPolicy.CLEAR))
                .on(keyPressed(LEFT,     SHORTCUT_DOWN)).act((b, e) -> b.area.previousWord(SelectionPolicy.CLEAR))
                .on(keyPressed(KP_LEFT,  SHORTCUT_DOWN)).act((b, e) -> b.area.previousWord(SelectionPolicy.CLEAR))
                .on(keyPressed(HOME,     SHORTCUT_DOWN)).act((b, e) -> b.area.start(SelectionPolicy.CLEAR))
                .on(keyPressed(END,      SHORTCUT_DOWN)).act((b, e) -> b.area.end(SelectionPolicy.CLEAR))
                // selection
                .on(keyPressed(RIGHT,    SHIFT_DOWN)).act(StyledTextAreaBehavior::selectRight)
                .on(keyPressed(KP_RIGHT, SHIFT_DOWN)).act(StyledTextAreaBehavior::selectRight)
                .on(keyPressed(LEFT,     SHIFT_DOWN)).act(StyledTextAreaBehavior::selectLeft)
                .on(keyPressed(KP_LEFT,  SHIFT_DOWN)).act(StyledTextAreaBehavior::selectLeft)
                .on(keyPressed(HOME,     SHIFT_DOWN)).act((b, e) -> b.area.lineStart(selPolicy))
                .on(keyPressed(END,      SHIFT_DOWN)).act((b, e) -> b.area.lineEnd(selPolicy))
                .on(keyPressed(HOME,     SHIFT_DOWN, SHORTCUT_DOWN)).act((b, e) -> b.area.start(selPolicy))
                .on(keyPressed(END,      SHIFT_DOWN, SHORTCUT_DOWN)).act((b, e) -> b.area.end(selPolicy))
                .on(keyPressed(LEFT,     SHIFT_DOWN, SHORTCUT_DOWN)).act((b, e) -> b.area.previousWord(selPolicy))
                .on(keyPressed(KP_LEFT,  SHIFT_DOWN, SHORTCUT_DOWN)).act((b, e) -> b.area.previousWord(selPolicy))
                .on(keyPressed(RIGHT,    SHIFT_DOWN, SHORTCUT_DOWN)).act((b, e) -> b.area.nextWord(selPolicy))
                .on(keyPressed(KP_RIGHT, SHIFT_DOWN, SHORTCUT_DOWN)).act((b, e) -> b.area.nextWord(selPolicy))
                .on(keyPressed(A, SHORTCUT_DOWN)).act((b, e) -> b.area.selectAll())

                .create();

        EventHandlerTemplate<StyledTextAreaBehavior, InputEvent> otherActions = EventHandlerTemplate
                .<StyledTextAreaBehavior, InputEvent, KeyEvent>
                // copy
                 on(keyPressed(COPY))                 .act((b, e) -> b.area.copy())
                .on(keyPressed(C,      SHORTCUT_DOWN)).act((b, e) -> b.area.copy())
                .on(keyPressed(INSERT, SHORTCUT_DOWN)).act((b, e) -> b.area.copy())
                .create();

        TEMPLATE = edits.orElse(otherNavigation).ifConsumed((b, e) -> b.clearTargetCaretOffset())
                .orElse(verticalNavigation)
                .orElse(otherActions);
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
    private void clearTargetCaretOffset() {
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

    public StyledTextAreaBehavior(StyledTextAreaVisual<?> visual) {
        this.area = visual.getControl();
        this.visual = visual;

        EventHandler<InputEvent> keyHandler = TEMPLATE.bind(this);
        EventHandlerHelper.install(visual.onInputProperty(), keyHandler);

        subscription = Subscription.multi(
                visual.cellMouseEvents()
                        .subscribe(pair -> pair.exec(this::handleMouseEvent)),
                EventStreams.eventsOf(area, MouseEvent.ANY)
                        .subscribe(this::handleMouseEvent),
                () -> EventHandlerHelper.remove(visual.onInputProperty(), keyHandler));
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

    private void keyTyped(KeyEvent event) {
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

    private void deleteBackward(KeyEvent ignore) {
        IndexRange selection = area.getSelection();
        if(selection.getLength() == 0) {
            area.deletePreviousChar();
        } else {
            area.replaceSelection("");
        }
    }

    private void deleteForward(KeyEvent ignore) {
        IndexRange selection = area.getSelection();
        if(selection.getLength() == 0) {
            area.deleteNextChar();
        } else {
            area.replaceSelection("");
        }
    }

    private void left(KeyEvent ignore) {
        IndexRange sel = area.getSelection();
        if(sel.getLength() == 0) {
            area.previousChar(SelectionPolicy.CLEAR);
        } else {
            area.moveTo(sel.getStart(), SelectionPolicy.CLEAR);
        }
    }

    private void right(KeyEvent ignore) {
        IndexRange sel = area.getSelection();
        if(sel.getLength() == 0) {
            area.nextChar(SelectionPolicy.CLEAR);
        } else {
            area.moveTo(sel.getEnd(), SelectionPolicy.CLEAR);
        }
    }

    private void selectLeft(KeyEvent ignore) {
        area.previousChar(SelectionPolicy.ADJUST);
    }

    private void selectRight(KeyEvent ignore) {
        area.nextChar(SelectionPolicy.ADJUST);
    }

    private void selectWord() {
        area.previousWord(SelectionPolicy.CLEAR);
        area.nextWord(SelectionPolicy.ADJUST);
    }

    private void deletePrevWord(KeyEvent ignore) {
        int end = area.getCaretPosition();

        if (end > 0) {
            area.previousWord(SelectionPolicy.CLEAR);
            int start = area.getCaretPosition();
            area.replaceText(start, end, "");
        }
    }

    private void deleteNextWord(KeyEvent ignore) {
        int start = area.getCaretPosition();

        if (start < area.getLength()) {
            area.nextWord(SelectionPolicy.CLEAR);
            int end = area.getCaretPosition();
            area.replaceText(start, end, "");
        }
    }

    private void downLines(SelectionPolicy selectionPolicy, int nLines) {
        Position currentLine = visual.currentLine();
        Position targetLine = currentLine.offsetBy(nLines, Forward).clamp();
        if(!currentLine.sameAs(targetLine)) {
            // compute new caret position
            int newCaretPos = visual.getInsertionIndex(getTargetCaretOffset(), targetLine);

            // update model
            visual.getControl().moveTo(newCaretPos, selectionPolicy);
        }
    }

    private void prevLine(SelectionPolicy selectionPolicy) {
        downLines(selectionPolicy, -1);
    }

    private void nextLine(SelectionPolicy selectionPolicy) {
        downLines(selectionPolicy, 1);
    }

    private void prevPage(SelectionPolicy selectionPolicy) {
        visual.followCaret(); // make sure caret is in the viewport
        double height = visual.getViewportHeight();
        Bounds caretBounds = visual.getCaretBounds().get();
        double caretMidY = caretBounds.getMinY() + caretBounds.getHeight() / 2;

        int newCaretPos = visual.getInsertionIndex(getTargetCaretOffset(), caretMidY - height);
        visual.show(-height);
        visual.getControl().moveTo(newCaretPos, selectionPolicy);
    }

    private void nextPage(SelectionPolicy selectionPolicy) {
        visual.followCaret(); // make sure caret is in the viewport
        double height = visual.getViewportHeight();
        Bounds caretBounds = visual.getCaretBounds().get();
        double caretMidY = caretBounds.getMinY() + caretBounds.getHeight() / 2;

        int newCaretPos = visual.getInsertionIndex(getTargetCaretOffset(), caretMidY + height);
        visual.show(2*height);
        visual.getControl().moveTo(newCaretPos, selectionPolicy);
    }


    /* ********************************************************************** *
     * Mouse handling implementation                                          *
     * ********************************************************************** */

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
