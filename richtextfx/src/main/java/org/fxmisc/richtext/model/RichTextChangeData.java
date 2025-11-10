package org.fxmisc.richtext.model;

import java.util.Objects;

/**
 * An object that specifies where a change occurred in a {@link org.fxmisc.richtext.GenericStyledArea}.
 */
public class RichTextChangeData<PS, SEG, S> implements TextChangeData<StyledDocument<PS, SEG, S>, RichTextChangeData<PS, SEG, S>> {
    private final StyledDocument<PS, SEG, S> document;

    public RichTextChangeData(StyledDocument<PS, SEG, S> document) {
        this.document = document;
    }

    public String getText() {
        return document.getText();
    }

    @Override
    public int length() {
        return document.length();
    }

    @Override
    public RichTextChangeData<PS, SEG, S> concat(RichTextChangeData<PS, SEG, S> b) {
        return new RichTextChangeData<>(document.concat(b.document));
    }

    @Override
    public RichTextChangeData<PS, SEG, S> sub(int from, int to) {
        return new RichTextChangeData<>(document.subSequence(from, to));
    }

    @Override
    public StyledDocument<PS, SEG, S> data() {
        return document;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RichTextChangeData<?, ?, ?> that)) return false;
        return Objects.equals(document, that.document);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(document);
    }
}
