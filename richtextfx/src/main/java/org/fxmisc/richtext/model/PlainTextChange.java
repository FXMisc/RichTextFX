package org.fxmisc.richtext.model;

/**
 * An object that specifies where a non-style change occurred in a {@link org.fxmisc.richtext.GenericStyledArea}.
 */
public class PlainTextChange extends TextChange<PlainTextChangeData, PlainTextChange> {
    public PlainTextChange(int caretBefore, int position, String removed, String inserted) {
        this(caretBefore, position, new PlainTextChangeData(removed), new PlainTextChangeData(inserted));
    }

    private PlainTextChange(int caretBefore, int position, PlainTextChangeData removed, PlainTextChangeData inserted) {
        super(caretBefore, position, removed, inserted);
    }

    private PlainTextChange(CaretChange caretChange, int position, PlainTextChangeData removed, PlainTextChangeData inserted) {
        super(caretChange, position, removed, inserted);
    }

    @Override
    protected final PlainTextChange create(CaretChange caretChange, int position, PlainTextChangeData removed, PlainTextChangeData inserted) {
        return new PlainTextChange(caretChange, position, removed, inserted);
    }
}
