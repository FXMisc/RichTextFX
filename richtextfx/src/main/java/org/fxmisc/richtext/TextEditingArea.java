package org.fxmisc.richtext;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.control.IndexRange;

import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.RichTextChange;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.StyledDocument;
import org.reactfx.EventStream;
import org.reactfx.SuspendableNo;
import org.reactfx.value.Var;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Interface for a text editing control.
 *
 * Defines the core methods. Other interfaces define default
 * higher-level methods implemented on top of the core methods.
 *
 * @param <S> type of style that can be applied to text.
 */
public interface TextEditingArea<PS, SEG, S> {

    /*******************
     *                 *
     *   Observables   *
     *                 *
     *******************/

    /**
     * The number of characters in this text-editing area.
     */
    default int getLength() { return lengthProperty().getValue(); }
    ObservableValue<Integer> lengthProperty();

    /**
     * This is used to determine word and sentence breaks while navigating or selecting.
     * Override this method if your paragraph or text style accommodates Locales as well.
     */
    default Locale getLocale() { return Locale.getDefault(); }

    /**
     * Text content of this text-editing area.
     */
    default String getText() { return textProperty().getValue(); }
    ObservableValue<String> textProperty();

    /**
     * Rich-text content of this text-editing area.
     * The returned document is immutable, it does not reflect
     * subsequent edits of this text-editing area.
     */
    StyledDocument<PS, SEG, S> getDocument();

    /**
     * The underlying document of this area that can be displayed by multiple {@code StyledTextArea}s.
     */
    EditableStyledDocument<PS, SEG, S> getContent();

    /**
     * Returns the object used for operating over {@link SEG segments} and their styles
     */
    SegmentOps<SEG, S> getSegOps();

    /**
     * The current position of the caret, as a character offset in the text.
     *
     * Most of the time, caret is at the boundary of the selection (if there
     * is any selection). However, there are circumstances when the caret is
     * positioned inside or outside the selected text. For example, when the
     * user is dragging the selected text, the caret moves with the cursor
     * to point at the position where the selected text moves upon release.
     */
    default int getCaretPosition() { return getCaretSelectionBind().getPosition(); }
    default ObservableValue<Integer> caretPositionProperty() { return getCaretSelectionBind().positionProperty(); }

    /**
     * Index of the current paragraph, i.e. the paragraph with the caret.
     */
    default int getCurrentParagraph() { return getCaretSelectionBind().getParagraphIndex(); }
    default ObservableValue<Integer> currentParagraphProperty() { return getCaretSelectionBind().paragraphIndexProperty(); }

    /**
     * The caret position within the current paragraph.
     */
    default int getCaretColumn() { return getCaretSelectionBind().getColumnPosition(); }
    default ObservableValue<Integer> caretColumnProperty() { return getCaretSelectionBind().columnPositionProperty(); }

    /**
     * Gets the bounds of the caret in the Screen's coordinate system or {@link Optional#empty()}
     * if caret is not visible in the viewport.
     */
    default Optional<Bounds> getCaretBounds() { return getCaretSelectionBind().getCaretBounds(); }
    default ObservableValue<Optional<Bounds>> caretBoundsProperty() { return getCaretSelectionBind().caretBoundsProperty(); }

    /**
     * Indicates when this text area should display a caret.
     */
    default Caret.CaretVisibility getShowCaret() { return getCaretSelectionBind().getShowCaret(); }
    default void setShowCaret(Caret.CaretVisibility value) { getCaretSelectionBind().setShowCaret(value); }
    default Var<Caret.CaretVisibility> showCaretProperty() { return getCaretSelectionBind().showCaretProperty(); }

    /**
     * Gets the area's main {@link CaretSelectionBind}.
     */
    CaretSelectionBind<PS, SEG, S> getCaretSelectionBind();

    /**
     * The anchor of the selection.
     * If there is no selection, this is the same as caret position.
     */
    default int getAnchor() { return getCaretSelectionBind().getAnchorPosition(); }
    default ObservableValue<Integer> anchorProperty() { return getCaretSelectionBind().anchorPositionProperty(); }

    /**
     * The selection range.
     *
     * One boundary is always equal to anchor, and the other one is most
     * of the time equal to caret position.
     */
    default IndexRange getSelection() { return getCaretSelectionBind().getRange(); }
    default ObservableValue<IndexRange> selectionProperty() { return getCaretSelectionBind().rangeProperty(); }

    /**
     * The selected text.
     */
    default String getSelectedText() { return getCaretSelectionBind().getSelectedText(); }
    default ObservableValue<String> selectedTextProperty() { return getCaretSelectionBind().selectedTextProperty(); }

    /**
     * Gets the bounds of the selection in the Screen's coordinate system if something is selected and visible in the
     * viewport or {@link Optional#empty()} if selection is not visible in the viewport.
     */
    default Optional<Bounds> getSelectionBounds() { return getCaretSelectionBind().getSelectionBounds(); }
    default ObservableValue<Optional<Bounds>> selectionBoundsProperty() { return getCaretSelectionBind().selectionBoundsProperty(); }

    /**
     * Unmodifiable observable list of paragraphs in this text area.
     */
    ObservableList<Paragraph<PS, SEG, S>> getParagraphs();
    default Paragraph<PS, SEG, S> getParagraph(int index) { return getParagraphs().get(index); }
    default int getParagraphLength(int index) { return getParagraph(index).length(); }

    /**
     * True when an update to the area's {@link #getContent() underling editable document} is still occurring
     * or the viewport is being updated.
     */
    SuspendableNo beingUpdatedProperty();
    default boolean isBeingUpdated() { return beingUpdatedProperty().get(); }

    /*********************
     *                   *
     *   Event streams   *
     *                   *
     *********************/

    /**
     * See {@link org.fxmisc.richtext.model.EditableStyledDocument#multiPlainChanges()}
     */
    EventStream<List<PlainTextChange>> multiPlainChanges();

    /**
     * See {@link org.fxmisc.richtext.model.EditableStyledDocument#plainChanges()}
     */
    EventStream<PlainTextChange> plainTextChanges();

    /**
     * See {@link org.fxmisc.richtext.model.EditableStyledDocument#multiRichChanges()}
     */
    EventStream<List<RichTextChange<PS, SEG, S>>> multiRichChanges();

    /**
     * See {@link org.fxmisc.richtext.model.EditableStyledDocument#richChanges()}
     */
    EventStream<RichTextChange<PS, SEG, S>> richChanges();


    /***************
     *             *
     *   Queries   *
     *             *
     ***************/

    /**
     * Returns text content of the given paragraph.
     */
    String getText(int paragraphIndex);

    /**
     * Returns text content of the given character range.
     */
    String getText(int start, int end);

    /**
     * Returns text content of the given character range.
     */
    String getText(IndexRange range);

    /**
     * Returns text content of the given character range.
     *
     * <p><b>Caution:</b> see {@link #getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     */
    default String getText(int startParagraph, int startColumn, int endParagraph, int endColumn) {
        int start = getAbsolutePosition(startParagraph, startColumn);
        int end = getAbsolutePosition(endParagraph, endColumn);
        return getText(start, end);
    }

    /**
     * Returns rich-text content of the given paragraph.
     */
    StyledDocument<PS, SEG, S> subDocument(int paragraphIndex);

    /**
     * Returns rich-text content of the given character range.
     */
    default StyledDocument<PS, SEG, S> subDocument(IndexRange range) {
        return subDocument(range.getStart(), range.getEnd());
    };

    /**
     * Returns rich-text content of the given character range.
     */
    StyledDocument<PS, SEG, S> subDocument(int start, int end);

    /**
     * Returns rich-text content of the given character range.
     *
     * <p><b>Caution:</b> see {@link #getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     */
    default StyledDocument<PS, SEG, S> subDocument(int startParagraph, int startColumn, int endParagraph, int endColumn) {
        int start = getAbsolutePosition(startParagraph, startColumn);
        int end = getAbsolutePosition(endParagraph, endColumn);
        return subDocument(start, end);
    }

    /**
     * Returns the selection range in the given paragraph. Note: this method will return
     * {@code IndexRange(start, paragraph.length() + 1)} when the selection includes a newline character.
     */
    default IndexRange getParagraphSelection(int paragraph) {
        return getParagraphSelection(getCaretSelectionBind(), paragraph);
    }

    public IndexRange getParagraphSelection(Selection selection, int paragraph);


    /***************
     *             *
     *   Actions   *
     *             *
     ***************/

    /**
     * Positions the anchor and caretPosition explicitly,
     * effectively creating a selection.
     */
    default void selectRange(int anchor, int caretPosition) {
        getCaretSelectionBind().selectRangeExpl(anchor, caretPosition);
    }

    /**
     * Positions the anchor and caretPosition explicitly,
     * effectively creating a selection.
     *
     * <p><b>Caution:</b> see {@link #getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     */
    default void selectRange(int anchorParagraph, int anchorColumn, int caretPositionParagraph, int caretPositionColumn) {
        int anchor = getAbsolutePosition(anchorParagraph, anchorColumn);
        int caretPosition = getAbsolutePosition(caretPositionParagraph, caretPositionColumn);
        selectRange(anchor, caretPosition);
    }

    /**
     * Displaces the caret from the selection by positioning only the caret to the new location without
     * also affecting the selection's {@link #getAnchor() anchor} or the {@link #getSelection() selection}.
     * Do not confuse this method with {@link NavigationActions#moveTo(int)}, which is the normal way of moving the
     * caret. This method can be used to achieve the special case of positioning the caret outside or inside the
     * selection, as opposed to always being at the boundary. Use with care.
     */
    public void displaceCaret(int pos);

    /**
     * Replaces a range of characters with the given text.
     *
     * It must hold {@code 0 <= start <= end <= getLength()}.
     *
     * @param start Start index of the range to replace, inclusive.
     * @param end End index of the range to replace, exclusive.
     * @param text The text to put in place of the deleted range.
     * It must not be null.
     */
    void replaceText(int start, int end, String text);

    /**
     * Replaces a range of characters with the given text.
     *
     * It must hold {@code 0 <= start <= end <= getLength()} where
     * {@code start = getAbsolutePosition(startParagraph, startColumn);} and is <b>inclusive</b>, and
     * {@code int end = getAbsolutePosition(endParagraph, endColumn);} and is <b>exclusive</b>.
     *
     * <p><b>Caution:</b> see {@link #getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     *
     * @param text The text to put in place of the deleted range.
     * It must not be null.
     */
    default void replaceText(int startParagraph, int startColumn, int endParagraph, int endColumn, String text) {
        int start = getAbsolutePosition(startParagraph, startColumn);
        int end = getAbsolutePosition(endParagraph, endColumn);
        replaceText(start, end, text);
    }

    /**
     * Replaces a range of characters with the given rich-text document.
     */
    void replace(int start, int end, StyledDocument<PS, SEG, S> replacement);

    /**
     * Starts building a list of changes to be used to update multiple portions of the underlying document
     * in one call. To execute the changes, call {@link MultiChangeBuilder#commit()}. If the number of
     * changes are known at compile time, use {@link #createMultiChange(int)} for better memory efficiency.
     */
    MultiChangeBuilder<PS, SEG, S> createMultiChange();

    /**
     * Same as {@link #createMultiChange()} but the number of changes are specified to be more memory efficient.
     */
    MultiChangeBuilder<PS, SEG, S> createMultiChange(int numOfChanges);

    /**
     * Replaces a range of characters with the given segment.
     *
     * It must hold {@code 0 <= start <= end <= getLength()}.
     *
     * @param start Start index of the range to replace, inclusive.
     * @param end End index of the range to replace, exclusive.
     * @param seg The seg to put in place of the deleted range.
     * It must not be null.
     */
    void replace(int start, int end, SEG seg, S style);

    /**
     * Replaces a range of characters with the given segment.
     *
     * It must hold {@code 0 <= start <= end <= getLength()} where
     * {@code start = getAbsolutePosition(startParagraph, startColumn);} and is <b>inclusive</b>, and
     * {@code int end = getAbsolutePosition(endParagraph, endColumn);} and is <b>exclusive</b>.
     *
     * <p><b>Caution:</b> see {@link #getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     *
     * @param seg The segment to put in place of the deleted range.
     * It must not be null.
     */
    default void replace(int startParagraph, int startColumn, int endParagraph, int endColumn, SEG seg, S style) {
        int start = getAbsolutePosition(startParagraph, startColumn);
        int end = getAbsolutePosition(endParagraph, endColumn);
        replace(start, end, seg, style);
    }

    /**
     * Replaces a range of characters with the given rich-text document.
     *
     * <p><b>Caution:</b> see {@link #getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     */
    default void replace(int startParagraph, int startColumn, int endParagraph, int endColumn, StyledDocument<PS, SEG, S> replacement) {
        int start = getAbsolutePosition(startParagraph, startColumn);
        int end = getAbsolutePosition(endParagraph, endColumn);
        replace(start, end, replacement);
    }

    /**
     * Replaces a range of characters with the given text.
     *
     * @param range The range to replace. It must not be null.
     * @param text The text to put in place of the deleted range.
     * It must not be null.
     *
     * @see #replaceText(int, int, String)
     */
    default void replaceText(IndexRange range, String text) {
        replaceText(range.getStart(), range.getEnd(), text);
    }

    /**
     * Replaces a range of characters with the given seg.
     *
     * @param range The range to replace. It must not be null.
     * @param seg The segment to put in place of the deleted range.
     * It must not be null.
     *
     * @see #replace(int, int, Object, Object)
     */
    default void replace(IndexRange range, SEG seg, S style) {
        replace(range.getStart(), range.getEnd(), seg, style);
    }

    /**
     * Equivalent to
     * {@code replace(range.getStart(), range.getEnd(), replacement)}.
     */
    default void replace(IndexRange range, StyledDocument<PS, SEG, S> replacement) {
        replace(range.getStart(), range.getEnd(), replacement);
    }

    /**
     * Returns the absolute position (i.e. the spot in-between characters) to the left of the given column in the given paragraph.
     *
     * <p>For example, given a text with only one line {@code "text"} and the columnIndex value of {@code 1}, "position 1" would be returned:</p>
     * <pre>
     *  ┌ character index 0
     *  | ┌ character index 1
     *  | |   ┌ character index 3
     *  | |   |
     *  v v   v
     *
     * |t|e|x|t|
     *
     * ^ ^     ^
     * | |     |
     * | |     └ position 4
     * | └ position 1
     * └ position 0
     * </pre>
     *
     * <h3>Warning: Off-By-One errors can easily occur</h3>
     * <p>If the column index spans outside of the given paragraph's length, the returned value will
     * pass on to the previous/next paragraph. In other words, given a document with two paragraphs
     * (where the first paragraph's text is "some" and the second "thing"), then the following statements are true:</p>
     * <ul>
     *     <li><code>getAbsolutePosition(0, "some".length()) == 4 == getAbsolutePosition(1, -1)</code></li>
     *     <li><code>getAbsolutePosition(0, "some".length() + 1) == 5 == getAbsolutePosition(1, 0)</code></li>
     * </ul>
     *
     * @param paragraphIndex The index of the paragraph from which to start.
     * @param columnIndex If positive, the index going forward (the given paragraph's line or the next one(s)).
     *                    If negative, the index going backward (the previous paragraph's line(s)
     */
    int getAbsolutePosition(int paragraphIndex, int columnIndex);

    /**
     * Disposes this area, preventing memory leaks.
     */
    public void dispose();
}
