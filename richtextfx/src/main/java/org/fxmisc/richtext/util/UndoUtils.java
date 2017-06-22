package org.fxmisc.richtext.util;

import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.RichTextChange;
import org.fxmisc.richtext.model.TextChange;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.UndoManagerFactory;

import java.util.function.Consumer;

public final class UndoUtils {

    private UndoUtils() {
        throw new IllegalStateException("UndoUtils cannot be instantiated");
    }

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

    public static <PS, SEG, S> UndoManager<RichTextChange<PS, SEG, S>> richTextUndoManager(GenericStyledArea<PS, SEG, S> area) {
        return richTextUndoManager(area, UndoManagerFactory.unlimitedHistoryFactory());
    }

    public static <PS, SEG, S> UndoManager<RichTextChange<PS, SEG, S>> richTextUndoManager(GenericStyledArea<PS, SEG, S> area,
                                                               UndoManagerFactory factory) {
        return factory.create(area.richChanges(), RichTextChange::invert, applyRichTextChange(area), TextChange::mergeWith, TextChange::isIdentity);
    };

    public static <PS, SEG, S> UndoManager<PlainTextChange> plainTextUndoManager(GenericStyledArea<PS, SEG, S> area) {
        return plainTextUndoManager(area, UndoManagerFactory.unlimitedHistoryFactory());
    }

    public static <PS, SEG, S> UndoManager<PlainTextChange> plainTextUndoManager(GenericStyledArea<PS, SEG, S> area,
                                                                UndoManagerFactory factory) {
        return factory.create(area.plainTextChanges(), PlainTextChange::invert, applyPlainTextChange(area), TextChange::mergeWith, TextChange::isIdentity);
    }

    /* ********************************************************************** *
     *                                                                        *
     * Change Appliers                                                        *
     *                                                                        *
     * Code that handles how a change should be applied to the area           *
     *                                                                        *
     * ********************************************************************** */

    public static <PS, SEG, S> Consumer<PlainTextChange> applyPlainTextChange(GenericStyledArea<PS, SEG, S> area) {
        return change -> area.replaceText(change.getPosition(), change.getPosition() + change.getRemoved().length(), change.getInserted());
    }

    public static <PS, SEG, S> Consumer<RichTextChange<PS, SEG, S>> applyRichTextChange(GenericStyledArea<PS, SEG, S> area) {
        return change -> area.replace(change.getPosition(), change.getPosition() + change.getRemoved().length(), change.getInserted());
    }
}
