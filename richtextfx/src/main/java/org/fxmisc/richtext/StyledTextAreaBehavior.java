package org.fxmisc.richtext;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyCombination.*;
import static org.fxmisc.richtext.model.TwoDimensional.Bias.*;
import static org.fxmisc.wellbehaved.event.EventPattern.*;
import static org.fxmisc.wellbehaved.event.template.InputMapTemplate.consume;
import static org.fxmisc.wellbehaved.event.template.InputMapTemplate.sequence;
import static org.fxmisc.wellbehaved.event.template.InputMapTemplate.when;
import static org.reactfx.EventStreams.*;

import java.util.Optional;
import java.util.function.Predicate;

import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import org.fxmisc.richtext.model.StyledTextAreaModel;
import org.fxmisc.richtext.model.NavigationActions.SelectionPolicy;
import org.fxmisc.richtext.model.TwoDimensional.Position;
import org.fxmisc.richtext.ParagraphBox.CaretOffsetX;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.template.InputMapTemplate;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

/**
 * Controller for StyledTextArea.
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
                consume(keyPressed(ENTER), (b, e) -> b.model.replaceSelection("\n")),
                consume(keyPressed(TAB),   (b, e) -> b.model.replaceSelection("\t")),
                // undo/redo
                consume(keyPressed(Z, SHORTCUT_DOWN), (b, e) -> b.model.undo()),
                consume(
                        anyOf(keyPressed(Y, SHORTCUT_DOWN), keyPressed(Z, SHORTCUT_DOWN, SHIFT_DOWN)),
                        (b, e) -> b.model.redo())
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
                consume(keyPressed(PAGE_UP),    (b, e) -> b.prevPage(SelectionPolicy.CLEAR)),
                consume(keyPressed(PAGE_DOWN),  (b, e) -> b.nextPage(SelectionPolicy.CLEAR)),
                // vertical selection
                consume(
                        anyOf(keyPressed(UP,   SHIFT_DOWN), keyPressed(KP_UP, SHIFT_DOWN)),
                        (b, e) -> b.prevLine(SelectionPolicy.ADJUST)),
                consume(
                        anyOf(keyPressed(DOWN, SHIFT_DOWN), keyPressed(KP_DOWN, SHIFT_DOWN)),
                        (b, e) -> b.nextLine(SelectionPolicy.ADJUST)),
                consume(keyPressed(PAGE_UP,   SHIFT_DOWN),  (b, e) -> b.prevPage(SelectionPolicy.ADJUST)),
                consume(keyPressed(PAGE_DOWN, SHIFT_DOWN),  (b, e) -> b.nextPage(SelectionPolicy.ADJUST))
        );

        InputMapTemplate<StyledTextAreaBehavior, KeyEvent> otherNavigation = sequence(
                // caret movement
                consume(anyOf(keyPressed(RIGHT), keyPressed(KP_RIGHT)), StyledTextAreaBehavior::right),
                consume(anyOf(keyPressed(LEFT),  keyPressed(KP_LEFT)),  StyledTextAreaBehavior::left),
                consume(keyPressed(HOME), (b, e) -> b.model.lineStart(SelectionPolicy.CLEAR)),
                consume(keyPressed(END),  (b, e) -> b.model.lineEnd(SelectionPolicy.CLEAR)),
                consume(
                        anyOf(
                                keyPressed(RIGHT,    SHORTCUT_DOWN),
                                keyPressed(KP_RIGHT, SHORTCUT_DOWN)
                        ), (b, e) -> b.model.wordBreaksForwards(2, SelectionPolicy.CLEAR)),
                consume(
                        anyOf(
                                keyPressed(LEFT,     SHORTCUT_DOWN),
                                keyPressed(KP_LEFT,  SHORTCUT_DOWN)
                        ), (b, e) -> b.model.wordBreaksBackwards(2, SelectionPolicy.CLEAR)),
                consume(keyPressed(HOME, SHORTCUT_DOWN), (b, e) -> b.model.start(SelectionPolicy.CLEAR)),
                consume(keyPressed(END,  SHORTCUT_DOWN), (b, e) -> b.model.end(SelectionPolicy.CLEAR)),
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
                consume(keyPressed(HOME, SHIFT_DOWN),                (b, e) -> b.model.lineStart(selPolicy)),
                consume(keyPressed(END,  SHIFT_DOWN),                (b, e) -> b.model.lineEnd(selPolicy)),
                consume(keyPressed(HOME, SHIFT_DOWN, SHORTCUT_DOWN), (b, e) -> b.model.start(selPolicy)),
                consume(keyPressed(END,  SHIFT_DOWN, SHORTCUT_DOWN), (b, e) -> b.model.end(selPolicy)),
                consume(
                        anyOf(
                                keyPressed(RIGHT,    SHIFT_DOWN, SHORTCUT_DOWN),
                                keyPressed(KP_RIGHT, SHIFT_DOWN, SHORTCUT_DOWN)
                        ), (b, e) -> b.model.wordBreaksForwards(2, selPolicy)),
                consume(
                        anyOf(
                                keyPressed(LEFT,     SHIFT_DOWN, SHORTCUT_DOWN),
                                keyPressed(KP_LEFT,  SHIFT_DOWN, SHORTCUT_DOWN)
                        ), (b, e) -> b.model.wordBreaksBackwards(2, selPolicy)),
                consume(keyPressed(A, SHORTCUT_DOWN), (b, e) -> b.model.selectAll())
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
                .orElse(otherNavigation).ifConsumed((b, e) -> b.clearTargetCaretOffset())
                .orElse(verticalNavigation)
                .orElse(copyAction)
                .orElse(charPressConsumer);

        InputMapTemplate<StyledTextAreaBehavior, KeyEvent> keyTypedBase = consume(
                // character input
                EventPattern.keyTyped().onlyIf(noControlKeys.and(e -> isLegal(e.getCharacter()))),
                StyledTextAreaBehavior::keyTyped
        );
        InputMapTemplate<StyledTextAreaBehavior, ? super KeyEvent> keyTypedTemplate = when(b -> b.view.isEditable(), keyTypedBase);

        InputMapTemplate<StyledTextAreaBehavior, ? super MouseEvent> mouseEventTemplate = sequence(
            consume(eventType(MouseEvent.MOUSE_PRESSED),  StyledTextAreaBehavior::mousePressed),
            consume(eventType(MouseEvent.MOUSE_DRAGGED), StyledTextAreaBehavior::mouseDragged),
            consume(eventType(MouseEvent.DRAG_DETECTED), StyledTextAreaBehavior::dragDetected),
            consume(eventType(MouseEvent.MOUSE_RELEASED), StyledTextAreaBehavior::mouseReleased)
        );

        EVENT_TEMPLATE = sequence(mouseEventTemplate, keyPressedTemplate, keyTypedTemplate);
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

    private final StyledTextArea<?, ?> view;

    private final StyledTextAreaModel<?, ?> model;

    /**
     * Indicates whether selection is being dragged by the user.
     */
    private DragState dragSelection = DragState.NO_DRAG;

    /**
     * Remembers horizontal position when traversing up / down.
     */
    private Optional<CaretOffsetX> targetCaretOffset = Optional.empty();
    private void clearTargetCaretOffset() {
        targetCaretOffset = Optional.empty();
    }
    private CaretOffsetX getTargetCaretOffset() {
        if(!targetCaretOffset.isPresent())
            targetCaretOffset = Optional.of(view.getCaretOffsetX());
        return targetCaretOffset.get();
    }

    private final Var<Point2D> autoscrollTo = Var.newSimpleVar(null);

    /* ********************************************************************** *
     * Constructors                                                           *
     * ********************************************************************** */

    StyledTextAreaBehavior(StyledTextArea<?, ?> area) {
        this.view = area;
        this.model = area.getModel();

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

        model.replaceSelection(text);
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
        IndexRange selection = model.getSelection();
        if(selection.getLength() == 0) {
            model.deletePreviousChar();
        } else {
            model.replaceSelection("");
        }
    }

    private void deleteForward(KeyEvent ignore) {
        IndexRange selection = model.getSelection();
        if(selection.getLength() == 0) {
            model.deleteNextChar();
        } else {
            model.replaceSelection("");
        }
    }

    private void left(KeyEvent ignore) {
        IndexRange sel = model.getSelection();
        if(sel.getLength() == 0) {
            model.previousChar(SelectionPolicy.CLEAR);
        } else {
            model.moveTo(sel.getStart(), SelectionPolicy.CLEAR);
        }
    }

    private void right(KeyEvent ignore) {
        IndexRange sel = model.getSelection();
        if(sel.getLength() == 0) {
            model.nextChar(SelectionPolicy.CLEAR);
        } else {
            model.moveTo(sel.getEnd(), SelectionPolicy.CLEAR);
        }
    }

    private void selectLeft(KeyEvent ignore) {
        model.previousChar(SelectionPolicy.ADJUST);
    }

    private void selectRight(KeyEvent ignore) {
        model.nextChar(SelectionPolicy.ADJUST);
    }

    private void selectWord() {
        model.wordBreaksBackwards(1, SelectionPolicy.CLEAR);
        model.wordBreaksForwards(1, SelectionPolicy.ADJUST);
    }

    private void deletePrevWord(KeyEvent ignore) {
        int end = model.getCaretPosition();

        if (end > 0) {
            model.wordBreaksBackwards(2, SelectionPolicy.CLEAR);
            int start = model.getCaretPosition();
            model.replaceText(start, end, "");
        }
    }

    private void deleteNextWord(KeyEvent ignore) {
        int start = model.getCaretPosition();

        if (start < model.getLength()) {
            model.wordBreaksForwards(2, SelectionPolicy.CLEAR);
            int end = model.getCaretPosition();
            model.replaceText(start, end, "");
        }
    }

    private void downLines(SelectionPolicy selectionPolicy, int nLines) {
        Position currentLine = view.currentLine();
        Position targetLine = currentLine.offsetBy(nLines, Forward).clamp();
        if(!currentLine.sameAs(targetLine)) {
            // compute new caret position
            CharacterHit hit = view.hit(getTargetCaretOffset(), targetLine);

            // update model
            model.moveTo(hit.getInsertionIndex(), selectionPolicy);
        }
    }

    private void prevLine(SelectionPolicy selectionPolicy) {
        downLines(selectionPolicy, -1);
    }

    private void nextLine(SelectionPolicy selectionPolicy) {
        downLines(selectionPolicy, 1);
    }

    private void prevPage(SelectionPolicy selectionPolicy) {
        view.showCaretAtBottom();
        CharacterHit hit = view.hit(getTargetCaretOffset(), 1.0);
        model.moveTo(hit.getInsertionIndex(), selectionPolicy);
    }

    private void nextPage(SelectionPolicy selectionPolicy) {
        view.showCaretAtTop();
        CharacterHit hit = view.hit(getTargetCaretOffset(), view.getViewportHeight() - 1.0);
        model.moveTo(hit.getInsertionIndex(), selectionPolicy);
    }


    /* ********************************************************************** *
     * Mouse handling implementation                                          *
     * ********************************************************************** */

    private void mousePressed(MouseEvent e) {
        // don't respond if disabled
        if(view.isDisabled()) {
            return;
        }

        if(e.getButton() == MouseButton.PRIMARY) {
            // ensure focus
            view.requestFocus();

            CharacterHit hit = view.hit(e.getX(), e.getY());

            if(e.isShiftDown()) {
                // On Mac always extend selection,
                // switching anchor and caret if necessary.
                model.moveTo(
                        hit.getInsertionIndex(),
                        isMac ? SelectionPolicy.EXTEND : SelectionPolicy.ADJUST);
            } else {
                switch (e.getClickCount()) {
                    case 1: firstLeftPress(hit); break;
                    case 2: selectWord(); break;
                    case 3: model.selectLine(); break;
                    default: // do nothing
                }
            }

            e.consume();
        }
    }

    private void firstLeftPress(CharacterHit hit) {
        clearTargetCaretOffset();
        IndexRange selection = model.getSelection();
        if(view.isEditable() &&
                selection.getLength() != 0 &&
                hit.getCharacterIndex().isPresent() &&
                hit.getCharacterIndex().getAsInt() >= selection.getStart() &&
                hit.getCharacterIndex().getAsInt() < selection.getEnd()) {
            // press inside selection
            dragSelection = DragState.POTENTIAL_DRAG;
        } else {
            dragSelection = DragState.NO_DRAG;
            model.moveTo(hit.getInsertionIndex(), SelectionPolicy.CLEAR);
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
            model.positionCaret(hit.getInsertionIndex());
        } else {
            model.moveTo(hit.getInsertionIndex(), SelectionPolicy.ADJUST);
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
                model.moveTo(hit.getInsertionIndex(), SelectionPolicy.CLEAR);
                break;
            case DRAG:
                // move selection to the target position
                CharacterHit h = view.hit(e.getX(), e.getY());
                view.getOnSelectionDrop().accept(h.getInsertionIndex());
                // do nothing, handled by mouseDragReleased
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
