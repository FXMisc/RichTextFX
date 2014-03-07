package org.fxmisc.richtext;

public class RichTextChange<S> extends TextChange<StyledDocument<S>, RichTextChange<S>> {

    public RichTextChange(int position, StyledDocument<S> removed, StyledDocument<S> inserted) {
        super(position, removed, inserted);
    }

    @Override
    protected final StyledDocument<S> concat(StyledDocument<S> a, StyledDocument<S> b) {
        return a.concat(b);
    }

    @Override
    protected final StyledDocument<S> sub(StyledDocument<S> doc, int from, int to) {
        return doc.subSequence(from, to);
    }

    @Override
    protected final RichTextChange<S> create(int position, StyledDocument<S> removed, StyledDocument<S> inserted) {
        return new RichTextChange<S>(position, removed, inserted);
    }
}
