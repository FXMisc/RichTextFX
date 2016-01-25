package org.fxmisc.richtext;

public class RichTextChange<S, PS> extends TextChange<StyledDocument<S, PS>, RichTextChange<S, PS>> {

    public RichTextChange(int position, StyledDocument<S, PS> removed, StyledDocument<S, PS> inserted) {
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
    protected final StyledDocument<S, PS> concat(StyledDocument<S, PS> a, StyledDocument<S, PS> b) {
        return a.concat(b);
    }

    @Override
    protected final StyledDocument<S, PS> sub(StyledDocument<S, PS> doc, int from, int to) {
        return doc.subSequence(from, to);
    }

    @Override
    protected final RichTextChange<S, PS> create(int position, StyledDocument<S, PS> removed, StyledDocument<S, PS> inserted) {
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
