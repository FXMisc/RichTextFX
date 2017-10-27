package org.fxmisc.richtext.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Base change class for style changes ({@link RichTextChange}) and non-style changes ({@link PlainTextChange})
 * in a {@link org.fxmisc.richtext.TextEditingArea}.
 *
 * @param <S> type of data that was removed and inserted in the {@link org.fxmisc.richtext.TextEditingArea}.
 * @param <Self> a subclass of TextChange
 */
public abstract class TextChange<S, Self extends TextChange<S, Self>> {

    protected final int position;
    protected final S removed;
    protected final S inserted;

    public TextChange(int position, S removed, S inserted) {
        this.position = position;
        this.removed = removed;
        this.inserted = inserted;
    }

    /**
     * Gets the start position of where the replacement happened
     */
    public int getPosition() { return position; };

    public S getRemoved() { return removed; }
    public S getInserted() { return inserted; }

    /**
     * Returns a new subclass of {@link TextChange} that makes the {@code inserted} the removed object and
     * the {@code removed} the inserted object
     */
    public Self invert() { return create(position, inserted, removed); }

    /** Returns the position where the removal ends (e.g. {@code position + removedLength())} */
    public int getRemovalEnd() { return position + removedLength(); }

    /** Returns the position where the inserted ends (e.g. {@code position + insertedLength())} */
    public int getInsertionEnd() { return position + insertedLength(); }

    /**
     * Gets the net length of this change (i.e., {@code insertedLength() - removedLength()})
     */
    public int getNetLength() { return insertedLength() - removedLength(); }

    protected abstract int removedLength();
    protected abstract int insertedLength();
    protected abstract S concat(S a, S b);
    protected abstract S sub(S s, int from, int to);
    protected abstract Self create(int position, S removed, S inserted);

    /**
     * Returns true if this change is an identity change: applying it does nothing as it removes what it inserts.
     * See also {@link java.util.function.Function#identity()}
     */
    public final boolean isIdentity() {
        return removed.equals(inserted);
    }

    /**
     * Merges this change with the given change, if possible.
     * This change is considered to be the former and the given
     * change is considered to be the latter.
     * Changes can be merged if either
     * <ul>
     *   <li>the latter's start matches the former's added text end; or</li>
     *   <li>the latter's removed text end matches the former's added text end.</li>
     * </ul>
     * @param latter change to merge with this change.
     * @return a new merged change if changes can be merged,
     * {@code null} otherwise.
     */
    public Optional<Self> mergeWith(Self latter) {
        if(latter.position == this.position + this.insertedLength()) {
            S removedText = concat(this.removed, latter.removed);
            S addedText = concat(this.inserted, latter.inserted);
            return Optional.of(create(this.position, removedText, addedText));
        } else if(latter.position + latter.removedLength() == this.position + this.insertedLength()) {
            if(this.position <= latter.position) {
                S addedText = concat(sub(this.inserted, 0, latter.position - this.position), latter.inserted);
                return Optional.of(create(this.position, this.removed, addedText));
            }
            else {
                S removedText = concat(sub(latter.removed, 0, this.position - latter.position), this.removed);
                return Optional.of(create(latter.position, removedText, latter.inserted));
            }
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof TextChange) {
            TextChange<?, ?> that = (TextChange<?, ?>) other;
            return Objects.equals(this.position, that.position)
                && Objects.equals(this.removed,  that.removed )
                && Objects.equals(this.inserted, that.inserted);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, removed, inserted);
    }

    @Override
    public final String toString() {
        return
                this.getClass().getSimpleName() + "{\n" +
                "\tposition: "  + position  + "\n" +
                "\tremoved: "   + removed   + "\n" +
                "\tinserted: "  + inserted  + "\n" +
                "}";
    }
}
