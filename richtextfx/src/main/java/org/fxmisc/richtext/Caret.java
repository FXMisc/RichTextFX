package org.fxmisc.richtext;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.util.Duration;
import org.reactfx.value.Var;

import java.text.BreakIterator;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * An object for encapsulating a caret in a given area.
 *
 * <p>
 *     <b>"Position"</b> refers to the place in-between characters. In other words, every {@code "|"} in
 *     {@code "|t|e|x|t|"} is a valid position. There are two kinds of positions used here:</p>
 *     <ol>
 *         <li>
 *             {@link #getPosition()}, which refers to a position somewhere in the entire area's content.
 *             It's bounds are {@code 0 <= x <= area.getLength()}.
 *         </li>
 *         <li>
 *             {@link #getColumnPosition()}, which refers to a position somewhere in the current paragraph.
 *             It's bounds are {@code 0 <= x <= area.getParagraphLength(index)}.
 *         </li>
 *     </ol>
 *
 * Note: when parameter names are "position" without the "column" prefix, they refer to the position in the entire area.
 *
 * <p>
 *     <b>"Line"</b> refers either to a single paragraph when {@link GenericStyledArea#isWrapText() the area does not wrap
 *     its text} or to a line on a multi-line paragraph when the area does wrap its text.
 * </p>
 */
public interface Caret {

    /**
     * Determines whether the caret is visible. Those who wish to use the default configuration should use
     * {@link #AUTO} while those who want a more custom configuration should make a caret's {@code CaretVisibility}
     * value oscillate between {@link #ON} and {@link #OFF}.
     */
    public static enum CaretVisibility {
        /** Caret is displayed. */
        ON,
        /** Caret is displayed when area is focused, enabled, and editable. */
        AUTO,
        /** Caret is not displayed. */
        OFF
    }

    /* ********************************************************************** *
     *                                                                        *
     * Observables                                                            *
     *                                                                        *
     * Observables are "dynamic" (i.e. changing) characteristics of this      *
     * control. They are not directly settable by the client code, but change *
     * in response to user input and/or API actions.                          *
     *                                                                        *
     * ********************************************************************** */

    /** The position of the caret within the text */
    public ObservableValue<Integer> positionProperty();
    public int getPosition();

    /** The paragraph index that contains this caret */
    public ObservableValue<Integer> paragraphIndexProperty();
    public int getParagraphIndex();

    /** The line index of a multi-line paragraph that contains this caret */
    public ObservableValue<OptionalInt> lineIndexProperty();
    public OptionalInt getLineIndex();

    /** The column position of the caret on its given line */
    public ObservableValue<Integer> columnPositionProperty();
    public int getColumnPosition();

    /**
     * Whether to display the caret or not. Default value is
     * {@link CaretVisibility#AUTO}.
     */
    public Var<CaretVisibility> showCaretProperty();
    public CaretVisibility getShowCaret();
    public void setShowCaret(CaretVisibility value);

    /** Whether the caret is being shown in the viewport */
    public ObservableValue<Boolean> visibleProperty();
    public boolean isVisible();

    public ObservableValue<Duration> blinkRateProperty();
    public Duration getBlinkRate();
    public void setBlinkRate(Duration blinkRate);

    /**
     * The selectionBoundsProperty of the caret in the Screen's coordinate system or {@link Optional#empty()} if caret is not visible
     * in the viewport.
     */
    public ObservableValue<Optional<Bounds>> caretBoundsProperty();
    public Optional<Bounds> getCaretBounds();

    /**
     * Clears the caret's x offset
     */
    void clearTargetOffset();

    /**
     * Stores the caret's current column position, so that moving the caret vertically will keep it close to its
     * original offset in a line.
     */
    ParagraphBox.CaretOffsetX getTargetOffset();

    boolean isBeingUpdated();
    ObservableValue<Boolean> beingUpdatedProperty();

    /* ********************************************************************** *
     *                                                                        *
     * Actions                                                                *
     *                                                                        *
     * Actions change the state of this control. They typically cause a       *
     * change of one or more observables and/or produce an event.             *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Moves the caret to the given position in the area. If this caret is bound to a {@link CaretSelectionBind},
     * it displaces the caret from the selection by positioning only the caret to the new location without
     * also affecting the {@link CaretSelectionBind#getAnchorPosition()} bounded selection's anchor} or the
     * {@link CaretSelectionBind#getRange()} selection}.
     * <br>
     * This method can be used to achieve the special case of positioning the caret outside or inside the selection,
     * as opposed to always being at the boundary. Use with care.
     *
     * <p><b>Caution:</b> see {@link TextEditingArea#getAbsolutePosition(int, int)} to
     * know how the column index argument can affect the returned position.</p>
     */
    public void moveTo(int paragraphIndex, int columnPosition);

    /**
     * Moves the caret to the given position in the area. If this caret is bound to a {@link CaretSelectionBind},
     * it displaces the caret from the selection by positioning only the caret to the new location without
     * also affecting the {@link CaretSelectionBind#getAnchorPosition()} bounded selection's anchor} or the
     * {@link CaretSelectionBind#getRange()} selection}.
     * <br>
     * This method can be used to achieve the special case of positioning the caret outside or inside the selection,
     * as opposed to always being at the boundary. Use with care.
     */
    public void moveTo(int position);

    /**
     * Moves the caret to the beginning of the current paragraph.
     */
    void moveToParStart();

    /**
     * Moves the caret to the end of the current paragraph.
     */
    void moveToParEnd();

    /**
     * Moves the caret to the beginning of the area.
     */
    default void moveToAreaStart() {
        moveTo(0);
    }

    /**
     * Moves the caret to the end of the area.
     */
    void moveToAreaEnd();

    /**
     * Moves the caret backward one char in the text.
     */
    void moveToPrevChar();

    /**
     * Moves the caret forward one char in the text.
     */
    void moveToNextChar();

    /**
     * Moves the caret forwards by the number of breaks.
     */
    void moveBreaksForwards(int numOfBreaks, BreakIterator breakIterator);

    default void moveWordBreaksForwards(int numOfWordBreaks) {
        moveBreaksForwards(numOfWordBreaks, BreakIterator.getWordInstance( getArea().getLocale() ));
    }

    default void moveSentenceBreaksForwards(int numOfSentenceBreaks) {
        moveBreaksForwards(numOfSentenceBreaks, BreakIterator.getSentenceInstance( getArea().getLocale() ));
    }

    /**
     * Moves the caret backwards by the number of breaks.
     */
    void moveBreaksBackwards(int numOfBreaks, BreakIterator breakIterator);

    default void moveWordBreaksBackwards(int numOfWordBreaks) {
        moveBreaksBackwards(numOfWordBreaks, BreakIterator.getWordInstance( getArea().getLocale() ));
    }

    default void moveSentenceBreaksBackwards(int numOfSentenceBreaks) {
        moveBreaksBackwards(numOfSentenceBreaks, BreakIterator.getSentenceInstance( getArea().getLocale() ));
    }

    /**
     * Disposes the caret and prevents memory leaks
     */
    public void dispose();

    /**
     * Gets the area with which this caret is associated.
     */
    public GenericStyledArea<?, ?, ?> getArea();

    /**
     * Gets the name of this caret. Each caret that is added to an area must have a unique name since it is used
     * to distinguish it from others in a Set.
     */
    public String getCaretName();
}
