package org.fxmisc.richtext.model;

import java.util.Objects;

/**
 * An object that specifies where a non-style change occurred in a {@link org.fxmisc.richtext.GenericStyledArea}.
 */
public class PlainTextChangeData implements TextChangeData<String, PlainTextChangeData> {
    private final String value;

    public PlainTextChangeData(String value) {
        this.value = value;
    }

    @Override
    public int length() {
        return value.length();
    }

    @Override
    public PlainTextChangeData concat(PlainTextChangeData b) {
        return new PlainTextChangeData(this.value + b.value);
    }

    @Override
    public PlainTextChangeData sub(int from, int to) {
        return new PlainTextChangeData(value.substring(from, to));
    }

    @Override
    public String data() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlainTextChangeData that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
