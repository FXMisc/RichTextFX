package codearea.control;

import java.util.List;

import javafx.scene.control.IndexRange;

public interface StyledDocument<S> extends TwoDimensional {
    String getText();
    String getText(IndexRange range);
    String getText(int start, int end);

    StyledDocument<S> subDocument(IndexRange range);
    StyledDocument<S> subDocument(int start, int end);

    int getLength();

    S getStyleAt(int pos);
    S getStyleAt(int paragraph, int column);

    List<Paragraph<S>> getParagraphs();
}
