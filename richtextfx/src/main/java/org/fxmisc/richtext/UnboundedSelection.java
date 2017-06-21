package org.fxmisc.richtext;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.control.IndexRange;
import org.reactfx.EventStream;

import java.util.Optional;

/**
 * An object for encapsulating a selection in a given area that is not bound to any caret. In other words,
 * {@link #selectRange0(int, int) selecting some range in the area} will not also move a caret in the same call.
 *
 * <p>
 *     <b>"Position"</b> refers to the place in-between characters. In other words, every {@code "|"} in
 *     {@code "|t|e|x|t|"} is a valid position. There are two kinds of positions used here:</p>
 *     <ol>
 *         <li>
 *             {@link #getStartPosition()}/{@link #getEndPosition()}, which refers to a position somewhere in the
 *             entire area's content. It's bounds are {@code 0 <= x < area.getLength()}.
 *         </li>
 *         <li>
 *             {@link #getStartColumnPosition()}/{@link #getEndColumnPosition()}, which refers to a position
 *             somewhere in the current paragraph. It's bounds are {@code 0 <= x < area.getParagraphLength(index)}.
 *         </li>
 *     </ol>
 *
 * Note: when parameter names are "position" without the "column" prefix, they refer to the position in the entire area.
 *
 * @see BoundedSelection
 * @see Caret
 */
public interface UnboundedSelection {

    public static enum Direction {
        LEFT,
        RIGHT
    }

    /**
     * Returns true if this is an {@link UnboundedSelection} and true if this is an {@link BoundedSelection}.
     */
    default boolean isBoundToCaret() {
        return false;
    }

    /**
     * If {@link #isBoundToCaret()} returns true, then casts this object to a {@link BoundedSelection}. Otherwise,
     * throws an {@link IllegalStateException}.
     */
    default BoundedSelection asBoundedSelection() {
        throw new IllegalStateException("An UnboundedSelection cannot be cast to a BoundedSelection");
    }

    /**
     * The start and end positions in the area as an {@link IndexRange}.
     */
    ObservableValue<IndexRange> rangeProperty();
    IndexRange getRange();

    /**
     * The length of the selection
     */
    ObservableValue<Integer> lengthProperty();
    int getLength();

    /**
     * The number of paragraphs the selection spans
     */
    ObservableValue<Integer> paragraphSpanProperty();
    int getParagraphSpan();

    ObservableValue<String> selectedTextProperty();
    String getSelectedText();


    ObservableValue<Integer> startPositionProperty();
    int getStartPosition();

    ObservableValue<Integer> startParagraphIndexProperty();
    int getStartParagraphIndex();

    ObservableValue<Integer> startColumnPositionProperty();
    int getStartColumnPosition();


    ObservableValue<Integer> endPositionProperty();
    int getEndPosition();

    ObservableValue<Integer> endPararagraphIndexProperty();
    int getEndPararagraphIndex();

    ObservableValue<Integer> endColumnPositionProperty();
    int getEndColumnPosition();


    /**
     * The boundsProperty of the selection in the Screen's coordinate system if something is selected and visible in the
     * viewport, or {@link Optional#empty()} if selection is not visible in the viewport.
     */
    ObservableValue<Optional<Bounds>> boundsProperty();
    Optional<Bounds> getBounds();

    /**
     * Emits an event every time the selection becomes dirty: the start/end positions change or the area's paragraphs
     * change.
     */
    EventStream<?> dirtyEvents();

    boolean isBeingUpdated();
    ObservableValue<Boolean> beingUpdatedProperty();


    /**
     * Selects the given range. Note: this method's "0" suffix distinguishes it's signature from
     * {@link BoundedSelection#selectRange(int, int, int, int)}.
     *
     * <p><b>Caution:</b> see {@link org.fxmisc.richtext.model.TextEditingArea#getAbsolutePosition(int, int)} to
     * know how the column index argument can affect the returned position.</p>
     */
    void selectRange0(int startParagraphIndex, int startColPosition, int endParagraphIndex, int endColPosition);

    /**
     * Selects the given range. Note: this method's "0" suffix distinguishes it's signature from
     * {@link BoundedSelection#selectRange(int, int)}.
     */
    void selectRange0(int startPosition, int endPosition);

    void moveStartBy(int amount, Direction direction);

    void moveEndBy(int amount, Direction direction);

    void moveStartTo(int position);

    void moveStartTo(int paragraphIndex, int columnPosition);

    void moveEndTo(int position);

    void moveEndTo(int paragraphIndex, int columnPosition);

    /**
     * Disposes the selection and prevents memory leaks
     */
    void dispose();

}
