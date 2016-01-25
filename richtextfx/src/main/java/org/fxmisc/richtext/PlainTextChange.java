package org.fxmisc.richtext;


public class PlainTextChange extends TextChange<String, PlainTextChange> {

    public PlainTextChange(int position, String removed, String inserted) {
        super(position, removed, inserted);
    }

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
