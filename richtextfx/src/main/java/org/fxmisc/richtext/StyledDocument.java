package org.fxmisc.richtext;

import java.util.List;

import javafx.scene.control.IndexRange;

public interface StyledDocument<S, PS> extends CharSequence, TwoDimensional {
    String getText();
    String getText(int start, int end);
    String getText(IndexRange range);

    @Override
    StyledDocument<S, PS> subSequence(int start, int end);
    StyledDocument<S, PS> subSequence(IndexRange range);
    StyledDocument<S, PS> subDocument(int paragraphIndex);

    StyledDocument<S, PS> concat(StyledDocument<S, PS> latter);

    S getStyleOfChar(int index);
    S getStyleOfChar(int paragraph, int column);

    S getStyleAtPosition(int position);
    S getStyleAtPosition(int paragraph, int position);

    IndexRange getStyleRangeAtPosition(int position);
    IndexRange getStyleRangeAtPosition(int paragraph, int position);

    StyleSpans<S> getStyleSpans(int from, int to);
    StyleSpans<S> getStyleSpans(int paragraph);
    StyleSpans<S> getStyleSpans(int paragraph, int from, int to);

    List<Paragraph<S, PS>> getParagraphs();
}
