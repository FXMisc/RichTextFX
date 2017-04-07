package org.fxmisc.richtext.model;


public class PlainTextChange extends TextChange<String, PlainTextChange> {

    private final ChangeType type;

    public PlainTextChange(int position, String removed, String inserted) {
        super(position, removed, inserted);
        if (insertedLength() == 0) {
            if (removedLength() == 0) {
                throw new IllegalStateException(String.format("Cannot get the type of a change that neither inserts nor deletes anything." +
                        "removed=%s inserted=%s", removed, inserted));
            } else {
                type = ChangeType.DELETION;
            }
        } else if (removedLength() == 0) {
            type = ChangeType.INSERTION;
        } else {
            type = ChangeType.REPLACEMENT;
        }
    }

    @Override
    public final ChangeType getType() { return type; }

    @Override
    protected int removedLength() {
        return removed.length();
    }

    @Override
    protected int insertedLength() {
        return inserted.length();
    }

    @Override
    protected final String concat(String a, String b) {
        return a + b;
    }

    @Override
    protected final String sub(String s, int from, int to) {
        return s.substring(from, to);
    }

    @Override
    protected final PlainTextChange create(int position, String removed, String inserted) {
        return new PlainTextChange(position, removed, inserted);
    }
}
