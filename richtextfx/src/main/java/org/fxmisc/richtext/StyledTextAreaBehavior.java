package org.fxmisc.richtext;

import static java.lang.Character.*;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyCombination.*;
import static org.fxmisc.richtext.model.TwoDimensional.Bias.*;
import static org.fxmisc.wellbehaved.event.EventPattern.*;
import static org.fxmisc.wellbehaved.event.template.InputMapTemplate.*;
import static org.reactfx.EventStreams.*;

import java.util.function.Predicate;

import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import org.fxmisc.richtext.NavigationActions.SelectionPolicy;
import org.fxmisc.richtext.model.TwoDimensional.Position;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.template.InputMapTemplate;
import org.reactfx.EventStream;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

/**
 * Controller for GenericStyledArea.
 */
class StyledTextAreaBehavior {

    private static final boolean isMac;
    private static final boolean isWindows;
    static {
        String os = System.getProperty("os.name");
        isMac = os.startsWith("Mac");
        isWindows = os.startsWith("Windows");
    }

    private static final InputMapTemplate<StyledTextAreaBehavior, ? super Event> EVENT_TEMPLATE;

    static {
        SelectionPolicy selPolicy = isMac
                ? SelectionPolicy.EXTEND
                : SelectionPolicy.ADJUST;

        InputMapTemplate<StyledTextAreaBehavior, KeyEvent> editsBase = sequence(
                // deletion
                consume(keyPressed(DELETE),                     StyledTextAreaBehavior::deleteForward),
                consume(keyPressed(BACK_SPACE),                 StyledTextAreaBehavior::deleteBackward),
                consume(keyPressed(DELETE,     SHORTCUT_DOWN),  StyledTextAreaBehavior::deleteNextWord),
                consume(keyPressed(BACK_SPACE, SHORTCUT_DOWN),  StyledTextAreaBehavior::deletePrevWord),
                // cut
                consume(
                        anyOf(keyPressed(CUT),   keyPressed(X, SHORTCUT_DOWN), keyPressed(DELETE, SHIFT_DOWN)),
                        (b, e) -> b.view.cut()),
                // paste
                consume(
                        anyOf(keyPressed(PASTE), keyPressed(V, SHORTCUT_DOWN), keyPressed(INSERT, SHIFT_DOWN)),
                        (b, e) -> b.view.paste()),
                // tab & newline
                consume(keyPressed(ENTER), (b, e) -> b.view.replaceSelection("\n")),
                consume(keyPressed(TAB),   (b, e) -> b.view.replaceSelection("\t")),
                // undo/redo
                consume(keyPressed(Z, SHORTCUT_DOWN), (b, e) -> b.view.undo()),
                consume(
                        anyOf(keyPressed(Y, SHORTCUT_DOWN), keyPressed(Z, SHORTCUT_DOWN, SHIFT_DOWN)),
                        (b, e) -> b.view.redo())
        );
        InputMapTemplate<StyledTextAreaBehavior, KeyEvent> edits = when(b -> b.view.isEditable(), editsBase);

        InputMapTemplate<StyledTextAreaBehavior, KeyEvent> verticalNavigation = sequence(
                // vertical caret movement
                consume(
                        anyOf(keyPressed(UP), keyPressed(KP_UP)),
                        (b, e) -> b.prevLine(SelectionPolicy.CLEAR)),
                consume(
                        anyOf(keyPressed(DOWN), keyPressed(KP_DOWN)),
                        (b, e) -> b.nextLine(SelectionPolicy.CLEAR)),
                consume(keyPressed(PAGE_UP),    (b, e) -> b.view.prevPage(SelectionPolicy.CLEAR)),
                consume(keyPressed(PAGE_DOWN),  (b, e) -> b.view.nextPage(SelectionPolicy.CLEAR)),
                // vertical selection
                consume(
                        anyOf(keyPressed(UP,   SHIFT_DOWN), keyPressed(KP_UP, SHIFT_DOWN)),
                        (b, e) -> b.prevLine(SelectionPolicy.ADJUST)),
                consume(
                        anyOf(keyPressed(DOWN, SHIFT_DOWN), keyPressed(KP_DOWN, SHIFT_DOWN)),
                        (b, e) -> b.nextLine(SelectionPolicy.ADJUST)),
                consume(keyPressed(PAGE_UP,   SHIFT_DOWN),  (b, e) -> b.view.prevPage(SelectionPolicy.ADJUST)),
                consume(keyPressed(PAGE_DOWN, SHIFT_DOWN),  (b, e) -> b.view.nextPage(SelectionPolicy.ADJUST))
        );

        InputMapTemplate<StyledTextAreaBehavior, KeyEvent> otherNavigation = sequence(
                // caret movement
                consume(anyOf(keyPressed(RIGHT), keyPressed(KP_RIGHT)), StyledTextAreaBehavior::right),
                consume(anyOf(keyPressed(LEFT),  keyPressed(KP_LEFT)),  StyledTextAreaBehavior::left),
                consume(keyPressed(HOME), (b, e) -> b.view.lineStart(SelectionPolicy.CLEAR)),
                consume(keyPressed(END),  (b, e) -> b.view.lineEnd(SelectionPolicy.CLEAR)),
                consume(
                        anyOf(
                                keyPressed(RIGHT,    SHORTCUT_DOWN),
                                keyPressed(KP_RIGHT, SHORTCUT_DOWN)
                        ), (b, e) -> b.skipToNextWord(SelectionPolicy.CLEAR)),
                consume(
                        anyOf(
                                keyPressed(LEFT,     SHORTCUT_DOWN),
                                keyPressed(KP_LEFT,  SHORTCUT_DOWN)
                        ), (b, e) -> b.skipToPrevWord(SelectionPolicy.CLEAR)),
                consume(keyPressed(HOME, SHORTCUT_DOWN), (b, e) -> b.view.start(SelectionPolicy.CLEAR)),
                consume(keyPressed(END,  SHORTCUT_DOWN), (b, e) -> b.view.end(SelectionPolicy.CLEAR)),
                // selection
                consume(
                        anyOf(
                                keyPressed(RIGHT,    SHIFT_DOWN),
                                keyPressed(KP_RIGHT, SHIFT_DOWN)
                        ), StyledTextAreaBehavior::selectRight),
                consume(
                        anyOf(
                                keyPressed(LEFT,     SHIFT_DOWN),
                                keyPressed(KP_LEFT,  SHIFT_DOWN)
                        ), StyledTextAreaBehavior::selectLeft),
                consume(keyPressed(HOME, SHIFT_DOWN),                (b, e) -> b.view.lineStart(selPolicy)),
                consume(keyPressed(END,  SHIFT_DOWN),                (b, e) -> b.view.lineEnd(selPolicy)),
                consume(keyPressed(HOME, SHIFT_DOWN, SHORTCUT_DOWN), (b, e) -> b.view.start(selPolicy)),
                consume(keyPressed(END,  SHIFT_DOWN, SHORTCUT_DOWN), (b, e) -> b.view.end(selPolicy)),
                consume(
                        anyOf(
                                keyPressed(RIGHT,    SHIFT_DOWN, SHORTCUT_DOWN),
                                keyPressed(KP_RIGHT, SHIFT_DOWN, SHORTCUT_DOWN)
                        ), (b, e) -> b.skipToNextWord(selPolicy)),
                consume(
                        anyOf(
                                keyPressed(LEFT,     SHIFT_DOWN, SHORTCUT_DOWN),
                                keyPressed(KP_LEFT,  SHIFT_DOWN, SHORTCUT_DOWN)
                        ), (b, e) -> b.skipToPrevWord(selPolicy)),
                consume(keyPressed(A, SHORTCUT_DOWN), (b, e) -> b.view.selectAll())
        );

        InputMapTemplate<StyledTextAreaBehavior, KeyEvent> copyAction = consume(
                anyOf(
                        keyPressed(COPY),
                        keyPressed(C, SHORTCUT_DOWN),
                        keyPressed(INSERT, SHORTCUT_DOWN)
                ), (b, e) -> b.view.copy()
        );

        Predicate<KeyEvent> noControlKeys = e ->
                // filter out control keys
                (!e.isControlDown() && !e.isMetaDown())
                // except on Windows allow the Ctrl+Alt combination (produced by AltGr)
                || (isWindows && !e.isMetaDown() && (!e.isControlDown() || e.isAltDown()));

        Predicate<KeyEvent> isChar = e ->
                e.getCode().isLetterKey() ||
                e.getCode().isDigitKey() ||
                e.getCode().isWhitespaceKey();

        InputMapTemplate<StyledTextAreaBehavior, KeyEvent> charPressConsumer = consume(keyPressed().onlyIf(isChar.and(noControlKeys)));

        InputMapTemplate<StyledTextAreaBehavior, ? super KeyEvent> keyPressedTemplate = edits
                .orElse(otherNavigation).ifConsumed((b, e) -> b.view.clearTargetCaretOffset())
                .orElse(verticalNavigation)
                .orElse(copyAction)
                .ifConsumed((b, e) -> b.view.requestFollowCaret())
                // no need to add 'ifConsumed' after charPress since
                // requestFollowCaret is called in keyTypedTemplate
                .orElse(charPressConsumer);

        InputMapTemplate<StyledTextAreaBehavior, KeyEvent> keyTypedBase = consume(
                // character input
                EventPattern.keyTyped().onlyIf(noControlKeys.and(e -> isLegal(e.getCharacter()))),
                StyledTextAreaBehavior::keyTyped
        ).ifConsumed((b, e) -> b.view.requestFollowCaret());
        InputMapTemplate<StyledTextAreaBehavior, ? super KeyEvent> keyTypedTemplate = when(b -> b.view.isEditable(), keyTypedBase);

        InputMapTemplate<StyledTextAreaBehavior, ? super MouseEvent> mouseEventTemplate = sequence(
            consume(eventType(MouseEvent.MOUSE_PRESSED),  StyledTextAreaBehavior::mousePressed),
            consume(eventType(MouseEvent.MOUSE_DRAGGED), StyledTextAreaBehavior::mouseDragged),
            consume(eventType(MouseEvent.DRAG_DETECTED), StyledTextAreaBehavior::dragDetected),
            consume(eventType(MouseEvent.MOUSE_RELEASED), StyledTextAreaBehavior::mouseReleased)
        );

        InputMapTemplate<StyledTextAreaBehavior, ? super ContextMenuEvent> contextMenuEventTemplate = consumeWhen(
                EventPattern.eventType(ContextMenuEvent.CONTEXT_MENU_REQUESTED),
                b -> !b.view.isDisabled() && b.view.isContextMenuPresent(),
                StyledTextAreaBehavior::showContextMenu
        );

        EVENT_TEMPLATE = sequence(mouseEventTemplate, keyPressedTemplate, keyTypedTemplate, contextMenuEventTemplate);
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

    private final GenericStyledArea<?, ?, ?> view;

    /**
     * Indicates whether selection is being dragged by the user.
     */
    private DragState dragSelection = DragState.NO_DRAG;

    private final Var<Point2D> autoscrollTo = Var.newSimpleVar(null);

    /* ********************************************************************** *
     * Constructors                                                           *
     * ********************************************************************** */

    StyledTextAreaBehavior(GenericStyledArea<?, ?, ?> area) {
        this.view = area;

        InputMapTemplate.installFallback(EVENT_TEMPLATE, this, b -> b.view);

        // setup auto-scroll
        Val<Point2D> projection = Val.combine(
                autoscrollTo,
                area.layoutBoundsProperty(),
                StyledTextAreaBehavior::project);
        Val<Point2D> distance = Val.combine(
                autoscrollTo,
                projection,
                Point2D::subtract);
        EventStream<Point2D> deltas = nonNullValuesOf(distance)
                .emitBothOnEach(animationFrames())
                .map(t -> t.map((ds, nanos) -> ds.multiply(nanos / 100_000_000.0)));
        valuesOf(autoscrollTo).flatMap(p -> p == null
                ? never() // automatically stops the scroll animation
                : deltas)
            .subscribe(ds -> {
                area.scrollBy(ds);
                projection.ifPresent(this::dragTo);
            });
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

        view.replaceSelection(text);
    }

    private static boolean isLegal(String text) {
        int n = text.length();
        for(int i = 0; i < n; ++i) {
            if(Character.isISOControl(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void deleteBackward(KeyEvent ignore) {
        IndexRange selection = view.getSelection();
        if(selection.getLength() == 0) {
            view.deletePreviousChar();
        } else {
            view.replaceSelection("");
        }
    }

    private void deleteForward(KeyEvent ignore) {
        IndexRange selection = view.getSelection();
        if(selection.getLength() == 0) {
            view.deleteNextChar();
        } else {
            view.replaceSelection("");
        }
    }

    private void left(KeyEvent ignore) {
        IndexRange sel = view.getSelection();
        if(sel.getLength() == 0) {
            view.previousChar(SelectionPolicy.CLEAR);
        } else {
            view.moveTo(sel.getStart(), SelectionPolicy.CLEAR);
        }
    }

    private void right(KeyEvent ignore) {
        IndexRange sel = view.getSelection();
        if(sel.getLength() == 0) {
            view.nextChar(SelectionPolicy.CLEAR);
        } else {
            view.moveTo(sel.getEnd(), SelectionPolicy.CLEAR);
        }
    }

    private void selectLeft(KeyEvent ignore) {
        view.previousChar(SelectionPolicy.ADJUST);
    }

    private void selectRight(KeyEvent ignore) {
        view.nextChar(SelectionPolicy.ADJUST);
    }

    private void selectWord() {
        view.wordBreaksBackwards(1, SelectionPolicy.CLEAR);
        view.wordBreaksForwards(1, SelectionPolicy.ADJUST);
    }

    private void deletePrevWord(KeyEvent ignore) {
        int end = view.getCaretPosition();

        if (end > 0) {
            view.wordBreaksBackwards(2, SelectionPolicy.CLEAR);
            int start = view.getCaretPosition();
            view.replaceText(start, end, "");
        }
    }

    private void deleteNextWord(KeyEvent ignore) {
        int start = view.getCaretPosition();

        if (start < view.getLength()) {
            view.wordBreaksForwards(2, SelectionPolicy.CLEAR);
            int end = view.getCaretPosition();
            view.replaceText(start, end, "");
        }
    }

    private void downLines(SelectionPolicy selectionPolicy, int nLines) {
        Position currentLine = view.currentLine();
        Position targetLine = currentLine.offsetBy(nLines, Forward).clamp();
        if(!currentLine.sameAs(targetLine)) {
            // compute new caret position
            CharacterHit hit = view.hit(view.getTargetCaretOffset(), targetLine);

            // update model
            view.moveTo(hit.getInsertionIndex(), selectionPolicy);
        }
    }

    private void prevLine(SelectionPolicy selectionPolicy) {
        downLines(selectionPolicy, -1);
    }

    private void nextLine(SelectionPolicy selectionPolicy) {
        downLines(selectionPolicy, 1);
    }

    private void skipToPrevWord(SelectionPolicy selectionPolicy) {
        int caretPos = view.getCaretPosition();

        // if (0 == caretPos), do nothing as can't move to the left anyway
        if (1 <= caretPos ) {
            boolean prevCharIsWhiteSpace = isWhitespace(view.getText(caretPos - 1, caretPos).charAt(0));
            view.wordBreaksBackwards(prevCharIsWhiteSpace ? 2 : 1, selectionPolicy);
        }
    }

    private void skipToNextWord(SelectionPolicy selectionPolicy) {
        int caretPos = view.getCaretPosition();
        int length = view.getLength();

        // if (caretPos == length), do nothing as can't move to the right anyway
        if (caretPos <= length - 1) {
            boolean nextCharIsWhiteSpace = isWhitespace(view.getText(caretPos, caretPos + 1).charAt(0));
            view.wordBreaksForwards(nextCharIsWhiteSpace ? 2 : 1, selectionPolicy);
        }
    }

    /* ********************************************************************** *
     * Mouse handling implementation                                          *
     * ********************************************************************** */

    private void showContextMenu(ContextMenuEvent e) {
        ContextMenu menu = view.getContextMenu();
        double xOffset = view.getContextMenuXOffset();
        double yOffset = view.getContextMenuYOffset();

        menu.show(view, e.getScreenX() + xOffset, e.getScreenY() + yOffset);
    }

    private void mousePressed(MouseEvent e) {
        // don't respond if disabled
        if(view.isDisabled()) {
            return;
        }

        if (view.isContextMenuPresent() && view.getContextMenu().isShowing()) {
            view.getContextMenu().hide();
        }

        if(e.getButton() == MouseButton.PRIMARY) {
            // ensure focus
            view.requestFocus();

            CharacterHit hit = view.hit(e.getX(), e.getY());

            if(e.isShiftDown()) {
                // On Mac always extend selection,
                // switching anchor and caret if necessary.
                view.moveTo(
                        hit.getInsertionIndex(),
                        isMac ? SelectionPolicy.EXTEND : SelectionPolicy.ADJUST);
            } else {
                switch (e.getClickCount()) {
                    case 1: firstLeftPress(hit); break;
                    case 2: selectWord(); break;
                    case 3: view.selectParagraph(); break;
                    default: // do nothing
                }
            }

            e.consume();
        }
    }

    private void firstLeftPress(CharacterHit hit) {
        view.clearTargetCaretOffset();
        IndexRange selection = view.getSelection();
        if(view.isEditable() &&
                selection.getLength() != 0 &&
                hit.getCharacterIndex().isPresent() &&
                hit.getCharacterIndex().getAsInt() >= selection.getStart() &&
                hit.getCharacterIndex().getAsInt() < selection.getEnd()) {
            // press inside selection
            dragSelection = DragState.POTENTIAL_DRAG;
        } else {
            dragSelection = DragState.NO_DRAG;
            view.moveTo(hit.getInsertionIndex(), SelectionPolicy.CLEAR);
        }
    }

    private void dragDetected(MouseEvent e) {
        if(dragSelection == DragState.POTENTIAL_DRAG) {
            dragSelection = DragState.DRAG;
        }
        e.consume();
    }

    private void mouseDragged(MouseEvent e) {
        // don't respond if disabled
        if(view.isDisabled()) {
            return;
        }

        // only respond to primary button alone
        if(e.getButton() != MouseButton.PRIMARY || e.isMiddleButtonDown() || e.isSecondaryButtonDown()) {
            return;
        }

        Point2D p = new Point2D(e.getX(), e.getY());
        if(view.getLayoutBounds().contains(p)) {
            dragTo(p);
            autoscrollTo.setValue(null); // stops auto-scroll
        } else {
            autoscrollTo.setValue(p);    // starts auto-scroll
        }

        e.consume();
    }

    private void dragTo(Point2D p) {
        CharacterHit hit = view.hit(p.getX(), p.getY());

        if(dragSelection == DragState.DRAG ||
                dragSelection == DragState.POTENTIAL_DRAG) { // MOUSE_DRAGGED may arrive even before DRAG_DETECTED
            view.positionCaret(hit.getInsertionIndex());
        } else {
            view.moveTo(hit.getInsertionIndex(), SelectionPolicy.ADJUST);
        }
    }

    private void mouseReleased(MouseEvent e) {
        // stop auto-scroll
        autoscrollTo.setValue(null);

        // don't respond if disabled
        if(view.isDisabled()) {
            return;
        }

        switch(dragSelection) {
            case POTENTIAL_DRAG:
                // drag didn't happen, position caret
                CharacterHit hit = view.hit(e.getX(), e.getY());
                view.moveTo(hit.getInsertionIndex(), SelectionPolicy.CLEAR);
                break;
            case DRAG:
                // only handle drags if mouse was released inside of view
                if (view.getLayoutBounds().contains(e.getX(), e.getY())) {
                    // move selection to the target position
                    CharacterHit h = view.hit(e.getX(), e.getY());
                    view.getOnSelectionDrop().accept(h.getInsertionIndex());
                    // do nothing, handled by mouseDragReleased
                }
            case NO_DRAG:
                // do nothing, caret already repositioned in mousePressed
        }
        dragSelection = DragState.NO_DRAG;
        e.consume();
    }

    private static Point2D project(Point2D p, Bounds bounds) {
        double x = clamp(p.getX(), bounds.getMinX(), bounds.getMaxX());
        double y = clamp(p.getY(), bounds.getMinY(), bounds.getMaxY());
        return new Point2D(x, y);
    }

    private static double clamp(double x, double min, double max) {
        return Math.min(Math.max(x, min), max);
    }
}
