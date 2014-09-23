package org.fxmisc.richtext.skin;

import static com.sun.javafx.PlatformUtil.*;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyCombination.*;
import static javafx.scene.input.KeyEvent.*;
import static javafx.scene.input.MouseDragEvent.*;
import static javafx.scene.input.MouseEvent.*;
import static org.fxmisc.richtext.TwoDimensional.Bias.*;
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
import org.fxmisc.wellbehaved.input.AffinedEventHandler;
import org.fxmisc.wellbehaved.input.InputHandlerTemplate;
import org.fxmisc.wellbehaved.input.InputReceiver;
import org.fxmisc.wellbehaved.input.StatelessInputHandlerTemplate;
import org.fxmisc.wellbehaved.skin.Behavior;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.scene.text.HitInfo;

/**
 * Controller for StyledTextArea.
 */
public class StyledTextAreaBehavior implements Behavior, InputReceiver {

    private static final InputHandlerTemplate<StyledTextAreaBehavior> TEMPLATE;
    static {
        SelectionPolicy selPolicy = PlatformUtil.isMac()
                ? SelectionPolicy.EXTEND
                : SelectionPolicy.ADJUST;

        InputHandlerTemplate<StyledTextAreaBehavior> edits = StatelessInputHandlerTemplate
                // deletion
                .on(DELETE).<StyledTextAreaBehavior>act(StyledTextAreaBehavior::deleteForward)
                .on(BACK_SPACE)                    .act(StyledTextAreaBehavior::deleteBackward)
                .on(DELETE,     SHORTCUT_DOWN).act(StyledTextAreaBehavior::deleteNextWord)
                .on(BACK_SPACE, SHORTCUT_DOWN).act(StyledTextAreaBehavior::deletePrevWord)
                // cut
                .on(CUT)               .act((b, e) -> b.area.cut())
                .on(X, SHORTCUT_DOWN)  .act((b, e) -> b.area.cut())
                .on(DELETE, SHIFT_DOWN).act((b, e) -> b.area.cut())
                // paste
                .on(PASTE)             .act((b, e) -> b.area.paste())
                .on(V, SHORTCUT_DOWN)  .act((b, e) -> b.area.paste())
                .on(INSERT, SHIFT_DOWN).act((b, e) -> b.area.paste())
                // tab & newline
                .on(ENTER).act((b, e) -> b.area.replaceSelection("\n"))
                .on(TAB)  .act((b, e) -> b.area.replaceSelection("\t"))
                // undo/redo,
                .on(Z, SHORTCUT_DOWN)            .act((b, e) -> b.area.undo())
                .on(Y, SHORTCUT_DOWN)            .act((b, e) -> b.area.redo())
                .on(Z, SHORTCUT_DOWN, SHIFT_DOWN).act((b, e) -> b.area.redo())
                // Consume KEY_TYPED events for Enter and Tab,
                // because they are already handled as KEY_PRESSED
                .on("\t",   ALT_ANY, CONTROL_ANY, META_ANY, SHIFT_ANY, SHORTCUT_ANY).act((b, e) -> {})
                .on("\n",   ALT_ANY, CONTROL_ANY, META_ANY, SHIFT_ANY, SHORTCUT_ANY).act((b, e) -> {})
                .on("\r",   ALT_ANY, CONTROL_ANY, META_ANY, SHIFT_ANY, SHORTCUT_ANY).act((b, e) -> {})
                .on("\r\n", ALT_ANY, CONTROL_ANY, META_ANY, SHIFT_ANY, SHORTCUT_ANY).act((b, e) -> {})
                // character input
                .on(KEY_TYPED).where(e -> !e.isControlDown() && !e.isAltDown() && !e.isMetaDown())
                        .act(StyledTextAreaBehavior::keyTyped)

                .create()
                .onlyWhen(b -> b.area.isEditable());

        InputHandlerTemplate<StyledTextAreaBehavior> verticalNavigation = StatelessInputHandlerTemplate
                // vertical caret movement
                .on(UP)       .<StyledTextAreaBehavior>act((b, e) -> b.prevLine(SelectionPolicy.CLEAR))
                .on(KP_UP)    .act((b, e) -> b.prevLine(SelectionPolicy.CLEAR))
                .on(DOWN)     .act((b, e) -> b.nextLine(SelectionPolicy.CLEAR))
                .on(KP_DOWN)  .act((b, e) -> b.nextLine(SelectionPolicy.CLEAR))
                .on(PAGE_UP)  .act((b, e) -> b.prevPage(SelectionPolicy.CLEAR))
                .on(PAGE_DOWN).act((b, e) -> b.nextPage(SelectionPolicy.CLEAR))
                // vertical selection
                .on(UP,        SHIFT_DOWN).act((b, e) -> b.prevLine(SelectionPolicy.ADJUST))
                .on(KP_UP,     SHIFT_DOWN).act((b, e) -> b.prevLine(SelectionPolicy.ADJUST))
                .on(DOWN,      SHIFT_DOWN).act((b, e) -> b.nextLine(SelectionPolicy.ADJUST))
                .on(KP_DOWN,   SHIFT_DOWN).act((b, e) -> b.nextLine(SelectionPolicy.ADJUST))
                .on(PAGE_UP,   SHIFT_DOWN).act((b, e) -> b.prevPage(SelectionPolicy.ADJUST))
                .on(PAGE_DOWN, SHIFT_DOWN).act((b, e) -> b.nextPage(SelectionPolicy.ADJUST))

                .create();

        InputHandlerTemplate<StyledTextAreaBehavior> otherNavigation = StatelessInputHandlerTemplate
                // caret movement
                .on(RIGHT)   .<StyledTextAreaBehavior>act(StyledTextAreaBehavior::right)
                .on(KP_RIGHT).act(StyledTextAreaBehavior::right)
                .on(LEFT)    .act(StyledTextAreaBehavior::left)
                .on(KP_LEFT) .act(StyledTextAreaBehavior::left)
                .on(HOME)    .act((b, e) -> b.area.lineStart(SelectionPolicy.CLEAR))
                .on(END)     .act((b, e) -> b.area.lineEnd(SelectionPolicy.CLEAR))
                .on(RIGHT,    SHORTCUT_DOWN).act((b, e) -> b.area.nextWord(SelectionPolicy.CLEAR))
                .on(KP_RIGHT, SHORTCUT_DOWN).act((b, e) -> b.area.nextWord(SelectionPolicy.CLEAR))
                .on(LEFT,     SHORTCUT_DOWN).act((b, e) -> b.area.previousWord(SelectionPolicy.CLEAR))
                .on(KP_LEFT,  SHORTCUT_DOWN).act((b, e) -> b.area.previousWord(SelectionPolicy.CLEAR))
                .on(HOME,     SHORTCUT_DOWN).act((b, e) -> b.area.start(SelectionPolicy.CLEAR))
                .on(END,      SHORTCUT_DOWN).act((b, e) -> b.area.end(SelectionPolicy.CLEAR))
                // selection
                .on(RIGHT,    SHIFT_DOWN).act(StyledTextAreaBehavior::selectRight)
                .on(KP_RIGHT, SHIFT_DOWN).act(StyledTextAreaBehavior::selectRight)
                .on(LEFT,     SHIFT_DOWN).act(StyledTextAreaBehavior::selectLeft)
                .on(KP_LEFT,  SHIFT_DOWN).act(StyledTextAreaBehavior::selectLeft)
                .on(HOME,     SHIFT_DOWN).act((b, e) -> b.area.lineStart(selPolicy))
                .on(END,      SHIFT_DOWN).act((b, e) -> b.area.lineEnd(selPolicy))
                .on(HOME,     SHIFT_DOWN, SHORTCUT_DOWN).act((b, e) -> b.area.start(selPolicy))
                .on(END,      SHIFT_DOWN, SHORTCUT_DOWN).act((b, e) -> b.area.end(selPolicy))
                .on(LEFT,     SHIFT_DOWN, SHORTCUT_DOWN).act((b, e) -> b.area.previousWord(selPolicy))
                .on(KP_LEFT,  SHIFT_DOWN, SHORTCUT_DOWN).act((b, e) -> b.area.previousWord(selPolicy))
                .on(RIGHT,    SHIFT_DOWN, SHORTCUT_DOWN).act((b, e) -> b.area.nextWord(selPolicy))
                .on(KP_RIGHT, SHIFT_DOWN, SHORTCUT_DOWN).act((b, e) -> b.area.nextWord(selPolicy))
                .on(A, SHORTCUT_DOWN)    .act((b, e) -> b.area.selectAll())

                .create();

        InputHandlerTemplate<StyledTextAreaBehavior> otherActions = StatelessInputHandlerTemplate
                // copy
                .on(COPY).<StyledTextAreaBehavior>act((b, e) -> b.area.copy())
                .on(C,      SHORTCUT_DOWN)       .act((b, e) -> b.area.copy())
                .on(INSERT, SHORTCUT_DOWN)       .act((b, e) -> b.area.copy())
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

        AffinedEventHandler keyHandler = TEMPLATE.bind(this);
        keyHandler.install();

        subscription = Subscription.multi(
                visual.cellMouseEvents()
                        .subscribe(pair -> pair.exec(this::handleMouseEvent)),
                EventStreams.eventsOf(area, MouseEvent.ANY)
                        .subscribe(this::handleMouseEvent),
                keyHandler::remove);
    }

    /* ********************************************************************** *
     * Public API (from Behavior & InputReceiver)                             *
     * ********************************************************************** */

    @Override
    public void dispose() {
        subscription.unsubscribe();
    }

    @Override
    public EventHandler<? super InputEvent> getOnInput() {
        return visual.getOnInput();
    }

    @Override
    public void setOnInput(EventHandler<? super InputEvent> handler) {
        visual.setOnInput(handler);
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
