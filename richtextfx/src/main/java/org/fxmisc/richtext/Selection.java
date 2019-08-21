package org.fxmisc.richtext;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.model.StyledDocument;

import java.text.BreakIterator;
import java.util.Optional;

/**
 * An object for encapsulating a selection in a given area.
 *
 * <p>
 *     <b>"Position"</b> refers to the place in-between characters. In other words, every {@code "|"} in
 *     {@code "|t|e|x|t|"} is a valid position. There are two kinds of positions used here:</p>
 *     <ol>
 *         <li>
 *             {@link #getStartPosition()}/{@link #getEndPosition()}, which refers to a position somewhere in the
 *             entire area's content. It's bounds are {@code 0 <= x <= area.getLength()}.
 *         </li>
 *         <li>
 *             {@link #getStartColumnPosition()}/{@link #getEndColumnPosition()}, which refers to a position
 *             somewhere in the current paragraph. It's bounds are {@code 0 <= x <= area.getParagraphLength(index)}.
 *         </li>
 *     </ol>
 *
 * Note: when parameter names are "position" without the "column" prefix, they refer to the position in the entire area.
 *
 * <p>
 *     For type safety, {@link #getSelectedDocument()} requires the same generic types from {@link StyledDocument}.
 *     This means that one must write a lot of boilerplate for the generics:
 *     {@code Selection<Collection<String>, StyledText<Collection<String>>, Collection<String>> selection}.
 *     However, this is only necessary if one is using {@link #getSelectedDocument()} or
 *     {@link #selectedDocumentProperty()}. <b>If you are not going to use the "selectedDocument" getter or property,
 *     then just write the much simpler {@code Selection<?, ?, ?> selection}</b>.
 * </p>
 *
 * @see CaretSelectionBind
 * @see Caret
 *
 * @param <PS> type for {@link StyledDocument}'s paragraph style; only necessary when using the "selectedDocument"
 *            getter or property
 * @param <SEG> type for {@link StyledDocument}'s segment type; only necessary when using the "selectedDocument"
 *            getter or property
 * @param <S> type for {@link StyledDocument}'s segment style; only necessary when using the "selectedDocument"
 *            getter or property
 */
public interface Selection<PS, SEG, S> {

    /**
     * Specifies whether to update the start/end value of a selection to the left (towards 0) or right (away from 0)
     */
    public static enum Direction {
        LEFT,
        RIGHT
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

    ObservableValue<StyledDocument<PS, SEG, S>> selectedDocumentProperty();
    StyledDocument<PS, SEG, S> getSelectedDocument();

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

    ObservableValue<Integer> endParagraphIndexProperty();
    int getEndParagraphIndex();

    ObservableValue<Integer> endColumnPositionProperty();
    int getEndColumnPosition();


    /**
     * The selectionBoundsProperty of the selection in the Screen's coordinate system if something is selected and visible in the
     * viewport, or {@link Optional#empty()} if selection is not visible in the viewport.
     */
    ObservableValue<Optional<Bounds>> selectionBoundsProperty();
    Optional<Bounds> getSelectionBounds();

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
     * Selects the given range.
     *
     * <p><b>Caution:</b> see {@link TextEditingArea#getAbsolutePosition(int, int)} to
     * know how the column index argument can affect the returned position.</p>
     */
    void selectRange(int startParagraphIndex, int startColPosition, int endParagraphIndex, int endColPosition);

    /**
     * Selects the given range.
     */
    void selectRange(int startPosition, int endPosition);

    void updateStartBy(int amount, Direction direction);

    void updateEndBy(int amount, Direction direction);

    void updateStartTo(int position);

    void updateStartTo(int paragraphIndex, int columnPosition);

    void updateStartByBreaksForward(int numOfBreaks, BreakIterator breakIterator);

    void updateStartByBreaksBackward(int numOfBreaks, BreakIterator breakIterator);

    void updateEndTo(int position);

    void updateEndTo(int paragraphIndex, int columnPosition);

    void updateEndByBreaksForward(int numOfBreaks, BreakIterator breakIterator);

    void updateEndByBreaksBackward(int numOfBreaks, BreakIterator breakIterator);

    void selectAll();

    void selectParagraph(int paragraphIndex);

    void selectWord(int wordPositionInArea);

    /**
     * Clears the selection via {@code selectRange(getStartPosition(), getStartPosition())}.
     */
    default void deselect() {
        selectRange(getStartPosition(), getStartPosition());
    }

    /**
     * Configures a {@link SelectionPath} that will be used to render a portion or all of this selection
     * on a single paragraph. When the selection is a multi-paragraph selection, one path will be used
     * to render that portion of the selection on a paragraph.
     */
    void configureSelectionPath(SelectionPath path);

    /**
     * Disposes the selection and prevents memory leaks
     */
    void dispose();

    /**
     * Gets the name of this selection. Each selection in an area must have a unique name.<br>
     * The name is also used as a StyleClass, so the Selection can be styled using CSS selectors
     * from Path, Shape, and Node eg:<br>.styled-text-area .my-selection { -fx-fill: lime; }
     */
    String getSelectionName();

    /**
     * Gets the area with which this selection is associated
     */
    GenericStyledArea<PS, SEG, S> getArea();

}
