package org.fxmisc.richtext;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.model.NavigationActions;
import org.fxmisc.richtext.model.StyledDocument;

import java.text.BreakIterator;

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
public interface CaretSelectionBind<PS, SEG, S> extends Selection<PS, SEG, S>, Caret {

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

    // caret

    /**
     * Moves the caret to the given position in the text
     * and clears any selection.
     */
    @Override
    default void moveTo(int position) {
        moveTo(position, NavigationActions.SelectionPolicy.CLEAR);
    }

    /**
     * Moves the caret to the position returned from
     * {@link org.fxmisc.richtext.model.TextEditingArea#getAbsolutePosition(int, int)}
     * and clears any selection.
     */
    @Override
    default void moveTo(int paragraphIndex, int columnPosition) {
        moveTo(paragraphIndex, columnPosition, NavigationActions.SelectionPolicy.CLEAR);
    }

    /**
     * Moves the caret to the beginning of the current paragraph and clears any selection
     */
    @Override
    default void moveToParStart() {
        moveToParStart(NavigationActions.SelectionPolicy.CLEAR);
    }

    /**
     * Moves the caret to the end of the current paragraph and clears any selection
     */
    @Override
    default void moveToParEnd() {
        moveToParEnd(NavigationActions.SelectionPolicy.CLEAR);
    }

    /**
     * Moves the caret to the beginning of the area and clears any selection
     */
    @Override
    default void moveToAreaStart() {
        moveToAreaStart(NavigationActions.SelectionPolicy.CLEAR);
    }

    /**
     * Moves the caret to the end of the area and clears any selection
     */
    @Override
    default void moveToAreaEnd() {
        moveToAreaEnd(NavigationActions.SelectionPolicy.CLEAR);
    }

    /**
     * Moves the caret backward one char in the text and clears any selection
     */
    @Override
    default void moveToPrevChar() {
        moveToPrevChar(NavigationActions.SelectionPolicy.CLEAR);
    }

    /**
     * Moves the caret forward one char in the text and clears any selection
     */
    @Override
    default void moveToNextChar() {
        moveToNextChar(NavigationActions.SelectionPolicy.CLEAR);
    }

    /**
     * Moves the caret forwards by the number of breaks and clears any selection
     */
    void moveBreaksForwards(int numOfBreaks, BreakIterator breakIterator);

    /**
     * Moves the caret backwards by the number of breaks and clears any selection
     */
    void moveBreaksBackwards(int numOfBreaks, BreakIterator breakIterator);

    // selection

    // caret selection bind
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

    /**
     * Moves the caret to the position indicated by {@code pos}.
     * Based on the selection policy, the selection is either <em>cleared</em>
     * (i.e. anchor is set to the same position as caret), <em>adjusted</em>
     * (i.e. anchor is not moved at all), or <em>extended</em>
     * (i.e. {@code pos} becomes the new caret and, if {@code pos} points
     * outside the current selection, the far end of the current selection
     * becomes the anchor.
     */
    default void moveTo(int pos, NavigationActions.SelectionPolicy selectionPolicy) {
        switch(selectionPolicy) {
            case CLEAR:
                selectRange(pos, pos);
                break;
            case ADJUST:
                selectRange(getAnchorPosition(), pos);
                break;
            case EXTEND:
                IndexRange sel = getRange();
                int anchor;
                if(pos <= sel.getStart())
                    anchor = sel.getEnd();
                else if(pos >= sel.getEnd())
                    anchor = sel.getStart();
                else
                    anchor = getAnchorPosition();
                selectRangeExpl(anchor, pos);
                break;
        }
    }

    /**
     * Moves the caret to the position returned from
     * {@link org.fxmisc.richtext.model.TextEditingArea#getAbsolutePosition(int, int)}.
     */
    void moveTo(int paragraphIndex, int columnIndex, NavigationActions.SelectionPolicy selectionPolicy);

    /**
     * Moves the caret backward one char in the text.
     * Based on the given selection policy, anchor either moves with
     * the caret, stays put, or moves to the former caret position.
     */
    void moveToPrevChar(NavigationActions.SelectionPolicy selectionPolicy);

    /**
     * Moves the caret forward one char in the text.
     * Based on the given selection policy, anchor either moves with
     * the caret, stays put, or moves to the former caret position.
     */
    void moveToNextChar(NavigationActions.SelectionPolicy selectionPolicy);

    void moveToParStart(NavigationActions.SelectionPolicy selectionPolicy);

    void moveToParEnd(NavigationActions.SelectionPolicy selectionPolicy);

    void moveToAreaStart(NavigationActions.SelectionPolicy selectionPolicy);

    void moveToAreaEnd(NavigationActions.SelectionPolicy selectionPolicy);

    default void selectParagraph() {
        moveToParStart(NavigationActions.SelectionPolicy.CLEAR);
        moveToParEnd(NavigationActions.SelectionPolicy.ADJUST);
    }

    /**
     * Selects the word closest to the caret
     */
    default void selectWord() {
        selectWord(getPosition());
    };

    /**
     * Displaces the caret from the selection by positioning only the caret to the new location without
     * also affecting the selection's {@link #getAnchorPosition()} anchor} or the {@link #getRange()}  selection}.
     * Do not confuse this method with {@link #moveTo(int)}, which is the normal way of moving the caret.
     * This method can be used to achieve the special case of positioning the caret outside or inside the selection,
     * as opposed to always being at the boundary. Use with care.
     */
    void displaceCaret(int position);

    /**
     * Displaces the caret from the selection by positioning only the selection to the new location without
     * also affecting the {@link #getPosition() caret's position}.
     * This method can be used to achieve the special case of selecting some range in the area without affecting the
     * caret's position. Use with care.
     */
    void displaceSelection(int startPosition, int endPosition);

}
