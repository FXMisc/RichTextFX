package org.fxmisc.richtext.skin;

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
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TwoDimensional.Position;
import org.fxmisc.richtext.skin.ParagraphBox.CaretOffsetX;
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
public class StyledTextAreaBehavior implements Behavior {

    private static final boolean isMac;
    private static final boolean isWindows;
    static {
        String os = System.getProperty("os.name");
        isMac = os.startsWith("Mac");
        isWindows = os.startsWith("Windows");
    }

    private static final EventHandlerTemplate<StyledTextAreaBehavior, ? super KeyEvent> KEY_PRESSED_TEMPLATE;
    private static final EventHandlerTemplate<StyledTextAreaBehavior, ? super KeyEvent> KEY_TYPED_TEMPLATE;
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

                .create()
                .onlyWhen(b -> b.area.isEditable());

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

        EventHandlerTemplate<StyledTextAreaBehavior, KeyEvent> otherActions = EventHandlerTemplate
                .<StyledTextAreaBehavior, KeyEvent, KeyEvent>
                // copy
                 on(keyPressed(COPY))                 .act((b, e) -> b.area.copy())
                .on(keyPressed(C,      SHORTCUT_DOWN)).act((b, e) -> b.area.copy())
                .on(keyPressed(INSERT, SHORTCUT_DOWN)).act((b, e) -> b.area.copy())
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
                .onlyWhen(b -> b.area.isEditable());
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

    private final StyledTextArea<?, ?> area;
    private final StyledTextAreaVisual<?, ?> visual;
    private final StyledTextAreaView<?, ?> view;

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

    public StyledTextAreaBehavior(StyledTextAreaVisual<?, ?> visual) {
        this.area = visual.getControl();
        this.visual = visual;
        this.view = visual.getNode();

        EventHandler<? super KeyEvent> keyPressedHandler = KEY_PRESSED_TEMPLATE.bind(this);
        EventHandler<? super KeyEvent> keyTypedHandler = KEY_TYPED_TEMPLATE.bind(this);

        EventHandlerHelper.installAfter(area.onKeyPressedProperty(), keyPressedHandler);
        EventHandlerHelper.installAfter(area.onKeyTypedProperty(), keyTypedHandler);

        subscription = Subscription.multi(
                eventsOf(area, MouseEvent.MOUSE_PRESSED).subscribe(this::mousePressed),
                eventsOf(area, MouseEvent.MOUSE_DRAGGED).subscribe(this::mouseDragged),
                eventsOf(area, MouseEvent.DRAG_DETECTED).subscribe(this::dragDetected),
                eventsOf(area, MouseEvent.MOUSE_RELEASED).subscribe(this::mouseReleased),
                () -> {
                    EventHandlerHelper.remove(area.onKeyPressedProperty(), keyPressedHandler);
                    EventHandlerHelper.remove(area.onKeyTypedProperty(), keyTypedHandler);
                });

        // setup auto-scroll
        Val<Point2D> projection = Val.combine(
                autoscrollTo,
                view.layoutBoundsProperty(),
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
                view.scrollBy(ds);
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

        area.replaceSelection(text);
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
        Position currentLine = view.currentLine();
        Position targetLine = currentLine.offsetBy(nLines, Forward).clamp();
        if(!currentLine.sameAs(targetLine)) {
            // compute new caret position
            CharacterHit hit = view.hit(getTargetCaretOffset(), targetLine);

            // update model
            visual.getControl().moveTo(hit.getInsertionIndex(), selectionPolicy);
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
        visual.getControl().moveTo(hit.getInsertionIndex(), selectionPolicy);
    }

    private void nextPage(SelectionPolicy selectionPolicy) {
        view.showCaretAtTop();
        CharacterHit hit = view.hit(getTargetCaretOffset(), view.getViewportHeight() - 1.0);
        visual.getControl().moveTo(hit.getInsertionIndex(), selectionPolicy);
    }


    /* ********************************************************************** *
     * Mouse handling implementation                                          *
     * ********************************************************************** */

    private void mousePressed(MouseEvent e) {
        // don't respond if disabled
        if(area.isDisabled()) {
            return;
        }

        if(e.getButton() == MouseButton.PRIMARY) {
            // ensure focus
            area.requestFocus();

            CharacterHit hit = view.hit(e.getX(), e.getY());

            if(e.isShiftDown()) {
                // On Mac always extend selection,
                // switching anchor and caret if necessary.
                area.moveTo(
                        hit.getInsertionIndex(),
                        isMac ? SelectionPolicy.EXTEND : SelectionPolicy.ADJUST);
            } else {
                switch (e.getClickCount()) {
                    case 1: firstLeftPress(hit); break;
                    case 2: selectWord(); break;
                    case 3: area.selectLine(); break;
                    default: // do nothing
                }
            }

            e.consume();
        }
    }

    private void firstLeftPress(CharacterHit hit) {
        clearTargetCaretOffset();
        IndexRange selection = area.getSelection();
        if(area.isEditable() &&
                selection.getLength() != 0 &&
                hit.getCharacterIndex().isPresent() &&
                hit.getCharacterIndex().getAsInt() >= selection.getStart() &&
                hit.getCharacterIndex().getAsInt() < selection.getEnd()) {
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
        e.consume();
    }

    private void mouseDragged(MouseEvent e) {
        // don't respond if disabled
        if(area.isDisabled()) {
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
            area.positionCaret(hit.getInsertionIndex());
        } else {
            area.moveTo(hit.getInsertionIndex(), SelectionPolicy.ADJUST);
        }
    }

    private void mouseReleased(MouseEvent e) {
        // stop auto-scroll
        autoscrollTo.setValue(null);

        // don't respond if disabled
        if(area.isDisabled()) {
            return;
        }

        switch(dragSelection) {
            case POTENTIAL_DRAG:
                // drag didn't happen, position caret
                CharacterHit hit = view.hit(e.getX(), e.getY());
                area.moveTo(hit.getInsertionIndex(), SelectionPolicy.CLEAR);
                break;
            case DRAG:
                // move selection to the target position
                CharacterHit h = view.hit(e.getX(), e.getY());
                area.moveSelectedText(h.getInsertionIndex());
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
