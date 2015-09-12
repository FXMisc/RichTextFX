package org.fxmisc.richtext;

import java.util.Objects;


public class StyledText<S> implements CharSequence {
    private final String text;
    private S style;

    public StyledText(String text, S style) {
        this.text = text;
        this.style = style;
    }

    @Override
    public int length() {
        return text.length();
    }

    @Override
    public char charAt(int index) {
        return text.charAt(index);
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public StyledText<S> subSequence(int start, int end) {
        return new StyledText<S>(text.substring(start, end), style);
    }

    public StyledText<S> subSequence(int start) {
        return new StyledText<S>(text.substring(start), style);
    }

    public StyledText<S> concat(CharSequence str) {
        return new StyledText<S>(text + str, style);
    }

    public StyledText<S> spliced(int from, int to, CharSequence replacement) {
        String left = text.substring(0, from);
        String right = text.substring(to);
        return new StyledText<S>(left + replacement + right, style);
    }

    public S getStyle() {
        return style;
    }

    public void setStyle(S style) {
        this.style = style;
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
