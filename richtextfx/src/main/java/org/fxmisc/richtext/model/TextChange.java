package org.fxmisc.richtext.model;

import java.util.Objects;
import java.util.Optional;

public abstract class TextChange<S, Self extends TextChange<S, Self>> {

    public static enum ChangeType {
        /** Indicates that the change will insert something but not remove anything */
        INSERTION,
        /** Indicates that the change will delete something but not insert anything */
        DELETION,
        /** Indicates that the change will remove something and insert something as its replacement */
        REPLACEMENT
    }

    private ChangeType type;
    public final ChangeType getType() {
        if (type == null) {
            if (insertedLength() == 0) {
                if (removedLength() == 0) {
                    throw new IllegalStateException("Cannot get the type of a change that neither inserts nor deletes anything.");
                } else {
                    type = ChangeType.DELETION;
                }
            } else if (removedLength() == 0) {
                type = ChangeType.INSERTION;
            } else {
                type = ChangeType.REPLACEMENT;
            }
        }
        return type;
    }

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
    public int getRemovalEnd() { return position + removedLength(); }
    public int getInsertionEnd() { return position + insertedLength(); }

    protected abstract int removedLength();
    protected abstract int insertedLength();
    protected abstract S concat(S a, S b);
    protected abstract S sub(S s, int from, int to);
    protected abstract Self create(int position, S removed, S inserted);

    /**
     * Merges this change with the given change only if the end of this change's inserted text
     * equals the latter's position and both are either insertion or deletion changes.
     *
     * @param latter change to merge with this change.
     * @return a new merged change if changes can be merged,
     * {@code null} otherwise.
     */
    public Optional<Self> mergeWith(Self latter) {
        if(this.getType() != ChangeType.REPLACEMENT
                && this.getType() == latter.getType()
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
                "\ttype: "      + getType() + "\n" +
                "\tremoved: "   + removed   + "\n" +
                "\tinserted: "  + inserted  + "\n" +
                "}";
    }
}
