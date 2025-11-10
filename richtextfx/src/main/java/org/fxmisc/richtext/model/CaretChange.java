package org.fxmisc.richtext.model;

import java.util.Objects;

/**
 * Record the caret position change
 * @param before the caret position before
 * @param after the caret position after the change
 */
public record CaretChange(int before, int after) {
    public CaretChange invert() {
        return new CaretChange(after, before);
    }

    public CaretChange merge(CaretChange latter, int removalEnd) {
        int after = latter.after();
        // If the former caret position is one of the edge, it is the one used
        int before = (this.before == after || this.before == removalEnd) ?
                this.before : Math.min(this.before, latter.before);
        return new CaretChange(before, after);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CaretChange that)) return false;
        return after == that.after && before == that.before;
    }

    @Override
    public int hashCode() {
        return Objects.hash(before, after);
    }
}
