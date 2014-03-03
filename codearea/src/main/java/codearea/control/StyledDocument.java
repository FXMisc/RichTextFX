package codearea.control;

import java.util.List;

import javafx.scene.control.IndexRange;

public interface StyledDocument<S> extends CharSequence, TwoDimensional {
    String getText();
    String getText(int start, int end);
    String getText(IndexRange range);

    @Override
    StyledDocument<S> subSequence(int start, int end);
    StyledDocument<S> subSequence(IndexRange range);
    StyledDocument<S> subDocument(int paragraphIndex);

    StyledDocument<S> concat(StyledDocument<S> latter);

    S getStyleAt(int pos);
    S getStyleAt(int paragraph, int column);

    StyleSpans<S> getStyleSpans(int from, int to);
    StyleSpans<S> getStyleSpans(int paragraph, int from, int to);

    List<Paragraph<S>> getParagraphs();
}
