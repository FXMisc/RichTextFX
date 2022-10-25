package org.fxmisc.richtext.util;

import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.MultiChangeBuilder;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.RichTextChange;
import org.fxmisc.richtext.model.TextChange;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.UndoManagerFactory;
import org.fxmisc.undo.impl.MultiChangeUndoManagerImpl;
import org.fxmisc.undo.impl.UnlimitedChangeQueue;
import org.reactfx.SuspendableYes;
import org.reactfx.value.Val;

import javafx.beans.value.ObservableBooleanValue;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * A class filled with factory methods to help easily construct an {@link UndoManager} for a {@link GenericStyledArea}.
 */
public final class UndoUtils {

    private UndoUtils() {
        throw new IllegalStateException("UndoUtils cannot be instantiated");
    }

    public static final Duration DEFAULT_PREVENT_MERGE_DELAY = Duration.ofMillis(500);

    /**
     * Constructs an UndoManager with an unlimited history:
     * if {@link GenericStyledArea#isPreserveStyle() the area's preserveStyle flag is true}, the returned UndoManager
     * can undo/redo multiple {@link RichTextChange}s; otherwise, it can undo/redo multiple {@link PlainTextChange}s.
     */
    public static <PS, SEG, S> UndoManager defaultUndoManager(GenericStyledArea<PS, SEG, S> area) {
        return area.isPreserveStyle()
                ? richTextUndoManager(area)
                : plainTextUndoManager(area);
    }

    /**
     * Constructs an UndoManager with no history
     */
    public static UndoManager noOpUndoManager() {
        return new UndoManager() {

            private final Val<Boolean> alwaysFalse = Val.constant(false);

            @Override public boolean undo() { return false; }
            @Override public boolean redo() { return false; }
            @Override public Val<Boolean> undoAvailableProperty() { return alwaysFalse; }
            @Override public boolean isUndoAvailable() { return false; }
            @Override public Val<Boolean> redoAvailableProperty() { return alwaysFalse; }
            @Override public boolean isRedoAvailable() { return false; }
            @Override public boolean isPerformingAction() { return false; }
            @Override public boolean isAtMarkedPosition() { return false; }

            // not sure whether these may throw NPEs at some point
            @Override public Val nextUndoProperty() { return null; }
            @Override public Val nextRedoProperty() { return null; }
            @Override public ObservableBooleanValue performingActionProperty() { return null; }
            @Override public UndoPosition getCurrentPosition() { return null; }
            @Override public ObservableBooleanValue atMarkedPositionProperty() { return null; }

            // ignore these
            @Override public void preventMerge() { }
            @Override public void forgetHistory() { }
            @Override public void close() { }
        };
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
    public static <PS, SEG, S> UndoManager<List<RichTextChange<PS, SEG, S>>> richTextUndoManager(
            GenericStyledArea<PS, SEG, S> area) {
        return richTextUndoManager(area, UndoManagerFactory.unlimitedHistoryFactory());
    }

    /**
     * Returns an UndoManager that can undo/redo {@link RichTextChange}s. New changes
     * emitted from the stream will not be merged with the previous change
     * after {@code preventMergeDelay}
     */
    public static <PS, SEG, S> UndoManager<List<RichTextChange<PS, SEG, S>>> richTextUndoManager(
            GenericStyledArea<PS, SEG, S> area, Duration preventMergeDelay) {
        return richTextUndoManager(area, UndoManagerFactory.unlimitedHistoryFactory(), preventMergeDelay);
    };

    /**
     * Returns an UndoManager that can undo/redo {@link RichTextChange}s. New changes
     * emitted from the stream will not be merged with the previous change
     * after {@link #DEFAULT_PREVENT_MERGE_DELAY}
     */
    public static <PS, SEG, S> UndoManager<List<RichTextChange<PS, SEG, S>>> richTextUndoManager(
            GenericStyledArea<PS, SEG, S> area, UndoManagerFactory factory) {
        return richTextUndoManager(area, factory, DEFAULT_PREVENT_MERGE_DELAY);
    };

    /**
     * Returns an UndoManager that can undo/redo {@link RichTextChange}s. New changes
     * emitted from the stream will not be merged with the previous change
     * after {@code preventMergeDelay}
     */
    public static <PS, SEG, S> UndoManager<List<RichTextChange<PS, SEG, S>>> richTextUndoManager(
            GenericStyledArea<PS, SEG, S> area, UndoManagerFactory factory, Duration preventMergeDelay) {
        return factory.createMultiChangeUM(area.multiRichChanges(),
                TextChange::invert,
                applyMultiRichTextChange(area),
                TextChange::mergeWith,
                TextChange::isIdentity,
                preventMergeDelay);
    };

    /**
     * Returns an UndoManager with an unlimited history that can undo/redo {@link RichTextChange}s. New changes
     * emitted from the stream will not be merged with the previous change after {@link #DEFAULT_PREVENT_MERGE_DELAY}
     * <p><b>Note</b>: that <u>only styling changes</u> may occur <u>during suspension</u> of the undo manager.
     */
    public static <PS, SEG, S> UndoManager<List<RichTextChange<PS, SEG, S>>> richTextSuspendableUndoManager(
            GenericStyledArea<PS, SEG, S> area, SuspendableYes suspendUndo) {
        return richTextSuspendableUndoManager(area, DEFAULT_PREVENT_MERGE_DELAY, suspendUndo);
    }

    /**
     * Returns an UndoManager with an unlimited history that can undo/redo {@link RichTextChange}s. New changes
     * emitted from the stream will not be merged with the previous change after {@code preventMergeDelay}.
     * <p><b>Note</b>: that <u>only styling changes</u> may occur <u>during suspension</u> of the undo manager.
     */
    public static <PS, SEG, S> UndoManager<List<RichTextChange<PS, SEG, S>>> richTextSuspendableUndoManager(
            GenericStyledArea<PS, SEG, S> area, Duration preventMergeDelay, SuspendableYes suspendUndo) {

        RichTextChange.skipStyleComparison( true );

        return new MultiChangeUndoManagerImpl<>
        (
            new UnlimitedChangeQueue<>(),
            TextChange::invert,
            applyMultiRichTextChange(area),
            TextChange::mergeWith,
            TextChange::isIdentity,
            area.multiRichChanges().conditionOn(suspendUndo),
            preventMergeDelay
        );
    };

    /**
     * Returns an UndoManager with an unlimited history that can undo/redo {@link PlainTextChange}s. New changes
     * emitted from the stream will not be merged with the previous change
     * after {@link #DEFAULT_PREVENT_MERGE_DELAY}
     */
    public static <PS, SEG, S> UndoManager<List<PlainTextChange>> plainTextUndoManager(
            GenericStyledArea<PS, SEG, S> area) {
        return plainTextUndoManager(area, DEFAULT_PREVENT_MERGE_DELAY);
    }

    /**
     * Returns an UndoManager that can undo/redo {@link PlainTextChange}s. New changes
     * emitted from the stream will not be merged with the previous change
     * after {@code preventMergeDelay}
     */
    public static <PS, SEG, S> UndoManager<List<PlainTextChange>> plainTextUndoManager(
            GenericStyledArea<PS, SEG, S> area, Duration preventMergeDelay) {
        return plainTextUndoManager(area, UndoManagerFactory.unlimitedHistoryFactory(), preventMergeDelay);
    }

    /**
     * Returns an UndoManager that can undo/redo {@link PlainTextChange}s. New changes
     * emitted from the stream will not be merged with the previous change
     * after {@link #DEFAULT_PREVENT_MERGE_DELAY}
     */
    public static <PS, SEG, S> UndoManager<List<PlainTextChange>> plainTextUndoManager(
            GenericStyledArea<PS, SEG, S> area, UndoManagerFactory factory) {
        return plainTextUndoManager(area, factory, DEFAULT_PREVENT_MERGE_DELAY);
    }

    /**
     * Returns an UndoManager that can undo/redo {@link PlainTextChange}s. New changes
     * emitted from the stream will not be merged with the previous change
     * after {@code preventMergeDelay}
     */
    public static <PS, SEG, S> UndoManager<List<PlainTextChange>> plainTextUndoManager(
            GenericStyledArea<PS, SEG, S> area, UndoManagerFactory factory, Duration preventMergeDelay) {
        return factory.createMultiChangeUM(area.multiPlainChanges(),
                TextChange::invert,
                applyMultiPlainTextChange(area),
                TextChange::mergeWith,
                TextChange::isIdentity,
                preventMergeDelay);
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
        return change -> {
            area.replaceText(change.getPosition(), change.getRemovalEnd(), change.getInserted());
            moveToChange( area, change );
        };
    }

    /**
     * Applies a {@link RichTextChange} to the given area when the {@link UndoManager}'s change stream emits an event
     * by {@code area.replace(change.getPosition(), change.getRemovalEnd(), change.getInserted()}.
     */
    public static <PS, SEG, S> Consumer<RichTextChange<PS, SEG, S>> applyRichTextChange(
            GenericStyledArea<PS, SEG, S> area) {
        return change -> {
            area.replace(change.getPosition(), change.getRemovalEnd(), change.getInserted());
            moveToChange( area, change );
        };
    }

    /**
     * Applies a list of {@link PlainTextChange}s to the given area when the {@link UndoManager}'s change stream emits
     * an event by {@code area.replaceAbsolutely(change.getPosition(), change.getRemovalEnd(), change.getInserted()}.
     */
    public static <PS, SEG, S> Consumer<List<PlainTextChange>> applyMultiPlainTextChange(
            GenericStyledArea<PS, SEG, S> area) {
        return changeList -> {
            MultiChangeBuilder<PS, SEG, S> builder = area.createMultiChange(changeList.size());
            for (PlainTextChange c : changeList) {
                builder.replaceTextAbsolutely(c.getPosition(), c.getRemovalEnd(), c.getInserted());
            }
            builder.commit();
            moveToChange( area, changeList.get( changeList.size()-1 ) );
        };
    }

    /**
     * Applies a list of {@link RichTextChange} to the given area when the {@link UndoManager}'s change stream emits
     * an event by {@code area.replaceAbsolutely(change.getPosition(), change.getRemovalEnd(), change.getInserted()}.
     */
    public static <PS, SEG, S> Consumer<List<RichTextChange<PS, SEG, S>>> applyMultiRichTextChange(
            GenericStyledArea<PS, SEG, S> area) {
        return changeList -> {
            MultiChangeBuilder<PS, SEG, S> builder = area.createMultiChange(changeList.size());
            for (RichTextChange<PS, SEG, S> c : changeList) {
                builder.replaceAbsolutely(c.getPosition(), c.getRemovalEnd(), c.getInserted());
            }
            builder.commit();
            moveToChange( area, changeList.get( changeList.size()-1 ) );
        };
    }

    /*
     * Address #912 "After undo/redo, new text is inserted at the end".
     * Without breaking PositionTests. (org.fxmisc.richtext.api.caret)
     */
    private static void moveToChange( GenericStyledArea area, TextChange chg )
    {
        int pos = chg.getPosition();
        int len = chg.getNetLength();
        if ( len > 0 ) pos += len;

        area.moveTo( Math.min( pos, area.getLength() ) );
    }
}
