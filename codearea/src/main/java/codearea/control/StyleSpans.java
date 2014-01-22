package codearea.control;


public interface StyleSpans<S> extends Iterable<StyleSpan<S>>, TwoDimensional {
    int length();
    int getSpanCount();
    StyleSpan<S> getStyleSpan(int index);
    StyleSpans<S> subView(Position from, Position to);
}
