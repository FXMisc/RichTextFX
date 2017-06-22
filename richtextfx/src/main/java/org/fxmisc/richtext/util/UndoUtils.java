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

    public static <PS, SEG, S> UndoManager createUndoManager(GenericStyledArea<PS, SEG, S> area) {
        return area.isPreserveStyle()
                ? richTextUndoManager(area)
                : plainTextUndoManager(area);
    }

    public static <PS, SEG, S> UndoManager<RichTextChange<PS, SEG, S>> richTextUndoManager(GenericStyledArea<PS, SEG, S> area) {
        return richTextUndoManager(area, UndoManagerFactory.unlimitedHistoryFactory());
    }

    public static <PS, SEG, S> UndoManager<RichTextChange<PS, SEG, S>> richTextUndoManager(GenericStyledArea<PS, SEG, S> area,
                                                               UndoManagerFactory factory) {
        Consumer<RichTextChange<PS, SEG, S>> apply = change -> area.replace(change.getPosition(), change.getPosition() + change.getRemoved().length(), change.getInserted());
        return factory.create(area.richChanges(), RichTextChange::invert, apply, TextChange::mergeWith, TextChange::isIdentity);
    };

    public static <PS, SEG, S> UndoManager<PlainTextChange> plainTextUndoManager(GenericStyledArea<PS, SEG, S> area) {
        return plainTextUndoManager(area, UndoManagerFactory.unlimitedHistoryFactory());
    }

    public static <PS, SEG, S> UndoManager<PlainTextChange> plainTextUndoManager(GenericStyledArea<PS, SEG, S> area,
                                                                UndoManagerFactory factory) {
        Consumer<PlainTextChange> apply = change -> area.replaceText(change.getPosition(), change.getPosition() + change.getRemoved().length(), change.getInserted());
        return factory.create(area.plainTextChanges(), PlainTextChange::invert, apply, TextChange::mergeWith, TextChange::isIdentity);
    }
}
