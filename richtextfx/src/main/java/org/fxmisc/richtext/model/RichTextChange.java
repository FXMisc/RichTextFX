package org.fxmisc.richtext.model;

/**
 * An object that specifies where a change occurred in a {@link org.fxmisc.richtext.GenericStyledArea}.
 */
public class RichTextChange<PS, SEG, S> extends TextChange<StyledDocument<PS, SEG, S>, RichTextChange<PS, SEG, S>> {

    public RichTextChange(int position, StyledDocument<PS, SEG, S> removed, StyledDocument<PS, SEG, S> inserted) {
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
    protected final StyledDocument<PS, SEG, S> concat(StyledDocument<PS, SEG, S> a, StyledDocument<PS, SEG, S> b) {
        return a.concat(b);
    }

    @Override
    protected final StyledDocument<PS, SEG, S> sub(StyledDocument<PS, SEG, S> doc, int from, int to) {
        return doc.subSequence(from, to);
    }

    @Override
    protected final RichTextChange<PS, SEG, S> create(int position, StyledDocument<PS, SEG, S> removed, StyledDocument<PS, SEG, S> inserted) {
        return new RichTextChange<>(position, removed, inserted);
    }

    public final PlainTextChange toPlainTextChange() {
        return new PlainTextChange(position, removed.getText(), inserted.getText());
    }

    /**
     * Equivalent to {@code richChange.toPlainTextChange().isIdentity()} but without the additional object
     * creation via {@link #toPlainTextChange()}.
     */
    public final boolean isPlainTextIdentity() {
        return removed.getText().equals(inserted.getText());
    }
}
