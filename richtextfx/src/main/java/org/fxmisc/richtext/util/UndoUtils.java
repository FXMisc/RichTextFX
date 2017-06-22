package org.fxmisc.richtext.util;

import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.RichTextChange;
import org.fxmisc.richtext.model.TextChange;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.UndoManagerFactory;

import java.util.function.Consumer;

/**
 * A class filled with factory methods to help easily construct an {@link UndoManager} for a {@link GenericStyledArea}.
 */
public final class UndoUtils {

    private UndoUtils() {
        throw new IllegalStateException("UndoUtils cannot be instantiated");
    }

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
     * Returns an UndoManager with an unlimited history that can undo/redo {@link RichTextChange}s.
     */
    public static <PS, SEG, S> UndoManager<RichTextChange<PS, SEG, S>> richTextUndoManager(GenericStyledArea<PS, SEG, S> area) {
        return richTextUndoManager(area, UndoManagerFactory.unlimitedHistoryFactory());
    }

    /**
     * Returns an UndoManager that can undo/redo {@link RichTextChange}s.
     */
    public static <PS, SEG, S> UndoManager<RichTextChange<PS, SEG, S>> richTextUndoManager(GenericStyledArea<PS, SEG, S> area,
                                                               UndoManagerFactory factory) {
        return factory.create(area.richChanges(), TextChange::invert, applyRichTextChange(area), TextChange::mergeWith, TextChange::isIdentity);
    };

    /**
     * Returns an UndoManager with an unlimited history that can undo/redo {@link PlainTextChange}s.
     */
    public static <PS, SEG, S> UndoManager<PlainTextChange> plainTextUndoManager(GenericStyledArea<PS, SEG, S> area) {
        return plainTextUndoManager(area, UndoManagerFactory.unlimitedHistoryFactory());
    }

    /**
     * Returns an UndoManager that can undo/redo {@link PlainTextChange}s.
     */
    public static <PS, SEG, S> UndoManager<PlainTextChange> plainTextUndoManager(GenericStyledArea<PS, SEG, S> area,
                                                                UndoManagerFactory factory) {
        return factory.create(area.plainTextChanges(), TextChange::invert, applyPlainTextChange(area), TextChange::mergeWith, TextChange::isIdentity);
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
}
