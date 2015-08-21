package org.fxmisc.richtext;

public class RichTextChange<S, PS> extends TextChange<StyledDocument<S, PS>, RichTextChange<S, PS>> {

    public RichTextChange(int position, StyledDocument<S, PS> removed, StyledDocument<S, PS> inserted) {
        super(position, removed, inserted);
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
}
