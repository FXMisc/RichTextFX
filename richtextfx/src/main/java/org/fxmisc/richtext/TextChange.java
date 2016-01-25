package org.fxmisc.richtext;

import java.util.Objects;
import java.util.Optional;

public abstract class TextChange<S, Self extends TextChange<S, Self>> {

    protected final int position;
    protected final S removed;
    protected final S inserted;

    public TextChange(int position, S removed, S inserted) {
        this.position = position;
        this.removed = removed;
        this.inserted = inserted;
    }

    public int getPosition() { return position; };
    public S getRemoved() { return removed; }
    public S getInserted() { return inserted; }
    public Self invert() { return create(position, inserted, removed); }

    protected abstract int removedLength();
    protected abstract int insertedLength();
    protected abstract S concat(S a, S b);
    protected abstract S sub(S s, int from, int to);
    protected abstract Self create(int position, S removed, S inserted);

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
        }
        else if(latter.position + latter.removedLength() == this.position + this.insertedLength()) {
            if(this.position <= latter.position) {
                S addedText = concat(sub(this.inserted, 0, latter.position - this.position), latter.inserted);
                return Optional.of(create(this.position, this.removed, addedText));
            }
            else {
                S removedText = concat(sub(latter.removed, 0, this.position - latter.position), this.removed);
                return Optional.of(create(latter.position, removedText, latter.inserted));
            }
        }
        else {
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
}
