package org.fxmisc.richtext.model;

/**
 * An object that specifies where a non-style change occurred in a {@link org.fxmisc.richtext.GenericStyledArea}.
 */
public class PlainTextChange extends TextChange<PlainTextChangeData, PlainTextChange> {
    public PlainTextChange(int position, String removed, String inserted) {
        this(position, new PlainTextChangeData(removed), new PlainTextChangeData(inserted));
    }

    private PlainTextChange(int position, PlainTextChangeData removed, PlainTextChangeData inserted) {
        super(position, removed, inserted);
    }

    @Override
    protected final PlainTextChange create(int position, PlainTextChangeData removed, PlainTextChangeData inserted) {
        return new PlainTextChange(position, removed, inserted);
    }
}
