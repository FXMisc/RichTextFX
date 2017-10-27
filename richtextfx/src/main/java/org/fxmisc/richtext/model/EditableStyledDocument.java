package org.fxmisc.richtext.model;

import javafx.beans.value.ObservableValue;

import org.reactfx.EventStream;
import org.reactfx.SuspendableNo;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;

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
     * Returns an {@link EventStream} that emits a {@link PlainTextChange} every time a non-style change is made
     * to this document. A style change would include setting a segment's style, but not changing that segment
     * or setting a paragraph's style. A non-style change would include adding/removing/modifying a segment itself
     */
    default EventStream<PlainTextChange> plainChanges() {
        return richChanges()
                .map(RichTextChange::toPlainTextChange)
                // filter out rich changes where the style was changed but text wasn't added/removed
                .filter(pc -> !pc.isIdentity());
    }

    /**
     * Returns an {@link EventStream} that emits a {@link RichTextChange} every time a change is made
     * to this document, even when such a change does not modify the underlying document in any way.
     */
    EventStream<RichTextChange<PS, SEG, S>> richChanges();

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
