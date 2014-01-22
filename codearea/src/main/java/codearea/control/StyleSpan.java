package codearea.control;

public class StyleSpan<S> {

    private final S style;
    private final int length;

    public StyleSpan(S style, int length) {
        this.style = style;
        this.length = length;
    }

    public S getStyle() {
        return style;
    }

    public int getLength() {
        return length;
    }
}
