package org.fxmisc.richtext.util;

import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.RichTextChange;
import org.fxmisc.richtext.model.TextChange;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.UndoManagerFactory;
import org.reactfx.EventStream;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * A class filled with factory methods to help easily construct an {@link UndoManager} for a {@link GenericStyledArea}.
 *
 * <p>
 *     To create an UndoManager that will prevent incoming changes from merging with the previous one after a period
 *     of user inactivity (via {@link UndoManager#preventMerge()}),
 *     use {@link #wrap(UndoManager, EventStream, Duration)}.
 * </p>
 */
public final class UndoUtils {

    private UndoUtils() {
        throw new IllegalStateException("UndoUtils cannot be instantiated");
    }

    public static final Duration DEFAULT_PREVENT_MERGE_DELAY = Duration.ofMillis(500);

    /**
     * Constructs an UndoManager with an unlimited history:
     * if {@link GenericStyledArea#isPreserveStyle() the area's preserveStyle flag is true}, the returned UndoManager
     * can undo/redo {@link RichTextChange}s; otherwise, it can undo/redo {@link PlainTextChange}s.
     */
    public static <PS, SEG, S> UndoManager defaultUndoManager(GenericStyledArea<PS, SEG, S> area) {
        return area.isPreserveStyle()
                ? richTextUndoManager(area)
                : plainTextUndoManager(area);
    }

    /* ********************************************************************** *
     *                                                                        *
     * UndoManager Factory Methods                                            *
     *                                                                        *
     * Code that constructs different kinds of UndoManagers for an area       *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Returns an UndoManager with an unlimited history that can undo/redo {@link RichTextChange}s. New changes
     * emitted from the stream will not be merged with the previous change
     * after {@link #DEFAULT_PREVENT_MERGE_DELAY}
     */
    public static <PS, SEG, S> UndoManager<RichTextChange<PS, SEG, S>> richTextUndoManager(GenericStyledArea<PS, SEG, S> area) {
        return richTextUndoManager(area, UndoManagerFactory.unlimitedHistoryFactory());
    }

    /**
     * Returns an UndoManager that can undo/redo {@link RichTextChange}s. New changes
     * emitted from the stream will not be merged with the previous change
     * after {@code preventMergeDelay}
     */
    public static <PS, SEG, S> UndoManager<RichTextChange<PS, SEG, S>> richTextUndoManager(GenericStyledArea<PS, SEG, S> area,
                                                                                           Duration preventMergeDelay) {
        return richTextUndoManager(area, UndoManagerFactory.unlimitedHistoryFactory(), preventMergeDelay);
    };

    /**
     * Returns an UndoManager that can undo/redo {@link RichTextChange}s. New changes
     * emitted from the stream will not be merged with the previous change
     * after {@link #DEFAULT_PREVENT_MERGE_DELAY}
     */
    public static <PS, SEG, S> UndoManager<RichTextChange<PS, SEG, S>> richTextUndoManager(GenericStyledArea<PS, SEG, S> area,
                                                                                           UndoManagerFactory factory) {
        return richTextUndoManager(area, factory, DEFAULT_PREVENT_MERGE_DELAY);
    };

    /**
     * Returns an UndoManager that can undo/redo {@link RichTextChange}s. New changes
     * emitted from the stream will not be merged with the previous change
     * after {@code preventMergeDelay}
     */
    public static <PS, SEG, S> UndoManager<RichTextChange<PS, SEG, S>> richTextUndoManager(GenericStyledArea<PS, SEG, S> area,
                                                                                           UndoManagerFactory factory,
                                                                                           Duration preventMergeDelay) {
        return wrap(
                factory.create(area.richChanges(), TextChange::invert, applyRichTextChange(area), TextChange::mergeWith, TextChange::isIdentity),
                area.richChanges(),
                preventMergeDelay
        );
    };

    /**
     * Returns an UndoManager with an unlimited history that can undo/redo {@link PlainTextChange}s. New changes
     * emitted from the stream will not be merged with the previous change
     * after {@link #DEFAULT_PREVENT_MERGE_DELAY}
     */
    public static <PS, SEG, S> UndoManager<PlainTextChange> plainTextUndoManager(GenericStyledArea<PS, SEG, S> area) {
        return plainTextUndoManager(area, DEFAULT_PREVENT_MERGE_DELAY);
    }

    /**
     * Returns an UndoManager that can undo/redo {@link PlainTextChange}s. New changes
     * emitted from the stream will not be merged with the previous change
     * after {@code preventMergeDelay}
     */
    public static <PS, SEG, S> UndoManager<PlainTextChange> plainTextUndoManager(GenericStyledArea<PS, SEG, S> area,
                                                                                 Duration preventMergeDelay) {
        return plainTextUndoManager(area, UndoManagerFactory.unlimitedHistoryFactory(), preventMergeDelay);
    }

    /**
     * Returns an UndoManager that can undo/redo {@link PlainTextChange}s. New changes
     * emitted from the stream will not be merged with the previous change
     * after {@link #DEFAULT_PREVENT_MERGE_DELAY}
     */
    public static <PS, SEG, S> UndoManager<PlainTextChange> plainTextUndoManager(GenericStyledArea<PS, SEG, S> area,
                                                                UndoManagerFactory factory) {
        return plainTextUndoManager(area, factory, DEFAULT_PREVENT_MERGE_DELAY);
    }

    /**
     * Returns an UndoManager that can undo/redo {@link PlainTextChange}s. New changes
     * emitted from the stream will not be merged with the previous change
     * after {@code preventMergeDelay}
     */
    public static <PS, SEG, S> UndoManager<PlainTextChange> plainTextUndoManager(GenericStyledArea<PS, SEG, S> area,
                                                                                 UndoManagerFactory factory,
                                                                                 Duration preventMergeDelay) {
        return wrap(
                factory.create(area.plainTextChanges(), TextChange::invert, applyPlainTextChange(area), TextChange::mergeWith, TextChange::isIdentity),
                area.plainTextChanges(),
                preventMergeDelay
        );
    }

    /* ********************************************************************** *
     *                                                                        *
     * Change Appliers                                                        *
     *                                                                        *
     * Code that handles how a change should be applied to the area           *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Applies a {@link PlainTextChange} to the given area when the {@link UndoManager}'s change stream emits an event
     * by {@code area.replaceText(change.getPosition(), change.getRemovalEnd(), change.getInserted()}.
     */
    public static <PS, SEG, S> Consumer<PlainTextChange> applyPlainTextChange(GenericStyledArea<PS, SEG, S> area) {
        return change -> area.replaceText(change.getPosition(), change.getRemovalEnd(), change.getInserted());
    }

    /**
     * Applies a {@link PlainTextChange} to the given area when the {@link UndoManager}'s change stream emits an event
     * by {@code area.replace(change.getPosition(), change.getRemovalEnd(), change.getInserted()}.
     */
    public static <PS, SEG, S> Consumer<RichTextChange<PS, SEG, S>> applyRichTextChange(GenericStyledArea<PS, SEG, S> area) {
        return change -> area.replace(change.getPosition(), change.getRemovalEnd(), change.getInserted());
    }

    /**
     * Wraps an {@link UndoManager} and prevents the next emitted change from merging with the previous one are a
     * period of inactivity (i.e., the {@code changeStream} has not emitted an event in {@code preventMergeDelay}
     */
    public static <T> UndoManager<T> wrap(UndoManager<T> undoManager, EventStream<T> changeStream, Duration preventMergeDelay) {
        return new UndoManagerInactivityWrapper<>(undoManager, changeStream, preventMergeDelay);
    }
}
