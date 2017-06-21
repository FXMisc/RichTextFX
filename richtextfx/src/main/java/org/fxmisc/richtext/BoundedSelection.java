package org.fxmisc.richtext;

import javafx.beans.value.ObservableValue;

/**
 * An object for encapsulating a selection in a given area that is bound to an underlying caret. In other words,
 * {@link #selectRange(int, int) selecting some range in the area} will move a caret in the same call.
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
 * <p>
 *     The selection is typically made using the {@link #getAnchorPosition() anchor's position} and
 *     the underlying {@link Caret#getPosition() caret's position}. Hence, {@link #selectRange(int, int)}
 *     is the typical method to use, although {@link #selectRange0(int, int)} can also be used.
 * </p>
 * <p>
 *     Be careful about calling the underlying {@link Caret#moveTo(int)} method. This will displace the caret
 *     from the selection bounds and may lead to undesirable/unexpected behavior. If this is done, a
 *     {@link #selectRange(int, int)} call will reposition the caret, so that it is either the start or end
 *     bound of this selection.
 * </p>
 */
public interface BoundedSelection extends UnboundedSelection {

    @Override
    default boolean isBoundToCaret() {
        return true;
    }

    @Override
    default BoundedSelection asBoundedSelection() {
        return this;
    }

    int getAnchorPosition();
    ObservableValue<Integer> anchorPositionProperty();

    int getAnchorParIndex();
    ObservableValue<Integer> anchorParIndexProperty();

    int getAnchorColPosition();
    ObservableValue<Integer> anchorColPositionProperty();

    /**
     * Positions the anchor and caretPosition explicitly,
     * effectively creating a selection.
     *
     * <p><b>Caution:</b> see {@link org.fxmisc.richtext.model.TextEditingArea#getAbsolutePosition(int, int)}
     * to know how the column index argument can affect the returned position.</p>
     */
    void selectRange(int anchorParagraph, int anchorColumn, int caretParagraph, int caretColumn);

    /**
     * Positions the anchor and caretPosition explicitly,
     * effectively creating a selection.
     */
    void selectRange(int anchorPosition, int caretPosition);

}
