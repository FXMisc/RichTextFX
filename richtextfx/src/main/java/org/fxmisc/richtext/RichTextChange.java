package org.fxmisc.richtext;

public class RichTextChange<PS, S> extends TextChange<StyledDocument<PS, S>, RichTextChange<PS, S>> {

    public RichTextChange(int position, StyledDocument<PS, S> removed, StyledDocument<PS, S> inserted) {
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
    protected final StyledDocument<PS, S> concat(StyledDocument<PS, S> a, StyledDocument<PS, S> b) {
        return a.concat(b);
    }

    @Override
    protected final StyledDocument<PS, S> sub(StyledDocument<PS, S> doc, int from, int to) {
        return doc.subSequence(from, to);
    }

    @Override
    protected final RichTextChange<PS, S> create(int position, StyledDocument<PS, S> removed, StyledDocument<PS, S> inserted) {
        return new RichTextChange<>(position, removed, inserted);
    }

    @Override
    public final String toString() {
        return
                "RichTextChange{\n" +
                "\tposition: " + position + "\n" +
                "\tremoved: " + removed + "\n" +
                "\tinserted: " + inserted + "\n" +
                "}";
    }
}
