package org.fxmisc.richtext;

import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.Replacement;
import org.fxmisc.richtext.model.StyledDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Constructs a list of {@link Replacement}s that are used to update an
 * {@link org.fxmisc.richtext.model.EditableStyledDocument} in one call via {@link #commit()}. <strong>Note: this builder
 * cannot be reused and a new one must be created for each multi-change.</strong>
 *
 * <h3>Relative vs Absolute Changes</h3>
 * <p>
 *     Let's say that a document has the text {@code |(|t|e|x|t||)|} where each {@code |} represents a position
 *     in-between the characters of the text . If one wants to remove the opening
 *     and closing parenthesis in two update calls, they would write:
 * </p>
 * <pre><code>
 * // Text:     |(|t|e|x|t|)|
 * // Position: 0 1 2 3 4 5 6
 * area.deleteText(0, 1); // delete the first parenthesis
 *
 * // now the second parenthesis is positioned in a different spot than before
 * // Text:     |t|e|x|t|)|
 * // Position: 0 1 2 3 4 5
 * area.deleteText(4, 5); // delete the second parenthesis
 * </code></pre>
 * <p>
 *     {@code MultiChangeBuilder} can do the same thing in one call and works by applying earlier changes before
 *     later ones. However, this creates a problem. Later changes' start and end positions must be updated to still
 *     point to the correct spot in the text. Wouldn't it be easier to be able to define both changes in the original
 *     coordinate system of the document before any changes were applied? Returning to the previous example,
 *     wouldn't it be better if one could write:
 * </p>
 * <pre><code>
 * // Text:     |(|t|e|x|t|)|
 * // Position: 0 1 2 3 4 5 6
 *
 * // start multi change
 * area.deleteText(0, 1); // delete the 1st parenthesis
 * area.deleteText(5, 6); // delete the 2nd parenthesis
 * // end multi change
 * </code></pre>
 * <p>
 *     Fortunately, that is already handled for you. Such changes are known as "relative" changes: their
 *     start and end positions are updated to point to where they should point once all the earlier changes before
 *     them have been applied. To execute the same code as before, we would write:
 * </p>
 * <pre><code>
 * area.createMultiChange()
 *      // deletes the 1st parenthesis
 *      .deleteText(0, 1)
 *
 *      // internally updates this to delete(4, 5), so that it deletes the 2nd parenthesis
 *      .deleteText(5, 6)
 *
 *      // executes the call and updates the document
 *      .commit();
 * </code></pre>
 * <p>
 *     However, a developer may sometimes want an "absolute" change: their start and end positions
 *     are exactly what the developer specifies regardless of how changes before them modify the underlying document.
 *     For example, the following code would still delete both parenthesis:
 * </p>
 * <pre><code>
 * area.createMultiChange()
 *      .deleteText(0, 1) // deletes the 1st parenthesis
 *      .deleteTextAbsolutely(4, 5) // deletes the 2nd parenthesis
 *      .commit();
 * </code></pre>
 * <p>
 *     Thus, absolute changes (e.g. {@link #replaceAbsolutely(int, int, StyledDocument)}) are all the methods
 *     which have "absolute" as a suffix and relative changes (e.g. {@link #replace(int, int, StyledDocument)})
 *     do not. To make things easier for the developer, the methods declared here are similar to those declared
 *     in {@link EditActions}, minus a few (e.g. {@link EditActions#append(StyledDocument)}).
 * </p>
 * <h3>Other Considerations</h3>
 * <ul>
 *     <li>
 *         <strong>Warning: </strong> relative changes will not be updated if one starts a multi-change and fails
 *         to commit those changes before updating the area's underlying document in a
 *         {@link GenericStyledArea#replace(int, int, StyledDocument)} call.
 *     </li>
 *     <li>
 *         The builder does not optimize performance in cases where the developer adds a later change that makes
 *         an earlier change obsolete. For example,
 * <pre><code>
 * // This code...
 * area.createMultiChange(4)
 *     .replaceText(0, 1, "a")
 *     .replaceTextAbsolutely(0, 1, "b")
 *     .replaceTextAbsolutely(0, 1, "c")
 *     .replaceTextAbsolutely(0, 1, "d")
 *     .commit();
 *
 * // ...could be optimized to...
 * area.createMultiChange(2)
 *     .replaceText(0, 1, "a")
 *     // .replaceTextAbsolutely(0, 1, "b") // superseded by next change
 *     // .replaceTextAbsolutely(0, 1, "c") // superseded by next change
 *     .replaceTextAbsolutely(0, 1, "d")
 *     .commit();
 * // ...as it would reduce the number of updates by two and not create all the other objects
 * // associated with an update (e.g. RichTextChange, PlainTextChange, etc.).
 * </code></pre>
 *     </li>
 * </ul>
 */
public class MultiChangeBuilder<PS, SEG, S> {

    private final GenericStyledArea<PS, SEG, S> area;
    private final List<Replacement<PS, SEG, S>> list;

    private boolean alreadyCreated = false;

    MultiChangeBuilder(GenericStyledArea<PS, SEG, S> area) {
        this(area, new ArrayList<>());
    }

    MultiChangeBuilder(GenericStyledArea<PS, SEG, S> area, int initialListSize) {
        this(area, new ArrayList<>(initialListSize));
    }

    private MultiChangeBuilder(GenericStyledArea<PS, SEG, S> area, List<Replacement<PS, SEG, S>> list) {
        this.area = area;
        this.list = list;
    }

    /**
     * Inserts the given text at the given position.
     *
     * @param position The position to insert the text.
     * @param text The text to insert.
     */
    public MultiChangeBuilder<PS, SEG, S> insertText(int position, String text) {
        return replaceText(position, position, text);
    }

    /**
     * Inserts the given text at the given position.
     *
     * @param position The position to insert the text.
     * @param text The text to insert.
     */
    public MultiChangeBuilder<PS, SEG, S> insertTextAbsolutely(int position, String text) {
        return replaceTextAbsolutely(position, position, text);
    }

    /**
     * Inserts the given text at the position returned from
     * {@code getAbsolutePosition(paragraphIndex, columnPosition)}.
     *
     * <p><b>Caution:</b> see {@link StyledDocument#getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     *
     * @param text The text to insert
     */
    public MultiChangeBuilder<PS, SEG, S> insertText(int paragraphIndex, int columnPosition, String text) {
        int index = area.getAbsolutePosition(paragraphIndex, columnPosition);
        return replaceText(index, index, text);
    }

    /**
     * Inserts the given text at the position returned from
     * {@code getAbsolutePosition(paragraphIndex, columnPosition)}.
     *
     * <p><b>Caution:</b> see {@link StyledDocument#getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     *
     * @param text The text to insert
     */
    public MultiChangeBuilder<PS, SEG, S> insertTextAbsolutely(int paragraphIndex, int columnPosition, String text) {
        int index = area.getAbsolutePosition(paragraphIndex, columnPosition);
        return replaceTextAbsolutely(index, index, text);
    }

    /**
     * Inserts the given rich-text content at the given position.
     *
     * @param position The position to insert the text.
     * @param document The rich-text content to insert.
     */
    public MultiChangeBuilder<PS, SEG, S> insert(int position, StyledDocument<PS, SEG, S> document) {
        return replace(position, position, document);
    }

    /**
     * Inserts the given rich-text content at the given position.
     *
     * @param position The position to insert the text.
     * @param document The rich-text content to insert.
     */
    public MultiChangeBuilder<PS, SEG, S> insertAbsolutely(int position, StyledDocument<PS, SEG, S> document) {
        return replaceAbsolutely(position, position, document);
    }

    /**
     * Inserts the given rich-text content at the position returned from
     * {@code getAbsolutePosition(paragraphIndex, columnPosition)}.
     *
     * <p><b>Caution:</b> see {@link StyledDocument#getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     *
     * @param document The rich-text content to insert.
     */
    public MultiChangeBuilder<PS, SEG, S> insert(int paragraphIndex, int columnPosition, StyledDocument<PS, SEG, S> document) {
        int pos = area.getAbsolutePosition(paragraphIndex, columnPosition);
        return replace(pos, pos, document);
    }

    /**
     * Inserts the given rich-text content at the position returned from
     * {@code getAbsolutePosition(paragraphIndex, columnPosition)}.
     *
     * <p><b>Caution:</b> see {@link StyledDocument#getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     *
     * @param document The rich-text content to insert.
     */
    public MultiChangeBuilder<PS, SEG, S> insertAbsolutely(int paragraphIndex, int columnPosition, StyledDocument<PS, SEG, S> document) {
        int pos = area.getAbsolutePosition(paragraphIndex, columnPosition);
        return replaceAbsolutely(pos, pos, document);
    }

    /**
     * Removes a range of text.
     *
     * @param range The range of text to delete. It must not be null. Its start and end values specify the start
     *              and end positions within the area.
     *
     * @see #deleteText(int, int)
     */
    public MultiChangeBuilder<PS, SEG, S> deleteText(IndexRange range) {
        return deleteText(range.getStart(), range.getEnd());
    }

    /**
     * Removes a range of text.
     *
     * @param range The range of text to delete. It must not be null. Its start and end values specify the start
     *              and end positions within the area.
     *
     * @see #deleteText(int, int)
     */
    public MultiChangeBuilder<PS, SEG, S> deleteTextAbsolutely(IndexRange range) {
        return deleteTextAbsolutely(range.getStart(), range.getEnd());
    }

    /**
     * Removes a range of text.
     *
     * It must hold {@code 0 <= start <= end <= getLength()}.
     *
     * @param start Start position of the range to remove
     * @param end End position of the range to remove
     */
    public MultiChangeBuilder<PS, SEG, S> deleteText(int start, int end) {
        return replaceText(start, end, "");
    }

    /**
     * Removes a range of text.
     *
     * It must hold {@code 0 <= start <= end <= getLength()}.
     *
     * @param start Start position of the range to remove
     * @param end End position of the range to remove
     */
    public MultiChangeBuilder<PS, SEG, S> deleteTextAbsolutely(int start, int end) {
        return replaceTextAbsolutely(start, end, "");
    }

    /**
     * Removes a range of text.
     *
     * It must hold {@code 0 <= start <= end <= getLength()} where
     * {@code start = getAbsolutePosition(startParagraph, startColumn);} and is <b>inclusive</b>, and
     * {@code int end = getAbsolutePosition(endParagraph, endColumn);} and is <b>exclusive</b>.
     *
     * <p><b>Caution:</b> see {@link StyledDocument#getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     */
    public MultiChangeBuilder<PS, SEG, S> deleteText(int startParagraph, int startColumn, int endParagraph, int endColumn) {
        int start = area.getAbsolutePosition(startParagraph, startColumn);
        int end = area.getAbsolutePosition(endParagraph, endColumn);
        return replaceText(start, end, "");
    }

    /**
     * Removes a range of text.
     *
     * It must hold {@code 0 <= start <= end <= getLength()} where
     * {@code start = getAbsolutePosition(startParagraph, startColumn);} and is <b>inclusive</b>, and
     * {@code int end = getAbsolutePosition(endParagraph, endColumn);} and is <b>exclusive</b>.
     *
     * <p><b>Caution:</b> see {@link StyledDocument#getAbsolutePosition(int, int)} to know how the column index argument
     * can affect the returned position.</p>
     */
    public MultiChangeBuilder<PS, SEG, S> deleteTextAbsolutely(int startParagraph, int startColumn, int endParagraph, int endColumn) {
        int start = area.getAbsolutePosition(startParagraph, startColumn);
        int end = area.getAbsolutePosition(endParagraph, endColumn);
        return replaceTextAbsolutely(start, end, "");
    }

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
    public MultiChangeBuilder<PS, SEG, S> replaceText(int start, int end, String text) {
        return relativeReplace(start, end, ReadOnlyStyledDocument.fromString(
                text, area.getInitialParagraphStyle(), area.getInitialTextStyle(), area.getSegOps())
        );
    }

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
    public MultiChangeBuilder<PS, SEG, S> replaceTextAbsolutely(int start, int end, String text) {
        return absoluteReplace(start, end, ReadOnlyStyledDocument.fromString(
                text, area.getInitialParagraphStyle(), area.getInitialTextStyle(), area.getSegOps())
        );
    }

    /**
     * Replaces a range of characters with the given rich-text document.
     */
    public MultiChangeBuilder<PS, SEG, S> replace(int start, int end, StyledDocument<PS, SEG, S> replacement) {
        return relativeReplace(start, end, ReadOnlyStyledDocument.from(replacement));
    }

    /**
     * Replaces a range of characters with the given rich-text document.
     */
    public MultiChangeBuilder<PS, SEG, S> replaceAbsolutely(int start, int end, StyledDocument<PS, SEG, S> replacement) {
        return absoluteReplace(start, end, ReadOnlyStyledDocument.from(replacement));
    }

    /**
     * Applies all the changes stored in this object to the underlying document of the area. <strong>Note: this builder
     * cannot be reused and a new one must be created for each multi-change.</strong>
     */
    public final void commit() {
        ensureNotYetCreated();
        ensureHasChanges();
        alreadyCreated = true;
        area.replaceMulti(Collections.unmodifiableList(list));
    }

    private MultiChangeBuilder<PS, SEG, S> relativeReplace(int start, int end, ReadOnlyStyledDocument<PS, SEG, S> replacement) {
        if (list.isEmpty()) {
            return absoluteReplace(start, end, replacement);
        } else {

            int realStart = start;
            int realEnd = end;
            for (Replacement<PS, SEG, S> r : list) {
                if (r.getStart() <= realStart) {
                    realStart += r.getNetLength();
                    if (r.getEnd() <= realEnd) {
                        realEnd += r.getNetLength();
                    }
                } else if (r.getEnd() <= realEnd) {
                    realEnd += r.getNetLength();
                }
            }
            return absoluteReplace(realStart, realEnd, replacement);
        }
    }

    private MultiChangeBuilder<PS, SEG, S> absoluteReplace(int start, int end, ReadOnlyStyledDocument<PS, SEG, S> replacement) {
        // TODO: could be optimized to ignore earlier changes when later one makes them obsolete
        // for example:
        //  builder
        //  .replaceTextAbsolutely(0, 1, "a")
        //  .replaceTextAbsolutely(0, 1, "b")
        //  .replaceTextAbsolutely(0, 1, "c") // makes previous two commits obsolete
        //  .commit();
        list.add(new Replacement<>(start, end, replacement));

        return this;
    }

    private void ensureNotYetCreated() {
        if (alreadyCreated) {
            throw new IllegalStateException("Cannot reuse a builder multiple times");
        }
    }

    private void ensureHasChanges() {
        if (list.isEmpty()) {
            throw new IllegalStateException("Cannot commit multiple changes since none have been added");
        }
    }
}
