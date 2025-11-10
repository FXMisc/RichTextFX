package org.fxmisc.richtext.model;

/**
 * An object that specifies where a change occurred in a {@link org.fxmisc.richtext.GenericStyledArea}.
 */
public class RichTextChange<PS, SEG, S> extends TextChange<RichTextChangeData<PS, SEG, S>, RichTextChange<PS, SEG, S>> {

    public RichTextChange(int position, StyledDocument<PS, SEG, S> removed, StyledDocument<PS, SEG, S> inserted) {
        super(position, new RichTextChangeData<>(removed), new RichTextChangeData<>(inserted));
    }

    private RichTextChange(int position, RichTextChangeData<PS, SEG, S> removed, RichTextChangeData<PS, SEG, S> inserted) {
        super(position, removed, inserted);
    }

    @Override
    protected final RichTextChange<PS, SEG, S> create(int position, RichTextChangeData<PS, SEG, S> removed, RichTextChangeData<PS, SEG, S> inserted) {
        return new RichTextChange<>(position, removed, inserted);
    }

    // TODO SMA move to adapter
    public final PlainTextChange toPlainTextChange() {
        return new PlainTextChange(getPosition(), getRemoved().getText(), getInserted().getText());
    }

    /**
     * Equivalent to {@code richChange.toPlainTextChange().isIdentity()} but without the additional object
     * creation via {@link #toPlainTextChange()}.
     */
    public final boolean isPlainTextIdentity() {
        return getRemoved().getText().equals(getInserted().getText());
    }
    
    private static boolean skipStyleComparison = false;
    
    public static void skipStyleComparison( boolean value )
    {
        skipStyleComparison = value;
    }
    
    /* 
     * This gets used, by the default UndoManagers supplied by UndoUtils,
     * to check that a submitted undo/redo matches the change reported. 
     */
    public boolean equals( Object other )
    {
        if( skipStyleComparison && other instanceof RichTextChange )
        {
            PlainTextChange otherChange = ((RichTextChange<?, ?, ?>) other).toPlainTextChange();
            boolean matches = toPlainTextChange().equals( otherChange );
            if ( ! matches ) System.err.println(
                "Plain text comparison mismatch caused by text change"
                +" during undo manager suspension (styling ignored)."
            );
            return matches;
        }

        return super.equals( other );
    }
}
