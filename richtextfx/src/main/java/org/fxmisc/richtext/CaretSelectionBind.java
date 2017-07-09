package org.fxmisc.richtext;

import javafx.beans.value.ObservableValue;
import org.fxmisc.richtext.model.StyledDocument;

/**
 * An object for encapsulating a selection in a given area that is bound to an underlying caret. In other words,
 * {@link #selectRangeExpl(int, int) selecting some range in the area} will move a caret in the same call.
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
 *     the underlying {@link Caret#getPosition() caret's position}. Hence, {@link #selectRangeExpl(int, int)}
 *     is the typical method to use, although {@link #selectRange(int, int)} can also be used.
 * </p>
 * <p>
 *     Be careful about calling the underlying {@link Caret#moveTo(int)} method. This will displace the caret
 *     from the selection bounds and may lead to undesirable/unexpected behavior. If this is done, a
 *     {@link #selectRangeExpl(int, int)} call will reposition the caret, so that it is either the start or end
 *     bound of this selection.
 * </p>
 *
 * <p>
 *     For type safety, {@link #getSelectedDocument()} requires the same generic types from {@link StyledDocument}.
 *     This means that one must write a lot of boilerplate for the generics:
 *     {@code CaretSelectionBind<Collection<String>, StyledText<Collection<String>>, Collection<String>> selection}.
 *     However, this is only necessary if one is using {@link #getSelectedDocument()} or
 *     {@link #selectedDocumentProperty()}. <b>If you are not going to use the "selectedDocument" getter or property,
 *     then just write the much simpler {@code CaretSelectionBind<?, ?, ?> selection}</b>.
 * </p>
 *
 * @see Caret
 * @see Selection
 *
 * @param <PS> type for {@link StyledDocument}'s paragraph style; only necessary when using the "selectedDocument"
 *            getter or property
 * @param <SEG> type for {@link StyledDocument}'s segment type; only necessary when using the "selectedDocument"
 *            getter or property
 * @param <S> type for {@link StyledDocument}'s segment style; only necessary when using the "selectedDocument"
 *            getter or property
 */
public interface CaretSelectionBind<PS, SEG, S> extends Selection<PS, SEG, S> {

    /* ********************************************************************** *
     *                                                                        *
     * Observables                                                            *
     *                                                                        *
     * Observables are "dynamic" (i.e. changing) characteristics of this      *
     * control. They are not directly settable by the client code, but change *
     * in response to user input and/or API actions.                          *
     *                                                                        *
     * ********************************************************************** */

    int getAnchorPosition();
    ObservableValue<Integer> anchorPositionProperty();

    int getAnchorParIndex();
    ObservableValue<Integer> anchorParIndexProperty();

    int getAnchorColPosition();
    ObservableValue<Integer> anchorColPositionProperty();

    /* ********************************************************************** *
     *                                                                        *
     * Actions                                                                *
     *                                                                        *
     * Actions change the state of this control. They typically cause a       *
     * change of one or more observables and/or produce an event.             *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Positions the anchor and caretPosition explicitly,
     * effectively creating a selection.
     */
    void selectRangeExpl(int anchorParagraph, int anchorColumn, int caretParagraph, int caretColumn);

    /**
     * Positions the anchor and caretPosition explicitly,
     * effectively creating a selection.
     */
    void selectRangeExpl(int anchorPosition, int caretPosition);

}
