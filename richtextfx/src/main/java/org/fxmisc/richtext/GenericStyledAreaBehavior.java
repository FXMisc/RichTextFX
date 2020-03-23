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
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import org.fxmisc.richtext.NavigationActions.SelectionPolicy;
import org.fxmisc.richtext.model.TwoDimensional.Position;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputHandler.Result;
import org.fxmisc.wellbehaved.event.template.InputMapTemplate;
import org.reactfx.EventStream;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

/**
 * Controller for GenericStyledArea.
 */
class GenericStyledAreaBehavior {

    private static final boolean isMac;
    private static final boolean isWindows;
    static {
        String os = System.getProperty("os.name");
        isMac = os.startsWith("Mac");
        isWindows = os.startsWith("Windows");
    }

    private static final InputMapTemplate<GenericStyledAreaBehavior, ? super Event> EVENT_TEMPLATE;

    static {
        SelectionPolicy selPolicy = isMac
                ? SelectionPolicy.EXTEND
                : SelectionPolicy.ADJUST;

        /*
         * KeyCodes are misinterpreted when using a different keyboard layout, for example:
         * on Dvorak: C results in KeyCode I, X -> B, and V -> . 
         * and on German layouts: Z and Y are reportedly switched
         * so then editing commands such as Ctrl+C, or CMD+Z are incorrectly processed.
         * KeyCharacterCombination however does keyboard translation before matching.
         * This resolves issue #799
         */
        KeyCharacterCombination SHORTCUT_A = new KeyCharacterCombination( "a", SHORTCUT_DOWN );
        KeyCharacterCombination SHORTCUT_C = new KeyCharacterCombination( "c", SHORTCUT_DOWN );
        KeyCharacterCombination SHORTCUT_V = new KeyCharacterCombination( "v", SHORTCUT_DOWN );
        KeyCharacterCombination SHORTCUT_X = new KeyCharacterCombination( "x", SHORTCUT_DOWN );
        KeyCharacterCombination SHORTCUT_Y = new KeyCharacterCombination( "y", SHORTCUT_DOWN );
        KeyCharacterCombination SHORTCUT_Z = new KeyCharacterCombination( "z", SHORTCUT_DOWN );
        KeyCharacterCombination SHORTCUT_SHIFT_Z = new KeyCharacterCombination( "z", SHORTCUT_DOWN, SHIFT_DOWN );
		
        InputMapTemplate<GenericStyledAreaBehavior, KeyEvent> editsBase = sequence(
                // deletion
                consume(keyPressed(DELETE),                     GenericStyledAreaBehavior::deleteForward),
                consume(keyPressed(BACK_SPACE),                 GenericStyledAreaBehavior::deleteBackward),
                consume(keyPressed(BACK_SPACE, SHIFT_DOWN),     GenericStyledAreaBehavior::deleteBackward),
                consume(keyPressed(DELETE,     SHORTCUT_DOWN),  GenericStyledAreaBehavior::deleteNextWord),
                consume(keyPressed(BACK_SPACE, SHORTCUT_DOWN),  GenericStyledAreaBehavior::deletePrevWord),
                // cut
                consume(
                        anyOf(keyPressed(CUT),   keyPressed(SHORTCUT_X), keyPressed(DELETE, SHIFT_DOWN)),
                        (b, e) -> b.view.cut()),
                // paste
                consume(
                        anyOf(keyPressed(PASTE), keyPressed(SHORTCUT_V), keyPressed(INSERT, SHIFT_DOWN)),
                        (b, e) -> b.view.paste()),
                // tab & newline
                consume(keyPressed(ENTER), (b, e) -> b.view.replaceSelection("\n")),
                consume(keyPressed(TAB),   (b, e) -> b.view.replaceSelection("\t")),
                // undo/redo
                consume(keyPressed(SHORTCUT_Z), (b, e) -> b.view.undo()),
                consume(
                        anyOf(keyPressed(SHORTCUT_Y), keyPressed(SHORTCUT_SHIFT_Z)),
                        (b, e) -> b.view.redo())
        );
        InputMapTemplate<GenericStyledAreaBehavior, KeyEvent> edits = when(b -> b.view.isEditable(), editsBase);

        InputMapTemplate<GenericStyledAreaBehavior, KeyEvent> verticalNavigation = sequence(
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

        InputMapTemplate<GenericStyledAreaBehavior, KeyEvent> otherNavigation = sequence(
                // caret movement
                consume(anyOf(keyPressed(RIGHT), keyPressed(KP_RIGHT)), GenericStyledAreaBehavior::right),
                consume(anyOf(keyPressed(LEFT),  keyPressed(KP_LEFT)),  GenericStyledAreaBehavior::left),
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
                        ), GenericStyledAreaBehavior::selectRight),
                consume(
                        anyOf(
                                keyPressed(LEFT,     SHIFT_DOWN),
                                keyPressed(KP_LEFT,  SHIFT_DOWN)
                        ), GenericStyledAreaBehavior::selectLeft),
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
                consume(keyPressed(SHORTCUT_A), (b, e) -> b.view.selectAll())
        );

        InputMapTemplate<GenericStyledAreaBehavior, KeyEvent> copyAction = consume(
                anyOf(
                        keyPressed(COPY),
                        keyPressed(SHORTCUT_C),
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

        InputMapTemplate<GenericStyledAreaBehavior, KeyEvent> charPressConsumer = consume(keyPressed().onlyIf(isChar.and(noControlKeys)));

        InputMapTemplate<GenericStyledAreaBehavior, ? super KeyEvent> keyPressedTemplate = edits
                .orElse(otherNavigation).ifConsumed((b, e) -> b.view.clearTargetCaretOffset())
                .orElse(verticalNavigation)
                .orElse(copyAction)
                .ifConsumed((b, e) -> b.view.requestFollowCaret())
                // no need to add 'ifConsumed' after charPress since
                // requestFollowCaret is called in keyTypedTemplate
                .orElse(charPressConsumer);

        InputMapTemplate<GenericStyledAreaBehavior, KeyEvent> keyTypedBase = consume(
                // character input
                EventPattern.keyTyped().onlyIf(noControlKeys.and(e -> isLegal(e.getCharacter()))),
                GenericStyledAreaBehavior::keyTyped
        ).ifConsumed((b, e) -> b.view.requestFollowCaret());
        InputMapTemplate<GenericStyledAreaBehavior, ? super KeyEvent> keyTypedTemplate = when(b -> b.view.isEditable(), keyTypedBase);

        InputMapTemplate<GenericStyledAreaBehavior, ? super MouseEvent> mousePressedTemplate = sequence(
                // ignore mouse pressed events if the view is disabled
                process(mousePressed(MouseButton.PRIMARY), (b, e) -> b.view.isDisabled() ? Result.IGNORE : Result.PROCEED),

                // hide context menu before any other handling
                process(
                        mousePressed(), (b, e) -> {
                            b.view.hideContextMenu();
                            return Result.PROCEED;
                        }
                ),
                consume(
                        mousePressed(MouseButton.PRIMARY).onlyIf(MouseEvent::isShiftDown),
                        GenericStyledAreaBehavior::handleShiftPress
                ),
                consume(
                        mousePressed(MouseButton.PRIMARY).onlyIf(e -> e.getClickCount() == 1),
                        GenericStyledAreaBehavior::handleFirstPrimaryPress
                ),
                consume(
                        mousePressed(MouseButton.PRIMARY).onlyIf(e -> e.getClickCount() == 2),
                        GenericStyledAreaBehavior::handleSecondPress
                ),
                consume(
                        mousePressed(MouseButton.PRIMARY).onlyIf(e -> e.getClickCount() == 3),
                        GenericStyledAreaBehavior::handleThirdPress
                )
        );

        Predicate<MouseEvent> primaryOnlyButton = e -> e.getButton() == MouseButton.PRIMARY && !e.isMiddleButtonDown() && !e.isSecondaryButtonDown();

        InputMapTemplate<GenericStyledAreaBehavior, ? super MouseEvent> mouseDragDetectedTemplate = consume(
                eventType(MouseEvent.DRAG_DETECTED).onlyIf(primaryOnlyButton),
                (b, e) -> b.handlePrimaryOnlyDragDetected()
        );

        InputMapTemplate<GenericStyledAreaBehavior, ? super MouseEvent> mouseDragTemplate = sequence(
                process(
                        mouseDragged().onlyIf(primaryOnlyButton),
                        GenericStyledAreaBehavior::processPrimaryOnlyMouseDragged
                ),
                consume(
                        mouseDragged(),
                        GenericStyledAreaBehavior::continueOrStopAutoScroll
                )
        );

        InputMapTemplate<GenericStyledAreaBehavior, ? super MouseEvent> mouseReleasedTemplate = sequence(
                process(
                        EventPattern.mouseReleased().onlyIf(primaryOnlyButton),
                        GenericStyledAreaBehavior::processMouseReleased
                ),
                consume(
                        mouseReleased(),
                        (b, e) -> b.autoscrollTo.setValue(null)  // stop auto scroll
                )
        );

        InputMapTemplate<GenericStyledAreaBehavior, ? super MouseEvent> mouseTemplate = sequence(
                mousePressedTemplate, mouseDragDetectedTemplate, mouseDragTemplate, mouseReleasedTemplate
        );

        InputMapTemplate<GenericStyledAreaBehavior, ? super ContextMenuEvent> contextMenuEventTemplate = consumeWhen(
                EventPattern.eventType(ContextMenuEvent.CONTEXT_MENU_REQUESTED),
                b -> !b.view.isDisabled(),
                GenericStyledAreaBehavior::showContextMenu
        );

        EVENT_TEMPLATE = sequence(mouseTemplate, keyPressedTemplate, keyTypedTemplate, contextMenuEventTemplate);
    }

    /**
     * Possible dragging states.
     */
    private enum DragState {
        /** No dragging is happening. */
        NO_DRAG,

        /** Mouse has been pressed inside of selected text, but drag has not been detected yet. */
        POTENTIAL_DRAG,

        /** Drag in progress. */
        DRAG,
    }

    /* ********************************************************************** *
     * Fields                                                                 *
     * ********************************************************************** */

    private final GenericStyledArea<?, ?, ?> view;

    /**
     * Indicates whether an existing selection is being dragged by the user.
     */
    private DragState dragSelection = DragState.NO_DRAG;

    /**
     * Indicates whether a new selection is being made by the user.
     */
    private DragState dragNewSelection = DragState.NO_DRAG;

    private final Var<Point2D> autoscrollTo = Var.newSimpleVar(null);

    /* ********************************************************************** *
     * Constructors                                                           *
     * ********************************************************************** */

    GenericStyledAreaBehavior(GenericStyledArea<?, ?, ?> area) {
        this.view = area;

        InputMapTemplate.installFallback(EVENT_TEMPLATE, this, b -> b.view);

        // setup auto-scroll
        Val<Point2D> projection = Val.combine(
                autoscrollTo,
                area.layoutBoundsProperty(),
                GenericStyledAreaBehavior::project);
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
        view.requestFocus();
        if ( view.isContextMenuPresent() ) {
            ContextMenu menu = view.getContextMenu();
            double x = e.getScreenX() + view.getContextMenuXOffset();
            double y = e.getScreenY() + view.getContextMenuYOffset();
            menu.show( view, x, y );
        }
    }

    private void handleShiftPress(MouseEvent e) {
        // ensure focus
        view.requestFocus();

        CharacterHit hit = view.hit(e.getX(), e.getY());

        // On Mac always extend selection,
        // switching anchor and caret if necessary.
        view.moveTo(hit.getInsertionIndex(), isMac ? SelectionPolicy.EXTEND : SelectionPolicy.ADJUST);
    }

    private void handleFirstPrimaryPress(MouseEvent e) {
        // ensure focus
        view.requestFocus();

        CharacterHit hit = view.hit(e.getX(), e.getY());

        view.clearTargetCaretOffset();
        IndexRange selection = view.getSelection();
        if(view.isEditable() &&
                selection.getLength() != 0 &&
                hit.getCharacterIndex().isPresent() &&
                hit.getCharacterIndex().getAsInt() >= selection.getStart() &&
                hit.getCharacterIndex().getAsInt() < selection.getEnd()) {
            // press inside selection
            dragSelection = DragState.POTENTIAL_DRAG;
            dragNewSelection = DragState.NO_DRAG;
        } else {
            dragSelection = DragState.NO_DRAG;
            dragNewSelection = DragState.NO_DRAG;
            view.getOnOutsideSelectionMousePressed().handle(e);
        }
    }

    private void handleSecondPress(MouseEvent e) {
        view.selectWord();
    }

    private void handleThirdPress(MouseEvent e) {
        view.selectParagraph();
    }

    private void handlePrimaryOnlyDragDetected() {
        if (dragSelection == DragState.POTENTIAL_DRAG) {
            dragSelection = DragState.DRAG;
        }
        else dragNewSelection = DragState.DRAG;
    }

    private Result processPrimaryOnlyMouseDragged(MouseEvent e) {
        Point2D p = new Point2D(e.getX(), e.getY());
        if(view.getLayoutBounds().contains(p)) {
            dragTo(p);
        }
        view.setAutoScrollOnDragDesired(true);
        // autoScrollTo will be set in "continueOrStopAutoScroll(MouseEvent)"
        return Result.PROCEED;
    }

    private void continueOrStopAutoScroll(MouseEvent e) {
        if (!view.isAutoScrollOnDragDesired()) {
            autoscrollTo.setValue(null); // stops auto-scroll
        }

        Point2D p = new Point2D(e.getX(), e.getY());
        if(view.getLayoutBounds().contains(p)) {
            autoscrollTo.setValue(null); // stops auto-scroll
        } else {
            autoscrollTo.setValue(p);    // starts auto-scroll
        }
    }

    private void dragTo(Point2D point) {
        if(dragSelection == DragState.DRAG ||
                dragSelection == DragState.POTENTIAL_DRAG) { // MOUSE_DRAGGED may arrive even before DRAG_DETECTED
            view.getOnSelectionDrag().accept(point);
        } else {
            view.getOnNewSelectionDrag().accept(point);
        }
    }

    private Result processMouseReleased(MouseEvent e) {
        if (view.isDisabled()) {
            return Result.IGNORE;
        }

        switch(dragSelection) {
            case POTENTIAL_DRAG:
                // selection was not dragged, but clicked
                view.getOnInsideSelectionMousePressReleased().handle(e);
            case DRAG:
                view.getOnSelectionDropped().handle(e);
                break;
            case NO_DRAG: if ( dragNewSelection == DragState.DRAG ) {
                view.getOnNewSelectionDragFinished().handle(e);
            }
        }
        dragNewSelection = DragState.NO_DRAG;
        dragSelection = DragState.NO_DRAG;

        return Result.PROCEED;
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
