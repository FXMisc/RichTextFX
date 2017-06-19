package org.fxmisc.richtext;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import org.reactfx.EventStream;
import org.reactfx.value.Var;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * An object for encapsulating a caret in a given area.
 *
 * <p>
 *     <b>"Position"</b> refers to the place in-between characters. In other words, every {@code "|"} in
 *     {@code "|t|e|x|t|"} is a valid position. There are two kinds of positions used here:
 *     <ol>
 *         <li>
 *             {@link #getPosition()}, which refers to a position somewhere in the entire area's content.
 *             It's bounds are {@code 0 < x < area.getLength()}.
 *         </li>
 *         <li>
 *             {@link #getColumnPosition()}, which refers to a position somewhere in the current paragraph. It's
 *             bounds are {@code 0 < x < area.getParagraphLength(index)}.
 *         </li>
 *     </ol>
 *     <br>
 *         Note: when parameter names are "position" without the "column" prefix, they refer to
 *         the position in the entire area.
 * </p>
 * <p>
 *     <b>"Line"</b> refers either to a single paragraph when {@link GenericStyledArea#isWrapText() the area does not wrap
 *     its text} or to a line on a multi-line paragraph when the area does wrap its text.
 * </p>
 */
public interface Caret {

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
     * {@link org.fxmisc.richtext.ViewActions.CaretVisibility#AUTO}.
     */
    public Var<GenericStyledArea.CaretVisibility> showCaretProperty();
    public GenericStyledArea.CaretVisibility getShowCaret();
    public void setShowCaret(GenericStyledArea.CaretVisibility value);

    /** Whether the caret is being shown in the viewport */
    public ObservableValue<Boolean> visibleProperty();
    public boolean isVisible();

    /**
     * The boundsProperty of the caret in the Screen's coordinate system or {@link Optional#empty()} if caret is not visible
     * in the viewport.
     */
    public ObservableValue<Optional<Bounds>> boundsProperty();
    public Optional<Bounds> getBounds();

    /**
     * Clears the caret's x offset
     */
    void clearTargetOffset();

    /**
     * Stores the caret's current column position, so that moving the caret vertically will keep it close to its
     * original offset in a line.
     */
    ParagraphBox.CaretOffsetX getTargetOffset();

    /** Emit an event whenever the caret's position becomes dirty */
    public EventStream<?> dirtyEvents();

    boolean isBeingUpdated();
    ObservableValue<Boolean> beingUpdatedProperty();

    /**
     * Moves the caret to the given position in the area.
     *
     * <p><b>Caution:</b> see {@link org.fxmisc.richtext.model.TextEditingArea#getAbsolutePosition(int, int)} to
     * know how the column index argument can affect the returned position.</p>
     */
    public void moveTo(int paragraphIndex, int columnPosition);

    /**
     * Moves the caret to the given position in the area.
     */
    public void moveTo(int position);

    /**
     * Disposes the caret and prevents memory leaks
     */
    public void dispose();

}
