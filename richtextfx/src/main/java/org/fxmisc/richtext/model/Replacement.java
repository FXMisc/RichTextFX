package org.fxmisc.richtext.model;

import java.util.Objects;

/**
 * Encapsulates the all the arguments for {@link EditableStyledDocument#replace(int, int, StyledDocument)}.
 */
public class Replacement<PS, SEG, S> {

    private final int start;
    public final int getStart() { return start; }

    private final int end;
    public final int getEnd() { return end; }

    private final ReadOnlyStyledDocument<PS, SEG, S> document;
    public final ReadOnlyStyledDocument<PS, SEG, S> getDocument() { return document; }

    public Replacement(int start, int end, ReadOnlyStyledDocument<PS, SEG, S> document) {
        this.start = start;
        this.end = end;
        this.document = document;
    }

    /**
     * Shortcut for {@code document.length() - (end - start)}
     */
    public int getNetLength() {
        return document.length() - (end - start);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Replacement) {
            Replacement<?, ?, ?> that = (Replacement<?, ?, ?>) obj;
            return Objects.equals(start, that.start) &&
                    Objects.equals(end, that.end) &&
                    Objects.equals(document, that.document);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, document);
    }

    @Override
    public String toString() {
        return String.format("Replacement(start=%s end=%s document=%s)", start, end, document);
    }
}
