package org.fxmisc.richtext.model;

import javafx.beans.value.ObservableValue;

import org.reactfx.EventStream;
import org.reactfx.EventStreamBase;
import org.reactfx.Subscription;
import org.reactfx.SuspendableNo;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;

import java.util.Arrays;
import java.util.List;

/**
 * Content model for {@link org.fxmisc.richtext.GenericStyledArea}. Specifies edit operations
 * on paragraph's styles, segments (like text), and segments' style, but does not worry about view-related aspects
 * (e.g. scrolling).
 *
 * @param <PS> the paragraph style type
 * @param <SEG> the segment type
 * @param <S> the segment's style
 */
public interface EditableStyledDocument<PS, SEG, S> extends StyledDocument<PS, SEG, S> {

    /* ********************************************************************** *
     *                                                                        *
     * Observables                                                            *
     *                                                                        *
     * Observables are "dynamic" (i.e. changing) characteristics of an object.*
     * They are not directly settable by the client code, but change in       *
     * response to user input and/or API actions.                             *
     *                                                                        *
     * ********************************************************************** */

    ObservableValue<String> textProperty();

    int getLength();
    Val<Integer> lengthProperty();

    @Override
    LiveList<Paragraph<PS, SEG, S>> getParagraphs();

    /**
     * Read-only snapshot of the current state of this document.
     */
    ReadOnlyStyledDocument<PS, SEG, S> snapshot();

    /* ********************************************************************** *
     *                                                                        *
     * Event streams                                                          *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Returns an {@link EventStream} that emits a {@link List} of {@link RichTextChange}s every time a change is made
     * to this document, even when such a change does not modify the underlying document in any way. The emitted
     * list will only have one item in it unless one used {@link #replaceMulti(List)}.
     */
    EventStream<List<RichTextChange<PS, SEG, S>>> multiRichChanges();

    /**
     * Returns an {@link EventStream} that emits a {@link List} of {@link PlainTextChange}s every time a non-style
     * change is made to this document. A style change would include setting a segment's style, but not changing
     * that segment or setting a paragraph's style. A non-style change would include adding/removing/modifying a
     * segment itself. The emitted list will only have one item in it unless one used {@link #replaceMulti(List)}.
     */
    default EventStream<List<PlainTextChange>> multiPlainChanges() {
        return multiRichChanges()
                // map to a List<PlainTextChange>
                .map(list -> Arrays.asList(list.stream()
                        // filter out rich changes where the style was changed but text wasn't added/removed
                        .filter(rtc -> !rtc.isPlainTextIdentity())
                        .map(RichTextChange::toPlainTextChange)
                        .toArray(PlainTextChange[]::new)))
                // only emit non-empty lists
                .filter(list -> !list.isEmpty());
    }

    /**
     * Returns an {@link EventStream} that emits each {@link PlainTextChange} in {@link #multiPlainChanges()}'s
     * emitted list.
     */
    default EventStream<PlainTextChange> plainChanges() {
        return new EventStreamBase<PlainTextChange>() {
            @Override
            protected Subscription observeInputs() {
                return multiPlainChanges().subscribe(l -> l.forEach(this::emit));
            }
        };
    }

    /**
     * Returns an {@link EventStream} that emits each {@link RichTextChange} in {@link #multiRichChanges()}'s
     * emitted list.
     */
    default EventStream<RichTextChange<PS, SEG, S>> richChanges() {
        return new EventStreamBase<RichTextChange<PS, SEG, S>>() {
            @Override
            protected Subscription observeInputs() {
                return multiRichChanges().subscribe(list -> list.forEach(this::emit));
            }
        };
    }

    SuspendableNo beingUpdatedProperty();
    boolean isBeingUpdated();

    /* ********************************************************************** *
     *                                                                        *
     * Actions                                                                *
     *                                                                        *
     * Actions change the state of the object. They typically cause a change  *
     * of one or more observables and/or produce an event.                    *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Replaces multiple portions of this document in one update.
     */
    void replaceMulti(List<Replacement<PS, SEG, S>> replacements);

    /**
     * Convenience method for {@link #replace(int, int, StyledDocument)} using a {@link Replacement} argument.
     */
    default void replace(Replacement<PS, SEG, S> replacement) {
        replace(replacement.getStart(), replacement.getEnd(), replacement.getDocument());
    }

    /**
     * Replaces the portion of this document {@code "from..to"} with the given {@code replacement}.
     *
     * @param start the absolute position in the document that starts the portion to replace
     * @param end the absolute position in the document that ends the portion to replace
     * @param replacement the document that replaces the removed portion of this document
     */
    void replace(int start, int end, StyledDocument<PS, SEG, S> replacement);

    /**
     * Sets the style of all segments in the given "from..to" range to the given style.
     *
     * @param from the absolute position in the document that starts the range to re-style
     * @param to the absolute position in the document that ends the range to re-style
     */
    void setStyle(int from, int to, S style);

    /**
     * Sets all segments in the given paragraph to the given style
     */
    void setStyle(int paragraphIndex, S style);

    /**
     * Sets the given range "fromCol..toCol" in the given paragraph to the given style
     */
    void setStyle(int paragraphIndex, int fromCol, int toCol, S style);

    /**
     * Replaces the style spans for the given range {@code "from..(from + styleSpans.length())"} with the given
     * style spans
     */
    void setStyleSpans(int from, StyleSpans<? extends S> styleSpens);

    /**
     * Replaces the style spans for the given range {@code "from..(from + styleSpans.length())"} in the given
     * paragraph with the given style spans
     */
    void setStyleSpans(int paragraphIndex, int from, StyleSpans<? extends S> styleSpens);

    /**
     * Sets the given paragraph to the given paragraph style
     */
    void setParagraphStyle(int paragraphIndex, PS style);

}
