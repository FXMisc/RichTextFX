package org.fxmisc.richtext;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.IndexRange;
import org.reactfx.EventStream;
import org.reactfx.SuspendableNo;
import org.reactfx.value.Val;

public interface EditableStyledDocument<PS, S> extends StyledDocument<PS, S> {

    String getText();
    ObservableValue<String> textProperty();

    int getLength();
    Val<Integer> lengthProperty();

    ObservableList<Paragraph<PS, S>> getParagraphs();

    ReadOnlyStyledDocument<PS, S> snapshot();

    default EventStream<PlainTextChange> plainChanges() {
        return richChanges()
                // map is used to prevent code repetition: StyledDocument#getText()
                .map(c -> new PlainTextChange(c.position, c.removed.getText(), c.inserted.getText()))
                // filter out rich changes where the style was changed but text wasn't added/removed
                .filter(pc -> !pc.removed.equals(pc.inserted));
    }

    EventStream<RichTextChange<PS, S>> richChanges();

    SuspendableNo beingUpdatedProperty();
    boolean isBeingUpdated();

    StyledDocument<PS, S> subSequence(int start, int end);

    StyledDocument<PS, S> subDocument(int paragraphIndex);

    void replace(int start, int end, StyledDocument<PS, S> replacement);

    void setStyle(int from, int to, S style);

    void setStyle(int paragraph, S style);

    void setStyle(int paragraph, int fromCol, int toCol, S style);

    void setStyleSpans(int from, StyleSpans<? extends S> styleSpens);

    void setStyleSpans(int paragraph, int from, StyleSpans<? extends S> styleSpens);

    void setParagraphStyle(int parIdx, PS style);

    S getStyleOfChar(int index);

    S getStyleOfChar(int paragraphIndex, int colIndex);

    S getStyleAtPosition(int position);

    S getStyleAtPosition(int paragraphIndex, int colIndex);

    PS getParagraphStyleAtPosition(int pos);

    IndexRange getStyleRangeAtPosition(int paragraph, int position);

    IndexRange getStyleRangeAtPosition(int position);

    StyleSpans<S> getStyleSpans(int from, int to);

    TwoDimensional.Position offsetToPosition(int offset, TwoDimensional.Bias bias);

}
