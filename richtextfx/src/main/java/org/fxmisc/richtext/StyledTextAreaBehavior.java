package org.fxmisc.richtext;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyCombination.*;
import static javafx.scene.input.KeyEvent.*;
import static org.fxmisc.richtext.TwoDimensional.Bias.*;
import static org.fxmisc.wellbehaved.event.EventPattern.*;
import static org.reactfx.EventStreams.*;

import java.util.Optional;
import java.util.function.Predicate;

import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import org.fxmisc.richtext.NavigationActions.SelectionPolicy;
import org.fxmisc.richtext.TwoDimensional.Position;
import org.fxmisc.richtext.ParagraphBox.CaretOffsetX;
import org.fxmisc.wellbehaved.event.EventHandlerHelper;
import org.fxmisc.wellbehaved.event.EventHandlerTemplate;
import org.fxmisc.wellbehaved.skin.Behavior;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

/**
 * Controller for StyledTextArea.
 */
class StyledTextAreaBehavior implements Behavior {

    private static final boolean isMac;
    private static final boolean isWindows;
    static {
        String os = System.getProperty("os.name");
        isMac = os.startsWith("Mac");
        isWindows = os.startsWith("Windows");
    }

    private static final EventHandlerTemplate<StyledTextAreaBehavior, ? super KeyEvent> KEY_PRESSED_TEMPLATE;
    private static final EventHandlerTemplate<StyledTextAreaBehavior, ? super KeyEvent> KEY_TYPED_TEMPLATE;
    private static final EventHandlerTemplate<StyledTextAreaBehavior, ? super MouseEvent> MOUSE_PRESSED_TEMPLATE;
    private static final EventHandlerTemplate<StyledTextAreaBehavior, ? super MouseEvent> MOUSE_DRAGGED_TEMPLATE;
    private static final EventHandlerTemplate<StyledTextAreaBehavior, ? super MouseEvent> DRAG_DETECTED_TEMPLATE;
    private static final EventHandlerTemplate<StyledTextAreaBehavior, ? super MouseEvent> MOUSE_RELEASED_TEMPLATE;

    static {
        SelectionPolicy selPolicy = isMac
                ? SelectionPolicy.EXTEND
                : SelectionPolicy.ADJUST;

        EventHandlerTemplate<StyledTextAreaBehavior, KeyEvent> edits = EventHandlerTemplate
                // deletion
                .on(keyPressed(DELETE))                   .act(StyledTextAreaBehavior::deleteForward)
                .on(keyPressed(BACK_SPACE))               .act(StyledTextAreaBehavior::deleteBackward)
                .on(keyPressed(DELETE,     SHORTCUT_DOWN)).act(StyledTextAreaBehavior::deleteNextWord)
                .on(keyPressed(BACK_SPACE, SHORTCUT_DOWN)).act(StyledTextAreaBehavior::deletePrevWord)
                // cut
                .on(keyPressed(CUT))               .act((b, e) -> b.view.cut())
                .on(keyPressed(X, SHORTCUT_DOWN))  .act((b, e) -> b.view.cut())
                .on(keyPressed(DELETE, SHIFT_DOWN)).act((b, e) -> b.view.cut())
                // paste
                .on(keyPressed(PASTE))             .act((b, e) -> b.view.paste())
                .on(keyPressed(V, SHORTCUT_DOWN))  .act((b, e) -> b.view.paste())
                .on(keyPressed(INSERT, SHIFT_DOWN)).act((b, e) -> b.view.paste())
                // tab & newline
                .on(keyPressed(ENTER)).act((b, e) -> b.model.replaceSelection("\n"))
                .on(keyPressed(TAB))  .act((b, e) -> b.model.replaceSelection("\t"))
                // undo/redo,
                .on(keyPressed(Z, SHORTCUT_DOWN))            .act((b, e) -> b.model.undo())
                .on(keyPressed(Y, SHORTCUT_DOWN))            .act((b, e) -> b.model.redo())
                .on(keyPressed(Z, SHORTCUT_DOWN, SHIFT_DOWN)).act((b, e) -> b.model.redo())

                .create()
                .onlyWhen(b -> b.view.isEditable());

        EventHandlerTemplate<StyledTextAreaBehavior, KeyEvent> verticalNavigation = EventHandlerTemplate
                .<StyledTextAreaBehavior, KeyEvent, KeyEvent>
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

        EventHandlerTemplate<StyledTextAreaBehavior, KeyEvent> otherNavigation = EventHandlerTemplate
                // caret movement
                .on(keyPressed(RIGHT))   .act(StyledTextAreaBehavior::right)
                .on(keyPressed(KP_RIGHT)).act(StyledTextAreaBehavior::right)
                .on(keyPressed(LEFT))    .act(StyledTextAreaBehavior::left)
                .on(keyPressed(KP_LEFT)) .act(StyledTextAreaBehavior::left)
                .on(keyPressed(HOME))    .act((b, e) -> b.model.lineStart(SelectionPolicy.CLEAR))
                .on(keyPressed(END))     .act((b, e) -> b.model.lineEnd(SelectionPolicy.CLEAR))
                .on(keyPressed(RIGHT,    SHORTCUT_DOWN)).act((b, e) -> b.model.wordBreaksForwards(2, SelectionPolicy.CLEAR))
                .on(keyPressed(KP_RIGHT, SHORTCUT_DOWN)).act((b, e) -> b.model.wordBreaksForwards(2, SelectionPolicy.CLEAR))
                .on(keyPressed(LEFT,     SHORTCUT_DOWN)).act((b, e) -> b.model.wordBreaksBackwards(2, SelectionPolicy.CLEAR))
                .on(keyPressed(KP_LEFT,  SHORTCUT_DOWN)).act((b, e) -> b.model.wordBreaksBackwards(2, SelectionPolicy.CLEAR))
                .on(keyPressed(HOME,     SHORTCUT_DOWN)).act((b, e) -> b.model.start(SelectionPolicy.CLEAR))
                .on(keyPressed(END,      SHORTCUT_DOWN)).act((b, e) -> b.model.end(SelectionPolicy.CLEAR))
                // selection
                .on(keyPressed(RIGHT,    SHIFT_DOWN)).act(StyledTextAreaBehavior::selectRight)
                .on(keyPressed(KP_RIGHT, SHIFT_DOWN)).act(StyledTextAreaBehavior::selectRight)
                .on(keyPressed(LEFT,     SHIFT_DOWN)).act(StyledTextAreaBehavior::selectLeft)
                .on(keyPressed(KP_LEFT,  SHIFT_DOWN)).act(StyledTextAreaBehavior::selectLeft)
                .on(keyPressed(HOME,     SHIFT_DOWN)).act((b, e) -> b.model.lineStart(selPolicy))
                .on(keyPressed(END,      SHIFT_DOWN)).act((b, e) -> b.model.lineEnd(selPolicy))
                .on(keyPressed(HOME,     SHIFT_DOWN, SHORTCUT_DOWN)).act((b, e) -> b.model.start(selPolicy))
                .on(keyPressed(END,      SHIFT_DOWN, SHORTCUT_DOWN)).act((b, e) -> b.model.end(selPolicy))
                .on(keyPressed(LEFT,     SHIFT_DOWN, SHORTCUT_DOWN)).act((b, e) -> b.model.wordBreaksBackwards(2, selPolicy))
                .on(keyPressed(KP_LEFT,  SHIFT_DOWN, SHORTCUT_DOWN)).act((b, e) -> b.model.wordBreaksBackwards(2, selPolicy))
                .on(keyPressed(RIGHT,    SHIFT_DOWN, SHORTCUT_DOWN)).act((b, e) -> b.model.wordBreaksForwards(2, selPolicy))
                .on(keyPressed(KP_RIGHT, SHIFT_DOWN, SHORTCUT_DOWN)).act((b, e) -> b.model.wordBreaksForwards(2, selPolicy))
                .on(keyPressed(A, SHORTCUT_DOWN)).act((b, e) -> b.model.selectAll())

                .create();

        EventHandlerTemplate<StyledTextAreaBehavior, KeyEvent> otherActions = EventHandlerTemplate
                .<StyledTextAreaBehavior, KeyEvent, KeyEvent>
                // copy
                on(keyPressed(COPY))                 .act((b, e) -> b.view.copy())
                .on(keyPressed(C,      SHORTCUT_DOWN)).act((b, e) -> b.view.copy())
                .on(keyPressed(INSERT, SHORTCUT_DOWN)).act((b, e) -> b.view.copy())
                .create();

        Predicate<KeyEvent> noControlKeys = e ->
                // filter out control keys
                (!e.isControlDown() && !e.isMetaDown())
                // except on Windows allow the Ctrl+Alt combination (produced by AltGr)
                || (isWindows && !e.isMetaDown() && (!e.isControlDown() || e.isAltDown()));

        Predicate<KeyEvent> isChar = e ->
                e.getCode().isLetterKey() ||
                e.getCode().isDigitKey() ||
                e.getCode().isWhitespaceKey();

        EventHandlerTemplate<StyledTextAreaBehavior, KeyEvent> charPressConsumer = EventHandlerTemplate
                .<StyledTextAreaBehavior, KeyEvent, KeyEvent>
                on(keyPressed()).where(isChar.and(noControlKeys)).act((b, e) -> {})
                .create();

        KEY_PRESSED_TEMPLATE = edits.orElse(otherNavigation).ifConsumed((b, e) -> b.clearTargetCaretOffset())
                .orElse(verticalNavigation)
                .orElse(otherActions)
                .orElse(charPressConsumer);

        KEY_TYPED_TEMPLATE = EventHandlerTemplate
                // character input
                .on(KEY_TYPED)
                .where(noControlKeys)
                .where(e -> isLegal(e.getCharacter()))
                .act(StyledTextAreaBehavior::keyTyped)

                .create()
                .onlyWhen(b -> b.view.isEditable());

        MOUSE_PRESSED_TEMPLATE = EventHandlerTemplate
                .on(MouseEvent.MOUSE_PRESSED)
                .act(StyledTextAreaBehavior::mousePressed)
                .create();

        MOUSE_DRAGGED_TEMPLATE = EventHandlerTemplate
                .on(MouseEvent.MOUSE_DRAGGED)
                .act(StyledTextAreaBehavior::mouseDragged)
                .create();

        DRAG_DETECTED_TEMPLATE = EventHandlerTemplate
                .on(MouseEvent.DRAG_DETECTED)
                .act(StyledTextAreaBehavior::dragDetected)
                .create();

        MOUSE_RELEASED_TEMPLATE = EventHandlerTemplate
                .on(MouseEvent.MOUSE_RELEASED)
                .act(StyledTextAreaBehavior::mouseReleased)
                .create();
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

    private final Subscription subscription;


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

        EventHandler<? super KeyEvent> keyPressedHandler = KEY_PRESSED_TEMPLATE.bind(this);
        EventHandler<? super KeyEvent> keyTypedHandler = KEY_TYPED_TEMPLATE.bind(this);

        EventHandler<? super MouseEvent> mousePressedHandler = MOUSE_PRESSED_TEMPLATE.bind(this);
        EventHandler<? super MouseEvent> mouseDraggedHandler = MOUSE_DRAGGED_TEMPLATE.bind(this);
        EventHandler<? super MouseEvent> dragDetectedHandler = DRAG_DETECTED_TEMPLATE.bind(this);
        EventHandler<? super MouseEvent> mouseReleasedHandler = MOUSE_RELEASED_TEMPLATE.bind(this);

        EventHandlerHelper.installAfter(area.onKeyPressedProperty(), keyPressedHandler);
        EventHandlerHelper.installAfter(area.onKeyTypedProperty(), keyTypedHandler);

        EventHandlerHelper.installAfter(area.onMousePressedProperty(), mousePressedHandler);
        EventHandlerHelper.installAfter(area.onMouseDraggedProperty(), mouseDraggedHandler);
        EventHandlerHelper.installAfter(area.onDragDetectedProperty(), dragDetectedHandler);
        EventHandlerHelper.installAfter(area.onMouseReleasedProperty(), mouseReleasedHandler);

        subscription = () -> {
                    EventHandlerHelper.remove(area.onKeyPressedProperty(), keyPressedHandler);
                    EventHandlerHelper.remove(area.onKeyTypedProperty(), keyTypedHandler);

                    EventHandlerHelper.remove(area.onMousePressedProperty(), mousePressedHandler);
                    EventHandlerHelper.remove(area.onMouseDraggedProperty(), mouseDraggedHandler);
                    EventHandlerHelper.remove(area.onDragDetectedProperty(), dragDetectedHandler);
                    EventHandlerHelper.remove(area.onMouseReleasedProperty(), mouseReleasedHandler);
                };

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
