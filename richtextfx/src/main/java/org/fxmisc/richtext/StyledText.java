package org.fxmisc.richtext;

import java.util.Objects;

public class StyledText<S> {
    private final String text;
    private S style;

    public StyledText(String text, S style) {
        this.text = text;
        this.style = style;
    }

    public int length() {
        return text.length();
    }

    public char charAt(int index) {
        return text.charAt(index);
    }

    public String getText() {
        return text;
    }

    public StyledText<S> subSequence(int start, int end) {
        return new StyledText<>(text.substring(start, end), style);
    }

    public StyledText<S> subSequence(int start) {
        return new StyledText<>(text.substring(start), style);
    }

    public StyledText<S> append(String str) {
        return new StyledText<>(text + str, style);
    }

    public StyledText<S> spliced(int from, int to, CharSequence replacement) {
        String left = text.substring(0, from);
        String right = text.substring(to);
        return new StyledText<>(left + replacement + right, style);
    }

    public S getStyle() {
        return style;
    }

    public void setStyle(S style) {
        this.style = style;
    }

    @Override
    public String toString() {
        return '"' + text + '"' + ":" + style;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof StyledText) {
            StyledText<?> that = (StyledText<?>) obj;
            return Objects.equals(this.text, that.text)
                && Objects.equals(this.style, that.style);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, style);
    }
}
