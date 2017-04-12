package org.fxmisc.richtext.model;

import java.util.Objects;
import java.util.Optional;

public abstract class TextChange<S, Self extends TextChange<S, Self>> {

    /**
     * Indicates whether this change can be merged with another change if they are both {@link #INSERTION} or
     * both {@link #DELETION}.
     */
    public static enum MergeType {
        /** Indicates that the change will insert something but not remove anything */
        INSERTION,
        /** Indicates that the change will delete something but not insert anything */
        DELETION,
        /** Indicates that the change is a style change, a replacement or something other than the other two types */
        NONE,
    }


    protected final int position;
    protected final S removed;
    protected final S inserted;
    protected final MergeType mergeType;

    public TextChange(int position, S removed, S inserted) {
        this.position = position;
        this.removed = removed;
        this.inserted = inserted;

        if (insertedLength() == 0) {
            mergeType = removedLength() != 0
                    ? MergeType.DELETION
                    : MergeType.NONE;
        } else if (removedLength() == 0) {
            mergeType = MergeType.INSERTION;
        } else {
            mergeType = MergeType.NONE;
        }
    }

    public int getPosition() { return position; };
    public S getRemoved() { return removed; }
    public S getInserted() { return inserted; }
    public Self invert() { return create(position, inserted, removed); }
    public int getRemovalEnd() { return position + removedLength(); }
    public int getInsertionEnd() { return position + insertedLength(); }
    public final MergeType getMergeType() { return mergeType; };

    protected abstract int removedLength();
    protected abstract int insertedLength();
    protected abstract S concat(S a, S b);
    protected abstract S sub(S s, int from, int to);
    protected abstract Self create(int position, S removed, S inserted);

    /**
     * Merges this change with the given change only if the end of this change's inserted text equals the
     * latter's position and both are either {@link MergeType#INSERTION} or {@link MergeType#DELETION} changes.
     *
     * @param latter change to merge with this change.
     * @return a new merged change if changes can be merged,
     * {@code null} otherwise.
     */
    public Optional<Self> mergeWith(Self latter) {
        if((this.mergeType == MergeType.INSERTION || this.mergeType == MergeType.DELETION)
                && this.mergeType == latter.mergeType
                && this.getInsertionEnd() == latter.position) {
            S removedText = concat(this.removed, latter.removed);
            S addedText = concat(this.inserted, latter.inserted);
            return Optional.of(create(this.position, removedText, addedText));
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
                "\tmergeType: " + mergeType + "\n" +
                "\tremoved: "   + removed   + "\n" +
                "\tinserted: "  + inserted  + "\n" +
                "}";
    }
}
